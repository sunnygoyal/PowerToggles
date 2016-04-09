package com.painless.pc.folder;

import java.util.Collections;
import java.util.HashSet;

import android.content.Context;

import com.painless.pc.util.SettingsDecoder;

public class FolderUtils {

  public static final String PREFS = "folder_prefs";
  public static final String KEY_NAME_PREFIX = "name_";
  public static final String ACTION = "com.painless.pc.FOLDER";

  private static final String DB_NAME_PREFIX = "folder_";
  public static final String DB_NAME_REGX = "^folder_\\d+\\.db$";

  public static final String KEY_HIDE_LABEL = "hide_label";
  public static final String DEFAULT_SETTINGS = SettingsDecoder.CONFIG_DARK;

  public static String newDbName(Context context) {
    return newDbName(context, new HashSet<String>());
  }

  public static String newDbName(Context context, HashSet<String> ignoreSet) {
    Collections.addAll(ignoreSet, context.databaseList());
    int i = 1;
    while (true) {
      String name = getDbName(i);
      if (!ignoreSet.contains(name)) {
        return name;
      }
      i++;
    }
  }

  public static String getDbName(int id) {
    return DB_NAME_PREFIX + id + ".db";
  }

  public static void setName(String name, String folderId, Context context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putString(KEY_NAME_PREFIX + folderId, name)
      .commit();      
  }
}
