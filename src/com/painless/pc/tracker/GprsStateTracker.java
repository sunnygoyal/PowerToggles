package com.painless.pc.tracker;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.ServiceManager;
import android.provider.Settings;

import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephony;
import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.ReflectionUtil;
import com.painless.pc.util.Thunk;

public class GprsStateTracker extends AbstractDoubleClickTracker {

	@Thunk static long TIMER_DURATION = 5000;

	public GprsStateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_gprs));
	}

	// Add a timer since actual network change notification sometimes get delayed.
	@Thunk Timer timer = new Timer();

	@Override
	public void onActualStateChange(Context context, Intent intent) {
		timer.cancel();
		timer.purge();
		setCurrentState(context, getActualState(context));
	}

	@Override
	public int getActualState(Context context) {
		return getStaticState(context);
	}

	@Override
	protected void requestStateChange(final Context context, final boolean desiredState) {
    final UiUpdater updater = new UiUpdater(context);

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... args) {
        if (Globals.IS_LOLLIPOP) {
          return (Globals.hasPermission(context, Manifest.permission.MODIFY_PHONE_STATE) && setStateL(desiredState))
          || (RootTools.isRooted() && RootTools.runSuCommand("svc data " + (desiredState ? "enable" : "disable")));
        } else {
          boolean[] wasError = new boolean[] {false};
          ReflectionUtil.get(context)
              .invokeSetter("setMobileDataEnabled", Boolean.TYPE, desiredState, wasError);
          return !wasError[0];
        }
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (!result) {
          showSettings(context);
          updater.refresh();
        } else {
          // Start a timer to check for the actual state change.
          final long startTime = System.currentTimeMillis();
          final ReflectionUtil connectivity = ReflectionUtil.get(context);
          timer.cancel();
          timer.purge();
          timer = new Timer();
          timer.scheduleAtFixedRate(new TimerTask() {
      
            @Override
            public void run() {
              final Boolean state = getState(connectivity);
              if ((state == null) || (state == desiredState) || (System.currentTimeMillis() - startTime > TIMER_DURATION)) {
                timer.cancel();
                timer.purge();
                updater.refresh();
              }
            }
          }, 500, 500);
        }
      }
    }.execute();
	}

	public static int getStaticState(Context context) {
		final ReflectionUtil connectivity = ReflectionUtil.get(context);
		if (connectivity.isNull()) {
			return STATE_UNKNOWN;
		}
		final Boolean state = getState(connectivity);
		if (state == null) {
			return STATE_UNKNOWN;
		} else {
			return state ? STATE_ENABLED : STATE_DISABLED;
		}
	}
	
	@Thunk static Boolean getState(ReflectionUtil connectivity) {
		return (Boolean) connectivity.invokeGetter("getMobileDataEnabled");
	}


	@Override
	Intent getDCIntent(Context context) {
	  return new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
	}

	@Override
	public boolean shouldProxy(Context context) {
	  return Globals.IS_LOLLIPOP && !RootTools.isRooted();
	}

	@Thunk static void showSettings(Context context){
	  Globals.startIntent(context, new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

  @Thunk static boolean setStateL(boolean newState) {
    try {
      ITelephony stub = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
      ReflectionUtil rUtil = new ReflectionUtil(stub);
      if (rUtil.getMethod("setDataEnabled", Boolean.TYPE) != null) {
        rUtil.invokeSetter("setDataEnabled", Boolean.TYPE, newState);
      } else {
        Method altMethod = rUtil.getMethod("setDataEnabled", Integer.TYPE, Boolean.TYPE);
        if (altMethod != null) {
          int subId = ISub.Stub.asInterface(ServiceManager.getService("isub")).getDefaultSubId();
          final Object[] values = new Object[] { subId, newState };
          altMethod.invoke(stub, values);
        } else {
          return false;
        }
      }
      return true;
    } catch (Throwable e) {
      Debug.log(e);
      return false;
    }
  }
}
