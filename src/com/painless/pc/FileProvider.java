package com.painless.pc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.painless.pc.singleton.Debug;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * {@link ContentProvider} for background images and other internal files.
 */
public class FileProvider extends ContentProvider {

  private static final String TEMP_BACK_IMAGE_NAME = "cback";
  private static final String BACK_IMAGE_PREFIX = "back_";

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Debug.log(uri);
    return null;
  }

  @Override
  public String getType(Uri uri) {
    return "image/png";
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    String path = uri.getPath();
    File result;
    if (path.startsWith("/back")) {
      try {
        result = widgetBackFile(getContext(), Integer.parseInt(uri.getQuery()));
      } catch (Exception e) {
        throw new FileNotFoundException();
      }
    } else if (path.startsWith("/config")) {
      result = tempBackImage(getContext());
    } else if (path.startsWith("/crop")) {
      result = new File(getContext().getCacheDir(), "crop.png");
    } else {
      throw new FileNotFoundException();
    }
    return ParcelFileDescriptor.open(result, modeToMode(mode));
  }

  /**
   * Copied from ContentResolver.java
   */
  private static int modeToMode(String mode) {
      int modeBits;
      if ("r".equals(mode)) {
          modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
      } else if ("w".equals(mode) || "wt".equals(mode)) {
          modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                  | ParcelFileDescriptor.MODE_CREATE
                  | ParcelFileDescriptor.MODE_TRUNCATE;
      } else if ("wa".equals(mode)) {
          modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                  | ParcelFileDescriptor.MODE_CREATE
                  | ParcelFileDescriptor.MODE_APPEND;
      } else if ("rw".equals(mode)) {
          modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                  | ParcelFileDescriptor.MODE_CREATE;
      } else if ("rwt".equals(mode)) {
          modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                  | ParcelFileDescriptor.MODE_CREATE
                  | ParcelFileDescriptor.MODE_TRUNCATE;
      } else {
          throw new IllegalArgumentException("Invalid mode: " + mode);
      }
      return modeBits;
  }

  public static String backFileName(int widgetId) {
    return BACK_IMAGE_PREFIX + widgetId;
  }

  public static File tempBackImage(Context context) {
    return new File(context.getCacheDir(), TEMP_BACK_IMAGE_NAME);
  }

  public static File widgetBackFile(Context context, int widgetId) {
    return new File(context.getFilesDir(), backFileName(widgetId));
  }
}
