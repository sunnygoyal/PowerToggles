package com.painless.pc.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.Toast;

import com.painless.pc.R;

public class SipCallTracker extends AbstractTracker {

	private static final String
			SIP_CALL_OPTIONS = "sip_call_options",
			SIP_ALWAYS = "SIP_ALWAYS",
			SIP_ADDRESS_ONLY = "SIP_ADDRESS_ONLY",
			SIP_ASK_ME_EACH_TIME = "SIP_ASK_ME_EACH_TIME";

	public SipCallTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, new int[] {
				COLOR_DEFAULT, R.drawable.icon_toggle_sip_ask,
				COLOR_ON, R.drawable.icon_toggle_sip_some,
				COLOR_ON, R.drawable.icon_toggle_sip_all });
	}

	@Override
	public int getActualState(Context context) {
		String callMode = Settings.System.getString(context.getContentResolver(), SIP_CALL_OPTIONS);
		return SIP_ASK_ME_EACH_TIME.equals(callMode) ? STATE_DISABLED :
			(SIP_ADDRESS_ONLY.equals(callMode) ? STATE_TURNING_ON : STATE_ENABLED);
	}

	@Override
	public void toggleState(Context context) {
		requestStateChange(context, false);
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		String callMode = Settings.System.getString(context.getContentResolver(), SIP_CALL_OPTIONS);

		String newValue;
		int msg;
		if (SIP_ASK_ME_EACH_TIME.equals(callMode)) {
			newValue = SIP_ALWAYS;
			msg = R.string.sip_all;
		} else if (SIP_ADDRESS_ONLY.equals(callMode)) {
			newValue = SIP_ASK_ME_EACH_TIME;
			msg = R.string.sip_ask;
		} else {
			newValue = SIP_ADDRESS_ONLY;
			msg = R.string.sip_some;
		}

		boolean changed = false;
    if (AbstractSystemSettingsTracker.hasPermission(context)) {
      try {
        Settings.System.putString(context.getContentResolver(), SIP_CALL_OPTIONS, newValue);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        changed = true;
      } catch (Exception e) { }
    }

    if (!changed) {
      AbstractSystemSettingsTracker.showPermissionDialog(context, "android.telecom.action.CHANGE_PHONE_ACCOUNTS");
    }
    setCurrentState(context, getActualState(context));
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return states[14 + mDisplayNumber];
	}
}
