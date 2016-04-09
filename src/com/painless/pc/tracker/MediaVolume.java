package com.painless.pc.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

import com.painless.pc.R;

public final class MediaVolume extends AbstractTracker {
	private static final String media_volume_key = "media_volume";

	public MediaVolume(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, new int[] {
				COLOR_DEFAULT, R.drawable.icon_toggle_media_volume_off,
				COLOR_ON, R.drawable.icon_toggle_media_volume_on
		});
	}
 
	@Override
	public void toggleState(Context context) {
		final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (am == null) {
			return;
		}
		int volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		SharedPreferences pref = getPref(context);
		if (volume > 0) {
			pref.edit().putInt(media_volume_key, volume).commit();
			am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
		} else {
			int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int oldVolume = pref.getInt(media_volume_key, maxVolume);
			if (oldVolume > maxVolume || oldVolume <= 0) {
				oldVolume = maxVolume;
			}
			am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0);
		}
	}

	@Override
	public int getActualState(Context context) {
		final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (am == null) {
			return STATE_INTERMEDIATE;
		}
		int volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		return volume > 0 ? STATE_INTERMEDIATE : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
	}
}
