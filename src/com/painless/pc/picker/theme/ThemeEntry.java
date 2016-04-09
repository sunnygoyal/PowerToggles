package com.painless.pc.picker.theme;

import java.io.File;
import java.net.URL;

import org.json.JSONObject;

import android.graphics.Bitmap;

/**
 * An theme entry.
 */
public class ThemeEntry {

  // Title if its a section title
  public int title = 0;

  // Set to true is loading this theme failed
  public boolean failed = false;

  // Theme file for local themes
  public File themeFile;
  public URL remoteUrl;

  public JSONObject config;
  public Bitmap background;
  public int backgroundRes = 0;

  // Values used for rendering
  public int[] padding;
  public float[] stretch;
  public boolean hideDividers;
  public int dividerColor;

  public final int[] buttonColors = new int[3];
  public final int[] buttonAlphas = new int[3];

  public final boolean isLoaded() {
    return background != null ||
            backgroundRes != 0;
  }
}
