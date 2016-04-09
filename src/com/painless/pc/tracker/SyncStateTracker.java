package com.painless.pc.tracker;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;

import com.painless.pc.R;

public final class SyncStateTracker extends AbstractDoubleClickTracker {
	private static final String CHANGE_ACTION = "com.android.sync.SYNC_CONN_STATUS_CHANGED";

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}





	public SyncStateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_sync));
	}

	@Override
	public int getActualState(Context context) {
		return ContentResolver.getMasterSyncAutomatically() ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(final Context context, final boolean desiredState) {
		final boolean sync = ContentResolver.getMasterSyncAutomatically();
		final UiUpdater updater = new UiUpdater(context);
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... args) {
				if (desiredState) {
					// Turning sync on.
					if (!sync) {
						ContentResolver.setMasterSyncAutomatically(true);
					}
				} else if (sync) {
					// Turning sync off
					ContentResolver.setMasterSyncAutomatically(false);
				}
				updater.refresh();
				return null;
			}
		}.execute();
	}





	@Override
	Intent getDCIntent(Context context) {
		return new Intent(Settings.ACTION_SYNC_SETTINGS);
	}
}
