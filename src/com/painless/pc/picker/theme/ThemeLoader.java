package com.painless.pc.picker.theme;

import static com.painless.pc.util.SettingsDecoder.DEFAULT_DIVIDER_COLOR;
import static com.painless.pc.util.SettingsDecoder.KEY_COLORS;
import static com.painless.pc.util.SettingsDecoder.KEY_DENSITY;
import static com.painless.pc.util.SettingsDecoder.KEY_DIVIDER_COLOR;
import static com.painless.pc.util.SettingsDecoder.KEY_HIDE_DIVIDERS;
import static com.painless.pc.util.SettingsDecoder.KEY_PADDING;
import static com.painless.pc.util.SettingsDecoder.KEY_STRETCH;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Pair;

import com.painless.pc.singleton.BackupUtil;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.WidgetSetting;

/**
 * A class to handle theme loading
 */
public class ThemeLoader implements Callback {

  private static final int CONFIG_LOADED = 1;

  private final Set<ThemeEntry> mPendingTasks;
  private final BitmapCache mCache;
  private final ThemeAdapter mNotifier;
  
  private final ExecutorService mLocalService;
  private final ExecutorService mRemoteService;
  @Thunk final Handler mResponseHandler;

  private final int mDensity;

  public ThemeLoader(ThemeAdapter loadCallback) {
    mNotifier = loadCallback;
    mCache = new BitmapCache();
    mPendingTasks = new HashSet<ThemeEntry>();

    mRemoteService = Executors.newFixedThreadPool(3);
    mLocalService = Executors.newSingleThreadExecutor();
    mResponseHandler = new Handler(this);
    mDensity = loadCallback.getContext().getResources().getDisplayMetrics().densityDpi;
  }

  public synchronized void submic(ThemeEntry request) {
    if (mPendingTasks.contains(request) || request.failed) {
      // Request already pending.
      return;
    }

    if (request.themeFile != null) {
      mPendingTasks.add(request);
      mLocalService.submit(new LocalThemeLoader(request));
    } else if (request.remoteUrl != null) {
      mPendingTasks.add(request);
      mRemoteService.submit(new RemoteThemeLoader(request));
    }
  }

  public void destroy() {
    mRemoteService.shutdown();
    mLocalService.shutdownNow();
    mCache.evictAll();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean handleMessage(Message msg) {
    if (msg.what == CONFIG_LOADED) {
      Pair<ThemeEntry, Bitmap> result = (Pair<ThemeEntry, Bitmap>) msg.obj;
      ThemeEntry request = result.first;
      request.background = result.second;
      if (result.second != null) {
        mCache.put(result.second, request);
      }
      mPendingTasks.remove(request);
      mNotifier.notifyDataSetChanged();
      return true;
    }
    return false;
  }

  /**
   * Marks a bitmap as being used.
   */
  public void register(Bitmap img) {
    mCache.get(img);
  }

  /**
   * A task to load local themes.
   */
  private class LocalThemeLoader implements Runnable {

    private final ThemeEntry mRequest;

    public LocalThemeLoader(ThemeEntry request) {
      mRequest = request;
    }

    @Override
    public void run() {
      Bitmap image = null;
      ZipFile zip = null;
      try {
        zip = new ZipFile(mRequest.themeFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry("theme.txt"))));
        String config = reader.readLine();
        reader.close();
 
        mRequest.config = new JSONObject(config);
        ZipEntry backImage = zip.getEntry("back.png");
        if (backImage != null) {
          image = parseConfig(mRequest, BitmapFactory.decodeStream(zip.getInputStream(backImage)));
        }
      } catch (Exception e) {
        Debug.log(e);
        mRequest.failed = true;
      } finally {
        if (zip != null) {
          try {
            zip.close();
          } catch (Exception e) { }
        }
      }

      Message.obtain(mResponseHandler, CONFIG_LOADED, Pair.create(mRequest, image)).sendToTarget();
    }
  }

  /**
   * A task to load remote themes.
   */
  private class RemoteThemeLoader implements Runnable {

    private final ThemeEntry mRequest;

    public RemoteThemeLoader(ThemeEntry request) {
      mRequest = request;
    }

    @Override
    public void run() {
      Bitmap image = null;
      try {
        image = parseConfig(mRequest, BitmapFactory.decodeStream(mRequest.remoteUrl.openStream()));
      } catch (Exception e) {
        Debug.log(e);
        mRequest.failed = true;
      }
      Message.obtain(mResponseHandler, CONFIG_LOADED, Pair.create(mRequest, image)).sendToTarget();
    }
  }

  @Thunk Bitmap parseConfig(ThemeEntry entry, Bitmap loadedIcon) {
    SettingsDecoder decoder = new SettingsDecoder(entry.config);
    int density = decoder.getValue(KEY_DENSITY, mDensity);

    Bitmap resized = Bitmap.createScaledBitmap(loadedIcon,
            loadedIcon.getWidth() * mDensity / density,
            loadedIcon.getHeight() * mDensity / density,
            true);
    if (resized != loadedIcon) {
      loadedIcon.recycle();
    }
    
    entry.padding = notmalizeRect(decoder, KEY_PADDING, density);
    
    entry.stretch = new float[4];
    
    float[] sizes = new float[] {resized.getWidth(), resized.getHeight()};
    int[] stretch = notmalizeRect(decoder, KEY_STRETCH, density);
    for (int i = 0; i < 4; i++) {
      entry.stretch[i] = stretch[i] / sizes[i & 1];
    }

    entry.hideDividers = decoder.is(KEY_HIDE_DIVIDERS, true);
    entry.dividerColor = decoder.getValue(KEY_DIVIDER_COLOR, DEFAULT_DIVIDER_COLOR);
    WidgetSetting.parseColors(decoder, KEY_COLORS, entry.buttonColors, entry.buttonAlphas);

    return resized;
  }

  private int[] notmalizeRect(SettingsDecoder decoder, String key, int settingDensity) {
    return BackupUtil.normalizeRect(decoder, key, mDensity, settingDensity);
  }
}
