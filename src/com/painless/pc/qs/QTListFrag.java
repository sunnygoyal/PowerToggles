package com.painless.pc.qs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;

import com.painless.pc.R;
import com.painless.pc.nav.AbsListFrag;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.WidgetDB;
import com.painless.pc.util.SectionAdapter;
import com.painless.pc.util.SectionAdapter.SectionItem;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.UiUtils;

@TargetApi(Build.VERSION_CODES.M)
public class QTListFrag extends AbsListFrag implements OnClickListener {
  private static final String TILE_PREFIX = "intent(";

  private boolean mIsTunerEnabled;
  @Thunk SectionAdapter mAdapter;
  @Thunk Context mContext;
  @Thunk VideoView mVV;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mIsTunerEnabled = !getContext().getPackageManager().queryIntentActivities(tunerIntent(), 0).isEmpty();
    setHasOptionsMenu(mIsTunerEnabled);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mContext = view.getContext();
    if (mIsTunerEnabled) {
      setEmptyMsg(R.string.qs_empty_help_1, R.string.qs_empty_help_2);
      loadItems();
    } else {
      ((TextView) getView().findViewById(android.R.id.text1)).setText(
          joinMsg(R.string.qs_activate_help_1, R.string.qs_activate_help_2, R.string.qs_activate_help_3));

      String fileName = "android.resource://"+  mContext.getPackageName() + "/" + R.raw.qs_guide;
      mVV = (VideoView) getView().findViewById(R.id.videoView);
      mVV.setVideoURI(Uri.parse(fileName));
      mVV.setOnPreparedListener(new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
          mp.setLooping(true);
        }
      });
      mVV.setOnErrorListener(new OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
          mVV.setVisibility(View.GONE);
          return true;
        }
      });
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (mIsTunerEnabled) {
      return super.onCreateView(inflater, container, savedInstanceState);
    } else {
      return inflater.inflate(R.layout.qs_activate, container, false);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mIsTunerEnabled) {
      loadItems();
    } else {
      mVV.start();
    }
  }

  @Override
  public void onListItemClick(ListView list, View v, int position, long id) {
    SectionItem item = mAdapter.getItem(position);
    mContext.startActivity(new Intent(mContext, TileConfigActivity.class)
      .putExtra(TileConfigActivity.EXTRA_KEY, item.label));
  }

  private void loadItems() {
    final Map<String, QTInfo> items = QTStorage.getAllTiles(mContext);
    new AsyncTask<Void, Void, List<SectionItem>>() {

      @Override
      protected List<SectionItem> doInBackground(Void... args) {
        List<SectionItem> result = new ArrayList<>();
        for (Map.Entry<String, QTInfo> entry : items.entrySet()) {
          byte[] iconArray = WidgetDB.get(mContext).getIconBytes(entry.getValue().widgetId << 3);
          Bitmap icon;
          if (iconArray == null) {
            icon = BitmapUtils.cropMaxVisibleBitmap(mContext.getDrawable(R.drawable.icon_quick_tile),
                getResources().getDimensionPixelSize(R.dimen.qs_tile_icon_size));
          } else {
            icon = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
          }
          result.add(new SectionItem(entry.getKey(), new BitmapDrawable(getResources(), icon)));
        }
        return result;
      }

      @Override
      protected void onPostExecute(List<SectionItem> result) {
        mAdapter = new SectionAdapter(mContext);
        mAdapter.addAll(result);
        setListAdapter(mAdapter);
      }
    }.execute();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    UiUtils.addMenuBtn(R.layout.ab_add_menu, R.string.act_add, menu, this);
  }

  @Override
  public void onClick(View view) {
    // Find available broadcast tiles.
    Set<String> availableTiles = getAvailableTiles();
    if (availableTiles.isEmpty()) {
      showTileDialog();
      return;
    }

    final CharSequence[] tiles = new CharSequence[availableTiles.size()];
    int i = 0;
    for (String tile : availableTiles) {
      tiles[i++] = tile;
    }
    new AlertDialog.Builder(mContext)
        .setTitle(R.string.qs_pick_tile)
        .setItems(tiles, new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            mContext.startActivity(new Intent(mContext, TileConfigActivity.class)
            .putExtra(TileConfigActivity.EXTRA_KEY, tiles[which]));
          }
        }).setNeutralButton(R.string.qs_manual_title, new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            showTileDialog();
          }
        }).show();
  }

  @Thunk void showTileDialog() {
    new AlertDialog.Builder(mContext)
      .setMessage(joinMsg(R.string.qs_manual_1, R.string.qs_manual_2, R.string.qs_manual_3, R.string.qs_manual_4, R.string.qs_manual_5))
      .setTitle(R.string.qs_manual_title)
      .setPositiveButton(R.string.qs_button_tuner, new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          mContext.startActivity(tunerIntent());
        }
      }).show();
  }

  private Set<String> getAvailableTiles() {
    String tileConfig = Settings.Secure.getString(mContext.getContentResolver(), "sysui_qs_tiles");
    if (TextUtils.isEmpty(tileConfig)) {
      return Collections.emptySet();
    }
    Set<String> result = new HashSet<>();
    for (String tile : tileConfig.split(",")) {
      tile = tile.trim();
      if (tile.isEmpty() || !tile.startsWith(TILE_PREFIX) || !tile.endsWith(")")) continue;
      final String action = tile.substring(TILE_PREFIX.length(), tile.length() - 1);
      result.add(action);
    }

    result.removeAll(QTStorage.getAllTiles(mContext).keySet());
    return result;
  }

  private String joinMsg(int... msgIds) {
    String msg = "";
    int i = 1;
    for (int msgId : msgIds) {
      if (!TextUtils.isEmpty(msg)) {
        msg += "\n";
      }
      msg += (i++) + ".  " + mContext.getString(msgId);
    }
    return msg;
  }

  @Thunk static Intent tunerIntent() {
    return new Intent().setComponent(new ComponentName("com.android.systemui", "com.android.systemui.tuner.TunerActivity"));
  }
}
