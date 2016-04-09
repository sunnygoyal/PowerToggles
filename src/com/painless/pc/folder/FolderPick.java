package com.painless.pc.folder;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

import com.painless.pc.R;
import com.painless.pc.nav.CFolderFrag;
import com.painless.pc.nav.FolderFrag;
import com.painless.pc.picker.IconPicker;
import com.painless.pc.picker.IconPicker.Callback;
import com.painless.pc.util.CallerActivity;

public class FolderPick extends CallerActivity implements TabContentFactory, OnTabChangeListener, OnClickListener, Callback  {

  private CFolderFrag mNewFolderFrag;
  private Fragment mExistingFolderFragmet;

  private ImageView mIconSelect;
  private Bitmap mIcon;
  private IconPicker mIconPicker;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setResult(RESULT_CANCELED);
    setContentView(R.layout.folder_picker);

    TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
    tabHost.setup();
    
    tabHost.addTab(tabHost.newTabSpec("t1").setIndicator(getString(R.string.folder_new)).setContent(this));
    tabHost.addTab(tabHost.newTabSpec("t2").setIndicator(getString(R.string.folder_existing)).setContent(this));
    tabHost.setCurrentTab(0);
    tabHost.setOnTabChangedListener(this);

    onTabChanged(null);

    mIconPicker = new IconPicker(this, true);
    mIconSelect = (ImageView) findViewById(R.id.folder_icon);
    mIconSelect.setOnClickListener(this);
  }

  @Override
  public void onTabChanged(String tabId) {
    Fragment currentFrag;
    if ("t2".equals(tabId)) {
      mExistingFolderFragmet = getIfNull(FolderFrag.class, mExistingFolderFragmet);
      currentFrag = mExistingFolderFragmet;
    } else {
      mNewFolderFrag = (CFolderFrag) getIfNull(CFolderFrag.class, mNewFolderFrag);
      currentFrag = mNewFolderFrag;
    }
    
    FragmentTransaction transaction = getFragmentManager().beginTransaction();

    // Replace whatever is in the fragment_container view with this fragment,
    // and add the transaction to the back stack
    transaction.replace(android.R.id.tabcontent, currentFrag);

    // Commit the transaction
    transaction.commit();
  }

  private Fragment getIfNull(Class<?> fragClass, Fragment toCheck) {
    return (toCheck == null) ? Fragment.instantiate(this, fragClass.getName()) : toCheck;
  }

  @Override
  public View createTabContent(String tag) {
    return new View(this);
  }

  private void returnFolder(String dbName, String shrtName, String settings) {
    int icon = R.drawable.folder_icon;
    // The result we are passing back from this activity
    setResult(RESULT_OK, new Intent()
      .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(this, FolderActivity.class).setAction(FolderUtils.ACTION).setData(Uri.parse("folder/?" + dbName)))
      .putExtra(Intent.EXTRA_SHORTCUT_NAME, shrtName)
      .putExtra(Intent.EXTRA_SHORTCUT_ICON, mIcon)
      .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, icon)));
    finish();
  }

  public void returnFolder(String dbName, String shrtName) {
    FolderDb db = new FolderDb(this, dbName);
    String settings = db.getSettings();
    db.close();
    returnFolder(dbName, shrtName, settings);
  }

  public void returnNew(String settings, String shrtName, byte[] background) {
    String dbName = FolderUtils.newDbName(this);
    // create a database file
    FolderDb db = new FolderDb(this, dbName);
    db.saveSettigns(settings, background);
    db.close();

    shrtName = TextUtils.isEmpty(shrtName) ? getString(R.string.folder_name) : shrtName;
    FolderUtils.setName(shrtName, dbName, this);
    returnFolder(dbName, shrtName, settings);
  }

  @Override
  public void onDoneClicked() { }

  @Override
  public void onClick(View v) {
    if (mNewFolderFrag != null) {
      mIconPicker.icon3dColors = mNewFolderFrag.getCurrentColors();
    }
    mIconPicker.pickIcon(this, Color.WHITE);
  }

  @Override
  public void onIconReceived(Bitmap icon) {
    mIcon = icon;
    if (icon == null) {
      mIconSelect.setImageResource(R.drawable.folder_icon);
    } else {
      mIconSelect.setImageBitmap(icon);
    }
  }
}
