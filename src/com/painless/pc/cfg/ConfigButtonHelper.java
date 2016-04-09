package com.painless.pc.cfg;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.painless.pc.R;
import com.painless.pc.picker.IconPicker;
import com.painless.pc.picker.TogglePicker.TogglePickerListener;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.singleton.PluginDB;
import com.painless.pc.singleton.WidgetDB;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.ImportExportActivity;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.WidgetSetting;

public class ConfigButtonHelper implements OnClickListener, IconPicker.Callback, TogglePickerListener {

	public final ArrayList<TrackerItem> mItemList = new ArrayList<TrackerItem>();
	@Thunk final ImportExportActivity<?> mContext;
	private final IconPicker mIconPicker;
	@Thunk final String[] mTrackerNames;

	@Thunk final LinearLayout mPreviewHolder;
	private final ToggleConfigHandler mToggleConfigHandler;

	private final boolean mIsNotificationMode;

	private WidgetSetting mSettings;
	private int mOldLayoutId = 0;
	@Thunk TrackerItem mSelectedTracker;

	public ConfigButtonHelper(ImportExportActivity<?> main, boolean isNotification) {
		this.mContext = main;
		this.mIconPicker = new IconPicker(main);
		mTrackerNames = main.getResources().getStringArray(R.array.tracker_names);
		mPreviewHolder = (LinearLayout) main.findViewById(R.id.prewiewHolder);
		
		if (isNotification) {
		  mPreviewHolder.getLayoutParams().height = main.getResources().getDimensionPixelSize(R.dimen.notification_height);
		}

		View toggleConfigView = main.findViewById(R.id.wc_toggle_info);
		mToggleConfigHandler = (toggleConfigView == null) ?
		        new PopupToggleConfigHandler() : new InlineToggleConfigHandler(toggleConfigView);

		mIsNotificationMode = isNotification;
		mSelectedTracker = null;
	}

	public void readSettings(WidgetSetting setting, Bitmap[] icons) {
		mSettings = setting;

		mItemList.clear();
		for (int i = 0, pos = 0; i < 8; i++) {
			AbstractTracker tracker = setting.trackers[i];
			if (tracker != null) {
				mItemList.add(newTrackerItem(tracker, icons[pos]));
				pos++;
			}
		}
		mSelectedTracker = mItemList.isEmpty() ? null : mItemList.get(0);

		mToggleConfigHandler.onTrackerListUpdated(mSelectedTracker);
	}

	private TrackerItem newTrackerItem(AbstractTracker tracker, Bitmap icon) {
		return new TrackerItem(tracker, icon, mContext);
	}

	/**
	 * Redraws the widget preview.
	 * @return the selected view or null.
	 */
	public void refreshView() {		
		int lastIndex = mItemList.size() - 1;
		int selectedPosition = -1;

		int buttonIndex = 0;
		for (int i=0; i<8; i++) {
			TrackerItem item = null;
			if (i < lastIndex) {
				item = mItemList.get(i);
			} else if (i == 7) {
				item = mItemList.get(lastIndex);
			}
			if (item == null) {
				mSettings.trackers[i] = null;
			} else {
				mSettings.trackers[i] = item.tracker;
				mSettings.imageProviders[buttonIndex++] = item.imageProvider;

				if (item == mSelectedTracker) {
					selectedPosition = i;
				}
			}
		}

		if (mSettings.rvFactory.layoutId != mOldLayoutId) {
			mPreviewHolder.removeAllViews();
			mOldLayoutId = mSettings.rvFactory.layoutId;
		}

		RemoteViews views = mSettings.rvFactory.getRemoteView(mContext, mSettings, mIsNotificationMode, false, 0);
		if (mPreviewHolder.getChildCount() == 0) {
			mPreviewHolder.addView(views.apply(mContext, mPreviewHolder));

			for (int i=0; i<Globals.BUTTONS.length; i++) {
				View button = mPreviewHolder.findViewById(Globals.BUTTONS[i]);
				button.setTag(i);
				button.setOnClickListener(this);
			}
			mPreviewHolder.requestLayout();
		} else {
			views.reapply(mContext, mPreviewHolder);
		}

		if (selectedPosition > -1) {
		  mToggleConfigHandler.setArrowPosition(mPreviewHolder.findViewById(Globals.BUTTONS[selectedPosition]));
		}
	}

	public int getToggleCount() {
	  return mItemList.size();
	}

