/*
Copyright 2019 Peter Stoiber

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Please contact the author if you need another license.
This Repository is provided "as is", without warranties of any kind.

*/

package humer.UvcCamera;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import java.util.Locale;

public class ReadMeActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent=getIntent();
        Bundle bundle=intent.getBundleExtra("bun");
        String languageToLoad=bundle.getString("locale"); // your language

        log(getBaseContext().getResources().getConfiguration().locale.toString());


        log(Locale.getDefault().getLanguage());
        log(languageToLoad);
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.layout_readme);
    }

    public void log(String msg) {
        Log.i("ReadMe", msg);
    }

}
