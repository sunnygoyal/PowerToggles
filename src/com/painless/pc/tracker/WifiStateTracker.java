package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.TextUtils;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;

public final class WifiStateTracker extends AbstractDoubleClickTracker {

  public static final String KEY_SHOW_SSID = "show_ssid"; 
	private static final String CHANGE_ACTION = WifiManager.WIFI_STATE_CHANGED_ACTION;

	private boolean mShowSSID;
	private String mSSID;

	
	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}

	public WifiStateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_wifi));
	}

	@Override
	public void init(SharedPreferences pref) {
	  super.init(pref);
	  mShowSSID = pref.getBoolean(KEY_SHOW_SSID, false);
	}

	@Override
	public int getActualState(Context context) {
		final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
			int state = wifiStateToFiveState(wifiManager.getWifiState());
			if (mShowSSID && (state == STATE_ENABLED)) {
			  mSSID = wifiManager.getConnectionInfo().getSSID();
			  if (TextUtils.isEmpty(mSSID) || mSSID.startsWith("0x")) {
			    mSSID = null;
			  } else {
			    mSSID = mSSID.replaceAll("^\"|\"$", "");
			  }
			}
			return state;
		}
		return STATE_UNKNOWN;
	}

	@Override
	protected void requestStateChange(Context context, final boolean desiredState) {
		setWifiStateAsync(context, desiredState);
	}

	@Override
	public void onActualStateChange(Context context, Intent intent) {
		if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			return;
		}
		final int wifiState = wifiStateToFiveState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1));
		setCurrentState(context, wifiState);
	}

	private static int wifiStateToFiveState(int wifiState) {
		switch (wifiState) {
			case WifiManager.WIFI_STATE_DISABLED:
				return STATE_DISABLED;
			case WifiManager.WIFI_STATE_ENABLED:
				return STATE_ENABLED;
			case WifiManager.WIFI_STATE_DISABLING:
				return STATE_TURNING_OFF;
			case WifiManager.WIFI_STATE_ENABLING:
				return STATE_TURNING_ON;
			default:
				return STATE_UNKNOWN;
		}
	}

	static void setWifiStateAsync(final Context context, final boolean desiredState) {
		final WifiManager wifiManager =
			(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager == null) {
			return;
		}

		// Actually request the wifi change and persistent
		// settings write off the UI thread, as it can take a
		// user-noticeable amount of time, especially if there's
		// disk contention.
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... args) {
				try {
					wifiManager.setWifiEnabled(desiredState);
				} catch (SecurityException e) {
					Globals.startIntent(context, new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
				}
				return null;
			}
		}.execute();
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
	  if (mShowSSID && (state == COLOR_ON) && (mSSID != null)) {
	    return mSSID;
	  }
	  return super.getStateText(state, states, labelArray);
	}

	@Override
	Intent getDCIntent(Context context) {
		return new Intent(Settings.ACTION_WIFI_SETTINGS);
	}
}
