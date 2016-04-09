package com.painless.pc.util.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;

import com.painless.pc.R;
import com.painless.pc.util.PathDrawable;

abstract class AbstractPopupPref extends ArrayAdapter<String> implements OnClickListener, DialogInterface.OnClickListener {

  private static final String PATH_POPUP = "M13.1,16.5 L12.1,16.5 12.1,36.5 32,36.5 32,35.5 13.1,35.5 13.1,16.5z M36,11.5 L15.5,11.5 15.5,32 36,32 36,11.5z M35,31 L16.5,31 16.5,16.6 35,16.6 35,31z";

	public final CheckedTextView view;
	
	protected final SharedPreferences mPrefs;
	protected final LayoutInflater mInflator;

	protected Button mOKButton;

	AbstractPopupPref(LayoutInflater inflator, SharedPreferences prefs, int textId, int itemLayout) {
		super(inflator.getContext(), itemLayout);
		mPrefs = prefs;
		view = (CheckedTextView) inflator.inflate(R.layout.ts_pref_checkbox, null);
		view.setCheckMarkDrawable(new PathDrawable(PATH_POPUP, getContext(), 18));
		view.setText(textId);
		view.setOnClickListener(this);

		mInflator = inflator;
	}

	@Override
	public final void onClick(View v) {
		AlertDialog dialog = showBuilder(new AlertDialog.Builder(mInflator.getContext())
			.setTitle(view.getText())
			.setNegativeButton(R.string.act_cancel, null)
			.setPositiveButton(R.string.act_ok, this));

		mOKButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
	}

	public abstract AlertDialog showBuilder(AlertDialog.Builder builder); 
}
