package com.painless.pc;

import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class FlashActivity extends Activity implements SurfaceHolder.Callback, OnColorChangedListener {

	private static final String COLOR_PREF = "screen_light_color";
	private static final String FLASH_PREF = "screen_light_flash";

	private View root;
	private SharedPreferences pref;
	
	private boolean zeroBrightness;
	
	private SurfaceView surfaceView;
	private ImageButton flashBtn;
	private SurfaceHolder holder;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN |
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

		setContentView(R.layout.screen_light);

		root = findViewById(R.id.bgImage);
		flashBtn = (ImageButton) findViewById(R.id.btn_flash);
		surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		holder = surfaceView.getHolder();
		holder.addCallback(this);
		holder.setType(3);

		pref = Globals.getAppPrefs(this);
		zeroBrightness = pref.getBoolean("bright_slider_zero", false);

		ColorButton colorButton = (ColorButton) findViewById(R.id.button_color_1);
		colorButton.setAlphaMsg(R.string.lbl_brightness);
		colorButton.setOnColorChangeListener(this, true);
		colorButton.setColor(pref.getInt(COLOR_PREF, Color.WHITE), true);
	}

	@Override
	protected void onPause() {
		turnFlashOff();
		setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
		super.onPause();
		finish();
	}

	private void setBrightness(float b) {
		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
	    layoutParams.screenBrightness = b;
	    getWindow().setAttributes(layoutParams);
	}

	@Override
	public void onColorChanged(int color, View v) {
		pref.edit().putInt(COLOR_PREF, color).commit();
		root.setBackgroundColor(ParseUtil.removeAlphaFromColor(color));

		float brightness = Color.alpha(color);
		if (!zeroBrightness && (brightness < 10)) {
			brightness = 10;
		}
		setBrightness(brightness * WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL / 255);
	}

	/* ******************** Torch related functionality ******************* */
	public void onLedClicked(View v) {
		if (holder.isCreating()) {
			return;
		}
		if (camera != null) {
			turnFlashOff();
			pref.edit().putBoolean(FLASH_PREF, false).commit();
			flashBtn.setImageResource(R.drawable.led_off);
		} else if (turnFlashOn()) {
			pref.edit().putBoolean(FLASH_PREF, true).commit();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (pref.getBoolean(FLASH_PREF, false)) {
			turnFlashOn();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {}

	private Camera camera;

	private void turnFlashOff() {
		if (camera != null) {
			try {
				camera.stopPreview();
			} catch (Throwable e) {
				Debug.log(e);
			}
			try {
				camera.release();
			} catch (Throwable e) {
				Debug.log(e);
			}
			camera = null;
		}
	}

	private boolean turnFlashOn() {
		int resp = turnFlashOn_wrapper();
		if (resp > 0) {
			Toast.makeText(this, resp, Toast.LENGTH_LONG).show();
		} else {
			flashBtn.setImageResource(R.drawable.led_on);
		}
		return resp == 0;
	}

	private int turnFlashOn_wrapper() {
		turnFlashOff();
		try {
			camera = Camera.open();
		} catch (RuntimeException e) {
			// Some unexpected error occurred.
			// Nothing can be done. Flash will not be supported.
			return R.string.sl_no_camera;
		}

		Parameters param = camera.getParameters();
		if (param == null) {
			return R.string.sl_no_torch;
		}
		
		List<String> modes = param.getSupportedFlashModes();
		if (modes == null || !modes.contains(Parameters.FLASH_MODE_TORCH)) {
			// Torch not supported.
			return R.string.sl_no_torch;
		}

		param.setFlashMode(Parameters.FLASH_MODE_TORCH);
		try {
			camera.setParameters(param);
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (final Throwable e) {
			Debug.log(e);
			return R.string.sl_camera_error;
		}
		
		return 0;
	}
}
