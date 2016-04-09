package com.painless.pc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class ThreeView extends RelativeLayout {

  public ThreeView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int w = MeasureSpec.getSize(widthMeasureSpec);
    if (w > 0) {
      if (w > 0) {
        int cW = w / 3;
        for (int i = 0; i < 3; i++) {
          getChildAt(i).getLayoutParams().width = cW;
        }
      }
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}
