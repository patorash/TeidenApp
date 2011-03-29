package com.okolabo.android.teidennotify;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.kayo.android.exceptionlib.ExceptionBinder;
import jp.co.nobot.libAdMaker.libAdMaker;
import net.it4myself.util.RestfulClient;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.okolabo.android.teidennotify.DatabaseHelper.Histories;
import com.okolabo.android.teidennotify.DatabaseHelper.InputHistories;
import com.okolabo.android.teidennotify.DatabaseHelper.LocationHistories;

public class Top extends Activity implements LocationListener{
    
    // 現在地と地名検索で処理をわけるためのフラグ
    private static final int TEIDEN_LOCATION = 1;
    private static final int TEIDEN_SEARCH = 2;
    
    // 入力履歴のリクエストコード
    private static final int REQ_INPUT_HISTORY = 1;
    // 現在地履歴のリクエストコード
    private static final int REQ_LOCATION_HISTORY = 2;
    // 郵便番号検索のリクエストコード
    private static final int REQ_SEARCH_ZIPCODE = 3;
    // 住所入力のリクエストコード
    private static final int REQ_SUGGEST_ADDRESS = 4;
    // 地図での場所設定のリクエストコード
    private static final int REQ_PLACE = 5;
    
    
    private static final HashMap<String, String> TEIDEN_URL_LIST;
    
    /** 住所を渡したら所属グループが取得できるWebAPIのURL */
    private static final String URL_GROUP = "http://prayforjapanandroid.appspot.com/api/group";
    
    /** 停電スケジュールをjsonで取得できるWebAPIのURL */
    private static final String URL_SCHEDULE = "http://prayforjapanandroid.appspot.com/api/schedule";
    
    /** 位置情報を見るマネージャー */
    protected LocationManager mLocationManager;
    
    /** 位置情報プロバイダ(GPS or Network) */
    protected LocationProvider mLocationProvider;
    
    /** 現在地 */
    protected Location mLocation;

    /**  */
    protected Geocoder mGeocoder;
    
    /** 都県のスピナー */
    private Spinner mSpnPref;
    /** 都県以降の住所を入力するエリア */
    private EditText mEditAddress;
    /** グループのスピナー */
    private Spinner mSpnGroup;
    
    private TabHost mTabs;
    
    private Calendar mCalendar;
    
    private libAdMaker mAdView;
    
    /** 今日の日付(365日で) */
    private int mCurrentDayOfYear;
    
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
    
    /** 停電スケジュールをキャッシュするJSONObject */
    private JSONObject mSchedule = null;
    
    /** サブグループ */
    private static final String[] mSubGroups = new String[]{
        "",
        "1-A",
        "1-B",
        "1-C",
        "1-D",
        "1-E",
        "2-A",
        "2-B",
        "2-C",
        "2-D",
        "2-E",
        "3-A",
        "3-B",
        "3-C",
        "3-D",
        "3-E",
        "4-A",
        "4-B",
        "4-C",
        "4-D",
        "4-E",
        "5-A",
        "5-B",
        "5-C",
        "5-D",
        "5-E"
    };
    
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
        // 今日の日付(365日で) 月が変わっても対応できるように。
        mCurrentDayOfYear = mCalendar.get(Calendar.DAY_OF_YEAR);
        
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
        
        // グループスケジュールタブ
        spec = mTabs.newTabSpec("tag4");
        spec.setContent(R.id.tab4);
        spec.setIndicator(getString(R.string.tab_group));
        mTabs.addTab(spec);
        
