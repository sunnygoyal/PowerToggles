package com.painless.pc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.PluginDB;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;

public class PluginUpdateReceiver extends BroadcastReceiver {

	private static final String TASK_COMPLETE_INTENT = "net.dinglisch.android.tasker.ACTION_TASK_COMPLETE";
	private static final String FIRE_SETTING_INTENT = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";

	@Override
	public void onReceive(Context context, Intent intent) {
		String varId;
		boolean newState;
		if (intent.getBooleanExtra("refresh", false)) {
		  PCWidgetActivity.partialUpdateAllWidgets(context);
		  return;
		} if (TASK_COMPLETE_INTENT.equals(intent.getAction())) {
			varId = Globals.TASKER_KEY_PREFIX + intent.getData().getSchemeSpecificPart();
			newState = PluginDB.get(context).getState(varId);
		} else if (FIRE_SETTING_INTENT.equals(intent.getAction())) {
			varId = Globals.TASKER_KEY_PREFIX + intent.getStringExtra("varID");
			newState = Boolean.parseBoolean(intent.getStringExtra("state"));
		} else {
			varId = intent.getStringExtra("varID");
			newState = intent.getBooleanExtra("state", false);
			String pluginID = intent.getStringExtra(Intent.EXTRA_UID);
			if (!TextUtils.isEmpty(varId) && !TextUtils.isEmpty(pluginID)) {
			  varId = varId + '-' + pluginID;
			}
		}
		if ((varId != null) && PluginDB.get(context).isActive(varId)) {
			AbstractTracker tracker = SettingStorage.getTracker("pl_" + varId, context, Globals.getAppPrefs(context));
			if (tracker instanceof PluginTracker) {
			  int count = PluginDB.get(context).getCount(varId);
			  try {
			    count = Integer.parseInt(intent.getStringExtra("count")); 
			  } catch (Exception e) {
			    // Ignore
			  }
				PluginTracker plugin = (PluginTracker) tracker;
				plugin.setChangedState(context, newState, count);

				PluginDB.get(context).setState(varId, newState, count);
				PCWidgetActivity.partialUpdateAllWidgets(context);
			}
		}
	}
}
