package com.painless.pc.tracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class WifiOptimizeTracker extends AbstractTracker {

  private static final String KEY = "wifi_suspend_optimizations_enabled";

  public WifiOptimizeTracker(int trackerId, SharedPreferences pref) {
    super(trackerId, pref, getTriImageConfig(R.drawable.icon_wifi_opt));
  }

  @Override
  public int getActualState(Context context) {
    return (Settings.Global.getInt(context.getContentResolver(), KEY, 1) == 1) ? STATE_ENABLED : STATE_DISABLED;
  }

  @Override
  protected void requestStateChange(final Context context, final boolean desiredState) {
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        if (Globals.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
          try {
            int state = desiredState ? 1 : 0;
            Settings.Global.putInt(context.getContentResolver(), KEY, state);
            if (Settings.Global.getInt(context.getContentResolver(), KEY, 1) == state) {
              return true;
            }
          } catch (Exception e) { }
        }
        return RootTools.isRooted() &&
                RootTools.runSuCommand(
                        "settings put global wifi_suspend_optimizations_enabled " + (desiredState ? "1" : "0"));
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (!result) {
          // All methods failed. Launch the screen.
          Globals.startIntent(context, new Intent(android.provider.Settings.ACTION_WIFI_IP_SETTINGS));      
        }
        new UiUpdater(context).run();
      }
    }.execute();
  }
}