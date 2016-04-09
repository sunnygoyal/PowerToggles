package com.painless.pc.tracker;

import android.content.SharedPreferences;

import com.painless.pc.R;

public class FontDecreaseTracker extends FontIncreaseTracker {

	public FontDecreaseTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_font_des);
	}

	@Override
	public void init(SharedPreferences pref) {
		super.init(pref);
		delta = -delta;
	}
}
