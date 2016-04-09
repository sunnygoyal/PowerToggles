package com.painless.pc.tracker;

import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;

import com.painless.pc.CmdNfc;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.Thunk;

public class NfcStateTracker extends AbstractTracker {

	public NfcStateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_nfc));
	}

	@Override
	public int getActualState(Context context) {
		final NfcAdapter adapter = getAdapter(context);
		final boolean enabled = adapter != null && adapter.isEnabled();
		return enabled ? STATE_ENABLED : STATE_DISABLED ;
	}

	@Override
	public void toggleState(Context context) {
		if (RootTools.isRooted()) {
			super.toggleState(context);
		} else {
			showSettings(context);	
		}
	}

	@Thunk void showSettings(Context context) {
		final Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Globals.startIntent(context, intent);	
	}

	private NfcAdapter getAdapter(Context context) {
    return NfcAdapter.getDefaultAdapter(context);
	}

	@Override
	protected void requestStateChange(final Context context, final boolean desiredState) {
		final UiUpdater updater = new UiUpdater(context);
		final NfcAdapter adapter = getAdapter(context);
		
		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... args) {
				if (adapter==null) {
					return false;
				}

				return (Globals.hasPermission(context, Manifest.permission.MODIFY_PHONE_STATE) && CmdNfc.run(desiredState))
				    || (RootTools.isRooted() && RootTools.runJavaCommand(CmdNfc.class, "nfc", context, desiredState));
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (!result) {
					showSettings(context);
					updater.refresh();
				} else {
					final Timer timer = new Timer();
					timer.scheduleAtFixedRate(
						new TimerTask() {
							
							@Override
							public void run() {
								if (adapter.isEnabled() == desiredState) {
									timer.cancel();
									timer.purge();
									updater.refresh();
								}
							}
						}, 100, 500);
				}
			}
		}.execute();
	}
}
