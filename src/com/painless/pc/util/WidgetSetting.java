
package com.painless.pc.util;

import static com.painless.pc.singleton.Globals.STATUS_BAR_WIDGET_ID;
import static com.painless.pc.util.SettingsDecoder.CLICK_TYPE_DEFAULT;
import static com.painless.pc.util.SettingsDecoder.CLICK_TYPE_KITKAT;
import static com.painless.pc.util.SettingsDecoder.CLICK_TYPE_RIPPLE;
import static com.painless.pc.util.SettingsDecoder.CLICK_TYPE_TRANSPARENT;
import static com.painless.pc.util.SettingsDecoder.DEFAULT_COLORS;
import static com.painless.pc.util.SettingsDecoder.DEFAULT_DIVIDER_COLOR;
import static com.painless.pc.util.SettingsDecoder.KEY_CLICK_FEEDBACK;
import static com.painless.pc.util.SettingsDecoder.KEY_COLORS;
import static com.painless.pc.util.SettingsDecoder.KEY_DIVIDER_COLOR;
import static com.painless.pc.util.SettingsDecoder.KEY_FLAT_CORNER;
import static com.painless.pc.util.SettingsDecoder.KEY_HIDE_DIVIDERS;
import static com.painless.pc.util.SettingsDecoder.KEY_PADDING;
import static com.painless.pc.util.SettingsDecoder.KEY_TINT;
import static com.painless.pc.util.SettingsDecoder.KEY_TRANSPARANCY;

import java.io.File;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import com.painless.pc.FileProvider;
import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.theme.RVFactory;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.BatteryTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.TimeoutTracker;

public final class WidgetSetting {

	public final int[] buttonColors = new int[3];
	public final int[] buttonAlphas = new int[3];
	public final int[] padding;

	public final AbstractTracker[] trackers;
	public final ToggleBitmapProvider[] imageProviders;
	public final int[] clickFeedback = new int[8];

	public final boolean batteryEnabled;
	public final boolean isLockScreenWidget;

	public int backTrans;
	public Integer backTint;	// this can be null
	public Uri backimage;	// this can be null

	public boolean hideDividers;
	public boolean isFlatCorners;
	public int dividerColor;

	public RVFactory rvFactory;

	public WidgetSetting(Context context, AbstractTracker[] trackers, SettingsDecoder decoder, int widgetId, Bitmap[] allicons) {
		this.trackers = trackers;
		this.imageProviders = new ToggleBitmapProvider[trackers.length];
	
		// init providers
		int pos = 0;
		for (AbstractTracker tracker : trackers) {
			if (tracker != null) {
			  
			  // Optimize bitmaps.
	      if ((allicons[pos] != null) // Icon not null
	              && (tracker.trackerId != BatteryTracker.ID) //  Not a battery
	              && !((tracker.trackerId == TimeoutTracker.ID) 
                        && (allicons[pos].getWidth() == (allicons[pos].getHeight() + 1))) // Timeout in battery mode
	              && (tracker.buttonConfig.length > 2) // Has colors
	              && !((tracker.trackerId == PluginTracker.TRACKER_ID) 
	                      && (allicons[pos].getWidth() == (allicons[pos].getHeight() + 1))) // Plugin in battery mode
	              ) {

	        // Optimize icon.
	        Bitmap old = allicons[pos];
	        if (old.getConfig() != Bitmap.Config.ALPHA_8) {
	          // Create a new bitmap with only alpha channel.
	          allicons[pos] = Bitmap.createBitmap(old.getWidth(), old.getHeight(), Bitmap.Config.ALPHA_8);
	          new Canvas(allicons[pos]).drawBitmap(old, 0, 0, null);
	          old.recycle();
	        }
	      }

	      imageProviders[pos] = tracker.getImageProvider(context, allicons[pos]);
				pos++;
			}
		}

		// button colors;
		parseColors(decoder, KEY_COLORS, buttonColors, buttonAlphas);
		updateClickFeedback(decoder);

		this.backTrans = decoder.getValue(KEY_TRANSPARANCY, 255);
		this.backTint = decoder.hasValue(KEY_TINT) ? decoder.getValue(KEY_TINT, 0) : null;
		boolean batteryEnabled = false;
		if (trackers != null) {
			for (AbstractTracker tracker : trackers) {
				batteryEnabled |= (tracker != null && tracker instanceof BatteryTracker);
			}
		}
		this.hideDividers = decoder.is(KEY_HIDE_DIVIDERS, true);
		this.rvFactory = RVFactory.get(context, decoder, (widgetId == STATUS_BAR_WIDGET_ID) || (widgetId == STATUS_BAR_WIDGET_ID));

		this.batteryEnabled = batteryEnabled;
		this.isLockScreenWidget = isLockWidget(context, widgetId);
		this.dividerColor = decoder.getValue(KEY_DIVIDER_COLOR, DEFAULT_DIVIDER_COLOR);

		File backImgFile = FileProvider.widgetBackFile(context, widgetId);
		if (backImgFile.exists()) {
		  backimage = Uri.parse("content://com.painless.pc.file/back/?" + widgetId + "#" + backImgFile.lastModified());
		  padding = decoder.getRect(KEY_PADDING);
		} else {
		  padding = new int[4];
		}
	}

