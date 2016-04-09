package com.painless.pc.cfg;

import java.io.File;
import java.io.OutputStream;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.picker.IconPicker;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.util.ImportExportActivity;
import com.painless.pc.util.Thunk;

public class IconThemeEditor extends ImportExportActivity<Bitmap> {

	public IconThemeEditor() {
		super(R.menu.ite_menu, ".icons.png",
				R.string.ite_export, R.array.ite_export_msg,
				R.string.ite_import, R.array.ite_import_msg);
	}

	public static final String ICON_STRIP = "strip";
  public static final String TRACKER_NAME = "name";
  public static final String ICON_CONFIG = "iconconfig";
  public static final String ALLOW_NULL = "allownull";

	@Thunk MyAdapter adapter;

	@Thunk IconPicker iconPicker;
	private Resources res;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    addActionDoneButton();

		setResult(RESULT_CANCELED);

		setContentView(R.layout.cfg_icon_theme_editor);
		iconPicker = new IconPicker(this);
		res = getResources();

//		int trackerId = getIntent().getIntExtra(ITRACKER_ID, 7);	// Default to backlight tracker
//		AbstractTracker tracker =
//				TrackerManager.getTracker(trackerId, Globals.getAppPrefs(this));

//		String trackerName = getResources().getStringArray(R.array.tracker_names)[tracker.trackerId];
		((TextView) findViewById(R.id.txt_toggle_name)).setText(getIntent().getStringExtra(TRACKER_NAME));

		adapter = new MyAdapter(this);

		Bitmap strip = getIntent().getParcelableExtra(ICON_STRIP);

		int x = 0;
		int height = getStripHeight(strip);

		int[] config = getIntent().getIntArrayExtra(ICON_CONFIG);
		for (int i=1; i<config.length; i+=2) {

			ListItem item = new ListItem();
			item.iconDefault = config[i];
			item.defaultIcon = BitmapFactory.decodeResource(res, item.iconDefault);

			if (!setItemFromImageStrip(x, strip, item)) {
				setListItemBitmap(item, item.defaultIcon);
				item.isIconEmpty = true;
				item.defaultSelected = true;
			}

			x += height;
			adapter.add(item);
		}

		((ListView) findViewById(android.R.id.list)).setAdapter(adapter);
	}

  @Override
  public void onDoneClicked() {
		Intent result = new Intent();
		result.putExtra(ICON_STRIP, getImageStrip(getIntent().getBooleanExtra(ALLOW_NULL, true)));
		setResult(RESULT_OK, result);

		finish();
	}

	private Bitmap getImageStrip(boolean checkIfAllDefault) {
		int size = BitmapUtils.getIconSize(this);
		int total = adapter.getCount();

		Bitmap strip = Bitmap.createBitmap(total*size, size, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(strip);

		Rect destRect = new Rect(0, 0, size, size);
		boolean isAllDefault = true;

		for (int i = 0; i < total; i++) {
			ListItem item = adapter.getItem(i);

			Bitmap iconToDraw = item.defaultSelected ? item.defaultIcon : item.mainIcon;
			c.drawBitmap(
					iconToDraw,
					new Rect(0, 0, iconToDraw.getWidth(), iconToDraw.getHeight()),
					destRect,
					null);
			destRect.offset(size, 0);

			isAllDefault &= item.defaultSelected;
		}

		if (isAllDefault && checkIfAllDefault) {
			strip = null;
		}
		return strip;
	}

	@Thunk void setListItemBitmap(ListItem item, Bitmap icon) {
		item.mainIcon = icon;

		item.iconCustom = new BitmapDrawable(res, icon);
		item.iconCustom.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
	}

	@Override
	public void doExport(OutputStream out) throws Exception {
		Bitmap image = getImageStrip(false);
		image.compress(Bitmap.CompressFormat.PNG, 100, out);
		out.close();
	}

	@Override
	public Bitmap doImportInBackground(File importFile) throws Exception {
		return BitmapFactory.decodeFile(importFile.getAbsolutePath());
	}

	@Override
	public void onPostImport(Bitmap result) {
		int height = getStripHeight(result);
		int x = 0;
		int total = adapter.getCount();

		for (int i = 0; i < total; i++) {
			ListItem item = adapter.getItem(i);
			setItemFromImageStrip(x, result, item);
			x += height;
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Sets the ListItem to custom image selection from the image strip and returns if the operation was
	 * successful or not.
	 */
	@Thunk boolean setItemFromImageStrip(int x, Bitmap strip, ListItem item) {
		int width = strip == null ? 0 : strip.getWidth();
		int height = getStripHeight(strip);
		int iconSize = BitmapUtils.getIconSize(this);

		if ((x + height) <= width) {
			// icon is present in the strip.
			Bitmap icon = Bitmap.createBitmap(strip, x, 0, height, height);
			if (height != iconSize) {
				icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, true);
			}
			setListItemBitmap(item, icon);
			item.isIconEmpty = false;
			item.defaultSelected = false;
			return true;
		}
		return false;
	}

	private int getStripHeight(Bitmap strip) {
		return strip == null ? 10 : strip.getHeight();
	}

	private class MyAdapter extends ArrayAdapter<ListItem> {

		private final LayoutInflater inflator;

		public MyAdapter(Context context) {
			super(context, 0);
			inflator = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Holder holder;
			if (convertView == null) {
				convertView = inflator.inflate(R.layout.cfg_icon_theme_editor_item, parent, false);
				holder = new Holder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}

			ListItem item = getItem(position);

			holder.item = item;

			holder.text1.setCompoundDrawablesWithIntrinsicBounds(item.iconDefault, 0, 0, 0);
			holder.text1.setChecked(item.defaultSelected);
			
			holder.text2.setCompoundDrawablesWithIntrinsicBounds(item.iconCustom, null, null, null);
			holder.text2.setChecked(!item.defaultSelected);

			return convertView;
		}
	}

	private class Holder implements OnClickListener, IconPicker.Callback {

		@Thunk final CheckedTextView text1;
		@Thunk final CheckedTextView text2;

		@Thunk ListItem item;

		public Holder(View main) {
			text1 = (CheckedTextView) main.findViewById(android.R.id.text1);
			text2 = (CheckedTextView) main.findViewById(android.R.id.text2);

			text1.setOnClickListener(this);
			text2.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			if (v == text1) {
				item.defaultSelected = true;
				adapter.notifyDataSetChanged();
			} else if (item.defaultSelected && !item.isIconEmpty) {
				item.defaultSelected = false;
				adapter.notifyDataSetChanged();
			} else {
				iconPicker.pickIcon(this, Color.WHITE);
			}
		}

		@Override
		public void onIconReceived(Bitmap icon) {
			setListItemBitmap(item, icon);
			item.defaultSelected = false;
			item.isIconEmpty = false;

			adapter.notifyDataSetChanged();
		}
	}


	@Thunk static class ListItem {
		int iconDefault;
		Drawable iconCustom;

		boolean defaultSelected;

		Bitmap mainIcon;
		Bitmap defaultIcon;
		boolean isIconEmpty;
	}
}
