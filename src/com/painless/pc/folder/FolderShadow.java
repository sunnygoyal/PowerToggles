package com.painless.pc.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.MeasureSpec;

import com.painless.pc.R;

public class FolderShadow extends DragShadowBuilder {

  private final FolderViews mViews;
  private final View v;

  public FolderShadow(Context context) {
    mViews = new FolderViews(context, View.inflate(context, R.layout.folder_preview, null));
    v = mViews.root;
  }

  public void copy(FolderViews target, String name) {
    mViews.image.setImageDrawable(target.image.getDrawable());
    mViews.image.setColorFilter(target.image.getColorFilter());
    mViews.image.setAlpha(target.image.getAlpha());
    mViews.text.setText(name);

    v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
  }

  @Override
  public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
    shadowSize.set(v.getMeasuredWidth(), v.getMeasuredHeight());
    shadowTouchPoint.set(shadowSize.x / 2, shadowSize.y);
  }

  @Override
  public void onDrawShadow(Canvas canvas) {
    v.draw(canvas);
  }
}

