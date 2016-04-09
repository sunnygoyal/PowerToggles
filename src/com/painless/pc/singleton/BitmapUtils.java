package com.painless.pc.singleton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.WindowManager;

import com.painless.pc.R;
import com.painless.pc.util.UiUtils;

public class BitmapUtils {

  private static int ICON_SIZE;
  public static int getIconSize(Context context) {
    if (ICON_SIZE <= 0) {
      final Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_toggle_airplane);
      ICON_SIZE = b.getHeight();
      b.recycle();
    }
    return ICON_SIZE;
  }

  public static int getActivityIconSize(Context c) {
    return 2 * ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getLauncherLargeIconSize();
  }

  public static Bitmap drawableToBitmap(Drawable drawable, Context context) {
    return drawableToBitmap(drawable, getIconSize(context), true);
  }

  public static Bitmap drawableToBitmap(Drawable drawable, int iconSize, boolean recycle) {
    Bitmap tmp;
    if (drawable instanceof BitmapDrawable && recycle) {
      tmp =  ((BitmapDrawable) drawable).getBitmap();
    } else {
      tmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(tmp); 
      drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
      drawable.draw(canvas);
    }
    return Bitmap.createScaledBitmap(tmp, iconSize, iconSize, true);
  }

  public static Bitmap cropMaxVisibleBitmap(Drawable drawable, int iconSize) {
    Bitmap tmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(tmp);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    Rect crop = new Rect(tmp.getWidth(), tmp.getHeight(), -1, -1);
    for(int y = 0; y < tmp.getHeight(); y++) {
      for(int x = 0; x < tmp.getWidth(); x++) {
        int alpha = (tmp.getPixel(x, y) >> 24) & 255;
        if(alpha > 0) {   // pixel is not 100% transparent
          if(x < crop.left)
            crop.left = x;
          if(x > crop.right)
            crop.right = x;
          if(y < crop.top)
            crop.top = y;
          if(y > crop.bottom)
            crop.bottom = y;
        }
      }
    }

    if(crop.width() <= 0 || crop.height() <= 0) {
      return Bitmap.createScaledBitmap(tmp, iconSize, iconSize, true);
    }

    // We want to crop a square region.
    float size = Math.max(crop.width(), crop.height());
    float xShift = (size - crop.width()) * 0.5f;
    crop.left -= Math.floor(xShift);
    crop.right += Math.ceil(xShift);

    float yShift = (size - crop.height()) * 0.5f;
    crop.top -= Math.floor(yShift);
    crop.bottom += Math.ceil(yShift);

    Bitmap finalImage = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
    canvas.setBitmap(finalImage);
    float scale = iconSize / size;

    canvas.scale(scale, scale);
    canvas.drawBitmap(tmp, -crop.left, -crop.top, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
    canvas.setBitmap(null);
    return finalImage;
  }

  public static Bitmap resizeToIconSize(Bitmap src, Context context, boolean makeSquare) {
    int iconSize = getIconSize(context);

    if (makeSquare) {
      return Bitmap.createScaledBitmap(src, iconSize, iconSize, true);
    }

    int height = src.getHeight();
    if (height == iconSize) {
      return src;
    }

    int width = src.getWidth();
    if (width == (height + 1)) {
      // Battery icon format
      Bitmap background = Bitmap.createBitmap(src, 0, 0, height, height);
      return createBatteryBitmapFormat(background, src.getPixel(height, 0), context);

    } else {
      return Bitmap.createScaledBitmap(src, (width * iconSize / height), iconSize, true);
    }
  }


  public static Bitmap createBatteryBitmapFormat(Bitmap background, int formatColor, Context context) {
    int iconSize = getIconSize(context);
    Bitmap output = Bitmap.createBitmap(iconSize + 1, iconSize, Bitmap.Config.ARGB_8888);

    Paint aliasPaint = new Paint();
    aliasPaint.setAntiAlias(true);
    aliasPaint.setFilterBitmap(true);
    Canvas c = new Canvas(output);
    c.drawBitmap(background, new Rect(0, 0, background.getWidth(), background.getHeight()),
            new Rect(0, 0, iconSize, iconSize), aliasPaint);

    Paint linePaint = new Paint();
    linePaint.setColor(formatColor);
    c.drawRect(iconSize, 0, iconSize + 1, iconSize, linePaint);
    return output;
  }

  public static Bitmap resizeToScreenSizeLimit(Bitmap background, Context context) {

    // check if resize is needed.
    Bitmap resizedIcon = null;
    Point displaySize = UiUtils.getDisplaySize((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
    int width = Math.min(displaySize.x, displaySize.y);

    int iconWidth = background.getWidth();
    int iconHeight = background.getHeight();
    
    resizedIcon = Bitmap.createScaledBitmap(background,
            Math.min(width, iconWidth),
            Math.min(2 * getIconSize(context), iconHeight),
            true);

    if ((resizedIcon != null) && (resizedIcon != background)) {
      Debug.log("Resized");
      // New image was created.
      background.recycle();
      return resizedIcon;
    } else {
      return background;
    }
  }

  public static boolean saveBitmap(Bitmap image, int[] strech, File out) {
    try {
      return saveBitmap(image, strech, new FileOutputStream(out));
    } catch (Exception e) {
      Debug.log(e);
      return false;
    }
  }

  private static boolean saveBitmap(Bitmap image, OutputStream out) {
    if (image != null) {
      try {
        image.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        return true;
      } catch (Exception e) {
        Debug.log(e);
      }
    }
    return false;
  }

  public static boolean saveBitmap(Bitmap image, int[] strech, OutputStream out) {
    if ((strech[2] <= strech[0]) || (strech[3] <= strech[1])) {
      // Invalid stretch region, do not add any nine patch chunk.
      return saveBitmap(image, out);
    }
    if (image != null) {            
      try {
        byte[] npTcChunk = createNtpcChunk(strech[0], strech[2], strech[1], strech[3]);
        ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, imgOut);

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(imgOut.toByteArray()));
        DataOutputStream fileOut = new DataOutputStream(out);

        // PNG header
        fileOut.writeInt(input.readInt());
        fileOut.writeInt(input.readInt());

        // copy IHDR chunk
        int ihdrLen = input.readInt();
        fileOut.writeInt(ihdrLen);
        fileOut.writeInt(input.readInt());  // chunk name (=IHDR)
        // Chunk data
        byte[] chunkData = new byte[ihdrLen];
        input.read(chunkData);
        fileOut.write(chunkData);
        fileOut.writeInt(input.readInt());  // CRC

        // Inject npTc chunk.
        fileOut.writeInt(npTcChunk.length);
        // header
        CRC32 crc = new CRC32();
        byte[] header = "npTc".getBytes();
        crc.update(header);
        fileOut.write(header);
        // chunk data
        crc.update(npTcChunk);
        fileOut.write(npTcChunk);
        // crc
        fileOut.writeInt((int) crc.getValue());
        
        BackupUtil.copy(input, fileOut);
        fileOut.close();
        return true;
      } catch (Exception e) {
        Debug.log(e);
      }
    }
    return false;
  }

  private static byte[] createNtpcChunk(int xS, int xE, int yS, int yE) {
    byte[] data = new byte[84];
    data[1] = 2;
    data[2] = 2;
    data[3] = 9;
    writeInt(data, 32, xS);
    writeInt(data, 36, xE);
    writeInt(data, 40, yS);
    writeInt(data, 44, yE);
    for (int i = 48; i < 84; i+=4) {
      writeInt(data, i, i);
    }
    return data;
  }

  private static void writeInt(byte[] target, int index, int val) {
    for (int i=0, shift = 24; i<4; i++, index++, shift -=8) {
      target[index] =(byte) ((val >> shift) & 0xFF);
    }
  }

  public static ByteArrayOutputStream compressImage(Bitmap img) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!img.compress(Bitmap.CompressFormat.PNG, 100, out)) {
      out.reset();
      Debug.log("Image save failed. Copying image");
      Bitmap copy = img.copy(Bitmap.Config.ARGB_8888, false);
      copy.compress(Bitmap.CompressFormat.PNG, 100, out);
      copy.recycle();
    }
    return out;
  }
}
