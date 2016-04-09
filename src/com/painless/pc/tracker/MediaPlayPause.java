package com.painless.pc.tracker;

import android.content.SharedPreferences;
import android.view.KeyEvent;

import com.painless.pc.R;

public final class MediaPlayPause extends MediaButton {

	public MediaPlayPause(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_media_play, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
	}

}
