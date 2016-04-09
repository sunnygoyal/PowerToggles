package com.painless.pc;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.res.Configuration;

import com.painless.pc.singleton.Debug;

/**
 * A command to change system font size.
 */
public class CmdFont {

  public static void main(String[] args) throws Exception {
    run(Float.parseFloat(args[0]));
  }

  public static boolean run(float delta) {
    try {
      IActivityManager amn = ActivityManagerNative.getDefault();
      Configuration config = amn.getConfiguration();
      config.fontScale = (delta == 0) ? 1 : (config.fontScale + delta);
      amn.updatePersistentConfiguration(config);
      return true;
    } catch (Throwable e) {
      Debug.log(e);
      return false;
    }
  }
}
