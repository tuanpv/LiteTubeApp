/*
 * Created by Christian Schabesberger on 02.08.16.
 * <p>
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * DownloadActivity.java is part of NewPipe.
 * <p>
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.youtubelite.floattube;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.youtubelite.floattube.download.DownloadActivity;
import com.youtubelite.floattube.extractor.StreamingService;
import com.youtubelite.floattube.fragments.MainFragment;
import com.youtubelite.floattube.fragments.detail.VideoDetailFragment;
import com.youtubelite.floattube.fragments.search.SearchFragment;
import com.youtubelite.floattube.settings.SettingsActivity;
import com.youtubelite.floattube.util.AppRater;
import com.youtubelite.floattube.util.Connectivity;
import com.youtubelite.floattube.util.Constants;
import com.youtubelite.floattube.util.NavigationHelper;
import com.youtubelite.floattube.util.PermissionHelper;
import com.youtubelite.floattube.util.ThemeHelper;
import com.youtubelite.floattube.util.Utils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final boolean DEBUG = false;

    private AdRequest adRequest;
    private AdView adView;
    private InterstitialAd interstitial;
    private final static int LIMIT_INDEX = 6;
    //    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int index = 0;
    private final static String INDEX_ADMOB = "index_admob";

    private SharedPreferences mPreferences;

    /*//////////////////////////////////////////////////////////////////////////
    // Activity's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG)
            Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppRater.app_launched(this);

        if (getSupportFragmentManager() != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
            initFragments();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            editor = mPreferences.edit();
            index = mPreferences.getInt(INDEX_ADMOB, 1);
            if (index % LIMIT_INDEX == 0) {
                index--;
            } else {
                index++;
            }
            editor.putInt(INDEX_ADMOB, index).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Connectivity.isConnected(this)) {
            adView = new AdView(this);
            adView.setAdSize(AdSize.SMART_BANNER);
            final LinearLayout layout = (LinearLayout) findViewById(R.id.list_grid_ll_ad);
            layout.setVisibility(View.VISIBLE);
            layout.addView(adView);
            adView.setAdUnitId(getString(R.string.banner_lite_tube));
            adRequest = new AdRequest.Builder()/*.addTestDevice("B36624619B602BB4EEEEB3EE6B0ECBC3")*/.build();
            // Start loading the ad in the background.
            adView.loadAd(adRequest);
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    layout.setVisibility(View.GONE);
                    adView.loadAd(adRequest);
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    layout.setVisibility(View.VISIBLE);
                }
            });
        }


        // Create the interstitial.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getString(R.string.interstitial_lite_tube));
        requestNewInterstitial();
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int i) {
                requestNewInterstitial();
            }

            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("B36624619B602BB4EEEEB3EE6B0ECBC3")
                .build();
        interstitial.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(Constants.KEY_THEME_CHANGE, false)) {
            if (DEBUG) Log.d(TAG, "Theme has changed, recreating activity...");
            sharedPreferences.edit().putBoolean(Constants.KEY_THEME_CHANGE, false).apply();
            this.recreate();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "onNewIntent() called with: intent = [" + intent + "]");
        if (intent != null) {
            // Return if launched from a launcher (e.g. Nova Launcher, Pixel Launcher ...)
            // to not destroy the already created backstack
            String action = intent.getAction();
            if ((action != null && action.equals(Intent.ACTION_MAIN)) && intent.hasCategory(Intent.CATEGORY_LAUNCHER))
                return;
        }

        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.d(TAG, "onBackPressed() called");

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (fragment instanceof VideoDetailFragment) if (((VideoDetailFragment) fragment).onActivityBackPressed()) return;

        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackCount > 0) {
//                    LogUtils.d(TAG, "popping back stack");
            getSupportFragmentManager().popBackStack();
            if (backStackCount == 1) {
//                        finishActivity();
                super.onBackPressed();
            }
        } else {
//                    LogUtils.d(TAG, "nothing on back stack, calling super");
//            mActivity.onBackPressed();
//                    finishActivity();
            super.onBackPressed();
        }


