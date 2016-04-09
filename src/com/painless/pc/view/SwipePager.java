package com.painless.pc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * A simplified implementation of ViewPager from support library
 */
public class SwipePager extends LinearLayout {

	private static final int MAX_SETTLE_DURATION = 600; // ms

	private final Scroller mScroller;
	private final int mTouchSlop;
	private final int mMaximumVelocity;
	private final float mBaseLineFlingVelocity;
	private final float mFlingVelocityInfluence;

	private int mVisibleChild = -1;
	private VelocityTracker mVelocityTracker;

	private boolean mIsBeingDragged;
	private boolean mIsUnableToDrag;
	private OnPageChangeListener mListener;

	/**
	 * Position of the last motion event.
	 */
	private float mLastMotionX;
	private float mLastMotionY;

	public SwipePager(Context context, AttributeSet attrs) {
		super(context, attrs);
		mScroller = new Scroller(context, new AccelerateDecelerateInterpolator());

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledPagingTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		float density = context.getResources().getDisplayMetrics().density;
		mBaseLineFlingVelocity = 2500.0f * density;
		mFlingVelocityInfluence = 0.4f;
	}

	/**
	 * Listens to page change events.
	 */
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		mListener = listener;
	}

	/**
	 * Sets the visible child for the view.
	 */
	public void setDisplayChild(int index) {
		setDisplayChild(index, 0);
	}

	private void setDisplayChild(int index, int velocity) {
		if (mVisibleChild == -1) {
			// initial layout
			scrollTo(index * getMeasuredWidth(), 0);
			mVisibleChild = index;

		} else {
			int currentX = getScrollX();
			int finalScroll = index * getMeasuredWidth();
			if (currentX != finalScroll) {
				int dx = finalScroll - currentX;

				final float pageDelta = (float) Math.abs(dx) / getMeasuredWidth();
				int duration = (int) (pageDelta * 100);

				velocity = Math.abs(velocity);
				if (velocity > 0) {
					duration += (duration / (velocity / mBaseLineFlingVelocity)) * mFlingVelocityInfluence;
				} else {
					duration += 100;
				}
				duration = Math.min(duration, MAX_SETTLE_DURATION);

				mScroller.startScroll(currentX, 0, dx, 0, duration);

				invalidate();
			}
			mVisibleChild = index;
		}
	}

	@Override
	public void computeScroll() {
		if (!mScroller.isFinished()) {
			if (mScroller.computeScrollOffset()) {
				int oldX = getScrollX();
				int oldY = getScrollY();
				int x = mScroller.getCurrX();
				int y = mScroller.getCurrY();

				if (oldX != x || oldY != y) {
					scrollTo(x, y);
				}

				// Keep on drawing until the animation has finished.
				invalidate();
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
				getDefaultSize(0, heightMeasureSpec));

		// Children are just made to fill our space.
		int mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
		int mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);

		final int size = getChildCount();
		for (int i = 0; i < size; ++i) {
			final View child = getChildAt(i);
			child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if ((w != oldw) && (mVisibleChild > -1)) {
			scrollTo(mVisibleChild * w, 0);
		}
	}

	/**
	 * This method JUST determines whether we want to intercept the motion.
	 * If we return true, onMotionEvent will be called and we do the actual
	 * scrolling there.
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction() & MotionEvent.ACTION_MASK;

		// Always take care of the touch gesture being complete.
		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {

			// Release the drag.
			mIsBeingDragged = false;
			mIsUnableToDrag = false;
			return false;
		}

		// Nothing more to do here if we have decided whether or not we
		// are dragging.
		if (action != MotionEvent.ACTION_DOWN) {
			if (mIsBeingDragged) {
				return true;
			}
			if (mIsUnableToDrag) {
				return false;
			}
		}

		switch (action) {
		case MotionEvent.ACTION_MOVE: {
			/*
			 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
			 * whether the user has moved far enough from his original down touch.
			 */

			/*
			 * Locally do absolute value. mLastMotionY is set to the y value
			 * of the down event.
			 */
			final float x = ev.getX();
			final float dx = x - mLastMotionX;
			final float xDiff = Math.abs(dx);
			final float y = ev.getY();
			final float yDiff = Math.abs(y - mLastMotionY);

			if (xDiff > mTouchSlop && xDiff > yDiff) {
				mIsBeingDragged = true;
				mLastMotionX = x;
			} else {
				if (yDiff > mTouchSlop) {
					// The finger has moved enough in the vertical
					// direction to be counted as a drag...  abort
					// any attempt to drag horizontally, to work correctly
					// with children that have scrolling containers.
					mIsUnableToDrag = true;
				}
			}
			break;
		}

		case MotionEvent.ACTION_DOWN: {
			// Do not capture touch event if we are still scrolling.
			if (!mScroller.isFinished()) {
				return false;
			}

			/*
			 * Remember location of down touch.
			 * ACTION_DOWN always refers to pointer index 0.
			 */
			mLastMotionX = ev.getX();
			mLastMotionY = ev.getY();

			mIsBeingDragged = false;
			mIsUnableToDrag = false;
			break;
		}
		}

		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
			// Don't handle edge touches immediately -- they may actually belong to one of our
			// descendants.
			return false;
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {

			// Remember where the motion event started
			mLastMotionX = ev.getX();
			break;
		}
		case MotionEvent.ACTION_MOVE:
			if (!mIsBeingDragged) {
				final float x = ev.getX();
				final float xDiff = Math.abs(x - mLastMotionX);
				final float y = ev.getY();
				final float yDiff = Math.abs(y - mLastMotionY);
				if (xDiff > mTouchSlop && xDiff > yDiff) {
					mIsBeingDragged = true;
					mLastMotionX = x;
				}
			}
			if (mIsBeingDragged) {
				// Scroll to follow the motion event
				final float x = ev.getX();
				final float deltaX = mLastMotionX - x;
				mLastMotionX = x;
				float oldScrollX = getScrollX();
				float scrollX = oldScrollX + deltaX;
				final int width = getMeasuredWidth();
				final int lastItemIndex = getChildCount() - 1;

				final float leftBound = Math.max(0, (mVisibleChild - 1) * width);
				final float rightBound = Math.min(mVisibleChild + 1, lastItemIndex) * width;

				scrollX = Math.max(leftBound, Math.min(rightBound, scrollX));
				// Don't lose the rounded component
				mLastMotionX += scrollX - (int) scrollX;
				scrollTo((int) scrollX, getScrollY());
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsBeingDragged) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

				final int currentPage = getScrollX() / getMeasuredWidth();
				int initialVelocity = (int) velocityTracker.getXVelocity();          
				int nextPage = Math.min(initialVelocity > 0 ? currentPage : currentPage + 1, getChildCount() - 1);
				setDisplayChild(nextPage, initialVelocity);
				if (mListener != null) {
					mListener.onPageChanged(nextPage);
				}
			}
			endDrag();
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeingDragged) {
				setDisplayChild(mVisibleChild);
			}
			endDrag();
			break;
		}
		return true;
	}

	private void endDrag() {
		mIsBeingDragged = false;
		mIsUnableToDrag = false;

		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	public static interface OnPageChangeListener {
		void onPageChanged(int position); 
	}
}
