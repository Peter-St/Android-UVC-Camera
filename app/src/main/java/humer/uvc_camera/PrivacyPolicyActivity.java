package humer.uvc_camera;


import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class PrivacyPolicyActivity extends Activity {
    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policyctivity);

        web =(WebView)findViewById(R.id.webView);
        web.loadUrl("file:///android_asset/Privacy_Policy_UVC_Camera.html");

    }

}