	public static final Integer[] parseUiSettings(String str) {
		final Integer[] settings = new Integer[4];
		settings[0] = 255;

		if (str != null) {
			final String[] parts = str.split(",");
			final Integer alpha = mayBeParse(parts, 0);
			if (alpha != null) {
				settings[0] = alpha;
			}

			settings[1] = mayBeParse(parts, 3);
			settings[2] = mayBeParse(parts, 2);
			settings[3] = mayBeParse(parts, 1);
		}
		return settings;
	}

	private static final Integer mayBeParse(String[] parts, int pos) {
		try {
			return Integer.parseInt(parts[pos]);
		} catch (final Exception e) {
			return null;
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private static boolean isLockWidget(Context context, int widgetId) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return false;
		}
		
		// Get the value of OPTION_APPWIDGET_HOST_CATEGORY
		int category = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
				.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);

		// If the value is WIDGET_CATEGORY_KEYGUARD, it's a lockscreen widget
		return category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
	}

	public void updateClickFeedback(SettingsDecoder decoder) {
	  isFlatCorners = decoder.hasValue(KEY_FLAT_CORNER);
	  int clickFeedbackId = decoder.getValue(KEY_CLICK_FEEDBACK, CLICK_TYPE_DEFAULT);
	  if (clickFeedbackId == CLICK_TYPE_RIPPLE && !Globals.IS_LOLLIPOP) {
	    clickFeedbackId = CLICK_TYPE_DEFAULT;
	  }
	  switch(clickFeedbackId) {
	    case CLICK_TYPE_TRANSPARENT:
	      Arrays.fill(clickFeedback, 0);
	      break;
	    case CLICK_TYPE_KITKAT:
	      Arrays.fill(clickFeedback, 1, 7, R.drawable.wbg_kitkat_center);
	      clickFeedback[0] = isFlatCorners ? R.drawable.wbg_kitkat_center : R.drawable.wbg_kitkat_left;
	      clickFeedback[7] = isFlatCorners ? R.drawable.wbg_kitkat_center : R.drawable.wbg_kitkat_right;
	      break;
	    case CLICK_TYPE_RIPPLE:
	      Arrays.fill(clickFeedback, 1, 7, R.drawable.wbg_ripple_center);
        clickFeedback[0] = isFlatCorners ? R.drawable.wbg_ripple_center : R.drawable.wbg_ripple_left;
        clickFeedback[7] = isFlatCorners ? R.drawable.wbg_ripple_center : R.drawable.wbg_ripple_right;
        break;
      default:
        Arrays.fill(clickFeedback, 1, 7, R.drawable.wbg_holo_center);
        clickFeedback[0] = isFlatCorners ? R.drawable.wbg_holo_center : R.drawable.wbg_holo_left;
        clickFeedback[7] = isFlatCorners ? R.drawable.wbg_holo_center : R.drawable.wbg_holo_right;
        break;
	  }
	}

	public static void parseColors(SettingsDecoder decoder, String[] names, int[] colors, int[] alphas) {
		for (int i=0; i<3; i++) {
			int color = decoder.getValue(names[i], DEFAULT_COLORS[i]);
			colors[i] = ParseUtil.removeAlphaFromColor(color);
			alphas[i] = Color.alpha(color);
		}
	}
}
