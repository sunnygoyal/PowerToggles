package com.painless.pc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.painless.pc.notify.NotifyStatus;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.tracker.AbstractTracker;

public class CommandReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (action != null) {
      if (Globals.CUSTOM_ACTION.equals(action)) {
        action = intent.getExtras().getString("action_type");
        intent.setAction(action);
      } else if (PCWidgetActivity.BATTERY_POLL_ACTION.equals(action)) {
        // Check for battery change.
        int lastBattery = Globals.sLastBattery;
        int battery = Globals.getBattery(context);
        if (lastBattery == battery) {
          PCWidgetActivity.setBatteryAlarm(context);
          return;
        }
      } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
        Debug.log("Updating all widgets");
        PCWidgetActivity.updateAllWidgets(context, true);
        return;
      }
    }

    final AbstractTracker tracker = SettingStorage.actionToTracker.get(action);
    if (tracker != null) {
      tracker.onActualStateChange(context, intent);
    } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
      // Show feedback.
      if (GlobalFlags.hapticFeedback(context)) {
        GlobalFlags.playHaptic(context);
      }
      if (NotifyStatus.autoCollaseNotify(context)) {
        Globals.collapseStatusBar(context);
      }

      final Uri data = intent.getData();
      SharedPreferences pref = Globals.getAppPrefs(context);
      AbstractTracker clickedTracker = SettingStorage.getTracker(data.getQuery(), context, pref);

      if (clickedTracker.trackerId > -1) {
        pref.edit().putLong("last_used_" + clickedTracker.trackerId, System.currentTimeMillis()).commit();
      }

      if (clickedTracker.trackerId == 33) { // Widget settings
        try {
          int widgetId = Integer.parseInt(data.getFragment());
          Globals.showWidgetConfig(widgetId, context, true);
        } catch (Exception e) {
          Debug.log(e);
        }
      } else {
        clickedTracker.toggleState(context);
      }
    } else {
      // Do nothing
    }
    PCWidgetActivity.partialUpdateAllWidgets(context);

    if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
      SettingStorage.updateConnectivityReceiver(context);
    }
  }
}
