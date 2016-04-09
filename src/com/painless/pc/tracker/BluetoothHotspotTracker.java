package com.painless.pc.tracker;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.R;
import com.painless.pc.util.ReflectionUtil;
import com.painless.pc.util.Thunk;

public class BluetoothHotspotTracker extends AbstractTracker {

	private static final String BLUEOTTH_STATE_PREF = "hotspot-enable-bluetooth";

	@Thunk ReflectionUtil bluetoothPan;
	@Thunk BluetoothAdapter adapter;
	@Thunk Timer checkTimer = new Timer();

	public BluetoothHotspotTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_bluetooth_tether));
	}

	@Override
	public int getActualState(Context context) {
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			return STATE_UNKNOWN;
		}
		if (!adapter.isEnabled()) {
			return STATE_DISABLED;
		}
		
		new PanInitializer(context, new Refresher(context));
		if (bluetoothPan == null || bluetoothPan.isNull()) {
			return STATE_UNKNOWN;
		}
		Boolean result = (Boolean) bluetoothPan.invokeGetter("isTetheringOn");
		return result == null ? STATE_UNKNOWN : (result ? STATE_ENABLED : STATE_DISABLED);
	}

	@TargetApi(11)
	@Override
	protected void requestStateChange(final Context context, final boolean desiredState) {
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			return;
		}
		
    if (!AbstractSystemSettingsTracker.hasPermission(context)) {
      // Make sure we have the permission
      setCurrentState(context, getActualState(context));

      AbstractSystemSettingsTracker.showPermissionDialog(context,
          new Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"));
      return;
    }

		final SharedPreferences pref = getPref(context);
		if (desiredState) {
			boolean bluetoothEnabled = adapter.isEnabled();
			pref.edit().putBoolean(BLUEOTTH_STATE_PREF, bluetoothEnabled).commit();
			if (!bluetoothEnabled) {
				adapter.enable();
			}
			waitForAdaptertoEnable(context);

		} else {
			PanEnabler disabler = new PanEnabler(false, null, new Runnable() {
				
				@Override
				public void run() {
					if (!pref.getBoolean(BLUEOTTH_STATE_PREF, false)) {
						adapter.disable();
					}
					setCurrentState(context, STATE_DISABLED);
				}
			});
			new PanInitializer(context, disabler);
			disabler.run();
		}
	}

	private void waitForAdaptertoEnable(final Context context) {
		checkTimer.cancel();
		checkTimer.purge();

		final PanEnabler enabler = new PanEnabler(true, context, null);

		checkTimer = new Timer();
		checkTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				if (adapter.isEnabled()) {					
					checkTimer.cancel();
					checkTimer.purge();
					
					//Debug.log("Adapter on, Calling init");
					new PanInitializer(context, enabler);
					enabler.run();
				}
			}
		}, 100, 500);
	}

	private class PanEnabler implements Runnable {

		private final boolean mState;
		final UiUpdater mUpdater;
		final Runnable mMore;

		PanEnabler(boolean state, Context context, Runnable more) {
			mState = state;
			mUpdater = (context == null) ? null : new UiUpdater(context);
			this.mMore = more;
		}

		@Override
		public void run() {
			if (bluetoothPan != null && !bluetoothPan.isNull()) {
				bluetoothPan.invokeSetter("setBluetoothTethering", Boolean.TYPE, mState);
				if (mUpdater != null) {
					mUpdater.refresh();
				}
				if (mMore != null) {
					mMore.run();
				}
			}
		}
	}

	private class Refresher implements Runnable {
		final UiUpdater mUpdater;

		Refresher(Context context) {
			mUpdater = new UiUpdater(context);
		}

		@Override
		public void run() {
			mUpdater.refresh();
		}
	}

	private class PanInitializer implements BluetoothProfile.ServiceListener {
		
		private boolean initFlowTracker;
		private final Runnable callback;

		public PanInitializer(Context context, Runnable runnable) {
			callback = runnable;

			if (bluetoothPan != null) {
				return;
			}

			initFlowTracker = false;
			try {
				adapter.getProfileProxy(context.getApplicationContext(), this, 5);
			} catch (Exception e) {
			//	Debug.log("Failed to init bluetoothPan");
			}
			initFlowTracker = true;
		}

		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			bluetoothPan = new ReflectionUtil(proxy);
			if (initFlowTracker) {
				callback.run();
			}
		}

		@Override
		public void onServiceDisconnected(int profile) {
			bluetoothPan = null;
		}
	}
}
