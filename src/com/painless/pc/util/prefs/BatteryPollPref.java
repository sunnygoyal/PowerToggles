package com.painless.pc.util.prefs;

import java.util.Arrays;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;

import com.painless.pc.R;

public class BatteryPollPref extends AbstractPopupPref {

	private final int[] mValues = new int[] {0, 1, 2, 3, 5, 10, 30 };
	public BatteryPollPref(LayoutInflater inflator, SharedPreferences prefs) {
		super(inflator, prefs, R.string.as_poll_interval, android.R.layout.simple_list_item_1);
	}

	@Override
	public AlertDialog showBuilder(Builder builder) {
		int value = mPrefs.getInt("battery_poll_inv", 0);

		return builder.setPositiveButton(null, null)
				.setSingleChoiceItems(R.array.as_battery_poll_entries,
						Arrays.binarySearch(mValues, value),
						this)
						.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which > -1) {
			mPrefs.edit().putInt("battery_poll_inv", mValues[which]).commit();
			dialog.dismiss();
		}
	}
}
