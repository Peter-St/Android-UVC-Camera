package humer.UvcCamera.AutomaticDetection;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import humer.UvcCamera.R;


public class AutoDetect_Fragment extends Fragment {
    private final String KEY_SCROLL_Y = "scroll_y";

    private TextView percentageView;
    private static String percentDone = "0% done";

    public static Fragment newInstance(String value) {
        if (value != null) percentDone = value;
        return new AutoDetect_Fragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.auto_detect_fragment, container, false);
        percentageView = rootView.findViewById(R.id.percentage);
        percentageView.setText(percentDone);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}