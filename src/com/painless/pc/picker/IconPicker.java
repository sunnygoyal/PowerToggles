package com.painless.pc.picker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;

import com.painless.pc.R;
import com.painless.pc.cfg.BatteryIconEditor;
import com.painless.pc.cfg.IconThemeEditor;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.BatteryTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.TimeoutTracker;
import com.painless.pc.util.ActivityInfo;
import com.painless.pc.util.CallerActivity;
import com.painless.pc.util.ImageLoadTask;
import com.painless.pc.util.SectionAdapter;
import com.painless.pc.util.SectionAdapter.SectionItem;
import com.painless.pc.util.Thunk;

public class IconPicker implements DialogInterface.OnClickListener, CallerActivity.ResultReceiver {

  private static final String AWD_ICON_PACK_ACTION = "org.adw.launcher.icons.ACTION_PICK_ICON";
  private static final String GO_THEME_LIST_ACTION = "com.gau.go.launcherex.theme";
  private static final String APEX_THEME_CATEGORY = "com.anddoes.launcher.THEME";

  private static final int PICK_APP_ICON = 1;
  private static final int PICK_IMAGE = 2;
  private static final int PICK_IMAGE_AND_CROP = 3;
  private static final int PICK_ICON_FROM_PACK = 4;

  private static final int TYPE_BATTERY = 0;
  private static final int TYPE_MULTI_ICON = 1;
  private static final int TYPE_SINGLE_ICON = 2;
  private static final int TYPE_SINGLE_ICON_WITH_COUNTER = 3;

  private static final int REQ_ICON_THEME_EDITOR = 22;
  private static final int REQ_BATTERY_THEME_EDITOR = 23;

  private final CallerActivity main;
  private final boolean mActivityPicker;
  private final int mIconSize;
  private final boolean mCropMax;
  private final boolean mShowTint;

  public int[] icon3dColors;

  private SectionAdapter adapter;
  private AlertDialog dialog;

  private ArrayList<ActivityInfo> iconPackList;

  private int colorfilter;
  @Thunk Callback callback;
  private SectionItem mExtraItem;

  private Bitmap mCounterIcon;
  private boolean mAllowCounter = false;

  public IconPicker(CallerActivity main) {
    this(main, false);
  }

  public IconPicker(CallerActivity main, boolean activityPicker) {
    this.main = main;
    mActivityPicker = activityPicker;
    mIconSize = mActivityPicker ? BitmapUtils.getActivityIconSize(main) : BitmapUtils.getIconSize(main);
    mCropMax = false;
    mShowTint = false;
  }

  public IconPicker(CallerActivity main, int iconSize) {
    this.main = main;
    mActivityPicker = false;
    mIconSize = iconSize;
    mCropMax = true;
    mShowTint = true;
  }

  /**
   * Pops up a dialog to pick an icon.
   * @param callback to return the icon
   * @param colorfilter the color to use for the icon or null
   */
  public void pickIcon(Callback callback, int colorfilter) {
    if (dialog == null) {
      adapter = new SectionAdapter(main);

      // Add general items
      Resources res = main.getResources();
      adapter.addItem(res.getString(R.string.ip_theme_icons), R.drawable.icon_theme);
      if (mActivityPicker) {
        Bitmap defaulIcon = BitmapUtils.drawableToBitmap(res.getDrawable(R.drawable.folder_icon), main);
        adapter.addItem(res.getString(R.string.lbl_default), new BitmapDrawable(res, defaulIcon));
      } else {
        adapter.addItem(res.getString(R.string.ip_counter), R.drawable.icon_counter);
      }
      mExtraItem = adapter.getItem(1);

      adapter.addItem(res.getString(R.string.ip_app_icons), R.drawable.icon_application);
      adapter.addItem(res.getString(R.string.ip_image_pick), R.drawable.icon_config_picture);
      adapter.addItem(res.getString(R.string.ip_image_crop), R.drawable.icon_crop);

      // Load all icons
      iconPackList = new ArrayList<ActivityInfo>();
      // Add AWD_ICON_PACK_ACTION the first so that it is overridden in case of duplicates
      iconPackList.addAll(loadThemeList(AWD_ICON_PACK_ACTION, Intent.CATEGORY_DEFAULT));
      iconPackList.addAll(loadThemeList(GO_THEME_LIST_ACTION, Intent.CATEGORY_DEFAULT));
      iconPackList.addAll(loadThemeList(Intent.ACTION_MAIN, APEX_THEME_CATEGORY));
      // Remove duplicates
      HashMap<String, ActivityInfo> itemMap = new HashMap<String, ActivityInfo>();
      for (ActivityInfo item : iconPackList) {
        itemMap.put(item.packageName, item);
      }
      iconPackList.clear();
      iconPackList.addAll(itemMap.values());

      if (iconPackList.size() > 0) {
        Collections.sort(iconPackList);
        adapter.addHeader(res.getString(R.string.ip_icon_pack));
        for (ActivityInfo item : iconPackList) {
          adapter.addItem(item.label, item.icon);
        }
      }

      dialog = new AlertDialog.Builder(main)
      .setTitle(R.string.ip_title)
      .setAdapter(adapter, this)
      .create();

      main.registerDialog(dialog);
    }

    this.callback = callback;
    this.colorfilter = colorfilter;
    
    adapter.remove(mExtraItem);
    if (mActivityPicker) {
      adapter.insert(mExtraItem, 0);
    } else if (mAllowCounter) {
      adapter.insert(mExtraItem, 1);
    }

    dialog.show();
  }

