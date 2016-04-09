package com.painless.pc;

import static com.painless.pc.singleton.Globals.NOTIFICATION_PRIORITY;

import java.io.FileOutputStream;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.painless.pc.notify.NotifyStatus;
import com.painless.pc.notify.NotifyUtil;
import com.painless.pc.qs.QTStorage;
import com.painless.pc.singleton.BackupUtil;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.util.WidgetSetting;

public class PCWidgetActivity extends AppWidgetProvider {

  public static final String BATTERY_POLL_ACTION = "com.painless.pc.BATTERY_POLL";

  private static final String BUZZPIA_ACTION = "com.buzzpia.aqua.appwidget.";
  private static final String EXTRA_VERSION = "EXTRA_VERSION";
  private static final int RESULT_CONFIG_COMPLETE = 100;
  private static final int RESULT_CONFIG_NEEDED = 200;
  private static final int RESULT_SUCCESS = 300;
  private static final int RESULT_FAIL = 400;

  public static Runnable sUpdateHook = null;
  public static boolean sPollBattery = false;
  private static boolean sBatteryServiceActive = true;

  @Override
  public void onEnabled(Context context) {
    final PackageManager pm = context.getPackageManager();
    pm.setComponentEnabledSetting(
        new ComponentName(context, PCWidgetActivity.class),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP);
  }

  @Override
  public void onDisabled(Context context) {
    final PackageManager pm = context.getPackageManager();
    pm.setComponentEnabledSetting(
        new ComponentName(context, PCWidgetActivity.class),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    for (final int widgetId : appWidgetIds) {
      updateWidget(appWidgetManager, context, widgetId, true);
    }
  }

  @Override
  public void onDeleted(final Context context, final int[] appWidgetIds) {
    for (final int widgetId : appWidgetIds) {
      SettingStorage.deleteWidget(widgetId);
    }
  }

  @Override
  public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
    for (int i = 0; i < oldWidgetIds.length; i++) {
      SettingStorage.changeWidgetId(context, oldWidgetIds[i], newWidgetIds[i]);
    }
  }

  /**
   * Receives and processes a button pressed intent or state change.
   *
   * @param intent  Indicates the pressed button.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    String action = intent.getAction();
    if ((action != null)  && action.startsWith(BUZZPIA_ACTION)) {
      // Buzz homepack.
      String command = action.substring(BUZZPIA_ACTION.length());
      Bundle versionExtra = new Bundle();
      versionExtra.putInt(EXTRA_VERSION, 1);
      
      // Version Command.
      if ("GET_VERSION".equals(command)) {
        setResultExtras(versionExtra);
        return;
      }

      int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
      if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        return;
      } else {
        versionExtra.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      }
      setResultExtras(versionExtra);

      Uri configFileUri = intent.getData();
      if (configFileUri == null) {
        return;
      }
      String filePath = configFileUri.getPath();
      if (filePath == null) {
        return;
      }
  
      if ("GET_CONFIG_DATA".equals(command)) {
        // Return backup
        try {
          BackupUtil.createBackup(new FileOutputStream(filePath), appWidgetId, context);
          setResultCode(RESULT_SUCCESS);
        } catch (Exception e) {
          Debug.log(e);
          setResultCode(RESULT_FAIL);
        }
      } else if ("SET_CONFIG_DATA".equals(command)) {
        // Restore backup
        setResultCode(BackupUtil.importBackup(filePath, context, appWidgetId) ? RESULT_CONFIG_COMPLETE : RESULT_CONFIG_NEEDED);
      }
      return;
    }

    // State changes fall through
    updateAllWidgets(context, true);
  }

  /**
   * Updates all visible widgets partially.
   */
  public static final void partialUpdateAllWidgets(Context context) {
    updateAllWidgets(context, false);
  }

  /**
   * Updates a single widget from scratch.
   */
  public static final void fullUpdateSingleWidgets(Context context, int widgetId) {
    if ((widgetId == Globals.STATUS_BAR_WIDGET_ID) || (widgetId == Globals.STATUS_BAR_WIDGET_ID_2)) {
      sPollBattery |= updateStatusbarWidget(context);
    } else {
      sPollBattery |= updateWidget(AppWidgetManager.getInstance(context), context, widgetId, true);
    }
    setBatteryAlarm(context);
  }

  /**
   * Updates all visible widgets.
   */
  public static final void updateAllWidgets(Context context, boolean updateFull) {
    updateFull = updateFull || !GlobalFlags.partialUpdate(context);
    final AppWidgetManager awm = AppWidgetManager.getInstance(context);
    sPollBattery = false;
    for (int widgetId : awm.getAppWidgetIds(new ComponentName(context, PCWidgetActivity.class))) {
      sPollBattery |= updateWidget(awm, context, widgetId, updateFull);
    }
    sPollBattery |= updateStatusbarWidget(context);
    setBatteryAlarm(context);

    QTStorage.updateAllWidgets(context, updateFull);
    if (sUpdateHook != null) {
      sUpdateHook.run();
    }
  }

