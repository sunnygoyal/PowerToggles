package com.painless.pc.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.widget.RemoteViews;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.theme.ToggleBitmapProvider;
import com.painless.pc.util.WidgetSetting;

public class DataNetworkTracker extends AbstractTracker {

	public DataNetworkTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, 
				new int[] {
				COLOR_DEFAULT, R.drawable.icon_toggle_gprs_2g,
				COLOR_WORKING, R.drawable.icon_toggle_gprs_3g,
				COLOR_ON, R.drawable.icon_toggle_gprs_4g,
		});
	}

	@Override
	public int getActualState(Context context) {
		return GprsStateTracker.getStaticState(context);
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
	}

	@Override
	public void toggleState(Context context) {
		Globals.startIntent(context, new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
	}

	@Override
	public int setImageViewResources(Context context, RemoteViews views,
	        int buttonId, WidgetSetting setting,
	        ToggleBitmapProvider imageProvider) {
    final int colorId = getStateColor(context);

    int imageNumber = getImageNumber(context);
    Bitmap img = imageProvider==null ? null : imageProvider.getIcon(imageNumber);
    if (img == null) {
      views.setImageViewResource(buttonId, buttonConfig[2* imageNumber + 1]);
    } else {
      views.setImageViewBitmap(buttonId, img);
    }
    boolean useColor = img==null || buttonConfig.length > 2;
    views.setInt(buttonId, "setAlpha", useColor ? setting.buttonAlphas[colorId] : 255);
    views.setInt(buttonId, "setColorFilter", useColor ? setting.buttonColors[colorId] : 0);
    return colorId;
	}

	@Override
	public int getImageNumber(Context context) {
    int networkType = Globals.getNetworkType(context);
    if (networkType > 0) networkType--;
    return networkType;
	}

	@Override
	public boolean shouldProxy(Context context) {
		return true;
	}

	@Override
	public String getStateText(int state, String[] states, String[] labelArray) {
		return Globals.sNetworkName;
	}

	
	/**
	 * 2: 4g, 1: 3g, 0: 2g
	 */
	/**
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private int getPreferredNetworkType(Context context) {
	  final int preferred;
	  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
	    preferred = Settings.System.getInt(context.getContentResolver(), "preferred_network_mode", 0);
	  } else {
	    preferred = Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode", 0);
	  }
	  
	  switch (preferred) {
	    case 0:  // WCDMA preferred
	    case 2:  // WCDMA only
	    case 3:
	    case 4:  // CDMA and EvDo
	    case 6:  // EvDo only
	    case 7:  // GSM/WCDMA, CDMA, and EvDo
	      return 1;  // 3G

	    case 8:
	    case 9:
	    case 10:
	    case 11:
	    case 12:
	      return 2;  // 4G

      default:
        return 0; // 2G;
	  }
	}*/
}
