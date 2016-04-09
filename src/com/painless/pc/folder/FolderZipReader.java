package com.painless.pc.folder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;

import com.painless.pc.singleton.BackupUtil;
import com.painless.pc.util.Thunk;

/**
 * A helper class to reading folder backups from zip.
 */
public class FolderZipReader {

  private final ArrayList<ParseData> mParsedList = new ArrayList<ParseData>();
  private final HashSet<String> mIgnoreDbNames = new HashSet<String>();

  private final Context context;
  private final ZipFile zip;

  public FolderZipReader(Context context, ZipFile zip) {
    this.zip = zip;
    this.context = context;
  }

  /**
   * Reads the folder entry and returns the destination db name.
   */
  public String readEntry(String folderName, int folderId) throws Exception {
    String dbName = FolderUtils.getDbName(folderId);
    ZipEntry folderEntry = zip.getEntry(dbName);
    if (folderEntry == null) {
      // Folder data not present.
      throw new Exception();
    }

    ParseData parseData = new ParseData();
    parseData.destName = FolderUtils.newDbName(context, mIgnoreDbNames);
    parseData.folderName = folderName;
    parseData.srcEntry = folderEntry;

    // Add the new dest name to ignore names, so that it is not picked up next time.
    mIgnoreDbNames.add(parseData.destName);
    mParsedList.add(parseData);
    return parseData.destName;
  }

  public int writeAll() throws Exception {
    for (ParseData parseData : mParsedList) {
      File targetFile = context.getDatabasePath(parseData.destName);
      FileOutputStream out = new FileOutputStream(targetFile);
      InputStream in = zip.getInputStream(parseData.srcEntry);
      BackupUtil.copy(in, out);
      in.close();
      out.close();
      FolderUtils.setName(parseData.folderName, parseData.destName, context);
    }
    return mParsedList.size();
  }
  
  /**
   * A holder class to keep the parse list.
   */
  @Thunk static final class ParseData {
    String destName;
    String folderName;
    ZipEntry srcEntry;
  }
}
