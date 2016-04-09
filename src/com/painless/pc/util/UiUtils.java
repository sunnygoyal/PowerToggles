package com.painless.pc.util;

import android.graphics.Point;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.painless.pc.R;

public class UiUtils {

  public static boolean addDoneInMenu(Menu menu, OnClickListener doneClickListener) {
    return addMenuBtn(R.layout.ab_done_menu, R.string.act_done, menu, doneClickListener);
  }

  public static boolean addMenuBtn(int layout, int string, Menu menu, OnClickListener doneClickListener) {
    MenuItem item = menu.add(string);
    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    item.setActionView(layout);
    item.getActionView().findViewById(R.id.done).setOnClickListener(doneClickListener);
    return true;
  }

  public static Point getDisplaySize(WindowManager wm) {
    Point size = new Point();
    wm.getDefaultDisplay().getSize(size);
    return size;
  }

  public static void setCheckbox(boolean isChecked, CompoundButton chk, OnCheckedChangeListener listener) {
    chk.setOnCheckedChangeListener(null);
    chk.setChecked(isChecked);
    chk.setOnCheckedChangeListener(listener);
  }
}
