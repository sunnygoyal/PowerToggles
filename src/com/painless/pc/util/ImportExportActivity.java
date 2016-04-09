package com.painless.pc.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.picker.FilePicker;
import com.painless.pc.singleton.Debug;

/**
 * An abstract activity with utilities functions to start new activities and direct the result accordingly.
 * This activity also abstract outs some out the import/export functionality.
 * 
 * @param <T> Import settings type
 */
public abstract class ImportExportActivity<T> extends CallerActivity {

	private final int menuId;

	private final String fileExtension;
	private final int exportMsgTitle;
	private final int exportMsgArray;

	private final int importMsgTitle;
	private final int importMsgArray;

	public ImportExportActivity(int menuId, String fileExtension, int exportMsgTitle, int exportMsgArray,
			int importMsgTitle, int importMsgArray) {
		this.menuId = menuId;
		this.fileExtension = fileExtension;
		this.exportMsgTitle = exportMsgTitle;
		this.exportMsgArray = exportMsgArray;
		this.importMsgTitle = importMsgTitle;
		this.importMsgArray = importMsgArray;
	}

	// ************************ Options menu ************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(menuId, menu);
		for (int i=menu.size()-1; i >=0; i--) {
			MenuItem item = menu.getItem(i);
			Drawable d = item.getIcon();
			if (d != null) {
				d = d.getConstantState().newDrawable(getResources());
				d.setAlpha(204);
				item.setIcon(d);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.cfg_export) {
			Intent intent = new Intent(this, FilePicker.class)
				.putExtra("savemode", true)
				.putExtra("title", getText(exportMsgTitle))
				.putExtra("filter", fileExtension);
			requestResult(10, intent, new CallerActivity.ResultReceiver() {
				
				@Override
				public void onResult(int requestCode, Intent data) {
					startExportingInternal(data.getStringExtra("file"));	
				}
			});
		} else if (item.getItemId() == R.id.cfg_import) {
			Intent intent = new Intent(this, FilePicker.class)
				.putExtra("savemode", false)
				.putExtra("title", getText(importMsgTitle))
				.putExtra("filter", fileExtension);
			requestResult(10, intent, new CallerActivity.ResultReceiver() {

				@Override
				public void onResult(int requestCode, Intent data) {
					startImportInternal(data.getStringExtra("file"));	
				}
			});
		}
		return true;
	}

	@Thunk void startExportingInternal(final String filePath) {
		final String[] exportArray = getResources().getStringArray(exportMsgArray);

		new ProgressTask<String, File>(this, exportArray[0]) {

			@Override
			protected File doInBackground(String... params) {
				try {
					File exportFile = new File(filePath);
					doExport(new FileOutputStream(exportFile));
					
					return exportFile;
				} catch (Exception e) {
					Debug.log(e);
					return null;
				}
			}

			@Override
			public void onDone(File result) {
				String msg = (result == null) ? exportArray[2] :
					String.format(exportArray[1], result.getAbsolutePath());
				Toast.makeText(ImportExportActivity.this, msg, Toast.LENGTH_LONG).show();

				// Ask media scanner to scan the newly created file.
				if (result != null) {
					try {
						Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						intent.setData(Uri.fromFile(result));
						sendBroadcast(intent);
					} catch (Exception e) {
						Debug.log(e);
					}
				}
			}
		}.execute();
	}

	public abstract void doExport(OutputStream out) throws Exception;
	

	// ************************ Import ************************
	@Thunk void startImportInternal(final String filePath) {
		final String[] importArray = getResources().getStringArray(importMsgArray);
		new ProgressTask<Void, T>(this, importArray[0]) {

			@Override
			protected T doInBackground(Void... params) {
				try {
					return doImportInBackground(new File(filePath));
				} catch (Exception e) {
					Debug.log(e);
				}
				return null;
			}

			@Override
			public void onDone(T result) {
				if (result == null) {
					Toast.makeText(ImportExportActivity.this, importArray[1], Toast.LENGTH_LONG).show();
				} else {
					onPostImport(result);
				}
			}
		}.execute();
		
	}

	public abstract T doImportInBackground(File importFile) throws Exception;
	public abstract void onPostImport(T result);

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		maybeShowAnimation();
	}

	public void maybeShowAnimation() {
		if (!isTaskRoot()) {
			overridePendingTransition(R.anim.left_slide_in, R.anim.right_slide_out);
		}
	}
}
