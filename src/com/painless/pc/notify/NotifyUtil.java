package com.painless.pc.notify;

import java.util.Calendar;

import android.content.Context;
import android.content.res.Resources;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.util.WidgetSetting;

public class NotifyUtil {

	public static int getDrawable(int iconId) {
		if (iconId == 3) {
			int res = Resources.getSystem().getIdentifier("stat_sys_adb", "drawable", "android");
			if (res != 0) {
				return res;
			}
		}
		return new int[] {
				R.drawable.notify_icon,
				R.drawable.notify_icon_trans,
				R.drawable.notify_icon_network,
				R.drawable.notify_icon,
				R.drawable.notify_icon_digits_w,
				R.drawable.notify_icon_circle_ww,
				R.drawable.notify_icon_circle_wb,
				R.drawable.notify_icon_digits_b,
				R.drawable.notify_icon_circle_bb,
				R.drawable.notify_icon_digits_w,
				R.drawable.notify_icon_cal_ww,
				R.drawable.notify_icon_cal_wb,
				R.drawable.notify_icon_digits_b,
				R.drawable.notify_icon_cal_bb
		}[iconId];
	}

	public static int getIconLevel(int iconId, Context context) {
		if (iconId == 2) {
			return Globals.getNetworkType(context);
		} else if (iconId > 8) {
			return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		} else {
			return Globals.getBattery(context);
		}
	}

	public static WidgetSetting getSetting(Context context) {
		if (!NotifyStatus.isEnabled(context)) {
			return null;
		}
		return SettingStorage.getSettingForWidget(context, Globals.STATUS_BAR_WIDGET_ID);
	}

}
