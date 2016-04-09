package com.painless.pc;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import com.painless.pc.singleton.Debug;
import com.painless.pc.tracker.FlashStateTracker;
import com.painless.pc.util.Thunk;

@TargetApi(Build.VERSION_CODES.M)
public class FlashServiceM extends PriorityService {

  public FlashServiceM() {
    super(102, FlashStateTracker.CHANGE_ACTION);
  }

  private CameraCallback mCallback;
  private CameraManager mCameraManager;
  private String mCameraId;

  @Thunk boolean mWasEnabled;

  @Override
  public void onCreate() {
    super.onCreate();
    mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
    mCallback = new CameraCallback();
    mCameraManager.registerTorchCallback(mCallback, null);

    try {
      mCameraId = getCameraId();
      mCameraManager.setTorchMode(mCameraId, true);
    } catch (Throwable e) {
      Debug.log(e);
      stopSelf();
    }
  }

  private String getCameraId() throws CameraAccessException {
    String[] ids = mCameraManager.getCameraIdList();
    for (String id : ids) {
      CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
      Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
      Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
      if (flashAvailable != null && flashAvailable
          && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
        return id;
      }
    }
    return null;
  }

  @Override
  public void onDestroy() {
    mCameraManager.unregisterTorchCallback(mCallback);
    if (mCameraId != null) {
      try {
        mCameraManager.setTorchMode(mCameraId, false);
      } catch (Throwable e) {
        Debug.log(e);
      }
    }
    FlashService.FLASH_ON = false;
    broadcastState();
    super.onDestroy();
  }

  @Thunk class CameraCallback extends CameraManager.TorchCallback {

    @Override
    public void onTorchModeChanged(String cameraId, boolean enabled) {
      if (enabled) {
        mWasEnabled = true;
        FlashService.FLASH_ON = true;
        broadcastState();
      } else if (mWasEnabled) {
        FlashService.FLASH_ON = false;
        stopSelf();
      }
    }
  }
}
