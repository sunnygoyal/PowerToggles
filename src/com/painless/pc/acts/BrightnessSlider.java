package com.painless.pc.acts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.tracker.BacklightTracker;
import com.painless.pc.view.MultiSeek;

public class BrightnessSlider extends AbstractPopup implements OnClickListener {

	public BrightnessSlider() {
		super(R.layout.brightness_slider);
	}

	private ImageButton btnAuto;
	private MultiSeek seekBrightness;

	private int mBrightnessMin;

	@Override
	void initUi() {
		SharedPreferences pref = Globals.getAppPrefs(this);

		boolean isAuto = IsAuto();
		btnAuto = (ImageButton) findViewById(R.id.btn_auto);
		btnAuto.setImageResource(isAuto ? R.drawable.icon_toggle_bright_auto : R.drawable.icon_toggle_bright_3);

		mBrightnessMin = pref.getBoolean("bright_slider_zero", false) ? 0 : 10;

		seekBrightness = (MultiSeek) findViewById(R.id.seek_brightness);
		seekBrightness.setMax(255 - mBrightnessMin);
		seekBrightness.setProgress(getCurrentBrightnessProgress());
		seekBrightness.setOnSeekBarChangeListener(this);
		seekBrightness.changeLook(isAuto);

		if (pref.getBoolean("bright_slider_preset", false)) {
			ViewGroup parent = (ViewGroup) findViewById(R.id.bright_preset);
			parent.setVisibility(View.VISIBLE);

			int[] levels = ParseUtil.parseIntArray(null, pref.getString(BacklightTracker.KEY, BacklightTracker.DEFAULT));
			for (int i=levels.length - 1; i>=0; i--) {
				ImageView btn = (ImageView) getLayoutInflater().inflate(R.layout.brightness_slider_preset_btn, parent, false);
				btn.setImageResource((i == levels.length - 1) ? R.drawable.icon_toggle_bright_3 :
					((i == 0) ? R.drawable.icon_toggle_bright_1 :
						R.drawable.icon_toggle_bright_2));
				parent.addView(btn, 0);
				btn.setTag(levels[i]);
				btn.setOnClickListener(this);
			}
		}
	}

	public void onAutoClicked(View v) {
		int value, drawable, brightnessValue;
		if (IsAuto()) {
			value = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
			drawable = R.drawable.icon_toggle_bright_3;
			brightnessValue = seekBrightness.getProgress() + mBrightnessMin;
			seekBrightness.changeLook(false);
		} else {
			value = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
			drawable = R.drawable.icon_toggle_bright_auto;
			brightnessValue = 30;
			seekBrightness.changeLook(true);
		}
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, value);
		btnAuto.setImageResource(drawable);
		updateBrightness(brightnessValue);
	}

	public void onSettingsClicked(View v) {
		Globals.startIntent(this, new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));
		finish();
	}

	/**
	 * Called when a preset button is clicked
	 */
	@Override
	public void onClick(View v) {
		int bright = (Integer) v.getTag();
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS, bright);
		updateBrightness(bright);
		finish();
	}

	void updateUi() {
		btnAuto.setImageResource(IsAuto() ? R.drawable.icon_toggle_bright_auto : R.drawable.icon_toggle_bright_1);
		seekBrightness.setProgress(getCurrentBrightnessProgress());
	}

	private boolean IsAuto() {
		return getInt(Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
				== Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
	}

	private int getCurrentBrightnessProgress() {
		int brightness = getInt(Settings.System.SCREEN_BRIGHTNESS, 10) - mBrightnessMin;
		return Math.max(brightness, 0);
	}
	private int getInt(String key, int defaultV) {
		try {
			return android.provider.Settings.System.getInt(getContentResolver(), key);
		} catch (final SettingNotFoundException e) {
			return defaultV;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		if (IsAuto()) {
			onAutoClicked(null);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS, seekBrightness.getProgress() + mBrightnessMin);
		updateBrightness(seekBrightness.getProgress() + mBrightnessMin);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Integer val = null;
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

			val = Math.max(0, seekBrightness.getProgress() - 10);
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			val = Math.min(seekBrightness.getMax(), seekBrightness.getProgress() + 10);
		}

		if (val != null) {
			if (IsAuto()) {
				onAutoClicked(null);
			}
			seekBrightness.setProgress(val);
			onStopTrackingTouch(seekBrightness);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void updateBrightness(int value) {
		final WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = value * 1.0f / 255;
		getWindow().setAttributes(lp);
	}
}
