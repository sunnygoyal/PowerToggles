package com.painless.pc.theme;

import android.graphics.Bitmap;

public class FixedImageProvider implements ToggleBitmapProvider {

	private final Bitmap icon;

	public FixedImageProvider (Bitmap icon) {
		this.icon = icon;
	}

	@Override
	public Bitmap getIcon(int displayNo) {
//		if (icon != null) {
//			Bitmap cache  = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
//			Paint p = new Paint();
//			p.setShadowLayer(1, 2, 2, 0x66FFFFFF);
//			Canvas c = new Canvas(cache);
//			c.drawBitmap(icon.extractAlpha(), 0, 0, p);
//			p.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN));
//			c.drawBitmap(icon.extractAlpha(), 0, 0, p);
//			return cache;
//		}
		return icon;
	}
}
