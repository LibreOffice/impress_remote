/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.communication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.activity.MainActivity;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class DataLayerListenerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DataLayerListenerService";
    private static final int N_START=001;
    private static final int N_STATUS=002;

    GoogleApiClient mGoogleApiClient;


    public DataLayerListenerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Created");

        if(null == mGoogleApiClient) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            Log.v(TAG, "GoogleApiClient created");
        }

        if(!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
            Log.v(TAG, "Connecting to GoogleApiClient..");
        }
    }

    @Override
    public void onDestroy() {

        Log.v(TAG, "Destroyed");

        if(null != mGoogleApiClient){
            if(mGoogleApiClient.isConnected()){
                mGoogleApiClient.disconnect();
                Log.v(TAG, "GoogleApiClient disconnected");
            }
        }

        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG,"onConnectionSuspended called");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG,"onConnectionFailed called");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG,"onConnected called");
//        sendMessage("/connect","");

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "Data Changed");
     /*   DataMap dataMap;
        for (DataEvent event : dataEvents) {
            // Check the event type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                //Verify the data path, get the DataMap, and send local notification
                if (path.equals("/wearable_start")) {
                    // Create and send a local notification inviting the user to start the wearable app
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    sendLocalNotification(N_START,dataMap.getString("title"), dataMap.getString("body"));
                }
                if (path.equals("/wearable_status")) {
                    // Create and send a local notification inviting the user to start the wearable app
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    sendLocalNotification(N_STATUS,dataMap.getString("title"), dataMap.getString("body"));
                }
            }
            if (event.getType() == DataEvent.TYPE_DELETED) {

            }
        }
        */
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        try{
            String message=new String(messageEvent.getData(), "UTF-8");
            Log.v(TAG, "onMessageReceived " + messageEvent.getPath()+message);
            if(messageEvent.getPath().equals("/count")){
                Intent aIntent= new Intent("SLIDE_COUNT");
                aIntent.putExtra("DATA",message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
            }
            if(messageEvent.getPath().equals("/wearable_start")){
                sendLocalNotification(N_START,"Impress Remote",message);
            }
            if(messageEvent.getPath().equals("/wearable_status")){
                sendLocalNotification(N_STATUS,"Presentation Running",message);
            }
            if(messageEvent.getPath().equals("/wearable_stop")){
                //TODO update(or close) full screen
                cancelNotifications();
            }

        }catch(UnsupportedEncodingException e){

        }

    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.v(TAG, "Peer Connected " + peer.getDisplayName());
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        Log.v(TAG, "Peer Disconnected " + peer.getDisplayName());
    }
    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    Log.d(TAG, "SendMessage " + path + "-" + text);
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes(Charset.forName("UTF-8")) ).await();
                }
            }
        }).start();
    }
    private void cancelNotifications(){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(N_START);
        notificationManager.cancel(N_STATUS);
    }

    private void sendLocalNotification(int id,String title, String body) {
        Log.v(TAG, "sendLocalNotification");
//        int notificationId = 001;

        // Create a pending intent that starts this wearable app

        Intent startIntent;
        if(id==001){
            startIntent = new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN);
        }else{
            startIntent = new Intent(this, MainActivity.class).setAction(Intent.ACTION_VIEW);
        }

        PendingIntent startPendingIntent = PendingIntent.getActivity(this, 0, startIntent, 0);

        Notification notify = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(body)
                .setLocalOnly(true)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(startPendingIntent)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        cancelNotifications();
        notificationManager.notify(id, notify);

    }

}