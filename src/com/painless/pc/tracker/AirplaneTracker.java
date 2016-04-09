package com.painless.pc.tracker;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;

public final class AirplaneTracker extends AbstractTracker {

  @Override
  public String getChangeAction() {
    return Intent.ACTION_AIRPLANE_MODE_CHANGED;
  }

  public AirplaneTracker(int trackerId, SharedPreferences pref) {
    super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_airplane));
  }

  @Override
  public int getActualState(Context context) {
    final ContentResolver cr = context.getContentResolver();
    return isEnabled(cr) ? STATE_ENABLED : STATE_DISABLED;
  }

  @Override
  protected void requestStateChange(final Context context, final boolean desiredState) {
    ContentResolver resolver = context.getContentResolver();
    setValue(resolver, desiredState, context);
    boolean updateCompleted = false;
    if (isEnabled(resolver) == desiredState) {
      // Setting updated. Try to send the broadcast.
      try {
        final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", desiredState);
        context.sendBroadcast(intent);
        updateCompleted = true;
      } catch (Exception e) { }
    }

    if (!updateCompleted) {
      new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(Void... params) {
          // Try to use root
          if (RootTools.hasSettingBin()) {
            String command = String.format(
                    "settings put global airplane_mode_on %d; am broadcast -a android.intent.action.AIRPLANE_MODE --ez state %b",
                    desiredState ? 1 : 0, desiredState);
            return RootTools.runSuCommand(command);
          } else {
            return false;
          }
        } 

        @Override
        protected void onPostExecute(Boolean result) {
          if (!result) {
            // Root tools failed, open the airplane settings
            new UiUpdater(context).refresh();
            Globals.startIntent(context, new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
          }
        }
      }.execute();
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private void setValue(ContentResolver cr, boolean on, Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      Settings.System.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, on ? 1 : 0);			
    }
  }

  @Override
  public boolean shouldProxy(Context context) {
    return true;
  }

  private static boolean isEnabled(final ContentResolver resolver) {
    return Settings.System.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
  }
}
