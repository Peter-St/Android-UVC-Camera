package noman.zoomtextview;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by nor on 11/23/2015.
 */



public class ZoomTextView extends AppCompatTextView {
    private static final String TAG = "ZoomTextView";
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector gestureDetector;

    private float mScaleFactor = 1.f;
    private float defaultSize;

    private float zoomLimit = 3.0f;

    public static final int TEXT_MAX_SIZE = 140;
    public static final int TEXT_MIN_SIZE = 40;
    private static final int STEP = 4;
    private int mBaseDistZoomIn;
    private int mBaseDistZoomOut;


    public ZoomTextView(Context context) {
        super(context);
        initialize();
    }

    public ZoomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ZoomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        defaultSize = getTextSize();
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    /***
     * @param zoomLimit
     * Default value is 3, 3 means text can zoom 3 times the default size
     */

    public void setZoomLimit(float zoomLimit) {
        this.zoomLimit = zoomLimit;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        super.onTouchEvent(ev);
        mScaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);
        return true;
    }

    /*Scale Gesture listener class,
    mScaleFactor is getting the scaling value
    and mScaleFactor is mapped between 1.0 and and zoomLimit
    that is 3.0 by default. You can also change it. 3.0 means text
    can zoom to 3 times the default value.*/

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, zoomLimit));
            setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultSize * mScaleFactor);
            Log.d(TAG, String.valueOf(mScaleFactor));
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            Log.d(TAG, "Tapped at: (" + x + "," + y + ")");

            if (mScaleFactor == 1.0f) {
                mScaleFactor = 2.0f;
                setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultSize * mScaleFactor);

            } else if (mScaleFactor == 2.0f) {
                mScaleFactor = 3.0f;
                setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultSize * mScaleFactor);
            } else{
                mScaleFactor = 1.0f;
                setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultSize * mScaleFactor);
            }
            return true;
        }
    }


    // good function to get the distance between the multiple touch
    int getDistanceFromEvent(MotionEvent event) {
        int dx = (int) (event.getX(0) - event.getX(1));
        int dy = (int) (event.getY(0) - event.getY(1));
        return (int) (Math.sqrt(dx * dx + dy * dy));
    }
}