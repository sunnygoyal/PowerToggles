package com.painless.pc.singleton;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.painless.pc.tracker.SimpleShortcut;

public class WidgetDB extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "settings.db";
	private static final int DATABASE_VERSION = 1;

	private static final String SHORTCUT_TABLE = "shortcuts";

	private static final String[] SHORTCUR_COLUMNS = new String[] {
		"_id", "name", "intent"
	};

	private SQLiteDatabase db;

	private WidgetDB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	// TODO: Remove resource
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE shortcuts (_id INTEGER PRIMARY_KEY, name TEXT NOT NULL," +
				" intent TEXT NOT NULL, resource TEXT, image BLOB, UNIQUE (_id) ON CONFLICT REPLACE);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public void deleteToggles(int widgetID) {
		int start = widgetID << 3;
		int end = start + 7;
		getDB().delete(SHORTCUT_TABLE, "_id BETWEEN ? AND ?", params(start, end));
	}

	public void changeWidgetId(int oldId, int newId) {
    int start = oldId << 3;
    int end = start + 7;
    int diff = (newId << 3) - start;
    getDB().execSQL("UPDATE shortcuts SET _id = _id + ? WHERE _id BETWEEN ? AND ?", params(diff, start, end));
	}

	public void saveShrt(SimpleShortcut shrt, Bitmap image) {
		saveTracker(getIntId(shrt.getId()), shrt.getLabel(null), shrt.getIntent().toUri(Intent.URI_INTENT_SCHEME), image);
	}

	public void saveIcon(int id, Bitmap image) {
		saveTracker(id, "", "", image);
	}

	private void saveTracker(int id, String name, String intent, Bitmap icon) {
		ContentValues values = new ContentValues();
		values.put("_id", id);
		values.put("name", name == null ? "" : name);
		values.put("intent", intent == null ? "" : intent);
		values.put("image", BitmapUtils.compressImage(icon).toByteArray());

		getDB().insertWithOnConflict(SHORTCUT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public SimpleShortcut getShrt(String id) throws Exception {
		int parsedId = getIntId(id);
		Cursor cursor = getDB().query(SHORTCUT_TABLE, SHORTCUR_COLUMNS, "_id=" + getIntId(id), null, null, null, null);
		cursor.moveToNext();

		Intent intent = Intent.parseUri(cursor.getString(cursor.getColumnIndex("intent")), 0);
		SimpleShortcut shrt = new SimpleShortcut(intent, cursor.getString(cursor.getColumnIndex("name")));
		shrt.setId(parsedId);

		return shrt;
	}

	public Bitmap[] getAllIcons(int widgetId) {
		Bitmap[] iconArray = new Bitmap[8];
		int min = widgetId << 3;
		int max = min + 7;
		Cursor cursor = getDB().query(SHORTCUT_TABLE,
				new String[] {"_id", "image" },
				"_id BETWEEN ? AND ?",
				new String[] {min + "", max + ""},
				null, null, null);
		int idIndex = cursor.getColumnIndex("_id");
		int bitmapIndex = cursor.getColumnIndex("image");

		for (int i = cursor.getCount(); i>0; i--) {
			cursor.moveToNext();
			int id = cursor.getInt(idIndex) - min;
			if (id>=0 && id<8) {
				byte[] bitmapData = cursor.getBlob(bitmapIndex);
				iconArray[id] = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
				if ((iconArray[id] != null) && ((iconArray[id].getWidth() == 0) || (iconArray[id].getHeight() == 0))) {
					iconArray[id] = null;
				}
			}
		}
		return iconArray;
	}

	public byte[] getIconBytes(int id) {
    Cursor cursor = getDB().query(SHORTCUT_TABLE,
        new String[] {"image" },
        "_id = ?",
        new String[] {id + ""}, null, null, null);
    try {
      if (cursor.moveToNext()) {
        return cursor.getBlob(0);
      } else {
        return null;
      }
    } finally {
      cursor.close();
    }
  }

	private static int getIntId(String id) {
		return Integer.parseInt(id.substring(3));
	}

	private SQLiteDatabase getDB() {
		if (db == null) {
			db = getWritableDatabase();
		}
		return db;
	}

	@Override
	public synchronized void close() {
		try {
			super.close();
		} catch (Throwable e) {}
		if (db != null) {
			try {
				db.close();
			} catch (Throwable e) {}
			db = null;
		}
	}





	private static WidgetDB DB_;

	public static WidgetDB get(Context c) {
		if (DB_ == null) {
			DB_ = new WidgetDB(c);
		}
		return DB_;
	}

	public static void closeAll() {
		if (DB_ != null) {
			DB_.close();
			DB_ = null;
		}
	}

	
	private static String[] params(Object... objects) {
	  String[] result = new String[objects.length];
	  for (int i = 0; i < objects.length; i++) {
	    result[i] = objects[i].toString();
	  }
	  return result;
	}
}
