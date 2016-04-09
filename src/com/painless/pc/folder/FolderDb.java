package com.painless.pc.folder;

import java.util.ArrayList;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.singleton.SettingStorage.ShortcutIdParser;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.SimpleShortcut;

public class FolderDb implements ShortcutIdParser {

  public static final int SETTING_POS = -100;

  public static final String KEY_INTENT = "intent";
  public static final String KEY_NAME = "name";
  public static final int POS_DEFINITION = 0;
  public static final int POS_ICON = 1;
  public static final int POS_POSITION = 2;

  private static final String TABLE_NAME = "toggles";

  private final Context mContext;
  private final SQLiteDatabase mDb;

  public FolderDb(Context context, String name) {
    mContext = context;

    mDb = mContext.openOrCreateDatabase(name, 0, null);
    if (mDb.getVersion() == 0) {
      mDb.setVersion(1);
      mDb.execSQL("CREATE TABLE toggles (def TEXT NOT NULL, icon BLOB, pos INTEGER);");
    }
  }

  public void close() {
    mDb.close();
  }

  public Cursor getAllEntries() {
    return mDb.query(TABLE_NAME, null, null, null, null, null, "pos");
  }

  public ArrayList<FolderItem> getAll(Cursor cursor) {
    ArrayList<FolderItem> result = new ArrayList<FolderItem>();
    SharedPreferences pref = Globals.getAppPrefs(mContext);

    if (cursor.isBeforeFirst()) {
      cursor.moveToNext();
    }
    do {
      int position = cursor.getInt(POS_POSITION);
      if (position < 0) {
        continue;
      }

      String def = cursor.getString(POS_DEFINITION);
      AbstractTracker tracker = SettingStorage.getTracker(def, mContext, pref, this);

      byte[] bitmapData = cursor.getBlob(POS_ICON);
      Bitmap icon = bitmapData == null ? null : BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
      ToggleBitmapProvider provider = tracker.getImageProvider(mContext,
          ((icon != null) && (icon.getWidth() > 0) && (icon.getHeight() > 0)) ? icon : null);

      result.add(new FolderItem(tracker, provider, position));
    } while (cursor.moveToNext());

    cursor.close();
    return result;
  }

  public void addTracker(AbstractTracker tracker, Bitmap icon, int position) {
    ContentValues values = new ContentValues();
    values.put("def", tracker.getId());
    values.put("pos", position);
    if (icon != null) {
      values.put("icon", BitmapUtils.compressImage(icon).toByteArray());
    }
    mDb.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  @Override
  public SimpleShortcut parse(Context context, String def) throws Exception {
    JSONObject obj = new JSONObject(def.substring(3));
    return new SimpleShortcut(
        Intent.parseUri(obj.getString(KEY_INTENT), Intent.URI_INTENT_SCHEME),
        obj.getString(KEY_NAME));
  }

  public void delete(int position) {
    mDb.delete(TABLE_NAME, "pos = " + position, null);
  }

  public void move(int startPos, int finalPos) {
    int a, b, delta;
    if (startPos > finalPos) {
      a = finalPos;
      b = startPos;
      delta = 1;
    } else {
      a = startPos;
      b = finalPos;
      delta = -1;
    }

    delta -= a;
    int modulo = b - a + 1;
    while (delta < 0) delta += modulo;
    String sql = "UPDATE toggles SET pos = ((pos + " + delta + ") % " + modulo + " ) + " + a +
        " WHERE pos >= " + a + " AND pos <= " + b + ";";
    mDb.execSQL(sql);
  }

  public Bitmap getIcon(int pos) {
    Cursor cursor = mDb.query(TABLE_NAME, new String[] {"icon"}, "pos = " + pos, null, null, null, null);
    Bitmap icon = null;
    if (cursor.moveToNext()) {
      byte[] bitmapData = cursor.getBlob(cursor.getColumnIndex("icon"));
      icon = (bitmapData == null) ? null : BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
      icon = ((icon != null) && (icon.getWidth() > 0) && (icon.getHeight() > 0)) ? icon : null;
    }
    cursor.close();
    return icon;
  }

  public void setIcon(Bitmap icon, int pos) {
    ContentValues values = new ContentValues();
    if (icon != null) {
      values.put("icon", BitmapUtils.compressImage(icon).toByteArray());
    } else {
      values.putNull("icon");
    }
    mDb.update(TABLE_NAME, values, "pos = " + pos, null);
  }

  public void setShortcut(SimpleShortcut shrt, int pos) {
    ContentValues values = new ContentValues();
    values.put("def", shrt.getId());
    mDb.update(TABLE_NAME, values, "pos = " + pos, null);
  }

  public String getSettings() {
    String setting = FolderUtils.DEFAULT_SETTINGS;

    Cursor cursor = mDb.query(TABLE_NAME, new String[] {"def"}, "pos = " + SETTING_POS, null, null, null, null);
    if (cursor.moveToNext()) {
      setting = cursor.getString(0);
    }
    cursor.close();
    return setting;
  }

  public void saveSettigns(String settings, byte[] background) {
    ContentValues values = new ContentValues();
    values.put("def", settings);
    values.put("icon", background);
    
    if (mDb.update(TABLE_NAME, values, "pos = " + SETTING_POS, null) == 0) {
      values.put("pos", SETTING_POS);
      Debug.log(mDb.insert(TABLE_NAME, null, values));
    }
  }
}

