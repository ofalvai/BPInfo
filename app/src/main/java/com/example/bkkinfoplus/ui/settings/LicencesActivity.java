package com.example.bkkinfoplus.ui.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.example.bkkinfoplus.R;

public class LicencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licences);

        WebView webView = (WebView) findViewById(R.id.webview_licences);
        if (webView != null) {
            webView.loadUrl("file:///android_asset/licences.html");
        }


    }


}
