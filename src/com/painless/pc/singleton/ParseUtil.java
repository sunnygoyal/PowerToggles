package com.painless.pc.singleton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Color;

public class ParseUtil {

  private static final int BUF_SIZE = 0x1000; // 4K

	/**
	 * Parses the value to a boolean array bitwise
	 * @return the number of on bits
	 */
	public static int parseBoolArray(boolean[] dest, int value) {
		int checkedCount = 0;
		for (int i=0; i<dest.length; i++) {
			int isChecked = (value >> i) & 1;
			checkedCount += isChecked;
			dest[i] = isChecked == 1;
		}
		return checkedCount;
	}

	/**
	 * Parsed the comma separated string into a int array.
	 */
	public static int[] parseIntArray(int[] dest, String value) {
		String[] parts = value.split(",");
		if (dest == null) {
			dest = new int[parts.length];
		}
		for (int i=0; i<dest.length; i++) {
			dest[i] = Integer.parseInt(parts[i]);
		}

		return dest;
	}

	public static int addAlphaToColor(int alpha, int color) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}

	public static int removeAlphaFromColor(int color) {
		return addAlphaToColor(255, color);
	}

	public static byte[] readStream(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      byte[] buf = new byte[BUF_SIZE];
      long total = 0;
      while (true) {
        int r = in.read(buf);
        if (r == -1) {
          break;
        }
        out.write(buf, 0, r);
        total += r;
      }
      return out.toByteArray();
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        Debug.log(e);
      }
    }
	}
}
