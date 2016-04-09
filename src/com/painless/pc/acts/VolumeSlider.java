package com.painless.pc.acts;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.provider.Settings;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.ParseUtil;
import com.painless.pc.tracker.VolumeTracker;

public class VolumeSlider extends AbstractPopup implements OnCheckedChangeListener {

	public VolumeSlider() {
		super(R.layout.volume_sliders);
	}

	private final SparseArray<Ringtone> ringtoneMap = new SparseArray<Ringtone>();

	private Ringtone musicSample;
	private Ringtone alarmSample;
	private Ringtone notifySample;
	
	private AudioManager am;
	private RadioGroup volume_modes;
	private boolean playMusic;

	private LinearLayout volume_sliders_container;

	private SeekBar mLastControlledSeekbar = null;
	
	@Override
	void initUi() {
		SharedPreferences prefs = Globals.getAppPrefs(this);
		playMusic = !prefs.getBoolean("mute_slider", false);
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		musicSample = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_RINGTONE_URI);
		alarmSample = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
		notifySample = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_NOTIFICATION_URI);

		volume_modes = (RadioGroup) findViewById(R.id.volume_modes);
		if (!prefs.getBoolean("volume_slider_preset", true)) {
			volume_modes.setVisibility(View.GONE);
		}
		
		final int mode = am.getRingerMode();
		boolean ringAndVibrate = VolumeTracker.isVibrateOn(this, am);
		volume_modes.check(mode == AudioManager.RINGER_MODE_SILENT ? R.id.rd_vol_2 :
			(mode == AudioManager.RINGER_MODE_VIBRATE ? R.id.rd_vol_1 :
				(ringAndVibrate ? R.id.rd_vol_4 : R.id.rd_vol_3)));
		volume_modes.setOnCheckedChangeListener(this);
		
		boolean[] enabledModes = new boolean[6];
		ParseUtil.parseBoolArray(enabledModes, prefs.getInt("slider_toggles", 7));
		
		volume_sliders_container = (LinearLayout) findViewById(R.id.rootLayout);
		
		initSeekBar(AudioManager.STREAM_RING, musicSample, R.drawable.icon_ring, enabledModes[0]);
		initSeekBar(AudioManager.STREAM_MUSIC, musicSample, R.drawable.icon_music, enabledModes[1]);
		initSeekBar(AudioManager.STREAM_ALARM, alarmSample, R.drawable.icon_alarm, enabledModes[2]);
		initSeekBar(AudioManager.STREAM_NOTIFICATION, notifySample, R.drawable.icon_notify, enabledModes[3]);
		initSeekBar(AudioManager.STREAM_VOICE_CALL, alarmSample, R.drawable.icon_handset, enabledModes[4]);
		initSeekBar(AudioManager.STREAM_SYSTEM, notifySample, R.drawable.icon_application, enabledModes[5]);
	}


	private void initSeekBar(int stream_type, Ringtone tone, int iconRes, boolean enabled) {
		if (enabled) {
			View v = getLayoutInflater().inflate(R.layout.volume_sider_item, volume_sliders_container, false);
			((ImageView) v.findViewById(R.id.vs_icon)).setImageResource(iconRes);

			final SeekBar seek = (SeekBar) v.findViewById(R.id.vs_seek);
			seek.setTag(stream_type);
			seek.setMax(am.getStreamMaxVolume(stream_type));
			seek.setProgress(am.getStreamVolume(stream_type));
			seek.setOnSeekBarChangeListener(this);

			ringtoneMap.put(stream_type, tone);
			volume_sliders_container.addView(v);

			if (mLastControlledSeekbar == null) {
				mLastControlledSeekbar = seek;
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		stopMusic();
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		final int stream = (Integer) seekBar.getTag();
		am.setStreamVolume(stream, seekBar.getProgress(), 0);

		if (stream == AudioManager.STREAM_RING) {
			volume_modes.check(seekBar.getProgress() > 0 ?
					(VolumeTracker.isVibrateOn(this, am) ? R.id.rd_vol_4 : R.id.rd_vol_3) :
						R.id.rd_vol_1);
		}

		Ringtone tone = ringtoneMap.get(stream);
		if (tone!=null && playMusic) {
			tone.setStreamType(stream);
			tone.play();
		}
		mLastControlledSeekbar = seekBar;
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		final int mode = checkedId == R.id.rd_vol_1 ? AudioManager.RINGER_MODE_VIBRATE :
			(checkedId == R.id.rd_vol_2 ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
		am.setRingerMode(mode);

		VolumeTracker.setVibrate(this, am, (checkedId == R.id.rd_vol_4) ? 1 : 0);
		stopMusic();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopMusic();
	}

	private void stopMusic() {
		if (musicSample != null) {
			musicSample.stop();
		}
		if (alarmSample != null) {
			alarmSample.stop();
		}
		if (notifySample != null) {
			notifySample.stop();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Integer val = null;
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			val = Math.max(0, mLastControlledSeekbar.getProgress() - 1);
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			val = Math.min(mLastControlledSeekbar.getMax(), mLastControlledSeekbar.getProgress() + 1);
		}

		if (val != null) {
			stopMusic();
			mLastControlledSeekbar.setProgress(val);
			onStopTrackingTouch(mLastControlledSeekbar);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}