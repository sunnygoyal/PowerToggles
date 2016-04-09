package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.R;

public class WidgetSettingCommand extends AbstractCommand {

	public WidgetSettingCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_prefs);
	}

	@Override
	public Intent getIntent() {
		return null;
	}
}
