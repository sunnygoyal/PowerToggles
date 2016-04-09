package com.painless.pc.tracker;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;

public class BluetoothDiscoveryTracker extends AbstractTracker {

	public BluetoothDiscoveryTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_bluetooth_discovery));
	}

	@Override
	public int getActualState(Context context) {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			return STATE_UNKNOWN;
		}
		return (adapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) ?
				STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	public void toggleState(Context context) {
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
		Globals.startIntent(context, discoverableIntent);
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) { }

	@Override
	public boolean shouldProxy(Context context) {
		return true;
	}
}
