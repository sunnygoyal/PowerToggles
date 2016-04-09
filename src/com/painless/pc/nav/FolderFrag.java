package com.painless.pc.nav;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.folder.FolderAdapter;
import com.painless.pc.folder.FolderPick;
import com.painless.pc.folder.FolderUtils;
import com.painless.pc.folder.FolderZipReader;
import com.painless.pc.picker.FilePicker;
import com.painless.pc.singleton.BackupUtil;
import com.painless.pc.singleton.Debug;

public class FolderFrag extends AbsListFrag implements MultiChoiceModeListener, OnShareTargetSelectedListener, OnClickListener {

  private static final int REQUEST_BACKUP = 10;
  private static final int REQUEST_RESTORE = 11;
  private static final String SHARE_FILE_NAME = "folder.pcf";

  private Context mContext;
  private ArrayList<String> mFolderList;
  private ArrayList<String> mSelectedList;
  private FolderAdapter mAdapter;

  private ActionMode mMode;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    mContext = getActivity();
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mContext = view.getContext();
    setEmptyMsg(R.string.folder_help_1, R.string.folder_help_2);

    mFolderList = new ArrayList<String>();
    mSelectedList = new ArrayList<String>();
    mAdapter = new FolderAdapter(mContext);
    
    getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    getListView().setMultiChoiceModeListener(this);


    refreshList();
    setListAdapter(mAdapter);
  }

  private void refreshList() {
    String defaultName = getString(R.string.folder_name);
    SharedPreferences prefs = mContext.getSharedPreferences(FolderUtils.PREFS, Context.MODE_PRIVATE);
    mAdapter.clear();
    mFolderList.clear();

    for (String dbName : mContext.databaseList()) {
      if (dbName.matches(FolderUtils.DB_NAME_REGX)) {
        String name = prefs.getString(FolderUtils.KEY_NAME_PREFIX + dbName, defaultName);

        mFolderList.add(dbName);
        mAdapter.add(name);
      }
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    String db = mFolderList.get(position);
    if (getActivity() instanceof PreferenceActivity) {
      Bundle extra = new Bundle();
      extra.putString("id", db);
      ((PreferenceActivity) getActivity()).startPreferencePanel(
              CFolderFrag.class.getName(),
              extra,
              R.string.lbl_customize,
              "",
              null,
              0);
    } else if (getActivity() instanceof FolderPick) {
      ((FolderPick) getActivity()).returnFolder(db, mAdapter.getItem(position));
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.folder_restore, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.mnu_restore) {
      startActivityForResult(new Intent(mContext, FilePicker.class)
          .putExtra("savemode", false)
          .putExtra("title", getString(R.string.wp_restore))
          .putExtra("filter", ".pcf"),
      REQUEST_RESTORE);
    }
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }
    String fileName = data.getStringExtra("file");
    if (requestCode == REQUEST_BACKUP) {
      String[] msgArray = getResources().getStringArray(R.array.wc_export_msg);
      try {
        saveFolders(new FileOutputStream(fileName));
        String msg = String.format(msgArray[1], fileName);
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
        mMode.finish();
      } catch (Throwable e) {
        Debug.log(e);
        Toast.makeText(mContext, msgArray[2], Toast.LENGTH_LONG).show();
      }
    } else if (requestCode == REQUEST_RESTORE) {
      // Restore backup
      try {
        Toast.makeText(mContext, getString(R.string.folder_restore_msg, restoreBackup(fileName)), Toast.LENGTH_LONG).show();
      } catch (Exception e) {
        Debug.log(e);
        Toast.makeText(mContext, R.string.folder_restore_error, Toast.LENGTH_LONG).show();
      }
      refreshList();
    }
  }

  private int restoreBackup(String fileName) throws Exception {
    ZipFile zip = new ZipFile(fileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry("folders.txt"))));
    FolderZipReader folderReader = new FolderZipReader(mContext, zip);
    String line;
    int count = 0;
    while ((line = reader.readLine()) != null) {
      folderReader.readEntry(line, count);
      count++;
    }
    reader.close();
    return folderReader.writeAll();
  }

  private void saveFolders(OutputStream os) throws Exception {
    ZipOutputStream outStream = null;
    String defaultName = getString(R.string.folder_name);
    SharedPreferences prefs = mContext.getSharedPreferences(FolderUtils.PREFS, Context.MODE_PRIVATE);
    try {
      outStream = new ZipOutputStream(os);
      int i = 0;
      ArrayList<String> dbNames = new ArrayList<String>();
      for (String db : mSelectedList) {
        BackupUtil.addDbToZip(db, outStream, mContext, i);
        i++;
        dbNames.add(prefs.getString(FolderUtils.KEY_NAME_PREFIX + db, defaultName));
      }

      outStream.putNextEntry(new ZipEntry("folders.txt"));
      outStream.write(TextUtils.join("\n", dbNames).getBytes());
      outStream.closeEntry();
    } finally {
      if (outStream != null) {
        outStream.close();
      }
    }
  }

  ///////// code for share
  private void setSelectedList() {
    mSelectedList.clear();
    SparseBooleanArray checked = getListView().getCheckedItemPositions();

    for (int i = 0; i<checked.size(); i++) {
      if (checked.valueAt(i)) {
        mSelectedList.add(mFolderList.get(checked.keyAt(i)));
      }
    }
  }

  @Override
  public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
    setSelectedList();
    try {
      saveFolders(mContext.openFileOutput(SHARE_FILE_NAME, Context.MODE_WORLD_READABLE));
      mMode.finish();
      return true;
    } catch (Exception e) {
      Debug.log(e);
      return false;
    }
  }

  ///////// code for multi-select
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    mode.getMenuInflater().inflate(R.menu.folder_menu, menu);
    ShareActionProvider shareAction = (ShareActionProvider) menu.findItem(R.id.mnu_share).getActionProvider();
    shareAction.setOnShareTargetSelectedListener(this);
    shareAction.setShareIntent(new Intent(Intent.ACTION_SEND)
        .setType("application/zip")
        .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mContext.getFileStreamPath(SHARE_FILE_NAME))));
    mMode = mode;
    return true;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    setSelectedList();

    if (mSelectedList.size() < 1) {
      return false;
    }
    if (item.getItemId() == R.id.mnu_delete) {
      new AlertDialog.Builder(mContext)
        .setMessage(getString(R.string.folder_delete_prompt, mSelectedList.size()))
        .setNegativeButton(R.string.act_no, null)
        .setPositiveButton(R.string.act_yes, this)
        .show();
    } else if (item.getItemId() == R.id.mnu_backup) {
      startActivityForResult(new Intent(mContext, FilePicker.class)
          .putExtra("savemode", true)
          .putExtra("title", getString(R.string.wp_backup))
          .putExtra("filter", ".pcf"),
        REQUEST_BACKUP);
    }
    return true;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) { }

  @Override
  public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) { }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  /**
   * Called when deleting multiple folders and accepting the confirmation prompt.
   */
  @Override
  public void onClick(DialogInterface dialog, int which) {
    SharedPreferences.Editor editor = mContext.getSharedPreferences(FolderUtils.PREFS, Context.MODE_PRIVATE).edit();
    for (String folderId : mSelectedList) {
      editor.remove(FolderUtils.KEY_NAME_PREFIX + folderId);
      mContext.deleteDatabase(folderId);
    }
    editor.commit();
    mMode.finish();
    refreshList();
  }
}
