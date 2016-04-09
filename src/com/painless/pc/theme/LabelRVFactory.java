package com.painless.pc.theme;

import static com.painless.pc.singleton.Globals.BUTTONS_CONTAINERS;
import static com.painless.pc.singleton.Globals.BUTTONS_INDS;
import android.content.Context;
import android.widget.RemoteViews;

import com.painless.pc.R;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;

public class LabelRVFactory extends RVFactory {

  public static final String KEY_COLOR = "lblColor";
  public static final String KEY_SIZE = "lblSize";

	private final String[] shortNames;
	private final String[] states;

	private final int color, size;

	public LabelRVFactory(SettingsDecoder decoder, Context context) {
	  super(R.layout.aw_label, BUTTONS_CONTAINERS);
		shortNames = context.getResources().getStringArray(R.array.tracker_names_short);
		states = context.getResources().getStringArray(R.array.tracker_states);
		color = decoder.getValue(KEY_COLOR, 0xFFFFFFFF);
    size = decoder.getValue(KEY_SIZE, 12);
	}
	
	@Override
	public void updateStats(RemoteViews views, WidgetSetting settings, AbstractTracker tracker, int pos, int colorId) {
		views.setTextColor(BUTTONS_INDS[pos], color);
		views.setFloat(BUTTONS_INDS[pos], "setTextSize", size);
		views.setTextViewText(BUTTONS_INDS[pos], tracker.getStateText(colorId, states, shortNames));
	}
}
