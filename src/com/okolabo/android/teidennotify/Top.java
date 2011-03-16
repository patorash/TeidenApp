package com.okolabo.android.teidennotify;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.kayo.android.exceptionlib.ExceptionBinder;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.okolabo.android.teidennotify.DatabaseHelper.Histories;
import com.okolabo.android.teidennotify.DatabaseHelper.InputHistories;

public class Top extends Activity implements LocationListener{
    
    // 現在地と地名検索で処理をわけるためのフラグ
    private static final int TEIDEN_LOCATION = 1;
    private static final int TEIDEN_SEARCH = 2;
    
    // 入力履歴のリクエストコード
    private static final int REQ_INPUT_HISTORY = 1;
    
    private static final HashMap<String, String> TEIDEN_URL_LIST;
    
    
    /** 位置情報を見るマネージャー */
    protected LocationManager mLocationManager;
    
    /** 位置情報プロバイダ(GPS or Network) */
    protected LocationProvider mLocationProvider;
    
    /** 現在地 */
    protected Location mLocation;

    /**  */
    protected Geocoder mGeocoder;
    
    
    private Spinner mSpnPref;
    
    private EditText mEditAddress;
    
    private TabHost mTabs;
    
    private Calendar mCalendar;
    
    private int mMonth;
    
    /** 今日の日付(365日で) */
    private int mCurrentDayOfYear;
    
    /** 計画停電がわかっている日付(365日で) */
    private int mMaxKnownDayOfYear;
    
    /** DBHelper */
    private DatabaseHelper mDBHelper;
    
    /** DBのCursor */
    private Cursor mCursor;
    
    /** 検索履歴のApdater */
    private HistoryAdapter mAdapter;
    
    /** 入力履歴から復元された都道府県 */
    private String mInputHistoryPref = null;
    
    /** 入力履歴から復元された住所 */
    private String mInputHistoryAddress = null;
    
    static {
        TEIDEN_URL_LIST = new HashMap<String, String>();
        TEIDEN_URL_LIST.put("東京都", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/tokyo.html");
        TEIDEN_URL_LIST.put("神奈川県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/kanagawa.html");
        TEIDEN_URL_LIST.put("千葉県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/chiba.html");
        TEIDEN_URL_LIST.put("埼玉県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/saitama.html");
        TEIDEN_URL_LIST.put("静岡県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/shizuoka.html");
        TEIDEN_URL_LIST.put("栃木県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/tochigi.html");
        TEIDEN_URL_LIST.put("群馬県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/gunma.html");
        TEIDEN_URL_LIST.put("茨城県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/ibaraki.html");
        TEIDEN_URL_LIST.put("山梨県", "http://mainichi.jp/select/weathernews/20110311/mai/keikakuteiden/yamanashi.html");
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ExceptionBinder.bind(this, getString(R.string.android_exception_service_id));
        mGeocoder = new Geocoder(getApplicationContext(), Locale.JAPAN);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        // グループと停電時間が日ごとに変わるので、日付を取得しとく
        // 今日の日付を保存
        mCalendar = Calendar.getInstance();
        mMonth = mCalendar.get(Calendar.MONTH) + 1;
        // 今日の日付(365日で) 月が変わっても対応できるように。
        mCurrentDayOfYear = mCalendar.get(Calendar.DAY_OF_YEAR);
        
        // 計画停電情報がわかっている日まで
        mCalendar.set(Calendar.MONTH, mMonth - 1); // 3月
        mCalendar.set(Calendar.DATE, 21);
        mMaxKnownDayOfYear = mCalendar.get(Calendar.DAY_OF_YEAR);
        
        // DBHelperを生成
        mDBHelper = new DatabaseHelper(this);
        
        // タブを作成
        mTabs = (TabHost) findViewById(R.id.tabhost);
        mTabs.setup();
        // 現在地タブ
        TabHost.TabSpec spec = mTabs.newTabSpec("tag1");
        spec.setContent(R.id.tab1);
        spec.setIndicator(getString(R.string.tab_current_address));
        mTabs.addTab(spec);
        
        // 地域検索タブ
        spec = mTabs.newTabSpec("tag2");
        spec.setContent(R.id.tab2);
        spec.setIndicator(getString(R.string.tab_search));
        mTabs.addTab(spec);
        
        // 検索履歴タブ
        spec = mTabs.newTabSpec("tag3");
        spec.setContent(R.id.tab3);
        spec.setIndicator(getString(R.string.tab_history));
        mTabs.addTab(spec);
        
        // スピナー
        mSpnPref = (Spinner) findViewById(R.id.pref);
        ArrayAdapter<String> aa = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.prefs));
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnPref.setAdapter(aa);
        
