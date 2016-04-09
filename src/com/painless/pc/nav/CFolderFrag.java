package com.painless.pc.nav;

import static com.painless.pc.util.SettingsDecoder.KEY_PADDING;
import static com.painless.pc.util.SettingsDecoder.KEY_STRETCH;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.painless.pc.R;
import com.painless.pc.cfg.NinePatchEditor;
import com.painless.pc.cfg.section.BackgroundSection;
import com.painless.pc.cfg.section.ConfigSection;
import com.painless.pc.folder.FolderDb;
import com.painless.pc.folder.FolderPick;
import com.painless.pc.folder.FolderUtils;
import com.painless.pc.picker.ThemePicker;
import com.painless.pc.settings.LaunchActivity;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.theme.LabelRVFactory;
import com.painless.pc.util.ImageLoadTask.LargeImageLoadTask;
import com.painless.pc.util.SettingsDecoder;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.UiUtils;
import com.painless.pc.view.ColorButton;
import com.painless.pc.view.ColorPickerView.OnColorChangedListener;
import com.painless.pc.view.NinePatchView;

public class CFolderFrag extends Fragment implements OnColorChangedListener, OnCheckedChangeListener, OnClickListener {

  private static final int REQUEST_IMAGE = 11;
  private static final int REQUEST_9PATCH = 12;
  private static final int REQUEST_THEMES = 10;

  private String mFolderId;

  @Thunk ColorButton[] mColorButtons;
  private FolderDb mDb;

  private Context mContext;
  private TextView mTitleText;

  private NinePatchView mFolderPreview;

  @Thunk ImageView mPreviewImg;
  @Thunk TextView mPreviewLabel;
  @Thunk String[] mStateArray;
  private Handler mPreviewclickHandler;
  private PreviewSetter mSetterOn, mSetterOff;
  private boolean mPreviewOn = false;

  private CheckBox mLabelHidden;
  private ColorButton mLabelColor;

  private RadioButton mBtnBackDefault;
  @Thunk RadioButton mBtnBackImage;
  private View mPositionButton;
  
  @Thunk Bitmap mBackImage;
  @Thunk int[] mBackStretch = new int[4];
  @Thunk int[] mBackPadding = new int[4];

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    if (getArguments() != null) {
      mFolderId = getArguments().getString("id");
    }
    mContext = getActivity();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mContext = inflater.getContext();

    View root = inflater.inflate(R.layout.folder_config, container, false);
    mTitleText = (TextView) root.findViewById(R.id.edt_input);
    
    SettingsDecoder settings;
    Bitmap background = null;
    if (TextUtils.isEmpty(mFolderId)) {
      settings = new SettingsDecoder(FolderUtils.DEFAULT_SETTINGS);
    } else {
      mTitleText.setText(mContext.getSharedPreferences(FolderUtils.PREFS, Context.MODE_PRIVATE).getString(FolderUtils.KEY_NAME_PREFIX + mFolderId, ""));
      mDb = new FolderDb(mContext, mFolderId);
      background = mDb.getIcon(FolderDb.SETTING_POS);
      settings = new SettingsDecoder(mDb.getSettings());
    }

    mFolderPreview = (NinePatchView) root.findViewById(R.id.folder_preview);
    mFolderPreview.setOnClickListener(this);
    
    mPreviewImg = (ImageView) root.findViewById(R.id.img_preview);
    mPreviewLabel = (TextView) root.findViewById(android.R.id.text1);
    mStateArray = getResources().getStringArray(R.array.tracker_states);
    mPreviewclickHandler = new Handler();
    mSetterOn = new PreviewSetter(2);
    mSetterOff = new PreviewSetter(0);

    mColorButtons = new ColorButton[3];
    for (int i = 0; i < 3; i++) {
      mColorButtons[i] = (ColorButton) root.findViewById(ConfigSection.BUTTON_TRIPLET[i]);
      mColorButtons[i].setOnColorChangeListener(this, true);
    }

    mLabelHidden = (CheckBox) root.findViewById(R.id.chk_hide_label);
    mLabelHidden.setOnCheckedChangeListener(this);

    mLabelColor = (ColorButton) root.findViewById(R.id.cfg_lbl_color);
    mLabelColor.setOnColorChangeListener(this, true);

    mBtnBackDefault = (RadioButton) root.findViewById(R.id.btn_4);
    mBtnBackDefault.setOnCheckedChangeListener(this);
    mBtnBackImage = (RadioButton) root.findViewById(R.id.btn_5);
    mBtnBackImage.setOnCheckedChangeListener(this);

