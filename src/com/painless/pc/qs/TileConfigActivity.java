package com.painless.pc.qs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.ProxyActivity;
import com.painless.pc.R;
import com.painless.pc.nav.SettingsFrag;
import com.painless.pc.picker.IconPicker;
import com.painless.pc.picker.SimpleTaskerTaskPicker;
import com.painless.pc.picker.TogglePicker;
import com.painless.pc.picker.TogglePicker.TogglePickerListener;
import com.painless.pc.settings.LaunchActivity;
import com.painless.pc.singleton.BackupUtil;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.singleton.PluginDB;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.singleton.WidgetDB;
import com.painless.pc.theme.RVFactory;
import com.painless.pc.tracker.AbstractCommand;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.ImportExportActivity;
import com.painless.pc.util.Thunk;

public class TileConfigActivity extends ImportExportActivity<QTInfo> implements OnCheckedChangeListener, TogglePickerListener, IconPicker.Callback {

  public TileConfigActivity() {
    super(R.menu.qs_cfg_menu, ".tile.zip",
        R.string.wp_backup, R.array.wc_export_msg,
        R.string.wp_restore, R.array.wc_import_msg);
  }

  public static final String EXTRA_KEY = "widget_key";

  private final int[] mColorAlphas = new int[] {
      60,   // COLOR_DEFAULT
      120,  // COLOR_WORKING
      255   // COLOR_ON
  };

  private TogglePicker mTogglePicker;
  private IconPicker mIconPicker;

  private String mKey;
  private int mWidgetId;
  private AbstractTracker mClickTracker;
  private SimpleShortcut mLongClickTracker;
  private Bitmap[] mIconBitmaps;
  private boolean mAllSameIcons;

  private int mIconSize;
  private CheckBox mCustomLabelChk;
  private TextView mCustomLabel;
  private ImageView[] mIcons;
  private boolean mClickTargetActive;

  private boolean mIsExistingTile;
  private int mWaitingIconIndex;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mKey = getIntent().getStringExtra(EXTRA_KEY);
    if (TextUtils.isEmpty(mKey)) {
      finish();
      return;
    }
    addActionDoneButton();
    setContentView(R.layout.qs_config_activity);

    mCustomLabelChk = (CheckBox) findViewById(R.id.chk_custom_label);
    mCustomLabelChk.setOnCheckedChangeListener(this);
    mCustomLabel = (TextView) findViewById(R.id.edt_input);

    mIcons = new ImageView[4];
    for (int i = 0; i < 4; i++) {
      mIcons[i] = (ImageView) findViewById(Globals.BUTTONS[i]);
      mIcons[i].setTag(i);
    }

    mIconSize = getResources().getDimensionPixelSize(R.dimen.qs_tile_icon_size);
    try {
      Resources sysRes = getPackageManager().getResourcesForApplication("com.android.systemui");
      int dimId = sysRes.getIdentifier("qs_tile_icon_size", "dimen", "com.android.systemui");
      if (dimId != 0) {
        mIconSize = getResources().getDimensionPixelSize(R.dimen.qs_tile_icon_size);
      }
    } catch (Exception e) {
      Debug.log(e);
    }
    mIconPicker = new IconPicker(this, mIconSize);

