package com.painless.pc.singleton;

import static com.painless.pc.util.SettingsDecoder.KEY_TRACKER_ARRAY;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.SparseArray;

import com.painless.pc.ConnectivityReceiver;
import com.painless.pc.FileProvider;
import com.painless.pc.PCWidgetActivity;
import com.painless.pc.TrackerManager;
import com.painless.pc.qs.QTStorage;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;

public class SettingStorage {

	public static final Map<String, AbstractTracker> actionToTracker = new HashMap<String, AbstractTracker>();

	private static final String WIDGET_PREF_PREFIX = "widget";
	private static final AbstractTracker[] trackerList =
		new AbstractTracker[TrackerManager.TRACKER_LIST.length];
	private static final HashMap<String, PluginTracker> sActivePlugins = new HashMap<String, PluginTracker>();

	/**
	 * Returns the tracker for the given position if it is already initialized.
	 */
	public static final AbstractTracker maybeGetTracker(int trackerId) {
		return trackerList[trackerId];
	}

	public static AbstractTracker[] getCache() {
		return trackerList;
	}

	private static final SparseArray<WidgetSetting> widgetSettingsCache = new SparseArray<>();

	public static void clearCache() {
		widgetSettingsCache.clear();
		QTStorage.clearCache();
		for (int i=0; i<trackerList.length; i++) {
			trackerList[i] = null;
		}
		actionToTracker.clear();
		sActivePlugins.clear();
	}
	/**
	 * Returns the tracker list definition for the given widget id. If the definition is not in the cache
	 * creates and adds the definition to the cache.
	 */
	public static final WidgetSetting getSettingForWidget(Context context, int widgetId) {
		WidgetSetting settings = widgetSettingsCache.get(widgetId);
		if (settings == null) {
			final String definition = getSettingString(context, widgetId);
			settings = buildWidgetSettings(definition, context, widgetId);
			widgetSettingsCache.put(widgetId, settings);
		}

		return settings;
	}

	/**
	 * @return the definition on a widget.
	 */
	public static final String getSettingString(Context context, int widgetId) {
		return Globals.getAppPrefs(context).getString(WIDGET_PREF_PREFIX + widgetId, getDefaultSettings(widgetId));
	}

	public static final String getDefaultSettings(int widgetId) {
	  return (Globals.IS_LOLLIPOP && Globals.isNotificationWidget(widgetId)) ?
	      SettingsDecoder.CONFIG_DARK : SettingsDecoder.CONFIG_LIGHT;
	}

	public static final void changeWidgetId(Context context, int oldId, int newId) {
    widgetSettingsCache.remove(oldId);
    widgetSettingsCache.remove(newId);
	  SharedPreferences prefs = Globals.getAppPrefs(context);
	  String oldKey = WIDGET_PREF_PREFIX + oldId;
	  if (prefs.contains(oldKey)) {
	    // widget present.

	    // Migrate definition
	    String def = prefs.getString(oldKey, "");
	    SettingsDecoder decoder = new SettingsDecoder(def);
	    String trackers = decoder.getTrackerDef();
	    // Update shortcut ids.
	    String[] ids = trackers.split(",");
	    for (int i = 0; i < ids.length; i++) {
	      if (ids[i].startsWith("ss_")) {
	        try {
	          int nId = Integer.parseInt(ids[i].substring(3));
	          ids[i] = "ss_" + SimpleShortcut.getId(newId, nId & 7);
	        } catch (Exception e) { }
	      }
	    }
	    try {
	      decoder.settings.put(KEY_TRACKER_ARRAY, TextUtils.join(",", ids));
	    } catch (Exception e) {
	      Debug.log(e);
	    }
	    prefs.edit().putString(WIDGET_PREF_PREFIX + newId, decoder.settings.toString()).remove(oldKey).commit();

	    // Migrate background.
	    File backImgFile = FileProvider.widgetBackFile(context, oldId);
	    if (backImgFile.exists()) {
	      backImgFile.renameTo(FileProvider.widgetBackFile(context, newId));
	    }

	    // Migrate widget db
	    WidgetDB.get(context).changeWidgetId(oldId, newId);
	  }
	}

	/**
	 * Build an array of state trackers from the given definition list.
	 * The array will be of length 8 always.
	 * Definition is of the form 0,1,2,3,4,5,6,7 where each integer specifies the tracker in the
	 * tracker list. A null position means no tracker.
	 * It will always try to fill the first and last entry with a non-null value.
	 */
	public static final WidgetSetting buildWidgetSettings(String def, Context context, int widgetId, Bitmap[] allicons) {
		SettingsDecoder decoder = new SettingsDecoder(def);

		final AbstractTracker[] trackers = new AbstractTracker[8];
		final String[] ids = decoder.getTrackerDef().split(",");

		final SharedPreferences pref = Globals.getAppPrefs(context);

		if (ids.length == 1) {
			trackers[1] = getTracker(ids[0], context, pref);
		} else if (ids.length > 1){
			int pos = 0;
			for (int i = 0; i < ids.length - 1 && pos < 8; i++, pos++) {
				trackers[pos] = getTracker(ids[i], context, pref);
			}

			trackers[7] = getTracker(ids[ids.length - 1], context, pref);			
		}

		return new WidgetSetting(context, trackers, decoder, widgetId, allicons);
	}

