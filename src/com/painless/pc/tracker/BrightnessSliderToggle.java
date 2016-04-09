package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.painless.pc.acts.BrightnessSlider;
import com.painless.pc.singleton.Globals;

public final class BrightnessSliderToggle extends BacklightTracker {

  public BrightnessSliderToggle(int trackerId, SharedPreferences pref) {
    super(trackerId, pref);
  }

  @Override
  public void toggleState(Context context) {
    if (AbstractSystemSettingsTracker.hasPermission(context)) {
      Globals.startIntent(context, Globals.setIncognetoIntent(new Intent(context, BrightnessSlider.class)));
    } else {
      AbstractSystemSettingsTracker.showPermissionDialog(context, Settings.ACTION_DISPLAY_SETTINGS);
    }
  }

  @Override
  public boolean shouldProxy(Context context) {
    return true;
  }
}
