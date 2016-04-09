package com.painless.pc.tracker;

import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.FlashActivity;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;

public class ScreenLightCommand extends AbstractCommand {

	public ScreenLightCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_screen_light);
	}

	@Override
	public Intent getIntent() {
		return Globals.setIncognetoIntent(new Intent(mContext, FlashActivity.class));
	}
}
