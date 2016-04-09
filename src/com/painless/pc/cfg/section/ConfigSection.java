package com.painless.pc.cfg.section;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.WidgetSetting;

public abstract class ConfigSection {

  public static final int[] BUTTON_TRIPLET = new int[] { R.id.btn_1, R.id.btn_2, R.id.btn_3};

  @Thunk final TextView mHeaderView;

  protected final ConfigCallback mCallback;
  protected final View mContainerView;

  protected WidgetSetting mSetting;
  protected ConfigExtraData mExtraData;
  @Thunk boolean mIsCollapsed;

  public ConfigSection(Activity activity, ConfigCallback callback, int headerId, int containerId) {
    mCallback = callback;
    mContainerView = activity.findViewById(containerId);

    mHeaderView = (TextView) activity.findViewById(headerId);
    mHeaderView.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        mIsCollapsed = !mIsCollapsed;
        mContainerView.setVisibility(mIsCollapsed ? View.GONE : View.VISIBLE);
        mHeaderView.setCompoundDrawablesWithIntrinsicBounds(0, 0, mIsCollapsed ? R.drawable.icon_expand : R.drawable.icon_collapse, 0);
      }
    });

    mIsCollapsed = true;
    mContainerView.setVisibility(View.GONE);
    mHeaderView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_expand, 0);
  }

  public void readSetting(WidgetSetting setting, ConfigExtraData extraData) {
    mSetting = setting;
    mExtraData = extraData;
  }

  public abstract void writeSettings(JSONObject json) throws JSONException;

  public abstract void writeTheme(JSONObject json) throws JSONException;

  public abstract void readTheme(SettingsDecoder decoder, Bitmap icon);

  public static interface ConfigCallback {
    void onConfigSectionUpdate();
  }
}
