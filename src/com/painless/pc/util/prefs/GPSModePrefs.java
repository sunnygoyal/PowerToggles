package com.painless.pc.util.prefs;

import static com.painless.pc.tracker.GpsStateTracker.DEFAULT_SOURCES;
import static com.painless.pc.tracker.GpsStateTracker.KEY_SOURCES;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.view.LayoutInflater;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;

public class GPSModePrefs extends AbstractPopupPref implements OnMultiChoiceClickListener {


  private final boolean[] mCheckedItems = new boolean[4];

  public GPSModePrefs(LayoutInflater inflator, SharedPreferences prefs) {
    super(inflator, prefs, R.string.ts_gps_sources, 0);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    int[] result = new int[2];
    int c1 = getCheckedIndex(0);
    result[0] = 3 - c1;
    result[1] = 3 - getCheckedIndex(c1 + 1);
    mPrefs.edit().putString(KEY_SOURCES, result[0] +"," + result[1]).commit();
  }

  @Override
  public AlertDialog showBuilder(Builder builder) {
    int[] parts = ParseUtil.parseIntArray(new int[2], mPrefs.getString(KEY_SOURCES, DEFAULT_SOURCES));
    mCheckedItems[0] = parts[0] == 3;
    mCheckedItems[1] = (parts[0] == 2) || (parts[1] == 2);
    mCheckedItems[2] = (parts[0] == 1) || (parts[1] == 1);
    mCheckedItems[3] = parts[1] == 0;

    return builder.setMultiChoiceItems(R.array.ts_gps_source_op, mCheckedItems, this).show();
  }

  @Override
  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
    mCheckedItems[which] = isChecked;
    int count = getCheckedCount();

    mOKButton.setEnabled((count == 2) ||
            ((count == 1) && (mCheckedItems[1] || mCheckedItems[2])));
  }

  private int getCheckedCount() {
    int count = 0;
    for (int i=0; i<4; i++) {
      count += mCheckedItems[i] ? 1 : 0;
    }
    return count;
  }

  private int getCheckedIndex(int start) {
    for (int i = start; i < 4; i++) {
      if (mCheckedItems[i]) return i;
    }
    return 4;
  }
}
