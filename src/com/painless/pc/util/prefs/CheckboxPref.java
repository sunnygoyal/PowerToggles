package com.painless.pc.util.prefs;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;

import com.painless.pc.R;

public class CheckboxPref implements OnClickListener {

	public final CheckedTextView view;
	
	private final SharedPreferences mPrefs;
	private final String mKey;
	
	public CheckboxPref(LayoutInflater inflator, String key, SharedPreferences prefs, int textId) {
		this(inflator, key, prefs, textId, false);
	}

	public CheckboxPref(LayoutInflater inflator, String key, SharedPreferences prefs, String title) {
		this(inflator, key, prefs, R.string.act_add, false);
		view.setText(title);
	}

	public CheckboxPref(LayoutInflater inflator, String key, SharedPreferences prefs, int textId, boolean defaultValue) {
		mPrefs = prefs;
		view = (CheckedTextView) inflator.inflate(R.layout.ts_pref_checkbox, null);
		view.setChecked(prefs.getBoolean(key, defaultValue));
		view.setText(textId);
		view.setOnClickListener(this);
		
		mKey = key;
	}

	@Override
	public void onClick(View v) {
		view.setChecked(!view.isChecked());
		mPrefs.edit().putBoolean(mKey, view.isChecked()).commit();
	}
}
