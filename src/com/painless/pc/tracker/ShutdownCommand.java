package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.BootDialog;
import com.painless.pc.R;

public class ShutdownCommand extends AbstractCommand {

	public ShutdownCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_shutdown);
	}

	@Override
	public Intent getIntent() {
		return new Intent(mContext, BootDialog.class);
	}

}
