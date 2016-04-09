package com.painless.pc.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;

import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;

public class SettingsDecoder {

  // Colors
  public static final int COLOR_DEFAULT_LIGHT = 0x66FFFFFF;
  public static final int COLOR_DEFAULT_DARK = 0xAA333333;
  public static final int COLOR_DEFAULT_MID = Globals.IS_LOLLIPOP ? 0xFFFB8C00 : 0xFFFF9900;
  public static final int COLOR_DEFAULT_ON = Globals.IS_LOLLIPOP ? 0xFF039BE5 : 0xff3AB0DC;

  public static final String CONFIG_DARK = "{color1: 0xAA333333}";
  public static final String CONFIG_LIGHT = "{}";

  public static final String[] KEY_COLORS = new String[] {"color1", "color2", "color3"};
  public static final int[] DEFAULT_COLORS = new int[] {COLOR_DEFAULT_LIGHT, COLOR_DEFAULT_MID, COLOR_DEFAULT_ON};

  public static final String KEY_TRANSPARANCY = "back_trans";
  public static final String KEY_BACK_STYLE = "back_style";
  public static final String KEY_TINT = "back_tint";
  public static final String KEY_DENSITY = "density";

  public static final String KEY_HIDE_DIVIDERS = "hide_dividers";

  public static final String KEY_DIVIDER_COLOR = "divider_color";
  public static final int DEFAULT_DIVIDER_COLOR = 0x4D000000;

  public static final String KEY_CLICK_FEEDBACK = "click_type";
  public static final String KEY_FLAT_CORNER = "flat_corners";

  public static final int CLICK_TYPE_TRANSPARENT = 0;
  public static final int CLICK_TYPE_HOLO = 1;
  public static final int CLICK_TYPE_KITKAT = 2;
  public static final int CLICK_TYPE_RIPPLE = 3;
  public static final int CLICK_TYPE_DEFAULT = Globals.IS_LOLLIPOP ? CLICK_TYPE_RIPPLE :
          (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? CLICK_TYPE_KITKAT : CLICK_TYPE_HOLO);

  public static final String KEY_TRACKER_ARRAY = "trackers";
  private static final String DEFAULT_TRACKERS = "1,2,3,4,5,6,7";

  public static final String KEY_PADDING = "padding";
  public static final String KEY_STRETCH = "stretch";


  public final JSONObject settings;

  public SettingsDecoder(String value) {
    JSONObject parsedSettings;
    try {
      parsedSettings = new JSONObject(value);
    } catch (JSONException e) {
      parsedSettings = new JSONObject();
    }
    settings = parsedSettings;
  }

  public SettingsDecoder(JSONObject parsedSettings) {
    settings = parsedSettings;
  }

  public boolean hasValue(String key) {
    return settings.has(key);
  }

  public boolean is(String key, boolean defaultValue) {
    try {
      return settings.getBoolean(key);
    } catch (JSONException e) {
      //Debug.log(e);
      return defaultValue;
    }
  }

  public int getValue(String key, int defaultValue) {
    try {
      return settings.getInt(key);
    } catch (JSONException e) {
      //Debug.log(e);
      return defaultValue;
    }
  }

  public String getTrackerDef() {
    try {
      return settings.getString(KEY_TRACKER_ARRAY);
    } catch (JSONException e) {
      //			Debug.log(e);
      return DEFAULT_TRACKERS;
    }
  }

  public int[] getRect(String key) {
    try {
      return ParseUtil.parseIntArray(new int[4], settings.getString(key));
    } catch (Exception e) {
      return new int[4];
    }
  }
}
