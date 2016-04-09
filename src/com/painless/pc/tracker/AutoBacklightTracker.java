package com.painless.pc.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.painless.pc.R;
import com.painless.pc.acts.BrightnessActivity;

public final class AutoBacklightTracker extends AbstractTracker {
	private boolean quick_brightness;

	public AutoBacklightTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_bright_auto));
	}

	@Override
	public int getActualState(Context context) {
		try {
			return
			(android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ==
				android.provider.Settings.System.getInt(
						context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE)) ?
								STATE_ENABLED : STATE_DISABLED;
		} catch (final SettingNotFoundException e) {
			return STATE_DISABLED;
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
		try {
			final int newState =
				(android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ==
					android.provider.Settings.System.getInt(
							context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE)) ?
									android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL :
										android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
			android.provider.Settings.System.putInt(context.getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, newState);

			BrightnessActivity.changeBrightness(context, quick_brightness);
		} catch (final SettingNotFoundException e) { }
	}

	@Override
	public void init(SharedPreferences pref) {
		quick_brightness = pref.getBoolean("quick_brightness", false);
	}
}
