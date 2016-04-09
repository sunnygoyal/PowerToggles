package com.painless.pc.tracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;

import com.painless.pc.CmdFont;
import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.Thunk;

public class FontIncreaseTracker extends AbstractTracker implements Runnable {

	private Handler handler = null;
	@Thunk AsyncTask<Void, Void, Void> task;
	@Thunk Context mContext;

	protected float delta;
  private long doubleClickGap;

	public FontIncreaseTracker(int trackerId, SharedPreferences pref) {
		this(trackerId, pref, R.drawable.icon_toggle_font_inc);
	}

	FontIncreaseTracker(int trackerId, SharedPreferences pref, int imgId) {
		super(trackerId, pref, getTriImageConfig(imgId));
	}

	@Override
	public void init(SharedPreferences pref) {
		delta = pref.getInt("font_delta_step", 15) / 100F;
    doubleClickGap = pref.getInt("tap_speed", AbstractDoubleClickTracker.DEFAULT_DOUBLE_CLICK_GAP);
	}

	@Override
	public int getActualState(Context context) {
		return (task != null) ? STATE_ENABLED :
			(handler != null ? STATE_INTERMEDIATE : STATE_DISABLED);
	}

	@Override
	public final void toggleState(Context context) {
		if (task != null) {
			return;
		}
		if (handler == null) {
			handler = new Handler();
			mContext = context;
			handler.postDelayed(this, doubleClickGap);
		} else {
			handler.removeCallbacks(this);
			handler = null;
			setFontSize(0);
		}
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		// Never Called
	}
	
	@Override
	public void run() {
		handler = null;
		setFontSize(delta);
		PCWidgetActivity.partialUpdateAllWidgets(mContext);
	}

	private void setFontSize(final float delta) {
		task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
			  if (!(Globals.hasPermission(mContext, Manifest.permission.CHANGE_CONFIGURATION) && CmdFont.run(delta)) && RootTools.isRooted()) {
          RootTools.runJavaCommand(CmdFont.class, "font", mContext, delta);
			  }
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				task = null;
				PCWidgetActivity.partialUpdateAllWidgets(mContext);
				mContext = null;
			}
		}.execute();
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return getLabel(labelArray);
	}
}
