package com.painless.pc.tracker;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.provider.Settings;
import android.view.Surface;
import android.view.WindowManager;

import com.painless.pc.R;
import com.painless.pc.RLPicker;
import com.painless.pc.RLService;
import com.painless.pc.singleton.Globals;

public class RotationLockTracker extends AbstractTracker {

  private boolean mShowPrompt;

  public RotationLockTracker(int trackerId, SharedPreferences pref) {
    super(trackerId, pref, new int[] {
            COLOR_DEFAULT, R.drawable.icon_rotation_port, COLOR_DEFAULT, R.drawable.icon_rotation_land,
            COLOR_ON, R.drawable.icon_toggle_autorotate
    });
  }

  @Override
  public void init(SharedPreferences pref) {
    mShowPrompt = pref.getBoolean("rotation_lock_prompt", true);
  }

  @Override
  public int getActualState(Context context) {
    final ContentResolver cr = context.getContentResolver();

    if (isAutoRotate(cr) && (RLService.LOCK_VIEW == null)) {
      return STATE_ENABLED;
    } else {
      boolean isLandScapeDefault = isLandScapeDefault(context);
      boolean isPortrait = isLandScapeDefault ^ RLService.LOCK_VIEW == null;
      return isPortrait ? STATE_DISABLED : STATE_INTERMEDIATE;
    }
  }

  @Override
  public void toggleState(Context context) {
    if (mShowPrompt) {
      Globals.startIntent(context, Globals.setIncognetoIntent(new Intent(context, RLPicker.class)));
    } else {
      Intent i = new Intent(context, RLService.class);
      if (isAutoRotate(context.getContentResolver())) {
        context.stopService(i);
      } else if (RLService.LOCK_VIEW == null) {
        context.startService(i);
      } else {
        context.stopService(i);
      }
      RLPicker.setAutoRotate(context, 0);
    }
  }

  @Override
  protected void requestStateChange(Context context, boolean desiredState) {
    // Never Called
  }

  @Override
  public boolean shouldProxy(Context context) {
    return true;
  }

  @Override
  public String getStateText(int state, String[] states, String[] labelArray) {
    return states[7 + mDisplayNumber];
  }

  public static final boolean isLandScapeDefault(Context ctx) {
    WindowManager lWindowManager =  (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

    Configuration cfg = ctx.getResources().getConfiguration();
    int lRotation = lWindowManager.getDefaultDisplay().getRotation();

    return (((lRotation == Surface.ROTATION_0) || (lRotation == Surface.ROTATION_180)) &&   
            (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE)) ||
            (((lRotation == Surface.ROTATION_90) || (lRotation == Surface.ROTATION_270)) &&    
                    (cfg.orientation == Configuration.ORIENTATION_PORTRAIT));
  }

  private static boolean isAutoRotate(final ContentResolver resolver) {
    return Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1;
  }
}
