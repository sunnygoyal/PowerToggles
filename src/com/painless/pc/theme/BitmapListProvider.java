package com.painless.pc.theme;

import android.graphics.Bitmap;

/**
 * The provider receives a horizontal list of images. It then splits the images into
 * square blocks and stores the images in order. The returned image is bated on the
 * display number and the bitmap array index.
 */
public class BitmapListProvider implements ToggleBitmapProvider {

  private final Bitmap mIconListImage;
  private final int mIconCount;
  private final int mIconHeight;
	private final Bitmap[] iconList;

	public BitmapListProvider(Bitmap iconListImage) {
	  mIconHeight = iconListImage.getHeight();
		mIconCount = iconListImage.getWidth() / mIconHeight;
		mIconListImage = iconListImage;
		iconList = new Bitmap[mIconCount];
	}

	@Override
	public Bitmap getIcon(int displayNo) {
	  if (displayNo < iconList.length) {
	    if (iconList[displayNo] == null) {
	      // Cache icon
	      iconList[displayNo] = Bitmap.createBitmap(mIconListImage, displayNo * mIconHeight, 0, mIconHeight, mIconHeight);
	    }
	    return iconList[displayNo];
	  }
		return null;
	}
}
