package com.painless.pc.cfg;

import java.io.File;
import java.io.OutputStream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.painless.pc.R;
import com.painless.pc.picker.IconPicker;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Globals;
import com.painless.pc.theme.BatteryImageProvider;
import com.painless.pc.util.ImportExportActivity;
import com.painless.pc.view.RectView;
import com.painless.pc.view.RectView.RectListener;

public class BatteryIconEditor extends ImportExportActivity<Bitmap>
		implements IconPicker.Callback, OnSeekBarChangeListener, RectListener {

	public static final String IMAGE = "image";

	public BatteryIconEditor() {
		super(R.menu.bid_menu, ".bt.png",
				R.string.bid_export, R.array.bid_export_msg,
				R.string.bid_import, R.array.bid_import_msg);
	}

	private IconPicker iconPicker;
	private BatteryImageProvider imageProvider;

	private RectView rectView;
	private ImageView imgPreview;
	private SeekBar seekPreview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addActionDoneButton();

		setResult(RESULT_CANCELED);
		setContentView(R.layout.cfg_battery_icon_editor);

		Bitmap img = getIntent().getParcelableExtra(IMAGE);
		if (img == null) {
			img = BitmapFactory.decodeResource(getResources(), R.drawable.icon_toggle_battery);
		}

		iconPicker = new IconPicker(this);

		imgPreview = (ImageView) findViewById(R.id.img_preview);
		imgPreview.setColorFilter(Color.WHITE);

		seekPreview = (SeekBar) findViewById(R.id.seek_preview);
		seekPreview.setProgress(Globals.getBattery(this) - 1);
		seekPreview.setOnSeekBarChangeListener(this);

    rectView = (RectView) findViewById(R.id.img_digit_pos);
		rectView.setListener(this);
		rectView.setBackTint(0xFFF7A736);
    onPostImport(img);
	}

	public void onChangeBackClicked(View v) {
		iconPicker.pickIcon(this, Color.WHITE);
	}

	@Override
	public void onIconReceived(Bitmap icon) {
		rectView.setBitmap(icon);
		imageProvider = new BatteryImageProvider(icon, this);
		imageProvider.loadDimFromColor(getColorSettings());
		onProgressChanged(seekPreview, seekPreview.getProgress(), false);
	}

	@Override
	public void doExport(OutputStream out) throws Exception {
		Bitmap image = BitmapUtils.createBatteryBitmapFormat(rectView.getBitmap(), getColorSettings(), this);
		image.compress(Bitmap.CompressFormat.PNG, 100, out);
		out.close();
	}

	@Override
	public Bitmap doImportInBackground(File importFile) throws Exception {
		Bitmap image = BitmapFactory.decodeFile(importFile.getAbsolutePath());
		image = BitmapUtils.resizeToIconSize(image, this, false);
		return (image.getWidth() > 0 && image.getHeight() > 0) ? image : null;
	}

	@Override
	public void onPostImport(Bitmap result) {
		imageProvider = new BatteryImageProvider(result, this);
		rectView.setBitmap(Bitmap.createBitmap(result, 0, 0, result.getHeight(), result.getHeight()));
		rectView.updateRect(imageProvider.getDigitRect(), imageProvider.getSize(), imageProvider.getSize());
		onProgressChanged(seekPreview, seekPreview.getProgress(), false);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		Bitmap img = imageProvider.getIcon(progress + 1);
		imgPreview.setImageBitmap(img);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onRectChange(float[] rect, View v) {
    imageProvider.loadDimFromColor(getColorSettings());
    onProgressChanged(seekPreview, seekPreview.getProgress(), false);
	}

	@Override
	public void onDoneClicked() {
		Intent result = new Intent();
		result.putExtra(IMAGE,
				BitmapUtils.createBatteryBitmapFormat(rectView.getBitmap(), getColorSettings(), this));
		setResult(RESULT_OK, result);
		finish();
	}

	private int getColorSettings() {
	  float[] rect = rectView.getAppliedRect();
	  int color = 0;
	  for (int i=0; i<4; i++) {
	    color = color << 8;
	    color += (int) (rect[3-i] * 255);
	  }
	  return color;
	}
}
