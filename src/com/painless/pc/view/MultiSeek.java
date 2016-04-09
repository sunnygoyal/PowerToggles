package com.painless.pc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * A {@link SeekBar} which allows the UI to look as disabled without actually
 * disabling the view.
 */
public class MultiSeek extends SeekBar {

  private boolean mDisabled = false;

  public MultiSeek(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void changeLook(boolean isDisabled) {
    if (mDisabled != isDisabled) {
      mDisabled = isDisabled;
      refreshDrawableState();
      invalidate();
    }
  }

  @Override
  protected int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (mDisabled) {
      int N = drawableState.length;
      boolean foundEnabled = false;

      // Shift all values starting from the enabled state.
      for (int i = 0; i < N; i++) {
        if (drawableState[i] == android.R.attr.state_enabled) {
          foundEnabled = true;
        } else if (foundEnabled) {
          drawableState[i - 1] = drawableState[i];
        }
      }
    }
    return drawableState;
  }
}
