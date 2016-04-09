package com.painless.pc.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.singleton.Debug;

public class SeekNumberPicker implements OnSeekBarChangeListener, TextWatcher {

	private final View main;

	private final SeekBar mSeek;
	protected final EditText mValuePreview;

	public SeekNumberPicker(Context context) {
		main = LayoutInflater.from(context).inflate(R.layout.number_picker, null);

		mSeek = (SeekBar) main.findViewById(R.id.seekBar);
		mSeek.setOnSeekBarChangeListener(this);
		mValuePreview = (EditText) main.findViewById(R.id.valuePreview);
		mValuePreview.addTextChangedListener(this);
	}

	public void setSummary(int res) {
		((TextView) main.findViewById(R.id.valueInfo)).setText(res);
	}

	public View getView() {
		return main;
	}

	public void setMax(int max) {
		mSeek.setMax(max);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			String value = String.valueOf(progress);
			mValuePreview.setText(value);
			mValuePreview.setHint(value);
		}
	}

	public int getValue() {
		return mSeek.getProgress();
	}
	
	public void setValue(int value) {
		mSeek.setProgress(value);
		onProgressChanged(mSeek, value, true);
	}

	@Override
	public void afterTextChanged(Editable s) {
		try {
			int value = Integer.parseInt(s.toString());
			if ((value >= 0) && (value <= mSeek.getMax())) {
				mSeek.setProgress(value);
			}
		} catch (Exception e) {
			Debug.log(e);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
}
