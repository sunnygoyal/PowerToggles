package com.painless.pc.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.painless.pc.R;
import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;

public abstract class ImageLoadTask extends ProgressTask<Uri, Bitmap> {

  protected final Context mContext;
  protected final int mIconSize;

  public ImageLoadTask(Context context, int size) {
    super(context, context.getString(R.string.ip_loading_img));
    mContext = context;
    mIconSize = size;
  }

  
  @Override
  protected Bitmap doInBackground(Uri... params) {
    try {
      Bitmap image = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), params[0]);
      return resizeBitmap(image);
    } catch (Throwable e) {
      Debug.log(e);
      return null;
    }
  }

  protected Bitmap resizeBitmap(Bitmap original) {
    return Bitmap.createScaledBitmap(original, mIconSize, mIconSize, true);
  }

  @Override
  public void onDone(Bitmap result) {
    if (result != null) {
      onSuccess(result);
    } else {
      Toast.makeText(mContext, R.string.ip_loading_img_failed, Toast.LENGTH_LONG).show();
    }
  }

  public void checkAndExecute(Uri uri) {
    if (uri != null) {
      execute(uri);
    }
  }

  protected abstract void onSuccess(Bitmap result);

  public static abstract class LargeImageLoadTask extends ImageLoadTask {

    public LargeImageLoadTask(Context context) {
      super(context, 0);
    }

    @Override
    protected Bitmap resizeBitmap(Bitmap original) {
      return BitmapUtils.resizeToScreenSizeLimit(original, mContext);
    }
  }
}
