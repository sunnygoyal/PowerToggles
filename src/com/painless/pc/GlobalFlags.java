package com.painless.pc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;

import com.painless.pc.singleton.Globals;

public class GlobalFlags {

  private static Boolean sHapticFeedback = null; 
  private static Integer sHapticLength = null;

  private static Boolean sEnablePartialUpdate = null; 

  public static void clearFlags() {
    sHapticFeedback = null;
    sHapticLength = null;
    sEnablePartialUpdate = null;
  }

  private static void setFlags(Context c) {
    if (sHapticFeedback == null) {
      SharedPreferences prefs = Globals.getAppPrefs(c);
      sHapticFeedback = prefs.getBoolean("heptic_feedback", false);
      sHapticLength = prefs.getInt("haptic_len", 10);
      sEnablePartialUpdate = prefs.getBoolean("partial_update", true);
    }
  }

  public static boolean partialUpdate(Context c) {
    setFlags(c);
    return sEnablePartialUpdate;
  }

  public static boolean hapticFeedback(Context c) {
    setFlags(c);
    return sHapticFeedback;
  }

  public static int hapticLength(Context c) {
    setFlags(c);
    return sHapticLength;
  }

  public static void playHaptic(Context c) {
    Vibrator myVib = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    if (myVib != null) {
      myVib.vibrate(hapticLength(c));
    }
  }
}
