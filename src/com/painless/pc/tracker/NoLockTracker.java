package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.NoLockService;
import com.painless.pc.R;

public class NoLockTracker extends AbstractTracker {

	public static final String CHANGE_ACTION = "custom_no_lock";

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}

	public NoLockTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_no_lock));
	}

	@Override
	public int getActualState(Context context) {
		return NoLockService.NO_LOCK_ON ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(final Context context, boolean desiredState) {
		Intent i = new Intent(context, NoLockService.class);
		if (desiredState) {
			context.startService(i);
		} else {
			context.stopService(i);
		}
	}
}
