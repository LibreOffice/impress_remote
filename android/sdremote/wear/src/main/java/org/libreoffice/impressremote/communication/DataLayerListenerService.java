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
    private static final int NOTIFICATION_ID=001;
    private static final String NOTIFICATION_TITLE="Impress Remote";
    private static final String SLIDE="Slide";
    private static final String COUNT_SPLIT="/";
    private static final String OF="of";
    private static final String SPACE=" ";
    private static final String NULL_STRING_COUNT="0/0";

    private static final String COMMAND_NEXT="/next";
    private static final String COMMAND_PREVIOUS="/previous";
    private static final String COMMAND_PAUSERESUME="/pauseResume";
    private static final String COMMAND_CONNECT="/connect";
    private static final String COMMAND_APP_PAUSED="/appPaused";
    private static final String COMMAND_PRESENTATION_STOPPED="/wearableStop";
    private static final String COMMAND_SLIDE_COUNT="/count";

    private static GoogleApiClient mGoogleApiClient;


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
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "Data Changed");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        try{
            String message=new String(messageEvent.getData(), "UTF-8");
            Log.v(TAG, "onMessageReceived " + messageEvent.getPath()+message);
            if(messageEvent.getPath().equals(COMMAND_SLIDE_COUNT)){
                Intent aIntent= new Intent("SLIDE_COUNT");
                aIntent.putExtra("DATA",message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
                sendLocalNotification(message);
            }
            if(messageEvent.getPath().equals(COMMAND_PRESENTATION_STOPPED)){
                cancelLocalNotification();
                Intent aIntent= new Intent("SLIDE_COUNT");
                aIntent.putExtra("DATA",NULL_STRING_COUNT);
                LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
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
    private static void sendMessage( final String path, final String text ) {
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
    private static void sendMessage( final String path) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(mGoogleApiClient!=null){
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                    for(Node node : nodes.getNodes()) {
                        Log.d(TAG, "SendMessage " + path );
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), path, null ).await();
                    }
                }
            }
        }).start();
    }

    private void cancelLocalNotification(){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void sendLocalNotification(String count) {
        Log.v(TAG, "sendLocalNotification");
        Intent startIntent;
        startIntent = new Intent(this, MainActivity.class).setAction(Intent.ACTION_VIEW);

        PendingIntent startPendingIntent = PendingIntent.getActivity(this, 0, startIntent, 0);

        Notification notify = new NotificationCompat.Builder(this)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(getNotificationMessage(count))
                .setLocalOnly(true)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(startPendingIntent)
                .build();

        cancelLocalNotification();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notify);

    }

    private String getNotificationMessage(String count){
        return SLIDE+SPACE+count.substring(0,count.indexOf(COUNT_SPLIT))+SPACE+OF+SPACE+count.substring(1+count.indexOf(COUNT_SPLIT));
    }

    public static void commandNext(){
        sendMessage(COMMAND_NEXT);
    }
    public static void commandPrevious(){
        sendMessage(COMMAND_PREVIOUS);
    }
    public static void commandPauseResume(){
        sendMessage(COMMAND_PAUSERESUME);
    }
    public static void commandConnect(){
        sendMessage(COMMAND_CONNECT);
    }
    public static void commandAppPaused(){
        sendMessage(COMMAND_APP_PAUSED);
    }

}