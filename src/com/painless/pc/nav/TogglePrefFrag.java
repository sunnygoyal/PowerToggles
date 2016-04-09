package com.painless.pc.nav;

import android.os.Build;
import android.view.View;

import com.painless.pc.BootDialog;
import com.painless.pc.R;
import com.painless.pc.TrackerManager;
import com.painless.pc.tracker.BatteryTracker;
import com.painless.pc.tracker.LockScreenToggle;
import com.painless.pc.tracker.WifiStateTracker;
import com.painless.pc.util.prefs.BatteryColorPrefs;
import com.painless.pc.util.prefs.BatteryLevelPref;
import com.painless.pc.util.prefs.BrightModeListPref;
import com.painless.pc.util.prefs.CheckboxPref;
import com.painless.pc.util.prefs.FontSizePref;
import com.painless.pc.util.prefs.GPSModePrefs;
import com.painless.pc.util.prefs.MediaPickerPref;
import com.painless.pc.util.prefs.MultiChoicePref;
import com.painless.pc.util.prefs.TimeoutPopupPref;

public class TogglePrefFrag extends SettingsFrag {

  private final int[] mConfigurableToggles = new int[] {
          3, 7, 23,
          15, 5, 16,
          10, 27,
          18,
          4, 13,
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? -1 : 47),
          38,
          28,
          36, 25, 45,
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 ? -1 : 29),
          14
        };

  @Override
  protected void buildScreen() {
    String trackerLables[] = getResources().getStringArray(R.array.tracker_names);
    for (int id : mConfigurableToggles) {
      if (id > -1) {
        addSection(TrackerManager.getTracker(id, mPrefs).buttonConfig[1], id, trackerLables[id]);
      }
    }
  }

  @Override
  protected View getPrefView(int id) {
    switch (id) {
      case 3:  // Wifi State tracker
        return new CheckboxPref(mInflator, WifiStateTracker.KEY_SHOW_SSID, mPrefs, R.string.ts_show_ssid).view;
      case 7:  // BacklightTracker
        return wrapContent(
            new CheckboxPref(mInflator, "quick_brightness", mPrefs, R.string.ts_instant_bright_toggle).view,
            new BrightModeListPref(mInflator, mPrefs).view);
      case 23: // BrightnessSliderToggle
          return wrapContent(
              new CheckboxPref(mInflator, "bright_slider_zero", mPrefs, R.string.ts_allow_zero_bright).view,
              new CheckboxPref(mInflator, "bright_slider_preset", mPrefs, R.string.ts_show_quick_buttons).view);
      case 15: // Battery Toggle
        CheckboxPref chkPref = new CheckboxPref(mInflator, BatteryTracker.ENABLED_KEY, mPrefs, R.string.ts_battery_custom_colors);
        return wrapContent(new BatteryLevelPref(mInflator, mPrefs).view,
            chkPref.view,
            new BatteryColorPrefs(mInflator, mPrefs, chkPref).view);
      case 5:  // GPS
        return new GPSModePrefs(mInflator, mPrefs).view;
      case 16: // TimeoutTracker
        return new TimeoutPopupPref(mInflator, mPrefs).view;
      case 10: // VolumeToggle
        return new MultiChoicePref(mInflator, mPrefs, R.array.ts_volume_modes, R.array.st_volume_icons, "volume_toggles", R.string.ts_volume_modes, 2).view;
      case 27: // VolumeSliderToggle
        return wrapContent(
            new CheckboxPref(mInflator, "volume_slider_preset", mPrefs, R.string.ts_show_quick_buttons, true).view,
            new CheckboxPref(mInflator, "mute_slider", mPrefs, R.string.ts_mute_slider).view,
            new MultiChoicePref(mInflator, mPrefs, R.array.ts_vslider_modes,
                R.array.st_vslider_icons, "slider_toggles", R.string.ts_slider_controls, 1).view);
      case 18:
        return new MediaPickerPref(mInflator, mPrefs).view;
      case 4:  // Flash Light
        return wrapContent(
                new CheckboxPref(mInflator, "flash_lock", mPrefs, R.string.ts_exit_on_lock).view,
                new CheckboxPref(mInflator, "flash_notify_hidden", mPrefs, R.string.ts_hide_notify, true).view);
      case 13: // Wake lock
        return wrapContent(
                new CheckboxPref(mInflator, "wake_lock", mPrefs, R.string.ts_exit_on_lock).view,
                new CheckboxPref(mInflator, "wake_lock_notify_hidden", mPrefs, R.string.ts_hide_notify, true).view);
      case 47: // Immersive mode
        return wrapContent(
                new CheckboxPref(mInflator, "immersive_lock", mPrefs, R.string.ts_exit_on_lock).view,
                new CheckboxPref(mInflator, "immersive_notify_hidden", mPrefs, R.string.ts_hide_notify).view);
      case 38: // Rotation Lock
        return wrapContent(
                new CheckboxPref(mInflator, "rotation_lock_notify_hidden", mPrefs, R.string.ts_hide_notify, true).view,
                new CheckboxPref(mInflator, "rotation_lock_prompt", mPrefs, R.string.ts_show_prompt, true).view);
      case 28: // Sync now
        return new CheckboxPref(mInflator, "sync_now_idp", mPrefs, R.string.ts_not_all_sync).view;
      case 25: //LockScreenToggle
        return new CheckboxPref(mInflator, LockScreenToggle.KEY_DOUBLE_LOCK, mPrefs, R.string.ts_screen_lock_fix, LockScreenToggle.DEFAULT_VALUE).view;
      case 36: // FontIncreaseTracker
        return new FontSizePref(mContext, mPrefs).getView();
      case 45: // No Lock
        return new CheckboxPref(mInflator, "no_lock_hidden", mPrefs, R.string.ts_hide_notify).view;
      case 29: // Shutdown command
        return new CheckboxPref(mInflator, BootDialog.SOFT_MODE, mPrefs, R.string.ts_soft_boot, true).view;
      case 14: // Wimax Tracker
        return new CheckboxPref(mInflator, "shrt_4g", mPrefs, R.string.ts_us_as_shortcut).view;
    }
    return null;
  }
}
