package com.painless.pc.theme;

import static com.painless.pc.singleton.Globals.BUTTONS;
import static com.painless.pc.singleton.Globals.BUTTON_DIVS;
import static com.painless.pc.util.SettingsDecoder.KEY_BACK_STYLE;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.painless.pc.CommandReceiver;
import com.painless.pc.GlobalFlags;
import com.painless.pc.ProxyActivity;
import com.painless.pc.R;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;

/**
 * An interface to create remote view factory based on the background style.
 */
public abstract class RVFactory {

  private static String[] sLabelArray = null;

  public final int layoutId;
  private final int[] mContainers;

  RVFactory(int layout, int[] containers) {
    layoutId = layout;
    mContainers = containers;
  }
 
  /**
   * This is called for for both fast and full updates.
   */
  public void updateStats(RemoteViews views, WidgetSetting settings, AbstractTracker tracker, int pos, int colorId) { }

  /**
   * This is only called for full updates.
   */
  protected void updateLayout(Context c, RemoteViews views, WidgetSetting settings) { }

  public final RemoteViews getRemoteView(Context context, WidgetSetting settings, boolean isNotification, boolean addOnClick, int widgetID) {
    if (sLabelArray == null) {
      sLabelArray = context.getResources().getStringArray(R.array.tracker_names);
    }
    final RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
    updateLayout(context, views, settings);
    int pos = 0;

    int dividerVisibility = settings.hideDividers ? View.GONE : View.VISIBLE;

    for (int i=0; i<settings.trackers.length; i++) {
      AbstractTracker tracker = settings.trackers[i];
      if (tracker == null) {
        views.setViewVisibility(BUTTON_DIVS[i], View.GONE);
        views.setViewVisibility(mContainers[i], View.GONE);
      } else {
        int buttonId = BUTTONS[i];
        views.setViewVisibility(BUTTON_DIVS[i], dividerVisibility);
        if (i < 7) {
          views.setInt(BUTTON_DIVS[i], "setBackgroundColor", settings.dividerColor);
        }
        views.setViewVisibility(mContainers[i], View.VISIBLE);
        views.setViewVisibility(buttonId, View.VISIBLE);
        views.setInt(buttonId, "setBackgroundResource", settings.clickFeedback[i]);

        int colorId = tracker.setImageViewResources(context, views, buttonId, settings, settings.imageProviders[pos++]);
        updateStats(views, settings, tracker, i, colorId);

        // Add on click
        if (addOnClick) {
          views.setOnClickPendingIntent(buttonId, getLaunchPendingIntent(context, tracker, widgetID, i, isNotification, settings.isLockScreenWidget));
          views.setContentDescription(buttonId, tracker.getLabel(sLabelArray));
        }
      }
    }

    if (!isNotification) {
      views.setInt(R.id.bgImage, "setAlpha", settings.backTrans);
      for (int i=settings.trackers.length-2; i >= 0; i--) {
        if (settings.trackers[i] != null) {
          views.setInt(BUTTON_DIVS[i], "setAlpha", settings.backTrans);
        }
      }
    }

    // +++++++++++++ Background Tint ++++++++++++++++
    if (settings.backTint != null) {
      views.setImageViewResource(R.id.bgImage, isNotification ? R.drawable.icon_bg_white : R.drawable.appwidget_bg_white);
      views.setInt(R.id.bgImage, "setColorFilter", settings.backTint);
    } else if (settings.backimage != null) {
      views.setImageViewUri(R.id.bgImage, settings.backimage);
      views.setInt(R.id.bgImage, "setColorFilter", 0);
    } else {
      views.setImageViewResource(R.id.bgImage, isNotification ? R.drawable.icon_bg_trans : R.drawable.appwidget_bg);
      views.setInt(R.id.bgImage, "setColorFilter", 0);
    }
    views.setViewPadding(R.id.layout_wrapper, settings.padding[0], settings.padding[1], settings.padding[2], settings.padding[3]);

    return views;
  }

  public final RemoteViews getFastRemoteView(Context context, WidgetSetting settings, boolean isNotification) {

    final RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
    int pos = 0;

    for (int i=0; i<settings.trackers.length; i++) {
      AbstractTracker tracker = settings.trackers[i];
      if (tracker != null) {
        int buttonId = BUTTONS[i];
        int colorId = tracker.setImageViewResources(context, views, buttonId, settings, settings.imageProviders[pos++]);
        updateStats(views, settings, tracker, i, colorId);
      }
    }
    return views;
  }

  /**
   * Creates PendingIntent to notify the widget of a tracker was clicked.
   */
  private static final PendingIntent getLaunchPendingIntent(
          Context context, AbstractTracker tracker, int widgetID, int buttonId, boolean isNotification, boolean shouldProxy) {

    Uri data = Uri.parse("tracker/?" + tracker.getId() + "#" + widgetID);
    if (shouldProxy && tracker.shouldProxy(context)) {
      Intent proxyIntent = new Intent(context, ProxyActivity.class);
      proxyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      proxyIntent.setData(data);
      return PendingIntent.getActivity(context, 0, proxyIntent, Intent.FILL_IN_DATA);

    } else {
      if (!isNotification && (tracker.trackerId == -1) && !GlobalFlags.hapticFeedback(context)) {
        // Simple Shortcut;
        SimpleShortcut shrt = (SimpleShortcut) tracker;
        return PendingIntent.getActivity(context, SimpleShortcut.getId(widgetID, buttonId),
                shrt.getIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);
      }
      return PendingIntent.getBroadcast(context, 0, /* no requestCode */
          makeIntent(context, data), 0 /* no flags */);
    }
  }

  public static Intent makeIntent(Context context, Uri uri) {
    return new Intent(context, CommandReceiver.class)
        .addCategory(Intent.CATEGORY_ALTERNATIVE)
        .setData(uri);
  }

  public static RVFactory get(Context context, SettingsDecoder decoder, boolean isNotification) {
    switch (decoder.getValue(KEY_BACK_STYLE, 0)) {
      case 3:
        return new LabelRVFactory(decoder, context);
      case 2:
        return new IndicatorRVFactory(decoder);
      default:
        return new SimpleRVFactory(decoder, isNotification);
    }
  }
}
