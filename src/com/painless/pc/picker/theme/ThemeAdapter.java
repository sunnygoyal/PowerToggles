package com.painless.pc.picker.theme;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.util.Thunk;
import com.painless.pc.view.NinePatchView;

public class ThemeAdapter extends ArrayAdapter<ThemeEntry> {

  private static final int TYPE_HEADER = 0;
  private static final int TYPE_LOADING = 1;
  private static final int TYPE_PREVIEW = 2;

  private final int[] mIconState = {2, 1, 0, 0, 2};
  private final LayoutInflater mInflator;
  public final ThemeLoader mLoader;
  
  public ThemeAdapter(Context context) {
    super(context, 0);
    mInflator = LayoutInflater.from(context);
    mLoader = new ThemeLoader(this);
  }

  @Override
  public int getViewTypeCount() {
    return 3;
  }

  @Override
  public int getItemViewType(int position) {
    ThemeEntry entry = getItem(position);
    return (entry.title != 0) ? TYPE_HEADER : (entry.isLoaded() ? TYPE_PREVIEW : TYPE_LOADING);
  }

  @Override
  public boolean areAllItemsEnabled() {
    return false;
  }

  @Override
  public boolean isEnabled(int position) {
    return getItem(position).isLoaded();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ThemeEntry entry = getItem(position);
    if (entry.title != 0) {
      // Section
      TextView title = (TextView) ((convertView != null) ? convertView :
        mInflator.inflate(R.layout.list_item_header, parent, false));
      title.setText(entry.title);
      return title;
    } else if (!entry.isLoaded()) {
      // Submit for loading
      mLoader.submic(entry);
      return convertView != null ? convertView : mInflator.inflate(R.layout.theme_item_loading, parent, false);
    } else {
      // Show preview
      MyHolder holder;
      
      if (convertView == null) {
        convertView = mInflator.inflate(R.layout.theme_item_preview, parent, false);
        holder = new MyHolder(convertView);
        convertView.setTag(holder);
      } else {
        holder = (MyHolder) convertView.getTag();
      }

      if (entry.background != null) {
        mLoader.register(entry.background);
        holder.back.setBackgroundColor(Color.TRANSPARENT);
        holder.back.setBitmap(entry.background);
        holder.back.setDim(entry.stretch);
        holder.back.setPadding(entry.padding[0], entry.padding[1], entry.padding[2], entry.padding[2]);
      } else {
        holder.back.setBitmap(null);
        holder.back.setBackgroundResource(entry.backgroundRes);
      }

      if (entry.hideDividers) {
        for (int i=0; i<4; i++) {
          holder.mDivs[i].setVisibility(View.GONE);
        }
      } else {
        for (int i=0; i<4; i++) {
          holder.mDivs[i].setVisibility(View.VISIBLE);
          holder.mDivs[i].setBackgroundColor(entry.dividerColor);
        }
      }
      
      
      for (int i=0; i<5; i++) {
        int state = mIconState[i];
        holder.mIcons[i].setColorFilter(entry.buttonColors[state]);
        holder.mIcons[i].setAlpha(entry.buttonAlphas[state]);
      }
      return convertView;
    }
  }

  private static class MyHolder {
    @Thunk final NinePatchView back;
    @Thunk final View[] mDivs;
    @Thunk final ImageView[] mIcons;

    public MyHolder(View container) {
      back = (NinePatchView) container.findViewById(R.id.bgImage);
      mDivs = new View[4];
      for (int i=0; i<4; i++) {
        mDivs[i] = container.findViewById(Globals.BUTTON_DIVS[i]);
      }
      
      mIcons = new ImageView[5];
      for (int i=0; i<5; i++) {
        mIcons[i] = (ImageView) container.findViewById(Globals.BUTTONS[i]);
      }
    }
  }
}
