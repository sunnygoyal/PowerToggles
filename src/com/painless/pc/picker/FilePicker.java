package com.painless.pc.picker;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.SectionAdapter;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.UiUtils;

public class FilePicker extends ListActivity implements OnItemClickListener, android.view.View.OnClickListener {

  private static final int PERMISSION_REQUEST_CODE = 1;

  @Thunk String mFilter;
  private boolean mSaveMode;

  private TextView edtInput;
  private File mCurrentFile;

  private TextView txtFpLocation;
  @Thunk HorizontalScrollView fpFolderContainer;
  @Thunk LinearLayout fpFolders;

  private final ArrayList<File> mFilesInList = new ArrayList<File>();
  private SectionAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.file_picker);
    setTitle(getIntent().getStringExtra("title"));

    mFilter = getIntent().getStringExtra("filter");
    mSaveMode = getIntent().getBooleanExtra("savemode", false);

    edtInput = (TextView) findViewById(R.id.edt_input);

    txtFpLocation = (TextView) findViewById(R.id.txt_fp_location);
    fpFolderContainer = (HorizontalScrollView) findViewById(R.id.fp_folder_c);
    fpFolderContainer.setSmoothScrollingEnabled(false);

    fpFolders = (LinearLayout) findViewById(R.id.fp_folders);

    mAdapter = new SectionAdapter(this);
    setListAdapter(mAdapter);
    getListView().setOnItemClickListener(this);

    if (!mSaveMode) {
      edtInput.setVisibility(View.GONE);
    }

    setResult(RESULT_CANCELED);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      initFolders();
    } else {
      initMarshMallow();
    }
  }

  @TargetApi(23)
  private void initMarshMallow() {
    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
      initFolders();
    } else {
      requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == PERMISSION_REQUEST_CODE) {
      if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0]) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        initFolders();
      } else {
        finish();
      }
    }
  }

  private void initFolders() {
    File startFolder = new File(Environment.getExternalStorageDirectory(), "Power Toggles");
    if (!startFolder.exists()) {
      startFolder.mkdirs();
    }
    initFolder(startFolder);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return mSaveMode && UiUtils.addDoneInMenu(menu, this);
  }

  /**
   * Done clicked
   */
  @Override
  public void onClick(View v) {
    String fileName = edtInput.getText().toString();
    if (fileName.isEmpty()) {
      Toast.makeText(this, R.string.fp_invalid, Toast.LENGTH_LONG).show();
      return;
    }
    if (!fileName.endsWith(mFilter)) {
      fileName += mFilter;
    }

    try {
      final File f = new File(mCurrentFile, fileName);
      if (f.isDirectory()) {
        initFolder(f);
      } else if (f.exists()) {
        new AlertDialog.Builder(this)
        .setTitle(R.string.fp_overwrite)
        .setMessage(getDisplayName(f))
        .setNegativeButton(R.string.act_no, null)
        .setPositiveButton(R.string.act_yes, new OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            sendResult(f);
          }
        }).show();
      } else if (!f.getParentFile().equals(mCurrentFile)) {
        Toast.makeText(this, R.string.fp_invalid, Toast.LENGTH_LONG).show();
      } else {
        sendResult(f);
      }
    } catch (Throwable e) {
      Debug.log(e);
    }
  }

  public void onFolderClicked(View v) {
    String path = (String) v.getTag(R.layout.file_picker_folderitem);
    initFolder(new File(path));
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    File f = mFilesInList.get(position);
    if (!f.exists()) {
      return;
    }
    if (f.isDirectory()) {
      initFolder(f);
    } else {
      if (mSaveMode) {
        edtInput.setText(f.getName());
      } else {
        sendResult(f);
      }
    }
  }

  private void initFolder(File dir) {
    mCurrentFile = dir;
    File target = Environment.getExternalStorageDirectory();
    txtFpLocation.setText(getDisplayName(dir));

    fpFolders.removeAllViews();
    for (File current = dir; !current.equals(target); current = current.getParentFile()) {
      TextView tv = (TextView) getLayoutInflater().inflate(R.layout.file_picker_folderitem, null);
      tv.setText(current.getName());
      tv.setTag(R.layout.file_picker_folderitem, current.getPath());
      fpFolders.addView(tv, 0);

      ImageView divider = new ImageView(this);
      divider.setImageResource(R.drawable.fp_folder_divider);
      fpFolders.addView(divider, 0);
    }

    TextView tv = (TextView) getLayoutInflater().inflate(R.layout.file_picker_folderitem, null);
    tv.setText(R.string.fp_sdcard);
    tv.setTag(R.layout.file_picker_folderitem, target.getPath());
    tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_sdcard, 0, 0, 0);
    fpFolders.addView(tv, 0);

    fpFolderContainer.postDelayed(new Runnable() {

      @Override
      public void run() {
        fpFolderContainer.scrollTo(fpFolders.getWidth(), 0);
      }
    }, 100);

    mAdapter.clear();
    mFilesInList.clear();

    addFiles(dir, R.drawable.icon_folder_closed, new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });
    addFiles(dir, R.drawable.icon_file, new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return !pathname.isDirectory() && pathname.getName().endsWith(mFilter);
      }
    });
  }

  private void addFiles(File directory, int icon, FileFilter filter) {
    File[] files = directory.listFiles(filter);
    if (files == null) {
      return;
    }
    Arrays.sort(files);
    for (File f : files) {
      mFilesInList.add(f);
      mAdapter.addItem(f.getName(), icon);
    }
  }

  private String getDisplayName(File f) {
    return getString(R.string.fp_sdcard) + f.getPath().substring(Environment.getExternalStorageDirectory().getPath().length());
  }

  @Thunk void sendResult(File file) {
    Intent data = new Intent();
    data.putExtra("file", file.getPath());
    setResult(RESULT_OK, data);
    finish();
  }
}
