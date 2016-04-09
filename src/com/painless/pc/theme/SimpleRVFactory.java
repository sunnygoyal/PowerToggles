package com.painless.pc.theme;

import static com.painless.pc.singleton.Globals.BUTTONS;

import com.painless.pc.R;
import com.painless.pc.util.SettingsDecoder;

/**
 * A factory class to create remove views for a widget.
 */
public class SimpleRVFactory extends RVFactory {

  public static final String KEY_LARGE_ICONS = "hugeIcon";
  public static final String KEY_FULL_HEIGHT = "fullHeight";

  public SimpleRVFactory(SettingsDecoder decoder, boolean isNotification) {
    super(getLayoutId(decoder, isNotification), BUTTONS);
  }

  private static int getLayoutId(SettingsDecoder decoder, boolean isNotification) {
    boolean useLargeIcons = decoder.hasValue(KEY_LARGE_ICONS);
    if (isNotification || decoder.hasValue(KEY_FULL_HEIGHT)) {
      return useLargeIcons ? R.layout.aw_huge_icons : R.layout.aw_default;
    } else {
      return useLargeIcons ? R.layout.aw_huge_icons_60 : R.layout.aw_default_60;
    }
  }
}
