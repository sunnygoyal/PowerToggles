package com.painless.pc.acts;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;

public abstract class AbstractPopup extends Activity implements OnSeekBarChangeListener {

	private final int layoutId;

	AbstractPopup(int layoutId) {
		this.layoutId = layoutId;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    final Window window = getWindow();
    window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

		setContentView(layoutId);
		initUi();

		DisplayMetrics m = getResources().getDisplayMetrics();
		int width = m.widthPixels - (int)(20 * m.density);
		window.setLayout(width, LayoutParams.WRAP_CONTENT);

		final WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		lp.y = (int)(30 * m.density);
		window.setAttributes(lp);

    View root = findViewById(R.id.rootLayout);
    root.measure(0, 0);
    root.setTranslationY(-root.getMeasuredHeight());
    root.animate()
      .translationY(0)
      .setDuration(300)
      .setInterpolator(new DecelerateInterpolator(3));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	private boolean mRunningFinishAnimation;

	@Override
	protected void onPause() {
		if (!mRunningFinishAnimation) {
		  mRunningFinishAnimation = true;
	    PCWidgetActivity.partialUpdateAllWidgets(this);

	    View root = findViewById(R.id.rootLayout);
	    root.animate().translationY(-root.getHeight()).setDuration(250)
	      .setInterpolator(new AccelerateInterpolator(3)).withEndAction(new Runnable() {

          @Override
          public void run() {
            finish();
          }
        });
		}
    super.onPause();
    overridePendingTransition(0, 0);
	}

	@Override
	public void onBackPressed() {
    onPause();
	}

	@Override
	public void finish() {
	  super.finish();
    overridePendingTransition(0, 0);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
			onPause();
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	}

	abstract void initUi();
}
