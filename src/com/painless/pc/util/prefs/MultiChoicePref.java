package com.painless.pc.util.prefs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.painless.pc.R;
import com.painless.pc.singleton.ParseUtil;

public class MultiChoicePref extends AbstractPopupPref implements OnItemClickListener {

	private final TypedArray mIcons;
	private final String mKey;
	private final int mMinCount;

	private final boolean[] mCheckedItems = new boolean[6];
	private int mCheckedCount = 0;

	private ListView mListView;

	public MultiChoicePref(LayoutInflater inflator, SharedPreferences prefs, int itemArray, int iconArray,
			String key, int title, int minCount) {
		super(inflator, prefs, title, R.layout.list_item_multi_choice);

		mIcons = inflator.getContext().getResources().obtainTypedArray(iconArray);
		mKey = key;
		mMinCount = minCount;

		for (String item : inflator.getContext().getResources().getStringArray(itemArray)) {
			add(item);
		}
	}

	@Override
	public AlertDialog showBuilder(Builder builder) {
		mCheckedCount = ParseUtil.parseBoolArray(mCheckedItems, mPrefs.getInt(mKey, 7));
		
		mListView = new ListView(getContext());
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mListView.setAdapter(this);
		mListView.setOnItemClickListener(this);

		return builder.setView(mListView).show();
	}

	/**
	 * OK clicked
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			int value = 0;
			for (int i=0; i< 6; i++) {
				if (mCheckedItems[i]) {
					value += 1 << i;
				}
			}
			mPrefs.edit().putInt(mKey, value).commit();
		}
		mIcons.recycle();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		boolean isChecked = !mCheckedItems[position];
		mCheckedItems[position] = isChecked;
		mCheckedCount += isChecked ? 1 : -1;

		mOKButton.setEnabled(mCheckedCount >= mMinCount);
		mListView.setItemChecked(position, isChecked);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CheckedTextView tv = (CheckedTextView) super.getView(position, convertView, parent);
		tv.setCompoundDrawablesWithIntrinsicBounds(mIcons.getDrawable(position), null, null, null);
		mListView.setItemChecked(position, mCheckedItems[position]);
		return tv;
	}
}
