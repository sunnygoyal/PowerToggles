package com.painless.pc.cfg.section;

import static com.painless.pc.util.SettingsDecoder.KEY_DIVIDER_COLOR;
import static com.painless.pc.util.SettingsDecoder.KEY_HIDE_DIVIDERS;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.View.OnClickListener;

import com.painless.pc.R;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class DividersSection extends ConfigSection implements OnColorChangedListener, OnClickListener {

	private final View mHiddenTab;
	private final View mTintTab;
	private final ColorButton mTintButton;

	private View mSelectedView;

	public DividersSection(Activity activity, ConfigCallback callback) {
		super(activity, callback, R.id.hdr_button_divs, R.id.cnt_button_divs);

		mTintButton = (ColorButton) mContainerView.findViewById(R.id.cfg_div_tint);
		mTintButton.setOnColorChangeListener(this, false);
		
		mHiddenTab =  mContainerView.findViewById(R.id.btn_1);
		mHiddenTab.setOnClickListener(this);
		mTintTab =  mContainerView.findViewById(R.id.btn_2);
		mTintTab.setOnClickListener(this);
	}

	@Override
	public void readSetting(WidgetSetting setting, ConfigExtraData extraData) {
		super.readSetting(setting, extraData);
		mTintButton.setColor(setting.dividerColor);

		selectTab(setting.hideDividers ? mHiddenTab : mTintTab);
	}

	private void selectTab(View v) {
		mHiddenTab.setSelected(false);
		mTintTab.setSelected(false);
		mTintButton.setEnabled(v == mTintTab);

		v.setSelected(true);
		mSelectedView = v;
	}

	@Override
	public void onClick(View v) {
		if ((v == mSelectedView) && (v == mTintTab)) {
			mTintButton.onClick(mTintButton);
		}

		mSetting.hideDividers = (v == mHiddenTab);
		selectTab(v);
		mCallback.onConfigSectionUpdate();
	}

	@Override
	public void onColorChanged(int color, View v) {
		mSetting.dividerColor = color;
		mCallback.onConfigSectionUpdate();
	}

	@Override
	public void writeSettings(JSONObject json) throws JSONException {
	  json
	    .put(KEY_DIVIDER_COLOR, mTintButton.getColor())
	    .put(KEY_HIDE_DIVIDERS, mHiddenTab.isSelected());
	}

	@Override
	public void writeTheme(JSONObject json) throws JSONException {
	  writeSettings(json);
	}

	@Override
	public void readTheme(SettingsDecoder decoder, Bitmap icon) {
	  mSetting.dividerColor = decoder.getValue(KEY_DIVIDER_COLOR, mTintButton.getColor());
    mTintButton.setColor(mSetting.dividerColor);

    mSetting.hideDividers = decoder.is(KEY_HIDE_DIVIDERS, true);
	  selectTab(mSetting.hideDividers  ? mHiddenTab : mTintTab);
	}
}
