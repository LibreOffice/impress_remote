/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;

import org.libreoffice.impressremote.adapter.ComputersPagerAdapter;
import org.libreoffice.impressremote.fragment.ComputersFragment.Type;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.R;

public class ComputersActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 0;
    private static final String SELECT_BLUETOOTH = "SELECT_BLUETOOTH";
    private static final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final boolean disableBTOnQuit = btAdapter != null && !btAdapter.isEnabled();
    private static TabLayout tabLayout;
    private static TabLayout.Tab btTab;
    private static TabLayout.Tab wifiTab;
    private static FloatingActionButton btFab;
    private static FloatingActionButton addFab;
    private final ComputersPagerAdapter computersPagerAdapter = new ComputersPagerAdapter(getSupportFragmentManager(), this);

    @Override
    protected void onCreate(Bundle aSavedInstanceState) {
        super.onCreate(aSavedInstanceState);

        setContentView(R.layout.activity_computers);

        ActionBar aActionBar = getSupportActionBar();
        // Looks hacky but it seems to be the best way to set activity’s title
        // different to application’s label. The other way is setting title
        // to intents filter but it shows wrong label for recent apps screen then.
        assert aActionBar != null;
        aActionBar.setTitle(R.string.title_computers);
        aActionBar.setDisplayHomeAsUpEnabled(false);

        computersPagerAdapter.addFragment(Type.BLUETOOTH);
        computersPagerAdapter.addFragment(Type.WIFI);

        ViewPager aComputersPager = (ViewPager) findViewById(R.id.pager_computers);
        assert aComputersPager != null;
        aComputersPager.setAdapter(computersPagerAdapter);
        tabLayout = (TabLayout) findViewById(R.id.pager_computers_tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(aComputersPager);

        btTab = tabLayout.getTabAt(0);
        wifiTab = tabLayout.getTabAt(1);
        assert wifiTab != null;
        wifiTab.select();

        if (btAdapter == null) {
            computersPagerAdapter.removeFragment(Type.BLUETOOTH);
        }

        tabLayout.setOnTabSelectedListener(
            new TabLayout.ViewPagerOnTabSelectedListener(aComputersPager) {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    super.onTabSelected(tab);
                    if (btAdapter != null
                            && tab.getPosition() == btTab.getPosition()
                            && !btAdapter.isEnabled()) {
                        startActivityForResult(
                                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                                REQUEST_ENABLE_BT);
                    }
                }
            }
        );

        btFab = (FloatingActionButton) findViewById(R.id.btFab);
        btFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btAdapter != null && btAdapter.startDiscovery()) {
                    btFab.setClickable(false);
                    // workaround, see https://code.google.com/p/android/issues/detail?id=175696#c6
                    // when not delayed animation won't show on pre-lollipop unless switching tabs
                    btFab.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btFab.startAnimation(
                                    AnimationUtils.loadAnimation(getApplication(), R.anim.fabrotate));
                        }
                    }, 50);
                }
            }
        });

        addFab = (FloatingActionButton) findViewById(R.id.addFab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().getFragments().get(0).startActivityForResult(
                        Intents.buildComputerCreationIntent(tabLayout.getContext()),
                        Intents.RequestCodes.CREATE_SERVER);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                wifiTab.select();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu aMenu) {
        getMenuInflater().inflate(R.menu.menu_action_bar_computers, aMenu);
        // ComputerFragement uses invalidateOptionsMKenu when BT-discovery broadcast is received
        toggleFab(tabLayout.getSelectedTabPosition());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        switch (aMenuItem.getItemId()) {
            case R.id.menu_settings:
                startActivity(Intents.buildSettingsIntent(this));
                return true;

            case R.id.menu_requirements:
                startActivity(Intents.buildRequirementsIntent(this));
                return true;

            default:
                return super.onOptionsItemSelected(aMenuItem);
        }
    }

    private void toggleFab(int pos) {
        if(pos == btTab.getPosition()) {
            addFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    if( btAdapter == null) {
                        wifiTab.select();
                        return;
                    }
                    if (btAdapter.isDiscovering()) {
                        btFab.setClickable(false);
                        btFab.startAnimation(
                                AnimationUtils.loadAnimation(getApplication(), R.anim.fabrotate));
                    } else {
                        btFab.setClickable(true);
                        if(btFab.getAnimation() != null) {
                            btFab.clearAnimation();
                        }
                    }
                    btFab.show();
                }
            });
        } else {
            btFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    addFab.show();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SELECT_BLUETOOTH, btTab.getPosition() == tabLayout.getSelectedTabPosition());
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if (btTab.getPosition() != TabLayout.Tab.INVALID_POSITION
            && sharedPref.getBoolean(SELECT_BLUETOOTH, btAdapter != null)) {
            btTab.select();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && disableBTOnQuit) {
            btAdapter.disable();
        }
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
