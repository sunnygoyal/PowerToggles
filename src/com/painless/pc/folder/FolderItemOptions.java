package com.painless.pc.folder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.painless.pc.R;

public abstract class FolderItemOptions implements OnGlobalLayoutListener, OnClickListener {

  private final Context mContext;
  private final PopupWindow mWindow;
  private final View mRootView;
  private final View mArrowView;

  private int mViewLeft;
  private int mViewWidth;

  public FolderItemOptions(Context context) {
    mContext = context;
    mRootView = View.inflate(context, R.layout.folder_item_options, null);

    mArrowView = mRootView.findViewById(R.id.cp_arrow);
    mArrowView.measure(0, 0);

    mWindow = new PopupWindow(mContext);
    mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
    mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
    mWindow.setContentView(mRootView);
    mWindow.setAnimationStyle(R.style.MenuAnimation);

    mRootView.findViewById(R.id.mnu_rename).setOnClickListener(this);
    mRootView.findViewById(R.id.mnu_change_icon).setOnClickListener(this);
    mRootView.findViewById(R.id.mnu_delete).setOnClickListener(this);
  }

  public void show(View target, boolean enableRename) {
    mRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);

    mWindow.setTouchable(false);
    mWindow.setFocusable(false);
    mWindow.setOutsideTouchable(false);
    mRootView.findViewById(R.id.mnu_rename).setVisibility(enableRename ? View.VISIBLE : View.GONE);

    int[] location      = new int[2];
    target.getLocationInWindow(location);
    mWindow.showAtLocation(target, Gravity.LEFT | Gravity.TOP, location[0], location[1] + target.getHeight() - mArrowView.getMeasuredHeight());

    target.getLocationOnScreen(location);
    mViewLeft = location[0];
    mViewWidth = target.getMeasuredWidth();
  }

  @Override
  public void onGlobalLayout() {
    mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    int[] location = new int[2];
    mRootView.getLocationOnScreen(location);
    mArrowView.setTranslationX((mViewLeft - location[0]) + (mViewWidth - mArrowView.getMeasuredWidth()) / 2);
  }

  public void hide() {
    mWindow.dismiss();
  }

  public void activate() {
    if (mWindow.isShowing()) {
      mWindow.setTouchable(true);
      mWindow.setFocusable(true);
      mWindow.setOutsideTouchable(true);
      mWindow.update();
    }
  }
}
