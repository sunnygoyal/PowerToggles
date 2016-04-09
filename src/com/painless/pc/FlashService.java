package com.painless.pc;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.tracker.FlashStateTracker;

public class FlashService extends PriorityService implements Runnable, SurfaceHolder.Callback {

  public FlashService() {
    super(102, FlashStateTracker.CHANGE_ACTION);
  }

  public static boolean FLASH_ON = false;

  private Camera camera;

  private WindowManager wm;
  private LightSurfaceView surface;

  private PowerManager.WakeLock lock;
  private Handler handler;

  @Override
  public void onCreate() {
    super.onCreate();
    surface = new LightSurfaceView(this);
    surface.getHolder().addCallback(this);

    wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1, -10, -10,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.TOP | Gravity.LEFT;

    wm.addView(surface, params);

    surface.setZOrderOnTop(true);

    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "PTF");
    lock.acquire();

    handler = new Handler();
    handler.postDelayed(this, 500);
  }

  @Override
  public void run() {
    if (surface.getHolder().isCreating()) {
      handler.postDelayed(this, 500);
    } else {
      surfaceCreated(surface.getHolder());
    }		
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    try {
      handler.removeCallbacks(this);
    } catch (Throwable e) { }
    try {
      camera = Camera.open();
    } catch (RuntimeException e) {
      // Some unexpected error occurred.
      // Nothing can be done. Flash will not be supported.
    }

    if (camera != null) {
      final Parameters parameters = camera.getParameters();
      parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
      try {
        camera.setParameters(parameters);
        camera.setPreviewDisplay(surface.getHolder());
        camera.startPreview();
      } catch (final Throwable e) {
        Debug.log(e);
      }
    }
    FLASH_ON = true;
    broadcastState();

    maybeShowNotification("flash_notify_hidden", true, R.drawable.icon_toggle_flash, R.string.flash_active,
            R.string.click_to_deactive, Globals.getAppPrefs(this).getBoolean("flash_lock", false));
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    releaseCamera();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  private void releaseCamera() {
    if (camera != null) {
      try {
        camera.stopPreview();
      } catch (Throwable e) {
        Debug.log(e);
      }
      try {
        camera.release();
      } catch (Throwable e) {
        Debug.log(e);
      }
      camera = null;
    }
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacks(this);
    clearNotification();

    releaseCamera();

    lock.release();
    try {
      wm.removeView(surface);
    } catch (Throwable e) {
      Debug.log(e);
    }

    FLASH_ON = false;
    broadcastState();
    super.onDestroy();
  }

  private static class LightSurfaceView extends SurfaceView {

    public LightSurfaceView(Context paramContext) {
      super(paramContext);
      getHolder().setType(3);
    }
  }
}
