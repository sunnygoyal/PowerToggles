package com.painless.pc.tracker;

import android.content.SharedPreferences;
import android.view.KeyEvent;

import com.painless.pc.R;

public final class MediaNext extends MediaButton {

	public MediaNext(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_media_next, KeyEvent.KEYCODE_MEDIA_NEXT);
	}
}
