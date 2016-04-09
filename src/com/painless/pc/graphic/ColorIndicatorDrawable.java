package com.painless.pc.graphic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class ColorIndicatorDrawable extends AlphaPatternDrawable {

	public ColorIndicatorDrawable(Context context) {
		this(context.getResources().getDisplayMetrics().density);
	}

	private final Paint mStrokePaint = new Paint();
	private final Paint mColorPaint = new Paint();
	private final Rect mColorRect = new Rect();

	private boolean mIsEnabled = true;

	ColorIndicatorDrawable(float density) {
		super((int) (5 * density));
		
		mStrokePaint.setStrokeWidth(density * 2);
		mStrokePaint.setColor(Color.GRAY);
		mStrokePaint.setStyle(Paint.Style.STROKE);		
		
		setColor(Color.BLACK);
	}

	public void setColor(int color) {
		mColorPaint.setColor(color);
	}

	public void setEnabled(boolean isEnabled) {
		mIsEnabled = isEnabled;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		
		if (bounds.width() > 2 && bounds.height() > 2) {
			mColorRect.set(bounds.left + 1, bounds.top + 1, bounds.right - 1, bounds.bottom - 1);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if (mIsEnabled) {
			super.draw(canvas);
			canvas.drawRect(getBounds(), mStrokePaint);
			canvas.drawRect(mColorRect, mColorPaint);
		} else {
			canvas.drawColor(0x33333333);
//			canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
			canvas.drawRect(getBounds(), mStrokePaint);
		}
	}

	public int getColor() {
		return mColorPaint.getColor();
	}
}
