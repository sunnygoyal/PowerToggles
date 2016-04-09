package com.painless.pc.qs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.ArrayMap;

import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class QTStorage {

  private static final Map<String, QTInfo> sTileCache =
      Globals.IS_LOLLIPOP ? new ArrayMap<String, QTInfo>() : new HashMap<String, QTInfo>();

  private static boolean sTilesLoaded = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;

  private static final String PREFS_NAME = "qt_prefs";
  private static final String TILE_PREF_PREFIX = "qt_info_";

  public static void clearCache() {
    sTileCache.clear();
    sTilesLoaded = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
  }

  public static void saveTileInfo(String key, String definition, Context context) {
    try {
      sTileCache.put(key, QTInfo.parse(definition, context));
      prefs(context).edit()
        .putString(TILE_PREF_PREFIX + key, definition)
        .apply();
    } catch (Exception e) {
      Debug.log(e);
    }
  }

  public static int generateNewId(Context context) {
    loadAllTiles(context);
    Set<Integer> usedIds = new HashSet<>();
    for (QTInfo tile : sTileCache.values()) {
      usedIds.add(tile.widgetId);
    }
    Integer id = Globals.QS_MAX_WIDGET_ID;
    while (usedIds.contains(id)) id++;
    return id;
  }

  public static void loadAllTiles(Context context) {
    if (!sTilesLoaded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
        if (entry.getKey().startsWith(TILE_PREF_PREFIX)) {
          String key = entry.getKey().substring(TILE_PREF_PREFIX.length());
          if (!sTileCache.containsKey(key)) {
            try {
              sTileCache.put(key, QTInfo.parse((String) entry.getValue(), context));
            } catch (Exception e) {
              Debug.log(e);
            }
          }
        }
      }
      sTilesLoaded = true;
    }
  }

  public static void updateAllWidgets(Context context, boolean updateFull) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return;
    }
    loadAllTiles(context);
    for (Map.Entry<String, QTInfo> entry : sTileCache.entrySet()) {
      QTInfo info = entry.getValue();
      if (info.requiresUpdate || updateFull) {
        info.update(context, entry.getKey());
      }
    }
  }

  public static void deleteTile(String key, Context context) {
    sTileCache.remove(key);
    prefs(context).edit().remove(TILE_PREF_PREFIX + key).apply();
  }

  public static Map<String, QTInfo> getAllTiles(Context context) {
    loadAllTiles(context);
    return sTileCache;
  }

  public static QTInfo getTile(String key, Context context) {
    loadAllTiles(context);
    return sTileCache.get(key);
  }

  public static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
}