    QTInfo existingTile = QTStorage.getTile(mKey, this);
    mIsExistingTile = existingTile != null;
    if (mIsExistingTile) {
      mWidgetId = existingTile.widgetId;
      try {
        for (int i = 0; i < existingTile.icons.length; i++) {
          existingTile.loadIcon(i, this);
        }
        applyExisitingTile(existingTile);
      } catch (Exception e) {
        Debug.log(e);
        loadDefaultTile();
      }
    } else {
      mWidgetId = QTStorage.generateNewId(this);
      loadDefaultTile();
    }
  }

  private void loadDefaultTile() {
    SimpleShortcut defaultTracker = new SimpleShortcut(
        getPackageManager().getLaunchIntentForPackage(getPackageName()),
        getString(R.string.app_name));
    setupClickTracker(defaultTracker);
    setupLongClickTracker(defaultTracker);
  }

  private void applyExisitingTile(QTInfo existingTile) throws JSONException {
    if (existingTile.tracker != null) {
      setupClickTracker(existingTile.tracker);
    } else {
      setupClickTracker(new SimpleShortcut(existingTile.clickIntent, existingTile.contentDescription));
    }
    setupLongClickTracker(new SimpleShortcut(existingTile.longClickIntent, existingTile.longContentDescription));

    for (int i = 0; i < existingTile.icons.length; i++) {
      mIconBitmaps[i] = BitmapFactory.decodeByteArray(existingTile.icons[i], 0, existingTile.icons[i].length);
      mIcons[i].setImageBitmap(mIconBitmaps[i]);
    }

    if (existingTile.customLabel != null) {
      mCustomLabel.setText(existingTile.customLabel);
      mCustomLabelChk.setChecked(true);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
     if (Intent.ACTION_SEARCH.equals(intent.getAction()) && (mTogglePicker != null)) {
       mTogglePicker.searchIntent(intent);
     }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.qs_cfg_menu, menu);
    menu.findItem(R.id.mnu_delete).setVisible(mIsExistingTile);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.ui_prefs :
        startActivity(new Intent(this, LaunchActivity.class).putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsFrag.class.getName()));
        return true;
      case R.id.mnu_delete :
        new AlertDialog.Builder(this)
          .setTitle(R.string.qs_delete)
          .setMessage(R.string.qs_delete_msg)
          .setNegativeButton(R.string.act_cancel, null)
          .setPositiveButton(R.string.act_delete, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              deleteTile();
            }
          }).show();
        return true;
      default :
        return super.onOptionsItemSelected(item);
    }
  }

  @Thunk void deleteTile() {
    sendBroadcast(new Intent(mKey).putExtra("visible", false));
    QTStorage.deleteTile(mKey, this);
    WidgetDB.get(this).deleteToggles(mWidgetId);

    SettingStorage.clearCache();
    PCWidgetActivity.updateAllWidgets(this, true);
    SettingStorage.cleanupCachedPlugins(this);
    SettingStorage.updateConnectivityReceiver(this);
    finish();
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    mCustomLabel.setEnabled(isChecked);
  }

  private void initTogglePicker() {
    if (mTogglePicker == null) {
      mTogglePicker = new TogglePicker(this, this) {
        @Override
        protected void showTarkerPicker(ArrayList<String> tasks) {
          new SimpleTaskerTaskPicker(TileConfigActivity.this, TileConfigActivity.this, tasks);
        }
      };
    }
  }

  public void onClickActionClicked(View v) {
    initTogglePicker();
    mTogglePicker.setTitle(R.string.qs_cfg_click);
    mTogglePicker.setOnlyAppsMode(false);
    mClickTargetActive = true;
    mTogglePicker.show();
  }

  public void onLongClickActionClicked(View v) {
    initTogglePicker();
    mTogglePicker.setTitle(R.string.qs_cfg_long_click);
    mTogglePicker.setOnlyAppsMode(true);
    mClickTargetActive = false;
    mTogglePicker.show();
  }

  public void onPreviewButtonClicked(View v) {
    mWaitingIconIndex = (Integer) v.getTag();
    int color = Color.WHITE;
    if (mAllSameIcons) {
      int alpha = mColorAlphas[mClickTracker.buttonConfig[2 * mWaitingIconIndex]];
      color = ParseUtil.addAlphaToColor(alpha, color);
    }
    mIconPicker.pickIcon(this, color);
  }

  @Override
  public void onIconReceived(Bitmap icon) {
    maximizeImageAndApply(icon, mWaitingIconIndex);
  }

  private void maximizeImageAndApply(Bitmap icon, int index) {
    Bitmap cropped = icon;

    mIcons[index].setImageBitmap(cropped);
    mIconBitmaps[index] = cropped;
  }

  private QTInfo buildTile() {
    // Build settings
    QTInfo tileInfo = new QTInfo();

    Uri data = Uri.parse("tracker/?" + mClickTracker.getId());
    if (mClickTracker.trackerId == 33) { // Widget settings
      tileInfo.clickIntent = new Intent(this, TileConfigActivity.class).putExtra(EXTRA_KEY, mKey);
    } else if (mClickTracker instanceof AbstractCommand) {
      AbstractCommand cmd = (AbstractCommand) mClickTracker;
      cmd.mContext = getApplicationContext();
      tileInfo.clickIntent = cmd.getIntent();
    }
    tileInfo.contentDescription = ((TextView) findViewById(R.id.btn_click_action).findViewById(android.R.id.text1)).getText().toString();
    tileInfo.longContentDescription = ((TextView) findViewById(R.id.btn_long_click_action).findViewById(android.R.id.text1)).getText().toString();

    if (tileInfo.clickIntent == null && mClickTracker.shouldProxy(this)) {
      tileInfo.clickIntent = new Intent(this, ProxyActivity.class).setData(data);
      tileInfo.requiresUpdate = true;
    }

    if (tileInfo.clickIntent == null) {
      tileInfo.requiresUpdate = true;
      tileInfo.clickBroadcast = RVFactory.makeIntent(this, data);
    }

    if (mCustomLabelChk.isChecked()) {
      tileInfo.customLabel = mCustomLabel.getText().toString();
    }

    tileInfo.longClickIntent = mLongClickTracker.getIntent();
    tileInfo.widgetId = mWidgetId;
    tileInfo.tracker = mClickTracker;
    tileInfo.icons = new byte[mIconBitmaps.length][];

    return tileInfo;
  }

  @Override
  public void onDoneClicked() {
    // Build settings
    QTInfo tileInfo = buildTile();

    if (mClickTracker instanceof PluginTracker) {
      PluginDB.get(this).save((PluginTracker) mClickTracker);
    }

    WidgetDB.get(this).deleteToggles(mWidgetId);
    for (int i = 0; i < mIconBitmaps.length; i++) {
      WidgetDB.get(this).saveIcon((mWidgetId << 3) + i, mIconBitmaps[i]);
    }

    try {
      String settings = tileInfo.encodeToString();
      SettingStorage.clearCache();
      QTStorage.saveTileInfo(mKey, settings, this);
      PCWidgetActivity.updateAllWidgets(this, true);
      SettingStorage.cleanupCachedPlugins(this);
      SettingStorage.updateConnectivityReceiver(this);

      finish();
    } catch (Exception e) {
      Debug.log(e);
    }
  }

  @Override
  public void onTogglePicked(AbstractTracker tracker, Bitmap icon) {
    if (mClickTargetActive) {
      setupClickTracker(tracker);
      if (icon != null) {
        mWaitingIconIndex = 0;
        onIconReceived(icon);
      }
    } else if (tracker instanceof SimpleShortcut) {
      setupLongClickTracker((SimpleShortcut) tracker);
    }
  }

  private void setupClickTracker(AbstractTracker tracker) {
    String label = tracker.getLabel(getResources().getStringArray(R.array.tracker_names));
    setSummary(R.id.btn_click_action, label);
    mCustomLabel.setText(label);

    mClickTracker = tracker;

    int[] config = tracker.buttonConfig;
    mIconBitmaps = new Bitmap[config.length / 2];

    mAllSameIcons = config.length > 2;
    for (int i = 3; i < config.length; i+=2) {
      mAllSameIcons &= config[i] == config[1];
    }

    for (int i = 0; i < config.length; i+=2) {
      Drawable icon = getResources().getDrawable(config[i + 1]);
      if (mAllSameIcons) {
        icon = icon.mutate().getConstantState().newDrawable();
        icon.setAlpha(mColorAlphas[config[i]]);
      }
      Bitmap bitmap = BitmapUtils.cropMaxVisibleBitmap(icon, mIconSize);
      maximizeImageAndApply(bitmap, i / 2);
    }

    for (int i = 0; i < 4; i++) {
      mIcons[i].setVisibility((i < config.length / 2) ? View.VISIBLE : View.GONE);
    }
  }

  private void setupLongClickTracker(SimpleShortcut tracker) {
    mLongClickTracker = tracker;
    setSummary(R.id.btn_long_click_action, tracker.getLabel(getResources().getStringArray(R.array.tracker_names)));
  }

  private void setSummary(int id, String summary) {
    ((TextView) findViewById(id).findViewById(android.R.id.text1)).setText(summary);
  }

  @Override
  public void doExport(OutputStream fileOut) throws Exception {
    QTInfo tileInfo = buildTile();
    
    ZipOutputStream out = new ZipOutputStream(fileOut);
    for (int i = 0; i < tileInfo.icons.length; i++) {
      BackupUtil.addImage(out, mIconBitmaps[i], "icon_" + i);
    }

    out.putNextEntry(new ZipEntry("config.txt"));
    out.write(tileInfo.encodeToString().getBytes());
    out.closeEntry();
    out.close();
  }

  @Override
  public QTInfo doImportInBackground(File importFile) throws Exception {
    ZipFile zip = new ZipFile(importFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry("config.txt"))));
    QTInfo tileInfo = QTInfo.parse(reader.readLine(), this);
    reader.close();

    for (int i = 0; i < tileInfo.icons.length; i++) {
      tileInfo.icons[i] = ParseUtil.readStream(zip.getInputStream(zip.getEntry("icon_" + i)));
    }
    zip.close();
    return tileInfo;
  }

  @Override
  public void onPostImport(QTInfo result) {
    try {
      applyExisitingTile(result);
    } catch (Exception e) {
      Debug.log(e);
    }
  }
}