  /**
   * Shows a icon picker/editor bated on the type of the tracker.
   */
  public void pickTrackerIcon(AbstractTracker tracker, Callback callback, int colorfilter, Bitmap originalIcon) {
    this.callback = callback;
    mCounterIcon = null;
    mAllowCounter = false;
    switch(getTrackerType(tracker, originalIcon)) {
      case TYPE_SINGLE_ICON:
        pickIcon(callback,
            // If there is only one config, it means it is a simple shortcut or an abstract shortcut.
            tracker.buttonConfig.length > 2 ? Color.WHITE : colorfilter);
        break;
      case TYPE_SINGLE_ICON_WITH_COUNTER:
        mAllowCounter = true;
        mCounterIcon = originalIcon;
        pickIcon(callback, Color.WHITE);
        break;
      case TYPE_MULTI_ICON:
        Intent editorIntent = new Intent(main, IconThemeEditor.class)
            .putExtra(IconThemeEditor.ICON_STRIP, originalIcon)
            .putExtra(IconThemeEditor.TRACKER_NAME, tracker.getLabel(main.getResources().getStringArray(R.array.tracker_names)))
            .putExtra(IconThemeEditor.ALLOW_NULL, tracker.trackerId != PluginTracker.TRACKER_ID);
        
        if (tracker.trackerId == PluginTracker.TRACKER_ID) {
          int[] fakeConfig = new int[2 * originalIcon.getWidth() / originalIcon.getHeight()];
          Arrays.fill(fakeConfig, R.drawable.icon_tasker);
          editorIntent.putExtra(IconThemeEditor.ICON_CONFIG, fakeConfig);
        } else {
          editorIntent.putExtra(IconThemeEditor.ICON_CONFIG, tracker.buttonConfig);
        }
        main.requestResult(REQ_ICON_THEME_EDITOR, editorIntent, this);
        break;
      case TYPE_BATTERY:
        Intent bidIntent = new Intent(main, BatteryIconEditor.class);
        bidIntent.putExtra(BatteryIconEditor.IMAGE, originalIcon);
        main.requestResult(REQ_BATTERY_THEME_EDITOR, bidIntent, this);
    }
  }


