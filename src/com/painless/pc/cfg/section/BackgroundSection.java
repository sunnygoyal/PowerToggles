package com.painless.pc.cfg.section;

import static com.painless.pc.util.SettingsDecoder.KEY_DENSITY;
import static com.painless.pc.util.SettingsDecoder.KEY_PADDING;
import static com.painless.pc.util.SettingsDecoder.KEY_STRETCH;
import static com.painless.pc.util.SettingsDecoder.KEY_TINT;
import static com.painless.pc.util.SettingsDecoder.KEY_TRANSPARANCY;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.painless.pc.FileProvider;
import com.painless.pc.R;
import com.painless.pc.cfg.NinePatchEditor;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.CallerActivity;
import com.painless.pc.util.ImageLoadTask.LargeImageLoadTask;
import com.painless.pc.util.ImportExportActivity;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.WidgetSetting;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class BackgroundSection extends ConfigSection implements OnClickListener, OnColorChangedListener, CallerActivity.ResultReceiver, OnSeekBarChangeListener {

  private static final int REQUEST_IMAGE = 11;
  private static final int REQUEST_9PATCH = 12;

  private static final String BACK_IMAGE_URI = "content://com.painless.pc.file/config#";

  @Thunk final ImportExportActivity<?> mProxy;
  private final View[] mTabs = new View[3];
  private final ColorButton mTintButton;
  private final View mStretchBtn;
  private final SeekBar mBackTransSeek;

  private int mSelectedIndex = -1;
  @Thunk Bitmap mCurrentBitmap = null;
  private int mRefreshCount = 0;

  @Thunk int[] mNinePatchRect, mPaddingRect;

  public BackgroundSection(ImportExportActivity<?> activity, ConfigCallback callback) {
    super(activity, callback, R.id.hdr_back_fill, R.id.cnt_back_fill);

    mProxy = activity;
    mTintButton = (ColorButton) mContainerView.findViewById(R.id.cfg_back_tint);
    mTintButton.setOnColorChangeListener(this, false);

    mBackTransSeek = (SeekBar) mContainerView.findViewById(R.id.cfg_back_trans);
    mBackTransSeek.setOnSeekBarChangeListener(this);

    mStretchBtn = mContainerView.findViewById(R.id.cfg_back_stretch);
    mStretchBtn.setOnClickListener(this);

    for (int i = 0; i < 3; i++) {
      mTabs[i] = mContainerView.findViewById(BUTTON_TRIPLET[i]);
      mTabs[i].setOnClickListener(this);
    }
  }

  @Override
  public void readSetting(WidgetSetting setting, ConfigExtraData extraData) {
    super.readSetting(setting, extraData);

    int index = 0;
    if (setting.backTint != null) {
      mTintButton.setColor(setting.backTint);
      index = 2;
    }

    mNinePatchRect = extraData.decoder.getRect(KEY_STRETCH);
    mPaddingRect = new int[4];

    if (BitmapUtils.saveBitmap(extraData.backBitmap, mNinePatchRect, FileProvider.tempBackImage(mProxy))) {
      mCurrentBitmap = extraData.backBitmap;
      setting.backimage = Uri.parse(BACK_IMAGE_URI + mRefreshCount++);
      index = 1;

      // Initialize padding
      mPaddingRect = extraData.decoder.getRect(KEY_PADDING);
    } else {
      mNinePatchRect = new int[4];
      setting.backimage = null;
      extraData.backBitmap = null;
    }

    mBackTransSeek.setProgress(setting.backTrans);
    selectTab(index);
  }

  @Override
  public void onClick(View v) {
    switch(v.getId()) {
      case R.id.cfg_back_stretch: {
        int[] padding = new int[] {
                mPaddingRect[0],
                mPaddingRect[1],
                mCurrentBitmap.getWidth() - mPaddingRect[2],
                mCurrentBitmap.getHeight() - mPaddingRect[3]
        };
        Intent intent = new Intent(mProxy, NinePatchEditor.class)
        .putExtra("img", mCurrentBitmap)
        .putExtra(KEY_STRETCH, mNinePatchRect)
        .putExtra(KEY_PADDING, padding);
        mProxy.requestResult(REQUEST_9PATCH, intent, this);
        break;
      }
      case R.id.btn_3:	// Tint
        if (mSelectedIndex == 2) {	// second time press
          mTintButton.onClick(mTintButton);
        } else {	// first time click
          selectTab(2);
          mSetting.backimage = null;
          mExtraData.backBitmap = null;
          onColorChanged(mTintButton.getColor(), mTintButton);
        }
        break;
      case R.id.btn_2:	// Image
        if ((mSelectedIndex == 1) || (mCurrentBitmap == null)) {
          // Pick image.
          Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
          try {
            mProxy.requestResult(REQUEST_IMAGE, intent, this);
          } catch (ActivityNotFoundException e) {
            Debug.log(e);
            Toast.makeText(mProxy, R.string.err_no_activity, Toast.LENGTH_LONG).show();
          }
        } else {
          setImageMode();
        }
        break;
      default:
        selectTab(0);
        mSetting.backTint = null;
        mSetting.backimage = null;	
        mExtraData.backBitmap = null;
        mCallback.onConfigSectionUpdate();
    }
  }

  private void selectTab(int index) {
    for (int i=0; i<3; i++) {
      mTabs[i].setSelected(i == index);
    }
    mSelectedIndex = index;
    mTintButton.setVisibility(mSelectedIndex == 2 ? View.VISIBLE : View.GONE);
    mStretchBtn.setVisibility(mSelectedIndex == 1 ? View.VISIBLE : View.GONE);

    // Update padding in the settings
    int[] src = mSelectedIndex == 1 ? mPaddingRect : new int[4];
    for (int i = 0; i < 4; i++) {
      mSetting.padding[i] = src[i];
    }
  }

  @Override
  public void onColorChanged(int color, View v) {
    mSetting.backTint = color;
    mCallback.onConfigSectionUpdate();
  }

  /**
   * Received pick image result.
   */
  @Override
  public void onResult(int requestCode, Intent data) {
    switch (requestCode) {
      case REQUEST_IMAGE: {
        new LargeImageLoadTask(mProxy) {

          @Override
          protected void onSuccess(Bitmap result) {
            if (BitmapUtils.saveBitmap(result,
                    new int[4],
                    FileProvider.tempBackImage(mProxy))) {
              mCurrentBitmap = result;
              mNinePatchRect = new int[4];
              mPaddingRect = new int[4];
            }
            setImageMode();
          }
        }.checkAndExecute(data.getData());
        break;
      }
      case REQUEST_9PATCH: {
        mNinePatchRect = data.getIntArrayExtra(KEY_STRETCH);
        mPaddingRect = data.getIntArrayExtra(KEY_PADDING);
        mPaddingRect[2] = mCurrentBitmap.getWidth() - mPaddingRect[2];
        mPaddingRect[3] = mCurrentBitmap.getHeight() - mPaddingRect[3];
        if (BitmapUtils.saveBitmap(mCurrentBitmap, mNinePatchRect, FileProvider.tempBackImage(mProxy))) {
          setImageMode();
        }
        break;
      }
    }
  }

  @Thunk void setImageMode() {
    setImageModeNoRefresh();
    mCallback.onConfigSectionUpdate();
  }

  @Thunk void setImageModeNoRefresh() {
    mSetting.backimage = Uri.parse(BACK_IMAGE_URI + mRefreshCount++);
    mSetting.backTint = null;
    mExtraData.backBitmap = mCurrentBitmap;
    selectTab(1);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (fromUser) {
      mSetting.backTrans = progress;
      mCallback.onConfigSectionUpdate();
    }
  }
  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }
  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void writeSettings(JSONObject json) throws JSONException {
    json.put(KEY_TRANSPARANCY, mBackTransSeek.getProgress())
        .put(KEY_DENSITY, mProxy.getResources().getDisplayMetrics().densityDpi);
    if (mSelectedIndex == 2) {
      json.put(KEY_TINT, mTintButton.getColor());
    } else if (mSelectedIndex == 1) {
      json.put(KEY_STRETCH, getStr(mNinePatchRect)).put(KEY_PADDING, getStr(mPaddingRect));
    }
  }

  @Override
  public void writeTheme(JSONObject json) throws JSONException {
    writeSettings(json);
  }

  @Override
  public void readTheme(SettingsDecoder decoder, Bitmap icon) {
    mCurrentBitmap = icon;
    mNinePatchRect = decoder.getRect(KEY_STRETCH);
    mPaddingRect = decoder.getRect(KEY_PADDING);
    BitmapUtils.saveBitmap(mCurrentBitmap, mNinePatchRect, FileProvider.tempBackImage(mProxy));
    
    int trans = decoder.getValue(KEY_TRANSPARANCY, mBackTransSeek.getProgress());
    mBackTransSeek.setProgress(trans);
    mSetting.backTrans = trans;
    
    setImageModeNoRefresh();
  }

  public int[] getCode() {
    return mNinePatchRect;
  }

  public static String getStr(int[] arr) {
    StringBuilder val = new StringBuilder().append(arr[0]);
    for (int i=1; i <4; i++) {
      val.append(',').append(arr[i]);
    }
    return val.toString();
  }
}
