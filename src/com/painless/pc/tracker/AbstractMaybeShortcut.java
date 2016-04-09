package com.painless.pc.tracker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.singleton.Globals;

abstract class AbstractMaybeShortcut extends AbstractTracker {

	private final String pref_id;
	private final String intent_action;

	private boolean isShortcut = false;
	private boolean isCurrentlyShortcut = false;

	AbstractMaybeShortcut(int trackerId, SharedPreferences pref, String pref_id, String intent_action, int[] buttonConfig) {
		super(trackerId, pref, buttonConfig);
		this.pref_id = pref_id;
		this.intent_action = intent_action;
		init(pref);
	}

	@Override
	public void toggleState(Context context) {
		if (isCurrentlyShortcut != isShortcut) {
			isShortcut = isCurrentlyShortcut;
			setCurrentState(context, getActualState(context));
		}
		if (isShortcut) {
			final Intent intent = new Intent("android.intent.action.MAIN");
			intent.setComponent(new ComponentName("com.android.settings", intent_action));
			Globals.startIntent(context, intent);
		} else {
			super.toggleState(context);
		}
	}

	@Override
	public void init(SharedPreferences pref) {
		if (pref_id == null) {
			return;
		}
		isCurrentlyShortcut = pref.getBoolean(pref_id, isShortcut);
	}

	@Override
	public boolean shouldProxy(Context context) {
		return isCurrentlyShortcut;
	}
}
