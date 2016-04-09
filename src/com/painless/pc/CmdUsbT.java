package com.painless.pc;

import android.content.Context;
import android.net.IConnectivityManager;
import android.os.ServiceManager;

import com.painless.pc.singleton.Debug;

public class CmdUsbT {

  public static void main(String[] args) throws Exception {
    if (!run(Boolean.parseBoolean(args[0]))) {
      System.exit(1);
    }
  }

  public static boolean run(boolean newState) {
    try {
      return IConnectivityManager.Stub.asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE)).setUsbTethering(newState) == 0;
    } catch (Throwable e) {
      Debug.log(e);
      return false;
    }
  }
}
