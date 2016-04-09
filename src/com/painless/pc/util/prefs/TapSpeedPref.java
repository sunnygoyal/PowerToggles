package com.painless.pc.util.prefs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.painless.pc.R;
import com.painless.pc.nav.SettingsFrag;
import com.painless.pc.tracker.AbstractDoubleClickTracker;
import com.painless.pc.util.SeekNumberPicker;
import com.painless.pc.util.Thunk;

public class TapSpeedPref extends AbstractPopupPref implements Runnable {

  private final Handler mTapHandler;

  private SeekNumberPicker mPicker;
  private View mTapTarget;

  private long mStartTime;
  private int mInitialValue;

  public TapSpeedPref(LayoutInflater inflator, SharedPreferences prefs) {
    super(inflator, prefs, R.string.ts_tap_speed, android.R.layout.simple_list_item_1);
    mTapHandler = new Handler();
  }

  @Override
  public AlertDialog showBuilder(Builder builder) {
    int value = mPrefs.getInt("tap_speed", AbstractDoubleClickTracker.DEFAULT_DOUBLE_CLICK_GAP);
    mPicker = new SeekNumberPicker(getContext());
    mPicker.setSummary(R.string.ts_msec);
    mPicker.setMax(999);
    mPicker.setValue(value);

    View tapContainer = mInflator.inflate(R.layout.ts_tap_speed, null);
    mTapTarget = tapContainer.findViewById(R.id.btn_1);
    mTapTarget.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        tapDetectionClicked();
      }
    });

    return builder.setView(SettingsFrag.getWrapper(getContext(), mPicker.getView(), tapContainer)).show();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    mPrefs.edit().putInt("tap_speed", mPicker.getValue()).commit();
  }

  @Thunk void tapDetectionClicked() {
    mTapHandler.removeCallbacks(this);
    
    if (mTapTarget.isSelected()) {
      // Already running.
      long diff = System.currentTimeMillis() - mStartTime;
      mTapTarget.setSelected(false);
      mPicker.setValue(diff < 1000 ? (int) diff : mInitialValue); 

    } else {
      mStartTime = System.currentTimeMillis();
      mInitialValue = mPicker.getValue();
      mTapTarget.setSelected(true);
      run();
    }
  }

  @Override
  public void run() {
    long diff = System.currentTimeMillis() - mStartTime;
    if (diff < 1000) {
      // keep running
      mPicker.setValue((int) diff); 
      mTapHandler.post(this);
    } else {
      
      // Too long. Stop listener.
      mPicker.setValue(mInitialValue);
      mTapTarget.setSelected(false);
    }
  }
}
