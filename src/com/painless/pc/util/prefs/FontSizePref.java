package com.painless.pc.util.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.widget.SeekBar;

import com.painless.pc.R;
import com.painless.pc.util.SeekNumberPicker;


public class FontSizePref extends SeekNumberPicker {

  private static final int MIN_VALUE = 5;
  private static final int MAX_VALUE = 50;
  private static final int DELTA_STEPS = 5;
  private static final int SEEK_MAX = (MAX_VALUE - MIN_VALUE) / DELTA_STEPS; // == 9

  private final SharedPreferences mPrefs;

  public FontSizePref(Context context, SharedPreferences prefs) {
    super(context);
    mPrefs = prefs;

    mValuePreview.setInputType(InputType.TYPE_NULL);
    mValuePreview.setEnabled(false);
    mValuePreview.setFocusable(false);
    mValuePreview.setBackgroundResource(0);
    mValuePreview.setTextColor(Color.WHITE);
    mValuePreview.removeTextChangedListener(this);

    setSummary(R.string.ts_font_delta);
    setMax(SEEK_MAX);

    int value = mPrefs.getInt("font_delta_step", 15);
    value = Math.max(MIN_VALUE, Math.min(value, MAX_VALUE));
    setValue((value - MIN_VALUE) / DELTA_STEPS);

  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress,
          boolean fromUser) {
    int value = progress * DELTA_STEPS + MIN_VALUE;
    mValuePreview.setText(value + "%");
    if (fromUser) {
      mPrefs.edit().putInt("font_delta_step", value).commit();
    }
  }
}
