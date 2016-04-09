package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.RemoteViews;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.singleton.Globals;
import com.painless.pc.theme.BitmapListProvider;
import com.painless.pc.theme.FixedImageProvider;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.util.WidgetSetting;

public abstract class AbstractTracker {

	// This widget keeps track of two sets of states:
	// "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
	// "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
	static final int STATE_DISABLED = 0;
	static final int STATE_ENABLED = 1;
	static final int STATE_TURNING_ON = 2;
	static final int STATE_TURNING_OFF = 3;
	static final int STATE_UNKNOWN = 4;
	static final int STATE_INTERMEDIATE = 5;

	public static final int COLOR_DEFAULT = 0;
	public static final int COLOR_WORKING = 1;
	public static final int COLOR_ON = 2;

	// Is the state in the process of changing?
	private boolean mInTransition = false;
	private Boolean mActualState = null;  // initially not set
	private Boolean mIntendedState = null;  // initially not set

	// Did a toggle request arrive while a state update was
	// already in-flight?  If so, the mIntendedState needs to be
	// requested when the other one is done, unless we happened to
	// arrive at that state already.
	private boolean mDeferredStateChangeRequestNeeded = false;

	// The position in the tracker list, used for reverse mapping.
	public final int trackerId;
	public final int[] buttonConfig;

	AbstractTracker(int trackerId, SharedPreferences pref, int[] buttonConfig) {
		this.trackerId = trackerId;
		this.buttonConfig = buttonConfig;
		init(pref);
	}

	public void init(SharedPreferences pref) {
	}

	/**
	 * User pressed a button to change the state.  Something
	 * should immediately appear to the user afterwards, even if
	 * we effectively do nothing.  Their press must be heard.
	 */
	public void toggleState(Context context) {
		final int currentState = getTriState(context);
		boolean newState = false;
		switch (currentState) {
			case STATE_ENABLED:
				newState = false;
				break;
			case STATE_DISABLED:
				newState = true;
				break;
			case STATE_INTERMEDIATE:
				if (mIntendedState != null) {
					newState = !mIntendedState;
				}
				break;
		}
		mIntendedState = newState;
		if (mInTransition) {
			// We don't send off a transition request if we're
			// already transitioning.  Makes our state tracking
			// easier, and is probably nicer on lower levels.
			// (even though they should be able to take it...)
			mDeferredStateChangeRequestNeeded = true;
		} else {
			mInTransition = true;
			requestStateChange(context, newState);
		}
	}

    protected int mDisplayNumber;
	/**
	 * Updates the remote views depending on the state (off, on,
	 * turning off, turning on) of the setting.
	 * @param buttonId The button in the widget to update
	 * @return the color of the indicator
	 */
	public int setImageViewResources(Context context, RemoteViews views,
			int buttonId, WidgetSetting setting, ToggleBitmapProvider imageProvider) {
		final int colorId = getStateColor(context);

		Bitmap img = imageProvider==null ? null : imageProvider.getIcon(mDisplayNumber);
		if (img == null) {
			views.setImageViewResource(buttonId, buttonConfig[2* mDisplayNumber + 1]);
		} else {
			views.setImageViewBitmap(buttonId, img);
		}
		boolean useColor = img==null || buttonConfig.length > 2;
		views.setInt(buttonId, "setAlpha", useColor ? setting.buttonAlphas[colorId] : 255);
		views.setInt(buttonId, "setColorFilter", useColor ? setting.buttonColors[colorId] : 0);
		return colorId;
	}

	public final int getStateColor(Context context) {
	   mDisplayNumber = getDisplayNo(context);
	   return buttonConfig[2* mDisplayNumber];
	}

	public int getImageNumber(Context context) {
	  return mDisplayNumber;
	}

	/**
	 * Update internal state from a broadcast state change.
	 */
	public void onActualStateChange(Context context, Intent intent) {
		if (intent.getAction() == null || !intent.getAction().equals(getChangeAction())) {
			return;
		}
		setCurrentState(context, getActualState(context));
	}

