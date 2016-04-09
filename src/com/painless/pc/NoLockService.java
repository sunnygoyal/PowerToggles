package com.painless.pc;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;

import com.painless.pc.tracker.NoLockTracker;

public class NoLockService extends PriorityService {

  public NoLockService() {
    super(101, NoLockTracker.CHANGE_ACTION);
  }

	public static boolean NO_LOCK_ON = false;

	private KeyguardLock mLock;

	@Override
	public void onCreate() {
		super.onCreate();
		KeyguardManager manager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		mLock = manager.newKeyguardLock("power toggles");
		mLock.disableKeyguard();
		NO_LOCK_ON = true;

		maybeShowNotification("no_lock_hidden", false, R.drawable.icon_toggle_no_lock, R.string.no_lock_action, R.string.click_to_reenable, false);
		broadcastState();
	}

	@Override
	public void onDestroy() {
		mLock.reenableKeyguard();
		clearNotification();
		NO_LOCK_ON = false;
		broadcastState();
		super.onDestroy();
	}
}
