package com.painless.pc.picker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.painless.pc.R;
import com.painless.pc.singleton.BitmapUtils;

public class FolderIconCreater {

  private static final int SHADOW_COLOR = 0x55000000;

  private static final float MIN_COLOR_SATURATION = 0.1f;
  private static final float MAX_COLOR_BRIGHTNESS = 0.9f;
  private static final float MAX_WHITE_BRIGHTNESS = 0.4f;

  private static final int NUM_ITEMS_IN_PREVIEW = 3;
  // The degree to which the item in the back of the stack is scaled [0...1]
  // (0 means it's not scaled at all, 1 means it's scaled to nothing)
  private static final float PERSPECTIVE_SCALE_FACTOR = 0.35f;
  // The amount of vertical spread between items in the stack [0...1]
  private static final float PERSPECTIVE_SHIFT_FACTOR = 0.18f;

  private final Resources mRes;
  final int mSize;
  private final int mIconSize;

  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Canvas mCanvas = new Canvas();

  private final Path mRoundRect;

  // These variables are all associated with the drawing of the preview; they are stored
  // as member variables for shared usage and to avoid computation on each frame
  private final float mBaselineIconScale;
  private final int mBaselineIconSize;
  private final float mMaxPerspectiveShift;

  public FolderIconCreater(Context c) {
    mRes = c.getResources();
    mSize = BitmapUtils.getActivityIconSize(c);
    mIconSize = mSize * 3 / 5;

    mRoundRect = new Path();
    float gap = (mSize - mIconSize) * 0.25f;
    mRoundRect.addRoundRect(new RectF(gap, gap, mSize - gap, mSize - gap), gap / 2, gap / 2, Direction.CW);

    // Init variables
    // cos(45) = 0.707  + ~= 0.1) = 0.8f
    mBaselineIconScale = (1 + 0.8f) / (2 * (1 + PERSPECTIVE_SHIFT_FACTOR));
    mBaselineIconSize = (int) (mSize * mBaselineIconScale);
    mMaxPerspectiveShift = mBaselineIconSize * PERSPECTIVE_SHIFT_FACTOR;
  }

  public Bitmap createIcon(int resId, int bgColor) {
    Bitmap center = BitmapFactory.decodeResource(mRes, resId).extractAlpha();
    Bitmap center2 = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);

    mPaint.setColor(Color.BLACK);
    int shift = (mSize - mIconSize) / 2;
    mCanvas.setBitmap(center2);
    mCanvas.drawBitmap(center,
        new Rect(0, 0, center.getWidth(), center.getHeight()),
        new Rect(shift, shift, shift + mIconSize, shift + mIconSize),
        mPaint);

    Bitmap shadow = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
    int total = mSize * mSize;
    int[] pixels = new int[total];
    center2.getPixels(pixels, 0, mSize, 0, 0, mSize, mSize);

    shift = mSize + 1;
    int col = 0;
    for (int i = 0; i < total; i++) {
      col++;
      if (col == mSize) {
        col = 0;
      }
      if (((i - shift) >= 0) && (col > 0) && (Color.alpha(pixels[i - shift]) > 20)) {
        pixels[i] = SHADOW_COLOR;
      }
    }
    shadow.setPixels(pixels, 0, mSize, 0, 0, mSize, mSize);

    Bitmap finalImg = createBase(bgColor);
    mCanvas.setBitmap(finalImg);
    mCanvas.save();
    mCanvas.clipPath(mRoundRect);
    mCanvas.drawBitmap(shadow, 0, 0, null);

    mPaint.setColor(Color.WHITE);
    mCanvas.drawBitmap(center2.extractAlpha(), 0, 0, mPaint);
    mCanvas.restore();
    mCanvas.setBitmap(null);
    return finalImg;
  }

  /**
   * Creates a rounded rect with the given color.
   */
  private Bitmap createBase(int color) {
      Bitmap finalImg = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);

      mCanvas.setBitmap(finalImg);
      mCanvas.save();

      // Normalize color
      float[] hsv = new float[3];
      Color.colorToHSV(color, hsv);
      if (hsv[1] == 0) {
        hsv[2] = Math.min(hsv[2], MAX_WHITE_BRIGHTNESS);
      } else {
        hsv[2] = Math.min(hsv[2], MAX_COLOR_BRIGHTNESS);
        hsv[1] = Math.max(MIN_COLOR_SATURATION, hsv[1]);
      }
      int colorMid = Color.HSVToColor(hsv);
      hsv[2] += 0.1f;
      int colorTop = Color.HSVToColor(hsv);
      hsv[2] -= 0.2f;
      int colorBot = Color.HSVToColor(hsv);
      float edge = mSize * 0.01f;

      mCanvas.translate(-edge, -edge);
      mPaint.setColor(colorTop);
      mCanvas.drawPath(mRoundRect, mPaint);

      mPaint.setColor(colorBot);
      mPaint.setXfermode(new PorterDuffXfermode(Mode.XOR));
      mCanvas.translate(edge, edge);
      mCanvas.drawPath(mRoundRect, mPaint);

      mPaint.setColor(colorMid);
      mPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OVER));
      mCanvas.drawPath(mRoundRect, mPaint);
      mCanvas.restore();
      mCanvas.setBitmap(null);
      mPaint.setXfermode(null);

      return finalImg;
  }

  public Bitmap createFolderIcon(int resId, int[] colors) {
      Bitmap icon = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
      Bitmap icon1 = createBase(colors[0]);
      Bitmap icon2 = createBase(colors[1]);
      Bitmap icon3 = createIcon(resId, colors[2]);

      Bitmap bg = BitmapFactory.decodeResource(mRes, R.drawable.portal_ring_inner_holo);
      mCanvas.setBitmap(icon);

      mCanvas.drawBitmap(bg,
              new Rect(0, 0, bg.getWidth(), bg.getHeight()),
              new Rect(0, 0, mSize, mSize),
              mPaint);

      drawPreview(2, icon1);
      drawPreview(1, icon2);
      drawPreview(0, icon3);

      mCanvas.setBitmap(null);

//      try {
//        FileOutputStream out = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "folder.png"));
//        icon.compress(CompressFormat.PNG, 100, out);
//        out.close();
//      } catch (Exception e) {
//        Debug.log(e);
//      }
      return icon;
  }

  private void drawPreview(int index, Bitmap icon) {
      index = NUM_ITEMS_IN_PREVIEW - index - 1;
      float r = (index * 1.0f) / (NUM_ITEMS_IN_PREVIEW - 1);
      float scale = (1 - PERSPECTIVE_SCALE_FACTOR * (1 - r));

      float offset = (1 - r) * mMaxPerspectiveShift;
      float scaledSize = scale * mBaselineIconSize;
      float scaleOffsetCorrection = (1 - scale) * mBaselineIconSize;

      // We want to imagine our coordinates from the bottom left, growing up and to the
      // right. This is natural for the x-axis, but for the y-axis, we have to invert things.
      float transY = mSize - (offset + scaledSize + scaleOffsetCorrection);
      float transX = (mSize - scaledSize) / 2;
      float totalScale = mBaselineIconScale * scale;
      final int overlayAlpha = (int) (80 * (1 - r));

      mCanvas.save();
      mCanvas.translate(transX, transY);
      mCanvas.scale(totalScale, totalScale);

      mPaint.setColorFilter(new PorterDuffColorFilter(
              Color.argb(overlayAlpha, 255, 255, 255), PorterDuff.Mode.SRC_ATOP));
      mCanvas.drawBitmap(icon, 0, 0, mPaint);
      mPaint.setColorFilter(null);
      mCanvas.restore();
  }
}
