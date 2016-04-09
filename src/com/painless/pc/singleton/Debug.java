package com.painless.pc.singleton;

import android.util.Log;

// A wrapper over android logging. This is removed in release builds.
public class Debug {

	private static final String TAG = "PC";

	public static void log(Object msg) {
		Log.e(getTag(), msg + "");
	}

	public static void log(Throwable err) {
		Log.e(getTag(), err.getMessage() + " ");
		err.printStackTrace();
	}

	private static String getTag() {
		boolean startFound = false;
		String className = Debug.class.getName();

		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		for(StackTraceElement item : trace){
			if (item.getClassName().equals(className)) {
				startFound = true;
			} else if (startFound) {
				return String.format(
						"[%s/%s/%s]", item.getFileName(), item.getMethodName(), item.getLineNumber());
			}
		}

		return TAG;
	}
//
//	public static void test(Context context, OutputStream fileOut) throws Exception {
//		ZipOutputStream out = new ZipOutputStream(fileOut);
//
//		int[][] widgetSections = new int[][] {
//				new int[] { 1, 11, 0, 26},						// Mobile data
//				new int[] { 3, 2, 28, 8, 6, 22, 24, 41, 42},	// Network
//				new int[] { 18, 19, 20, 21},					// Multimedia
//				new int[] { 7, 17, 23, 13, 16, 9, 31, 38},		// Display
//				new int[] { 4, 5, 25, 10, 27, 15, 40, 43, 44}, 	// Hardware
//				new int[] { 33, 32, 34},						// App Commands
//				new int[] { 29, 30, 35, 39},					// Root
//				new int[] { 14, 12, 36, 37}						// Others
//		};
//		
//		int count = 0;
//		for (int[] f : widgetSections) {
//			count += f.length;
//		}
//		
//		String names = "";
//		Bitmap icon = Bitmap.createBitmap(count*17 - 1, 16, Bitmap.Config.ARGB_8888);
//		Canvas c = new Canvas(icon);
//		
//		Rect dest = new Rect(0, 0, 16, 16);
//		
//		String trackerLables[] = context.getResources().getStringArray(R.array.tracker_names);
//		String trackerHeaders[] = context.getResources().getStringArray(R.array.tracker_category);
//		SharedPreferences pref = context.getSharedPreferences(Globals.SHARED_PREFS_NAME, 0);
//
//		for (int i=0; i<widgetSections.length; i++) {
//			names += trackerHeaders[i] + "\n\n";
//			
//
//			for (final int tId : widgetSections[i]) {
//				names += trackerLables[tId] + "\n";
//				int img = TrackerManager.getTracker(tId, pref).getButtonImageConfig()[1];
//				
//				Bitmap imgBitmap = BitmapFactory.decodeResource(context.getResources(), img);
//				Rect r = new Rect(imgBitmap.getWidth() / 8, imgBitmap.getHeight()/8, imgBitmap.getWidth()*7/8, imgBitmap.getHeight()*7/8);
//				
//				c.drawBitmap(imgBitmap, r, dest, null);
//				dest.offset(17, 0);
//			}
//			names += "\n\n\n\n";
//		}
//
//		out.putNextEntry(new ZipEntry("icon.png"));
//		icon.compress(Bitmap.CompressFormat.PNG, 100, out);
//		out.closeEntry();
//		
//		out.putNextEntry(new ZipEntry("names.txt"));
//		out.write(names.getBytes());
//		out.closeEntry();
//		out.close();
//	}
}
