package com.painless.pc.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.painless.pc.R;

public class BatteryImageProvider implements ToggleBitmapProvider {

	private static final int DEFAULT_SETTINGS = 0xA5AA5A20;

	private final Bitmap background;
	private final int iconSize;

	private final Bitmap cache;
	private final Canvas cacheCanvas;

	private final Bitmap digitsBitmap;
	private final Drawable digitsDrawable;
	private final Canvas digitsCanvas;

	private final Rect digitSrcRect;	
	private final RectF digitDestRect;	
	
	private int cacheValue;

	public BatteryImageProvider(Bitmap background, Context context) {
	  this(background, context, DEFAULT_SETTINGS);
	}

	 public BatteryImageProvider(Bitmap background, Context context, int settings) {
	    this.background = background;
	    iconSize = background.getHeight();
	    
	    cache = Bitmap.createBitmap(background.getHeight(), background.getHeight(), Bitmap.Config.ALPHA_8);
	    cacheCanvas = new Canvas(cache);

	    digitsDrawable = context.getResources().getDrawable(R.drawable.notify_icon_digits_w);
	    digitsBitmap = Bitmap.createBitmap(digitsDrawable.getIntrinsicWidth(), digitsDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
	    digitsCanvas = new Canvas(digitsBitmap);
	    digitsDrawable.setBounds(0, 0, digitsCanvas.getWidth(), digitsCanvas.getHeight());

	    digitSrcRect = new Rect(0, 0, digitsCanvas.getWidth(), digitsCanvas.getHeight());
	    digitDestRect = new RectF();

	    loadDimFromColor(background.getWidth() > background.getHeight() ?
	        background.getPixel(background.getHeight(), 0) : settings);
	  }

	@Override
	public Bitmap getIcon(int displayNo) {
		if (cacheValue != displayNo) {
			updateCache(displayNo);
		}
		return cache;
	}

	private void updateCache(int value) {
		cacheValue = value;

		cacheCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

		digitsCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		digitsDrawable.setLevel(value);
		digitsDrawable.draw(digitsCanvas);

		final Paint digitPaint = new Paint();
		digitPaint.setFilterBitmap(true);
		cacheCanvas.drawBitmap(digitsBitmap, digitSrcRect, digitDestRect, digitPaint);
		
		final Paint maskPaint = new Paint();
		maskPaint.setFilterBitmap(true);
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
		cacheCanvas.drawBitmap(background, 0, 0, maskPaint);
	}

	public void loadDimFromColor(int color) {
	  digitDestRect.set(iconSize * Color.blue(color) / 255.0f,
	          iconSize * Color.green(color) / 255.0f,
	          iconSize * Color.red(color) / 255.0f,
	          iconSize * Color.alpha(color) / 255.0f);
		cacheValue = -1;
	}

	public RectF getDigitRect() {
		return digitDestRect;
	}

	public int getSize() {
		return iconSize;
	}
}
