package com.painless.pc.singleton;

import static com.painless.pc.util.SettingsDecoder.KEY_DENSITY;
import static com.painless.pc.util.SettingsDecoder.KEY_PADDING;
import static com.painless.pc.util.SettingsDecoder.KEY_STRETCH;
import static com.painless.pc.util.SettingsDecoder.KEY_TRACKER_ARRAY;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.painless.pc.FileProvider;
import com.painless.pc.PCWidgetActivity;
import com.painless.pc.TrackerManager;
import com.painless.pc.cfg.section.BackgroundSection;
import com.painless.pc.folder.FolderUtils;
import com.painless.pc.folder.FolderZipReader;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.tracker.SimpleShortcut;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.WidgetSetting;

/**
 * Utility class to handle backup and restores.
 */
public class BackupUtil {

  public static void createBackup(OutputStream os, int widgetId, Context context) throws Exception {
    ZipOutputStream outStream = null;
    try {
      outStream = new ZipOutputStream(os);
      String widgetConfig = SettingStorage.getSettingString(context, widgetId);
      Bitmap[] allIcons = WidgetDB.get(context).getAllIcons(widgetId);
      WidgetSetting setting = SettingStorage.buildWidgetSettings(widgetConfig, context, widgetId, allIcons);

      for (int i = 0; i < 8 ; i++) {
        widgetConfig += BackupUtil.addTrackerToZip(outStream, setting.trackers[i], allIcons[i], i, context);
      }
      
      if (setting.backimage != null) {
        outStream.putNextEntry(new ZipEntry("back.png"));
        copy(context.getContentResolver().openInputStream(setting.backimage), outStream);
        outStream.closeEntry();
      }
  
      outStream.putNextEntry(new ZipEntry("config.txt"));
      outStream.write(widgetConfig.getBytes());
      outStream.closeEntry();
    } finally {
      if (outStream != null) {
        outStream.close();
      }
    }
  }
  
  /**
   * Adds the tracker related information to the settings and returns the config update.
   */
  public static String addTrackerToZip(ZipOutputStream zip, AbstractTracker tracker, Bitmap icon, int pos, Context context) throws Exception {
    addImage(zip, icon, pos + ".png");
    if (tracker == null) {
      return "";
    }
    if (tracker instanceof SimpleShortcut) {
      SimpleShortcut shrt = (SimpleShortcut) tracker;
      Intent intent = shrt.getIntent();

      if (FolderUtils.ACTION.equals(intent.getAction())) {
        // It is a folder. Export DB.
        addDbToZip(intent.getData().getQuery(), zip, context, pos);
      }
      
      return "\n" + intent.toUri(Intent.URI_INTENT_SCHEME) + "\n" + shrt.getLabel(null);
    } else if (tracker instanceof PluginTracker) {
      PluginTracker plugin = (PluginTracker) tracker;
      return "\n" + plugin.getIntent().toUri(Intent.URI_INTENT_SCHEME) + "\n" + plugin.getLabel(null);
    } 
    return "";
  }

  public static void addDbToZip(String dbName, ZipOutputStream zip, Context context, int pos) throws Exception {
    File dbFile = context.getDatabasePath(dbName);
    FileInputStream in = new FileInputStream(dbFile);

    zip.putNextEntry(new ZipEntry(FolderUtils.getDbName(pos)));
    copy(in, zip);
    in.close();
    zip.closeEntry();
  }

  public static void addImage(ZipOutputStream zip, Bitmap icon, String name) throws Exception {
    if (icon != null) {
      zip.putNextEntry(new ZipEntry(name));
      BitmapUtils.compressImage(icon).writeTo(zip);
      zip.closeEntry();
    }
  }
 
