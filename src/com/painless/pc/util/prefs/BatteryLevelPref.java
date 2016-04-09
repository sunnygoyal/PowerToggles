package com.painless.pc.util.prefs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;

import com.painless.pc.R;
import com.painless.pc.nav.SettingsFrag;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.util.SeekNumberPicker;

public class BatteryLevelPref extends AbstractPopupPref {

	private SeekNumberPicker mPicker1, mPicker2;

	public BatteryLevelPref(LayoutInflater inflator, SharedPreferences prefs) {
		super(inflator, prefs, R.string.ts_levels, android.R.layout.simple_list_item_1);
	}

	@Override
	public AlertDialog showBuilder(Builder builder) {
		int[] levels = ParseUtil.parseIntArray(null, mPrefs.getString("battery_levels", "20,60"));

		mPicker1 = new SeekNumberPicker(getContext());
		mPicker1.setSummary(R.string.ts_battery_low);
		mPicker1.setMax(100);
		mPicker1.setValue(levels[0]);
		
		mPicker2 = new SeekNumberPicker(getContext());
		mPicker2.setSummary(R.string.ts_battery_high);
		mPicker2.setMax(100);
		mPicker2.setValue(levels[1]);

		return builder.setView(SettingsFrag.getWrapper(getContext(), mPicker1.getView(), mPicker2.getView())).show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		int v1 = mPicker1.getValue();
		int v2 = mPicker2.getValue();
		if (v1 > v2) {
			v2 = v1;
			v1 = mPicker2.getValue();
		}
		// TODO remove ,100
		mPrefs.edit().putString("battery_levels", v1 + "," + v2 + ",100").commit();
	}
}