        // テキスト
        mEditAddress = (EditText) findViewById(R.id.address);
        
        // 検索ボタン
        Button btnSearch = (Button) findViewById(R.id.search);
        btnSearch.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                // 非同期で検索
                // スピナーから都道府県を取得
                String pref = (String)mSpnPref.getSelectedItem();
                new SearchAsyncTask(TEIDEN_SEARCH, null).execute(pref);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // GPSのリスナーを解除
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
    }
    
    @Override
    protected void onDestroy() {
        mDBHelper.close();
        super.onDestroy();
    }

    /**
     * 使えるプロバイダーがない
     */
    private void noProviderEnabled() {
        
    }

    
    public void onProviderDisabled(String provider) {} // 今回は実装しない

    public void onProviderEnabled(String provider) {} // 今回は実装しない

    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                break;
                
            case LocationProvider.OUT_OF_SERVICE:
                // GPSサービスが利用できない
                gpsOutOfService();
                break;
                
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                // GPSデータを取得できない
                gpsTemporarilyUnavailable();
                break;
        }
    }

    /**
     * 位置情報が取得できた
     */
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (location != null) {
            // これ以上電池を消費しても意味がないので消去
            mLocationManager.removeUpdates(this);
            gpsLocationChanged();
        }
    }

    /**
     * 位置情報が取得できたので処理を行う
     */
    private void gpsLocationChanged() {
        // 位置情報を住所に変換
        TextView currentAddress = (TextView) findViewById(R.id.currentAddress);
        try {
            List<Address> list_address = mGeocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 10);
            Address address = list_address.get(0);
            StringBuilder builder = new StringBuilder();
            String buf;
            for (int i = 1; (buf = address.getAddressLine(i)) != null; i++) {
                builder.append(buf);
            }
            String strAddress = builder.toString();
            // TODO テストの際に使う このTODOはテストのときにここを見つけるために付けてます。
            // グループが1つの住所
//            strAddress = "東京都調布市小島町２丁目XX−X";
            // グループが複数の住所
//            strAddress = "神奈川県相模原市南区相南１丁目";
            currentAddress.setText(strAddress);
            
            // 住所がどのエリアであるか照合する
            Iterator<String> prefIterator = TEIDEN_URL_LIST.keySet().iterator();
            String hitPref = null;
            while (prefIterator.hasNext()) {
                String pref = (String) prefIterator.next();
                Pattern pattern = Pattern.compile(pref);
                Matcher matcher = pattern.matcher(strAddress);
                if (matcher.find()) {
                    hitPref = pref;
                    break;
                }
            }
            if (hitPref != null) {
                // 非同期でデータを取得
                new SearchAsyncTask(TEIDEN_LOCATION, strAddress).execute(hitPref);
            } else {
                // 対象の都道府県以外なので、計画停電エリア外と表示
                Button btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
                btnGetMyLocation.setEnabled(true);
                btnGetMyLocation.setText(R.string.get_my_location);
                ((TextView) findViewById(R.id.groupNumber)).setText(R.string.out_of_area);
                ((TextView) findViewById(R.id.teidenSpan)).setText(R.string.out_of_area);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * GPSサービスが利用出来ない場合の処理
     */
    protected void gpsOutOfService() {
        Toast.makeText(this, "GPSサービスが利用できません", Toast.LENGTH_LONG).show();
    }

    /**
     * GPSデータが取得できない場合の処理
     */
    protected void gpsTemporarilyUnavailable() {
        Toast.makeText(this, "GPSデータを取得できません", Toast.LENGTH_LONG).show();
    }
    
    
    protected HashMap<String, ArrayList<Integer>> getTeidenInfo(String url) {
        // 住所がどのエリアであるか照合する
        Source source;
        HashMap<String, ArrayList<Integer>> areaMap = new HashMap<String, ArrayList<Integer>>();
        try {
            source = new Source(new URL(url));
            List<Element> elementList = source.getAllElementsByClass("NewsBody");
//          List<Element> elementList=source.getAllElements();
            for (Element element : elementList) {
                String newsBody = element.toString();
//                Log.d("TeidenApp", newsBody);
                String[] areas = newsBody.split("<br />|<br>");
                for (int i = 1; i < areas.length; i++) {
                    String[] area = areas[i].trim().split("　");
                    if (area.length > 1) {
                        String[] areaNames = area[0].split(" ");
                        StringBuilder areaNameBuilder = new StringBuilder();
                        for (String areaName: areaNames) {
                            areaNameBuilder.append(areaName);
                        }
                        String areaName = areaNameBuilder.toString();
                        // areaName から「大字」を除去する
                        areaName = areaName.replaceAll("大字", "");
                        // areaName から「の一部」を除去する
                        areaName = areaName.replaceAll("の一部", "");
                        ArrayList<Integer> groups;
                        if (areaMap.containsKey(areaName)) {
                            groups = areaMap.get(areaName);
                        } else {
                            groups = new ArrayList<Integer>();
                        }
                        area[1] = zenkakuToHankaku(area[1]);
//                        Log.d("TeidenApp", area[1]);
                        String areaNum = null;
                        Pattern pattern = Pattern.compile("\\d");
                        Matcher matcher = pattern.matcher(area[1]);
                        if (matcher.find()) {
                            areaNum = matcher.group();
                        }
//                        Log.d("TeidenApp", "areaNum = " + areaNum);
                        if (areaNum != null) {
                            // 念を押して、<br>を削除するようにした
                            areaNum = areaNum.replace("<br>", "");
                            areaNum = areaNum.replace("<br />", "");
                            int intAreaNum = Integer.valueOf(areaNum);
                            if (!groups.contains(intAreaNum)) {
                                groups.add(intAreaNum);
                                areaMap.put(areaName, groups);
                            }
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
       return areaMap;
    }
    
    /**
     * 非同期で検索する
     *
     */
    private class SearchAsyncTask extends AsyncTask<String, Void, HashMap<String, ArrayList<Integer>>> {

        private ProgressDialog mProgress;
        private int mMode;
        private String mStrAddress;
        
        public SearchAsyncTask(int mode, String strAddress) {
            mMode = mode;
            mStrAddress = strAddress;
        }
        
        @Override
        protected HashMap<String, ArrayList<Integer>> doInBackground(String... params) {
            String pref = params[0];
            return getTeidenInfo(TEIDEN_URL_LIST.get(pref));
        }

        @Override
        protected void onPostExecute(HashMap<String, ArrayList<Integer>> areaMap) {
            TextView groupNumber = null;
            TextView teidenSpan = null;
            String pref = null, address = null;
            switch (mMode) {
                case TEIDEN_LOCATION:
                    groupNumber = (TextView) findViewById(R.id.groupNumber);
                    teidenSpan = (TextView) findViewById(R.id.teidenSpan);
                    Button btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
                    btnGetMyLocation.setEnabled(true);
                    btnGetMyLocation.setText(R.string.get_my_location);
                    break;
                    
                case TEIDEN_SEARCH:
                    groupNumber = (TextView) findViewById(R.id.searchGroupNumber);
                    teidenSpan = (TextView) findViewById(R.id.searchTeidenSpan);
                    // フォームから住所を取得
                    pref = (String) mSpnPref.getSelectedItem();
                    address = hankakuToZenkaku(mEditAddress.getText().toString());
                    // 半角英数を全角英数へ
                    mStrAddress = pref + address;
                    break;
            }
            
            boolean hit_flg = false;
            SimpleDateFormat fmt = new SimpleDateFormat("MM月dd日(E)");
            Iterator<String> iterator = areaMap.keySet().iterator();
            while (iterator.hasNext()) {
                String areaName = (String) iterator.next();
                Pattern pattern = Pattern.compile(areaName);
                Matcher matcher = pattern.matcher(mStrAddress);
                if (matcher.find()) {
                    hit_flg = true;
                    ArrayList<Integer> groups = areaMap.get(areaName);
                    StringBuilder groupBuilder = new StringBuilder();
                    StringBuilder teidenBuilder = new StringBuilder();
                    for (Integer group : groups) {
                        // グループ情報を記述
                        groupBuilder.append(areaName + " " + getString(R.string.dai) + group + getString(R.string.group) + "\n");
                    }
                    
                    for (int dayOfYear = mCurrentDayOfYear; dayOfYear <= mMaxKnownDayOfYear; dayOfYear++) {
                        mCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
                        int day = mCalendar.get(Calendar.DATE);
//                        teidenBuilder.append("" + mMonth + getString(R.string.month) + day + getString(R.string.day) + "\n");
                        teidenBuilder.append(fmt.format(mCalendar.getTime()) + "\n");
                        for (Integer group : groups) {
                            // 日付によってグループの停電時刻が変わるのに対応する！
                            String teidenGroupSpan = "";
                            switch (day) {
                                case 15:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_15)[group - 1];
                                    break;
                                    
                                case 16:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_16)[group - 1];
                                    break;
                                    
                                case 17:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_17)[group - 1];
                                    break;
                                    
                                case 18:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_18)[group - 1];
                                    break;
                                    
                                case 19:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_19)[group - 1];
                                    break;
                                    
                                case 20:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_20)[group - 1];
                                    break;
                                    
                                case 21:
                                    teidenGroupSpan = getResources().getStringArray(R.array.teiden_group_3_21)[group - 1];
                                    break;
                            }
                            // グループ名を前に記述
                            teidenBuilder.append(getString(R.string.dai) + group + getString(R.string.group) + "\n");
                            teidenBuilder.append(teidenGroupSpan + "\n");
                        }
                        teidenBuilder.append("\n");
                    }
                    if (groupNumber != null) {
                        groupNumber.setText(groupBuilder.toString());
                        if (teidenSpan != null) {
                            if (teidenBuilder.toString().equals("")) {
                                teidenSpan.setText(R.string.teiden_span_unknown);
                            } else {
                                teidenSpan.setText(teidenBuilder.toString());
                                // TODO 履歴をDBに保存する
                                mDBHelper.insertHistories(
                                        groupBuilder.toString()
                                        + "\n"
                                        + teidenBuilder.toString()
                                );
                                // ヒットしたパターンだけ、入力履歴に保存
                                if (pref != null && address != null) {
                                    // 入力履歴から復元したパターンは除外して保存
                                    if (!(pref.equals(mInputHistoryPref)
                                            && address.equals(mInputHistoryAddress))) {
                                        mDBHelper.insertInputHistories(pref, address);
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if (!hit_flg) {
                // エリア外の住所
                if (groupNumber != null && teidenSpan != null) {
                    groupNumber.setText(R.string.out_of_area);
                    teidenSpan.setText(R.string.out_of_area);
                }
            }
            mProgress.dismiss();
        }

        @Override
        protected void onCancelled() {
            TextView groupNumber = null;
            TextView teidenSpan = null;
            switch (mMode) {
                case TEIDEN_LOCATION:
                    groupNumber = (TextView) findViewById(R.id.groupNumber);
                    teidenSpan = (TextView) findViewById(R.id.teidenSpan);
                    ((Button) findViewById(R.id.btnGetMyLocation)).setEnabled(true);
                    break;
                    
                case TEIDEN_SEARCH:
                    groupNumber = (TextView) findViewById(R.id.searchGroupNumber);
                    teidenSpan = (TextView) findViewById(R.id.searchTeidenSpan);
                    break;
            }
            // エリア外の住所
            if (groupNumber != null && teidenSpan != null) {
                groupNumber.setText("");
                teidenSpan.setText("");
            }
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            // プログレスダイアログ表示
            mProgress = new ProgressDialog(Top.this);
            switch(mMode) {
                case TEIDEN_LOCATION:
                    mProgress.setMessage(getString(R.string.data_loading));
                    break;
                    
                case TEIDEN_SEARCH:
                    mProgress.setMessage(getString(R.string.address_loading));
                    break;
            }
            mProgress.setIcon(android.R.drawable.ic_dialog_info);
            mProgress.setIndeterminate(false);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.show();
        }

    }
    
    /**
     * 全角を半角に変換
     * @param value
     * @return
     */
    private String zenkakuToHankaku(String value) {
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < sb.length(); i++) {
            int c = (int) sb.charAt(i);
            if ((c >= 0xFF10 && c <= 0xFF19) || (c >= 0xFF21 && c <= 0xFF3A) || (c >= 0xFF41 && c <= 0xFF5A)) {
                sb.setCharAt(i, (char) (c - 0xFEE0));
            }
        }
        value = sb.toString();
        return value;
    }
    
    /**
     * 半角を全角に変換
     * @param value
     * @return
     */
    private String hankakuToZenkaku(String value) {
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < sb.length(); i++) {
            int c = (int) sb.charAt(i);
            if ((c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A)) {
                sb.setCharAt(i, (char) (c + 0xFEE0));
            }
        }
        value = sb.toString();
        return value;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                // 結果を共有する
                share();
                break;
                
            case R.id.menu_finish:
                finish();
                break;
                
            case R.id.menu_about_app:
                about_app();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * このアプリについてを表示する
     */
    private void about_app() {
        Intent intent = new Intent(this, About.class);
        startActivity(intent);
    }

    /**
     * 共有機能
     */
    private void share() {
        StringBuilder builder = new StringBuilder();
        TextView groupNumber;
        TextView teidenSpan;
        String current = mTabs.getCurrentTabTag();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (current == "tag1") {
            // 現在地
            groupNumber = (TextView) findViewById(R.id.groupNumber);
            teidenSpan = (TextView) findViewById(R.id.teidenSpan);
        } else if (current == "tag2") {
            // 検索
            groupNumber = (TextView) findViewById(R.id.searchGroupNumber);
            teidenSpan = (TextView) findViewById(R.id.searchTeidenSpan);
        } else {
            // 履歴
            Toast.makeText(this, R.string.caution_select_row, Toast.LENGTH_SHORT).show();
            return;
        }
        if (groupNumber.getText().toString().equals("")
                || groupNumber.getText().toString().equals(getString(R.string.address_loading))
                || groupNumber.getText().toString().equals(getString(R.string.out_of_area))) {
            Toast.makeText(this, R.string.please_show_result, Toast.LENGTH_LONG).show();
        } else {
            builder.append(groupNumber.getText())
            .append("\n")
            .append(teidenSpan.getText())
            .append("\n\n")
            .append(getString(R.string.cm_app));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_subject));
            intent.putExtra(Intent.EXTRA_TEXT, builder.toString());
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_top, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    /**
     * 現在地を取得ボタンが押された場合の挙動
     * @param v
     */
    public void getMyLocation(View v) {
        // GPSのリスナーを登録
        mLocationProvider = null;   // 一旦初期化
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        } else if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationProvider = mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER);
        } else {
            // SIMなし、かつ、GPSがOFF
            noProviderEnabled();
        }
        if (mLocationProvider != null) {
            mLocationManager.requestLocationUpdates(
                    mLocationProvider.getName(),
                    0,
                    0,
                    this);
        }
        // 取得中に変更
        Button btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
        btnGetMyLocation.setEnabled(false);
        btnGetMyLocation.setText(R.string.now_location_loading);
        ((TextView) findViewById(R.id.currentAddress)).setText(R.string.address_loading);
        ((TextView) findViewById(R.id.groupNumber)).setText(R.string.address_loading);
        ((TextView) findViewById(R.id.teidenSpan)).setText(R.string.address_loading);
        // 現在地取得中を表示
//        new LocationAsyncTask().execute();
    }
    
    /**
     * 画面回転対策
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 検索履歴を表示する
     * @param v
     */
    public void getHistory(View v) {
        ListView list = (ListView) findViewById(android.R.id.list);
        mCursor = mDBHelper.getAll();
        startManagingCursor(mCursor);
        mAdapter = new HistoryAdapter(this, mCursor);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View c, int position, long id) {
                mCursor.moveToPosition(position);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        Top.this,
                        android.R.layout.select_dialog_item,
                        getResources().getStringArray(R.array.dialog_menu_history)
                );
                AlertDialog dialog = new AlertDialog.Builder(Top.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    // 共有
                                    StringBuilder builder = new StringBuilder();
                                    builder.append(mCursor.getString(mCursor.getColumnIndex(Histories.HISTORY)))
                                    .append("\n\n")
                                    .append(getString(R.string.cm_app));
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("text/plain");
                                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                                    intent.putExtra(Intent.EXTRA_TEXT, builder.toString());
                                    startActivity(intent);
                                    dialog.dismiss();
                                    break;
                                    
                                case 1:
                                    // 履歴の削除
                                    long id = mCursor.getLong(mCursor.getColumnIndex(Histories.ID));
                                    mDBHelper.delete(id);
                                    mCursor = mDBHelper.getAll();
                                    mAdapter.changeCursor(mCursor);
                                    Toast.makeText(Top.this, R.string.delete_history, Toast.LENGTH_SHORT).show();
                                    break;
                                    
                                case 2:
                                    // キャンセル
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    }).create();
                dialog.show();
            }
        });
    }
    
    
    /**
     * 検索履歴用のカーソルアダプタ
     */
    private class HistoryAdapter extends CursorAdapter {

        public HistoryAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView txtHistroy = (TextView)view.findViewById(R.id.history);
            TextView txtCreated = (TextView)view.findViewById(R.id.created);
            
            txtHistroy.setText(cursor.getString(cursor.getColumnIndex(Histories.HISTORY)));
            txtCreated.setText(
                    getString(R.string.search_date)
                    + cursor.getString(cursor.getColumnIndex(Histories.CREATED))
            );
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflator = LayoutInflater.from(context);
            View v = inflator.inflate(R.layout.row_history, parent, false);
            bindView(v, context, cursor);
            return v;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_INPUT_HISTORY:
                if (resultCode == RESULT_OK) {
                    Bundle b = data.getExtras();
                    mInputHistoryPref = b.getString(InputHistories.PREF);
                    mInputHistoryAddress = b.getString(InputHistories.ADDRESS);
                    String[] prefs = getResources().getStringArray(R.array.prefs);
                    for (int i = 0; i < prefs.length; i++) {
                        if (prefs[i].equals(mInputHistoryPref)) {
                            mSpnPref.setSelection(i);
                            break;
                        }
                    }
                    mEditAddress.setText(mInputHistoryAddress);
                }
                break;
        }
    }
    
    /**
     * 入力履歴を取得するActivityを呼び出す
     * @param v
     */
    public void showInputHistory(View v) {
        Intent intent = new Intent(this, InputHistory.class);
        startActivityForResult(intent, REQ_INPUT_HISTORY);
    }
}