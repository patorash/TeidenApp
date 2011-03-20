
package com.okolabo.android.teidennotify;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static String TAG = "DatabaseHelper";

    private static final int DB_VERSION = 3;

    private static final String DATABASE_NAME = "teiden_app.db";

    private SQLiteDatabase mDB;
    
    private Context mContext;

    /** 検索履歴テーブル名 */
    private static final String TBL_HISTORIES = "Historis";

    /** 検索履歴 */
    interface Histories {
        public static final String ID = "_id";

        public static final String HISTORY = "history";

        public static final String CREATED = "created";
    }
    
    /** 入力履歴テーブル名 */
    private static final String TBL_INPUT_HISTRIES = "InputHistories";
    
    /** 入力履歴 */
    interface InputHistories {
        
        public static final String ID = "_id";

        public static final String PREF = "pref";

        public static final String ADDRESS = "address";
    }

    /** 入力履歴テーブル名 */
    private static final String TBL_LOCATION_HISTORIES = "LocationHistories";
    
    /** 入力履歴 */
    interface LocationHistories {
        
        public static final String ID = "_id";

        public static final String TITLE = "title";

        public static final String ADDRESS = "address";
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        mContext = context;
        mDB = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Historisテーブル
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_HISTORIES + " ("
                + Histories.ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Histories.HISTORY + " TEXT,"
                + Histories.CREATED + " TEXT"
                + ")"
                );
        // InputHistoriesテーブル
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_INPUT_HISTRIES + " ("
                + InputHistories.ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + InputHistories.PREF + " TEXT,"
                + InputHistories.ADDRESS + " TEXT"
                + ")"
                );
        // LocationHistoriesテーブル
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_LOCATION_HISTORIES + " ("
                + LocationHistories.ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LocationHistories.TITLE + " TEXT,"
                + LocationHistories.ADDRESS + " TEXT"
                + ")"
                );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int olderVersion, int newVersion) {
        if (olderVersion < 2) {
            // InputHistoriesテーブル
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_INPUT_HISTRIES + " ("
                    + InputHistories.ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + InputHistories.PREF + " TEXT,"
                    + InputHistories.ADDRESS + " TEXT"
                    + ")"
                    );
        }
        if (olderVersion < 3) {
            // LocationHistoriesテーブル
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_LOCATION_HISTORIES + " ("
                    + LocationHistories.ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + LocationHistories.TITLE + " TEXT,"
                    + LocationHistories.ADDRESS + " TEXT"
                    + ")"
                    );
        }
        
    }

    public long insertHistories(String history) {
        checkOpen();
        // インサート
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        
        ContentValues cv = new ContentValues();
        cv.put(Histories.HISTORY, history);
        cv.put(Histories.CREATED, fmt.format(cal.getTime()));
        long rowId = 0;
        rowId = mDB.insert(TBL_HISTORIES, null, cv);
        if (rowId <= 0) {
            Log.e(TAG, "Table: " + TBL_HISTORIES + " INSERT ERROR!");
        }
        return rowId;
    }

    public Cursor getAll() {
        checkOpen();
        String orderBy = Histories.ID + " DESC";
        return mDB.query(TBL_HISTORIES, null, null, null, null, null, orderBy);
    }
    
    public Cursor get(long id) {
        checkOpen();
        String where = Histories.ID + " = ?";
        String[] placeHolder = {
            String.valueOf(id)
        };
        return mDB.query(TBL_HISTORIES, null, where, placeHolder, null, null, null);
    }

    public int delete(long id) {
        checkOpen();
        String where = Histories.ID + " = ?";
        String[] placeHolder = {
            String.valueOf(id)
        };
        return mDB.delete(TBL_HISTORIES, where, placeHolder);
    }
    
    //
    // 以下、InputHistories
    //
    /**
     * 入力履歴を保存する
     * 
     * @param pref 都道府県名
     * @param address 都道府県以降の住所
     */
    public long insertInputHistories(String pref, String address) {
        checkOpen();
        // インサート
        ContentValues cv = new ContentValues();
        cv.put(InputHistories.PREF, pref);
        cv.put(InputHistories.ADDRESS, address);
        long rowId = 0;
        rowId = mDB.insert(TBL_INPUT_HISTRIES, null, cv);
        if (rowId <= 0) {
            Log.e(TAG, "Table: " + TBL_INPUT_HISTRIES + " INSERT ERROR!");
        }
        return rowId;
    }

    /** 入力履歴を全件取得 */
    public Cursor getAllInputHistories() {
        checkOpen();
        String orderBy = InputHistories.ID + " DESC";
        return mDB.query(TBL_INPUT_HISTRIES, null, null, null, null, null, orderBy);
    }
    
    /** 指定した入力履歴を取得 */
    public Cursor getInputHistory(long id) {
        checkOpen();
        String where = InputHistories.ID + " = ?";
        String[] placeHolder = {
            String.valueOf(id)
        };
        return mDB.query(TBL_INPUT_HISTRIES, null, where, placeHolder, null, null, null);
    }

    /** 入力履歴を消す */
    public int deleteInputHistory(long id) {
        checkOpen();
        String where = InputHistories.ID + " = ?";
        String[] placeHolder = {
            String.valueOf(id)
        };
        return mDB.delete(TBL_INPUT_HISTRIES, where, placeHolder);
    }
    
    //
    // 以下、LocationHistories
    //
    /**
     * 現在地履歴を保存する
     * 
     * @param address 現在地の住所
     */
    public long insertLocationHistories(String address) {
        checkOpen();
        // インサート
        ContentValues cv = new ContentValues();
        cv.put(LocationHistories.TITLE, mContext.getString(R.string.no_title));
        cv.put(LocationHistories.ADDRESS, address);
        long rowId = 0;
        rowId = mDB.insert(TBL_LOCATION_HISTORIES, null, cv);
        if (rowId <= 0) {
            Log.e(TAG, "Table: " + TBL_LOCATION_HISTORIES + " INSERT ERROR!");
        }
        return rowId;
    }
    
    

    /** 入力履歴を全件取得 */
    public Cursor getAllLocationHistories() {
        checkOpen();
        String orderBy = LocationHistories.ID + " DESC";
        return mDB.query(TBL_LOCATION_HISTORIES, null, null, null, null, null, orderBy);
    }
    
    /** 指定した入力履歴を取得 */
    public Cursor getLocationHistory(long id) {
        checkOpen();
        String where = LocationHistories.ID + " = ?";
        String[] placeHolder = {
            String.valueOf(id)
        };
        return mDB.query(TBL_LOCATION_HISTORIES, null, where, placeHolder, null, null, null);
    }

    /** 入力履歴を消す */
    public int deleteLocationHistory(long id) {
        checkOpen();
        String where = LocationHistories.ID + " = ?";
        String[] placeHolder = {
            String.valueOf(id)
        };
        return mDB.delete(TBL_LOCATION_HISTORIES, where, placeHolder);
    }
    
    /**
     * 現在地チェックした地名が既に履歴にあるか確認
     * @param address
     * @return
     */
    public boolean existsLocation(String address) {
        checkOpen();
        boolean result;
        String where = LocationHistories.ADDRESS + " = ?";
        String[] placeHolder = {
                address
        };
        Cursor c = mDB.query(TBL_LOCATION_HISTORIES, null, where, placeHolder, null, null, null);
        if (c.getCount() > 0) {
            result = true;
        } else {
            result = false;
        }
        c.close();
        return result;
    }
    
    /**
     * 地名のタイトルを更新する
     * @param id
     * @param title
     * @return
     */
    public int updateLocationTitle(long id, String title) {
        checkOpen();
        ContentValues cv = new ContentValues();
        cv.put(LocationHistories.TITLE, title);
        return mDB.update(TBL_LOCATION_HISTORIES, cv, LocationHistories.ID + " = ?", new String[] {
            String.valueOf(id)
        });
    }
    
    /**
     * データベースが開いているかチェックして、開いてなければ開く
     */
    private void checkOpen() {
        if (mDB == null || !mDB.isOpen()) {
            mDB = getWritableDatabase();
        }
    }
    
    /**
     * データベースをcloseする
     * 
     * 実験で使うために定義した
     */
    public void close() {
        mDB.close();
    }
    
    @Override
    protected void finalize() throws Throwable {
        mDB.close();
        this.close();
        super.finalize();
    }
}
