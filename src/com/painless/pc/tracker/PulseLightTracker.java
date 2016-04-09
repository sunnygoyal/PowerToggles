package com.painless.pc.tracker;

import android.content.SharedPreferences;

import com.painless.pc.R;

public class PulseLightTracker extends AbstractSystemSettingsTracker {

	public PulseLightTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, "notification_light_pulse", R.drawable.icon_toggle_pulse, "android.settings.NOTIFICATION_SETTINGS");
	}
}
