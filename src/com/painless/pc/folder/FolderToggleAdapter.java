package com.painless.pc.folder;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;

public class FolderToggleAdapter extends ArrayAdapter<FolderItem> {

	private FolderItem mAddItem;

	private final Context mContext;
	private final WidgetSetting mRenderSettings;
	private final String[] mShortNames;
	private final String[] mStates;
	private final LayoutInflater mInflator;
	private final OnDragListener mDragListener;
	private final boolean mShowLabels;
	private final int mLabelColor;

	public FolderToggleAdapter(Context context, SettingsDecoder settingDecoder, int labelColor, OnDragListener dragListener) {
		super(context, R.layout.folder_item, android.R.id.text1);

		mContext = context;
		mShowLabels = !settingDecoder.hasValue(FolderUtils.KEY_HIDE_LABEL);
		mLabelColor = labelColor;
		mInflator = LayoutInflater.from(context);
		mDragListener = dragListener;
		mRenderSettings = new WidgetSetting(context, new AbstractTracker[]{}, settingDecoder, -420, new Bitmap[]{});
		mShortNames = context.getResources().getStringArray(R.array.tracker_names_short);
		mStates = context.getResources().getStringArray(R.array.tracker_states);
	}

	public final int getDefaultColor() {
	  return ParseUtil.addAlphaToColor(mRenderSettings.buttonAlphas[0], mRenderSettings.buttonColors[0]);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FolderViews views;
		if (convertView == null) {
			views = new FolderViews(mContext, mInflator, parent);
			convertView = views.root;
			convertView.setOnDragListener(mDragListener);
			if (mShowLabels) {
				views.text.setTextColor(mLabelColor);
			} else {
			  views.text.setVisibility(View.GONE);
			}
		} else {
			views = (FolderViews) convertView.getTag();
		}
		views.position = position;
		FolderItem item = getItem(position);
		int colorId = item.first.setImageViewResources(mContext, views, R.id.img_preview, mRenderSettings, item.second);
		if (mShowLabels) {
			views.text.setText(item.first.getStateText(colorId, mStates, mShortNames));
		}
		return convertView;
	}

	public FolderItem getAddItem() {
		if (mAddItem == null) {
			mAddItem = FolderItem.create(mContext, mContext.getString(R.string.act_add), R.drawable.icon_add, mLabelColor);
		}
		return mAddItem;
	}
}
