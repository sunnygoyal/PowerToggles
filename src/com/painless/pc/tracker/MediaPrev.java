package com.painless.pc.tracker;

import android.content.SharedPreferences;
import android.view.KeyEvent;

import com.painless.pc.R;

public final class MediaPrev extends MediaButton {

	public MediaPrev(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_media_prev, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
	}

}
