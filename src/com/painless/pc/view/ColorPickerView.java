package com.painless.pc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.painless.pc.graphic.AlphaPatternDrawable;

public class ColorPickerView extends View {

  public interface OnColorChangedListener{
    public void onColorChanged(int color, View v);
  }

  private final static int        PANEL_NONE = -1;
  private final static int        PANEL_SAT_VAL = 0;
  private final static int        PANEL_HUE = 1;
  private final static int        PANEL_ALPHA = 2;

  private static final int HUE = 0;
  private static final int SAT = 1;
  private static final int VAL = 2;

  private final static float      BORDER_WIDTH_PX = 1;

  private static final int SLIDER_TRACKER_COLOR = 0xff1c1c1c;
  private static final int BORDER_COLOR = 0xff6E6E6E;

  // Various density constants
  private final float mDensity;
  private final float mDensity2;
  
  private final float mHuePanelWidth;
  private final float mAlphaPanelHeight;
  private final float mPanelSpacing;
  private final float mPaletteCircleTrackerRadius;
  private final float mRectangleTrackerOffset;

  /**
   * Offset from the edge we must have or else
   * the finger tracker will get clipped when
   * it is drawn outside of the view.
   */
  private final float mDrawingOffset;

  /**
   * Various drawing tools 
   */
  private final Paint mSatValPaint1;
  private final Paint mSatValPaint2;
  private final Paint mSatValPaint3;
  private final Paint mSatValTrackerPaint;
  private final Paint mHuePaint;
  private final Paint mHueTrackerPaint;
  private final Paint mAlphaPaint;
  private final Paint mAlphaTextPaint;
  private final Paint mBorderPaint;

  private final AlphaPatternDrawable mAlphaPattern;

  /*
   * Distance form the edges of the view
   * of where we are allowed to draw.
   */
  private final RectF mDrawingRect = new RectF();
  private final RectF mSatValRect = new RectF();
  private final RectF mHueRect = new RectF();
  private final RectF mAlphaRect = new RectF();

  private final RectF[] mTouchRects = new RectF[] { mSatValRect, mHueRect, mAlphaRect} ;

  /**
   * Various variables used for temporary calculations
   */
  private final RectF mTempRect = new RectF();

  private OnColorChangedListener  mListener;

  private Shader          mValShader;
  private Shader          mSatShader;
  private Shader          mHueShader;
  private Shader          mAlphaShader;

  private final float[] mHSV = {360f, 0, 0};
  private int mAlpha = 0xff;
  private int mColor;

  private String mAlphaSliderText = "";

  private int mLastTouchRect;

  public ColorPickerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mDensity = getContext().getResources().getDisplayMetrics().density;
    mDensity2 = mDensity * 2;

    mHuePanelWidth = 30f * mDensity;
    mAlphaPanelHeight = 30f * mDensity;
    mPanelSpacing = 10f * mDensity;
    mPaletteCircleTrackerRadius = 5f * mDensity;
    mRectangleTrackerOffset = 2f * mDensity;

    mDrawingOffset = mPaletteCircleTrackerRadius * 1.5f;
    mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));

    // Init paint tools.
    mSatValPaint1 = new Paint();
    mSatValPaint2 = new Paint();
    mSatValPaint3 = new Paint();
    mSatValTrackerPaint = new Paint();
    mHuePaint = new Paint();
    mHueTrackerPaint = new Paint();
    mAlphaPaint = new Paint();
    mAlphaTextPaint = new Paint();
    mBorderPaint = new Paint();

    mSatValTrackerPaint.setStyle(Style.STROKE);
    mSatValTrackerPaint.setStrokeWidth(2f * mDensity);
    mSatValTrackerPaint.setAntiAlias(true);

    mHueTrackerPaint.setColor(SLIDER_TRACKER_COLOR);
    mHueTrackerPaint.setStyle(Style.STROKE);
    mHueTrackerPaint.setStrokeWidth(2f * mDensity);
    mHueTrackerPaint.setAntiAlias(true);

    mAlphaTextPaint.setColor(0xff1c1c1c);
    mAlphaTextPaint.setTextSize(14f * mDensity);
    mAlphaTextPaint.setAntiAlias(true);
    mAlphaTextPaint.setTextAlign(Align.CENTER);
    mAlphaTextPaint.setFakeBoldText(true);

    mColor = Color.HSVToColor(mAlpha, mHSV);

    //Needed for receiving trackball motion events.
    setFocusable(true);
    setFocusableInTouchMode(true);
