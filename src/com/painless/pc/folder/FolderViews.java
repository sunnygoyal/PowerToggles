package com.painless.pc.folder;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.painless.pc.R;

public class FolderViews extends RemoteViews {

  public final View root;
  public final ImageView image;
  public final TextView text;

  public int position;

  public FolderViews(Context context,  LayoutInflater inflator, ViewGroup parent) {
    this(context, inflator.inflate(R.layout.folder_item, parent, false));
  }

  public FolderViews(Context context, View root) {
    super(context.getPackageName(), R.layout.folder_item);
    this.root = root;
    image = (ImageView) root.findViewById(R.id.img_preview);
    text = (TextView) root.findViewById(android.R.id.text1);
    root.setTag(this);
  }

  @Override
  public void setImageViewResource(int viewId, int srcId) {
    image.setImageResource(srcId);
  }

  @Override
  public void setImageViewBitmap(int viewId, Bitmap bitmap) {
    image.setImageBitmap(bitmap);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setInt(int viewId, String methodName, int value) {
    if ("setAlpha".equals(methodName)) {
      image.setAlpha(value);
    } else {
      image.setColorFilter(value);
    }
  }
}
