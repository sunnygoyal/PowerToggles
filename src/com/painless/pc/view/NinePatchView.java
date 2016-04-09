package com.painless.pc.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class NinePatchView extends FrameLayout {

  private final Rect mSrc = new Rect();
  private final RectF mDest = new RectF();
  private final RectF mPatch = new RectF(0, 0, 1, 1);
  private final Paint mPaint;

  private Bitmap mImg;

  public NinePatchView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mPaint = new Paint();
    mPaint.setFilterBitmap(true);

    setWillNotDraw(false);
  }

  public void setBitmap(Bitmap img) {
    mImg = img;
  }

  public void setDim(float[] rect) {
    mPatch.set(rect[0], rect[1], rect[2], rect[3]);
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (mImg == null) {
      return;
    }
    
    final boolean isL = mPatch.left > 0;
    final boolean isT = mPatch.top > 0;
    final boolean isR = mPatch.right < 1;
    final boolean isB = mPatch.bottom < 1;

    final int imgW = mImg.getWidth();
    final int imgH = mImg.getHeight();
    final int w = getWidth();
    final int h = getHeight();

    final int x1 = (int) (mPatch.left * imgW);
    final int x2 = (int) (mPatch.right * imgW);
    final int y1 = (int) (mPatch.top * imgH);
    final int y2 = (int) (mPatch.bottom * imgH);

    final float x3 =  w - (imgW - x2);
    final float y3 =  h - (imgH - y2);

    if (isL) {
      if (isT) {
        mSrc.set(0, 0, x1, y1);
        mDest.set(mSrc);
        canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
      }
      
      mSrc.set(0, y1, x1, y2);
      mDest.set(0, y1, x1, y3);
      canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
      
      if (isB) {
        mSrc.set(0, y2, x1, imgH);
        mDest.set(0, y3, x1, h);
        canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
      }
    }

    if (isR) {
      if (isT) {
        mSrc.set(x2, 0, imgW, y1);
        mDest.set(x3, 0, w, y1);
        canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
      }
      
      mSrc.set(x2, y1, imgW, y2);
      mDest.set(x3, y1, w, y3);
      canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
      
      if (isB) {
        mSrc.set(x2, y2, imgW, imgH);
        mDest.set(x3, y3, w, h);
        canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
      }
    }

    if (isT) {
      mSrc.set(x1, 0, x2, y1);
      mDest.set(x1, 0, x3, y1);
      canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
    }

    if (isB) {
      mSrc.set(x1, y2, x2, imgH);
      mDest.set(x1, y3, x3, h);
      canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
    }
    
    mSrc.set(x1, y1, x2, y2);
    mDest.set(x1, y1, x3, y3);
    canvas.drawBitmap(mImg, mSrc, mDest, mPaint);
  }
}
