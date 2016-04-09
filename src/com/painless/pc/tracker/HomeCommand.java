package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.R;

public class HomeCommand extends AbstractCommand {

	public HomeCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_home);
	}

	@Override
	public Intent getIntent() {
		return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
	}
}
