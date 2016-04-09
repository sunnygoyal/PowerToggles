package com.painless.pc.settings;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.nav.NotifyFrag;
import com.painless.pc.util.ReflectionUtil;

public class LaunchActivity extends PreferenceActivity {

  private List<Header> mHeaders;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if ((getIntent().getComponent() != null)
            && getIntent().getComponent().getClassName().endsWith("NotificationSettings")) {
      getIntent().putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, NotifyFrag.class.getName());
    }
    super.onCreate(savedInstanceState);
    setTitle(getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, R.string.app_name));
  }

  @Override
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.main_headers, target);
    mHeaders = target;
  }

  @Override
  protected boolean isValidFragment(String fragmentName) {
    return true;
  }

  @Override
  public void setListAdapter(ListAdapter adapter) {
    if (adapter == null) {
      super.setListAdapter(adapter);
    } else {
      super.setListAdapter(new HeaderAdapter(this, getMyHeaders()));
    }
  }

  @Override
  public Header onGetInitialHeader() {
    return getMyHeaders().get(1);
  }

  @SuppressWarnings("unchecked")
  private List<Header> getMyHeaders() {
    if (mHeaders == null) {
      mHeaders = (List<Header>) new ReflectionUtil(this, PreferenceActivity.class).invokeGetter("getHeaders");
    }
    if (mHeaders == null) {
      onBuildHeaders(new ArrayList<Header>());
    }
    return mHeaders;
  }

  private static class HeaderAdapter extends ArrayAdapter<Header> {

    private final LayoutInflater mInflater;

    public HeaderAdapter(Context context, List<Header> headers) {
      super(context, R.layout.list_item_header, headers);
      mInflater = LayoutInflater.from(context);
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Header item = getItem(position);
      TextView view = (TextView) ((convertView == null) ? mInflater.inflate(
          (item.fragment == null && item.intent == null)
          ? R.layout.list_item_header : R.layout.list_item_normal, parent, false) : convertView);
      view.setCompoundDrawablesWithIntrinsicBounds(item.iconRes, 0, 0, 0);
      view.setText(item.titleRes);
      return view;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public boolean isEnabled(int position) {
      Header item = getItem(position);
      return item.fragment != null || item.intent != null;
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      return isEnabled(position) ? 1 : 0;
    }
  }
}

