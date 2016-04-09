package com.painless.pc.picker;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

import com.painless.pc.R;
import com.painless.pc.picker.TogglePicker.TogglePickerListener;
import com.painless.pc.singleton.Globals;
import com.painless.pc.tracker.PluginTracker;

public class SimpleTaskerTaskPicker implements OnClickListener {

  private final TogglePickerListener mListener;
  private final List<String> mTasks;

  public SimpleTaskerTaskPicker(TogglePickerListener listener, Context context, List<String> tasks) {
    mListener = listener;
    mTasks = tasks;

    new AlertDialog.Builder(context)
      .setTitle(R.string.ps_tasker_pick_title)
      .setItems(tasks.toArray(new String[tasks.size()]), this)
      .show();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    String task = mTasks.get(which);
    Intent intent = new Intent("net.dinglisch.android.tasker.ACTION_TASK")
        .putExtra("version_number", "1.1").putExtra("task_name", task);
    mListener.onTogglePicked(new PluginTracker(Globals.TASKER_KEY_PREFIX + task, intent, task), null);
  }
}
