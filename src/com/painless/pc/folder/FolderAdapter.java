package com.painless.pc.folder;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.painless.pc.R;

public class FolderAdapter extends ArrayAdapter<String> {

  private final Drawable icon;

  public FolderAdapter(Context context) {
    super(context, android.R.layout.simple_list_item_activated_1);

    icon = context.getResources().getDrawable(R.drawable.icon_folder_open);
    icon.setColorFilter(context.getResources().getColor(R.color.list_item_color), PorterDuff.Mode.SRC_ATOP);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    TextView view = (TextView) super.getView(position, convertView, parent);
    view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
    view.setCompoundDrawablePadding(10);
    return view;
  }
}
