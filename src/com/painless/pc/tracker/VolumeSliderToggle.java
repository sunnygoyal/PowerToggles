package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.R;
import com.painless.pc.acts.VolumeSlider;
import com.painless.pc.singleton.Globals;

public class VolumeSliderToggle extends AbstractCommand {

	public VolumeSliderToggle(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_volume_slider);
	}

	@Override
	public Intent getIntent() {
		return Globals.setIncognetoIntent(new Intent(mContext, VolumeSlider.class));
	}
}
