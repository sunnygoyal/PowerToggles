package com.painless.pc.tracker;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.painless.pc.R;
import com.painless.pc.acts.BrightnessActivity;
import com.painless.pc.singleton.ParseUtil;

public class BacklightTracker extends AbstractTracker {

	@Override
	public int getDisplayNo(Context context) {
    int state = getTriState(context);
		if (auto == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
			return 1;
		}
		return (state == STATE_DISABLED) ? 0 : ((state == STATE_INTERMEDIATE) ? 2 : 3);
	}


	public static final String DEFAULT = "30,102,255";
	public static final String KEY = "backlight_level";

	public BacklightTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, new int[] {
				COLOR_DEFAULT, R.drawable.icon_toggle_bright_1,
				COLOR_ON, R.drawable.icon_toggle_bright_auto,
				COLOR_ON, R.drawable.icon_toggle_bright_2,
				COLOR_ON, R.drawable.icon_toggle_bright_3 });
	}

	public static int current = 30;

	private int[] mModes;
	private int mLastIndex;
	private boolean mAutoBrightEnabled;

	private int auto;
	private boolean quick_brightness;

	@Override
	public int getActualState(Context context) {
		try {
			auto = android.provider.Settings.System.getInt(
					context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
			current = android.provider.Settings.System.getInt(
					context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
			current = current < mModes[mLastIndex] ?
					(current <= mModes[0] ? mModes[0] : current) : mModes[mLastIndex];
		} catch (final SettingNotFoundException e) { }

		if (current == mModes[mLastIndex]) {
			return STATE_ENABLED;
		} else if (current == mModes[0]) {
			return STATE_DISABLED;
		} else {
			return STATE_INTERMEDIATE;
		}
	}

	@Override
	public void toggleState(Context context) {
	  if (AbstractSystemSettingsTracker.hasPermission(context)) {
	    requestStateChange(context, false);
	  } else {
	    AbstractSystemSettingsTracker.showPermissionDialog(context, Settings.ACTION_DISPLAY_SETTINGS);
	  }
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		int newState = mModes[0];
		if (auto == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
			auto = android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
		} else {
			auto = android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

			if (current >= mModes[mLastIndex] && mAutoBrightEnabled) {
				auto = android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
			} else {
				for (int i = mLastIndex; i >= 0; i--) {
					if (current < mModes[i]) {
						newState = mModes[i];
					}
				}
			}
		}

		android.provider.Settings.System.putInt(context.getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS, newState);
		android.provider.Settings.System.putInt(context.getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, auto);
		current = newState;
	
		BrightnessActivity.changeBrightness(context, quick_brightness);
	}

	@Override
	public void init(SharedPreferences pref) {
		mModes = ParseUtil.parseIntArray(null,  pref.getString(KEY, DEFAULT));
		Arrays.sort(mModes);
		mLastIndex = mModes.length - 1;
		mAutoBrightEnabled = pref.getBoolean("auto_bright_enabled", true);
		quick_brightness = pref.getBoolean("quick_brightness", false);
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return states[mDisplayNumber + 3];
	}
}
