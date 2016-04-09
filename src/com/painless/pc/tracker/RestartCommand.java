package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.BootDialog;
import com.painless.pc.R;

public class RestartCommand extends AbstractCommand {

	public RestartCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_restart);
	}

	@Override
	public Intent getIntent() {
		return new Intent(mContext, BootDialog.class).putExtra("restart", true);
	}
}
