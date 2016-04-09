package com.painless.pc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.painless.pc.theme.RVFactory;

public class ProxyActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri data = getIntent().getData();
		if (data != null) {
		  sendBroadcast(RVFactory.makeIntent(this, data));
		} else {
		  Intent proxy = getIntent().getParcelableExtra("proxy");
		  if (proxy != null) {
		    startActivity(proxy);
		  }
		}
		finish();
	}
}