	private static final WidgetSetting buildWidgetSettings(String def, Context context, int widgetId) {
	  return buildWidgetSettings(def, context, widgetId, WidgetDB.get(context).getAllIcons(widgetId));
	}

	public static AbstractTracker getTracker(String def, Context context, SharedPreferences pref) {
		return getTracker(def, context, pref, WIDGET_DB_PARSER);
	}

	public static AbstractTracker getTracker(String def, Context context, SharedPreferences pref, ShortcutIdParser shortcutParser) {
		int id;
		try {
			if (def.startsWith("ss_")) {
				return shortcutParser.parse(context, def);
			} else if (def.startsWith("pl_")) {
				String key = def.substring(3);
				PluginTracker tracker = sActivePlugins.get(key);
				if (tracker == null) {
					tracker = PluginDB.get(context).getPlugin(def.substring(3));
					sActivePlugins.put(key, tracker);
				}
				return tracker;
			}
			id = Integer.parseInt(def);
		} catch (Throwable e) {
			id = 3;
		}
				
		AbstractTracker tracker = trackerList[id];
		if (tracker == null) {
			tracker = TrackerManager.getTracker(id, pref);
			trackerList[id] = tracker;
			if (tracker.getChangeAction() != null) {
				actionToTracker.put(tracker.getChangeAction(), tracker);
			}
		}
		return tracker;
	}

	/**
	 * Adds a widget definition to the cache and preferences
	 */
	public static void addWidget(Context context, int widgetId, String definition) {
		Globals.getAppPrefs(context).edit().putString(WIDGET_PREF_PREFIX + widgetId, definition).commit();

		widgetSettingsCache.put(widgetId,
				buildWidgetSettings(definition, context, widgetId));
	}

	public static void deleteWidget(int widgetId) {
		widgetSettingsCache.remove(widgetId);
	}

	/**
	 * Clears any information stored of plugins not being used.
	 */
	public static void cleanupCachedPlugins(Context context) {
		getSettingForWidget(context, Globals.STATUS_BAR_WIDGET_ID);
    getSettingForWidget(context, Globals.STATUS_BAR_WIDGET_ID_2);
		PluginDB.get(context).removeOldInfo(sActivePlugins.keySet());
	}

	public static void cleanupUpUnusedWidgets(Context context) {
	  ArrayList<Integer> active = new ArrayList<Integer>();
	  active.add(Globals.STATUS_BAR_WIDGET_ID);
	  active.add(Globals.STATUS_BAR_WIDGET_ID_2);
    final AppWidgetManager awm = AppWidgetManager.getInstance(context);
    for (int widgetId : awm.getAppWidgetIds(new ComponentName(context, PCWidgetActivity.class))) {
      active.add(widgetId);
    }

    ArrayList<Integer> toRemove = new ArrayList<Integer>();
    SharedPreferences prefs = Globals.getAppPrefs(context);
    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(WIDGET_PREF_PREFIX)) {
        try {
          Integer wId = Integer.parseInt(key.substring(WIDGET_PREF_PREFIX.length()));
          if (!active.contains(wId)) {
            toRemove.add(wId);
          }
        } catch (Exception e) {
          Debug.log(e);
        }
      }
    }

    if (!toRemove.isEmpty()) {
      WidgetDB db = WidgetDB.get(context);
      SharedPreferences.Editor editor = prefs.edit();
      for (Integer id : toRemove) {

        editor.remove(WIDGET_PREF_PREFIX + id);
        context.deleteFile(FileProvider.backFileName(id));
        db.deleteToggles(id);

        widgetSettingsCache.remove(id);
      }
      editor.commit();
    }
	}

	public static void updateConnectivityReceiver(Context context) {
	  // Only enable if DataNetworkTracker is active
	  final int state = (trackerList[11] != null) ?
	      PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
	        PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

    PackageManager pm = context.getPackageManager();
    ComponentName componentName = new ComponentName(context, ConnectivityReceiver.class);

    if (pm.getComponentEnabledSetting(componentName) != state) {
      pm.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
    }
	}

	private static final ShortcutIdParser WIDGET_DB_PARSER = new ShortcutIdParser() {

		@Override
		public SimpleShortcut parse(Context context, String def) throws Exception {
			return WidgetDB.get(context).getShrt(def);
		}
	};

	/**
	 * A simple interface for simple shortcut parser.
	 */
	public static interface ShortcutIdParser {
		SimpleShortcut parse(Context context, String def) throws Exception;
	}
}
