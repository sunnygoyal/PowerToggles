package com.painless.pc.tracker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.Thunk;

public final class GpsStateTracker extends AbstractTracker {

  public static final String KEY_SOURCES = "gps_sources";
  public static final String DEFAULT_SOURCES = "2,-1";

  private static final String CHANGE_ACTION = "android.location.PROVIDERS_CHANGED";
  private static final int MODE_DEVICE = 2;
  private static final int MODE_NETOWRK = 1;

  @Thunk static final int[] mStates = new int[2];

  @Thunk GPSToggleTask mCurrentTask;

	public GpsStateTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? R.drawable.icon_toggle_gps_2 : R.drawable.icon_toggle_gps));
	}

	@Override
	public void init(SharedPreferences pref) {
	  ParseUtil.parseIntArray(mStates, pref.getString(KEY_SOURCES, DEFAULT_SOURCES));
	}

	public String getChangeAction() {
	  return CHANGE_ACTION;
	}

	@Override
	public int getActualState(Context context) {
	  if (mCurrentTask != null) {
	    return STATE_INTERMEDIATE;
	  }

		final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		final boolean statusOfGPS = 
            (((mStates[0] & MODE_DEVICE) == MODE_DEVICE) ? manager.isProviderEnabled(LocationManager.GPS_PROVIDER) : true) &&
            (((mStates[0] & MODE_NETOWRK) == MODE_NETOWRK) ? manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) : true);
		return statusOfGPS ? STATE_ENABLED : STATE_DISABLED;
	}

	@Override
	protected void requestStateChange(final Context context, boolean desiredState) {
	  mCurrentTask = new GPSToggleTask(context) {

	    @Override
	    protected void onPostExecute(Boolean result) {
	      mCurrentTask = null;
        if (!result) {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Try running the Hack method.
            String pkg = "com.android.settings";
            String clazz = "com.android.settings.widget.SettingsAppWidgetProvider";
            try {
              PackageInfo info = context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_RECEIVERS);
              for (ActivityInfo in : info.receivers) {
                if (in.enabled && in.exported && clazz.equals(in.name)) {
                  context.sendBroadcast(new Intent()
                      .setClassName(pkg, clazz)
                      .addCategory(Intent.CATEGORY_ALTERNATIVE)
                      .setData(Uri.parse("custom:3")));
                  return;
                }
              }
            } catch (Exception e) {
              Debug.log(e);
              // Hack not available.
            } 
          }
          launchIntent(context);
        }
        
	      new UiUpdater(context).refresh();
	    }
	  };
	  mCurrentTask.execute(desiredState);
	}

	@Thunk void launchIntent(Context context) {
    Globals.startIntent(context, new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));      
	}

	private class GPSToggleTask extends AsyncTask<Boolean, Void, Boolean> {

	  private final LocationManager mManager;
	  private final boolean mSystemMode;
	  private final ContentResolver mResolver;
	  
	  GPSToggleTask(Context context) {
	    mManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	    mSystemMode = Globals.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS);
	    mResolver = context.getContentResolver();
	  }

    @Override
    protected Boolean doInBackground(Boolean... params) {
      boolean desiredState = params[0];

      return mStates[1] == -1 ?
              setFirstStateTo(mManager, desiredState) :
                setNetworkState(mManager, mStates[desiredState ? 0 : 1]);

//      if (desiredState) {
//        return mStates[1] == -1 ? setFirstStateTo(mManager, true) : setNetworkState(mManager, mStates[0]);
//      } else {
//        return mStates[1] == -1 ? setFirstStateTo(mManager, false) : setNetworkState(mManager, mStates[1]);
//      }
    }

    /**
     * Only sets the providers defined in mState[0] to value
     */
    private boolean setFirstStateTo(LocationManager manager, boolean value) {
      return
          (((mStates[0] & MODE_DEVICE) == MODE_DEVICE) ? setProvider(manager, LocationManager.GPS_PROVIDER, value) : true) &&
          (((mStates[0] & MODE_NETOWRK) == MODE_NETOWRK) ? setProvider(manager, LocationManager.NETWORK_PROVIDER, value) : true);
    }

    /**
     * Sets the list of enabled providers to ones defined in value.
     */
    private boolean setNetworkState(LocationManager manager, int value) {
      return setProvider(manager, LocationManager.GPS_PROVIDER, (value & MODE_DEVICE) == MODE_DEVICE) &&
              setProvider(manager, LocationManager.NETWORK_PROVIDER, (value & MODE_NETOWRK) == MODE_NETOWRK);
    }


    /**
     * Sets the provider to that particular state. Returns true on success.
     */
    private boolean setProvider(LocationManager manager, String provider, boolean state) {
      if (manager.isProviderEnabled(provider) == state) {
        // already enabled
        return true;
      } else if (mSystemMode) {
        Settings.Secure.setLocationProviderEnabled(mResolver, provider, state);
        return true;
      } else {
        return RootTools.runSuCommand("settings put secure location_providers_allowed " + (state ? "+" : "-") + provider);
      }
    }
	}

}