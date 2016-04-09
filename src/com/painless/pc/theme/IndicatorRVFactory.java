package com.painless.pc.theme;

import static com.painless.pc.singleton.Globals.BUTTONS_CONTAINERS;
import static com.painless.pc.singleton.Globals.BUTTONS_INDS;
import android.content.Context;
import android.widget.RemoteViews;

import com.painless.pc.R;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;

public class IndicatorRVFactory extends RVFactory {

  public static final String[] KEY_MY_COLORS = new String[] {"indC1", "indC2", "indC3"};
  public static final String KEY_CUSTOM_COLOR = "indCustomC";

  private final int[] colors = new int[3];
  private final int[] alphas = new int[3];
  private final boolean useCustom;

	public IndicatorRVFactory(SettingsDecoder decoder) {
	  super(R.layout.aw_indicator, BUTTONS_CONTAINERS);
	  WidgetSetting.parseColors(decoder, KEY_MY_COLORS, colors, alphas);
    useCustom = decoder.hasValue(KEY_CUSTOM_COLOR);
	}

	@Override
	public void updateStats(RemoteViews views, WidgetSetting settings, AbstractTracker tracker, int pos, int colorId) {
		if (useCustom) {
			views.setInt(BUTTONS_INDS[pos], "setAlpha", alphas[colorId]);
			views.setInt(BUTTONS_INDS[pos], "setColorFilter", colors[colorId]);
		} else {
			views.setInt(BUTTONS_INDS[pos], "setAlpha", settings.buttonAlphas[colorId]);
			views.setInt(BUTTONS_INDS[pos], "setColorFilter", settings.buttonColors[colorId]);
		}
	}

  @Override
  protected void updateLayout(Context c, RemoteViews views, WidgetSetting settings) {
    views.setImageViewResource(BUTTONS_INDS[0], settings.isFlatCorners ? R.drawable.appwidget_ind_c : R.drawable.appwidget_ind_l);
    views.setImageViewResource(BUTTONS_INDS[7], settings.isFlatCorners ? R.drawable.appwidget_ind_c : R.drawable.appwidget_ind_r);
  }
}
