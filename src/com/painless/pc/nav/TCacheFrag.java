package com.painless.pc.nav;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.tracker.AbstractTracker;

public class TCacheFrag extends PreferenceFragment implements OnPreferenceClickListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.tcache);

    getPreferenceScreen().getPreference(0).setOnPreferenceClickListener(this);
    updateCachedToggleList(getActivity());
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    // Reset cache.
    SettingStorage.clearCache();
    PCWidgetActivity.updateAllWidgets(preference.getContext(), true);
    SettingStorage.updateConnectivityReceiver(preference.getContext());

    PreferenceScreen screen = getPreferenceScreen();
    screen.removeAll();
    screen.addPreference(preference);
    updateCachedToggleList(preference.getContext());
    return true;
  }

  private void updateCachedToggleList(Context context) {
    PreferenceScreen screen = getPreferenceScreen();

    String[] trackerNames = getResources().getStringArray(R.array.tracker_names);
    SharedPreferences prefs = Globals.getAppPrefs(context);

    long now = System.currentTimeMillis();
    for (AbstractTracker tracker : SettingStorage.getCache()) {
      if (tracker == null) {
        continue;
      }
      long usedTime = prefs.getLong("last_used_" + tracker.trackerId, 0);

      Preference trackerEntry = new Preference(context);
      trackerEntry.setTitle(tracker.getLabel(trackerNames));
      trackerEntry.setIcon(tracker.buttonConfig[1]);
      trackerEntry.setSummary(getString(R.string.stat_last_used, Globals.getTimeDiff(usedTime, context)));
      trackerEntry.setOrder((int) ((now - usedTime) / 1000));
      screen.addPreference(trackerEntry);
    }
  }
}
