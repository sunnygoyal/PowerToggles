package com.painless.pc.nav;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.cfg.WidgetConfigActivity;
import com.painless.pc.notify.NotifyIconPopup;
import com.painless.pc.notify.NotifyIconPopup.OnIconSelectListener;
import com.painless.pc.notify.NotifyStatus;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.util.Thunk;

public class NotifyFrag extends Fragment implements OnCheckedChangeListener, OnIconSelectListener, OnClickListener {

  @Thunk SharedPreferences mPrefs;

  private View mRoot;
  private Switch mEnableButton;
  private NotifyIconPopup mIconPopup;
  private TextView mNotifyIconButton;

  private CheckedTextView mAutoCollapse;
  private CheckedTextView mVisibility;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mRoot = inflater.inflate(R.layout.nav_notify, container, false);
    mPrefs = Globals.getAppPrefs(getActivity());

    Switch twoRowButton = (Switch) mRoot.findViewById(R.id.notify_two_row);
    twoRowButton.setChecked(NotifyStatus.isTwoRowEnabled(getActivity()));
    twoRowButton.setOnCheckedChangeListener(this);

    mIconPopup = new NotifyIconPopup(getActivity());

    mNotifyIconButton = (TextView) mRoot.findViewById(R.id.notify_icon);
    mNotifyIconButton.setCompoundDrawablesWithIntrinsicBounds(
        mIconPopup.getDrawable(NotifyStatus.getIconId(getActivity())),
        null,
        getResources().getDrawable(R.drawable.icon_expand),
        null);
    mNotifyIconButton.setOnClickListener(this);

    mAutoCollapse = (CheckedTextView) mRoot.findViewById(R.id.chk_auto_collapse);
    mAutoCollapse.setChecked(NotifyStatus.autoCollaseNotify(getActivity()));

    mRoot.findViewById(R.id.btn_customize).setOnClickListener(this);
    mRoot.findViewById(R.id.btn_customize2).setOnClickListener(this);
    mRoot.findViewById(R.id.btn_priority).setOnClickListener(this);
    mRoot.findViewById(R.id.btn_auto_collapse).setOnClickListener(this);

    View btnVisibility = mRoot.findViewById(R.id.btn_show_on_lock_screen);
    if (Globals.IS_LOLLIPOP) {
      btnVisibility.setOnClickListener(this);
      mVisibility = (CheckedTextView) mRoot.findViewById(R.id.chk_show_on_lock_screen);
      mVisibility.setChecked(NotifyStatus.notifyVisibility(getActivity()) == Notification.VISIBILITY_PUBLIC);

      mRoot.findViewById(R.id.btn_show_on_lock_screen_help).setOnClickListener(this);
    } else {
      ((View) btnVisibility.getParent()).setVisibility(View.GONE);
    }
    updatePriorityIcons();
    return mRoot;
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView == mEnableButton) {
      NotifyStatus.setEnabled(getActivity(), isChecked);
      PCWidgetActivity.partialUpdateAllWidgets(getActivity());
    } else {
      NotifyStatus.setTwoRowEnabled(getActivity(), isChecked);
      if (mEnableButton.isChecked()) {
        PCWidgetActivity.partialUpdateAllWidgets(getActivity());
      }
    }
  }

  /**
   * Called when notification icon changes
   */
  @Override
  public void onIconSelect(int iconID) {
    NotifyStatus.setIconId(iconID, getActivity());
    refreshNotification();
    mNotifyIconButton.setCompoundDrawablesWithIntrinsicBounds(
        mIconPopup.getDrawable(NotifyStatus.getIconId(getActivity())),
        null,
        getResources().getDrawable(R.drawable.icon_expand),
        null);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.notify_settings_menu, menu);
    MenuItem searchItem = menu.findItem(R.id.notify_switch);
    mEnableButton = (Switch) searchItem.getActionView().findViewById(R.id.my_switch);
    mEnableButton.setChecked(NotifyStatus.isEnabled(getActivity()));
    mEnableButton.setOnCheckedChangeListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_auto_collapse:
        NotifyStatus.setAutoCollapseNotify(getActivity(), !mAutoCollapse.isChecked());
        mAutoCollapse.setChecked(NotifyStatus.autoCollaseNotify(getActivity()));
        break;
      case R.id.notify_icon:
        mIconPopup.showDialog(this);
        break;
      case R.id.btn_customize:
      case R.id.btn_customize2:
        try {
          String tag = (String) v.getTag();
          startActivity(new Intent(getActivity(), WidgetConfigActivity.class).putExtra("edit_widget", -Integer.valueOf(tag)));
        } catch (Throwable e) {
          Debug.log(e);
        }
        break;
      case R.id.btn_priority: {
        int priority = mPrefs.getInt(Globals.NOTIFICATION_PRIORITY, 5);

        int selected = (priority > 2) ? ((priority > 4) ? 0 : 1) : 2;
        new AlertDialog.Builder(getActivity())
        .setTitle(R.string.notify_position_title)
        .setSingleChoiceItems(R.array.notify_positions, selected, new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            int priority = (which == 0) ? 5 : ((which == 1) ? 3 : 1);
            mPrefs.edit().putInt(Globals.NOTIFICATION_PRIORITY, priority).commit();
            refreshNotification();
            dialog.dismiss();
            updatePriorityIcons();
          }
        }).setNegativeButton(R.string.act_cancel, null)
        .show();
        break;
      }
      case R.id.btn_show_on_lock_screen:
        mVisibility.setChecked(!mVisibility.isChecked());
        NotifyStatus.setNotifyVisibility(getActivity(), mVisibility.isChecked() ? Notification.VISIBILITY_PUBLIC : Notification.VISIBILITY_SECRET);
        refreshNotification();
        break;
      case R.id.btn_show_on_lock_screen_help:
        startActivity(InfoFrag.getHelpIntent(37));
        break;
    }
  }

  @Thunk void refreshNotification() {
    if (NotifyStatus.isEnabled(getActivity())) {
      ((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Globals.NOTIFICATION_ID);
      PCWidgetActivity.partialUpdateAllWidgets(getActivity());
    }
  }

  @Thunk void updatePriorityIcons() {
    int priority = mPrefs.getInt(Globals.NOTIFICATION_PRIORITY, 5);
    ((ImageView) mRoot.findViewById(R.id.notify_status_1)).setImageResource(priority> 2 ? R.drawable.status_icon : R.drawable.status_no_icon);
    ((ImageView) mRoot.findViewById(R.id.notify_status_2)).setImageResource(priority==5 ? R.drawable.status_pin : R.drawable.status_no_pin);
  }
}
