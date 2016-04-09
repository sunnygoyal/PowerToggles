package com.painless.pc.tracker;

import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.RemoteViews;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.theme.BatteryImageProvider;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.util.WidgetSetting;

public class BatteryTracker extends AbstractTracker {

  public static final int ID = 15;

	public static final String ENABLED_KEY = "battery_custom_colors";
	public static final String CUSTOM_COLOR_KEY = "battery_colors";
	public static final String CUSTOM_COLOR_VALS = "-65536,-256,-12930852";

	// customizations
	private int min_level;
	private int max_level;

	private int[] battery_colors;
	private int[] battery_alpha;
	private boolean customColors;
	
	public BatteryTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, new int[] {COLOR_DEFAULT, R.drawable.icon_toggle_battery});
	}

	@Override
	public void init(SharedPreferences pref) {
		final String[] levels = pref.getString("battery_levels", "20,60").split(",");
		min_level = Integer.parseInt(levels[0]);
		max_level = Integer.parseInt(levels[1]);

		customColors = pref.getBoolean(ENABLED_KEY, false);
		battery_colors = new int[3];
		battery_alpha = new int[3];
		if (customColors) {
			final int[] values = ParseUtil.parseIntArray(null, pref.getString(CUSTOM_COLOR_KEY, CUSTOM_COLOR_VALS));
			for (int i=0; i<3; i++) {
				battery_colors[i] = ParseUtil.removeAlphaFromColor(values[i]);
				battery_alpha[i] = Color.alpha(values[i]);
			}
		}
	}

	@Override
	public void toggleState(Context context) {
		Globals.startIntent(context, new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
	}

	private int mBattery;
	@Override
	public int setImageViewResources(Context context, RemoteViews views,
			int buttonId, WidgetSetting setting, ToggleBitmapProvider imageProvider) {

		mBattery = Globals.getBattery(context);
		final int state = mBattery < min_level ? COLOR_DEFAULT :
			(mBattery < max_level ? COLOR_WORKING: COLOR_ON);
		
		Bitmap img = imageProvider==null ? null : imageProvider.getIcon(mBattery);
		
		if (img == null) {
			views.setImageViewResource(buttonId, buttonConfig[1]);
		} else {
			views.setImageViewBitmap(buttonId, img);
		}
		views.setInt(buttonId, "setAlpha", customColors ? battery_alpha[state] : setting.buttonAlphas[state]);
		views.setInt(buttonId, "setColorFilter", customColors ? battery_colors[state] : setting.buttonColors[state]);
		return state;
	}

	@Override
	public int getActualState(Context context) {
		// never called
		return 0;
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		// never called
	}

	@Override
	public ToggleBitmapProvider getImageProvider(Context context, Bitmap icon) {
		return new BatteryImageProvider(
				(icon != null && icon.getWidth() > 0 && icon.getHeight() > 0) ? icon :
						BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_toggle_battery),
				context);
	}

	@Override
	public boolean shouldProxy(Context context) {
		return true;
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return String.format(Locale.getDefault(), "%2d%%", mBattery);
	}

	@Override
	public int getImageNumber(Context context) {
	  return 0;
	}
}
