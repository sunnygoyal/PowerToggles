package com.painless.pc.singleton;

import java.util.ArrayList;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.painless.pc.tracker.PluginTracker;

public class PluginDB {

	private static final String PREF_NAME = "plugin_prefs";
	private static final String KEY_LABEL = "lbl_";
	private static final String KEY_INTENT = "int_";
  private static final String KEY_STATE = "st_";
  private static final String KEY_COUNT = "count_";

	private final SharedPreferences mPrefs;

	private PluginDB(Context c) {
		mPrefs = c.getSharedPreferences(PREF_NAME, 0);
	}

	public PluginTracker getPlugin(String varId) throws Exception {
		return new PluginTracker(varId,
				Intent.parseUri(mPrefs.getString(KEY_INTENT + varId, ""), Intent.URI_INTENT_SCHEME),
				mPrefs.getString(KEY_LABEL + varId, "Plugin"));
	}

	public void save(PluginTracker tracker) {
		String varId = tracker.getId().substring(3);
		mPrefs.edit()
			.putString(KEY_LABEL + varId, tracker.getLabel(null))
			.putString(KEY_INTENT + varId, tracker.getIntent().toUri(Intent.URI_INTENT_SCHEME)).commit();
	}

	public boolean getState(String varId) {
		return mPrefs.getBoolean(KEY_STATE + varId, false);
	}

	public int getCount(String varId) {
	  return mPrefs.getInt(KEY_COUNT + varId, 0);
	}

	public void setState(String varId, boolean state, int count) {
		mPrefs.edit().putBoolean(KEY_STATE + varId, state).putInt(KEY_COUNT + varId, count).commit();
	}

	public boolean isActive(String varId) {
		return mPrefs.contains(KEY_INTENT + varId);
	}

	public void removeOldInfo(Set<String> activePluginIDs) {
		ArrayList<String> keysToRemove = new ArrayList<String>();
		for (String key : mPrefs.getAll().keySet()) {
			if (!key.contains("_") || !activePluginIDs.contains(key.split("_", 2)[1])) {
				keysToRemove.add(key);
			}
		}
		if (keysToRemove.size() > 0) {
			Editor edit = mPrefs.edit();
			Debug.log("Removing plugin states");
			for (String key : keysToRemove) {
				edit.remove(key);
				Debug.log(key);
			}
			edit.commit();
		}
	}

	private static PluginDB DB_;

	public static PluginDB get(Context c) {
		if (DB_ == null) {
			DB_ = new PluginDB(c);
		}
		return DB_;
	}
}
