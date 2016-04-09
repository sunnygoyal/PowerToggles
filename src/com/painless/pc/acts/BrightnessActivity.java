package com.painless.pc.acts;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.tracker.BacklightTracker;

public class BrightnessActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = BacklightTracker.current * 1.0f / 255;
		getWindow().setAttributes(lp);

		// wait for some time before finishing
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				this.cancel();
				moveTaskToBack(true);
				finish();
			}
		}, 500, 5000);
	}

	public static void changeBrightness(Context c, boolean quick) {
		if (quick) {
			WindowManager.LayoutParams params = new WindowManager.LayoutParams(0, 0, 2005, 8, -3);
			params.screenBrightness = BacklightTracker.current * 1.0f / 255;
			final WindowManager wm = (WindowManager) c.getSystemService(WINDOW_SERVICE);
			final View v = new View(c);
			wm.addView(v, params);
			
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					try {
						Thread.sleep(500);
					} catch (Throwable e) {
						Debug.log(e);
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					try {
						wm.removeView(v);
					} catch (Throwable e) {
						Debug.log(e);
					}
				}
			}.execute();
		} else {
			final Intent i = new Intent(c, BrightnessActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			c.startActivity(Globals.setIncognetoIntent(i));
		}
	}
}
