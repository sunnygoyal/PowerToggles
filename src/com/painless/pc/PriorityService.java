package com.painless.pc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.painless.pc.singleton.Globals;

public abstract class PriorityService extends Service {

  private static final String STOP_INTENT_PREFIX = "com.painless.ps.";

  private final int mNotificationId;
  private final String mAction;

  private boolean mRegistered;

  private final BroadcastReceiver mStopReceiver = new BroadcastReceiver() {
    
    @Override
    public void onReceive(Context context, Intent intent) {
      stopSelf();
    }
  };

  PriorityService(int notificationId, String action) {
    mNotificationId = notificationId;
    mAction = action;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  protected void broadcastState() {
    Globals.sendCustomAction(this, mAction);
  }

  protected void maybeShowNotification(String key, boolean dValue, int icon, int title, int subtitle, boolean listenToDeviceLock) {
    IntentFilter stopFilter = new IntentFilter();
    if (listenToDeviceLock) {
      stopFilter.addAction(Intent.ACTION_SCREEN_OFF);
      mRegistered = true;
    }
    
    if (!Globals.getAppPrefs(this).getBoolean(key, dValue)) {
      String stopIntent = STOP_INTENT_PREFIX + key;

      Notification n = new Notification.Builder(this)
          .setSmallIcon(icon)
          .setContentTitle(getString(title))
          .setContentText(getString(subtitle))
          .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(stopIntent), 0))
          .setOngoing(true)
          .build();
      startForeground(mNotificationId, n);

      stopFilter.addAction(stopIntent);
      mRegistered = true;
    }
    if (mRegistered) {
      registerReceiver(mStopReceiver, stopFilter);
    }
  }

  protected void clearNotification() {
    if (mRegistered) {
      unregisterReceiver(mStopReceiver);
    }
    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(mNotificationId);
  }
}
