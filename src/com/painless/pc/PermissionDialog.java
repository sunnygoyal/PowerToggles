package com.painless.pc;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CheckBox;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.painless.pc.singleton.Globals;

public class PermissionDialog extends AlertActivity implements OnClickListener {

  private Intent mTargetIntent;

  @Override
  protected void onCreate(Bundle args) {
    super.onCreate(args);

    final AlertController.AlertParams p = mAlertParams;
    p.mViewLayoutResId = R.layout.permission_prompt;
    p.mTitle = getText(R.string.pp_title);

    p.mPositiveButtonText = getText(R.string.lbl_settings);
    p.mPositiveButtonListener = this;
    p.mNegativeButtonText = getText(R.string.pp_ignore);
    p.mNegativeButtonListener = this;
    setupAlert();

    mTargetIntent = getIntent().getParcelableExtra("target");
  }

  @TargetApi(23)
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == DialogInterface.BUTTON_POSITIVE) {
      startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .setData(Uri.parse("package:" + getPackageName())));
    } else {
      startActivity(mTargetIntent);
    }

    if (((CheckBox) findViewById(R.id.chk_never)).isChecked()) {
      Globals.getAppPrefs(this).edit().putBoolean("prompt_permission", true).apply();
    }

    finish();
  }

}
