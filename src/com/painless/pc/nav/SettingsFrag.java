package com.painless.pc.nav;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import com.painless.pc.GlobalFlags;
import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.TrackerManager;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.prefs.BatteryPollPref;
import com.painless.pc.util.prefs.CheckboxPref;
import com.painless.pc.util.prefs.HapticFeedbackPref;
import com.painless.pc.util.prefs.TapSpeedPref;

public class SettingsFrag extends Fragment implements OnClickListener {

  protected Context mContext;
  protected SharedPreferences mPrefs;
  protected LayoutInflater mInflator;

  @Thunk LinearLayout mRoot;
  @Thunk ScrollView mRootScroll;

  @Override
  public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mInflator = inflater;
    mContext = inflater.getContext();
    mPrefs = Globals.getAppPrefs(mContext);

    mRootScroll = (ScrollView) inflater.inflate(R.layout.ts_fragment, container, false);
    mRoot = (LinearLayout) mRootScroll.findViewById(R.id.rootLayout);
    buildScreen();
    return mRootScroll;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    for (int i=0; i<TrackerManager.TRACKER_LIST.length; i++) {
      AbstractTracker tracker = SettingStorage.maybeGetTracker(i);
      if (tracker != null) {
        tracker.init(mPrefs);
      }
    }
    GlobalFlags.clearFlags();
    PCWidgetActivity.updateAllWidgets(mContext, true);
  }

  @Override
  public void onClick(View v) {
    HeaderTag tag = (HeaderTag) v.getTag();
    final int index = mRoot.indexOfChild((View) v.getParent()) + 1;

    if (tag.opened) {
      mRoot.removeViewAt(index);
    } else {
      View settingView = getPrefView(tag.id);
      if (settingView == null) {
        return;
      }

      LinearLayout mid = (LinearLayout) mInflator.inflate(R.layout.ts_section_middle, mRoot, false);
      mid.addView(settingView);
      mRoot.addView(mid, index);
      mRoot.postDelayed(new Runnable() {

        @Override
        public void run() {
          int next = index + 2;
          if (mRoot.getChildCount() <= next) {
            mRootScroll.fullScroll(View.FOCUS_DOWN);
          } else {
            View bottom = mRoot.getChildAt(next);
            if ((mRootScroll.getScrollY() + mRootScroll.getHeight()) < bottom.getTop()) {
              int top = bottom.getTop() - mRootScroll.getHeight();
              mRootScroll.smoothScrollTo(0, top);
            }
          }
        }
      }, 500);
    }
    
    tag.opened = !tag.opened;
    ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(tag.iconId, 0,
        tag.opened ? R.drawable.icon_collapse : R.drawable.icon_expand, 0);
  }

  protected void buildScreen() {
    onClick(addSection(R.drawable.icon_prefs, 0, getString(R.string.as_general)));
    addSection(R.drawable.icon_battery, 1, getString(R.string.as_battery_service));
    addSection(R.drawable.icon_double_tap, 2, getString(R.string.as_double_tap));
    addSection(0, 3, getString(R.string.ts_title))
      .setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_toggle, 0, R.drawable.icon_nav_next, 0);
  }

  protected View getPrefView(int id) {
    switch(id) {
      case 0:  // General settings
        HapticFeedbackPref hf = new HapticFeedbackPref(mInflator, mPrefs);
        return wrapContent(
            hf.view1, hf.view2,
              new CheckboxPref(mInflator, "partial_update", mPrefs, R.string.as_optimize, true).view,
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? new CheckboxPref(mInflator, "prompt_permission", mPrefs, R.string.pp_pref).view : null);
      case 1:  // Battery service
        return wrapContent(
            new BatteryPollPref(mInflator, mPrefs).view,
            new CheckboxPref(mInflator, "motorola_hack", mPrefs, R.string.as_motorola_hack).view);
      case 2:  // Double Click
        int[] dcToggles = new int[] {1, 3, 0, 2, 6};
        String trackerLables[] = getResources().getStringArray(R.array.tracker_names);
        View[] views = new View[dcToggles.length + 1];
        views[0] = new TapSpeedPref(mInflator, mPrefs).view;
        for (int i=0; i<dcToggles.length; i++) {
          int tId = dcToggles[i];
          views[i + 1] = new CheckboxPref(mInflator, "doubleClick-" + tId, mPrefs, trackerLables[tId]).view;
        }
        return wrapContent(views);
      case 3:
        // Start TogglePrefFragment
        if (getActivity() instanceof PreferenceActivity) {
          ((PreferenceActivity) getActivity()).startPreferencePanel(
                  TogglePrefFrag.class.getName(),
                  new Bundle(),
                  R.string.ts_title,
                  "",
                  null,
                  0);
        }
    }
    return null;
  }

  protected final TextView addSection(int icon, int id, String title) {
    HeaderTag tag = new HeaderTag();
    tag.iconId = icon;
    tag.id = id;

    ViewGroup headerContainer = (ViewGroup) mInflator.inflate(R.layout.ts_section_header, mRoot, false);
    TextView header = (TextView) headerContainer.findViewById(android.R.id.text1);
    header.setText(title);
    header.setTag(tag);
    header.setOnClickListener(this);
    header.setCompoundDrawablesWithIntrinsicBounds(tag.iconId, 0, R.drawable.icon_expand, 0);
    mRoot.addView(headerContainer);

    // Add footer
    mInflator.inflate(R.layout.ts_section_bottom, mRoot);
    return header;
  }

  protected final View wrapContent(View... views) {
    return getWrapper(mContext, views);
  }

  /**
   * Creates a wrapper over all the view by adding them into a linear layout
   */
  public static View getWrapper(Context context, View... views) {
    LinearLayout wrapper = new LinearLayout(context);
    wrapper.setOrientation(LinearLayout.VERTICAL);
    wrapper.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    for (View v : views) {
      if (v != null) {
        wrapper.addView(v);
      }
    }
    return wrapper;
  }

  /**
   * A simple class to store information about headings
   */
  @Thunk static class HeaderTag {
    int iconId;
    int id;
    boolean opened = false;
  }
}
