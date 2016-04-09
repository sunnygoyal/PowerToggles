package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.BootDialog;
import com.painless.pc.R;

public class ShutdownMenuCommand extends AbstractCommand {

	public ShutdownMenuCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_shutdown_menu);
	}

	@Override
	public Intent getIntent() {
		return new Intent(mContext, BootDialog.class).putExtra("menu", true);
	}
}
