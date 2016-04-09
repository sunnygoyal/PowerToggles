package com.painless.pc.view;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.painless.pc.R;
import com.painless.pc.graphic.AlphaPatternDrawable;

public class RectView extends View {

  private static final float MIN_SIZE = 0.2f;
  private static final int LEFT = 0;
  private static final int TOP = 1;
  private static final int RIGHT = 2;
  private static final int BOTTOM = 3;

	private final Paint mBackgroundPaint = new Paint();
  private final RectF mDestImageRect = new RectF();

	private final Paint mCirclePaint = new Paint();
	private final Paint mRectPaint;
  private final float mCircleDim;

  private final float[] mDims = new float[2];
  private final float[] mAppliedRect = new float[4];
  private final float[] mDisplayRect = new float[4];
  private final float[] mStartRect = new float[4];
	private final boolean[] mMovingDims = new boolean[4];

	private boolean isMoving;
	private float startX, startY;

	private Bitmap imageBack;
	private Rect imageSrcRect;
	private RectListener mListener;

  public RectView(Context context, AttributeSet attrs) {
		super(context, attrs);

		Resources res = context.getResources();
		float density = res.getDisplayMetrics().density;

		mCircleDim = 3 * density;
		mCirclePaint.setColor(res.getColor(R.color.state_button_dark));
		mRectPaint = new Paint(mCirclePaint);
		mRectPaint.setStrokeWidth(2 * density);
		mRectPaint.setStyle(Paint.Style.STROKE);
		setBackgroundDrawable(new AlphaPatternDrawable((int) (7 * density))); 
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	  super.onSizeChanged(w, h, oldw, oldh);
	  if (w > 0 && h > 0) {
	    mDestImageRect.set(0, 0, w, h);
	    mDims[0] = w;
	    mDims[1] = h;
	    for (int i = 0; i < 4; i++) {
	      mDisplayRect[i] = mAppliedRect[i] * mDims[i & 1];
	    }
	  }
	}

	public void setBackTint(int color) {
    mBackgroundPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (imageBack != null) {
			canvas.drawBitmap(imageBack, imageSrcRect, mDestImageRect, mBackgroundPaint);
		}

		canvas.drawRect(mDisplayRect[LEFT], mDisplayRect[TOP], mDisplayRect[RIGHT], mDisplayRect[BOTTOM], mRectPaint);

		// draw circles
    canvas.drawCircle(mDisplayRect[LEFT], mDisplayRect[TOP], mCircleDim, mCirclePaint);
    canvas.drawCircle(mDisplayRect[LEFT], mDisplayRect[BOTTOM], mCircleDim, mCirclePaint);
    canvas.drawCircle(mDisplayRect[RIGHT], mDisplayRect[TOP], mCircleDim, mCirclePaint);
    canvas.drawCircle(mDisplayRect[RIGHT], mDisplayRect[BOTTOM], mCircleDim, mCirclePaint);

    float xMid = (mDisplayRect[LEFT] + mDisplayRect[RIGHT]) / 2;
    float yMid = (mDisplayRect[TOP] + mDisplayRect[BOTTOM]) / 2;
    canvas.drawCircle(xMid, mDisplayRect[TOP], mCircleDim, mCirclePaint);
    canvas.drawCircle(xMid, mDisplayRect[BOTTOM], mCircleDim, mCirclePaint);
    canvas.drawCircle(mDisplayRect[LEFT], yMid, mCircleDim, mCirclePaint);
    canvas.drawCircle(mDisplayRect[RIGHT], yMid, mCircleDim, mCirclePaint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case ACTION_DOWN:
				applyMoveData();
				getParent().requestDisallowInterceptTouchEvent(true);
				isMoving = true;
				startX = event.getX();
				startY = event.getY();
				copyRect(mDisplayRect, mStartRect);

				mMovingDims[LEFT] = startX < mDisplayRect[RIGHT];
				mMovingDims[TOP] = startY < mDisplayRect[BOTTOM];
				mMovingDims[RIGHT] = startX > mDisplayRect[LEFT];
				mMovingDims[BOTTOM] = startY > mDisplayRect[TOP];

				if ((mMovingDims[LEFT] ^ mMovingDims[TOP] ^ mMovingDims[RIGHT] ^ mMovingDims[BOTTOM])) {
					if (mMovingDims[LEFT] && mMovingDims[RIGHT]) {
						mMovingDims[LEFT] = mMovingDims[RIGHT] = false;
					}
					if (mMovingDims[TOP] && mMovingDims[BOTTOM]) {
						mMovingDims[TOP] = mMovingDims[BOTTOM] = false;
					}
				}

				break;
			case ACTION_MOVE:
				if (!isMoving) {
					break;
				}

				float[] delta = new float[] {
				        event.getX() - startX,
				        event.getY() - startY
				};

				for (int i = 0; i < 4; i++) {
				  if (mMovingDims[i]) {
				    mDisplayRect[i] = Math.max(0, Math.min(mDims[i & 1], mStartRect[i] + delta[i & 1]));
				  }
				}
				for (int i = 0; i < 2; i++) {
					int second = i + 2;
					if ((mDisplayRect[second] - mDisplayRect[i]) <= MIN_SIZE) {
						if (mDisplayRect[second] == 1) {
							mDisplayRect[i] = 1 - MIN_SIZE;

						} else if (mDisplayRect[i] == 0) {
							mDisplayRect[second] = MIN_SIZE;

						} else {
							if (mMovingDims[i]) {
								mDisplayRect[i] = mDisplayRect[second] - MIN_SIZE;
							} else {
								mDisplayRect[second] = mDisplayRect[i] + MIN_SIZE;
							}
						}
					}
				}
				applyMoveData();
				if (mListener != null) {
				  mListener.onRectChange(mAppliedRect, this);
				}
				break;

			case ACTION_UP:
		    isMoving = false;
				break;
		}
		invalidate();
		return true;
	}

	private void applyMoveData() {
		if ((mDims[0] > 0) && (mDims[1] > 0)) {
  		for (int i=0; i<4; i++) {
  			mAppliedRect[i] = mDisplayRect[i] / mDims[i & 1];
  		}
		}
	}

	public void setBitmap(Bitmap img) {
		imageBack = img;
		imageSrcRect = new Rect(0, 0, img.getWidth(), img.getHeight());
		invalidate();
	}

	public Bitmap getBitmap() {
		return imageBack;
	}

	public void updateRect(RectF rect, int sizeX, int sizeY) {
	  mAppliedRect[LEFT] = rect.left / sizeX;
	  mAppliedRect[TOP] = rect.top / sizeY;
	  mAppliedRect[RIGHT] = rect.right / sizeX;
	  mAppliedRect[BOTTOM] = rect.bottom / sizeY;
    for (int i = 0; i < 4; i++) {
      mDisplayRect[i] = mAppliedRect[i] * mDims[i & 1];
    }
    invalidate();
	}

	public void updateRect(float[] rect) {
	  for (int i =0; i < 4; i++) {
	    mAppliedRect[i] = rect[i];
      mDisplayRect[i] = mAppliedRect[i] * mDims[i & 1];
	  }
    invalidate();
	}

	public float[] getAppliedRect() {
	  return mAppliedRect;
	}

	public void setListener(RectListener listener) {
		this.mListener = listener;
	}

	/**
	 * An interface for rect area change listener
	 */
	public static interface RectListener {

	  void onRectChange(float[] rect, View v);
	}

	private static void copyRect(float[] src, float[] dest) {
	  for (int i = 0; i < 4; i++) {
	    dest[i] = src[i];
	  }
	}
}
