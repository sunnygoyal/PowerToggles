package com.painless.pc.cfg;

import static com.painless.pc.util.SettingsDecoder.KEY_PADDING;
import static com.painless.pc.util.SettingsDecoder.KEY_STRETCH;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

import com.painless.pc.R;
import com.painless.pc.util.UiUtils;
import com.painless.pc.view.NinePatchView;
import com.painless.pc.view.RectView;
import com.painless.pc.view.RectView.RectListener;
import com.painless.pc.view.SwipePager;
import com.painless.pc.view.SwipePager.OnPageChangeListener;

/**
 * The editor expects both rectangles in the format
 * left, top, right, bottom
 */
public class NinePatchEditor extends Activity implements RectListener,
    OnClickListener, TabContentFactory, OnTabChangeListener, OnPageChangeListener {

  private TabHost mTabHost;
  private SwipePager mPager;

  private Bitmap mImg;

  // Views for stretch
  private NinePatchView mNinePatchView;
  private RectView mStretchView;
  private float[] mStretchRect;

  // Views for padding
  private float[] mPaddingRect;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cfg_strectch_editor);
    mImg = (Bitmap) getIntent().getParcelableExtra("img");

    mStretchRect = getExtra(KEY_STRETCH);
    mPaddingRect = getExtra(KEY_PADDING);

    mTabHost = (TabHost) findViewById(android.R.id.tabhost);
    mTabHost.setup();
    mPager = (SwipePager) findViewById(R.id.swipeTabs);

    mTabHost.addTab(mTabHost.newTabSpec("t1").setIndicator(getString(R.string.npe_stretch)).setContent(this));
    mTabHost.addTab(mTabHost.newTabSpec("t2").setIndicator(getString(R.string.npe_padding)).setContent(this));
    mTabHost.setOnTabChangedListener(this);
    mPager.setOnPageChangeListener(this);
    mPager.setDisplayChild(0);

    
    mNinePatchView = (NinePatchView) findViewById(R.id.prewiewHolder);
    mNinePatchView.setBitmap(mImg);
    mNinePatchView.setDim(mStretchRect);
    LayoutParams lp = mNinePatchView.getLayoutParams();
    lp.height = Math.max(lp.height, mImg.getHeight() * 3 / 2);

    mStretchView = setRect(R.id.stretch_rect, mStretchRect, 2);
    setRect(R.id.padding_rect, mPaddingRect, 3);
  }

  private RectView setRect(int id, float[] rect, int scaleFactor) {
    RectView v = (RectView) findViewById(id);
    LayoutParams lp = v.getLayoutParams();
    lp.width = mImg.getWidth() * scaleFactor;
    lp.height = mImg.getHeight() * scaleFactor;
    v.setBitmap(mImg);

    v.updateRect(rect);
    v.setListener(this);
    return v;
  }

  private float[] getExtra(String key) {
    int[] extra = getIntent().getIntArrayExtra(key);
    float[] ret = new float[4];
    
    float[] dim = new float[] {mImg.getWidth(), mImg.getHeight()};
    for (int i = 0; i < 4; i++) {
      ret[i] = extra[i] / dim[i & 1];
    }
    if ((ret[2] <= ret[0]) || (ret[3] <= ret[1])) {
      return new float[] {0, 0, 1, 1};
    }
    return ret;
  }
  
  // View pager related callbacks.
  @Override
  public void onPageChanged(int position) {
    mTabHost.setCurrentTab(position);
  }

  // Tab host related callbacks
  @Override
  public View createTabContent(String tag) {
    return new View(this);
  }

  @Override
  public void onTabChanged(String tabId) {
    mPager.setDisplayChild(mTabHost.getCurrentTab());
  }

  @Override
  public void onRectChange(float[] rect, View v) {
    if (v == mStretchView) {
      mNinePatchView.setDim(rect);
      mStretchRect = rect;
    } else {
      mPaddingRect = rect;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return UiUtils.addDoneInMenu(menu, this);
  }

  /**
   * Done clicked
   */
  @Override
  public void onClick(View v) {
    setResult(RESULT_OK, new Intent()
      .putExtra(KEY_STRETCH, convertToInt(mStretchRect))
      .putExtra(KEY_PADDING, convertToInt(mPaddingRect)));
    finish();
  }

  private int[] convertToInt(float[] value) {
    int[] dim = new int[] { mImg.getWidth(), mImg.getHeight() };
    int[] ret = new int[4];
    
    for (int i = 0 ; i < 4; i++) {
      ret[i] = (int) (value[i] * dim[i & 1]);
    }
    return ret;
  }
}
