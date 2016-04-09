package com.painless.pc.folder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.nav.CFolderFrag;
import com.painless.pc.picker.IconPicker;
import com.painless.pc.picker.IconPicker.Callback;
import com.painless.pc.picker.TogglePicker;
import com.painless.pc.picker.TogglePicker.TogglePickerListener;
import com.painless.pc.settings.LaunchActivity;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.PluginDB;
import com.painless.pc.theme.FixedImageProvider;
import com.painless.pc.theme.LabelRVFactory;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.CallerActivity;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.UiUtils;

public class FolderActivity extends CallerActivity 
    implements OnItemLongClickListener, OnDragListener,
    Callback, OnItemClickListener, Runnable, TogglePickerListener {

  private static final long ANIM_DURATION = 200;

  private int mItemWidth, mMaxColumnCount;
  @Thunk int mDisplayWidth, mDisplayHeight;

  @Thunk View mContainer;
  @Thunk GridView mGrid;

  private IconPicker mPicker;

  @Thunk FolderDb mDb;
  @Thunk FolderToggleAdapter mAdapter;

  @Thunk int mDragIndex;
  @Thunk FolderItem mDragItem;
  private FolderItem mEmptyItem;
  private FolderShadow mShadow;
  private FolderItemOptions mItemOptions;

  @Thunk String mDbName;
  private boolean mFinishOnPause = true;

  private TogglePicker mTogglePicker;

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Uri data = getIntent().getData();
    if (data == null) {
      finish();
      return;
    }

    mDbName = data.getQuery();
    if (TextUtils.isEmpty(mDbName) || !mDbName.matches(FolderUtils.DB_NAME_REGX)) {
      finish();
      return;
    }

    getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

    setContentView(R.layout.folder_popup);
    mContainer = findViewById(R.id.container);
    mGrid = (GridView) findViewById(R.id.grid);

    // initial setup
    Point size = UiUtils.getDisplaySize(getWindowManager());
    mDisplayWidth = size.x;
    mDisplayHeight = size.y;

    mItemWidth = (int) getResources().getDimension(R.dimen.folder_item_width);
    mMaxColumnCount = mDisplayWidth / mItemWidth;

    // Initial load
    mDb = new FolderDb(this, mDbName);
    final Cursor cursor = mDb.getAllEntries();
    int total = cursor.getCount();
    
    final String settings;
    Drawable background = null;

    if (cursor.moveToFirst() && (cursor.getInt(FolderDb.POS_POSITION) == FolderDb.SETTING_POS)) {
      settings = cursor.getString(FolderDb.POS_DEFINITION);

      try {
        background = Drawable.createFromStream(new ByteArrayInputStream(cursor.getBlob(FolderDb.POS_ICON)), null);
      } catch (Exception e) { }
      total--;
    } else {
      settings = FolderUtils.DEFAULT_SETTINGS;
    }

    SettingsDecoder decoder = new SettingsDecoder(settings);

    // Apply theme
    if (background == null) {
      mContainer.setBackgroundResource(R.drawable.folder_back);
    } else {
      mContainer.setBackground(background);
      int[] padding = decoder.getRect(SettingsDecoder.KEY_PADDING);
      mContainer.setPadding(padding[0], padding[1], padding[2], padding[3]);
    }

    int labelColor = decoder.getValue(LabelRVFactory.KEY_COLOR, Color.BLACK);
    ((ImageView) findViewById(R.id.btn_add)).setColorFilter(labelColor);

    mAdapter = new FolderToggleAdapter(this, decoder, labelColor, this);

    if (total > 0) {
      // Add dummy entries to start with.
      FixedImageProvider ip = new FixedImageProvider(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8));
      SimpleShortcut shrt = new SimpleShortcut(new Intent(), "");
      while (total > 0) {
        mAdapter.add(new FolderItem(shrt, ip, 0));
        total--;
      }

      mAdapter.setNotifyOnChange(false);
      // Now load the actual toggles in an async task.
      new AsyncTask<Void, Void, ArrayList<FolderItem>>() {

        @Override
        protected ArrayList<FolderItem> doInBackground(Void... params) {
          return mDb.getAll(cursor);
        }

        @Override
        protected void onPostExecute(ArrayList<FolderItem> result) {
          mAdapter.clear();
          mAdapter.addAll(result);
          mGrid.setOnItemLongClickListener(FolderActivity.this);
          mGrid.setOnItemClickListener(FolderActivity.this);
        }
      }.execute();

    } else {
      mAdapter.add(mAdapter.getAddItem());
      mGrid.setOnItemLongClickListener(this);
      mGrid.setOnItemClickListener(this);
      cursor.close();
    }

    mGrid.setAdapter(mAdapter);
    setupView();

    TextView txtName = (TextView) findViewById(R.id.txt_toggle_name);
    txtName.setText(getSharedPreferences(FolderUtils.PREFS, MODE_PRIVATE).getString(FolderUtils.KEY_NAME_PREFIX + mDbName, ""));
    txtName.setTextColor(labelColor);
    txtName.setHintTextColor(labelColor);

    PCWidgetActivity.sUpdateHook = this;

    // Run animation.
    overridePendingTransition(0, 0);
    mContainer.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      
      @Override
      public void onGlobalLayout() {
        mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

        int centerX, centerY;
        int width = mContainer.getWidth(), height = mContainer.getHeight();
        Rect bounds = getIntent().getSourceBounds();
        if (bounds == null) {
          centerX = mDisplayWidth / 2;
          centerY = mDisplayHeight / 2;
        } else {
          centerX = bounds.centerX();
          centerY = bounds.centerY();
        }

        final WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.y = Math.min(Math.max(getResources().getDimensionPixelSize(R.dimen.status_bar_height), centerY - height/2), mDisplayHeight - height);
        lp.x = Math.min(Math.max(0, centerX - width/2), mDisplayWidth - width);
        getWindow().setAttributes(lp);

        int pivotX = centerX - lp.x;
        int pivotY = centerY - lp.y;
        if (Globals.IS_LOLLIPOP) {
          mContainer.setTranslationX(0.075f * (pivotX - width/2));
          mContainer.setTranslationY(0.075f * (pivotY - height/2));
          mContainer.setAlpha(0);


          int rx = (int) Math.max(Math.max(width - pivotX, 0), pivotX);
          int ry = (int) Math.max(Math.max(height - pivotY, 0), pivotY);
          float radius = (float) Math.sqrt(rx * rx + ry * ry);
          AnimatorSet set = new AnimatorSet();
          set.playTogether(
                  ViewAnimationUtils.createCircularReveal(mContainer, pivotX, pivotY, 0, radius),
                  ObjectAnimator.ofFloat(mContainer, View.TRANSLATION_X, 0),
                  ObjectAnimator.ofFloat(mContainer, View.TRANSLATION_Y, 0),
                  ObjectAnimator.ofFloat(mContainer, View.ALPHA, 1));
          set.setInterpolator(new DecelerateInterpolator(1));
          set.setDuration(200);
          set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
              mAdapter.setNotifyOnChange(true);
              mAdapter.notifyDataSetChanged();
            };
          });
          set.start();

        } else {
          mContainer.setScaleX(0.2f);
          mContainer.setScaleY(0.2f);
          mContainer.setTranslationX(pivotX - width/2);
          mContainer.setTranslationY(pivotY - height/2);
          mContainer.setAlpha(0.2f);

          mContainer.animate()
            .alpha(1)
            .scaleX(1)
            .scaleY(1)
            .translationX(0)
            .translationY(0)
            .setDuration(120)
            .withEndAction(new Runnable() {
              
              @Override
              public void run() {
                mAdapter.setNotifyOnChange(true);
                mAdapter.notifyDataSetChanged();
              }
            });
        }
      }
    });
  }

  private void setupView() {
    int toggleCount = mAdapter.getCount();
    int columnCount = Math.max(2, Math.min(mMaxColumnCount, (int) Math.ceil(Math.sqrt(toggleCount))));
    mGrid.getLayoutParams().width = (mItemWidth * columnCount);
  }

  public void onAddClicked(View v) {
    mFinishOnPause = false;
    if (mTogglePicker == null) {
      mTogglePicker = new TogglePicker(this, this);
    }
    mTogglePicker.show();
  }

  @Override
  protected void onNewIntent(Intent intent) {
     if (Intent.ACTION_SEARCH.equals(intent.getAction()) && (mTogglePicker != null)) {
       mTogglePicker.searchIntent(intent);
     }
  }

  @Override
  public void onTogglePicked(AbstractTracker tracker, Bitmap icon) {
    if (tracker instanceof PluginTracker) {
      PluginDB.get(this).save((PluginTracker) tracker);
    } else if (tracker instanceof SimpleShortcut) {
      SimpleShortcut shrt = (SimpleShortcut) tracker;
      try {
        shrt.setId(new JSONObject()
          .put(FolderDb.KEY_NAME, shrt.getLabel(null))
          .put(FolderDb.KEY_INTENT, shrt.getIntent().toUri(Intent.URI_INTENT_SCHEME)).toString());
      } catch (JSONException e) {
        Debug.log(e);
        return;
      }
    }
    int lastPosition = mAdapter.getItem(mAdapter.getCount() - 1).sortOrder + 1;
    mDb.addTracker(tracker, icon, lastPosition);
    mAdapter.remove(mAdapter.getAddItem());
    mAdapter.add(new FolderItem(tracker, tracker.getImageProvider(this, icon), lastPosition));
    setupView();
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    if (mAdapter.getCount() < 2) {
      return false;
    }
    
    mDragItem = mAdapter.getItem(position);
    mDragIndex = position;
    
    if (mItemOptions == null) {
      mItemOptions = new FolderItemOptions(this) {

        @Override
        public void onClick(View v) {
          onItemOptionsClicked(v.getId());
        }
      };
    }
    mItemOptions.show(view, mDragItem.first instanceof SimpleShortcut);

    if (mEmptyItem == null) {
      mEmptyItem = FolderItem.create(mGrid.getContext(), "", R.drawable.icon_bg_trans, 0);
    }
    if (mShadow == null) {
      mShadow = new FolderShadow(this);
    }
    mShadow.copy((FolderViews) view.getTag(), mDragItem.first.getLabel(getResources().getStringArray(R.array.tracker_names)));
    view.startDrag(null, mShadow, null, 0);
    mAdapter.remove(mDragItem);
    insertAtIndex(mEmptyItem, mDragIndex);

    Vibrator myVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    if (myVib != null) {
      myVib.vibrate(10);
    }
    return true;
  }

  @Override
  public boolean onDrag(View v, DragEvent event) {
    final int action = event.getAction();

    switch (action) {
      case DragEvent.ACTION_DRAG_STARTED:
        return true;
      case DragEvent.ACTION_DRAG_ENTERED: {
        Object tag = v.getTag();
        if (tag instanceof FolderViews) {
          FolderViews views = (FolderViews) tag;
          if (views.position < mDragIndex) {
            v.animate().translationX(v.getWidth() / 3).setDuration(ANIM_DURATION).start();
            mItemOptions.hide();
            return true;
          } else if (views.position > mDragIndex) {
            v.animate().translationX(-v.getWidth() / 3).setDuration(ANIM_DURATION).start();
            mItemOptions.hide();
            return true;
          } else {
            return false;
          }
        }
        return false;
      }
      case DragEvent.ACTION_DRAG_EXITED:
        v.animate().translationX(0).setDuration(ANIM_DURATION).start();
        return true;
      case DragEvent.ACTION_DROP: {
        Object tag = v.getTag();
        if (tag instanceof FolderViews) {
          FolderViews views = (FolderViews) tag;
          if (views.position != mDragIndex) {
            // Update DB
            int lastIndex = views.position;
            int finalPosition = mAdapter.getItem(lastIndex).sortOrder;
            mDb.move(mDragItem.sortOrder, finalPosition);

            // Update all references
            int delta = mDragIndex < lastIndex ? -1 : 1;
            for (int i = lastIndex; i != mDragIndex; i+=delta) {
              mAdapter.getItem(i).sortOrder += delta;
            }
            mDragItem.sortOrder = finalPosition;

            // Update UI
            mAdapter.remove(mEmptyItem);
            insertAtIndex(mDragItem, views.position);
            v.setTranslationX(0);
            return true;
          } else {
            mItemOptions.activate();
          }
        }
        return false;
      }
      case DragEvent.ACTION_DRAG_ENDED:
        if (!event.getResult()) {
          mAdapter.remove(mEmptyItem);
          mAdapter.remove(mDragItem);
          insertAtIndex(mDragItem, mDragIndex);
        }
    }
    return true;
  }

  @Thunk void onItemOptionsClicked(int id) {
    mItemOptions.hide();
    switch (id) {
      case R.id.mnu_delete: {
        mAdapter.remove(mDragItem);
        mDb.delete(mDragItem.sortOrder);
        setupView();
        break;
      }
      case R.id.mnu_change_icon: {
        // Change icon
        if (mPicker == null) {
          mPicker = new IconPicker(this);
        }
        mFinishOnPause = false;
        Bitmap originalIcon = mDb.getIcon(mDragItem.sortOrder);
        mPicker.pickTrackerIcon(mDragItem.first, this,
                mAdapter.getDefaultColor(), originalIcon);
        break;
      }
      case R.id.mnu_rename: {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
          .setTitle(R.string.act_rename)
          .setView(input)
          .setPositiveButton(R.string.act_done, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                String name = input.getText().toString();
                SimpleShortcut shrt = (SimpleShortcut) mDragItem.first;
                shrt = new SimpleShortcut(shrt.getIntent(), name);
                try {
                  shrt.setId(new JSONObject()
                    .put(FolderDb.KEY_NAME, shrt.getLabel(null))
                    .put(FolderDb.KEY_INTENT, shrt.getIntent().toUri(Intent.URI_INTENT_SCHEME)).toString());
                } catch (JSONException e) {
                  Debug.log(e);
                  return;
                }
                mAdapter.remove(mDragItem);
                mDragItem = new FolderItem(shrt, mDragItem.second, mDragItem.sortOrder);
                mAdapter.insert(mDragItem, mDragIndex);
                mDb.setShortcut(shrt, mDragItem.sortOrder);
              }
          }).setNegativeButton(R.string.act_cancel, null).show();
        input.setText(mDragItem.first.getLabel(null));
        input.selectAll();
      }
    }
  }

  @Override
  public void onIconReceived(Bitmap icon) {
    // Yay icon received.
    mDb.setIcon(icon, mDragItem.sortOrder);
    FolderItem newItem = new FolderItem(mDragItem.first, mDragItem.first.getImageProvider(this, icon), mDragItem.sortOrder);
    mAdapter.remove(mDragItem);
    insertAtIndex(newItem, mDragIndex);
  }

  private void insertAtIndex(FolderItem item, int index) {
    if (index < mAdapter.getCount()) {
      mAdapter.insert(item, index);
    } else {
      mAdapter.add(item);
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    FolderItem item = mAdapter.getItem(position);
    if (item == mAdapter.getAddItem()) {
      onAddClicked(null);
    } else {
      mFinishOnPause = true;
      
      if (item.first.trackerId == 33) { // Widget settings
        Bundle folderBundle = new Bundle();
        folderBundle.putString("id", mDbName);
        startActivity(new Intent(this, LaunchActivity.class)
          .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, CFolderFrag.class.getName())
          .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, folderBundle)
          .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, R.string.lbl_customize)
          .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE, R.string.lbl_customize)
          .putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
          
                );
      } else {
        // Ignore clicks to WidgetSettings
        if (item.first.trackerId > -1) {
          Globals.getAppPrefs(this).edit()
            .putLong("last_used_" + item.first.trackerId, System.currentTimeMillis())
            .commit();
        }

        item.first.toggleState(this);
        PCWidgetActivity.partialUpdateAllWidgets(this);
      }
    }
  }

  @Override
  public void run() {
    if (!isFinishing()) {
      mAdapter.notifyDataSetChanged();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mFinishOnPause) {
      runFinishAnimation();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
      runFinishAnimation();
      return true;
    }
    return super.onTouchEvent(event);
  }

  @Override
  public void onBackPressed() {
    runFinishAnimation();
  }

  private void runFinishAnimation() {
    PCWidgetActivity.sUpdateHook = null;
    overridePendingTransition(0, 0);

    mContainer.setEnabled(false);
    mGrid.setOnItemClickListener(null);
    mGrid.setOnItemLongClickListener(null);

    mContainer.animate()
      .setDuration(100)
      .alpha(0)
      .scaleX(0.7f)
      .scaleY(0.7f)
      .withEndAction(new Runnable() {
        
        @Override
        public void run() {
          finish();
          overridePendingTransition(0, 0);
        }
      });
  }

  @Override
  protected void onDestroy() {
    if (mTogglePicker != null) {
      mTogglePicker.dismiss();
    }
    super.onDestroy();
    PCWidgetActivity.sUpdateHook = null;
  }

  @Override
  protected void onResume() {
    super.onResume();
    mFinishOnPause = true;
  }

  @Override
  public void onDoneClicked() {}
}

