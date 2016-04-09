package com.painless.pc.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;

public final class VolumeTracker extends AbstractTracker {

	private static final boolean[] enabledStates = new boolean[4];

	private boolean ringAndVibrate = false;

	public VolumeTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref,
				new int[] {
				COLOR_DEFAULT, R.drawable.icon_toggle_volume_off,
				COLOR_ON, R.drawable.icon_toggle_volume_vib,
				COLOR_ON, R.drawable.icon_toggle_volume_on,
				COLOR_ON, R.drawable.icon_toggle_volume_on_vib});
	}

	@Override
	public void init(SharedPreferences pref) {
		if (ParseUtil.parseBoolArray(enabledStates, pref.getInt("volume_toggles", 7)) == 0) {
			enabledStates[0] = enabledStates[1] = enabledStates[2] = true;
		}
	}

	@Override
	public int getDisplayNo(Context context) {
    int state = getTriState(context);
		if (ringAndVibrate) {
			return 3;
		}
    return (state == STATE_DISABLED) ? 0 : ((state == STATE_INTERMEDIATE) ? 1 : 2);
	}

	@Override
	public int getActualState(Context context) {
		ringAndVibrate = false;
		final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (am == null) {
			return STATE_ENABLED;
		}
		final int mode = am.getRingerMode();
		if (mode == AudioManager.RINGER_MODE_SILENT) {
			return STATE_DISABLED;
		} else if (mode == AudioManager.RINGER_MODE_VIBRATE) {
			return STATE_INTERMEDIATE;
		} else {
			ringAndVibrate = isVibrateOn(context, am);
			return STATE_ENABLED;
		}
	}

	@Override
	public void toggleState(Context context) {
		requestStateChange(context, false);
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (am == null) {
			return;
		}
		int mode = am.getRingerMode();
		mode = Math.min(mode, 2);
		if (mode == 2 && isVibrateOn(context, am)) {
			mode = 3;
		}

		while(true) {
			mode = (mode + 1) & 3;
			if (enabledStates[mode]) {
				break;
			}
		}
		am.setRingerMode(Math.min(mode, 2));

		// Bug in Nexus 7. Vibrate mode silently fails
		if ((mode == 1) && (am.getRingerMode() == 0)) {
			am.setRingerMode(2);
		}

		if (enabledStates[3]) {
			setVibrate(context, am, (mode == 3) ? 1 : 0);
		}
	}

	public static boolean isVibrateOn(Context c, AudioManager am) {
    try {
      return Settings.System.getInt(c.getContentResolver(), "vibrate_when_ringing") == 1;
    } catch (SettingNotFoundException e) {
      return am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_ON;
    }
	}

	public static void setVibrate(Context c, AudioManager am, int value) {
	  try {
	    Settings.System.putInt(c.getContentResolver(), "vibrate_when_ringing", value);
	  } catch (Exception e) { }
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return states[mDisplayNumber + 10];
	}
}
