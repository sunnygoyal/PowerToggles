package com.painless.pc.cfg.section;

import static com.painless.pc.util.SettingsDecoder.KEY_CLICK_FEEDBACK;
import static com.painless.pc.util.SettingsDecoder.KEY_FLAT_CORNER;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.UiUtils;
import com.painless.pc.util.WidgetSetting;

public class ClickFeedbackSection extends ConfigSection implements OnItemSelectedListener, OnCheckedChangeListener {

  private final Spinner mClickType;
  private final CheckBox mCorners;

  public ClickFeedbackSection(Activity activity, ConfigCallback callback) {
    super(activity, callback, R.id.hdr_click_feedback, R.id.cnt_click_feedback);

    mClickType = (Spinner) mContainerView.findViewById(R.id.spinner_type);
    mClickType.setOnItemSelectedListener(this);
    mCorners = (CheckBox) mContainerView.findViewById(R.id.chk_cf_corner);
    mCorners.setOnCheckedChangeListener(this);
  }

  @Override
  public void readSetting(WidgetSetting setting, ConfigExtraData extraData) {
    super.readSetting(setting, extraData);

    mClickType.setOnItemSelectedListener(null);
    int selection = setting.clickFeedback[1] == 0 ? 2 :
      (setting.clickFeedback[1] == R.drawable.wbg_kitkat_center ? 0 : 1);

    if (Globals.IS_LOLLIPOP) {
      if (setting.clickFeedback[1] == R.drawable.wbg_ripple_center) {
        selection = 0;
      } else {
        selection ++;
      }
    }
    mClickType.setSelection(selection);
    mClickType.setOnItemSelectedListener(this);
    UiUtils.setCheckbox(!setting.isFlatCorners, mCorners, this);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    updateUI(true);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) { }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    updateUI(true);
  }

  private void updateUI(boolean refresh) {
    JSONObject json = new JSONObject();
    try {
      writeSettings(json);
    } catch (Exception e) {
      Debug.log(e);
    }
    SettingsDecoder decoder = new SettingsDecoder(json);
    mSetting.updateClickFeedback(decoder);
    if (refresh) {
      mCallback.onConfigSectionUpdate();
    }
  }

  @Override
  public void writeSettings(JSONObject json) throws JSONException {
    json.put(KEY_CLICK_FEEDBACK, mClickType.getAdapter().getCount() - mClickType.getSelectedItemPosition() - 1);
    writeTheme(json);
  }

  @Override
  public void writeTheme(JSONObject json) throws JSONException {
    // Click feedback is not written in settings
    if (!mCorners.isChecked()) {
      json.put(KEY_FLAT_CORNER, true);
    }
  }

  @Override
  public void readTheme(SettingsDecoder decoder, Bitmap icon) {
    // Only read the round corner setting
    UiUtils.setCheckbox(!decoder.hasValue(KEY_FLAT_CORNER), mCorners, this);
    updateUI(false);
  }
}
