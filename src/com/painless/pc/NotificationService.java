package com.painless.pc;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;

import com.painless.pc.notify.NotifyStatus;

public class NotificationService extends AccessibilityService implements Runnable {

	private final Handler handler = new Handler();
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			return;
		}
		CharSequence packageName = event.getPackageName();
	    if (getPackageName().equals(packageName)) {
	      return;
	    }
		if (!NotifyStatus.isEnabled(this)) {
			return;
		}
		handler.removeCallbacks(this);
		handler.postDelayed(this, 3000);	// Update widget in 3 seconds.
	}

	@Override
	public void onInterrupt() {
	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo localAccessibilityServiceInfo = new AccessibilityServiceInfo();
		localAccessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		localAccessibilityServiceInfo.packageNames = null;
		localAccessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC;
		localAccessibilityServiceInfo.notificationTimeout = 0L;
		setServiceInfo(localAccessibilityServiceInfo);
	}

	@Override
	public void run() {
		PCWidgetActivity.updateStatusbarWidget(getApplicationContext());
	}
}
