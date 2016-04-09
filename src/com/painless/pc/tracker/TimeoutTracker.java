package com.painless.pc.tracker;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.theme.BatteryImageProvider;
import com.painless.pc.theme.FixedImageProvider;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.util.WidgetSetting;

public final class TimeoutTracker extends AbstractTracker {

  public static final int ID = 16;

  public static final String DEFAULT = "30,60,300";
  public static final String KEY = "timeout_level";

  public TimeoutTracker(int trackerId, SharedPreferences pref) {
    super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_timeout));
  }

  private int[] mModes;
  private int mLastIndex;
  private int mCheck;
  private int current = 30000;

  @Override
  public int getActualState(Context context) {
    try {
      current = Settings.System.getInt(context.getContentResolver(),
              Settings.System.SCREEN_OFF_TIMEOUT);
      current = current < mModes[mLastIndex] ?
              (current <= mModes[0] ? mModes[0] : current) : mModes[mLastIndex];
    } catch (final SettingNotFoundException e) { }

    if (current == mModes[mLastIndex]) {
      return STATE_ENABLED;
    } else if (current == mModes[0]) {
      return STATE_DISABLED;
    } else {
      return STATE_INTERMEDIATE;
    }
  }

  @Override
  public int setImageViewResources(Context context, RemoteViews views,
          int buttonId, WidgetSetting setting, ToggleBitmapProvider imageProvider) {
    final int colorId = getStateColor(context);

    Bitmap img = imageProvider==null ? null : imageProvider.getIcon(current / (current >= mCheck ? 60000 : 1000));
    if (img == null) {
      views.setImageViewResource(buttonId, buttonConfig[2* mDisplayNumber + 1]);
    } else {
      views.setImageViewBitmap(buttonId, img);
    }
    boolean useColor = img==null || buttonConfig.length > 2;
    views.setInt(buttonId, "setAlpha", useColor ? setting.buttonAlphas[colorId] : 255);
    views.setInt(buttonId, "setColorFilter", useColor ? setting.buttonColors[colorId] : 0);
    return colorId;
  }

  @Override
  public ToggleBitmapProvider getImageProvider(Context context, Bitmap icon) {
    return icon == null ? null :
      (icon.getWidth() > icon.getHeight() ?
          new BatteryImageProvider(icon, context) : new FixedImageProvider(icon));
  }

  @Override
  public void toggleState(Context context) {
    if (AbstractSystemSettingsTracker.hasPermission(context)) {
      requestStateChange(context, false);
    } else {
      AbstractSystemSettingsTracker.showPermissionDialog(context, Settings.ACTION_DISPLAY_SETTINGS);
    }
  }

  @Override
  protected void requestStateChange(Context context, boolean desiredState) {
    int newState = mModes[0];
    for (int i = mLastIndex; i >= 0; i--) {
      if (current < mModes[i]) {
        newState = mModes[i];
      }
    }

    android.provider.Settings.System.putInt(context.getContentResolver(),
            android.provider.Settings.System.SCREEN_OFF_TIMEOUT, newState);
    current = newState;

    Toast.makeText(context, "Screen timeout set to " + getStateText(0, null, null),
            Toast.LENGTH_SHORT).show();
  }

  @Override
  public void init(SharedPreferences pref) {
    mModes = ParseUtil.parseIntArray(null,  pref.getString(KEY, DEFAULT));
    Arrays.sort(mModes);
    mLastIndex = mModes.length - 1;
    for (int i = 0; i <= mLastIndex; i++) {
      mModes[i] = mModes[i] * 1000;
    }
    mCheck = mModes[mLastIndex] > 60000 ? 60000 : 60001;
  }

  @Override
  public String getStateText(int state, String[] states, String[] labelArray) {
    int c = current / 1000;
    return (c < 60) ? (c + " sec") : (c/60 + " min");
  }
}
