package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ServiceManager;

import com.android.internal.statusbar.IStatusBarService;
import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;

public class RecentAppsCommand extends AbstractCommand {

	public RecentAppsCommand(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_recent);
	}

	@Override
	public void toggleState(Context context) {
		Globals.collapseStatusBar(context);
		try{
		  IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar")).toggleRecentApps();
		} catch (final Throwable e) {
			Debug.log(e);
		}
	}

	@Override
	public Intent getIntent() {
		return null;
	}

}