	public String getDefinition(int widgetId, boolean saveDb) {
		String definition = "";
		int pos = 0;
		WidgetDB db = null;
		if (saveDb) {
			db = WidgetDB.get(mContext);
			db.deleteToggles(widgetId);
		}
		for (final TrackerItem item : mItemList) {
			if (item.tracker.getId().startsWith("ss_")) {
				final SimpleShortcut shrt = (SimpleShortcut) item.tracker;
				shrt.setId(SimpleShortcut.getId(widgetId, pos));
				if (saveDb) {
					db.saveShrt(shrt, item.originalIcon);
				}
			} else if (saveDb && item.originalIcon != null){
				db.saveIcon(SimpleShortcut.getId(widgetId, pos), item.originalIcon);

				if (item.tracker.getId().startsWith("pl_")) {
					PluginDB.get(mContext).save((PluginTracker) item.tracker);
				}
			}
			definition += item.tracker.getId() + ",";
			pos ++;
		}
		WidgetDB.closeAll();
		return definition.substring(0, definition.length() - 1);
	}

	/**
	 * Called when a toggle button is clicked.
	 */
	@Override
	public void onClick(View v) {
		int position = Math.min((Integer) v.getTag(), mItemList.size() - 1);

		mSelectedTracker = mItemList.get(position);
		mToggleConfigHandler.showForSelectedToggle();
		mToggleConfigHandler.showAtLocation(v);
		mToggleConfigHandler.setArrowPosition(v);
	}

	@Thunk void changeIconForSelectedToggle() {
    int defaultColor = ParseUtil.addAlphaToColor(mSettings.buttonAlphas[0], mSettings.buttonColors[0]);
	  mIconPicker.pickTrackerIcon(mSelectedTracker.tracker, this, defaultColor, mSelectedTracker.originalIcon);
	}

	@Override
	public void onTogglePicked(AbstractTracker tracker, Bitmap icon) {
	  TrackerItem added = new TrackerItem(tracker, icon, mContext);
    mItemList.add(added);
    mToggleConfigHandler.onTrackerListUpdated(added);
	}

	@Override
	public void onIconReceived(Bitmap icon) {
		mSelectedTracker.setIcon(icon, mContext);
		refreshView();		
	}

	
	/**
	 * Class which shows toggle config handler inside a popup.
	 */
	private class PopupToggleConfigHandler extends ToggleConfigHandler implements OnTouchListener {
	  private final PopupWindow mWindow;
	  private final View mArrow;

	  PopupToggleConfigHandler() {
	    super(LayoutInflater.from(mContext).inflate(R.layout.cfg_toggle_edit_popup, null));

	    mArrow = mRootView.findViewById(R.id.cp_arrow);

	    mWindow = new PopupWindow(mContext);
	    mWindow.setTouchInterceptor(this);
	    mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	    mWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
	    mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
	    mWindow.setTouchable(true);
	    mWindow.setFocusable(true);
	    mWindow.setOutsideTouchable(true);
	    mWindow.setContentView(mRootView);
	    mWindow.setAnimationStyle(android.R.style.Animation_Dialog);
	  }

    @Override
    public void onGlobalLayout() {
      if (mWindow.isShowing()) {
        if (mArrow.getMeasuredWidth() <= 0) {
          mArrow.measure(0, 0);
        }
        super.onGlobalLayout();

        if (mWindow.getWidth() != mPreviewHolder.getMeasuredWidth()) {
          mWindow.dismiss();
        }
      }
    }

    @Override
    void removeSelected() {
      mItemList.remove(mSelectedTracker);
      mSelectedTracker = null;
      refreshView();
      mWindow.dismiss();
    }

