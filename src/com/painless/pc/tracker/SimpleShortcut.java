package com.painless.pc.tracker;

import android.content.Intent;

import com.painless.pc.R;

public class SimpleShortcut extends AbstractCommand {

	private final String label;

	private final Intent launchIntent;
	private  String id;

	public SimpleShortcut(Intent intent, String label) {
		super(-1, null, R.drawable.icon_application);
		this.label = label;

		// Replace call privileged action.
		if ("android.intent.action.CALL_PRIVILEGED".equals(intent.getAction())) {
			intent.setAction(Intent.ACTION_CALL);
		}

		this.launchIntent = intent;
		this.id = "ss_";
	}

	@Override
	public String getLabel(String[] labelArray) {
		return label;
	}

	@Override
	public Intent getIntent() {
		return launchIntent;
	}

	public final void setId(int id) {
		this.id = "ss_" + id;
	}
	
	public final void setId(String id) {
		this.id = "ss_" + id;
	}

	@Override
	public String getId() {
		return id;
	}

	public static int getId(int widgetID, int pos) {
		int id = widgetID << 3;
		return id + pos;
	}
}