	/**
	 * Sets the value that we're now in.  To be called from onActualStateChange.
	 *
	 * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
	 *                 STATE_TURNING_OFF, STATE_UNKNOWN
	 */
	protected final void setCurrentState(Context context, int newState) {
		final boolean wasInTransition = mInTransition;
		switch (newState) {
			case STATE_DISABLED:
				mInTransition = false;
				mActualState = false;
				break;
			case STATE_ENABLED:
				mInTransition = false;
				mActualState = true;
				break;
			case STATE_TURNING_ON:
				mInTransition = true;
				mActualState = false;
				break;
			case STATE_TURNING_OFF:
				mInTransition = true;
				mActualState = true;
				break;
		}

		if (wasInTransition && !mInTransition) {
			if (mDeferredStateChangeRequestNeeded) {
				//Debug.log("Processing deferred state change");
				if (mActualState != null && mIntendedState != null &&
						mIntendedState.equals(mActualState)) {
				//	Debug.log("... but intended state matches, so no changes.");
				} else if (mIntendedState != null) {
					mInTransition = true;
					requestStateChange(context, mIntendedState);
				}
				mDeferredStateChangeRequestNeeded = false;
			}
		}
	}

	/**
	 * Returns simplified 3-state value from underlying 5-state.
	 *
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
	 */
	public int getTriState(Context context) {
		if (mInTransition) {
			// If we know we just got a toggle request recently
			// (which set mInTransition), don't even ask the
			// underlying interface for its state.  We know we're
			// changing.  This avoids blocking the UI thread
			// during UI refresh post-toggle if the underlying
			// service state accessor has coarse locking on its
			// state (to be fixed separately).
			return STATE_INTERMEDIATE;
		}
		switch (getActualState(context)) {
			case STATE_DISABLED:
				return STATE_DISABLED;
			case STATE_ENABLED:
				return STATE_ENABLED;
			default:
				return STATE_INTERMEDIATE;
		}
	}

	/**
	 * Gets underlying actual state.
	 *
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
	 *         or or STATE_UNKNOWN.
	 */
	public abstract int getActualState(Context context);

	/**
	 * Actually make the desired change to the underlying radio
	 * API.
	 */
	protected abstract void requestStateChange(Context context, boolean desiredState);

	/**
	 * Returns the image number to display based on the state.
	 */
	public int getDisplayNo(Context context) {
	  int state = getTriState(context);
		return (state == STATE_DISABLED) ? 0 : ((state == STATE_INTERMEDIATE) ? 1 : 2);
	}

	public String getChangeAction() {
		return null;
	}

	public boolean shouldProxy(Context context) {
		return false;
	}

	static int[] getTriImageConfig(int imgId) {
		return new int[] {COLOR_DEFAULT, imgId, COLOR_WORKING, imgId, COLOR_ON, imgId};
	}
	static int[] getBiImageConfig(int imgId) {
		return new int[] {COLOR_DEFAULT, imgId, COLOR_ON, imgId};
	}

	public ToggleBitmapProvider getImageProvider(Context context, Bitmap icon) {
		return icon == null ? null :
			(icon.getWidth() != icon.getHeight() ?
					new BitmapListProvider(icon) : new FixedImageProvider(icon));
	}

	public String getStateText(int state, String[] states, String[] labelArray) {
		return states[state];
	}

	// **************** these functions are only overridden by the AppShourcut *************
	public String getLabel(String[] labelArray) {
		return labelArray[trackerId];
	}

	public String getId() {
		return String.valueOf(trackerId);
	}

	protected final SharedPreferences getPref(Context context) {
		return Globals.getAppPrefs(context);
	}

	protected class UiUpdater implements Runnable {

		public final Handler handler;
		private final Context context;

		public UiUpdater(Context context) {
			handler = new Handler();
			this.context = context;
		}

		public void refresh() {
			handler.post(this);
		}

		@Override
		public void run() {
			setCurrentState(context, getActualState(context));
			PCWidgetActivity.partialUpdateAllWidgets(context);
		}
	}
}
