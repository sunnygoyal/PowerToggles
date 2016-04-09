package com.painless.pc.picker.theme;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;

public class BitmapCache extends LruCache<Bitmap, ThemeEntry> {

  public BitmapCache() {
    super(getDefaultLruCacheSize());
  }

  @Override
  @TargetApi(Build.VERSION_CODES.KITKAT)
  protected int sizeOf(Bitmap key, ThemeEntry value) {
    return ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ?
            key.getAllocationByteCount() : key.getByteCount()) / 1024;
  }

  @Override
  protected void entryRemoved(boolean evicted, Bitmap key, ThemeEntry oldValue,
          ThemeEntry newValue) {
    if (oldValue.background != null) {
      oldValue.background.recycle();
      oldValue.background = null;
    }
  }

  /**
   * @return Size in KB to be used for caches.
   */
  private static int getDefaultLruCacheSize() {
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    final int cacheSize = maxMemory / 8;
    return cacheSize;
  }
}

