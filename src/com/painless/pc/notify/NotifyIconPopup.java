package com.painless.pc.notify;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.painless.pc.R;
import com.painless.pc.util.SectionAdapter;
import com.painless.pc.util.Thunk;

public class NotifyIconPopup extends SectionAdapter {
	@Thunk final SparseIntArray iconIds = new SparseIntArray();

	@Thunk final Context mContext;
	private final Resources mRes;
	private final int mSize;
	@Thunk final String[] mValues;

	@Thunk AlertDialog mDialog;
	@Thunk ListView mList;

	public NotifyIconPopup(Context context) {
		super(context);
		textResource = R.layout.list_item_single_choice;
		this.mContext = context;

		mRes = context.getResources();
		mValues = mRes.getStringArray(R.array.notify_icon_values);
		mSize = (int) Math.ceil(25 * context.getResources().getDisplayMetrics().density);
	}

	public void showDialog(final OnIconSelectListener listener) {
		if (mDialog != null) {
			mDialog.show();
			return;
		}

		final ProgressDialog progressDialog = new ProgressDialog(mContext);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMessage(mContext.getText(R.string.notify_icon_loading));
		progressDialog.setCancelable(false);
		progressDialog.setProgress(0);
		progressDialog.show();

		final int delta = 100 / mValues.length;
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				// Add icons
				addArrayValue(0);
				addArrayValue(1);
				addArrayValue(2);
				addArrayValue(3);

				addHeader(mContext.getString(R.string.notify_icon_sec_battery));
				addArrayValue(4);
				addArrayValue(5);
				addArrayValue(6);
				addArrayValue(7);
				addArrayValue(8);

				addHeader(mContext.getString(R.string.notify_icon_sec_date));
				addArrayValue(9);
				addArrayValue(10);
				addArrayValue(11);
				addArrayValue(12);
				addArrayValue(13);
				return null;
			}

			private void addArrayValue(int pos) {
				iconIds.put(getCount(), pos);
				addItem(mValues[pos], getDrawable(pos));
				publishProgress();
			}

			@Override
			protected void onProgressUpdate(Void... progress) {
				progressDialog.incrementProgressBy(delta);
			}

			@Override
			protected void onPostExecute(Void result) {
				progressDialog.dismiss();
				mList = new ListView(mContext);
				mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				mList.setAdapter(NotifyIconPopup.this);
				mList.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						mDialog.dismiss();
						listener.onIconSelect(getIconId(position));
					}
				});
				mDialog = new AlertDialog.Builder(mContext)
					.setView(mList)
					.setNegativeButton(R.string.act_cancel, null)
					.setTitle(R.string.notify_icon)
					.create();
				mDialog.show();
			}

		}.execute();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		mList.setItemChecked(position, getIconId(position) == NotifyStatus.getIconId(mContext));
		return v;
	}

	public Drawable getDrawable(int iconId) {
		Drawable drawable = mRes.getDrawable(NotifyUtil.getDrawable(iconId));
		drawable.setLevel(NotifyUtil.getIconLevel(iconId, mContext));

		Bitmap tmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(tmp); 
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		Bitmap t2 = Bitmap.createScaledBitmap(tmp, mSize, mSize, true);
		tmp.recycle();
		return new BitmapDrawable(mRes, t2);
	}

	@Thunk int getIconId(int which) {
		return iconIds.get(which);
	}

	public static interface OnIconSelectListener {
		void onIconSelect(int iconId);
	}
}
