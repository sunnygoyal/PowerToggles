package com.painless.pc.tracker;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.util.Thunk;

public class SyncNowTracker extends AbstractTracker implements Runnable {

	@Thunk AsyncTask<Void, Void, Void> syncTask;
	@Thunk UiUpdater updater;
	@Thunk Toast infoToast;

	public SyncNowTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_sync));
	}

	@Override
	public int getActualState(Context context) {
		if (syncTask != null) {
			return STATE_TURNING_ON;
		}
		if (infoToast != null) {
			return STATE_ENABLED;
		}
		
		return STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(final Context context, boolean desiredState) {
		if (!desiredState) {
			return;
		}
		final boolean syncAll = !Globals.getAppPrefs(context).getBoolean("sync_now_idp", false);
		updater  = new UiUpdater(context);
		syncTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Account[] allAccounts = AccountManager.get(context).getAccounts();
				SyncAdapterType[] types = ContentResolver.getSyncAdapterTypes();

				for (Account account : allAccounts) {
					for (SyncAdapterType type : types) {
					    if (account.type.equals(type.accountType)) {
					        boolean doSync = (ContentResolver.getIsSyncable(account, type.authority) > 0) &&
					        		(syncAll || ContentResolver.getSyncAutomatically(account, type.authority));
					        if (doSync) {
					        	Bundle bundle = new Bundle();
					        	bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
					        	bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
					        	ContentResolver.requestSync(account, type.authority, bundle);
					        }
					    }
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				syncTask = null;
				infoToast = Toast.makeText(context, syncAll ? R.string.msg_account_synced : R.string.msg_e_account_synced, Toast.LENGTH_LONG);
				infoToast.show();
				updater.refresh();
				if (!updater.handler.postDelayed(SyncNowTracker.this, 3000)) {
					run();
				}
			}
		}.execute();
	}

	@Override
	public void run() {
		infoToast.cancel();
		infoToast = null;
		updater.refresh();
		updater = null;
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return getLabel(labelArray);
	}
}
