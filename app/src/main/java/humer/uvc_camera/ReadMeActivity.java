package humer.uvc_camera;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ReadMeActivity extends Activity {

    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_readme);
        tv = (TextView) findViewById(R.id.textDarstellung);
        tv.setText(getString(R.string.readme));



    }
}
