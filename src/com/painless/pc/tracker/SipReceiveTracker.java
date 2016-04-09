package com.painless.pc.tracker;

import android.content.SharedPreferences;

import com.painless.pc.R;

public class SipReceiveTracker extends AbstractSystemSettingsTracker {

	public SipReceiveTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, "sip_receive_calls", R.drawable.icon_toggle_sip_in, "android.telecom.action.CHANGE_PHONE_ACCOUNTS");
	}
}
