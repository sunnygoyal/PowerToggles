package com.painless.pc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.SectionAdapter.SectionItem;

public class ActivityInfo extends SectionItem implements Comparable<ActivityInfo> {

	public final Bitmap originalIcon;
	public final Intent targetIntent;

	public final String packageName;
	public final String name;
	public final boolean isReceiver;

	public ActivityInfo(String label, Drawable icon, Intent intent, Bitmap orgIcon, String packageName, String name, boolean isReceiver) {
		super(label, icon);
		targetIntent = intent;
		originalIcon = orgIcon;
		this.packageName = packageName;
		this.name = name;
		this.isReceiver = isReceiver;
	}

	@Override
	public int compareTo(ActivityInfo another) {
		return label.compareTo(another.label);
	}

	public static ArrayList<ActivityInfo> loadList(Context context, Intent targetIntent) {
		return parseResolvers(context, targetIntent, context.getPackageManager().queryIntentActivities(targetIntent, 0), false);
	}

	public static ArrayList<ActivityInfo> loadReceivers(Context context, Intent targetIntent) {
		return parseResolvers(context, targetIntent, context.getPackageManager().queryBroadcastReceivers(targetIntent, 0), true);
	}

	private static ArrayList<ActivityInfo> parseResolvers(Context context, Intent targetIntent, List<ResolveInfo> resolvers, boolean isReceiver) {
		ArrayList<ActivityInfo> result = new ArrayList<ActivityInfo>();
		PackageManager pm = context.getPackageManager();
		Resources res = context.getResources();	
		for (ResolveInfo info : resolvers) {
	
			try {
				Bitmap icon = BitmapUtils.drawableToBitmap(info.loadIcon(pm), context);
				Intent myIntent = new Intent(targetIntent);
				myIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
				ActivityInfo item = new ActivityInfo(
						info.loadLabel(pm).toString(),
						new BitmapDrawable(res, icon),
						myIntent,
						icon,
						info.activityInfo.packageName,
						info.activityInfo.name,
						isReceiver);
				result.add(item);
			} catch (Exception e) {
				Debug.log(e);
			}
		}
		Collections.sort(result);
		return result;
	}
}