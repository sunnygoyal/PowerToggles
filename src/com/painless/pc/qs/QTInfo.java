package com.painless.pc.qs;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.singleton.SettingStorage.ShortcutIdParser;
import com.painless.pc.singleton.WidgetDB;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.SimpleShortcut;

public class QTInfo implements ShortcutIdParser {

  public static final String KEY_REQUIRES_UPDATE = "required_update";
  public static final String KEY_CLICK_BROADCAST = "click_broadcast";
  public static final String KEY_CLICK_ACTIVITY = "click_activity";
  public static final String KEY_LONG_CLICK_ACTIVITY = "long_click_activity";
  public static final String KEY_WIDGET_ID = "widget_id";
  public static final String KEY_TRACKER_ID = "tracker_id";
  public static final String KEY_CUSTOM_LABEL = "custom_label";
  public static final String KEY_CONTENT_DESCRIPTION = "content_description";
  public static final String KEY_LONG_CONTENT_DESCRIPTION = "long_content_description";
  public static final String KEY_ICON_COUNT = "icon_count";

  public Intent clickIntent;
  public Intent clickBroadcast;
  public Intent longClickIntent;

  public boolean requiresUpdate;
  public int widgetId;

  public AbstractTracker tracker;
  public String customLabel;
  public String contentDescription;
  public String longContentDescription;
  public byte[][] icons;

  public String encodeToString() throws Exception {
    JSONObject settings = new JSONObject()
      .put(KEY_REQUIRES_UPDATE, requiresUpdate)
      .put(KEY_LONG_CLICK_ACTIVITY, longClickIntent.toUri(0))
      .put(KEY_WIDGET_ID, widgetId)
      .put(KEY_TRACKER_ID, tracker.getId())
      .put(KEY_ICON_COUNT, icons.length)
      .put(KEY_CONTENT_DESCRIPTION, contentDescription)
      .put(KEY_LONG_CONTENT_DESCRIPTION, longContentDescription);
    if (clickIntent != null) {
      settings.put(KEY_CLICK_ACTIVITY, clickIntent.toUri(0));
    } else {
      settings.put(KEY_CLICK_BROADCAST, clickBroadcast.toUri(0));
    }

    if (customLabel != null) {
      settings.put(KEY_CUSTOM_LABEL, customLabel);
    }

    return settings.toString();
  }

  @Override
  public SimpleShortcut parse(Context context, String def) throws Exception {
    return new SimpleShortcut(clickIntent == null ? longClickIntent : clickIntent, contentDescription);
  }

  public void update(Context context, String action) {
    Intent intent = new Intent(action);
    intent.putExtra("visible", true);

    int state = 0;
    int diplayNum = 0;
    if (tracker != null) {
      state = tracker.getStateColor(context);
      diplayNum = tracker.getImageNumber(context);
    }
    if (diplayNum > icons.length) {
      diplayNum = 0;
    }

    if (customLabel != null) {
      intent.putExtra("contentDescription", customLabel).putExtra("label", customLabel);
    } else {
      intent.putExtra("contentDescription", contentDescription);
      if (tracker != null) {
        intent.putExtra("label", tracker.getStateText(state,
            context.getResources().getStringArray(R.array.tracker_states),
            context.getResources().getStringArray(R.array.tracker_names_short)));
      } else {
        intent.putExtra("label", contentDescription);
      }
    }

    loadIcon(diplayNum, context);

    String pkg = context.getPackageName();
    if (icons[diplayNum] != null) {
      intent.putExtra("iconBitmap", icons[diplayNum]);
    } else {
      intent.putExtra("iconPackage", pkg).putExtra("iconId", R.drawable.icon_bg_white);
    }
    if (clickIntent != null) {
      intent.putExtra("onClick", PendingIntent.getActivity(context, (widgetId << 3), clickIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    } else {
      intent.putExtra("onClick", PendingIntent.getBroadcast(context, (widgetId << 3) + 2, clickBroadcast, PendingIntent.FLAG_UPDATE_CURRENT));
    }
    intent.putExtra("onLongClick", PendingIntent.getActivity(context, (widgetId << 3) + 1, longClickIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    intent.putExtra("package", pkg);
    context.sendBroadcast(intent);
  }

  public void loadIcon(int pos, Context context) {
    if (icons[pos] == null) {
      icons[pos] = WidgetDB.get(context).getIconBytes((widgetId << 3) + pos);
    }
  }

  public static QTInfo parse(String def, Context context) throws Exception {
    JSONObject obj = new JSONObject(def);

    QTInfo info = new QTInfo();
    info.icons = new byte[obj.getInt(KEY_ICON_COUNT)][];
    info.widgetId = obj.getInt(KEY_WIDGET_ID);
    info.requiresUpdate = obj.getBoolean(KEY_REQUIRES_UPDATE);
    info.customLabel = obj.optString(KEY_CUSTOM_LABEL, null);
    info.longClickIntent = Intent.parseUri(obj.getString(KEY_LONG_CLICK_ACTIVITY), 0);
    info.contentDescription = obj.getString(KEY_CONTENT_DESCRIPTION);
    info.longContentDescription = obj.getString(KEY_LONG_CONTENT_DESCRIPTION);

    String broadCast = obj.optString(KEY_CLICK_BROADCAST, null);
    if (broadCast == null) {
      info.clickIntent = Intent.parseUri(obj.getString(KEY_CLICK_ACTIVITY), 0);
    } else {
      info.clickBroadcast = Intent.parseUri(broadCast, 0);
    }

    info.tracker = SettingStorage.getTracker(obj.getString(KEY_TRACKER_ID), context, Globals.getAppPrefs(context), info);
    return info;
  }
}