  private ArrayList<ActivityInfo> loadThemeList(String action, String category) {
    return ActivityInfo.loadList(main, new Intent(action).addCategory(category));
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    Intent intent = null;
    if (mActivityPicker) {
      if (which == 0) {
        callback.onIconReceived(null);
        return;
      }
      which --;
    }
    if (mAllowCounter && which > 0) {
      if (which == 1) {
        main.requestResult(REQ_BATTERY_THEME_EDITOR,
                new Intent(main, BatteryIconEditor.class).putExtra(BatteryIconEditor.IMAGE, mCounterIcon),
                this);
        return;
      }
      which --;
    }
    switch (which) {
      case 0:
        intent = new Intent(main, IconPackPicker.class)
          .putExtra("color", colorfilter)
          .putExtra("colors3d", icon3dColors)
          .putExtra("cropMax", mCropMax)
          .putExtra("showtint", mShowTint)
          .putExtra("size", mIconSize);
        which = PICK_ICON_FROM_PACK;
        break;
      case PICK_APP_ICON:
        intent = new Intent(main, IconPackPicker.class)
          .putExtra("appicons", true)
          .putExtra("size", mIconSize)
          .putExtra("cropMax", mCropMax);
        break;
      case PICK_IMAGE:
      case PICK_IMAGE_AND_CROP:
        intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        setOutputExtra(intent);
        break;
      default :
        int index = which - PICK_ICON_FROM_PACK - 1;
        if ((which > PICK_ICON_FROM_PACK) && (index < iconPackList.size())) {
          ActivityInfo item = iconPackList.get(index);
          if (item.targetIntent.getAction().equals(AWD_ICON_PACK_ACTION)) {
            intent = item.targetIntent;
          } else {
            intent = new Intent(main, IconPackPicker.class)
              .putExtra("pkg", item.packageName)
              .putExtra("lbl", item.label)
              .putExtra("icon", item.originalIcon)
              .putExtra("size", mIconSize)
              .putExtra("cropMax", mCropMax);
          }
          which = PICK_ICON_FROM_PACK;
        }
    }
    if (intent != null) {
      main.requestResult(which, intent, this);
    }
  }

  @Override
  public void onResult(int requestCode, Intent data) {
    Bitmap icon = null;
    try {
      switch (requestCode) {
        case REQ_ICON_THEME_EDITOR:
          icon = data.getParcelableExtra(IconThemeEditor.ICON_STRIP);
          callback.onIconReceived(icon);
          return;
        case REQ_BATTERY_THEME_EDITOR:
          icon = data.getParcelableExtra(BatteryIconEditor.IMAGE);
          callback.onIconReceived(icon);
          return;
        case PICK_IMAGE:
          new ImageLoadTask(main, mIconSize) {
            
            @Override
            protected void onSuccess(Bitmap result) {
              callback.onIconReceived(result);
            }
          }.checkAndExecute(data.getData());
          break;
        case PICK_IMAGE_AND_CROP: {
          Uri uri = data.getData();
          Intent intent = new Intent("com.android.camera.action.CROP").setDataAndType(uri, "image/*");
          setOutputExtra(intent);
          intent.putExtra("crop", "true")
            .putExtra("scale", true)
            .putExtra("scaleUpIfNeeded", true)
            .putExtra("aspectX", 1)
            .putExtra("outputFormat", "png")
            .putExtra("aspectY", 1)
            .putExtra("outputX", mIconSize)
            .putExtra("outputY", mIconSize)
            .putExtra("noFaceDetection", false);
          main.requestResult(PICK_IMAGE, intent, this);
          return;
        }
        case PICK_ICON_FROM_PACK:
        case PICK_APP_ICON:
          icon = data.getParcelableExtra("icon");
          break;
      }
      if (icon != null) {
        icon = Bitmap.createScaledBitmap(icon, mIconSize, mIconSize, true);
      }
    } catch (Exception e) {
      Debug.log(e);
    }

    if (icon != null) {
      callback.onIconReceived(icon);
    }
  }

  private void setOutputExtra(Intent intent) {
    Uri output = Uri.parse("content://com.painless.pc.file/crop");
    intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, output));
  }

  public static interface Callback {
    void onIconReceived(Bitmap icon);
  }

  private static int getTrackerType(AbstractTracker tracker, Bitmap icon) {
    if (tracker instanceof BatteryTracker) {
      return TYPE_BATTERY;
    }
    if (tracker instanceof TimeoutTracker) {
      return TYPE_SINGLE_ICON_WITH_COUNTER;
    }
    if ((tracker.trackerId == PluginTracker.TRACKER_ID) && (icon != null)) {
      int height = icon.getHeight();
      int width = icon.getWidth();

      if ((width > 0) && (height > 0)) {
        return (width / height >= 2) ? TYPE_MULTI_ICON :
          (width == (height + 1) ? TYPE_BATTERY : TYPE_SINGLE_ICON);
      }
      return TYPE_SINGLE_ICON;
    }
    final int[] config = tracker.buttonConfig;
    final int imgId = config[1];
    for (int i=3; i<config.length; i+=2) {
      if (imgId != config[i]) {
        return TYPE_MULTI_ICON;
      }
    }

    return TYPE_SINGLE_ICON;
  }
}
