
package com.okolabo.android.teidennotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class Prefs {

    /** 初回起動時の確認をしたかどうか */
    public static final String KEY_CONFIRM = "confirm";
    /** 既定グループ設定 */
    public static final String KEY_DEFAULT_GROUP = "default_group";
    
    /** 広告を出してよいかどうか */
    public static final String KEY_CM = "cm";

    public static SharedPreferences get(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * 初回起動時に広告を表示してよいか確認したかを取得する
     * @param context
     * @return
     */
    public static boolean getConfirmFlg(Context context) {
        return get(context).getBoolean(KEY_CONFIRM, false);
    }
    
    /**
     * 広告を出してよいかを取得する
     * @param context
     * @return
     */
    public static boolean getCmFlg(Context context) {
        return get(context).getBoolean(KEY_CM, false);
    }
    
    /**
     * 既定のグループを取得する
     * @param context
     * @return
     */
    public static String getDefaultGroup(Context context) {
        return get(context).getString(KEY_DEFAULT_GROUP, "");
    }
}
