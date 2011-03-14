package com.okolabo.android.teidennotify;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class Top extends Activity implements LocationListener{
    
    private static final HashMap<String, String> TEIDEN_URL_LIST;
    
    
    /** 位置情報を見るマネージャー */
    protected LocationManager mLocationManager;
    
    /** 位置情報プロバイダ(GPS or Network) */
    protected LocationProvider mLocationProvider;
    
    /** 現在地 */
    protected Location mLocation;

    /**  */
    protected Geocoder mGeocoder;
    
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
    protected void onResume() {
        super.onResume();
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
    }

    /**
     * 使えるプロバイダーがない
     */
    private void noProviderEnabled() {
        
    }

    
    public void onProviderDisabled(String provider) {} // 今回は実装しない

    public void onProviderEnabled(String provider) {} // 今回は実装しない

    
    /**
     * 位置情報が取得できた
     */
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
        // TODO 位置情報を住所に変換
        TextView currentAddress = (TextView) findViewById(R.id.currentAddress);
        TextView groupNumber = (TextView) findViewById(R.id.groupNumber);
        TextView teidenSpan = (TextView) findViewById(R.id.teidenSpan);
        try {
            List<Address> list_address = mGeocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 10);
            Address address = list_address.get(0);
            StringBuilder builder = new StringBuilder();
            String buf;
            for (int i = 1; (buf = address.getAddressLine(i)) != null; i++) {
                builder.append(buf);
            }
            String strAddress = builder.toString();
            // テスト
            // strAddress = "神奈川県秦野市今川町";
            currentAddress.setText(strAddress);
            
            // TODO 住所がどのエリアであるか照合する
            Iterator<String> prefIterator = TEIDEN_URL_LIST.keySet().iterator();
            HashMap<String, ArrayList<Integer>> areaMap = new HashMap<String, ArrayList<Integer>>();
            while (prefIterator.hasNext()) {
                String pref = (String) prefIterator.next();
                Pattern pattern = Pattern.compile(pref);
                Matcher matcher = pattern.matcher(strAddress);
                if (matcher.find()) {
                    areaMap = getTeidenInfo(TEIDEN_URL_LIST.get(pref));
                    break;
                }
            }
            if (areaMap.isEmpty()) {
                // エリア外の住所
                groupNumber.setText(R.string.out_of_area);
                teidenSpan.setText(R.string.out_of_area);
            } else {
                Iterator<String> iterator = areaMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String areaName = (String) iterator.next();
                    Pattern pattern = Pattern.compile(areaName);
                    Matcher matcher = pattern.matcher(strAddress);
                    if (matcher.find()) {
                        ArrayList<Integer> groups = areaMap.get(areaName);
                        StringBuilder groupBuilder = new StringBuilder();
                        StringBuilder teidenBuilder = new StringBuilder();
                        for (Integer group : groups) {
                            groupBuilder.append(areaName + " " + getString(R.string.dai) + group + getString(R.string.group) + "\n");
                            teidenBuilder.append(getResources().getStringArray(R.array.teiden_group)[group - 1] + "\n");
                        }
                        groupNumber.setText(groupBuilder.toString());
                        teidenSpan.setText(teidenBuilder.toString());
                        break;
                    }
                }
            }
            
            // TODO エリアの停電時間を渡す。できればGoogleカレンダーに登録
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
                String[] areas = newsBody.split("<br />\n");
                for (int i = 1; i < areas.length; i++) {
                    String[] area = areas[i].split("　");
                    if (area.length > 1) {
                        String[] areaNames = area[0].split(" ");
                        StringBuilder areaNameBuilder = new StringBuilder();
                        for (String areaName: areaNames) {
                            areaNameBuilder.append(areaName);
                        }
                        String areaName = areaNameBuilder.toString();
                        ArrayList<Integer> groups;
                        if (areaMap.containsKey(areaName)) {
                            groups = areaMap.get(areaName);
                        } else {
                            groups = new ArrayList<Integer>();
                        }
                        groups.add(Integer.valueOf(area[1]));
                        areaMap.put(areaName, groups);
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
}