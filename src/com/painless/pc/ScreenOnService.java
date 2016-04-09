package com.painless.pc;

import android.content.Context;
import android.os.PowerManager;

import com.painless.pc.singleton.Globals;
import com.painless.pc.tracker.ScreenOnTracker;

public class ScreenOnService  extends PriorityService {

  public ScreenOnService() {
    super(103, ScreenOnTracker.CHANGE_ACTION);
  }

  public static boolean SCREEN_ON = false;

  private PowerManager.WakeLock lock;

  @Override
  public void onCreate() {
    super.onCreate();

    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    lock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "PT");
    lock.acquire();

    SCREEN_ON = true;
    broadcastState();

    maybeShowNotification("wake_lock_notify_hidden", true, R.drawable.icon_toggle_screen_on, R.string.wake_lock_active,
            R.string.click_to_deactive, Globals.getAppPrefs(this).getBoolean("wake_lock", false));
  }

  @Override
  public void onDestroy() {
    lock.release();
    SCREEN_ON = false;
    clearNotification();
    broadcastState();
    super.onDestroy();
  }
}
