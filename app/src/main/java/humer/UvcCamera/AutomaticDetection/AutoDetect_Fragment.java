package humer.UvcCamera.AutomaticDetection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import humer.UvcCamera.R;


public class LibUsb_Fragment extends Fragment {
    private final String KEY_SCROLL_Y = "scroll_y";

    private TextView percentageView;

    public static Fragment newInstance() {
        return new LibUsb_Fragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.libusb_autodetect_fragment, container, false);
        percentageView = rootView.findViewById(R.id.percentage);

        if (savedInstanceState != null) {
            // Restore y-position of scroll view.
            percentageView.scrollTo(0, savedInstanceState.getInt(KEY_SCROLL_Y));
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save y-position of scroll view.
        outState.putInt(KEY_SCROLL_Y, percentageView.getScrollY());
        super.onSaveInstanceState(outState);
    }
}