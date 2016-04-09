package com.painless.pc.tracker;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.painless.pc.R;

public final class HotSpotTracker extends AbstractDoubleClickTracker {

	private static final String CHANGE_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	private static final String WIFI_STATE_PREF = "hotspot-enable-wifi";

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}


	public HotSpotTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_hotspot));
	}

	@Override
	public int getActualState(Context context) {
	  return getFiveState((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
	}

	@Override
	protected void requestStateChange(Context context, final boolean desiredState) {
	  if (!AbstractSystemSettingsTracker.hasPermission(context)) {
	    // Make sure we have the permission
	    setCurrentState(context, getActualState(context));
      AbstractSystemSettingsTracker.showPermissionDialog(context,
          new Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"));
	    return;
	  }

		final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager == null) {
			return;
		}
		if (desiredState) {
			putWifiState(getPref(context), wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
		}
		// Execute everything on a separate thread as it can take a while
		// to complete the action
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... args) {
				try {
					if (desiredState) { // disable WiFi in any case
						wifiManager.setWifiEnabled(false);
					}

					final Method method = wifiManager.getClass().getMethod(
							"setWifiApEnabled", WifiConfiguration.class, boolean.class);
					final WifiConfiguration config = null;
					method.invoke(wifiManager, config, desiredState);
				} catch (final Exception e) {
				}
				return null;
			}
		}.execute();
	}

	// ReEnable WiFi if required.
	@Override
	public void onActualStateChange(Context context, Intent intent) {
		if (!intent.getAction().equals(CHANGE_ACTION)) {
			return;
		}
		int state = getActualState(context);
		setCurrentState(context, state);
		if (state == STATE_DISABLED) {
			SharedPreferences pref = getPref(context);
			if (pref.getBoolean(WIFI_STATE_PREF, false)) {
				// ReEnable WiFi
				WifiStateTracker.setWifiStateAsync(context, true);
			}
			putWifiState(pref, false);
		}
	}

	private void putWifiState(SharedPreferences pref, boolean state) {
		pref.edit().putBoolean(WIFI_STATE_PREF, state).commit();
	}
	
	public static int getFiveState(WifiManager wifiManager) {
	  try {
	    final Method method = wifiManager.getClass().getMethod("getWifiApState");
	    int hotspotState = (Integer) method.invoke(wifiManager);
	    switch (hotspotState % 10) {
	      case 0:
	        return STATE_TURNING_OFF;
	      case 1:
	        return STATE_DISABLED;
	      case 2:
	        return STATE_TURNING_ON;
	      case 3:
	        return STATE_ENABLED;
	      default:
	        return STATE_UNKNOWN;
	    }
	  } catch (final Exception e) {
	    return STATE_UNKNOWN;
	  }
	}

	@Override
	Intent getDCIntent(Context context) {
		Intent intent = new Intent("android.intent.action.MAIN");
		intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
		return intent;
	}
}
