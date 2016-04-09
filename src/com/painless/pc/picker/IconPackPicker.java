package com.painless.pc.picker;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.painless.pc.R;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.Thunk;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

/**
 * Activity to choose application icons as well as icons from apex and go themes.
 */
public class IconPackPicker extends Activity implements
    OnItemClickListener, OnQueryTextListener, OnActionExpandListener, OnColorChangedListener {

  @Thunk final ArrayList<BitmapInfo> mAllItems = new ArrayList<BitmapInfo>();

  private GridView mGrid;
  private PackageManager mPm;
  @Thunk ImageAdapter mAdapter;

  private int mDensity;
  private int mIconSize;
  private int[] m3DColors;
  private boolean mCropMax;

  private boolean mShowTint;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setResult(RESULT_CANCELED);
    mAdapter = new ImageAdapter(this);
    mAdapter.mColorFilter = getIntent().getIntExtra("color", Color.WHITE);

    mDensity = ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getLauncherLargeIconDensity();
    mIconSize = getIntent().getIntExtra("size", BitmapUtils.getIconSize(this));
    m3DColors = getIntent().getIntArrayExtra("colors3d");
    mCropMax = getIntent().getBooleanExtra("cropMax", false);
    mShowTint = getIntent().getBooleanExtra("showtint", false);

    setContentView(R.layout.theme_icon_picker);
    mGrid = (GridView) findViewById(R.id.grid);
    mGrid.setAdapter(mAdapter);
    mGrid.setOnItemClickListener(this);

    mPm = getPackageManager();
    String packageName = getIntent().getStringExtra("pkg");

    boolean loadDefault = true;
    if (packageName != null) {
      try {
        Resources res = mPm.getResourcesForApplication(packageName);
        if (res != null) {
          loadThemeIcons(res, packageName);
          setTitle(getIntent().getStringExtra("lbl"));
          getActionBar().setIcon(new BitmapDrawable(getResources(), (Bitmap) getIntent().getParcelableExtra("icon"))); 
          loadDefault = false;
        }
      } catch (NameNotFoundException e) {
        Debug.log(e);
      }
    } else if (getIntent().getBooleanExtra("appicons", false)) {
      loadAppIcons();
      setTitle(R.string.ip_app_icons);
      loadDefault = false;
    }
    if (loadDefault) {
      mAdapter.mUseFilter = true;
      loadThemeIcons();
    }

    mAdapter.addAll(mAllItems);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    SearchView searchView = new SearchView(this);
    searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
    searchView.setOnQueryTextListener(this);

    menu.add(android.R.string.search_go)
      .setIcon(android.R.drawable.ic_menu_search)
      .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW + MenuItem.SHOW_AS_ACTION_ALWAYS)
      .setActionView(searchView)
      .setOnActionExpandListener(this);

    if (mShowTint) {
      MenuItem item = menu.add(R.string.wc_tint);
      item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
      item.setActionView(R.layout.ab_color_btn);
      ColorButton cb = (ColorButton) item.getActionView().findViewById(R.id.btn_tint);
      cb.setColor(mAdapter.mColorFilter);
      cb.setOnColorChangeListener(this, true);
    }
    return true;
  }

  @Override
  public void onColorChanged(int color, View v) {
    mAdapter.mColorFilter = color;
    mAdapter.notifyDataSetChanged();
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    return true;
  }

  @Override
  public boolean onMenuItemActionCollapse(MenuItem item) {
    return onQueryTextSubmit("");
  }

  @Override
  public boolean onMenuItemActionExpand(MenuItem item) {
    return true;
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    if (TextUtils.isEmpty(query)) {
      mAdapter.clear();
      mAdapter.addAll(mAllItems);
      return true;
    }

    query = query.toLowerCase(Locale.ENGLISH);
    mAdapter.clear();
    for (BitmapInfo info : mAllItems) {
      if (info.label != null && info.label.contains(query)) {
        mAdapter.add(info);
      }
    }
    return true;
  }

  private void loadThemeIcons() {
    final R.drawable drawableResources = new R.drawable();
    final Class<R.drawable> c = R.drawable.class;
    final Field[] fields = c.getDeclaredFields();

    for (int i = 0, max = fields.length; i < max; i++) {
      try {
        if (fields[i].getName().startsWith("icon_")) {
          mAllItems.add(new BitmapInfo(getResources(), fields[i].getInt(drawableResources), fields[i].getName().substring(5)));
        }
      } catch (Throwable e) {
        continue;
      }
    }
  }

  private void loadAppIcons() {
    PackageManager pm = getPackageManager();
    List<ResolveInfo> infos = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
    for (ResolveInfo info : infos) {
      try {
        if (info.getIconResource() != 0) { 
          mAllItems.add(new BitmapInfo(pm.getResourcesForApplication(info.activityInfo.applicationInfo), info.getIconResource(), (info.loadLabel(pm) + "").toLowerCase(Locale.getDefault())));
        }
      } catch (NameNotFoundException e) {
        Debug.log(e);
      }
    }
  }

  private void loadThemeIcons(final Resources resources, final String pkg) {
    final HashSet<String> drawables = new HashSet<String>();
    parseAssets(resources, "appfilter", pkg, drawables);
    parseAssets(resources, "drawable", pkg, drawables);

    ArrayList<String> sorted = new ArrayList<>();
    sorted.addAll(drawables);
    Collections.sort(sorted);

    for (String item : sorted) {
      int id = resources.getIdentifier(item, "drawable", pkg);
      if (id > 0) {
        mAllItems.add(new BitmapInfo(resources, id, item.toLowerCase(Locale.ENGLISH)));
      }
    }
  }

  private void parseAssets(Resources res, String assetFile, String pkg, HashSet<String> target) {
    try {
      XmlPullParser parser;
      int xmlRes = res.getIdentifier(assetFile, "xml", pkg);
      if (xmlRes != 0) {
        parser = res.getXml(xmlRes);
      } else {
        InputStream is = res.getAssets().open(assetFile + ".xml");
        XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
        xmlFactory.setNamespaceAware(true);
        parser = xmlFactory.newPullParser();
        parser.setInput(is, "UTF-8");
      }

      while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
        if ((parser.getEventType() == XmlPullParser.START_TAG) && "item".equals(parser.getName())) {
          String drawable = parser.getAttributeValue(null, "drawable");
          if (!TextUtils.isEmpty(drawable)) {
            target.add(drawable);
          }
        }
        parser.next();
      }
    } catch (Throwable e) {
      Debug.log(e);
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Intent result = new Intent();
    BitmapInfo info = mAdapter.getItem(position);

    Bitmap icon;
    if (!mAdapter.mUseFilter) {
      Drawable d = info.res.getDrawableForDensity(info.resId, mDensity);
      icon = mCropMax ? BitmapUtils.cropMaxVisibleBitmap(d, mIconSize) : BitmapUtils.drawableToBitmap(d, mIconSize, true);
    } else if (m3DColors != null) {
      FolderIconCreater creator = new FolderIconCreater(this);
      icon = creator.createFolderIcon(info.resId, m3DColors);
    } else {
      Drawable d = info.res.getDrawableForDensity(info.resId, DisplayMetrics.DENSITY_XXHIGH).mutate().getConstantState().newDrawable();
      d.setColorFilter(new PorterDuffColorFilter(0xFF000000 | mAdapter.mColorFilter, PorterDuff.Mode.SRC_IN));
      d.setAlpha(Color.alpha(mAdapter.mColorFilter));
      icon = mCropMax ? BitmapUtils.cropMaxVisibleBitmap(d, mIconSize) : BitmapUtils.drawableToBitmap(d, mIconSize, false);
    }
    result.putExtra("icon", icon);
    setResult(RESULT_OK, result);
    finish();
  }

  private static class ImageAdapter extends ArrayAdapter<BitmapInfo> {

    @Thunk boolean mUseFilter = false;
    @Thunk int mColorFilter = Color.WHITE;
    private final LayoutInflater mInflator;
    private final int mIconSize;

    public ImageAdapter(Context context) {
      super(context, R.layout.theme_icon_picker_item);
      mInflator = LayoutInflater.from(context);
      mIconSize = BitmapUtils.getIconSize(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ImageView img = (ImageView) ((convertView == null) ?
          mInflator.inflate(R.layout.theme_icon_picker_item, parent, false) :
            convertView);
      BitmapInfo info = getItem(position);
      Drawable drawable = info.res.getDrawable(info.resId);
      drawable.setBounds(0, 0, mIconSize, mIconSize);
      img.setImageDrawable(drawable);

      if (mUseFilter) {
        img.setColorFilter(0xFF000000 | mColorFilter);
        img.setAlpha(Color.alpha(mColorFilter));
      }
      return img;
    }
  }

  private static class BitmapInfo {
    final int resId;
    final Resources res;
    final String label;

    BitmapInfo(Resources res, int resId, String lbl) {
      this.res = res;
      this.resId = resId;
      label = lbl;
    }
  }
}