  protected static void setBatteryAlarm(Context context) {
    if (sPollBattery) {
      // setup alarm callback. The battery will be refreshed every 5 minutes,
      // as long as there is a battery widget.
      int pollTime = Globals.getAppPrefs(context).getInt("battery_poll_inv", 0);

      if (pollTime <= 0) {
        context.startService(new Intent(context, BatteryService.class));
        sBatteryServiceActive = true;
        return;
      }

      final Calendar cal = Calendar.getInstance();
      cal.add(Calendar.MINUTE, pollTime);
      Globals.setAlarm(context, cal, new Intent(context, CommandReceiver.class).setAction(BATTERY_POLL_ACTION));
    }

    // Stop service
    if (sBatteryServiceActive) {     
      context.stopService(new Intent(context, BatteryService.class));
      sBatteryServiceActive = false;
    }
  }

  /**
   * Updates the widget with the provided id using the settings from the SettingsStorage
   * for that widget.
   * In this process it updates the images on all the buttons or hides them if they are disabled
   * and sets a pending intend for the onClick event
   */
  private static final boolean updateWidget(AppWidgetManager appWidgetManager, Context context, int widgetId, boolean updateFull) {
    final WidgetSetting settings = SettingStorage.getSettingForWidget(context, widgetId);
    if (updateFull) {
      appWidgetManager.updateAppWidget(widgetId, getRemoteView(context, settings, false, widgetId));
    } else {
      appWidgetManager.partiallyUpdateAppWidget(widgetId, settings.rvFactory.getFastRemoteView(context, settings, false));
    }

    return settings.batteryEnabled;
  }

  public static final RemoteViews getRemoteView(Context context, WidgetSetting settings,
      boolean isNotification, int widgetID) {

    return settings.rvFactory.getRemoteView(context, settings, isNotification, true, widgetID);
  }

  // ++++++++++++++ StatusBar Widget ++++++++++++++++++++++
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  static final boolean updateStatusbarWidget(Context context) {
    final WidgetSetting setting = NotifyUtil.getSetting(context);
    if (setting == null) {
      // Hide the notification
      return false;
    }

    final Notification notification = new Notification();

    int icon = NotifyStatus.getIconId(context);

    notification.icon = NotifyUtil.getDrawable(icon);
    notification.iconLevel = NotifyUtil.getIconLevel(icon, context);
    notification.when = System.currentTimeMillis();

    notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
    notification.tickerText = "";

    RemoteViews nwViews = getRemoteView(context, setting, true,  Globals.STATUS_BAR_WIDGET_ID);
    final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nw_collapsed);
    views.removeAllViews(R.id.big_notify_wrapper_1);
    views.addView(R.id.big_notify_wrapper_1, nwViews.clone());
    notification.contentView = views;

    // Visibility
    if (Globals.IS_LOLLIPOP) {
      notification.visibility = NotifyStatus.notifyVisibility(context);
    }

    boolean rowTowBattery = updateSecondRow(notification, context, nwViews);
    final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(Globals.NOTIFICATION_ID, notification);

    if (icon > 8) {
      // Set a timer for calendar event.
      final Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 1);
      cal.add(Calendar.DAY_OF_MONTH, 1);
      Globals.setAlarm(context, cal, new Intent(context, CommandReceiver.class));
    }

    return setting.batteryEnabled | rowTowBattery || (icon > 3 && icon < 9);	// Battery icon range
  }

  private static boolean updateSecondRow(Notification notification, Context context, RemoteViews nwViews) {
    int priority = Globals.getAppPrefs(context).getInt(NOTIFICATION_PRIORITY, 5);
    notification.priority = priority - 3;

    if (notification.priority == 2) {
      notification.when = -Long.MAX_VALUE;
    }

    if (!NotifyStatus.isTwoRowEnabled(context)) {
      return false;
    }
    final WidgetSetting settings = SettingStorage.getSettingForWidget(context, Globals.STATUS_BAR_WIDGET_ID_2);

    final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nw_expanded);
    views.removeAllViews(R.id.big_notify_wrapper_1);
    views.addView(R.id.big_notify_wrapper_1, nwViews);
    views.removeAllViews(R.id.big_notify_wrapper_2);
    views.addView(R.id.big_notify_wrapper_2, getRemoteView(context, settings, true,  Globals.STATUS_BAR_WIDGET_ID));

    /**
    Parcel p = Parcel.obtain();
    views.clone().writeToParcel(p, 0);
    Debug.log("Size " + p.dataSize());
    p.recycle();
     **/
    
    notification.bigContentView = views;
    return settings.batteryEnabled;
  }
}
