package android.support.v7.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;


public class LocaleChangerAppCompatDelegate extends AppCompatDelegate {

    private final AppCompatDelegate delegate;

    public LocaleChangerAppCompatDelegate(AppCompatDelegate delegate) {
        this.delegate = delegate;
    }

    @Nullable
    @Override
    public ActionBar getSupportActionBar() {
        return delegate.getSupportActionBar();
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        delegate.setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return delegate.getMenuInflater();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        delegate.onCreate(savedInstanceState);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        delegate.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        delegate.onConfigurationChanged(newConfig);
    }

    @Override
    public void onStart() {
        delegate.onStart();
    }

    @Override
    public void onStop() {
        delegate.onStop();
    }

    @Override
    public void onPostResume() {
        delegate.onPostResume();
    }

    @Nullable
    @Override
    public <T extends View> T findViewById(int id) {
        return delegate.findViewById(id);
    }

    @Override
    public void setContentView(View v) {
        delegate.setContentView(v);
    }

    @Override
    public void setContentView(int resId) {
        delegate.setContentView(resId);
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        delegate.setContentView(v, lp);
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        delegate.addContentView(v, lp);
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        delegate.setTitle(title);
    }

    @Override
    public void invalidateOptionsMenu() {
        delegate.invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        delegate.onDestroy();
    }

    @Nullable
    @Override
    public ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return delegate.getDrawerToggleDelegate();
    }

    @Override
    public boolean requestWindowFeature(int featureId) {
        return delegate.requestWindowFeature(featureId);
    }

    @Override
    public boolean hasWindowFeature(int featureId) {
        return delegate.hasWindowFeature(featureId);
    }


    @Nullable
    @Override
    public ActionMode startSupportActionMode(@NonNull ActionMode.Callback callback) {
        return delegate.startSupportActionMode(callback);
    }

    @Override
    public void installViewFactory() {
        delegate.installViewFactory();
    }

    @Override
    public View createView(@Nullable View parent, String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return delegate.createView(parent, name, context, attrs);
    }

    @Override
    public void setHandleNativeActionModesEnabled(boolean enabled) {
        delegate.setHandleNativeActionModesEnabled(enabled);
    }

    @Override
    public boolean isHandleNativeActionModesEnabled() {
        return delegate.isHandleNativeActionModesEnabled();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        delegate.onSaveInstanceState(outState);
    }

    @Override
    public boolean applyDayNight() {
        return delegate.applyDayNight();
    }

    @Override
    public void setLocalNightMode(int mode) {
        delegate.setLocalNightMode(mode);
    }
}