/*
 * Copyright (c)  2017  Francisco José Montiel Navarro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.franmontiel.localechanger.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A helper class with methods to detect Locale changes and recreate the Activities accordingly.
 */
public class ActivityRecreationHelper {

    private static final Map<String, Locale> localesOnActivities = new HashMap<>();

    private ActivityRecreationHelper(){
    }

    /**
     * Call this method on the Activity onResume. It will recreate the Activity if a Locale change is detected.
     * @param activity
     */
    public static void onResume(Activity activity) {
        Locale previousLocale = localesOnActivities.get(activity.toString());
        boolean shouldRestartActivity = previousLocale != null && !previousLocale.equals(Locale.getDefault());

        localesOnActivities.put(activity.toString(), Locale.getDefault());

        if (shouldRestartActivity) {
            recreate(activity, false);
        }
    }

    /**
     * Call this method on the Activity onDestroy.
     * @param activity
     */
    public static void onDestroy(Activity activity) {
        localesOnActivities.remove(activity.toString());
    }

    /**
     * Helper method to recreate the Activity. This method should be called after a Locale change.
     * @param activity the Activity that will be recreated
     * @param animate a flag indicating if the recreation will be animated or not
     */
    public static void recreate(Activity activity, boolean animate) {
        Intent restartIntent = new Intent(activity, activity.getClass());

        Bundle extras = activity.getIntent().getExtras();
        if (extras != null) {
            restartIntent.putExtras(extras);
        }

        if (animate) {
            ActivityCompat.startActivity(
                    activity,
                    restartIntent,
                    ActivityOptionsCompat
                            .makeCustomAnimation(activity, android.R.anim.fade_in, android.R.anim.fade_out)
                            .toBundle()
            );
        } else {
            activity.startActivity(restartIntent);
            activity.overridePendingTransition(0, 0);
        }

        activity.finish();

    }
}