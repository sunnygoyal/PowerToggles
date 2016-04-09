package com.painless.pc.theme;

import android.graphics.Bitmap;

public interface ToggleBitmapProvider {

	/**
	 * Returns the bitmap corresponding to the bitmap or null.
	 */
	Bitmap getIcon(int displayNo);
}
