package com.painless.pc.nav;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.view.View;

import com.painless.pc.LockAdmin;
import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.singleton.RootTools;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.tracker.AbstractTracker;

public class InfoFrag extends PreferenceFragment implements OnPreferenceChangeListener {

  private SwitchPreference mDeviceAdminPref;

  private DevicePolicyManager mDPM;
  private ComponentName mCN;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.app_info);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Context context = view.getContext();

    PreferenceCategory infoCatgory = (PreferenceCategory) findPreference("pref_info");
    PCWidgetActivity.partialUpdateAllWidgets(context);
    updateInfoPref(infoCatgory, 0, 4, PCWidgetActivity.sPollBattery);
    updateInfoPref(infoCatgory, 1, 2, RootTools.isRooted());
    
    // Device admin pref
    mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    mCN = new ComponentName(context, LockAdmin.class);
    mDeviceAdminPref = new SwitchPreference(context) {
      @Override
      protected void onBindView(View view) {
        super.onBindView(view);
      }
    };
    mDeviceAdminPref.setTitle(R.string.stat_admin);
    mDeviceAdminPref.setSummaryOn(R.string.stat_admin_msg_on);
    mDeviceAdminPref.setSummaryOff(R.string.stat_admin_msg_off);
    mDeviceAdminPref.setOnPreferenceChangeListener(this);
    mDeviceAdminPref.setOrder(0);
    infoCatgory.addPreference(mDeviceAdminPref);
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    if (preference == mDeviceAdminPref) {
      if (newValue == Boolean.FALSE) {
        mDPM.removeActiveAdmin(mCN);
        return true;
      } else {
        startActivity(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCN)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explain)));
      }
    }
    return false;
  }

  private void updateInfoPref(PreferenceCategory category, int index, int helpId, boolean isOn) {
    TwoStatePreference pref = (TwoStatePreference) category.getPreference(index);
    pref.setChecked(isOn);
    pref.setOnPreferenceChangeListener(this);
    pref.setIntent(getHelpIntent(helpId));
  }

  @Override
  public void onResume() {
    mDeviceAdminPref.setChecked(mDPM.isAdminActive(mCN));
    super.onResume();

    int activeTrackerCount = 0;
    for (AbstractTracker tracker : SettingStorage.getCache()) {
      if (tracker != null) {
        activeTrackerCount++;
      }
    }
    PreferenceScreen screen = (PreferenceScreen) findPreference("active_toggles");
    if (activeTrackerCount > 0) {
      screen.setEnabled(true);
      screen.setSummary(getString(R.string.stat_active_toggle_n, activeTrackerCount));
    } else {
      screen.setEnabled(false);
      screen.setSummary(R.string.stat_active_toggle_none);
    }
  }

  public static Intent getHelpIntent(int helpId) {
    return new Intent(Intent.ACTION_VIEW)
      .setData(Uri.parse("http://powertoggles.com/help?e=" + helpId))
      .putExtra("android.support.customtabs.extra.SESSION", "")
      .putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", 0xff212121);

  }
}
