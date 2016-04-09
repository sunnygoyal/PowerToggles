package com.painless.pc.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import com.painless.pc.R;
import com.painless.pc.notify.NotifyStatus;

public class NotifyWidgetTracker extends AbstractTracker {

	public NotifyWidgetTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_notify));
	}

	@Override
	public int getActualState(Context context) {
		return NotifyStatus.isEnabled(context) ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		NotifyStatus.setEnabled(context, desiredState);
		setCurrentState(context, desiredState ? STATE_ENABLED : STATE_DISABLED);
	}
}
