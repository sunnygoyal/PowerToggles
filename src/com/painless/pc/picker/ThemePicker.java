package com.painless.pc.picker;

import static com.painless.pc.util.SettingsDecoder.KEY_COLORS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.painless.pc.R;
import com.painless.pc.picker.theme.ThemeAdapter;
import com.painless.pc.picker.theme.ThemeEntry;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.WidgetSetting;

public class ThemePicker extends ListActivity implements OnItemClickListener {

  private static final String BASE_URL = "https://b4ed1c0ec81643f5b9ce1c461688fb066bf383db-www.googledrive.com/host/0B94XoKFv6ejBZ3VkTzhYX05SOGc/";
  private static final String IMAGE_URL = BASE_URL + "bg/";

  private static final String THEME_EXTENSION = ".pttheme";

  @Thunk ThemeAdapter mAdapter;
  @Thunk boolean mRemoteHeaderAdded = false;

  @Thunk ThemeEntry mServerLoadEntry;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setResult(RESULT_CANCELED);
    
    mAdapter = new ThemeAdapter(this);

    final String themeUrl;

    if (getIntent().getBooleanExtra("folders", false)) {
      ThemeEntry header = new ThemeEntry();
      header.title = R.string.tm_system;
      mAdapter.add(header);

      try {
        ThemeEntry entry = new ThemeEntry();
        entry.config = new JSONObject(SettingsDecoder.CONFIG_DARK);
        entry.backgroundRes = R.drawable.folder_back;
        entry.hideDividers = true;
        WidgetSetting.parseColors(new SettingsDecoder(entry.config), KEY_COLORS, entry.buttonColors, entry.buttonAlphas);
        mAdapter.add(entry);
      } catch (Exception e) {
        Debug.log(e);
      }

      themeUrl = BASE_URL + "folders.txt";
    } else {
      themeUrl = BASE_URL + "themes.txt";
    }

    File themeDir = new File(Environment.getExternalStorageDirectory(), "Power Toggles");
    if (themeDir.exists() && themeDir.isDirectory()) {
      for (File f : themeDir.listFiles()) {
        if (f.isFile() && f.getName().endsWith(THEME_EXTENSION)) {
          if (mAdapter.isEmpty()) {
            ThemeEntry header = new ThemeEntry();
            header.title = R.string.tm_local;
            mAdapter.add(header);
          }

          ThemeEntry theme = new ThemeEntry();
          theme.themeFile = f;
          mAdapter.add(theme);
        }
      }
    }

    setListAdapter(mAdapter);

    // Install http cache
    try {
      File httpCacheDir = new File(getCacheDir(), "http");
      long httpCacheSize = 4 * 1024 * 1024; // 4 MiB
      HttpResponseCache.install(httpCacheDir, httpCacheSize);
    } catch (IOException e) {
      Debug.log(e);
    }
    
    mServerLoadEntry = new ThemeEntry();
    mAdapter.add(mServerLoadEntry);

    // Load remote file
    new AsyncTask<Void, ThemeEntry, Void>() {

      @Override
      protected Void doInBackground(Void... arg0) {
        BufferedReader reader = null;
        try {
          URL url = new URL(themeUrl);
          reader = new BufferedReader(new InputStreamReader(url.openStream()));
          String line;
          while ((line = reader.readLine()) != null) {
            try {
              ThemeEntry entry = new ThemeEntry();
              entry.config = new JSONObject(line);
              entry.remoteUrl = new URL(IMAGE_URL + entry.config.getString("id") + ".png");
              publishProgress(entry);
            } catch (JSONException e) {
              // Ignore this entry
            }
          }
        } catch (Exception e) {
          
        } finally {
          if (reader != null) {
            try {
              reader.close();
            } catch (Exception e) { }
          }
        }
        return null;
      }

      @Override
      protected void onProgressUpdate(ThemeEntry... objs) {
        mAdapter.remove(mServerLoadEntry);
        if (!mRemoteHeaderAdded && !mAdapter.isEmpty()) {
          ThemeEntry header = new ThemeEntry();
          header.title = R.string.tm_remote;
          mAdapter.add(header);
        }
        mRemoteHeaderAdded = true;
        mAdapter.add(objs[0]);
      }
    }.execute();

    getListView().setOnItemClickListener(this);
  }

  @Override
  protected void onPause() {
    HttpResponseCache cache = HttpResponseCache.getInstalled();
    if (cache != null) {
        cache.flush();
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (mAdapter != null) {
      mAdapter.mLoader.destroy();
    }
    super.onDestroy();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    ThemeEntry entry = mAdapter.getItem(position);
    if (entry.isLoaded()) {
      setResult(RESULT_OK, new Intent()
          .putExtra("config", entry.config.toString())
          .putExtra("icon", entry.background));
      finish();
    }
  }
}
