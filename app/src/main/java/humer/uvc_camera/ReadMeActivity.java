package humer.uvc_camera;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ReadMeActivity extends Activity {

    TextView tv;
    public Button settingsButtonOverview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        tv = (TextView) findViewById(R.id.textDarstellung);
        tv.setText(getString(R.string.readme));

        settingsButtonOverview = (Button) findViewById(R.id.einstellungen);

        settingsButtonOverview.getBackground().setAlpha(1);  // 99% transparent


    }
}
