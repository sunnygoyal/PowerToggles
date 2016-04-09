package com.painless.pc.tracker;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.widget.Toast;

import com.painless.pc.BootDialog;
import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.Thunk;

public class AdbWirelessTracker extends AbstractTracker {

	private static final String WIFI_STATE_PREF = "adb-enable-wife";

	@Thunk boolean working = false;

	public AdbWirelessTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_recovery));
	}

	@Override
	public int getActualState(Context context) {
		try {
			return working ? STATE_INTERMEDIATE :
				("5555".equals(SystemProperties.get("service.adb.tcp.port")) ? STATE_ENABLED : STATE_DISABLED);
		} catch (Throwable e) {
			Debug.log(e);
			return STATE_DISABLED;
		}
	}

	@Override
	protected void requestStateChange(final Context context, final boolean desiredState) {
		final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager == null) {
			return;
		}

		working = true;
		final UiUpdater updater = new UiUpdater(context);
		// Execute everything on a separate thread as it can take a while
		// to complete the action
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... args) {
				try {
					if (desiredState) { // Enable WiFi if required
					  boolean shouldTurnOffWifiLater = false;

					  if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
					    int hotSpotState = HotSpotTracker.getFiveState(wifiManager);
					    if ((hotSpotState != STATE_ENABLED) && (hotSpotState != STATE_TURNING_ON)) {
					      shouldTurnOffWifiLater = true;
		            wifiManager.setWifiEnabled(true);
					    }
					  }
					  getPref(context).edit().putBoolean(WIFI_STATE_PREF, shouldTurnOffWifiLater).commit();

					} else if (getPref(context).getBoolean(WIFI_STATE_PREF, false)) {
						wifiManager.setWifiEnabled(false);
					}

					String propCommand = "setprop service.adb.tcp.port " + (desiredState ? "5555" : "-1");

					boolean suCheck = RootTools.runSuCommand(propCommand);
					if (!suCheck) {
						return false;
					}

					RootTools.runSuCommand("stop adbd");
					return RootTools.runSuCommand("start adbd");
				} catch (final Exception e) {
				}
				return null;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				working = false;
				updater.refresh();
				if (!result) {
					Intent prompt = new Intent(context, BootDialog.class);
					prompt.putExtra("info", true);
					Globals.startIntent(context, prompt);
				} else if (desiredState) {
					Toast.makeText(context, "adb Started at " + getWifiIpAddress() + ":5555", Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	/**
	 * Code for getting IP address copied from:
	 * http://stackoverflow.com/questions/9573196
	 */
	@Thunk static String getWifiIpAddress() {
	  try {
	    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
	            .hasMoreElements();) {
	      NetworkInterface intf = en.nextElement();
	      if (intf.getName().contains("wlan")) {
	        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
	                .hasMoreElements();) {
	          InetAddress inetAddress = enumIpAddr.nextElement();
	          if (!inetAddress.isLoopbackAddress()
	                  && (inetAddress.getAddress().length == 4)) {
	            return inetAddress.getHostAddress();
	          }
	        }
	      }
	    }
	  } catch (Exception e) {
	    Debug.log(e);
	  }
	  return "0.0.0.0";
	}
}
