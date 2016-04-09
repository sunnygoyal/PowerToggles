package com.painless.pc.cfg.section;

import static com.painless.pc.util.SettingsDecoder.KEY_COLORS;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class ButtonColorsSection extends ConfigSection implements OnColorChangedListener {

	private final ColorButton[] mButtons = new ColorButton[3];

	public ButtonColorsSection(Activity activity, ConfigCallback callback) {
		super(activity, callback, R.id.hdr_button_colors, R.id.cnt_button_colors);

		for (int i = 0; i < 3; i++) {
			mButtons[i] = (ColorButton) mContainerView.findViewById(BUTTON_TRIPLET[i]);
			mButtons[i].setOnColorChangeListener(this, true);
			mButtons[i].setTag(i);
		}
	}

	@Override
	public void readSetting(WidgetSetting setting, ConfigExtraData extraData) {
		super.readSetting(setting, extraData);

		for (int i = 0; i < 3; i++) {
			mButtons[i].setColor(ParseUtil.addAlphaToColor(setting.buttonAlphas[i], setting.buttonColors[i]));
		}
	}

	@Override
	public void onColorChanged(int color, View v) {
		int i = (Integer) v.getTag();
		mSetting.buttonAlphas[i] = Color.alpha(color);
		mSetting.buttonColors[i] = ParseUtil.removeAlphaFromColor(color);
		mCallback.onConfigSectionUpdate();
	}

	@Override
	public void writeSettings(JSONObject json) throws JSONException {
    for (int i=0; i<3; i++) {
      json.put(KEY_COLORS[i], mButtons[i].getColor());
    }
	}

	@Override
	public void writeTheme(JSONObject json) throws JSONException {
	  writeSettings(json);
	}

	@Override
	public void readTheme(SettingsDecoder decoder, Bitmap icon) {
    for (int i=0; i<3; i++) {
      int color = decoder.getValue(KEY_COLORS[i], mButtons[i].getColor());
      mButtons[i].setColor(color);
      mSetting.buttonAlphas[i] = Color.alpha(color);
      mSetting.buttonColors[i] = ParseUtil.removeAlphaFromColor(color);
    }
	}
}
