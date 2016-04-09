package com.painless.pc.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import com.painless.pc.R;
import com.painless.pc.notify.NotifyStatus;

public class TwoRowTracker extends AbstractTracker {

	public TwoRowTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_two_row));
	}

	@Override
	public int getActualState(Context context) {
		return NotifyStatus.isTwoRowEnabled(context) ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		NotifyStatus.setTwoRowEnabled(context, desiredState);
		setCurrentState(context, desiredState ? STATE_ENABLED : STATE_DISABLED);
	}
}
