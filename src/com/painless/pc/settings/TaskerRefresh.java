package com.painless.pc.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.painless.pc.R;

public class TaskerRefresh extends Activity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    Bundle extra = new Bundle();
    extra.putBoolean("refresh", true);
    
    setResult(
            RESULT_OK,
            new Intent()
            .putExtra(TaskerToggleSetup.EXTRA_STRING_BLURB, getString(R.string.ps_refresh))
            .putExtra(TaskerToggleSetup.EXTRA_BUNDLE, extra)
            );
    finish();
  }

}
