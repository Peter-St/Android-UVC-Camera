package com.example.androidthings.videortc;

import android.content.Context;
import android.util.Log;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

public class InitCapturer implements VideoCapturer {

    private static String TAG = "VideoCapturer";
    private SurfaceTextureHelper surfaceTextureHelper;
    private CapturerObserver capturerObserver;

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        Log.d(TAG, "initialize: ");
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int i, int i1, int i2) {
        Log.d(TAG, "startCapture: ");

    }

    @Override
    public void stopCapture() throws InterruptedException {
        Log.d(TAG, "stopCapture: ");
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {
        Log.d(TAG, "changeCaptureFormat: ");
    }

    @Override
    public void dispose() {
        Log.d(TAG, "dispose: ");
    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}