package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.painless.pc.FlashService;
import com.painless.pc.FlashServiceM;
import com.painless.pc.R;

public final class FlashStateTracker extends AbstractTracker {

	public static final String CHANGE_ACTION = "custom_flash";

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}

	public FlashStateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_flash));
	}

	@Override
	public int getActualState(Context context) {
		return FlashService.FLASH_ON ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(final Context context, boolean desiredState) {
		Intent i = new Intent(context, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FlashServiceM.class : FlashService.class);
		if (desiredState) {
			context.startService(i);
		} else {
			context.stopService(i);
		}
	}
}
