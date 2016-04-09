package com.painless.pc.util.prefs;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.painless.pc.GlobalFlags;
import com.painless.pc.R;
import com.painless.pc.view.MultiSeek;

public class HapticFeedbackPref implements OnSeekBarChangeListener, OnClickListener {

  private final CheckboxPref pf;
  private final SharedPreferences mPrefs;
 
  public final CheckedTextView view1;
  public final MultiSeek view2;
  
  public HapticFeedbackPref(LayoutInflater inflator, SharedPreferences prefs) {
    mPrefs = prefs;
    pf = new CheckboxPref(inflator, "heptic_feedback", prefs, R.string.as_haptic);
    view1 = pf.view;
    view1.setOnClickListener(this);

    int haptic = GlobalFlags.hapticLength(inflator.getContext()) / 10 - 1;
    view2 = new MultiSeek(inflator.getContext(), null);
    view2.setMax(15);
    view2.setProgress(Math.max(haptic, 0));
    view2.setOnSeekBarChangeListener(this);
    view2.changeLook(!view1.isChecked());
  }

  @Override
  public void onClick(View v) {
    pf.onClick(v);
    view2.changeLook(!view1.isChecked());
  }

  @Override
  public void onProgressChanged(SeekBar seek, int progress, boolean fromuser) { }

  @Override
  public void onStartTrackingTouch(SeekBar seek) {
    if (!view1.isChecked()) {
      onClick(view1);
    }
  }

  @Override
  public void onStopTrackingTouch(SeekBar seek) {
    mPrefs.edit().putInt("haptic_len", (seek.getProgress() +1)*10).commit();
    GlobalFlags.clearFlags();
    GlobalFlags.playHaptic(seek.getContext());
  }
}
