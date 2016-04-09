package com.painless.pc.folder;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

import com.painless.pc.theme.FixedImageProvider;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.WidgetSetting;

public class FolderItem {

  public final AbstractTracker first;
  public final ToggleBitmapProvider second;

  public int sortOrder;

	public FolderItem(AbstractTracker first, ToggleBitmapProvider second, int sortOrder) {
	  this.first = first;
	  this.second = second;
	  this.sortOrder = sortOrder;
	}

	public static FolderItem create(Context context, String text, int icon, final int color) {
	  return new FolderItem(
        new SimpleShortcut(new Intent(), text) {
          @Override
          public int setImageViewResources(Context context, RemoteViews views,
                  int buttonId, WidgetSetting setting,
                  ToggleBitmapProvider imageProvider) {
            super.setImageViewResources(context, views, buttonId, setting, imageProvider);
            views.setInt(buttonId, "setColorFilter", color);
            return 0;
          }
        },
        new FixedImageProvider(BitmapFactory.decodeResource(context.getResources(), icon)),
        -1);
	}
}
