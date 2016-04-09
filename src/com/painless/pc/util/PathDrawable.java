package com.painless.pc.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.painless.pc.R;

public class PathDrawable extends Drawable {

  private static final int mPressedColor = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 0x742ab0e0 : 0x1aebebeb;

  private static final int FULL_SIZE = 32;
  private static final int RADIUS = 3;

  private final float mDensity;
  private final Paint mPaint;
  private final Path mPath;

  private final int mNormalColor;

  private boolean mPressed = false;

  public PathDrawable(String path, Context context, int size) {
    this(path, context, size, context.getResources().getColor(R.color.state_button_light));
  }

  public PathDrawable(String path, Context context, int size, int color) {
    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setStyle(Paint.Style.FILL);

    mNormalColor = color;
    mDensity = context.getResources().getDisplayMetrics().density;

    mPath = new Path();
    ParserHelper.parse(mPath, path);
    RectF bounds = new RectF();
    mPath.computeBounds(bounds, true);
    float scale = size * mDensity / Math.max(bounds.width(), bounds.height());
    Matrix m = new Matrix();
    m.setTranslate(-bounds.left, -bounds.top);
    m.postScale(scale, scale);
    m.postTranslate( - bounds.width() * scale / 2,  - bounds.height() * scale / 2);
    mPath.transform(m);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mPressed) {
      mPaint.setColor(mPressedColor);
      canvas.drawRoundRect(new RectF(getBounds()), RADIUS * mDensity, RADIUS * mDensity, mPaint);
    }

    mPaint.setColor(mPressed ? Color.WHITE : mNormalColor);
    Rect bounds = getBounds();
    canvas.save();
    canvas.translate(bounds.left + bounds.width() / 2, bounds.top + bounds.height() / 2);
    canvas.drawPath(mPath, mPaint);
    canvas.restore();
  }

  @Override
  public int getOpacity() {
    return 0;
  }

  @Override
  public void setAlpha(int alpha) { }

  @Override
  public void setColorFilter(ColorFilter cf) { }

  @Override
  public int getIntrinsicWidth() {
    return (int) (FULL_SIZE * mDensity);
  }

  @Override
  public int getIntrinsicHeight() {
    return getIntrinsicWidth();
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    boolean pressed = false;
    for (int s : state) {
      if ((s == android.R.attr.state_pressed) || (s == android.R.attr.state_focused)) {
        pressed = true;
        break;
      }
    }
    if (pressed != mPressed) {
      mPressed = pressed;
      return true;
    }
    return false;
  }
}
