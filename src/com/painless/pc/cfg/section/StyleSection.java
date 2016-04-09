package com.painless.pc.cfg.section;

import static com.painless.pc.util.SettingsDecoder.DEFAULT_COLORS;
import static com.painless.pc.util.SettingsDecoder.KEY_BACK_STYLE;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ViewFlipper;

import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.theme.IndicatorRVFactory;
import com.painless.pc.theme.LabelRVFactory;
import com.painless.pc.theme.RVFactory;
import com.painless.pc.theme.SimpleRVFactory;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.UiUtils;
import com.painless.pc.util.WidgetSetting;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class StyleSection extends ConfigSection implements
OnClickListener, OnColorChangedListener, OnCheckedChangeListener, OnSeekBarChangeListener {

  private final boolean mIsNotification;
  private final View[] mTabs = new View[3];
  private final ViewFlipper mSectionFlipper;

  // Default section.
  private final CheckBox mChkWide;
  private final CheckBox mChkHugeIcon;

  // Indicator section.
  private final CheckBox mChkSameColor;
  private final ColorButton[] mIndicatorButtons = new ColorButton[3];

  // Labels section
  private final ColorButton mLabelColor;
  private final SeekBar mLabelSize;
  private final Context mContext;

  private int mSelectedIndex = -1;

  public StyleSection(Activity activity, ConfigCallback callback, boolean isNotification) {
    super(activity, callback, R.id.hdr_back_style, R.id.cnt_back_style);		
    mContext = activity;
    mIsNotification = isNotification;

    for (int i = 0; i < 3; i++) {
      mTabs[i] = mContainerView.findViewById(BUTTON_TRIPLET[i]);
      mTabs[i].setOnClickListener(this);
      mTabs[i].setTag(i);
    }

    mSectionFlipper = (ViewFlipper) mContainerView.findViewById(R.id.cfg_bs_flipper);

    mChkWide = (CheckBox) mContainerView.findViewById(R.id.chk_bs_wide);
    mChkWide.setOnCheckedChangeListener(this);
    mChkHugeIcon = (CheckBox) mContainerView.findViewById(R.id.chk_bs_hugeicon);
    mChkHugeIcon.setOnCheckedChangeListener(this);

    mChkSameColor = (CheckBox) mContainerView.findViewById(R.id.chk_bs_same_colors);
    mChkSameColor.setOnCheckedChangeListener(this);
    int[] btnColors = new int[] {R.id.btn_4, R.id.btn_5, R.id.btn_6};
    for (int i = 0; i < 3; i++) {
      mIndicatorButtons[i] = (ColorButton) mContainerView.findViewById(btnColors[i]);
      mIndicatorButtons[i].setColor(SettingsDecoder.DEFAULT_COLORS[i]);
      mIndicatorButtons[i].setOnColorChangeListener(this, true);
      mIndicatorButtons[i].setTag(i);
    }

    mLabelColor = (ColorButton) mContainerView.findViewById(R.id.cfg_lbl_color);
    mLabelColor.setColor(0xFFFFFFFF);
    mLabelColor.setOnColorChangeListener(this, true);
    mLabelSize = (SeekBar) mContainerView.findViewById(R.id.cfg_lbl_size);
    mLabelSize.setProgress(4);
    mLabelSize.setOnSeekBarChangeListener(this);
  }

  @Override
  public void readSetting(WidgetSetting setting, ConfigExtraData extraData) {
    super.readSetting(setting, extraData);

    // Set defaults
    for (int i=0; i<3; i++) {
      mIndicatorButtons[i].setColor(DEFAULT_COLORS[i]);
    }
    mLabelColor.setColor(0xFFFFFFFF);
    mLabelSize.setProgress(4);
    updateStyles(extraData.decoder);
    
    // Indicator colors
    if (setting.rvFactory instanceof IndicatorRVFactory) {
      selectTab(1);

    } else if (setting.rvFactory instanceof LabelRVFactory) {
      selectTab(2);

    } else {
      RVFactory rv = setting.rvFactory;
      UiUtils.setCheckbox((rv.layoutId == R.layout.aw_huge_icons_60) || (rv.layoutId == R.layout.aw_huge_icons), mChkHugeIcon, this);
      UiUtils.setCheckbox((rv.layoutId == R.layout.aw_default) || (rv.layoutId == R.layout.aw_huge_icons), mChkWide, this);
      selectTab(0);
    }
    mSectionFlipper.setInAnimation(null);
    mSectionFlipper.setOutAnimation(null);
    mSectionFlipper.setDisplayedChild(mSelectedIndex);
  }

  private void updateStyles(SettingsDecoder decoder) {
    // Label styles
    mLabelColor.setColor(decoder.getValue(LabelRVFactory.KEY_COLOR, mLabelColor.getColor()));
    mLabelSize.setProgress(decoder.getValue(LabelRVFactory.KEY_SIZE, mLabelSize.getProgress() + 8) - 8);

    // Indicator styles
    boolean customColor = decoder.hasValue(IndicatorRVFactory.KEY_CUSTOM_COLOR);
    for (int i=0; i<3; i++) {
      mIndicatorButtons[i].setColor(decoder.getValue(IndicatorRVFactory.KEY_MY_COLORS[i], mIndicatorButtons[i].getColor()));
      mIndicatorButtons[i].setEnabled(customColor);
    }
    UiUtils.setCheckbox(!customColor, mChkSameColor, this);
  }

  private void selectTab(int index) {
    for (int i=0; i<3; i++) {
      mTabs[i].setSelected(i == index);
    }
    mSelectedIndex = index;
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView == mChkSameColor) {
      for (int i = 0; i < 3; i++) {
        mIndicatorButtons[i].setEnabled(!isChecked);
      }
    }
    updateUI();
  }

  @Override
  public void onClick(View v) {
    Integer tag = (Integer) v.getTag();
    if (tag != mSelectedIndex) {
      if (tag > mSelectedIndex) {
        mSectionFlipper.setInAnimation(mContext, R.anim.right_slide_in);
        mSectionFlipper.setOutAnimation(mContext, R.anim.left_slide_out);
      } else {
        mSectionFlipper.setInAnimation(mContext, R.anim.left_slide_in);
        mSectionFlipper.setOutAnimation(mContext, R.anim.right_slide_out);
      }
      mSectionFlipper.setDisplayedChild(tag);
    }

    selectTab(tag);
    updateUI();
  }

  @Override
  public void onColorChanged(int color, View v) {
    updateUI();
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (fromUser) {
      updateUI();
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}
  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}

  private void updateUI() {
    updateUINoRefresh();
    mCallback.onConfigSectionUpdate();
  }

  private void updateUINoRefresh() {
    JSONObject json = new JSONObject();
    try {
      writeSettings(json);
    } catch (Exception e) {
      Debug.log(e);
    }
    SettingsDecoder decoder = new SettingsDecoder(json);
    mSetting.rvFactory = RVFactory.get(mContext, decoder, mIsNotification);
  }

  @Override
  public void writeSettings(JSONObject json) throws JSONException {
    writeTheme(json);
    int backStyle;
    switch (mSelectedIndex) {
      case 2:
        backStyle = 3;
        break;
      case 1:
        backStyle = 2;
        break;
      default:
        backStyle = 0;
        if (mChkHugeIcon.isChecked()) {
          json.put(SimpleRVFactory.KEY_LARGE_ICONS, true);
        }
        if (mChkWide.isChecked()) {
          json.put(SimpleRVFactory.KEY_FULL_HEIGHT, true);
        }
    }
    json.put(KEY_BACK_STYLE, backStyle);
  }

  @Override
  public void readTheme(SettingsDecoder decoder, Bitmap icon) {
    updateStyles(decoder);
    updateUINoRefresh();
  }

  @Override
  public void writeTheme(JSONObject json) throws JSONException {
    // Write settings corresponding to labels and indicator bars
    json.put(LabelRVFactory.KEY_COLOR, mLabelColor.getColor())
      .put(LabelRVFactory.KEY_SIZE, mLabelSize.getProgress() + 8);
    for (int i=0; i<3; i++) {
      json.put(IndicatorRVFactory.KEY_MY_COLORS[i], mIndicatorButtons[i].getColor());
    }
    if (!mChkSameColor.isChecked()) {
      json.put(IndicatorRVFactory.KEY_CUSTOM_COLOR, true);
    }
  }
}
