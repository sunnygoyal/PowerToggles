package com.painless.pc.tracker;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.painless.pc.CmdUsbT;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.ReflectionUtil;

public final class UsbTetherTracker extends AbstractTracker {

  private static final String CHANGE_ACTION = "android.net.conn.TETHER_STATE_CHANGED";

  @Override
  public String getChangeAction() {
    return CHANGE_ACTION;
  }

  private String[] mUsbRegexs;

  public UsbTetherTracker(int trackerId, SharedPreferences pref) {
    super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_usb));
  }

  @Override
  public void onActualStateChange(Context context, Intent intent) {
    if (intent.getAction() == null || !intent.getAction().equals(getChangeAction())) {
      return;
    }
    setCurrentState(context, parseIntent(intent, context));
  }

  private int parseIntent(Intent intent, Context context) {
    if (mUsbRegexs == null) {
      // try to initialize usb regexes.
      final ReflectionUtil connectivity = ReflectionUtil.get(context);
      mUsbRegexs = (String[]) connectivity.invokeGetter("getTetherableUsbRegexs");
    }
    
    if (mUsbRegexs == null || mUsbRegexs.length == 0) {
      return STATE_DISABLED;
    }

    for (String s : intent.getStringArrayListExtra("activeArray")) {
      for (String regex : mUsbRegexs) {
        if (s.matches(regex)) {
          return STATE_ENABLED;
        }
      }
    }
    return STATE_DISABLED;
  }

  @Override
  public int getActualState(Context context) {
    Intent intent = context.getApplicationContext().registerReceiver(null, new IntentFilter(CHANGE_ACTION));
    return (intent == null) ? STATE_UNKNOWN : parseIntent(intent, context);
  }

  @Override
  protected void requestStateChange(final Context context, final boolean desiredState) {
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... args) {
        return (Globals.hasPermission(context, "android.permission.MANAGE_USB") && CmdUsbT.run(desiredState))
            || (RootTools.isRooted() && RootTools.runJavaCommand(CmdUsbT.class, "usb", context, desiredState));
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (!result) {
          final Intent intent = new Intent("android.intent.action.MAIN");
          intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
          Globals.startIntent(context, intent);
          new UiUpdater(context).refresh();
        }
      }
    }.execute();
  }
}
