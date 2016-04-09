package com.painless.pc.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.util.SectionAdapter.SectionItem;

public class SectionAdapter extends ArrayAdapter<SectionItem> {

	protected final LayoutInflater mInflater;
	private final Resources res;
	private final int iconColor;

	protected int textResource;

	public SectionAdapter(Context context) {
		super(context, R.layout.list_item_header);
		mInflater = LayoutInflater.from(context);
		res = context.getResources();
		iconColor = res.getColor(R.color.list_item_color);

		textResource = R.layout.list_item_normal;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SectionItem item = getItem(position);
		TextView view = (TextView) ((convertView == null) ?
				mInflater.inflate(getLayoutId(item.icon == null, position), null) :
					convertView);
		view.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);
		view.setText(item.label);
		return view;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItem(position).icon != null;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return isEnabled(position) ? 1 : 0;
	}

	private int getLayoutId(boolean isHeader, int position) {
		return isHeader ? R.layout.list_item_header : textResource;
	}

	public void addHeader(String header) {
		add(new SectionItem(header, null));
	}

	public Drawable addItem(String title, int icon) {
		final Drawable drawable = res.getDrawable(icon);
		drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);

		addItem(title, drawable);
		return drawable;
	}

	public void addItem(String title, Drawable icon) {
		add(new SectionItem(title, icon));
	}

	public static class SectionItem {
		public final String label;
		public final Drawable icon;

		public SectionItem(String label, Drawable icon) {
			this.label = label;
			this.icon = icon;
		}
	}
}