//        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
//        if (fragment instanceof VideoDetailFragment)
//            if (((VideoDetailFragment) fragment).onActivityBackPressed()) return;
//
//        super.onBackPressed();
//
//        fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
//        if (getSupportFragmentManager().getBackStackEntryCount() == 0 && !(fragment instanceof MainFragment)) {
//            super.onBackPressed();
//        }

//        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
//        if (fragment instanceof VideoDetailFragment)
//            ((VideoDetailFragment) fragment).clearHistory();
//        try {
//            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//            NavigationHelper.openMainFragment(getSupportFragmentManager());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "]");
        super.onCreateOptionsMenu(menu);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (!(fragment instanceof VideoDetailFragment)) {
            findViewById(R.id.toolbar).findViewById(R.id.toolbar_spinner).setVisibility(View.GONE);
        }

        if (!(fragment instanceof SearchFragment)) {
            findViewById(R.id.toolbar).findViewById(R.id.toolbar_search_container).setVisibility(View.GONE);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "onOptionsItemSelected() called with: item = [" + item + "]");
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home: {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
                if (fragment instanceof VideoDetailFragment)
                    ((VideoDetailFragment) fragment).clearHistory();

                int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
                if (backStackCount > 0) {
//                    LogUtils.d(TAG, "popping back stack");
                    getSupportFragmentManager().popBackStack();
                    if (backStackCount == 1) {
//                        finishActivity();
                        try {
//                            NavigationHelper.openMainActivity(this);
                            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            NavigationHelper.openMainFragment(getSupportFragmentManager());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
//                    LogUtils.d(TAG, "nothing on back stack, calling super");
//            mActivity.onBackPressed();
//                    finishActivity();
                    try {
//                        NavigationHelper.openMainActivity(this);
                    getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    NavigationHelper.openMainFragment(getSupportFragmentManager());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


//                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
//                if (fragment instanceof VideoDetailFragment)
//                    ((VideoDetailFragment) fragment).clearHistory();
//                try {
//                    getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//                    NavigationHelper.openMainFragment(getSupportFragmentManager());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.menu_item_rate: {
                Utils.rateUs(MainActivity.this);
                return true;
            }

//            case R.id.action_show_downloads: {
//                if (!PermissionHelper.checkStoragePermissions(this)) {
//                    return false;
//                }
//                Intent intent = new Intent(this, DownloadActivity.class);
//                startActivity(intent);
//                return true;
//            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    private void initFragments() {
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_URL)) {
            handleIntent(getIntent());
        } else NavigationHelper.openMainFragment(getSupportFragmentManager());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void handleIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "handleIntent() called with: intent = [" + intent + "]");

        if (intent.hasExtra(Constants.KEY_LINK_TYPE)) {
            String url = intent.getStringExtra(Constants.KEY_URL);
            int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
            String title = intent.getStringExtra(Constants.KEY_TITLE);
            switch (((StreamingService.LinkType) intent.getSerializableExtra(Constants.KEY_LINK_TYPE))) {
                case STREAM:
                    boolean autoPlay = intent.getBooleanExtra(VideoDetailFragment.AUTO_PLAY, false);
                    NavigationHelper.openVideoDetailFragment(getSupportFragmentManager(), serviceId, url, title, autoPlay);
                    break;
                case CHANNEL:
                    NavigationHelper.openChannelFragment(getSupportFragmentManager(), serviceId, url, title);
                    break;
            }

            try {
                index = mPreferences.getInt(INDEX_ADMOB, 0);
                index++;

                if (Connectivity.isConnected(this)) {
                    if (index % LIMIT_INDEX == 0) {
                        index = 0;
                        if (interstitial.isLoaded()) {
                            interstitial.show();
                        } else {
                            index--;
                        }
                    }
                } else {
                    if (index % LIMIT_INDEX == 0) {
                        index--;
                    }
                }
                editor.putInt(INDEX_ADMOB, index).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (intent.hasExtra(Constants.KEY_OPEN_SEARCH)) {
            String searchQuery = intent.getStringExtra(Constants.KEY_QUERY);
            if (searchQuery == null) searchQuery = "";
            int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
            NavigationHelper.openSearchFragment(getSupportFragmentManager(), serviceId, searchQuery);
        } else {
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            NavigationHelper.openMainFragment(getSupportFragmentManager());
        }
    }
}