    @Override
    public void showAtLocation(View v) {
      int[] location      = new int[2];
      v.getLocationOnScreen(location);
      mWindow.setWidth(mPreviewHolder.getMeasuredWidth());
      mWindow.showAtLocation(v, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, location[1] + v.getHeight());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
        mWindow.dismiss();
        return true;
      }
      return false;
    }

	}

	/**
	 * Class which shows toggle config handler inside the main activity
	 */
	public class InlineToggleConfigHandler extends ToggleConfigHandler implements OnGlobalLayoutListener {

    InlineToggleConfigHandler(View rootView) {
      super(rootView);
    }

    @Override
    void removeSelected() {
      mItemList.remove(mSelectedTracker);
      mSelectedTracker = mItemList.get(0);
      showForSelectedToggle();
      refreshView();
    }

    @Override
    public void onTrackerListUpdated(TrackerItem newItem) {
      mSelectedTracker = newItem;
      showForSelectedToggle();
      refreshView();
    }
	}
	
	
	/**
	 * An abstract class to handle toggle configuration.
	 */
	private abstract class ToggleConfigHandler implements
	    OnClickListener, OnSeekBarChangeListener, OnEditorActionListener, OnGlobalLayoutListener {

    private final EditText cp_toggleName;
    private final SeekBar cp_mover;
    private final View cp_change_icon;
    private final View cp_remove;
    private final View cp_edit_name;

    private final InputMethodManager keyboard;

    final View mRootView;
    final View mArrowView;

    private View mCurrentTarget;

    ToggleConfigHandler(View rootView) {
      mRootView = rootView;

      cp_toggleName = (EditText) mRootView.findViewById(R.id.cp_toggleName);    
      cp_toggleName.setOnEditorActionListener(this);

      cp_mover = (SeekBar) mRootView.findViewById(R.id.cp_mover);
      cp_mover.setOnSeekBarChangeListener(this);

      cp_change_icon = mRootView.findViewById(R.id.cp_change_icon);
      cp_change_icon.setOnClickListener(this);
      cp_remove = mRootView.findViewById(R.id.cp_remove);
      cp_remove.setOnClickListener(this);
  
      cp_edit_name = mRootView.findViewById(R.id.cp_edit_name);
      cp_edit_name.setOnClickListener(this);

      keyboard = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
      
      View arrow = mRootView.findViewById(R.id.cp_arrow);
      mArrowView = (arrow == null) ? mContext.findViewById(R.id.config_popup_top) : arrow;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    /**
     * Set the seek bar position relative to the selected button.
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
      final int pos = mItemList.indexOf(mSelectedTracker);
      final int delta = 840 / mItemList.size();
      cp_mover.setProgress(pos * delta + delta/2);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
        boolean fromUser) {
      if (fromUser) {
        final int oldPos = mItemList.indexOf(mSelectedTracker);
        final int delta = 840 / mItemList.size();
        final int newPos = Math.min(progress / delta, mItemList.size()-1);

        if (oldPos != newPos) {
          mItemList.remove(mSelectedTracker);
          mItemList.add(newPos, mSelectedTracker);
          refreshView();
        }
      }
    }

    public final void setArrowPosition(View targetView) {
      if (mCurrentTarget != null) {
        mCurrentTarget.getViewTreeObserver().removeOnGlobalLayoutListener(this);
      }
      mCurrentTarget = targetView;
      onGlobalLayout();
      targetView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
      int[] location = new int[2];
      mCurrentTarget.getLocationOnScreen(location);
      int left = location[0];
      mPreviewHolder.getLocationInWindow(location);
      mArrowView.setTranslationX((left - location[0]) + (mCurrentTarget.getMeasuredWidth() - mArrowView.getMeasuredWidth()) / 2);
    }

    @Override
    public void onClick(View v) {
      if (v == cp_remove) {
        removeSelected();
      } else if (v == cp_change_icon) {
        changeIconForSelectedToggle();
      } else if (v == cp_edit_name) {
        cp_toggleName.setEnabled(true);
        cp_toggleName.setInputType(InputType.TYPE_CLASS_TEXT);
        cp_toggleName.selectAll();
        cp_toggleName.requestFocus();
        keyboard.showSoftInput(cp_toggleName, 0);
      }
    }

    abstract void removeSelected();

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      if(actionId == EditorInfo.IME_ACTION_DONE) {
        // Name edit done.
        String newName = v.getText().toString();
        SimpleShortcut shrt = (SimpleShortcut) mSelectedTracker.tracker;
        shrt = new SimpleShortcut(shrt.getIntent(), newName);

        TrackerItem newItem = new TrackerItem(shrt, mSelectedTracker.originalIcon, mContext);
        mItemList.add(mItemList.indexOf(mSelectedTracker), newItem);
        mItemList.remove(mSelectedTracker);
        mSelectedTracker = newItem;

        keyboard.hideSoftInputFromWindow(cp_toggleName.getWindowToken(), 0);
        showForSelectedToggle();
        return true;
      }
      return false;
    }

    public void showForSelectedToggle() {
      setEnabled(cp_remove, mItemList.size() > 1);
      setEnabled(cp_mover, mItemList.size() > 1);
      onStopTrackingTouch(cp_mover);

      cp_toggleName.setEnabled(false);
      cp_toggleName.setText(mSelectedTracker.tracker.getLabel(mTrackerNames));
      cp_toggleName.setInputType(InputType.TYPE_NULL);
      cp_toggleName.setVisibility(View.VISIBLE);

      cp_edit_name.setVisibility((mSelectedTracker.tracker instanceof SimpleShortcut) ? View.VISIBLE : View.INVISIBLE);
    };

    public void showAtLocation(View v) { }

    public void onTrackerListUpdated(TrackerItem newItem) {
      refreshView();
    }
	}

	public static class TrackerItem {
		public final AbstractTracker tracker;

		@Thunk ToggleBitmapProvider imageProvider;
		public Bitmap originalIcon;

		TrackerItem(AbstractTracker tracker, Bitmap icon, Context context) {
			this.tracker = tracker;
			setIcon(icon, context);
		}

		@Thunk void setIcon(Bitmap icon, Context context) {
			originalIcon = icon;
			imageProvider = tracker.getImageProvider(context, icon);
		}
	}

  @Thunk static void setEnabled(View v, boolean enabled) {
    v.setEnabled(enabled);
    v.setAlpha(enabled ? 1 : 0.4f);
  }
}
