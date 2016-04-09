package com.painless.pc.cfg;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout.LayoutParams;

import com.painless.pc.R;

public class ConfigGuide extends Activity {

  private int current;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cfg_config_guide);
    showBtn1();
    current = 0;
  }

  private void complete() {
    finish();
    overridePendingTransition(0, android.R.anim.fade_out);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    complete();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    finish();
  }

  public void onOkClicked(View v) {
    switch (current) {
      case 0:
        showBtn2();
        break;
      case 1:
        showBtn3();
        break;
      default:
        complete();
    }
    current++;
  }


  private void showBtn1() {
    int[] info = getIntent().getIntArrayExtra("b1");
    LayoutParams param = (LayoutParams) findViewById(R.id.btn_1_div).getLayoutParams();
    param.topMargin = info[1] + info[3];

    float cX = info[0] + info[2] * 0.5f;
    float cY = info[1] + info[3] * 0.5f;
    float radius = info[3] * 0.6f;

    GuideDrawable drawable = new GuideDrawable(cX, cY, radius);
    findViewById(R.id.btn_1_container).setBackground(drawable);
  }

  private void showBtn2() {
    int[] info = getIntent().getIntArrayExtra("b2");
    GuideDrawable drawable = new GuideDrawable(info[0], info[1], info[2], info[3]);
    findViewById(R.id.btn_2_container).setBackground(drawable);

    toggleView(R.id.btn_1_container, R.id.btn_2_container);
  }

  private void showBtn3() {
    int[] info = getIntent().getIntArrayExtra("b3");
    float margin = 8 * getResources().getDisplayMetrics().density;
    GuideDrawable drawable = new GuideDrawable(info[0] + margin, info[1] + margin, info[2] - 2 * margin, info[3] - 2 * margin);
    findViewById(R.id.btn_3_container).setBackground(drawable);
    toggleView(R.id.btn_2_container, R.id.btn_3_container);
  }

  private void toggleView(int id1, int id2) {
    Animation ani1 = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
    ani1.setFillAfter(true);
    findViewById(id1).startAnimation(ani1);

    View v2 = findViewById(id2);
    v2.setVisibility(View.VISIBLE);
    v2.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
  }

  private class GuideDrawable extends Drawable {

    private final Paint mPaint;
    private final Paint mPaint2;

    private final Path mClipPath;
    private final Path mBorderPath;
    private final Path mShadowPath;

    private final RectF mScale;

    public GuideDrawable(float x, float y, float w, float h) {
      float density = getResources().getDisplayMetrics().density;

      mClipPath = new Path();
      mClipPath.addRect(x, y, x + w, y + h, Path.Direction.CW);

      mBorderPath = new Path();
      mBorderPath.addRect(x - density, y - density, x + w + density, y + h + density, Path.Direction.CW);

      mShadowPath = new Path();
      mShadowPath.addRect(x - 3 * density, y - 3 * density, x + w + 3 * density, y + h + 3 * density, Path.Direction.CW);

      mScale = new RectF(x + w / 2, y + h / 2, 1 + 8 * density / w, 1 + 8 * density / h);

      mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPaint.setColor(getResources().getColor(R.color.state_button_light));

      mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPaint2.setColor(getResources().getColor(R.color.state_button_light_alpha));
      mPaint2.setMaskFilter(new BlurMaskFilter(density * 3, BlurMaskFilter.Blur.NORMAL));
    }

    public GuideDrawable(float cX, float cY, float radius) {
      float density = getResources().getDisplayMetrics().density;

      mClipPath = new Path();
      mClipPath.addCircle(cX, cY, radius, Path.Direction.CW);

      mBorderPath = new Path();
      mBorderPath.addCircle(cX, cY, radius + 2 * density, Path.Direction.CW);

      mShadowPath = new Path();
      mShadowPath.addCircle(cX, cY, radius + 4 * density, Path.Direction.CW);

      mScale = new RectF(cX, cY, 1 + 8 * density / radius, 1 + 8 * density / radius);

      mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPaint.setColor(getResources().getColor(R.color.state_button_light));

      mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPaint2.setColor(getResources().getColor(R.color.state_button_light_alpha));
      mPaint2.setMaskFilter(new BlurMaskFilter(density * 5, BlurMaskFilter.Blur.NORMAL));
    }

    @Override
    public void draw(Canvas c) {
      c.save();
      c.clipPath(mClipPath, Region.Op.DIFFERENCE);

      c.drawColor(0xcc000000);
      c.drawPath(mShadowPath, mPaint2);
      c.drawPath(mBorderPath, mPaint);
      c.restore();

      c.save();
      c.scale(mScale.right, mScale.bottom, mScale.left, mScale.top);
      c.clipPath(mClipPath, Region.Op.DIFFERENCE);
      c.drawPath(mShadowPath, mPaint2);
      c.drawPath(mBorderPath, mPaint);
      c.restore();
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(ColorFilter filter) { }
  }
}
