package com.painless.pc.util.prefs;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.painless.pc.R;
import com.painless.pc.tracker.MediaButton;
import com.painless.pc.util.ActivityInfo;

public class MediaPickerPref extends AbstractPopupPref {

  private ArrayList<ActivityInfo> mPlayers = null;
  private int mSelected = 0;

  public MediaPickerPref(LayoutInflater inflator, SharedPreferences prefs) {
    super(inflator, prefs, R.string.ts_media_player, R.layout.list_item_single_choice);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    mSelected = which;
    String value = (which > 0) ? mPlayers.get(which - 1).targetIntent.toUri(Intent.URI_INTENT_SCHEME) : "";
    mPrefs.edit().putString(MediaButton.KEY_PLAYER_INTENT, value).commit();
    dialog.dismiss();
  }

  @Override
  public AlertDialog showBuilder(Builder builder) {
    if (mPlayers == null) {
      String currentValue = mPrefs.getString(MediaButton.KEY_PLAYER_INTENT, "");
      mSelected = TextUtils.isEmpty(currentValue) ? 0 : -1;
      add(getContext().getString(R.string.lbl_default));

      int count = 1;
      mPlayers = ActivityInfo.loadReceivers(getContext(), new Intent(Intent.ACTION_MEDIA_BUTTON));
      for (ActivityInfo info : mPlayers) {
        add(info.label);
        if (currentValue.equals(info.targetIntent.toUri(Intent.URI_INTENT_SCHEME))) {
          mSelected = count;
        }
        count++;
      }
    }

    return builder.setPositiveButton("", null).setNegativeButton("", null).setSingleChoiceItems(this, mSelected, this).show();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    CheckedTextView tv = (CheckedTextView) super.getView(position, convertView, parent);
    if (position == 0) {
      tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_bg_trans, 0, 0, 0);
    } else {
      tv.setCompoundDrawablesWithIntrinsicBounds(mPlayers.get(position - 1).icon, null, null, null);
    }
    return tv;
  }
}
