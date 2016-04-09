package com.painless.pc.settings;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.util.UiUtils;

public class TaskerToggleSetup extends Activity implements OnClickListener {

  static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"; //$NON-NLS-1$
  static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"; //$NON-NLS-1$

  private Spinner mNewState;
  private Spinner mToggles;
  private ArrayAdapter<String> mAdapter;
  private EditText mCountText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setResult(RESULT_CANCELED);
    setContentView(R.layout.tasker_toggle_setup);

    mNewState = (Spinner) findViewById(R.id.newState);
    mToggles = (Spinner) findViewById(R.id.toggles);
    mCountText = (EditText) findViewById(R.id.edit_count);

    mAdapter = new ArrayAdapter<String>(this, R.layout.list_item_normal);
    mAdapter.add(getString(R.string.ps_current_toggle));
    for (String tasks : Globals.getTaskerTasks(this)) {
      mAdapter.add(tasks);
    }
    mToggles.setAdapter(mAdapter);
    String oldState = getIntent().getStringExtra("state");
    mNewState.setSelection("true".equals(oldState) ? 1 : ("false".equals(oldState) ? 2 : 0));

    String oldToggle = getIntent().getStringExtra("varID");
    int toggleIndex = mAdapter.getPosition(oldToggle);
    mToggles.setSelection(toggleIndex > 0 ? toggleIndex : 0);

    String countText = getIntent().getStringExtra("count");
    mCountText.setText((countText != null) ? countText : "");
  }

  /**
   * Done clicked
   */
  @Override
  public void onClick(View v) {
    Bundle result = new Bundle();

    int toggle = mToggles.getSelectedItemPosition();
    result.putString("varID", (toggle == 0) ? "%toggle" : mAdapter.getItem(toggle));

    int state = mNewState.getSelectedItemPosition();
    result.putString("state", (state == 0) ? "%state" : ((state == 1) ? "true" : "false"));

    String countText = mCountText.getText().toString();
    result.putString("count", countText.isEmpty() ? "%count" : countText);

    result.putString("net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS", "varID state count");

    String info = String.format(Locale.ENGLISH, "Set %s to %s",
            (toggle == 0) ? "current toggle" : mAdapter.getItem(toggle),
                    new String[] {"new state", "on", "off"}[state]);

    setResult(
            RESULT_OK,
            new Intent()
            .putExtra(EXTRA_STRING_BLURB, info)
            .putExtra(EXTRA_BUNDLE, result)
            );
    finish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return UiUtils.addDoneInMenu(menu, this);
  }
}
