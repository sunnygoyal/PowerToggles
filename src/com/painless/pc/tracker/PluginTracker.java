package com.painless.pc.tracker;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.PluginDB;
import com.painless.pc.theme.BatteryImageProvider;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.util.WidgetSetting;

/**
 * Toggle for handling plugin requests
 */
public class PluginTracker extends AbstractTracker {

  public static final int TRACKER_ID = 1000;

	private final String label;
	private final Intent launchIntent;
	private final String varId;

	private Boolean state = null;
	private int count = 0;

	public PluginTracker(String varId, Intent launchIntent, String label) {
		super(TRACKER_ID, null,
		    getTriImageConfig(varId.startsWith(Globals.TASKER_KEY_PREFIX) ? R.drawable.icon_tasker : R.drawable.icon_toggle_plugin));
		this.label = label;
		this.varId = varId;
		this.launchIntent = launchIntent;
	}

	public Intent getIntent() {
		return launchIntent;
	}

	@Override
	public int getActualState(Context context) {
		if (state == null) {
			state = PluginDB.get(context).getState(varId);
			count = PluginDB.get(context).getCount(varId);
		}
		return state ? STATE_ENABLED : STATE_DISABLED;
	}

	public void setChangedState(Context context, boolean newState, int newCount) {
		state = newState;
		count = newCount;
		setCurrentState(context, getActualState(context));
	}

	@Override
	protected void requestStateChange(final Context context, boolean desiredState) {
		if (varId.equals(Globals.TASKER_KEY_PREFIX + label)) {
			// Tasker toggle
			Cursor c = context.getContentResolver().query( Uri.parse( "content://net.dinglisch.android.tasker/prefs" ), null, null, null, null );
			boolean quit = true;
			if (c != null) {
				if (c.moveToNext()) {
					if (!Boolean.parseBoolean(c.getString(c.getColumnIndex( "enabled" )))) {
						Toast.makeText(context, R.string.ps_tasker_disabled, Toast.LENGTH_LONG).show();
					} else if (!Boolean.parseBoolean(c.getString(c.getColumnIndex( "ext_access" )))) {
						Toast.makeText(context, R.string.ps_tasker_no_access, Toast.LENGTH_LONG).show();
					} else {
						quit = false;
					}
				}
				c.close();
			}
			if (quit) {
				setCurrentState(context, STATE_DISABLED);
			} else {
				// run task.
				ArrayList<String> varNames = new ArrayList<String>(2);
				ArrayList<String> varValues = new ArrayList<String>(2);

				varNames.add("%toggle");
				varValues.add(label);

        varNames.add("%count");
        varValues.add(Integer.toString(count));

				if (desiredState) {
					varNames.add("%state");
					varValues.add("true");
				}

				context.sendBroadcast(new Intent(launchIntent)
					.putStringArrayListExtra("varNames", varNames)
					.putStringArrayListExtra("varValues", varValues));
			}

		} else {
			context.sendBroadcast(new Intent(launchIntent).putExtra("state", desiredState));
		}
	}

	@Override
	public String getLabel(String[] labelArray) {
		return label;
	}

	@Override
	public String getId() {
		return "pl_" + varId;
	}

	@Override
	public ToggleBitmapProvider getImageProvider(Context context, Bitmap icon) {
	  if ((icon != null) && (icon.getWidth() == icon.getHeight() + 1)) {
	    return new BatteryImageProvider(icon, context);
	  } else {
	    return super.getImageProvider(context, icon);  
	  }
	}

	@Override
  public int setImageViewResources(Context context, RemoteViews views,
      int buttonId, WidgetSetting setting, ToggleBitmapProvider imageProvider) {
    final int colorId = getStateColor(context);

    Bitmap img = imageProvider==null ? null : imageProvider.getIcon(count);
    if (img == null) {
      views.setImageViewResource(buttonId, R.drawable.icon_toggle_plugin);      
    } else {
      views.setImageViewBitmap(buttonId, img);
    }
    views.setInt(buttonId, "setAlpha", setting.buttonAlphas[colorId]);
    views.setInt(buttonId, "setColorFilter", setting.buttonColors[colorId]);
    return colorId;
  }
}
