package com.painless.pc.tracker;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.painless.pc.R;
import com.painless.pc.util.ReflectionUtil;
import com.painless.pc.util.Thunk;

public class WiMaxTracker extends AbstractMaybeShortcut {

	private static final String CHANGE_ACTION = "android.net.wimax.WIMAX_STATE_CHANGE";

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}





	private static final int RAW_WIMAX_STATE_DISABLING = 0;
	private static final int RAW_WIMAX_STATE_DISABLED = 1;
	private static final int RAW_WIMAX_STATE_ENABLING = 2;
	private static final int RAW_WIMAX_STATE_ENABLED = 3;

	public WiMaxTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, "shrt_4g", "com.android.settings.RadioInfo", getTriImageConfig(R.drawable.icon_toggle_gprs_4g));
	}

	// It is used to determine if a changed request has completed or not.
	@Thunk Timer timer = new Timer();


	@Override
	public void onActualStateChange(Context context, Intent intent) {
		timer.cancel();
		timer.purge();
		setCurrentState(context, getActualState(context));
	}

	@Override
	public int getActualState(Context context) {
		try {
			final ReflectionUtil connectivity = getMyConnectivity(context);
			if (connectivity.isNull()) {
				return STATE_UNKNOWN;
			}

			final Integer state = getState(connectivity);
			if (state == null) {
				return STATE_UNKNOWN;
			} else {
				switch (state) {
					case RAW_WIMAX_STATE_DISABLING:
						return STATE_TURNING_OFF;
					case RAW_WIMAX_STATE_DISABLED:
						return STATE_DISABLED;
					case RAW_WIMAX_STATE_ENABLING:
						return STATE_TURNING_ON;
					case RAW_WIMAX_STATE_ENABLED:
						return STATE_ENABLED;
					default:
						return STATE_UNKNOWN;
				}
			}
		} catch (Exception e) {
			return STATE_UNKNOWN;
		}
	}

	@Thunk Integer getState(ReflectionUtil connectivity) {
		try {
			final Integer state = (Integer) connectivity.invokeGetter("getWimaxState");
			return state != null ? state : (Integer) connectivity.invokeGetter("getWimaxStatus");
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	protected void requestStateChange(final Context context, final boolean desiredState) {
		final ReflectionUtil connectivity = getMyConnectivity(context);
		if (connectivity.isNull()) {
			return;
		}

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				connectivity.invokeSetter("setWimaxEnabled", Boolean.TYPE, desiredState);
				return null;
			}
		}.execute();

		timer.cancel();
		timer.purge();
		timer = new Timer();
		final int desiredWimaxState = desiredState ? RAW_WIMAX_STATE_ENABLED : RAW_WIMAX_STATE_DISABLED;
		final UiUpdater updater = new UiUpdater(context);
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Integer state = getState(connectivity);
				if (state == null) {
					state = -1;
				}
				if (state == desiredWimaxState) {
					timer.cancel();
					timer.purge();
					updater.refresh();
				}
			}
		}, 500, 500);
	}

	private ReflectionUtil getMyConnectivity(Context context) {
		return ReflectionUtil.get(context, "wimax", "WiMax");
	}
}
