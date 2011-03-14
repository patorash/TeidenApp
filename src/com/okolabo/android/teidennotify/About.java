package com.okolabo.android.teidennotify;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class About extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        WebView webView = (WebView)findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/about.html");
    }

}
