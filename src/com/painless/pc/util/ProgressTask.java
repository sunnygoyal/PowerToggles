package com.painless.pc.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class ProgressTask<P, R> extends AsyncTask<P, Void, R> {

	private final ProgressDialog workingDialog;

	public ProgressTask(Context context, CharSequence msg) {
		workingDialog = ProgressDialog.show(context, null, msg, true, false);
	}

	public void onDone(R result) { }

	@Override
	protected final void onPostExecute(R result) {
		workingDialog.dismiss();
		onDone(result);
	}
}