        // 地域検索の都県のスピナー
        mSpnPref = (Spinner) findViewById(R.id.pref);
        ArrayAdapter<String> aa = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.prefs));
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnPref.setAdapter(aa);
        
        // 地域検索の都県以降の住所。直接修正はできない。
        mEditAddress = (EditText) findViewById(R.id.address);
        mEditAddress.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                showSuggestAddress();
            }
        });
        
        // 検索ボタン
        ImageButton btnSearch = (ImageButton) findViewById(R.id.search);
        btnSearch.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                addressSearch();
            }
        });
        
        // グループ停電予定のスピナー
        mSpnGroup = (Spinner) findViewById(R.id.group);
        ArrayAdapter<String> ag = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.groups));
        ag.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnGroup.setAdapter(ag);
        mSpnGroup.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position > 0) {
                    new GroupScheduleAsyncTask(mSubGroups[position]).execute();
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // 広告表示に協力してくださる方のみに広告を表示
        boolean cmFlg = Prefs.getCmFlg(this);
        if (cmFlg) {
            showCm();
        }

        // 初回表示だったら、アラートダイアログで広告表示に協力してくれるか聞く
        boolean confirmFlg = Prefs.getConfirmFlg(this);
        if (!confirmFlg) {
            showConfirmCm();
        }
    }
    
    /**
     * 地域検索を行う
     */
    private void addressSearch() {
        // 非同期で検索
        // フォームから住所を取得
        String pref = (String)mSpnPref.getSelectedItem();
        String address = hankakuToZenkaku(mEditAddress.getText());
        // 半角英数を全角英数へ
        String strAddress = pref + address;
        new SearchAsyncTask(TEIDEN_SEARCH, strAddress).execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // GPSのリスナーを解除
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        Button btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
        btnGetMyLocation.setEnabled(true);
        btnGetMyLocation.setText(R.string.get_my_location);
        ((TextView) findViewById(R.id.currentAddress)).setText("");
        ((TextView) findViewById(R.id.groupNumber)).setText("");
        ((TextView) findViewById(R.id.teidenSpan)).setText("");
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
        try {
            List<Address> list_address = mGeocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 10);
            Address address = list_address.get(0);
            StringBuilder builder = new StringBuilder();
            String buf;
            for (int i = 1; (buf = address.getAddressLine(i)) != null; i++) {
                builder.append(buf);
            }
            // 以降、別メソッドにする
            setCurrentLocationInfo(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 現在地の情報を取得して画面に反映
     * 現在地の履歴から取得可能にするために、住所を引数にしたメソッドにした
     * @param address
     */
    private void setCurrentLocationInfo(String address) {
        TextView currentAddress = (TextView) findViewById(R.id.currentAddress);
        // TODO テストの際に使う このTODOはテストのときにここを見つけるために付けてます。
        // グループが1つの住所
//        address = "東京都調布市小島町２丁目XX−X";
        // グループが複数の住所
//            address = "神奈川県相模原市南区相南１丁目";
        currentAddress.setText(address);
        
        // 住所がどのエリアであるか照合する
        Iterator<String> prefIterator = TEIDEN_URL_LIST.keySet().iterator();
        String hitPref = null;
        while (prefIterator.hasNext()) {
            String pref = (String) prefIterator.next();
            Pattern pattern = Pattern.compile(pref);
            Matcher matcher = pattern.matcher(address);
            if (matcher.find()) {
                hitPref = pref;
                break;
            }
        }
        if (hitPref != null) {
            // 非同期でデータを取得
            new SearchAsyncTask(TEIDEN_LOCATION, address).execute();
        } else {
            // 対象の都道府県以外なので、計画停電エリア外と表示
            Button btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
            btnGetMyLocation.setEnabled(true);
            btnGetMyLocation.setText(R.string.get_my_location);
            ((TextView) findViewById(R.id.groupNumber)).setText(R.string.out_of_area);
            ((TextView) findViewById(R.id.teidenSpan)).setText(R.string.out_of_area);
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
    
    
//    protected HashMap<String, ArrayList<Integer>> getTeidenInfo(String url) {
//        // 住所がどのエリアであるか照合する
//        Source source;
//        HashMap<String, ArrayList<Integer>> areaMap = new HashMap<String, ArrayList<Integer>>();
//        try {
//            // 今は毎日新聞からデータ取ってる。ここをGAEに切り替える
//            source = new Source(new URL(url));
//            List<Element> elementList = source.getAllElementsByClass("NewsBody");
////          List<Element> elementList=source.getAllElements();
//            for (Element element : elementList) {
//                String newsBody = element.toString();
////                Log.d("TeidenApp", newsBody);
//                String[] areas = newsBody.split("<br />|<br>");
//                for (int i = 1; i < areas.length; i++) {
//                    String[] area = areas[i].trim().split("　");
//                    if (area.length > 1) {
//                        String[] areaNames = area[0].split(" ");
//                        StringBuilder areaNameBuilder = new StringBuilder();
//                        for (String areaName: areaNames) {
//                            areaNameBuilder.append(areaName);
//                        }
//                        String areaName = areaNameBuilder.toString();
//                        // areaName から「大字」を除去する
//                        areaName = areaName.replaceAll("大字", "");
//                        // areaName から「の一部」を除去する
//                        areaName = areaName.replaceAll("の一部", "");
//                        ArrayList<Integer> groups;
//                        if (areaMap.containsKey(areaName)) {
//                            groups = areaMap.get(areaName);
//                        } else {
//                            groups = new ArrayList<Integer>();
//                        }
//                        area[1] = zenkakuToHankaku(area[1]);
////                        Log.d("TeidenApp", area[1]);
//                        String areaNum = null;
//                        Pattern pattern = Pattern.compile("\\d");
//                        Matcher matcher = pattern.matcher(area[1]);
//                        if (matcher.find()) {
//                            areaNum = matcher.group();
//                        }
////                        Log.d("TeidenApp", "areaNum = " + areaNum);
//                        if (areaNum != null) {
//                            // 念を押して、<br>を削除するようにした
//                            areaNum = areaNum.replace("<br>", "");
//                            areaNum = areaNum.replace("<br />", "");
//                            int intAreaNum = Integer.valueOf(areaNum);
//                            if (!groups.contains(intAreaNum)) {
//                                groups.add(intAreaNum);
//                                areaMap.put(areaName, groups);
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//       return areaMap;
//    }
    
    /**
     * 非同期で検索する
     *
     */
    private class SearchAsyncTask extends AsyncTask<Void, Void, HashMap<String, ArrayList<String>>> {

        private ProgressDialog mProgress;
        private int mMode;
        private String mStrAddress;
        
        public SearchAsyncTask(int mode, String strAddress) {
            mMode = mode;
            mStrAddress = strAddress;
        }
        
        @Override
        protected HashMap<String, ArrayList<String>> doInBackground(Void... params) {
            String response;
            // スケジュールのキャッシュがなければ、GAEからDLする
            if (mSchedule == null) {
                try {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("v", Version.SCHEDULE_3);
                    response = RestfulClient.Get(URL_SCHEDULE, map);
                    mSchedule = new JSONObject(response);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            // 住所がどのグループに属しているかGAEに問い合わせる
            HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
            try {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("address", mStrAddress);
                map.put("v", Version.GROUP_2);
                response = RestfulClient.Get(URL_GROUP, map);
                JSONObject json = new JSONObject(response);
                ArrayList<String> groups = new ArrayList<String>();
                JSONArray groupsJson = json.getJSONArray("groups");
                for (int i = 0; i < groupsJson.length(); i++) {
                    groups.add(groupsJson.getString(i));
                }
                result.put(json.getString("address"), groups);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            return getTeidenInfo(TEIDEN_URL_LIST.get(pref));
            return result;
        }

        @Override
        protected void onPostExecute(HashMap<String, ArrayList<String>> areaMap) {
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
                    // 入力履歴を保存するため
                    pref = (String) mSpnPref.getSelectedItem();
                    address = hankakuToZenkaku(mEditAddress.getText());
//                    // 半角英数を全角英数へ
//                    mStrAddress = pref + address;
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
                    ArrayList<String> groups = areaMap.get(areaName);
                    StringBuilder groupBuilder = new StringBuilder();
                    StringBuilder teidenBuilder = new StringBuilder();
                    groupBuilder.append(areaName + "\n");
                    for (String group : groups) {
                        // グループ情報を記述
                        groupBuilder.append(getString(R.string.dai) + group + getString(R.string.group) + "\n");
                    }
                    
                    // 日付があるだけスケジュールを表示する
                    JSONObject date;
                    try {
                        date = mSchedule.getJSONObject("date");
                        for (int dayOfYear = mCurrentDayOfYear; dayOfYear < mCurrentDayOfYear + date.length(); dayOfYear++) {
                            mCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
                            String strDate = fmt.format(mCalendar.getTime());
                            JSONObject schedules = date.getJSONObject(strDate);
                            teidenBuilder.append(strDate + "\n");
                            for (String group : groups) {
                                // 日付によってグループの停電時刻が変わるのに対応する！
                                StringBuilder teidenGroupSpan = new StringBuilder();
                                JSONArray teidens = schedules.getJSONArray(group);
                                for (int i = 0; i < teidens.length(); i++) {
                                    JSONObject teiden = teidens.getJSONObject(i);
                                    teidenGroupSpan.append(teiden.getString("start"))
                                        .append("～")
                                        .append(teiden.getString("end"))
                                        .append(teiden.getString("note"))
                                        .append("\n");
                                }
                                // グループ名を前に記述
                                teidenBuilder.append(getString(R.string.dai) + group + getString(R.string.group) + "\n");
                                teidenBuilder.append(teidenGroupSpan.toString());
                            }
                            teidenBuilder.append("\n");
                        }
                    } catch (JSONException e) {
                        // date.getJSONArray(strDate)で例外が発生するが、無視する
                        // e.printStackTrace();
                    }
                    
                    if (groupNumber != null) {
                        groupNumber.setText(groupBuilder.toString());
                        if (teidenSpan != null) {
                            if (teidenBuilder.toString().equals("")) {
                                teidenSpan.setText(R.string.teiden_span_unknown);
                            } else {
                                teidenSpan.setText(teidenBuilder.toString());
                                // 履歴をDBに保存する
                                String history = groupBuilder.toString()
                                                    + "\n"
                                                    + teidenBuilder.toString();
                                // 同じ内容の履歴を削除
                                mDBHelper.deleteByHistory(history);
                                mDBHelper.insertHistories(history);
                                switch (mMode) {
                                    case TEIDEN_LOCATION:
                                        if (!mDBHelper.existsLocation(mStrAddress)) {
                                            // まだ保存されていない住所
                                            mDBHelper.insertLocationHistories(mStrAddress);
                                        }
                                        break;
                                        
                                    case TEIDEN_SEARCH:
                                        // ヒットしたパターンだけ、入力履歴に保存
                                        if (pref != null && address != null) {
                                            // 前のを消して、再インサート
                                            mDBHelper.deleteInputHistoryByPrefAndAddress(pref, address);
                                            mDBHelper.insertInputHistories(pref, address);
                                        }
                                        break;
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
    @SuppressWarnings("unused")
    private String zenkakuToHankaku(CharSequence value) {
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < sb.length(); i++) {
            int c = (int) sb.charAt(i);
            if ((c >= 0xFF10 && c <= 0xFF19) || (c >= 0xFF21 && c <= 0xFF3A) || (c >= 0xFF41 && c <= 0xFF5A)) {
                sb.setCharAt(i, (char) (c - 0xFEE0));
            }
        }
        final String result = sb.toString();
        return result;
    }
    
    /**
     * 半角を全角に変換
     * @param value
     * @return
     */
    private String hankakuToZenkaku(CharSequence value) {
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < sb.length(); i++) {
            int c = (int) sb.charAt(i);
            if ((c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A)) {
                sb.setCharAt(i, (char) (c + 0xFEE0));
            }
        }
        final String result = sb.toString();
        return result;
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
                
            case R.id.menu_cm_on_off:
                // CMのON/OFFを変更する
                showConfirmCm();
                break;
                
            case R.id.menu_call_touden:
                // 東京電力に電話をかける
                callTouden();
                break;
                
            case R.id.menu_support_app:
                showSupportApp();
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
        } else if (current == "tag3"){
            // 履歴
            Toast.makeText(this, R.string.caution_select_row, Toast.LENGTH_SHORT).show();
            return;
        } else {
            // グループ別
            if (mSpnGroup.getSelectedItemPosition() > 0) {
                teidenSpan = (TextView) findViewById(R.id.groupTeidenSpan);
                builder.append(teidenSpan.getText())
                       .append("\n\n")
                       .append(getString(R.string.cm_app));
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_subject));
                intent.putExtra(Intent.EXTRA_TEXT, builder.toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.caution_select_group, Toast.LENGTH_SHORT).show();
            }
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
            // 取得中に変更
            Button btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
            btnGetMyLocation.setEnabled(false);
            btnGetMyLocation.setText(R.string.now_location_loading);
            ((TextView) findViewById(R.id.currentAddress)).setText(R.string.address_loading);
            ((TextView) findViewById(R.id.groupNumber)).setText(R.string.address_loading);
            ((TextView) findViewById(R.id.teidenSpan)).setText(R.string.address_loading);
        }
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
                
            case REQ_LOCATION_HISTORY:
                if (resultCode == RESULT_OK) {
                    Bundle b = data.getExtras();
                    String locationHistoryAddress = b.getString(LocationHistories.ADDRESS);
                    setCurrentLocationInfo(locationHistoryAddress);
                }
                break;
                
            case REQ_SEARCH_ZIPCODE:
                if (resultCode == RESULT_OK) {
                    Bundle b = data.getExtras();
                    boolean hit_flg = false;
                    final String pref = b.getString(Constants.EXTRA_PREF);
                    final String address = b.getString(Constants.EXTRA_ADDRESS);
                    String[] prefs = getResources().getStringArray(R.array.prefs);
                    for (int i = 0; i < prefs.length; i++) {
                        if (prefs[i].equals(pref)) {
                            mSpnPref.setSelection(i);
                            hit_flg = true;
                            break;
                        }
                    }
                    if (hit_flg) {
                        mEditAddress.setText(address);
                    } else {
                        Toast.makeText(this, R.string.no_hit_zipcode, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
                
            case REQ_SUGGEST_ADDRESS:
                if (resultCode == RESULT_OK) {
                    Bundle b = data.getExtras();
                    final String pref = b.getString("pref");
                    final String address = b.getString("address");
                    String[] prefs = getResources().getStringArray(R.array.prefs);
                    for (int i = 0; i < prefs.length; i++) {
                        if (prefs[i].equals(pref)) {
                            mSpnPref.setSelection(i);
                            break;
                        }
                    }
                    mEditAddress.setText(address);
//                    addressSearch();
                }
                break;
                
            case REQ_PLACE:
                if (resultCode == RESULT_OK) {
                    Bundle b = data.getExtras();
                    String locationHistoryAddress = b.getString("address");
                    setCurrentLocationInfo(locationHistoryAddress);
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
    
    /**
     * 現在地履歴を取得するActivityを呼び出す
     * @param v
     */
    public void showLocationHistory(View v) {
        Intent intent = new Intent(this, LocationHistory.class);
        startActivityForResult(intent, REQ_LOCATION_HISTORY);
    }
    
    public void showZipSearch(View v) {
        try {
            final String zipcode = ((TextView)findViewById(R.id.zipcode)).getText().toString();
            Intent i = new Intent(Constants.ACTION_SEARCH);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.putExtra(Constants.EXTRA_ZIP, zipcode);
            startActivityForResult(i, REQ_SEARCH_ZIPCODE);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.zip_search)
                .setMessage(R.string.no_zip_search_description)
                .setPositiveButton(R.string.move, new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int which) {
                        // 英吉さんの郵便番号検索をDLしてもらうために、アンドロイドマーケットに誘導
                        Uri uri = Uri
                                .parse("market://details?id=luck.of.wise.zipsearch");
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
        }
    }
    
    /**
     * 住所入力補完するActivity SuggestAddressに移動する
     * @param v
     */
    public void showSuggestAddress() {
        Intent intent = new Intent(this, SuggestAddress.class);
        intent.putExtra("pref", (String)mSpnPref.getSelectedItem());
        intent.putExtra("address", mEditAddress.getText().toString());
        startActivityForResult(intent, REQ_SUGGEST_ADDRESS);
    }
    
    /**
     * 初回起動時に、広告表示への協力を聞くダイアログを表示する
     */
    private void showConfirmCm() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.implore)
                .setMessage(R.string.description_cm)
                .setPositiveButton(R.string.cooperate, new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = Prefs.get(Top.this);
                        Editor edit = prefs.edit();
                        edit.putBoolean(Prefs.KEY_CONFIRM, true);
                        edit.putBoolean(Prefs.KEY_CM, true);
                        edit.commit();
                        showCm();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = Prefs.get(Top.this);
                        Editor edit = prefs.edit();
                        edit.putBoolean(Prefs.KEY_CONFIRM, true);
                        edit.putBoolean(Prefs.KEY_CM, false);
                        edit.commit();
                        hideCm();
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }
    
    /**
     * 広告を表示する
     */
    private void showCm() {
        mAdView = null;
        mAdView = (libAdMaker)findViewById(R.id.admakerview);
        mAdView.setVisibility(libAdMaker.VISIBLE);
        mAdView.setActivity(this);
        mAdView.siteId = "212";
        mAdView.zoneId = "2123";
        mAdView.setUrl("http://images.ad-maker.info/apps/ibtwazn12a92.html");
        mAdView.setBackgroundColor(android.R.color.transparent);
        mAdView.start();
    }
    
    /**
     * 広告を非表示にする
     */
    private void hideCm() {
        if (mAdView != null) {
            mAdView.setVisibility(libAdMaker.GONE);
            mAdView = null;
        }
    }
    
    /**
     * 指定されたグループの停電情報を非同期で取得、表示する
     *
     */
    private class GroupScheduleAsyncTask extends AsyncTask<Void, Void, Void> {
        
        private ProgressDialog mProgress;
        private String mGroup;

        public GroupScheduleAsyncTask(String group) {
            this.mGroup = group;
        }
        
        @Override
        protected void onPreExecute() {
            // プログレスダイアログ表示
            this.mProgress = new ProgressDialog(Top.this);
            this.mProgress.setMessage(getString(R.string.teiden_loading));
            this.mProgress.setIcon(android.R.drawable.ic_dialog_info);
            this.mProgress.setIndeterminate(false);
            this.mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            this.mProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            String response;
            // スケジュールのキャッシュがなければ、GAEからDLする
            if (mSchedule == null) {
                try {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("v", Version.SCHEDULE_3);
                    response = RestfulClient.Get(URL_SCHEDULE, map);
                    mSchedule = new JSONObject(response);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // 日付があるだけスケジュールを表示する
            SimpleDateFormat fmt = new SimpleDateFormat("MM月dd日(E)");
            StringBuilder builder = new StringBuilder((String)mSpnGroup.getSelectedItem());
            builder.append("\n");
            JSONObject date;
            try {
                date = mSchedule.getJSONObject("date");
                for (int dayOfYear = mCurrentDayOfYear; dayOfYear < mCurrentDayOfYear + date.length(); dayOfYear++) {
                    mCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
                    String strDate = fmt.format(mCalendar.getTime());
                    JSONObject schedules = date.getJSONObject(strDate);
                    builder.append(strDate + "\n");
                    // 日付によってグループの停電時刻が変わるのに対応する！
                    StringBuilder teidenGroupSpan = new StringBuilder();
                    JSONArray teidens = schedules.getJSONArray(mGroup);
                    for (int i = 0; i < teidens.length(); i++) {
                        JSONObject teiden = teidens.getJSONObject(i);
                        teidenGroupSpan.append(teiden.getString("start"))
                            .append("～")
                            .append(teiden.getString("end"))
                            .append(teiden.getString("note"))
                            .append("\n");
                    }
                    builder.append(teidenGroupSpan.toString() + "\n");
                }
            } catch (JSONException e) {
                // date.getJSONArray(strDate)で例外が発生するが、無視する
                // e.printStackTrace();
            }
            ((TextView)findViewById(R.id.groupTeidenSpan)).setText(builder.toString());
            this.mProgress.dismiss();
        }
    }
    
    /**
     * 計画停電ご案内専用ダイヤルにかける
     */
    private void callTouden() {
        AlertDialog dialog = new AlertDialog.Builder(Top.this)
        .setIcon(android.R.drawable.ic_dialog_dialer)
        .setTitle(R.string.call_touden)
        .setMessage(R.string.call_touden_description)
        .setPositiveButton(R.string.call, new DialogInterface.OnClickListener() {
            
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:0120-925-433"));
                startActivity(intent);
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .create();
        dialog.show();
    }
    
    /**
     * 掲示板を表示する
     */
    private void showSupportApp() {
        Intent intent = new Intent(this, SupportBbs.class);
        startActivity(intent);
    }
    
    /**
     * 場所を取得する地図を表示する
     * @param v
     */
    public void showMap(View v) {
        Intent intent = new Intent(this, Place.class);
        startActivityForResult(intent, REQ_PLACE);
    }
    
    /**
     * カレンダーに計画停電のスケジュールを登録する
     * @param v
     */
    public void writeSchedule(View v) {
        if (mSchedule != null & mSpnGroup.getSelectedItemPosition() > 0) {
            Date date = new Date();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    new OnDateSetListener() {
                        
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            int version = Build.VERSION.SDK_INT;
                            final String calendarProvUri, eventProvUri, reminderProvUri;
                            if (version >= 8) {
                                calendarProvUri = "content://com.android.calendar/calendars";
                                eventProvUri    = "content://com.android.calendar/events";
                                reminderProvUri = "content://com.android.calendar/reminders";
                            } else {
                                calendarProvUri = "content://calendar/calendars";
                                eventProvUri    = "content://calendar/events";
                                reminderProvUri = "content://calendar/reminders";
                            }
                            
                            ContentResolver contentResolver = getContentResolver();
                            Date date = new Date(year - 1900, monthOfYear, dayOfMonth);
                            SimpleDateFormat fmt = new SimpleDateFormat("MM月dd日(E)");
                            // 指定された日付
                            String day = fmt.format(date);
                            // 指定されているグループを取得
                            String subGroup = mSubGroups[mSpnGroup.getSelectedItemPosition()];
                            try {
                                // カレンダーを取得する
                                String[] projection = new String[] { "_id", "name" };//idはプライマリーキー、nameはカレンダー名
                                String selection = "access_level" + "=?";
                                String[] selectionArgs = new String[] { "700" };
                                Cursor managedCursor = managedQuery(
                                        Uri.parse(calendarProvUri),
                                        projection,
                                        selection,
                                        selectionArgs,
                                        null);
                                int[] mCalIds;
                                String[] mCalNames;
                                if (managedCursor.moveToFirst()) {
                                    int len = managedCursor.getCount();
                                    mCalIds = new int[len];
                                    mCalNames = new String[len];

                                    int idColumnIndex = managedCursor.getColumnIndex("_id");
                                    int nameColumnIndex = managedCursor.getColumnIndex("name");

                                    int i = 0;
                                    do {
                                        mCalIds[i] = managedCursor.getInt(idColumnIndex);
                                        mCalNames[i] = managedCursor.getString(nameColumnIndex);
                                        i++;
                                    } while (managedCursor.moveToNext());
                                
                                    JSONArray teidens = mSchedule.getJSONObject("date").getJSONObject(day).getJSONArray(subGroup);
                                    for (int j = 0; j < teidens.length(); j++) {
                                        JSONObject teiden = teidens.getJSONObject(j);
                                        
                                        // 停電開始時刻を取得(ミリ秒で)
                                        String[] start = teiden.getString("start").split(":");
                                        date.setHours(Integer.valueOf(start[0]));
                                        date.setMinutes(Integer.valueOf(start[1]));
                                        long startDate = date.getTime();
                                        
                                        // 停電終了時刻を取得(ミリ秒で)
                                        String[] end = teiden.getString("end").split(":");
                                        date.setHours(Integer.valueOf(end[0]));
                                        date.setMinutes(Integer.valueOf(end[1]));
                                        long endDate = date.getTime();
                                        
                                        // 備考を取得(実施 or 中止)
                                        String note = teiden.getString("note");
                                        
                                        // 計画停電をカレンダーに登録する
                                        ContentValues cv = new ContentValues();
                                        cv.put("calendar_id", mCalIds[0]);
                                        cv.put("title", getString(R.string.plan_teiden) + note);
                                        cv.put("description", (String)mSpnGroup.getSelectedItem());
                                        // cv.put("eventLocation", "住所");
                                        cv.put("dtstart", startDate);//ミリ秒で指定
                                        cv.put("dtend", endDate);//ミリ秒で指定
                                        
                                        // 通知機能を使用する場合は1を設定する。使用しない場合は省略して良い
                                        if (!note.equals("(中止)")){
                                            // 実行される可能性があるので、10分前に通知
                                            cv.put("hasAlarm", 1);
                                            Uri eventUri = contentResolver.insert(Uri.parse(eventProvUri), cv);
                                            long rowId = Long.parseLong(eventUri.getLastPathSegment());
                                            cv = new ContentValues();
                                            cv.put("event_id", rowId);
                                            cv.put("method", 1);// 通知方法を指定する
                                            cv.put("minutes", 10);//分単位で指定する
                                            contentResolver.insert(Uri.parse(reminderProvUri), cv);
                                            Toast.makeText(Top.this, R.string.regist_calendar, Toast.LENGTH_LONG).show();
                                        } else {
                                            // 中止なので、登録はするが、通知はしない
                                            contentResolver.insert(Uri.parse(eventProvUri), cv);
                                            Toast.makeText(Top.this, R.string.regist_calendar_no_alarm, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                //e.printStackTrace();
                                Toast.makeText(Top.this, R.string.cannot_get_schedule, Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    date.getYear() + 1900,
                    date.getMonth(),
                    date.getDate());
            Toast.makeText(this, R.string.description_regist_calendar, Toast.LENGTH_LONG).show();
            dialog.show();
        } else {
            // まだグループが選択されていないのでカレンダー登録できない
            Toast.makeText(this, R.string.please_select_group, Toast.LENGTH_SHORT).show();
        }
   }
}