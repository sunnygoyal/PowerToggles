package com.painless.pc.util.prefs;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.tracker.BacklightTracker;
import com.painless.pc.util.PathDrawable;
import com.painless.pc.util.SeekNumberPicker;

public class BrightModeListPref implements OnClickListener, DialogInterface.OnClickListener {

  private static final String PATH_CROSS = "M33,12.2 L24,21.2 15,12.2 12.2,15 21.2,24 12.2,33 15,35.8 24,26.8 33,35.8 35.8,33 26.8,24 35.8,15 33,12.2z";

	public final View view;

	private final SharedPreferences mPrefs;
	private final LayoutInflater mInflator;
	private final Context mContext;

	private final LinearLayout mList;

	private SeekNumberPicker mNumberPicker;
	private int mLastValueAdded = 102;

	public BrightModeListPref(LayoutInflater inflator, SharedPreferences prefs) {
		mInflator = inflator;
		mPrefs = prefs;
		mContext = inflator.getContext();

		view = mInflator.inflate(R.layout.ts_pref_bright_modes, null);
		view.findViewById(R.id.add).setOnClickListener(this);

		mList = (LinearLayout) view.findViewById(android.R.id.list);
		mList.addView(new CheckboxPref(inflator, "auto_bright_enabled", prefs, R.string.ts_auto, true).view);

		// Initialize UI
		String prefList = mPrefs.getString(BacklightTracker.KEY, BacklightTracker.DEFAULT); 
		int[] values = ParseUtil.parseIntArray(null, prefList);
		Arrays.sort(values);

		for (int value : values) {
			mList.addView(getNewListItem(value));
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.add) {
			showAddDialog();

		} else if (mList.getChildCount() < 4) {
			// only 2 entries and a auto tab present. So removal is disabled.
			Toast.makeText(mContext, R.string.ts_msg_min_two_levels, Toast.LENGTH_LONG).show();

		} else {
			mList.removeView(v);
			updateValueInPrefs();
		}
	}

	private void showAddDialog() {
		mNumberPicker = new SeekNumberPicker(mContext);
		mNumberPicker.setSummary(R.string.ts_bright_summary);
		mNumberPicker.setMax(255);
		mNumberPicker.setValue(mLastValueAdded);

		new AlertDialog.Builder(mContext)
			.setTitle(R.string.lbl_brightness)
			.setView(mNumberPicker.getView())
			.setNegativeButton(R.string.act_cancel, null)
			.setPositiveButton(R.string.act_add, this)
			.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();

		int valueToAdd = mNumberPicker.getValue();
		int indexToAdd = mList.getChildCount();

		// Reverse iterate to find the new position to add. Exclude 0th position.
		for (int i = indexToAdd - 1; i > 0; i--) {
			if (valueToAdd < (Integer) mList.getChildAt(i).getTag()) {
				indexToAdd = i;
			}
		}
		mList.addView(getNewListItem(valueToAdd), indexToAdd);
		updateValueInPrefs();
		mLastValueAdded = valueToAdd;
	}

	private void updateValueInPrefs() {
		ArrayList<Integer> values = new ArrayList<Integer>();
		for (int i = 1; i<mList.getChildCount(); i++) {
			values.add((Integer) mList.getChildAt(i).getTag());
		}
		String newValue = TextUtils.join(",", values);
		mPrefs.edit().putString(BacklightTracker.KEY, newValue).commit();
	}

	private TextView getNewListItem(int value) {
		CheckedTextView tv = (CheckedTextView) mInflator.inflate(R.layout.ts_pref_checkbox, null);
		tv.setCheckMarkDrawable(new PathDrawable(PATH_CROSS, mContext, 15));
		tv.setBackgroundResource(R.drawable.bg_top_gray_divider);
		tv.setOnClickListener(this);

		int percent = value * 100 / 255;
		tv.setText(String.format("%d (%d%% of 255)", value, percent));
		tv.setTag(value);
		return tv;
	}
}