//    setLayerType(LAYER_TYPE_SOFTWARE, null);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if(mDrawingRect.width() <= 0 || mDrawingRect.height() <= 0) {
      return;
    }

    drawSatValPanel(canvas);
    drawHuePanel(canvas);
    drawAlphaPanel(canvas);
  }

  private void drawSatValPanel(Canvas canvas){
    final RectF rect = mSatValRect;

    if(BORDER_WIDTH_PX > 0){
      mBorderPaint.setColor(BORDER_COLOR);
      canvas.drawRect(mDrawingRect.left, mDrawingRect.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, mBorderPaint);
    }

    final int rgb = Color.HSVToColor(new float[]{mHSV[HUE], 1f, 1f});
    mSatValPaint1.setColor(rgb);

    if (mSatShader == null) {
      mSatShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
              0xFFFFFFFF, 0x00FFFFFF, TileMode.CLAMP);
      mSatValPaint2.setShader(mSatShader);
    }

    if (mValShader == null) {
      mValShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
              0x00000000, 0xFF000000, TileMode.CLAMP);
      mSatValPaint3.setShader(mValShader);
    }

    canvas.drawRect(rect, mSatValPaint1);
    canvas.drawRect(rect, mSatValPaint2);
    canvas.drawRect(rect, mSatValPaint3);

    float h = mSatValRect.height();
    float w = mSatValRect.width();
    float x = mHSV[SAT] * w + mSatValRect.left;
    float y = (1 - mHSV[VAL]) * h + mSatValRect.top;

    mSatValTrackerPaint.setColor(0xff000000);
    canvas.drawCircle(x, y, mPaletteCircleTrackerRadius - 1f * mDensity, mSatValTrackerPaint);

    mSatValTrackerPaint.setColor(0xffdddddd);
    canvas.drawCircle(x, y, mPaletteCircleTrackerRadius, mSatValTrackerPaint);
  }

  private void drawHuePanel(Canvas canvas){
    final RectF rect = mHueRect;
    if(BORDER_WIDTH_PX > 0){
      mBorderPaint.setColor(BORDER_COLOR);
      canvas.drawRect(rect.left - BORDER_WIDTH_PX,
              rect.top - BORDER_WIDTH_PX,
              rect.right + BORDER_WIDTH_PX,
              rect.bottom + BORDER_WIDTH_PX,
              mBorderPaint);
    }

    if (mHueShader == null) {
      final int[] hue = new int[361];

      int count = 0;
      for(int i = hue.length -1; i >= 0; i--, count++){
        hue[count] = Color.HSVToColor(new float[]{i, 1f, 1f});
      }

      mHueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, hue, null, TileMode.CLAMP);
      mHuePaint.setShader(mHueShader);
    }

    canvas.drawRect(rect, mHuePaint);

    float h = mHueRect.height();
    float y = h - (mHSV[HUE] * h / 360) + mHueRect.top;
    mTempRect.left = rect.left - mRectangleTrackerOffset;
    mTempRect.right = rect.right + mRectangleTrackerOffset;
    mTempRect.top = y - mDensity2;
    mTempRect.bottom = y + mDensity2;
    canvas.drawRoundRect(mTempRect, mDensity2, mDensity2, mHueTrackerPaint);
  }

  private void drawAlphaPanel(Canvas canvas){
    final RectF rect = mAlphaRect;

    if(BORDER_WIDTH_PX > 0){
      mBorderPaint.setColor(BORDER_COLOR);
      canvas.drawRect(rect.left - BORDER_WIDTH_PX,
              rect.top - BORDER_WIDTH_PX,
              rect.right + BORDER_WIDTH_PX,
              rect.bottom + BORDER_WIDTH_PX,
              mBorderPaint);
    }

    mAlphaPattern.draw(canvas);

    final int color = Color.HSVToColor(mHSV);
    final int acolor = Color.HSVToColor(0, mHSV);

    mAlphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
            color, acolor, TileMode.CLAMP);
    mAlphaPaint.setShader(mAlphaShader);
    canvas.drawRect(rect, mAlphaPaint);

    if(!TextUtils.isEmpty(mAlphaSliderText)){
      canvas.drawText(mAlphaSliderText, rect.centerX(), rect.centerY() + 4 * mDensity, mAlphaTextPaint);
    }
    
    float w = mAlphaRect.width();
    float x = w - (mAlpha * w / 0xff) + mAlphaRect.left;

    mTempRect.left = x - mDensity2;
    mTempRect.right = x + mDensity2;
    mTempRect.top = rect.top - mRectangleTrackerOffset;
    mTempRect.bottom = rect.bottom + mRectangleTrackerOffset;
    canvas.drawRoundRect(mTempRect, mDensity2, mDensity2, mHueTrackerPaint);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean update = false;
    switch(event.getAction()){
      case MotionEvent.ACTION_DOWN:
        mLastTouchRect = PANEL_NONE;
        for (int i=0; i<3; i++) {
          if (mTouchRects[i].contains(event.getX(), event.getY())) {
            mLastTouchRect = i;
          }
        }
        update = moveTrackersIfNeeded(event);
        break;

      case MotionEvent.ACTION_MOVE:
        update = moveTrackersIfNeeded(event);
        break;

      case MotionEvent.ACTION_UP:
        mLastTouchRect = PANEL_NONE;
        break;

    }

    if (update) {
      mColor = Color.HSVToColor(mAlpha, mHSV);
      if (mListener != null){
        mListener.onColorChanged(mColor, this);
      }
      invalidate();
    }
    return true;
  }

  private boolean moveTrackersIfNeeded(MotionEvent event) {
    switch (mLastTouchRect) {
      case PANEL_HUE: {
        float h = mHueRect.height();
        float y = inRange(event.getY(), h);
        mHSV[HUE] = 360f - (y * 360f / h);
        break;
      }
      
      case PANEL_SAT_VAL: {
        float w = mSatValRect.width();
        float h = mSatValRect.height();

        mHSV[SAT] = inRange(event.getX() - mSatValRect.left, w) / w;
        mHSV[VAL] = 1 - inRange(event.getY() - mSatValRect.top, h) / h;
        break;
      }

      case PANEL_ALPHA: {
        int w = (int) mAlphaRect.width();
        int x = (int) inRange(event.getX() - mAlphaRect.left, w);
        mAlpha = 0xff - (x * 0xff / w);
        break;
      }

      default:
        return false;
    }
    
    return true;
  }


  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = 0;
    int height = 0;

    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    int widthAllowed = MeasureSpec.getSize(widthMeasureSpec);
    int heightAllowed = MeasureSpec.getSize(heightMeasureSpec);


    widthAllowed = chooseWidth(widthMode, widthAllowed);
    heightAllowed = chooseHeight(heightMode, heightAllowed);

    width = (int) (heightAllowed - mAlphaPanelHeight + mHuePanelWidth);

    if(width > widthAllowed){
      width = widthAllowed;
      height = (int) (widthAllowed - mHuePanelWidth + mAlphaPanelHeight);
    }
    else{
      height = heightAllowed;
    }

    setMeasuredDimension(width, height);
  }

  private int chooseWidth(int mode, int size){
    if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
      return size;
    } else { // (mode == MeasureSpec.UNSPECIFIED)
      return getPrefferedWidth();
    }
  }

  private int chooseHeight(int mode, int size){
    if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
      return size;
    } else { // (mode == MeasureSpec.UNSPECIFIED)
      return getPrefferedHeight();
    }
  }

  private int getPrefferedWidth(){
    int width = getPrefferedHeight();
    width -= (mPanelSpacing + mAlphaPanelHeight);

    return (int) (width + mHuePanelWidth + mPanelSpacing);
  }

  private int getPrefferedHeight(){
    int height = (int)(200 * mDensity);
    height += mPanelSpacing + mAlphaPanelHeight;
    return height;
  }



  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    mDrawingRect.left = mDrawingOffset + getPaddingLeft();
    mDrawingRect.right  = w - mDrawingOffset - getPaddingRight();
    mDrawingRect.top = mDrawingOffset + getPaddingTop();
    mDrawingRect.bottom = h - mDrawingOffset - getPaddingBottom();

    setUpSatValRect();
    setUpHueRect();
    setUpAlphaRect();
  }

  private void setUpSatValRect(){
    final RectF dRect = mDrawingRect;
    float panelSide = dRect.height() - BORDER_WIDTH_PX * 2;
    panelSide -= mPanelSpacing + mAlphaPanelHeight;

    final float left = dRect.left + BORDER_WIDTH_PX;
    final float top = dRect.top + BORDER_WIDTH_PX;
    final float bottom = top + panelSide;
    final float right = left + panelSide;

    mSatValRect.set(left, top, right, bottom);

    mValShader = null;
    mSatShader = null;
  }

  private void setUpHueRect(){
    final RectF dRect = mDrawingRect;
    mHueRect.left = dRect.right - mHuePanelWidth + BORDER_WIDTH_PX;
    mHueRect.top = dRect.top + BORDER_WIDTH_PX;
    mHueRect.bottom = dRect.bottom - BORDER_WIDTH_PX - (mPanelSpacing + mAlphaPanelHeight);
    mHueRect.right = dRect.right - BORDER_WIDTH_PX;
    mHueShader = null;
  }

  private void setUpAlphaRect(){
    final RectF dRect = mDrawingRect;

    mAlphaRect.left = dRect.left + BORDER_WIDTH_PX;
    mAlphaRect.top = dRect.bottom - mAlphaPanelHeight + BORDER_WIDTH_PX;
    mAlphaRect.bottom = dRect.bottom - BORDER_WIDTH_PX;
    mAlphaRect.right = dRect.right - BORDER_WIDTH_PX;
    mAlphaPattern.setBounds(Math.round(mAlphaRect.left), Math
            .round(mAlphaRect.top), Math.round(mAlphaRect.right), Math
            .round(mAlphaRect.bottom));
  }


  /**
   * Set a OnColorChangedListener to get notified when the color
   * selected by the user has changed.
   * @param listener
   */
  public void setOnColorChangedListener(OnColorChangedListener listener){
    mListener = listener;
  }

  /**
   * Get the current color this view is showing.
   * @return the current color.
   */
  public int getColor() {
    return mColor;
  }

  /**
   * Set the color the view should show.
   * @param color The color that should be selected.
   */
  public void setColor(int color){
    if (mColor != color) {
      mColor = color;
      Color.colorToHSV(color, mHSV);
      mAlpha =  Color.alpha(color);
      invalidate();
    }
  }

  /**
   * Get the drawing offset of the color picker view.
   * The drawing offset is the distance from the side of
   * a panel to the side of the view minus the padding.
   * Useful if you want to have your own panel below showing
   * the currently selected color and want to align it perfectly.
   * @return The offset in pixels.
   */
  public float getDrawingOffset(){
    return mDrawingOffset;
  }

  /**
   * Set the text that should be shown in the
   * alpha slider. Set to null to disable text.
   * @param res string resource id.
   */
  public void setAlphaSliderText(int res){
    mAlphaSliderText = getContext().getString(res);
    invalidate();
  }

  private static float inRange(float val, float max) {
    return Math.max(Math.min(val, max), 0);
  }
}
