package com.painless.pc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class CenterLayout extends LinearLayout {

  private final int mMaxWidth;

  private final Paint mPaint, mPaint2;
  private final Rect mDrawRect;

  public CenterLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    mMaxWidth = (int) (((getOrientation() == HORIZONTAL) ? 900 : 500) *
            getResources().getDisplayMetrics().density);

    setWillNotDraw(false);

    mPaint = new Paint();
    mPaint.setColor(0xFF383838);

    mPaint2 = new Paint();
    mPaint2.setShader(new LinearGradient(0, 0, 20 * getResources().getDisplayMetrics().density, 0, 0xFF222222, 0x000000, TileMode.CLAMP));

    mDrawRect = new Rect();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mDrawRect.bottom = MeasureSpec.getSize(heightMeasureSpec);
    int width = MeasureSpec.getSize(widthMeasureSpec);
    mDrawRect.right = width > mMaxWidth ? (width - mMaxWidth) / 2 : 0;
    setPadding(mDrawRect.right, 0, mDrawRect.right, 0);
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    for (int i = -1; i < 2; i++) {
      canvas.save();
      canvas.scale(i, 1, getWidth() / 2, 0);
      canvas.drawRect(mDrawRect, mPaint);
      canvas.scale(-1, 1, mDrawRect.centerX(), 0);
      canvas.drawRect(mDrawRect, mPaint2);
      canvas.restore();
    }
  }
}
