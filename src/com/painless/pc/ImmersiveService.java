package com.painless.pc;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;

import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.tracker.ImmersiveTracker;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class ImmersiveService extends PriorityService implements OnKeyListener, OnTouchListener, OnSystemUiVisibilityChangeListener, Runnable {

  private static final int IMMERSIVE_MODE_FLAG =
          View.SYSTEM_UI_FLAG_IMMERSIVE |
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE | 
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_FULLSCREEN;

  public ImmersiveService() {
    super(105, ImmersiveTracker.CHANGE_ACTION);
  }

  public static boolean IMMERSIVE_ON = false;

  private Handler mHandler;
  private ImageView mBlockerView;

  @Override
  public void onCreate() {
    super.onCreate();
    mHandler = new Handler();

    mBlockerView = new ImageView(this);
    mBlockerView.setImageResource(R.drawable.icon_add);
    mBlockerView.setOnKeyListener(this);
    mBlockerView.setOnTouchListener(this);
    mBlockerView.setOnSystemUiVisibilityChangeListener(this);

    WindowManager.LayoutParams params= new WindowManager.LayoutParams();
    params.width = 0;
    params.height = 0;
    params.type = WindowManager.LayoutParams.TYPE_TOAST;
    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

    params.alpha = -3.0F;
    params.gravity = Gravity.TOP | Gravity.RIGHT;

    ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(mBlockerView, params);

    mBlockerView.setSystemUiVisibility(IMMERSIVE_MODE_FLAG);
    IMMERSIVE_ON = true;
    broadcastState();

    maybeShowNotification("immersive_notify_hidden", false, R.drawable.icon_toggle_immersive, R.string.immersive_active,
            R.string.click_to_deactive, Globals.getAppPrefs(this).getBoolean("immersive_lock", false));
  }

  @Override
  public void onDestroy() {
    mBlockerView.setVisibility(View.INVISIBLE);
    try {
      ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mBlockerView);
    } catch (Exception e) {
      Debug.log(e);
    }

    IMMERSIVE_ON = false;
    broadcastState();
    clearNotification();
    mHandler.removeCallbacks(this);
    super.onDestroy();
  }

  @Override
  public void run() {
    mBlockerView.setVisibility(View.VISIBLE);
    mBlockerView.setSystemUiVisibility(IMMERSIVE_MODE_FLAG);
  }


  @Override
  public void onSystemUiVisibilityChange(int visibility) {
    if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
      mBlockerView.setVisibility(View.INVISIBLE);
      mHandler.removeCallbacks(this);
      mHandler.postDelayed(this, 3000);
    }
  }

  @Override
  public boolean onKey(View v, int keyCode, KeyEvent event) {
    return false;
  }

  @Override
  public boolean onTouch(View v, MotionEvent e) {
    return false;
  }
}
