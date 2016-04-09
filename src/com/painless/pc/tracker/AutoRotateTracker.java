package com.painless.pc.tracker;

import android.content.SharedPreferences;
import android.provider.Settings;

import com.painless.pc.R;

public final class AutoRotateTracker extends AbstractSystemSettingsTracker {

	public AutoRotateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, Settings.System.ACCELEROMETER_ROTATION, R.drawable.icon_toggle_autorotate, Settings.ACTION_DISPLAY_SETTINGS);
	}
}
