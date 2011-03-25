package com.okolabo.android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class DeployUtils {

	/**
	 * マニフェストファイルを読んでデバッグモードかどうかを取得
	 */
	public static boolean isDebuggable(Context context) {
		PackageManager manager = context.getPackageManager();
		ApplicationInfo appInfo = null;
		try {
			appInfo = manager.getApplicationInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE) {
			return true;
		}
		return false;
	}
}
