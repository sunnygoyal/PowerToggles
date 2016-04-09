package com.painless.pc.notify;

import static com.painless.pc.singleton.Globals.getAppPrefs;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.painless.pc.singleton.Globals;

public class NotifyStatus {

	// ************** ENABLED *************************
	private static final String KEY_ENABLED = "status_bar_widget";

	public static boolean isEnabled(Context context) {
	  return getAppPrefs(context).getBoolean(KEY_ENABLED, false);
	}

	public static void setEnabled(Context context, boolean enabled) {
		getAppPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).commit();
		if (!enabled) {
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Globals.NOTIFICATION_ID);
		}
	}

	// ************** ICON *************************
	private static final String KEY_ICON = "notify_icon";

	public static int getIconId(Context context) {
		return getAppPrefs(context).getInt(KEY_ICON, 6);
	}

	public static void setIconId(int iconId, Context context) {
		getAppPrefs(context).edit().putInt(KEY_ICON, iconId).commit();
	}

	// ************** TWO ROW *************************
	private static final String KEY_TWO_ROW = "nofity_two_row";

	public static boolean isTwoRowEnabled(Context context) {
		return getAppPrefs(context).getBoolean(KEY_TWO_ROW, false);
	}

	public static void setTwoRowEnabled(Context context, boolean enabled) {
		getAppPrefs(context).edit().putBoolean(KEY_TWO_ROW, enabled).commit();
	}

	// ************** Auto Collapse *************************
	private static final String KEY_AUTO_COLLAPSE = "n_collapse";

	public static boolean autoCollaseNotify(Context context) {
		return getAppPrefs(context).getBoolean(KEY_AUTO_COLLAPSE, false);
	}

	public static void setAutoCollapseNotify(Context context, boolean enabled) {
		getAppPrefs(context).edit().putBoolean(KEY_AUTO_COLLAPSE, enabled).commit();
	}

	   // ************** Visibility *************************
    private static final String KEY_VISIBILITY = "notify_visibility";

    public static int notifyVisibility(Context context) {
        return getAppPrefs(context).getInt(KEY_VISIBILITY, Notification.VISIBILITY_PUBLIC);
    }

    public static void setNotifyVisibility(Context context, int visibility) {
        getAppPrefs(context).edit().putInt(KEY_VISIBILITY, visibility).commit();
    }
}
