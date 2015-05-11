/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.DataLayerListenerService;
import org.libreoffice.impressremote.util.SlideShowData;


public class FullscreenActivity extends ControlActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.full_screen_background);
        SlideShowData.getInstance().setFullscreen(true);
        changeSlideCount(SlideShowData.getInstance().getCount());
        if(SlideShowData.getInstance().hasPreview()){
            changePreview(SlideShowData.getInstance().getPreview());
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
     protected void onPause(){
        Log.v(TAG, "onPause");
        SlideShowData.getInstance().setFullscreen(false);
        super.onPause();
    }

    @Override
    protected void onStop(){
        Log.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG, "onConnectionSuspended called");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "onConnectionFailed called");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG,"onConnected called");
        DataLayerListenerService.commandConnect();
    }
    }
