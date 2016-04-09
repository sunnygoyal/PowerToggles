package com.painless.pc;

import java.lang.reflect.Constructor;

import android.content.SharedPreferences;

import com.painless.pc.singleton.Debug;
import com.painless.pc.tracker.AbstractTracker;
import com.painless.pc.tracker.AdbWirelessTracker;
import com.painless.pc.tracker.AirplaneTracker;
import com.painless.pc.tracker.AutoBacklightTracker;
import com.painless.pc.tracker.AutoRotateTracker;
import com.painless.pc.tracker.BacklightTracker;
import com.painless.pc.tracker.BatteryTracker;
import com.painless.pc.tracker.BluetoothDiscoveryTracker;
import com.painless.pc.tracker.BluetoothHotspotTracker;
import com.painless.pc.tracker.BluetoothTracker;
import com.painless.pc.tracker.BrightnessSliderToggle;
import com.painless.pc.tracker.DataNetworkTracker;
import com.painless.pc.tracker.FlashStateTracker;
import com.painless.pc.tracker.FontDecreaseTracker;
import com.painless.pc.tracker.FontIncreaseTracker;
import com.painless.pc.tracker.GprsStateTracker;
import com.painless.pc.tracker.GpsStateTracker;
import com.painless.pc.tracker.HomeCommand;
import com.painless.pc.tracker.HotSpotTracker;
import com.painless.pc.tracker.ImmersiveTracker;
import com.painless.pc.tracker.LockScreenToggle;
import com.painless.pc.tracker.MediaNext;
import com.painless.pc.tracker.MediaPlayPause;
import com.painless.pc.tracker.MediaPrev;
import com.painless.pc.tracker.MediaVolume;
import com.painless.pc.tracker.NfcStateTracker;
import com.painless.pc.tracker.NoLockTracker;
import com.painless.pc.tracker.NotifyWidgetTracker;
import com.painless.pc.tracker.PulseLightTracker;
import com.painless.pc.tracker.RecentAppsCommand;
import com.painless.pc.tracker.RestartCommand;
import com.painless.pc.tracker.RotationLockTracker;
import com.painless.pc.tracker.ScreenLightCommand;
import com.painless.pc.tracker.ScreenOnTracker;
import com.painless.pc.tracker.ShutdownCommand;
import com.painless.pc.tracker.ShutdownMenuCommand;
import com.painless.pc.tracker.SipCallTracker;
import com.painless.pc.tracker.SipReceiveTracker;
import com.painless.pc.tracker.SyncNowTracker;
import com.painless.pc.tracker.SyncStateTracker;
import com.painless.pc.tracker.TimeoutTracker;
import com.painless.pc.tracker.TwoRowTracker;
import com.painless.pc.tracker.UsbTetherTracker;
import com.painless.pc.tracker.VolumeSliderToggle;
import com.painless.pc.tracker.VolumeTracker;
import com.painless.pc.tracker.WiMaxTracker;
import com.painless.pc.tracker.WidgetSettingCommand;
import com.painless.pc.tracker.WifiOptimizeTracker;
import com.painless.pc.tracker.WifiStateTracker;

public class TrackerManager {

	@SuppressWarnings("unchecked")
	public static final Class<? extends AbstractTracker>[] TRACKER_LIST =
		new Class[] {
		HotSpotTracker.class, 		// 0
		GprsStateTracker.class,		// 1
		SyncStateTracker.class,		// 2
		WifiStateTracker.class,		// 3
		FlashStateTracker.class,	// 4
		GpsStateTracker.class,		// 5
		BluetoothTracker.class,		// 6
		BacklightTracker.class,		// 7
		AirplaneTracker.class,		// 8
		AutoRotateTracker.class,	// 9
		VolumeTracker.class,		// 10
		DataNetworkTracker.class,		// 11
		UsbTetherTracker.class,		// 12
		ScreenOnTracker.class,		// 13
		WiMaxTracker.class,			// 14
		BatteryTracker.class,		// 15
		TimeoutTracker.class,		// 16
		AutoBacklightTracker.class,	// 17
		MediaPlayPause.class,		// 18
		MediaNext.class,			// 19
		MediaPrev.class,			// 20
		MediaVolume.class,			// 21
		BluetoothDiscoveryTracker.class,// 22
		BrightnessSliderToggle.class,	//23
		NfcStateTracker.class,		// 24
		LockScreenToggle.class,		// 25
		BluetoothHotspotTracker.class,	// 26
		VolumeSliderToggle.class,	// 27
		SyncNowTracker.class,		// 28
		ShutdownCommand.class,		// 29
		RestartCommand.class,		// 30
		ScreenLightCommand.class,	// 31
		NotifyWidgetTracker.class,	// 32
		WidgetSettingCommand.class,	// 33
		TwoRowTracker.class,		// 34
		ShutdownMenuCommand.class,	// 35
		FontIncreaseTracker.class,	// 36
		FontDecreaseTracker.class,	// 37
		RotationLockTracker.class,	// 38
		AdbWirelessTracker.class,	// 39
		PulseLightTracker.class,	// 40
		SipReceiveTracker.class,	// 41
		SipCallTracker.class,		// 42
		HomeCommand.class,			// 43
		RecentAppsCommand.class,	// 44
		NoLockTracker.class,		// 45
    WifiOptimizeTracker.class,  // 46
    ImmersiveTracker.class  // 47
	};

	public static AbstractTracker getTracker(int id, SharedPreferences pref) {
		try {
			final Class<? extends AbstractTracker> trackerClass = TRACKER_LIST[id];
			final Constructor<? extends AbstractTracker> ct = trackerClass.getConstructor(
					new Class[] {Integer.TYPE, SharedPreferences.class});

			return ct.newInstance(id, pref);
		} catch (final Throwable e) {
			Debug.log(e);
			return new WifiStateTracker(3, pref);
		}
	}
}
