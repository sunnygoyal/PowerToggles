package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.painless.pc.ImmersiveService;
import com.painless.pc.R;

public class ImmersiveTracker  extends AbstractTracker {

  public static final String CHANGE_ACTION = "custom_immersive";

  @Override
  public String getChangeAction() {
    return CHANGE_ACTION;
  }

  public ImmersiveTracker(int trackerId, SharedPreferences pref) {
    super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_immersive));
  }

  @Override
  public int getActualState(Context context) {
    return ImmersiveService.IMMERSIVE_ON ? STATE_ENABLED : STATE_DISABLED;
  }

  @Override
  protected void requestStateChange(final Context context, boolean desiredState) {
    Intent i = new Intent(context, ImmersiveService.class);
    if (desiredState) {
      context.startService(i);
    } else {
      context.stopService(i);
    }
  }
}