    mPositionButton = root.findViewById(R.id.cfg_back_stretch);
    mPositionButton.setOnClickListener(this);

    root.findViewById(R.id.btn_theme).setOnClickListener(this);
    applyTheme(settings, background);
    return root;
  }

  private void applyTheme(SettingsDecoder settings, Bitmap icon) {
    for (int i = 0; i < 3; i++) {
      mColorButtons[i].setColor(settings.getValue(SettingsDecoder.KEY_COLORS[i], SettingsDecoder.DEFAULT_COLORS[i]));
    }
    mSetterOff.run();

    int labelColor = settings.getValue(LabelRVFactory.KEY_COLOR, Color.BLACK);
    mLabelHidden.setChecked(settings.hasValue(FolderUtils.KEY_HIDE_LABEL));
    mPreviewLabel.setTextColor(labelColor);
    mLabelColor.setColor(labelColor);

    mBackImage = icon;
    if (mBackImage == null) {
      selectBackTab(0);
      mFolderPreview.setBitmap(null);
      mFolderPreview.setBackgroundResource(R.drawable.folder_back);
      mPositionButton.setVisibility(View.INVISIBLE);
    } else {
      mBackPadding = settings.getRect(SettingsDecoder.KEY_PADDING);
      mBackStretch = settings.getRect(SettingsDecoder.KEY_STRETCH);
      applyBackImage();
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    UiUtils.addDoneInMenu(menu, this);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (v == mFolderPreview) {
      mPreviewclickHandler.removeCallbacks(mSetterOn);
      mPreviewclickHandler.removeCallbacks(mSetterOff);

      new PreviewSetter(1).run();
      mPreviewOn = !mPreviewOn;
      mPreviewclickHandler.postDelayed(mPreviewOn ? mSetterOn : mSetterOff, 1500);
    } else if (id == R.id.btn_5) {
      startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQUEST_IMAGE);
    } else if (id == R.id.done) {
      Debug.log("Done clicked");

      byte[] background = null;

      JSONObject setting = new JSONObject();
      try {
        for (int i = 0; i < 3; i++) {
          setting.put(SettingsDecoder.KEY_COLORS[i], mColorButtons[i].getColor());
        }
        if (mLabelHidden.isChecked()) {
          setting.put(FolderUtils.KEY_HIDE_LABEL, true);
        }
        setting.put(LabelRVFactory.KEY_COLOR, mLabelColor.getColor());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (mBtnBackImage.isChecked() && (mBackImage != null) && BitmapUtils.saveBitmap(mBackImage, mBackStretch, out)) {
          background = out.toByteArray();

          setting.put(SettingsDecoder.KEY_STRETCH, BackgroundSection.getStr(mBackStretch))
            .put(SettingsDecoder.KEY_PADDING, BackgroundSection.getStr(mBackPadding));
          
        }
      } catch (Exception e) {
        Debug.log(e);
      }
      String name = mTitleText.getText().toString();

      if (getActivity() instanceof LaunchActivity) {
        mDb.saveSettigns(setting.toString(), background);
        FolderUtils.setName(name, mFolderId, mContext);
        ((LaunchActivity) getActivity()).onBackPressed();
      } else if (getActivity() instanceof FolderPick) {
        ((FolderPick) getActivity()).returnNew(setting.toString(), name, background);
      }
    } else if (id == R.id.cfg_back_stretch) {
      int[] padding = new int[] {
              mBackPadding[0],
              mBackPadding[1],
              mBackImage.getWidth() - mBackPadding[2],
              mBackImage.getHeight() - mBackPadding[3]
      };
      Intent intent = new Intent(getActivity(), NinePatchEditor.class)
        .putExtra("img", mBackImage)
        .putExtra(KEY_STRETCH, isValidStretch(mBackStretch, mBackImage) ? mBackStretch : new int[]{0, 0, mBackImage.getWidth(), mBackImage.getHeight() })
        .putExtra(KEY_PADDING, padding);
      startActivityForResult(intent, REQUEST_9PATCH);
    } else if (id == R.id.btn_theme) {
      startActivityForResult(new Intent(getActivity(), ThemePicker.class).putExtra("folders", true), REQUEST_THEMES);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == REQUEST_IMAGE) {
        new LargeImageLoadTask(getActivity()) {

          @Override
          protected void onSuccess(Bitmap result) {
            mBackImage = result;
            mBackStretch = new int[4];
            mBackPadding = new int[4];
            applyBackImage();
          }
        }.checkAndExecute(data.getData());
      } else if (requestCode == REQUEST_9PATCH) {
        mBackStretch = data.getIntArrayExtra(KEY_STRETCH);
        mBackPadding = data.getIntArrayExtra(KEY_PADDING);
        mBackPadding[2] = mBackImage.getWidth() - mBackPadding[2];
        mBackPadding[3] = mBackImage.getHeight() - mBackPadding[3];
        applyBackImage();
      } else if (requestCode == REQUEST_THEMES) {
        String config = data.getStringExtra("config");
        Bitmap icon = data.getParcelableExtra("icon");
        SettingsDecoder decoder = new SettingsDecoder(config);
        applyTheme(decoder, icon);
      }
    }
  }

  @Thunk void applyBackImage() {
    selectBackTab(1);
    mPositionButton.setVisibility(View.VISIBLE);
    
    mFolderPreview.setBackgroundColor(Color.TRANSPARENT);
    mFolderPreview.setBitmap(mBackImage);
    mFolderPreview.setDim(stretchToFloat(mBackStretch, mBackImage));
    mFolderPreview.setPadding(mBackPadding[0], mBackPadding[1], mBackPadding[2], mBackPadding[3]);
  }

  private void selectBackTab(int tab) {
    UiUtils.setCheckbox(tab == 0, mBtnBackDefault, this);
    UiUtils.setCheckbox(tab != 0, mBtnBackImage, this);
    
    mBtnBackImage.setOnClickListener(null);
    if (tab != 0) {
      mBtnBackImage.postDelayed(new Runnable() {

        @Override
        public void run() {
          mBtnBackImage.setOnClickListener(CFolderFrag.this);
        }
      }, 100);
    }
  }

  @Override
  public void onColorChanged(int color, View v) {
    if (v == mLabelColor) {
      mPreviewLabel.setTextColor(color);
    } else if (v == mColorButtons[1]) {
      onClick(mFolderPreview);
    } else {
      mPreviewOn = (v == mColorButtons[2]);
      if (mPreviewOn) {
        mSetterOn.run();
      } else {
        mSetterOff.run();
      }
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView == mBtnBackDefault && isChecked) {
      selectBackTab(0);
      mFolderPreview.setBitmap(null);
      mFolderPreview.setBackgroundResource(R.drawable.folder_back);
      mPositionButton.setVisibility(View.INVISIBLE);
    } else if (buttonView == mBtnBackImage && mBtnBackImage.isChecked()) {
      if (mBackImage == null) {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQUEST_IMAGE);
        UiUtils.setCheckbox(false, buttonView, this);
      } else {
        applyBackImage();
      }
    } else if (buttonView == mLabelHidden) {
      mPreviewLabel.setVisibility(isChecked ? View.GONE : View.VISIBLE);
    }
  }

  public int[] getCurrentColors() {
    int[] colors = new int[3];
    for (int i = 0; i < 3; i++) {
      colors[i] = mColorButtons[i].getColor();
    }
    return colors;
  }

  /**
   * A simple runnable to animate button color change.
   */
  private class PreviewSetter implements Runnable {

    private final int mState;

    PreviewSetter(int state) {
      mState = state;
    }

    @Override
    public void run() {
      mPreviewLabel.setText(mStateArray[mState]);
      int color = mColorButtons[mState].getColor();
      mPreviewImg.setColorFilter(ParseUtil.removeAlphaFromColor(color));
      mPreviewImg.setAlpha(Color.alpha(color));
    }
  }

  private static float[] stretchToFloat(int[] stretch, Bitmap img) {
    if (isValidStretch(stretch, img)) {
      float w = img.getWidth();
      float h = img.getHeight();
      return new float[] {
              stretch[0] / w,
              stretch[1] / h,
              stretch[2] / w,
              stretch[3] / h
      };
    }
    return new float[] {0, 0, 1, 1};
  }

  private static boolean isValidStretch(int[] stretch, Bitmap img) {
    return (stretch[2] > stretch[0]) &&
            (stretch[3] > stretch[1]) &&
            (stretch[0] >= 0) &&
            (stretch[1] >= 0) &&
            (stretch[2] <= img.getWidth()) &&
            (stretch[3] <= img.getHeight());
  }
}
