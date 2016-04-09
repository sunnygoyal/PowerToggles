package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.R;
import com.painless.pc.ScreenOnService;

public final class ScreenOnTracker extends AbstractTracker {
	
	public static final String CHANGE_ACTION = "custom_screenOn";

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}

	public ScreenOnTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_screen_on));
	}

	@Override
	public int getActualState(Context context) {
		return ScreenOnService.SCREEN_ON ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		Intent i = new Intent(context, ScreenOnService.class);
		if (desiredState) {
			context.startService(i);
		} else {
			context.stopService(i);
		}
	}
}
