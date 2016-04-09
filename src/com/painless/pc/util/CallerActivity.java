package com.painless.pc.util;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.painless.pc.R;

public abstract class CallerActivity extends Activity {

  private final SparseArray<ResultReceiver> receivers = new SparseArray<ResultReceiver>();
  private final ArrayList<Dialog> mBoundDialogs = new ArrayList<>();

  @Override
  protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      ResultReceiver receiver = receivers.get(requestCode);
      receivers.remove(requestCode);
      
      if (receiver != null) {
        receiver.onResult(requestCode, data);
      }
    }
  }

  public void registerDialog(Dialog dialog) {
    mBoundDialogs.add(dialog);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    for (Dialog d : mBoundDialogs) {
      d.dismiss();
    }
  }

  public final void requestResult(int requestCode, Intent intent, CallerActivity.ResultReceiver callback) {
    receivers.put(requestCode, callback);
    startActivityForResult(intent, requestCode);
  }

  public static interface ResultReceiver {
  	void onResult(int requestCode, Intent data);
  }

  public abstract void onDoneClicked();

  /**
   * Utility method to add a done button in the actionbar.
   */
  protected void addActionDoneButton() {
    // Inflate a "Done" custom action bar view to serve as the "Up" affordance.
    final ActionBar actionBar = getActionBar();
    final LayoutInflater inflater = (LayoutInflater) actionBar.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
    final View customActionBarView = inflater.inflate(R.layout.ab_done_bar, null);
    customActionBarView.findViewById(R.id.done).setOnClickListener(new OnClickListener() {
      
      @Override
      public void onClick(View v) {
        onDoneClicked();
      }
    });
    // Show the custom action bar view and hide the normal Home icon and title.
    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    actionBar.setCustomView(customActionBarView);
  }
}
