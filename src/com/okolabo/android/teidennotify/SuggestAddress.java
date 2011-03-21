package com.okolabo.android.teidennotify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.it4myself.util.RestfulClient;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SuggestAddress extends Activity {
    private static final HashMap<String, String> PREFS;
    
    /** 都県の計画停電エリア情報が取得できるWebAPIのURL */
    private static final String URL_PREF = "http://prayforjapanandroid.appspot.com/api/pref";
    
    private AutoCompleteTextView mEditAddress;
    
    /** 都県名 */
    private String mPrefName = null;
    
    static {
        // 都県を設定
        PREFS = new HashMap<String, String>();
        PREFS.put("千葉県", "chiba");
        PREFS.put("群馬県", "gunma");
        PREFS.put("茨城県", "ibaraki");
        PREFS.put("神奈川県", "kanagawa");
        PREFS.put("静岡県", "numazu");
        PREFS.put("埼玉県", "saitama");
        PREFS.put("栃木県", "tochigi");
        PREFS.put("東京都", "tokyo");
        PREFS.put("山梨県", "yamanashi");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suggest_address);
        Intent intent = getIntent();
        mPrefName = intent.getStringExtra("pref");
        String address = intent.getStringExtra("address");
        mEditAddress = (AutoCompleteTextView) findViewById(R.id.address);
        mEditAddress.setText(address);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Iterator<String> iterator = PREFS.keySet().iterator();
        while (iterator.hasNext()) {
            String prefName = (String) iterator.next();
            if (mPrefName.equals(prefName)) {
                new AddressDataTask().execute(PREFS.get(prefName));
                break;
            }
        }
        ((TextView)findViewById(R.id.pref)).setText(mPrefName);
    }

    /**
     * トップに戻って結果を反映する
     */
    public void search(View v) {
        String address = mEditAddress.getText().toString();
        Intent intent = new Intent();
        intent.putExtra("pref", mPrefName);
        intent.putExtra("address", address);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 都県のアドレスデータを取得する
     * サジェストを実装するため
     *
     */
    class AddressDataTask extends AsyncTask<String, Void, ArrayList<String>> {
        
        private ProgressDialog mProgress;

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            String pref = params[0];
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("pref", pref);
            ArrayList<String> items = AddressCache.getData(pref);
            if (items == null) {
                items = new ArrayList<String>();
                try {
                    String response = RestfulClient.Get(URL_PREF, map);
                    JSONObject json = new JSONObject(response);
                    JSONArray addresses = json.getJSONArray("addresses");
                    for (int i = 0; i < addresses.length(); i++) {
                        JSONObject data = addresses.getJSONObject(i);
                        items.add(data.getString("address"));
                    }
                    AddressCache.setData(pref, items);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return items;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            mEditAddress.setAdapter(new ArrayAdapter<String>(
                    SuggestAddress.this,
                    android.R.layout.simple_dropdown_item_1line,
                    result));
            mProgress.dismiss();
        }
        
        @Override
        protected void onPreExecute() {
            // プログレスダイアログ表示
            mProgress = new ProgressDialog(SuggestAddress.this);
            mProgress.setMessage(getString(R.string.data_load_pref));
            mProgress.setIcon(android.R.drawable.ic_dialog_info);
            mProgress.setIndeterminate(false);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.show();
        }
    }
}
