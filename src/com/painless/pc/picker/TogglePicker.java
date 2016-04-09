package com.painless.pc.picker;

import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.painless.pc.R;
import com.painless.pc.TrackerManager;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.ActivityInfo;
import com.painless.pc.util.CallerActivity;
import com.painless.pc.util.CallerActivity.ResultReceiver;
import com.painless.pc.util.SectionAdapter;
import com.painless.pc.util.SectionAdapter.SectionItem;
import com.painless.pc.util.Thunk;
import com.painless.pc.view.SwipePager;
import com.painless.pc.view.SwipePager.OnPageChangeListener;

public class TogglePicker extends Dialog
    implements OnActionExpandListener, TabContentFactory, OnItemClickListener,
    OnTabChangeListener, OnPageChangeListener, ResultReceiver {

  private static final String TASKER_PACKAGE = "net.dinglisch.android.tasker";
  private static final String TASKER_PACKAGE_MARKET = TASKER_PACKAGE + "m";

  private static final int REQ_CUSTOM_SHORTCUT = 22;
  private static final int REQ_PLUGIN_CONFIG = 23;

  private static final int INDEX_ADAPTER_SEARCH = 4;
  private static final int INDEX_ADAPTER_SHORTCUTS = 3;
  private static final int INDEX_ADAPTER_CUSTOM = 2;

  @Thunk final int[][] mWidgetSections = new int[][] {
          new int[] { 1, 11, 0, 26, 12},                       // Mobile data
          new int[] { 3, 2, 28, 8, 6, 22, 24, 41, 42},         // Network
          new int[] { 18, 19, 20, 21},                         // Multimedia
          // Display
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ?
                  new int[] { 7, 17, 23, 13, 16, 9, 31, 38} :
                    new int[] {  7, 17, 23, 13, 16, 9, 31, 38, 47 }),

          new int[] { 4, 5, 25, 45, 10, 27, 15, 40, 43, 44},   // Hardware
          new int[] { 33, 32, 34},                             // App Commands

          // Root
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ?
                  new int[] { 29, 30, 35, 39, 36, 37} :
                    new int[] { 29, 30, 35, 39, 46, 36, 37}),

                    new int[] { 14}           // Others
  };

  @Thunk final ArrayList<SectionItem> mFullToggleList = new ArrayList<SectionItem>();

  @Thunk final CallerActivity mProxy;

  private final SearchView mSearchView;
  private final TabHost mTabHost;
  private final ViewSwitcher mRootLayout;
  private final SwipePager mPager;

  private MenuItem mSearchItem;
  private View mTogglesPage;

  @Thunk final LoadingSectionAdapter[] mAdapters = new LoadingSectionAdapter[5];

  private final TogglePickerListener mListener;

  // A counter for the loading progress.
  @Thunk int mLoadCount = 0;
  @Thunk Intent mLastSearchIntent;
  private ActivityInfo mWaitingPluginConfig;

  private boolean mShowOnlyApps = false;

  public TogglePicker(CallerActivity proxy, TogglePickerListener listener) {
    super(proxy, R.style.activityTheme);
    mProxy = proxy;
    mProxy.registerDialog(this);
    mListener = listener;

    setContentView(R.layout.toggle_picker_activity);
    setTitle(R.string.wc_add_toggle);

    mSearchView = new SearchView(getContext());
    mSearchView.setSearchableInfo(
            ((SearchManager) mProxy.getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(mProxy.getComponentName()));

    mRootLayout = (ViewSwitcher) findViewById(R.id.rootLayout);
    mTabHost = (TabHost) findViewById(android.R.id.tabhost);
    mTabHost.setup();
    mPager = (SwipePager) findViewById(R.id.swipeTabs);
    setupTabs();

    for (int i = 0; i < 5; i++) {
      ListView list;
      if (i < 3) {
        list = new ListView(mProxy);
        mPager.addView(list);
      } else {
        list = (ListView) findViewById(android.R.id.list);
      }
      list.setOnItemClickListener(this);
      mAdapters[i] = new LoadingSectionAdapter(mProxy);
      mAdapters[i].addLoading();
      list.setAdapter(mAdapters[i]);
    }

    mTabHost.setOnTabChangedListener(this);
    mPager.setOnPageChangeListener(this);
    mPager.setDisplayChild(0);

    final Resources res = mProxy.getResources();
    // Load items.
    new AsyncTask<Void, SectionItem, Void>() {

      @Override
      protected Void doInBackground(Void... params) {
        // Toggles
        String trackerLables[] = res.getStringArray(R.array.tracker_names);
        String trackerHeaders[] = res.getStringArray(R.array.tracker_category);
        SharedPreferences pref = Globals.getAppPrefs(mProxy);
        int iconColor = res.getColor(R.color.list_item_color);
        ArrayList<SectionItem> trackers = new ArrayList<SectionItem>();

        for (int i=0; i<mWidgetSections.length; i++) {
          trackers.add(new SectionItem(trackerHeaders[i], null));
          for (final int tId : mWidgetSections[i]) {
            Drawable drawable = res.getDrawable(TrackerManager.getTracker(tId, pref).buttonConfig[1])
                    .getConstantState().newDrawable();
            drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
            TrackerInfo item = new TrackerInfo(trackerLables[tId], drawable);
            item.mTrackerId = tId;
            trackers.add(item);
          }
        }
        publishProgress(trackers.toArray(new SectionItem[0]));
        mFullToggleList.addAll(trackers);

        // Applications
        ArrayList<SectionItem> apps = new ArrayList<SectionItem>();
        apps.addAll(ActivityInfo.loadList(mProxy,
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)));
        publishProgress(apps.toArray(new SectionItem[0]));
        mFullToggleList.add(new SectionItem(getString(R.string.tp_apps), null));
        mFullToggleList.addAll(apps);

        // Shortcuts.
        ArrayList<SectionItem> custom = new ArrayList<SectionItem>();

        ArrayList<ActivityInfo> plugins = ActivityInfo.loadReceivers(mProxy, new Intent(Globals.PLUGIN_INTENT));
        if (isPackageInstalled(TASKER_PACKAGE) || isPackageInstalled(TASKER_PACKAGE_MARKET)) {
          plugins.add(new ActivityInfo(getString(R.string.tp_tasker),
                  res.getDrawable(R.drawable.icon_tasker), null, null, null, null, true));
        }

        if (plugins.size() > 0) {
          custom.add(new SectionItem(getString(R.string.tp_plugin), null));
          custom.addAll(plugins);
          custom.add(new SectionItem(getString(R.string.tp_shrts), null));

          mFullToggleList.add(new SectionItem(getString(R.string.tp_plugin), null));
          mFullToggleList.addAll(plugins);
        }

        ArrayList<ActivityInfo> shortcuts = ActivityInfo.loadList(mProxy, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        // Promote ToggleFolders to the top.
        ActivityInfo toPromote = null;
        String packageName = mProxy.getPackageName();
        for (ActivityInfo info : shortcuts) {
          if (info.packageName.equals(packageName)) {
            toPromote = info;
            break;
          }
        }
        if (toPromote != null) {
          shortcuts.remove(toPromote);
          shortcuts.add(0, toPromote);
        }

        custom.addAll(shortcuts);
        publishProgress(custom.toArray(new SectionItem[0]));
        publishProgress(shortcuts.toArray(new SectionItem[0]));
        mFullToggleList.add(new SectionItem(getString(R.string.tp_shrts), null));
        mFullToggleList.addAll(shortcuts);
        return null;
      }

      @Override
      protected void onProgressUpdate(SectionItem... values) {
        mAdapters[mLoadCount].clear();
        mAdapters[mLoadCount].addAll(values);
        mLoadCount++;

        if (mLastSearchIntent != null) {
          searchIntent(mLastSearchIntent);
        }
      }
    }.execute();
  }

  private void setupTabs() {
    if (!mShowOnlyApps) {
      mTabHost.addTab(mTabHost.newTabSpec("t1").setIndicator(getString(R.string.tp_toggles)).setContent(this));
    }
    mTabHost.addTab(mTabHost.newTabSpec("t2").setIndicator(getString(R.string.tp_apps_short)).setContent(this));
    mTabHost.addTab(mTabHost.newTabSpec("t3").setIndicator(getString(
        mShowOnlyApps ? R.string.tp_shrts : R.string.tp_custom)).setContent(this));
  }

  public void setOnlyAppsMode(boolean showOnlyApps) {
    if (mShowOnlyApps != showOnlyApps) {
      mShowOnlyApps = showOnlyApps;

      mTabHost.getTabWidget().getChildAt(0).setVisibility(mShowOnlyApps ? View.GONE : View.VISIBLE);

      if (mShowOnlyApps) {
        ((ListView) mPager.getChildAt(2)).setAdapter(mAdapters[INDEX_ADAPTER_SHORTCUTS]);
        mTogglesPage = mPager.getChildAt(0);
        mPager.removeViewAt(0);
      } else {
        mPager.addView(mTogglesPage, 0);
        ((ListView) mPager.getChildAt(2)).setAdapter(mAdapters[INDEX_ADAPTER_CUSTOM]);
      }
      mTabHost.clearAllTabs();
      setupTabs();
      mPager.setDisplayChild(0);

      // Close search
      if (mSearchItem != null) {
        mSearchItem.collapseActionView();
      }
    }
  }

  // View pager related callbacks.
  @Override
  public void onPageChanged(int position) {
    mTabHost.setCurrentTab(position);
  }

  // Tab host related callbacks
  @Override
  public View createTabContent(String tag) {
    return new View(mProxy);
  }
  @Override
  public void onTabChanged(String tabId) {
    mPager.setDisplayChild(mTabHost.getCurrentTab());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mSearchItem = menu.add(android.R.string.search_go)
    .setIcon(android.R.drawable.ic_menu_search)
    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW + MenuItem.SHOW_AS_ACTION_ALWAYS)
    .setActionView(mSearchView)
    .setOnActionExpandListener(this);
    return true;
  }

  @Override
  public boolean onMenuItemActionCollapse(MenuItem item) {
    mRootLayout.setDisplayedChild(0);
    mLastSearchIntent = null;
    return true;
  }

  @Override
  public boolean onMenuItemActionExpand(MenuItem item) {
    return true;
  }

  public void searchIntent(Intent intent) {
    String query = intent.getStringExtra(SearchManager.QUERY).trim();
    if (query.isEmpty()) {
      return;
    }
    mSearchView.setQuery(query, false);
    InputMethodManager imm = (InputMethodManager) mProxy.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

    LoadingSectionAdapter adapter = mAdapters[INDEX_ADAPTER_SEARCH];
    adapter.clear();

    // Do search.
    query = query.toLowerCase(Locale.getDefault());
    SectionItem header = null;
    boolean headerAdded = false;
    for (SectionItem item : mFullToggleList) {
      if (item.icon == null) {
        header = item;
        headerAdded = false;
      } else if (item.label.toLowerCase(Locale.getDefault()).contains(query)
          && (!mShowOnlyApps || (item instanceof ActivityInfo && !((ActivityInfo) item).isReceiver))) {
        if ((header != null) && !headerAdded) {
          adapter.add(header);
        }
        adapter.add(item);
        headerAdded = true;
      }
    }

    if (mLoadCount < 3) {
      adapter.addLoading();
    }
    mRootLayout.setDisplayedChild(1);
    mLastSearchIntent = intent;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    SectionAdapter adapter = (SectionAdapter) parent.getAdapter();
    SectionItem item = adapter.getItem(position);

    if (item instanceof ActivityInfo) {
      ActivityInfo info = (ActivityInfo) item;
      if (info.targetIntent == null) {
        // Tasker task pick.
        startTaskerTaskPickFlow();
        return;
      }

      if (Intent.ACTION_CREATE_SHORTCUT.equals(info.targetIntent.getAction())) {
        // Create shortcut.
        mProxy.requestResult(REQ_CUSTOM_SHORTCUT, info.targetIntent, this);
      } else if (Globals.PLUGIN_INTENT.equals(info.targetIntent.getAction())) {
        // Check if the plugin supports config activity
        try {
          android.content.pm.ActivityInfo actInfo = mProxy.getPackageManager().getReceiverInfo(
                  info.targetIntent.getComponent(), PackageManager.GET_META_DATA);
          if ((actInfo != null) && (actInfo.metaData != null)) {
            String configActivity = actInfo.metaData.getString("com.painless.pc.CONFIG");
            mProxy.requestResult(REQ_PLUGIN_CONFIG,
                    new Intent().setComponent(new ComponentName(info.packageName, configActivity)), this);
            mWaitingPluginConfig = info;
            return;
          }
        } catch (Exception e) {
          Debug.log(e);
        }
        
        mListener.onTogglePicked(
                new PluginTracker(info.name, info.targetIntent, info.label),
                info.originalIcon);
        hide();
      } else {
        mListener.onTogglePicked(
                new SimpleShortcut(info.targetIntent, info.label),
                info.originalIcon);
        hide();
      }
    } else if (item instanceof TrackerInfo) {
      mListener.onTogglePicked(
              TrackerManager.getTracker(((TrackerInfo) item).mTrackerId, Globals.getAppPrefs(mProxy)), null);
      hide();
    } else {
      Debug.log("Some error occurred");
    }
  }

  @Override
  public void onResult(int requestCode, Intent data) {
    if (requestCode == REQ_CUSTOM_SHORTCUT) {

      Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
      String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
      Bitmap bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

      if (bitmap == null) {
        Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
        if (extra != null && extra instanceof ShortcutIconResource) {
          try {
            ShortcutIconResource iconResource = (ShortcutIconResource) extra;
            Resources resources = mProxy.getPackageManager().getResourcesForApplication(iconResource.packageName);
            final int id = resources.getIdentifier(iconResource.resourceName, null, null);
            bitmap = BitmapUtils.drawableToBitmap(resources.getDrawable(id), mProxy);
          } catch (Throwable e) {
            // Some error
          }
        }
        if (bitmap == null) {
          bitmap = BitmapFactory.decodeResource(mProxy.getResources(), R.drawable.icon_shortcut);
        }
      }

      mListener.onTogglePicked(new SimpleShortcut(intent, name), bitmap);
      hide();
    } else if (requestCode == REQ_PLUGIN_CONFIG) {
      
      String id = data.getStringExtra(Intent.EXTRA_UID);
      if (TextUtils.isEmpty(id)) {
        return;
      } else {
        mListener.onTogglePicked(
                new PluginTracker(
                        mWaitingPluginConfig.name + '-' + id,
                        new Intent(mWaitingPluginConfig.targetIntent).putExtra(Intent.EXTRA_UID, id),
                        mWaitingPluginConfig.label),
                        mWaitingPluginConfig.originalIcon);
        hide();
      }
    }
  }

  // Flow to pick tasker task.
  private void startTaskerTaskPickFlow() {
    // Check if external access is enabled or not.
    Cursor c = mProxy.getContentResolver().query( Uri.parse( "content://net.dinglisch.android.tasker/prefs" ), null, null, null, null);
    boolean enabled = false;
    if (c != null) {
      if (c.moveToNext()) {
        enabled = Boolean.parseBoolean(c.getString(c.getColumnIndex( "ext_access" )));
      }
      c.close();
    }
    if (!enabled) {
      new AlertDialog.Builder(mProxy)
        .setMessage(R.string.tp_tasker_disabled)
        .setTitle(R.string.tp_tasker_disabled_title)
        .setNeutralButton(R.string.act_ok, null)
        .show();
    } else {
      final ArrayList<String> tasks = Globals.getTaskerTasks(mProxy);
      if (tasks.isEmpty()) {
        Toast.makeText(mProxy, R.string.tp_tasker_empty, Toast.LENGTH_LONG).show();
      } else {
        showTarkerPicker(tasks);
        hide();
      }
    }
  }

  protected void showTarkerPicker(ArrayList<String> tasks) {
    new TaskerTogglePicker(mListener, mProxy, tasks);
  }

  @Thunk String getString(int resId) {
    return getContext().getString(resId);
  }
  @Thunk boolean isPackageInstalled(String packageName) {
    try {
      mProxy.getPackageManager().getPackageInfo(packageName, 0);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * A adapter which supports adding a loading symbol in the list.
   */
  private static class LoadingSectionAdapter extends SectionAdapter {

    private int mLoadingPosition = -1;

    public LoadingSectionAdapter(Context context) {
      super(context);
    }

    @Override
    public void clear() {
      mLoadingPosition = -1;
      super.clear();
    }

    public void addLoading() {
      mLoadingPosition = getCount();
      add(null);
    }

    @Override
    public boolean isEnabled(int position) {
      return (mLoadingPosition != position) && super.isEnabled(position);
    }

    @Override
    public int getItemViewType(int position) {
      return position == mLoadingPosition ? 2 : super.getItemViewType(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (position == mLoadingPosition) {
        return convertView == null ?
                mInflater.inflate(R.layout.list_item_loader, null) : convertView;
      }
      return super.getView(position, convertView, parent);
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }
  }

  private static class TrackerInfo extends SectionItem {

    @Thunk int mTrackerId;

    public TrackerInfo(String label, Drawable icon) {
      super(label, icon);
    }
  }

  public static interface TogglePickerListener {
    void onTogglePicked(AbstractTracker tracker, Bitmap icon);
  }
}


