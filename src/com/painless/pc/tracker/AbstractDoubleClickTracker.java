package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.singleton.Globals;

public abstract class AbstractDoubleClickTracker extends AbstractTracker implements Runnable {

  public static final int DEFAULT_DOUBLE_CLICK_GAP = 250;

	private boolean doubleClickEnabled;
	private long doubleClickGap;
	private Handler handler = null;
	private Context mContext;

	AbstractDoubleClickTracker(int trackerId, SharedPreferences pref, int[] buttonConfig) {
		super(trackerId, pref, buttonConfig);
	}

	@Override
	public void init(SharedPreferences pref) {
		doubleClickEnabled = pref.getBoolean("doubleClick-" + trackerId, false);
		doubleClickGap = pref.getInt("tap_speed", DEFAULT_DOUBLE_CLICK_GAP);
	}

	@Override
	public final void toggleState(Context context) {
		if (!doubleClickEnabled) {
			super.toggleState(context);
		} else if (handler == null) {
			mContext = context;
			handler = new Handler();
			handler.postDelayed(this, doubleClickGap);
		} else {
			mContext = null;
			handler.removeCallbacks(this);
			handler = null;
			Globals.startIntent(context, getDCIntent(context));
		}
	}

	@Override
	public final void run() {
		handler = null;
		super.toggleState(mContext);
		PCWidgetActivity.partialUpdateAllWidgets(mContext);
		mContext = null;
	}

	abstract Intent getDCIntent(Context context);
}
