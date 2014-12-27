/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.communication;

import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.libreoffice.impressremote.util.Intents;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommunicationServiceWear extends WearableListenerService {

    private static final String TAG = "CommunicationServiceWear";

    private static final String COMMAND_NEXT="/next";
    private static final String COMMAND_PREVIOUS="/previous";
    private static final String COMMAND_PAUSERESUME="/pauseResume";
    private static final String COMMAND_CONNECT="/connect";
    private static final String COMMAND_APP_PAUSED="/appPaused";
    private static final String COMMAND_PRESENTATION_STOPPED="/wearableStop";
    private static final String COMMAND_SLIDE_COUNT="/count";

    private static GoogleApiClient googleApiClient;


    @Override
     public void onCreate(){
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        Log.v(TAG, "GoogleApiClient created");
        googleApiClient.connect();
        Log.v(TAG, "Connecting to GoogleApiClient..");

        Intent aIntent= Intents.buildGoogleApiConnectedIntent();
        LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);

    }

    @Override
    public void onDestroy(){
        Log.v(TAG, "onDestroy");
        if(null != googleApiClient){
            notifyWearStop();
            final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
            exec.schedule(new Runnable(){
                @Override
                public void run(){
                    if(googleApiClient!=null){
                        if(googleApiClient.isConnected()){
                            googleApiClient.disconnect();
                            Log.v(TAG, "GoogleApiClient disconnected");
                        }
                    }
                }
            }, 2, TimeUnit.SECONDS);

    }
        super.onDestroy();

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

       Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());

        if(messageEvent.getPath().equals(COMMAND_NEXT)){
            Intent aIntent= Intents.buildWearNextIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
        if(messageEvent.getPath().equals(COMMAND_PREVIOUS)){
            Intent aIntent= Intents.buildWearPreviousIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
/*        if(messageEvent.getPath().equals("/pause")){
            Intent aIntent= Intents.buildWearPauseIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
        if(messageEvent.getPath().equals("/resume")){
            Intent aIntent= Intents.buildWearResumeIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }*/
        if(messageEvent.getPath().equals(COMMAND_CONNECT)){
            Intent aIntent= Intents.buildWearConnectIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
        if(messageEvent.getPath().equals(COMMAND_APP_PAUSED)){
            Intent aIntent= Intents.buildWearExitIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
        if(messageEvent.getPath().equals(COMMAND_PAUSERESUME)){
            Intent aIntent= Intents.buildWearPauseResumeIntent();
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }



    }
    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        String id = peer.getId();
        String name = peer.getDisplayName();

        Log.d(TAG, "Connected peer name & ID: " + name + "|" + id);


    }

    private void notifyWearStop(){
        Log.d(TAG, "notifyWearStop");
        sendMessage(COMMAND_PRESENTATION_STOPPED);
    }

    public static void sendStatusNotification(String count){
        Log.d(TAG, "sendStatusNotification");
        sendCountMessage(count);
    }

    private static void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(googleApiClient !=null){
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes( googleApiClient ).await();
                    for(Node node : nodes.getNodes()) {
                        Log.d(TAG, "SendMessage " + path + "-" + text);
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                googleApiClient, node.getId(), path, text.getBytes(Charset.forName("UTF-8")) ).await();
                    }
                }
            }
        }).start();
    }
    private static void sendMessage( final String path) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(googleApiClient !=null){
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes( googleApiClient ).await();
                    for(Node node : nodes.getNodes()) {
                        Log.d(TAG, "SendMessage " + path );
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                googleApiClient, node.getId(), path, null ).await();
                    }
                }
            }
        }).start();
    }

    public static void sendCountMessage(String s) {
        Log.d(TAG, "sendCountMessage");
        sendMessage(COMMAND_SLIDE_COUNT,s);
    }
}