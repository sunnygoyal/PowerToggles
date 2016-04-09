package com.painless.pc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.app.AlertActivity;
import com.painless.pc.singleton.Debug;
import com.painless.pc.tracker.AbstractSystemSettingsTracker;
import com.painless.pc.tracker.RotationLockTracker;
import com.painless.pc.util.SectionAdapter;

public class RLPicker extends AlertActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SectionAdapter adapter = new SectionAdapter(this);
    adapter.addItem(getString(R.string.rt_auto), R.drawable.icon_toggle_autorotate);

    if (RotationLockTracker.isLandScapeDefault(this)) {
      adapter.addItem(getString(R.string.rt_land, getText(R.string.rt_default)),R.drawable.icon_rotation_land);
      adapter.addItem(getString(R.string.rt_port, getText(R.string.rt_forced)),R.drawable.icon_rotation_port);
    } else {
      adapter.addItem(getString(R.string.rt_port, getText(R.string.rt_default)),R.drawable.icon_rotation_port);
      adapter.addItem(getString(R.string.rt_land, getText(R.string.rt_forced)),R.drawable.icon_rotation_land);
    }

    mAlertParams.mTitle = getText(R.string.rt_title);
    mAlertParams.mAdapter = adapter;
    mAlertParams.mOnClickListener = this;
    mAlertParams.mNegativeButtonText = getText(R.string.act_cancel);
    setupAlert();
	}

	@Override
  public void onClick(DialogInterface dialog, int type) {
		Intent i = new Intent(this, RLService.class);

		switch (type) {
			case 0:
				stopService(i);
				setAutoRotate(this, 1);
				break;
			case 1:
				stopService(i);
				setAutoRotate(this, 0);
				break;
			case 2:
				setAutoRotate(this, 1);
				startService(i);
				break;
		}
		PCWidgetActivity.partialUpdateAllWidgets(this);
	}

	public static void setAutoRotate(Context c, int value) {
	  try {
      if (Settings.System.getInt(c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) != value) {
        if (AbstractSystemSettingsTracker.hasPermission(c)) {
          Settings.System.putInt(c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, value);
        } else {
          AbstractSystemSettingsTracker.showPermissionDialog(c, Settings.ACTION_DISPLAY_SETTINGS);
        }
      }
    } catch (SettingNotFoundException e) {
      Debug.log(e);
    }
	}
}
