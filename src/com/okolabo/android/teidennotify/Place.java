
package com.okolabo.android.teidennotify;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.okolabo.android.util.DeployUtils;

public class Place extends MapActivity implements LocationListener, OnGestureListener,
        OnDoubleTapListener {

    private GeoPoint mGeoPoint;

    private GestureDetector mGDetector;

    private boolean mDoubleTap = false;

    private boolean mSingleTap = true;

    private boolean mButtonPush = false;

    private class IconOverlay extends Overlay {
        Bitmap mIcon;

        int mOffsetX;

        int mOffsetY;

        GeoPoint mPoint;

        public IconOverlay(Bitmap icon, GeoPoint point) {
            mIcon = icon;
            mOffsetX = 0 - icon.getWidth() / 2;
            mOffsetY = 0 - icon.getHeight();
            mPoint = point;

        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            super.draw(canvas, mapView, shadow);
            if (!shadow) {
                Projection projection = mapView.getProjection();
                Point point = new Point();
                projection.toPixels(mPoint, point);
                point.offset(mOffsetX, mOffsetY);
                canvas.drawBitmap(mIcon, point.x, point.y, null);
            }
        }

        @Override
        public boolean onTap(GeoPoint point, MapView mapView) {
            if (mSingleTap) {
                mPoint = point;
                return super.onTap(point, mapView);
            } else {
                return false;
            }
        }
    }

    static final String TAG = "Place";

    static final int INITIAL_ZOOM_LEVEL = 15;

    static final int INITIAL_LATITUDE = 35455281;

    static final int INITIAL_LONGITUDE = 139629711;

    private MapView mMapView;

    private Button btnMenuMyLocation;

    private Button btnMenuSearch;

    private Button btnMenuSettingPlace;

    private Geocoder mGeocoder;

    private LocationManager mLocationManager;

    private MapController mMapController;
    
    /**
     * 現在地を表示するオーバーレイ
     */
    private CustomMyLocationOverlay mMyLocation;

    private ProgressDialog mProgress;
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DeployUtils.isDebuggable(this)) {
            setContentView(R.layout.place_debug);
        } else {
            setContentView(R.layout.place);
        }

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);
        mMapController = mMapView.getController();
        ZoomButtonsController zbc = mMapView.getZoomButtonsController();
        zbc.setOnZoomListener(new OnZoomListener() {

            public void onZoom(boolean zoomIn) {
                mButtonPush = true;
                if (zoomIn) {
                    mMapController.zoomIn();
                } else {
                    mMapController.zoomOut();
                }
            }

            public void onVisibilityChanged(boolean visible) {

            }
        });
        
        // 現在地を取得する仕組み
        boolean requestByTop = getIntent().getBooleanExtra("myLocationRequest", false);
        mMyLocation = new CustomMyLocationOverlay(this, mMapView);
        mMyLocation.enableMyLocation();
        mMyLocation.setIsMyLocationRequestByTop(requestByTop);
        if (requestByTop) {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage(getString(R.string.now_location_loading));
            mProgress.setIcon(android.R.drawable.ic_dialog_map);
            mProgress.setIndeterminate(false);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.show();
        }

        btnMenuMyLocation = (Button) findViewById(R.id.btnMenuMyLocation);
        btnMenuSearch = (Button) findViewById(R.id.btnMenuSearch);
        btnMenuSettingPlace = (Button) findViewById(R.id.btnMenuSettingPlace);

        btnMenuMyLocation.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mButtonPush = true;
                menu_my_location();
            }
        });

        btnMenuSearch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mButtonPush = true;
                onSearchRequested();
            }
        });


        btnMenuSettingPlace.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mButtonPush = true;
                callFinish();
            }
        });

        mGeocoder = new Geocoder(getApplicationContext(), Locale.JAPAN);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            mGeoPoint = new GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE);
        } else {
            int latitude = extras.getInt("latitude");
            int longitude = extras.getInt("longitude");
            mGeoPoint = new GeoPoint(latitude, longitude);
        }

        mMapController.setCenter(mGeoPoint);
        mMapController.setZoom(INITIAL_ZOOM_LEVEL);

        // イメージを地図上に表示する
        setOverlay(mGeoPoint);
        mGDetector = new GestureDetector(this, this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return onTouchEvent(ev);
    }

    public void onLocationChanged(Location loc) {
//        GeoPoint p = mMyLocation.getMyLocation();
//        if (p != null) {
//            mGeoPoint = p;
//            mMapController.animateTo(mGeoPoint);
//            setOverlay(mGeoPoint);
//        } else {
//            // まだ記録されている位置情報がない
//            Toast.makeText(this, R.string.null_carrent_location, Toast.LENGTH_LONG).show();
//        }
    }

    private void setOverlay(GeoPoint point) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_pin);
        IconOverlay iconOverlay = new IconOverlay(icon, point);

        List<Overlay> overlays = mMapView.getOverlays();
        // 既に表示されているオーバーレイを消す
        overlays.clear();
        // オーバーレイを追加
        overlays.add(mMyLocation);
        overlays.add(iconOverlay);
    }

    // @Override
    protected void onResume() {
        if (mLocationManager != null) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    this);
        }
        mMyLocation.enableMyLocation();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        mMyLocation.disableMyLocation();
        super.onPause();
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void callFinish() {
        // GeoPointより住所を取得
        try {
            List<Address> list_address = mGeocoder.getFromLocation(
                    mGeoPoint.getLatitudeE6() / 1E6,
                    mGeoPoint.getLongitudeE6() / 1E6,
                    10);
            Address address = list_address.get(0);
            String strAddress = getAddressToString(address);
            Intent intent = new Intent();
            intent.putExtra("address", strAddress);
            setResult(RESULT_OK, intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    private void menu_my_location() {
        GeoPoint p = mMyLocation.getMyLocation();
        if (p != null) {
            mGeoPoint = p;
            mMapController.animateTo(mGeoPoint);
            setOverlay(mGeoPoint);
        } else {
            // まだ記録されている位置情報がない
            Toast.makeText(this, R.string.null_carrent_location, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    /**
     * 検索フォームから値を受け取る
     */
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            try {
                final List<Address> list_address = mGeocoder.getFromLocationName(query, 5);
                if (list_address.size() == 0) {
                    // 検索結果が0件
                    Toast.makeText(this, R.string.no_result_search_address, Toast.LENGTH_LONG).show();
                } else if (list_address.size() == 1) {
                    // 検索候補地が一カ所
                    Address address = list_address.get(0);
                    moveToAddress(address);
                    String strAddress = getAddressToString(address);
                    Toast.makeText(this, strAddress, Toast.LENGTH_SHORT).show();
                } else {
                    // 検索候補地が複数存在する
                    String[] str_list_address = new String[list_address.size()];
                    // 候補をリストで表示して、選択させる
                    for (int j = 0; j < list_address.size(); j++) {
                        Address address = list_address.get(j);
                        str_list_address[j] = getAddressToString(address);
                    }
                    final AlertDialog addressPicker = new AlertDialog.Builder(Place.this)
                            .setIcon(android.R.drawable.ic_dialog_map)
                            .setTitle(R.string.title_select_address)
                            .setSingleChoiceItems(str_list_address, 0,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Address address = list_address.get(which);
                                            moveToAddress(address);
                                            dialog.dismiss();
                                        }
                                    }).create();
                    addressPicker.show();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 指定された住所まで地図を移動させる
     * 
     * @param address
     */
    private void moveToAddress(Address address) {
        mGeoPoint = new GeoPoint((int) (address.getLatitude() * 1E6),
                (int) (address.getLongitude() * 1E6));
        setOverlay(mGeoPoint);
        mMapController.animateTo(mGeoPoint);
    }

    /**
     * Addressから住所文字列を取得する
     * 
     * @param address
     * @return
     */
    private String getAddressToString(Address address) {
        StringBuilder builder = new StringBuilder();
        if (address.getAddressLine(1) == null) {
            // 建物指定などの場合、0のみに値があるので、その場合を考慮
            builder.append(address.getAddressLine(0));
        } else {
            String buf;
            for (int i = 1; (buf = address.getAddressLine(i)) != null; i++) {
                builder.append(buf);
            }
        }
        return builder.toString();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean onDoubleTap(MotionEvent e) {
        mDoubleTap = true;
        return false;
    }

    public boolean onDoubleTapEvent(MotionEvent event) {
        if (mDoubleTap) {
            mDoubleTap = false;
            GeoPoint gp = getGeoPointByPoint((int) event.getX(), (int) event.getY());
            GeoPoint cgp = mMapView.getMapCenter();
            GeoPoint point = new GeoPoint((gp.getLatitudeE6() + cgp.getLatitudeE6()) / 2, (gp
                    .getLongitudeE6() + cgp.getLongitudeE6()) / 2);
            mMapController.setCenter(point);
            if (!mMapController.zoomIn()) {
                mMapController.animateTo(gp);
            }
        }
        return false;
    }

    public boolean onDown(MotionEvent event) {
        mSingleTap = false;
        return false;
    }

    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    public void onLongPress(MotionEvent arg0) {
    }

    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    public void onShowPress(MotionEvent arg0) {
    }

    public boolean onSingleTapUp(MotionEvent event) {
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent event) {
        mSingleTap = true;
        if (mButtonPush) {
            mButtonPush = false;
        } else {
            mGeoPoint = getGeoPointByPoint((int) event.getX(), (int) event.getY());
            mMapController.animateTo(mGeoPoint);
            List<Overlay> overlays = mMapView.getOverlays();
            for (int i = 0; i < overlays.size(); i++) {
                Overlay overlay = overlays.get(i);
                overlay.onTap(mGeoPoint, mMapView);
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (mGDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private GeoPoint getGeoPointByPoint(int x, int y) {
        Projection projection = mMapView.getProjection();
        return projection.fromPixels(x, y);
    }
    
    
    /**
     * 現在地を取得したらその場所に移動する機能をもったオーバーレイ
     * Topから現在地を取得ボタンを押された場合のみ、そのような動作をする
     */
    private class CustomMyLocationOverlay extends MyLocationOverlay {

        private boolean isMyLocationRequestByTop;
        
        private boolean isFirst = true;
        
        public void setIsMyLocationRequestByTop(boolean requestByTop) {
            this.isMyLocationRequestByTop = requestByTop;
        }

        public CustomMyLocationOverlay(Context context, MapView mapView) {
            super(context, mapView);
        }

        @Override
        public synchronized void onLocationChanged(Location location) {
            super.onLocationChanged(location);
            if (isFirst) {
                isFirst = false;
                GeoPoint p = getMyLocation();
                mGeoPoint = p;
                mMapController.animateTo(mGeoPoint);
                setOverlay(mGeoPoint);
                if (isMyLocationRequestByTop){
                    isMyLocationRequestByTop = false;
                    mProgress.dismiss();
                    callFinish();
                }
            }
        }
    }
}
