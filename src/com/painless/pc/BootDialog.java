package com.painless.pc;

import java.lang.reflect.Method;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.ServiceManager;

import com.android.internal.app.AlertActivity;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.util.ProgressTask;
import com.painless.pc.util.ReflectionUtil;
import com.painless.pc.util.SectionAdapter;
import com.painless.pc.util.Thunk;

public class BootDialog extends AlertActivity implements OnClickListener {

  public static final String SOFT_MODE = "soft_mode";
  @Thunk final String[] COMMANDS = new String[] {
          "reboot -p",
          "reboot now",
          "reboot recovery",
          "reboot bootloader",
          // Soft commands
          "svc power shutdown",
          "svc power reboot",
          "reboot recovery",
          "reboot bootloader"
  };

	private boolean rebootMode;
	@Thunk boolean blockFinish = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
    mAlertParams.mNegativeButtonText = getText(R.string.act_cancel);

    if (getIntent().getBooleanExtra("info", false)) {
      mAlertParams.mTitle = getText(R.string.bt_error_title);
	    mAlertParams.mMessage = getText(R.string.bt_error_summary);

		} else if (getIntent().getBooleanExtra("menu", false)) {
		  mAlertParams.mTitle = getText(R.string.bt_menu_title);

	    SectionAdapter adapter = new SectionAdapter(this);
      adapter.addItem(getString(R.string.bt_poweroff), R.drawable.icon_toggle_shutdown);
      adapter.addItem(getString(R.string.bt_restart), R.drawable.icon_toggle_restart);
      adapter.addItem(getString(R.string.bt_recovery), R.drawable.icon_recovery);
      adapter.addItem(getString(R.string.bt_bootloader), R.drawable.icon_toggle_usb);
      mAlertParams.mAdapter = adapter;
      mAlertParams.mOnClickListener = this;

		} else {
			rebootMode = getIntent().getBooleanExtra("restart", false);
			mAlertParams.mTitle = getText(rebootMode ? R.string.bt_restart : R.string.bt_poweroff);
      mAlertParams.mMessage = getText(rebootMode ? R.string.bt_restart_summary : R.string.bt_poweroff_summary);
      mAlertParams.mPositiveButtonText = getText(R.string.act_ok);
      mAlertParams.mPositiveButtonListener = this;
		}

		setupAlert();
	}

	@Override
	public void finish() {
	  if (!blockFinish) {
	    super.finish();
	  }
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
	  if (which == DialogInterface.BUTTON_POSITIVE) {
      runSuCommand(rebootMode ? 1 : 0);
	  } else if (which >= 0) {
	    rebootMode = which > 0;
	    runSuCommand(which);
	  }
	}

	private void runSuCommand(final int commandId) {
	  blockFinish = true;
		new ProgressTask<Void, Boolean>(this, getText(rebootMode ? R.string.bt_restart_working : R.string.bt_poweroff_working)) {

			@Override
			protected Boolean doInBackground(Void... params) {
			  boolean softMode = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) && Globals.getAppPrefs(BootDialog.this).getBoolean(SOFT_MODE, true);
			  return (softMode && commandId < 2 && Globals.hasPermission(BootDialog.this, Manifest.permission.REBOOT) && tryCommand(commandId == 0))
			      || RootTools.runSuCommand(softMode ? COMMANDS[commandId + 4] : COMMANDS[commandId]);
			}

			@Override
			public void onDone(Boolean result) {
			  blockFinish = false;
				if (!result) {
					showRootError();
				}
			}
		}.execute();
	}

	@Thunk void showRootError() {
		new Handler().post(new Runnable() {

			@Override
			public void run()
			{
				Intent intent = getIntent();
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
				intent.putExtra("info", true);
				overridePendingTransition(0, 0);
				finish();

				overridePendingTransition(0, 0);
				startActivity(intent);
			}
		});
	}

	@Thunk static boolean tryCommand(boolean isShutdown) {
	  try {
	    IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
      ReflectionUtil util = new ReflectionUtil(pm);
	    if (isShutdown) {
	      // Pre Lollipop
	      Method m = util.getMethod("shutdown", Boolean.TYPE, Boolean.TYPE);
	      if (m != null) {
	        m.invoke(pm, false, false);
	      } else {
	        util.getMethod("shutdown", Boolean.TYPE, String.class, Boolean.TYPE).invoke(pm, false, null, false);
	      }
	      return true;
	    } else {
	      // 4.1
	      Method m = util.getMethod("reboot", String.class);
	      if (m != null) {
          m.invoke(pm, null);
        } else {
          util.getMethod("reboot", Boolean.TYPE, String.class, Boolean.TYPE).invoke(pm, false, null, false);
        }
	      return true;
	    }
	  } catch (Throwable e) {
	    Debug.log(e);
	  }
	  return false;
	}
}
