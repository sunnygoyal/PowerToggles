package com.painless.pc.picker;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.painless.pc.R;
import com.painless.pc.picker.TogglePicker.TogglePickerListener;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Globals;
import com.painless.pc.theme.BatteryImageProvider;
import com.painless.pc.tracker.PluginTracker;
import com.painless.pc.util.Thunk;

public class TaskerTogglePicker implements OnItemSelectedListener, TextWatcher, OnItemClickListener {

  private static final int TASKER_COUNTER_CONFIG = 0xffffbbaf;

  private final TogglePickerListener mListener;
  private final Context mContext;
  private final ArrayAdapter<String> mTaskAdapter;

  private final Spinner mTypePicker;
  @Thunk final EditText mIconCount;
  private final AlertDialog mDialog;

  public TaskerTogglePicker(TogglePickerListener listener, Context context, List<String> tasks) {
    mListener = listener;
    mContext = context;
    mTaskAdapter = new ArrayAdapter<String>(mContext, R.layout.list_item_normal, tasks);

    View content = View.inflate(mContext, R.layout.tasker_task_picker, null);

    mIconCount = (EditText) content.findViewById(R.id.edt_count);
    mIconCount.setVisibility(View.INVISIBLE);
    mIconCount.addTextChangedListener(this);
    mTypePicker = (Spinner) content.findViewById(R.id.spinner_type);
    mTypePicker.setOnItemSelectedListener(this);

    ListView taskList = (ListView) content.findViewById(R.id.lst_task);
    taskList.setAdapter(mTaskAdapter);
    taskList.setOnItemClickListener(this);

    mDialog = new AlertDialog.Builder(mContext)
      .setTitle(R.string.tp_tasker)
      .setView(content)
      .show();
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    if (position == 1) {
      mIconCount.setVisibility(View.VISIBLE);
      mIconCount.post(new Runnable() {
        
        @Override
        public void run() {
          mIconCount.requestFocus();
        }
      });
    } else {
      mIconCount.setVisibility(View.INVISIBLE);
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) { }

  @Override
  public void afterTextChanged(Editable s) {
    if (s.length() > 0) {
      try {
        int value = Integer.parseInt(s.toString());
        if (value < 2 || value > 9) {
          s.clear();
        }
      } catch (Exception e) {
        s.clear();
      }
    }
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

  @Override
  public void onTextChanged(CharSequence s, int start, int count, int after) { }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    mDialog.dismiss();
    
    String task = mTaskAdapter.getItem(position);
    Intent intent = new Intent("net.dinglisch.android.tasker.ACTION_TASK")
        .putExtra("version_number", "1.1").putExtra("task_name", task);

    Bitmap mainIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_tasker);
    Bitmap toggleIcon;
    switch (mTypePicker.getSelectedItemPosition()) {
      case 1:
        // Multi icon
        int count = 2;
        try {
          count = Integer.parseInt(mIconCount.getText().toString());
          count = Math.min(9, Math.max(count, 2));
        } catch (Exception e) { }
        toggleIcon = Bitmap.createBitmap(mainIcon.getWidth() * count, mainIcon.getWidth(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(toggleIcon);
        BatteryImageProvider provider = new BatteryImageProvider(mainIcon, mContext, TASKER_COUNTER_CONFIG);
        while (count > 0) {
          count --;
          c.drawBitmap(provider.getIcon(count), mainIcon.getWidth() * count, 0, null);
        }
        mainIcon.recycle();
        break;
      case 2:
        // Counter type
        toggleIcon = BitmapUtils.createBatteryBitmapFormat(mainIcon, TASKER_COUNTER_CONFIG, mContext);
        mainIcon.recycle();
        break;
      default:
        toggleIcon = mainIcon;
    }
    mListener.onTogglePicked(new PluginTracker(Globals.TASKER_KEY_PREFIX + task, intent, task), toggleIcon);
  }
}
