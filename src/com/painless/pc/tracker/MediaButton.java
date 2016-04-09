package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;

public abstract class MediaButton extends AbstractCommand {

  public static final String KEY_PLAYER_INTENT = "media_player_intent";

	private final int keyCode;

	MediaButton(int trackerId, SharedPreferences pref, int imgId, int keyCode) {
		super(trackerId, pref, imgId);
		this.keyCode = keyCode;
	}

	@Override
	public void toggleState(Context context) {
	  String player = Globals.getAppPrefs(context).getString(KEY_PLAYER_INTENT, "");
	  try {
      Intent mediaIntent = TextUtils.isEmpty(player) ? new Intent(Intent.ACTION_MEDIA_BUTTON) : Intent.parseUri(player, 0);
      long eventtime = SystemClock.uptimeMillis();

      // Down intent
      context.sendOrderedBroadcast(new Intent(mediaIntent).putExtra(Intent.EXTRA_KEY_EVENT,
              new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, keyCode, 0)), null);

      // Up intent
      context.sendOrderedBroadcast(new Intent(mediaIntent).putExtra(Intent.EXTRA_KEY_EVENT,
              new KeyEvent(eventtime + 2, eventtime + 2, KeyEvent.ACTION_UP, keyCode, 0)), null);
    } catch (Exception e) {
      Debug.log(e);
    }
	}

	@Override
	public Intent getIntent() {
		return null;
	}

	@Override
	public boolean shouldProxy(Context context) {
		return false;
	}
}
