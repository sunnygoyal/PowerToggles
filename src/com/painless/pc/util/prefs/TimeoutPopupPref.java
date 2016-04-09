package com.painless.pc.util.prefs;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.GridView;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.tracker.TimeoutTracker;

public class TimeoutPopupPref extends AbstractPopupPref implements OnItemClickListener {

	private final int[] mValues = new int[] {
			15, 30, 45, 60, 120, 180, 240, 300, 480, 600, 720, 900
	};
	private final boolean[] mCheckedItems = new boolean[mValues.length];
	private int mCheckedCount = 0;


	public TimeoutPopupPref(LayoutInflater inflator, SharedPreferences prefs) {
		super(inflator, prefs, R.string.ts_time_intervals, R.layout.timeout_pref_item);
	}

	@Override
	public AlertDialog showBuilder(Builder builder) {
		clear();
		mCheckedCount = 0;

		int[] currentValues = ParseUtil.parseIntArray(
				null, mPrefs.getString(TimeoutTracker.KEY, TimeoutTracker.DEFAULT));

		for (int i = 0; i < mValues.length; i++) {
			int value = mValues[i];
			add(value >= 60 ?  (value / 60 + " min") : (value + " sec"));
			mCheckedItems[i] = Arrays.binarySearch(currentValues, value) > -1;
			mCheckedCount += mCheckedItems[i] ? 1 : 0;
		}

		GridView grid = (GridView) mInflator.inflate(R.layout.timeout_pref_layout, null);
		grid.setAdapter(this);
		grid.setOnItemClickListener(this);

		return builder.setView(grid).show();
	}

	/**
	 * Called on OK
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		ArrayList<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < mValues.length; i++) {
			if (mCheckedItems[i]) {
				values.add(mValues[i]);
			}
		}
		if (values.size() < 2) {
			// Unexpected error
			return;
		}

		mPrefs.edit().putString(TimeoutTracker.KEY, TextUtils.join(",", values)).commit();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mCheckedItems[position] = !mCheckedItems[position];
		mCheckedCount += mCheckedItems[position] ? 1 : -1;

		mOKButton.setEnabled(mCheckedCount > 1);

		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CheckedTextView v = (CheckedTextView) super.getView(position, convertView, parent);
		v.setChecked(mCheckedItems[position]);
		return v;
	}
}
