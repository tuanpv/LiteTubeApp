package com.youtubelite.floattube;

import android.app.Application;
import android.content.Context;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.youtubelite.floattube.extractor.NewPipe;
import com.youtubelite.floattube.report.AcraReportSenderFactory;
import com.youtubelite.floattube.report.ErrorActivity;
import com.youtubelite.floattube.settings.SettingsActivity;

import org.acra.ACRA;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.ReportSenderFactory;

import com.youtubelite.floattube.util.ThemeHelper;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;

/**
 * Copyright (C) Hans-Christoph Steiner 2016 <hans@eds.org>
 * App.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class App extends Application {
    private static final String TAG = App.class.toString();

    private static boolean useTor;

    final Class<? extends ReportSenderFactory>[] reportSenderFactoryClasses
            = new Class[]{AcraReportSenderFactory.class};

    @Override
    public void onCreate() {
        super.onCreate();

        // init crashreport
        try {
            final ACRAConfiguration acraConfig = new ConfigurationBuilder(this)
                    .setReportSenderFactoryClasses(reportSenderFactoryClasses)
                    .build();
            ACRA.init(this, acraConfig);
        } catch(ACRAConfigurationException ace) {
            ace.printStackTrace();
            ErrorActivity.reportError(this, ace, null, null,
                    ErrorActivity.ErrorInfo.make(ErrorActivity.SEARCHED, "none",
                            "Could not initialize ACRA crash report", R.string.app_ui_crash));
        }

        //init NewPipe
        NewPipe.init(Downloader.getInstance());

        // Initialize image loader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        /*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean(getString(R.string.use_tor_key), false)) {
            OrbotHelper.requestStartTor(this);
            configureTor(true);
        } else {
            configureTor(false);
        }*/
        configureTor(false);

        // DO NOT REMOVE THIS FUNCTION!!!
        // Otherwise downloadPathPreference has invalid value.
        SettingsActivity.initSettings(this);

        ThemeHelper.setTheme(getApplicationContext());
    }

    /**
     * Set the proxy settings based on whether Tor should be enabled or not.
     */
    public static void configureTor(boolean enabled) {
        useTor = enabled;
        if (useTor) {
            NetCipher.useTor();
        } else {
            NetCipher.setProxy(null);
        }
    }

    public static void checkStartTor(Context context) {
        if (useTor) {
            OrbotHelper.requestStartTor(context);
        }
    }

    public static boolean isUsingTor() {
        return useTor;
    }
}