  public static BackupData readSettings(File importFile, Context context, int widgetId) throws Exception {
    ZipFile zip = new ZipFile(importFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry("config.txt"))));

    SettingsDecoder decoder = new SettingsDecoder(reader.readLine());

    AbstractTracker[] trackers = new AbstractTracker[8];
    Bitmap[] allIcons = new Bitmap[8];
    String[] ids = decoder.getTrackerDef().split(",");
    SharedPreferences pref = Globals.getAppPrefs(context);

    if (ids.length == 0) {
      zip.close();
      // No tracker to import.
      throw new Exception();
    }

    FolderZipReader folderReader = new FolderZipReader(context, zip);

    String newTrackerList = "";
    for (int i = 0; i<8 && i<ids.length; i++) {
      String id = ids[i];
      if (id.startsWith("ss_")) {
        Intent intent = Intent.parseUri(reader.readLine(), 0);
        String folderName = reader.readLine();
        SimpleShortcut shrt = new SimpleShortcut(intent, folderName);
        shrt.setId(SimpleShortcut.getId(widgetId, i));
        trackers[i] = shrt;
        // Check if it is a folder
        if (FolderUtils.ACTION.equals(intent.getAction())) {
          // Update the shortcut intent.
          intent.setData(Uri.parse("folder/?" + folderReader.readEntry(folderName, i)));
        }

      } else if (id.startsWith("pl_")) { 
        trackers[i] = new PluginTracker(id.substring(3), Intent.parseUri(reader.readLine(), Intent.URI_INTENT_SCHEME), reader.readLine());
      } else {
        trackers[i] = TrackerManager.getTracker(Integer.parseInt(id), pref);
      }
      newTrackerList += trackers[i].getId() + ",";
      ZipEntry iconEntry = zip.getEntry(i + ".png");
      if (iconEntry != null) {
        allIcons[i] = BitmapUtils.resizeToIconSize(
            BitmapFactory.decodeStream(zip.getInputStream(iconEntry)),
            context,
            false);
      }
    }
    decoder.settings.put(KEY_TRACKER_ARRAY, newTrackerList.substring(0, newTrackerList.length() - 1));

    BackupData data = new BackupData();
    data.settings = new WidgetSetting(context, trackers, decoder, widgetId, allIcons);
    data.settings.backimage = null;

    data.decoder = decoder;

    int deviceDensity = context.getResources().getDisplayMetrics().densityDpi;
    int settingDensity = decoder.getValue(KEY_DENSITY, deviceDensity);
    normalizeRect(decoder, KEY_PADDING, deviceDensity, settingDensity);
    normalizeRect(decoder, KEY_STRETCH, deviceDensity, settingDensity);
    

    ZipEntry backImage = zip.getEntry("back.png");
    if (backImage != null) {
      Bitmap back = BitmapFactory.decodeStream(zip.getInputStream(backImage));
      data.backImage = Bitmap.createScaledBitmap(back,
              back.getWidth() * deviceDensity / settingDensity,
              back.getHeight() * deviceDensity / settingDensity,
              true);
      if (back != data.backImage) {
        back.recycle();
      }
    } else {
      data.backImage = null;
    }

    // Now save folders.
    folderReader.writeAll();

    data.icons = allIcons;
    zip.close();
    return data;
  }

  /**
   * Imports the backup and applies it to the widget.
   * @return {@true} on success;
   */
  public static boolean importBackup(String importFile, Context context, int widgetId) {
    try {
      BackupData data = readSettings(new File(importFile), context, widgetId);

      String backFileName = FileProvider.backFileName(widgetId);
      if (BitmapUtils.saveBitmap(
              data.backImage,
              data.decoder.getRect(KEY_STRETCH),
              FileProvider.widgetBackFile(context, widgetId))) {
      } else {
        context.deleteFile(backFileName);
      }
  
      // Save shortcuts and icons
      WidgetDB db = WidgetDB.get(context);
      db.deleteToggles(widgetId);
      for (int i = 0; i < 8; i++) {
        AbstractTracker tracker = data.settings.trackers[i];
        if (tracker == null) {
          continue;
        }
        if (tracker.getId().startsWith("ss_")) {
          final SimpleShortcut shrt = (SimpleShortcut) tracker;
          shrt.setId(SimpleShortcut.getId(widgetId, i));
          db.saveShrt(shrt, data.icons[i]);
        } else if (data.icons[i] != null) {
          db.saveIcon(SimpleShortcut.getId(widgetId, i), data.icons[i]);
          if (tracker.getId().startsWith("pl_")) {
            PluginDB.get(context).save((PluginTracker) tracker);
          }
        }
      }
      WidgetDB.closeAll();
      SettingStorage.clearCache();
      SettingStorage.addWidget(context, widgetId, data.decoder.settings.toString());
      context.getSharedPreferences(Globals.EXTRA_PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putLong("last_edit" + widgetId, System.currentTimeMillis()).commit();
  
      PCWidgetActivity.fullUpdateSingleWidgets(context, widgetId);
      SettingStorage.cleanupCachedPlugins(context);
      return true;
    } catch (Exception e) {
      Debug.log(e);
      return false;
    }
  }

  public static void copy(InputStream in, OutputStream out) throws Exception {
    byte[] bucket = new byte[8 * 1024];
    int bytesRead = 0;
    while ((bytesRead = in.read(bucket)) != -1) {
        out.write(bucket, 0, bytesRead);
    }
  }

  /**
   * Normalizes the rectangle stored in the settings based on the difference in density. 
   * @return the normalized rectangle.
   */
  public static int[] normalizeRect(SettingsDecoder decoder, String key, int deviceDensity, int settingDensity) {
    int[] rect = decoder.getRect(key);
    for (int i = 0; i < 4; i++) {
      rect[i] = rect[i] * deviceDensity / settingDensity;
    }
    try {
      decoder.settings.put(key, BackgroundSection.getStr(rect));
    } catch (Exception e) { }
    return rect;
  }

  /**
   * A holder class to pass around widget backup parsed data.
   */
  public static final class BackupData {
    public WidgetSetting settings;
    public Bitmap[] icons;
    public SettingsDecoder decoder;
    public Bitmap backImage;
  }
}
