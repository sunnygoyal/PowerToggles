package com.painless.pc.tracker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;

import com.painless.pc.LockAdmin;
import com.painless.pc.ProxyActivity;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;

public class LockScreenToggle extends AbstractCommand {

  public static final String KEY_DOUBLE_LOCK = "fix_screen_on";
  public static final boolean DEFAULT_VALUE = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1;

	public LockScreenToggle(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, R.drawable.icon_toggle_lock);
	}

	@Override
	public void toggleState(Context context) {
    final DevicePolicyManager pm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName cm = new ComponentName(context, LockAdmin.class);

    if (pm.isAdminActive(cm)) {
      Runnable lockRunnable = new Runnable() {

        @Override
        public void run() {
          pm.lockNow();
        }
      };
      Handler handler = new Handler();
      handler.post(lockRunnable);
      if (Globals.getAppPrefs(context).getBoolean(KEY_DOUBLE_LOCK, DEFAULT_VALUE)) {
        handler.postDelayed(lockRunnable, 500);
      }
    } else {
      Globals.startIntent(context, new Intent(context, ProxyActivity.class)
          .putExtra("proxy", new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
              .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cm)
              .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getResources().getString(R.string.admin_explain))));
    }
	}

	@Override
	public Intent getIntent() {
	  return null;
	}

	@Override
	public boolean shouldProxy(Context context) {
		return !((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE))
		          .isAdminActive(new ComponentName(context, LockAdmin.class));
	}
}
