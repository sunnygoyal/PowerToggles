package com.painless.pc;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.WindowManager;

import com.painless.pc.singleton.Debug;
import com.painless.pc.tracker.RotationLockTracker;

public class RLService extends PriorityService {

  public RLService() {
    super(104, null);
  }

	public static View LOCK_VIEW = null;
	private WindowManager wm;

	@Override
	public void onCreate() {
		super.onCreate();
		boolean isLandscapeDefault = RotationLockTracker.isLandScapeDefault(this);

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(0, 0, 2005, 8, -3);
		params.gravity = 48;
		params.screenOrientation = isLandscapeDefault ?
				ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

		wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		LOCK_VIEW = new View(this);
		wm.addView(LOCK_VIEW, params);

    maybeShowNotification("rotation_lock_notify_hidden", true,
            isLandscapeDefault ? R.drawable.icon_rotation_port : R.drawable.icon_rotation_land,
            R.string.rotation_locked, R.string.click_to_unlock, false);
		PCWidgetActivity.partialUpdateAllWidgets(this);
	}

	@Override
	public void onDestroy() {
		try {
			wm.removeView(LOCK_VIEW);
		} catch (Throwable e) {
			Debug.log(e);
		}
		LOCK_VIEW = null;
		clearNotification();
    PCWidgetActivity.partialUpdateAllWidgets(this);
		super.onDestroy();
	}
}
