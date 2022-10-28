/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.adapter.ComputersPagerAdapter;
import org.libreoffice.impressremote.fragment.ComputersFragment.Type;
import org.libreoffice.impressremote.util.Intents;

public class ComputersActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 0;
    private static final String SELECT_BLUETOOTH = "SELECT_BLUETOOTH";
    private static final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final boolean disableBTOnQuit = btAdapter != null && !btAdapter.isEnabled();
    private static boolean haveBluetoothPermissionConnect = false;
    private static boolean haveBluetoothPermissionScan = false;
    private TabLayout tabLayout;
    private static TabLayout.Tab btTab;
    private static TabLayout.Tab wifiTab;
    private FloatingActionButton btFab;
    private FloatingActionButton addFab;
    private final ComputersPagerAdapter computersPagerAdapter = new ComputersPagerAdapter(getSupportFragmentManager(), this);

    private final ActivityResultLauncher<String> requestPermissionBTConnect =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // TODO add blurp about not being able to use BT stuff if not granted
                haveBluetoothPermissionConnect = isGranted;
                init();
            });

    // requesting the connect permission actually asks for the nearby devices permission group,
    // requests for the scan permission are then automatically granted without a prompt, but you
    // still have to explicitly ask for it before you can call any related methods
    private final ActivityResultLauncher<String> requestPermissionBTScan =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                haveBluetoothPermissionScan = isGranted;
                btFab.setClickable(isGranted);
                btFab.performClick();
            });

    public static boolean getHaveBTConnect() {
        return haveBluetoothPermissionConnect;
    }

    public static boolean getHaveBTScan() {
        return haveBluetoothPermissionScan;
    }

    @Override
    protected void onCreate(Bundle aSavedInstanceState) {
        super.onCreate(aSavedInstanceState);
        // the pre-Android 12 Bluetooth permissions have "normal" danger level and don't require
        // asking at runtime
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            haveBluetoothPermissionConnect = true;
            init();
            // TODO: add else if (shouldShowRequestPermissionRationale(...)) {
            // without that the user is only prompted a single time/would have to clear permissions
            // in system settings to be prompted again.
        } else {
            requestPermissionBTConnect.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
    }

    private void init() {
        setContentView(R.layout.activity_computers);

        Toolbar toolbar = (Toolbar) findViewById(R.id.computers_toolbar);
        setSupportActionBar(toolbar);
        // Looks hacky but it seems to be the best way to set activity’s title
        // different to application’s label. The other way is setting title
        // to intents filter but it shows wrong label for recent apps screen then.
        assert toolbar != null;
        toolbar.setTitle(R.string.title_computers);
        computersPagerAdapter.reset();
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

        if (btAdapter == null || !haveBluetoothPermissionConnect) {
            computersPagerAdapter.removeFragment(Type.BLUETOOTH);
        }

        tabLayout.setOnTabSelectedListener(
            new TabLayout.ViewPagerOnTabSelectedListener(aComputersPager) {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    super.onTabSelected(tab);
                    if (btAdapter != null && haveBluetoothPermissionConnect
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
                btFab.clearAnimation();
                if (btAdapter == null) {
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(v.getContext(),
                        Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionBTScan.launch(Manifest.permission.BLUETOOTH_SCAN);
                    return;
                } else {
                    haveBluetoothPermissionScan = true;
                }
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                } else if (btAdapter.startDiscovery()) {
                    // workaround, see https://code.google.com/p/android/issues/detail?id=175696#c6
                    // when not delayed animation won't show on pre-lollipop unless switching tabs
                    btFab.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btFab.startAnimation(
                                    AnimationUtils.loadAnimation(getApplication(), R.anim.fabalpha));
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
        // ComputerFragment uses invalidateOptionsMenu when BT-discovery broadcast is received
        toggleFab(tabLayout.getSelectedTabPosition());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        int itemId = aMenuItem.getItemId();
        if (itemId == R.id.menu_settings) {
            startActivity(Intents.buildSettingsIntent(this));
            return true;
        } else if (itemId == R.id.menu_requirements) {
            startActivity(Intents.buildRequirementsIntent(this));
            return true;
        }
        return super.onOptionsItemSelected(aMenuItem);
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
                    btFab.setClickable(true);
                    btFab.clearAnimation();

                    if (haveBluetoothPermissionScan && btAdapter.isDiscovering()) {
                        btFab.startAnimation(
                                AnimationUtils.loadAnimation(getApplication(), R.anim.fabalpha));
                    }
                    btFab.show();
                }
            });
        } else {
            btFab.setClickable(false);
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
        if(btTab == null) {
            return;
        }
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SELECT_BLUETOOTH, btTab.getPosition() == tabLayout.getSelectedTabPosition());
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if (haveBluetoothPermissionConnect && btTab.getPosition() != TabLayout.Tab.INVALID_POSITION
            && sharedPref.getBoolean(SELECT_BLUETOOTH, btAdapter != null)) {
            btTab.select();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && disableBTOnQuit && haveBluetoothPermissionConnect) {
            btAdapter.disable();
        }
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
