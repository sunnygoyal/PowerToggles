package com.painless.pc.view;

import java.util.Locale;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;

import com.painless.pc.R;
import com.painless.pc.graphic.ColorIndicatorDrawable;
import com.painless.pc.util.Thunk;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;

public class ColorButton extends ImageView implements OnClickListener, OnColorChangedListener, TextWatcher {

	private final ColorIndicatorDrawable mDrawable;

	private Dialog mColorDialog;
	private OnColorChangedListener mListener;
	@Thunk ColorPickerView mPicker;

	private EditText mColorCode;
	private int mAlphaMsgId = R.string.cb_alpha;

	public ColorButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mDrawable = new ColorIndicatorDrawable(context);
		setImageDrawable(mDrawable);
	}

	public void setColor(int color) {
		setColor(color, false);
	}

	public void setColor(int color, boolean callback) {
		mDrawable.setColor(color);
		invalidate();
		if (callback && (mListener != null)) {
			mListener.onColorChanged(color, this);
		}
	}

	public int getColor() {
		return mDrawable.getColor();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mDrawable.setEnabled(enabled);
		invalidate();
	}

	public void setOnColorChangeListener(OnColorChangedListener listener, boolean addHandler) {
		if (listener != this) {
			mListener = listener;
		}
		if (addHandler) {
			setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		if (mColorDialog == null) {
			View content = LayoutInflater.from(getContext()).inflate(R.layout.color_picker, null);

			mPicker = (ColorPickerView) content.findViewById(R.id.color_picker_view);
			mPicker.setAlphaSliderText(mAlphaMsgId);
			mPicker.setOnColorChangedListener(this);

			mColorCode = (EditText) content.findViewById(R.id.txt_color_input);
			mColorCode.addTextChangedListener(this);

			final DialogInterface.OnClickListener dialogClick = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE)  {
						setColor(mPicker.getColor(), true);
					}
					dialog.dismiss();
				}
			};

			mColorDialog = new AlertDialog.Builder(getContext())
				.setPositiveButton(R.string.cb_set_color, dialogClick)
				.setNegativeButton(R.string.act_cancel, dialogClick)
				.setTitle(getContentDescription())
				.setView(content).create();
		}
		mPicker.setColor(getColor());
		onColorChanged(getColor(), mPicker);
		mColorDialog.show();
	}

	@Override
	public void onColorChanged(int color, View v) {
		mColorCode.setText(
				hexCode(Color.alpha(color)) +
				hexCode(Color.red(color)) +
				hexCode(Color.green(color)) +
				hexCode(Color.blue(color)));
	}

	public void setAlphaMsg(int msgId) {
		mAlphaMsgId = msgId;
	}
	
	private static final String CODE_PATTERN = "^[0-9a-fA-F]{8}$";
	@Override
	public void afterTextChanged(Editable s) {
		if (!mColorCode.hasFocus()) {
			return;
		}
		String code = s.toString();
		if (Pattern.matches(CODE_PATTERN, code)) {
			mPicker.setColor(Color.argb(parse(code, 0), parse(code, 2), parse(code, 4), parse(code, 6)));
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	private static int parse(String s, int start) {
		return Integer.parseInt(s.substring(start, start + 2), 16);
	}

	private static String hexCode(int code) {
		String s = Integer.toHexString(code);
		if (s.length() < 2) {
			s = "0" + s;
		}
		return s.toUpperCase(Locale.ENGLISH);
	}
}
