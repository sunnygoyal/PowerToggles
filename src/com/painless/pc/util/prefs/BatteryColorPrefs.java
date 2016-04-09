package com.painless.pc.util.prefs;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.tracker.BatteryTracker;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class BatteryColorPrefs implements OnColorChangedListener, OnClickListener {


	public final View view;
	
	private final SharedPreferences mPrefs;
	private final CheckboxPref mEnablePref;

	private final ColorButton[] mColorButtons;

	public BatteryColorPrefs(LayoutInflater inflator, SharedPreferences prefs, CheckboxPref enablePrefs) {
		mPrefs = prefs;

		mEnablePref = enablePrefs;
		mEnablePref.view.setOnClickListener(this);

		view = inflator.inflate(R.layout.ts_battery_colors, null);
		mColorButtons = new ColorButton[3];

		int[] values = new int[3];
		ParseUtil.parseIntArray(values, prefs.getString(BatteryTracker.CUSTOM_COLOR_KEY, BatteryTracker.CUSTOM_COLOR_VALS));

		for (int i=0; i<3; i++) {
			mColorButtons[i] = (ColorButton) view.findViewById(Globals.BUTTONS[i]);
			mColorButtons[i].setColor(values[i]);
			mColorButtons[i].setOnColorChangeListener(this, true);
		}
		setAllEnabled();

	}

	@Override
	public void onColorChanged(int color, View v) {
		String value = mColorButtons[0].getColor() + "," + mColorButtons[1].getColor() + "," + mColorButtons[2].getColor();
		mPrefs.edit().putString(BatteryTracker.CUSTOM_COLOR_KEY, value).commit();
	}

	private void setAllEnabled() {
		boolean enabled = mEnablePref.view.isChecked();
		for (ColorButton button : mColorButtons) {
			button.setEnabled(enabled);
		}
	}

	@Override
	public void onClick(View v) {
		mEnablePref.onClick(v);
		setAllEnabled();
	}
}
