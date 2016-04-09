package com.painless.pc;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.painless.pc.util.Thunk;

public class BatteryService extends Service {

  private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      int currentBattery = parseBattery(intent);
      if (currentBattery != mLastBattery) {
        mLastBattery = currentBattery;
        sendBroadcast(new Intent(context, CommandReceiver.class).setAction(PCWidgetActivity.BATTERY_POLL_ACTION));
      }
    }
  };
  @Thunk int mLastBattery = 0;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    Intent intent = registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    mLastBattery = parseBattery(intent);
  }

  @Thunk int parseBattery(Intent intent) {
    if (intent == null) {
      return mLastBattery;
    }
    final int level = intent.getIntExtra("level", 0);
    final int scale = intent.getIntExtra("scale", 100);
    return level * 100 / scale;
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(mBatteryReceiver);
    super.onDestroy();
  }
}
