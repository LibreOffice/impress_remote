/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.communication;

import static org.libreoffice.impressremote.communication.Commands.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.libreoffice.impressremote.BuildConfig;
import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.activity.FullscreenActivity;
import org.libreoffice.impressremote.activity.NotificationActivity;
import org.libreoffice.impressremote.util.SlideShowData;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class DataLayerListenerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DataLayerListenerSrvc";

    private static final int NOTIFICATION_ID=1;
    private static final String NOTIFICATION_TITLE="Impress Remote";
    private static final String NOTIFICATION_CHANNEL="LibreOffice Slide Notifications";

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
        // notification channel requires for Oreo or later...
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(BuildConfig.APPLICATION_ID, NOTIFICATION_CHANNEL, importance);
            NotificationManagerCompat.from(this).createNotificationChannel(notificationChannel);
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
        Log.v(TAG, "onConnectionSuspended called");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "onConnectionFailed called");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG, "onConnected called");
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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        String message=new String(messageEvent.getData(), StandardCharsets.UTF_8);
        Log.v(TAG, "onMessageReceived " + messageEvent.getPath()+message);
        if(messageEvent.getPath().equals(COMMAND_PRESENTATION_STOPPED)){
            cancelLocalNotification();
            if(SlideShowData.getInstance().isFullscreen()){
                Intent aIntent= new Intent(COMMAND_PRESENTATION_STOPPED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
            }
        }
        if(messageEvent.getPath().equals(COMMAND_PRESENTATION_PAUSED)){
            Intent aIntent= new Intent(COMMAND_PRESENTATION_PAUSED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
        if(messageEvent.getPath().equals(COMMAND_PRESENTATION_RESUMED)){
            Intent aIntent= new Intent(COMMAND_PRESENTATION_RESUMED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "Data Changed");
        for(DataEvent dataEvent: dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                Log.v(TAG, "Type Changed");
                String path=dataEvent.getDataItem().getUri().getPath();
                if(path.equals(COMMAND_NOTIFY)){
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                    final String count = dataMapItem.getDataMap().getString(INTENT_COUNT);
                    SlideShowData.getInstance().setCount(count);
                    if( dataMapItem.getDataMap().getBoolean(INTENT_HAS_ASSET)){
                        Asset asset = dataMapItem.getDataMap().getAsset(INTENT_PREVIEW);
                        SlideShowData.getInstance().setPreview(loadBitmapFromAsset(asset));
                        SlideShowData.getInstance().setHasPreview(true);
                    }else{
                        SlideShowData.getInstance().setHasPreview(false);
                    }
                    notifyActivity();
                }
            }
        }
    }

    private static void sendMessage( final String path) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(mGoogleApiClient!=null){
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                    for (Node node : nodes.getNodes()) {
                        Log.d(TAG, "SendMessage " + path);
                        Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), path, null).await();
                    }
                }
            }
        }).start();
    }

    private void notifyActivity(){
        if(SlideShowData.getInstance().isFullscreen()){
            broadcastLocalIntent();
        }else{
            sendLocalNotification();
        }
    }

    private void broadcastLocalIntent(){
        Log.v(TAG, "broadcastLocalIntent");
        Intent aIntent= new Intent(INTENT_UPDATE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(aIntent);
    }

    private void sendLocalNotification() {
        Log.v(TAG, "sendLocalNotification");

        Intent notificationIntent = new Intent(this, NotificationActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startIntent = new Intent(this, FullscreenActivity.class).setAction(Intent.ACTION_VIEW);
        PendingIntent startPendingIntent = PendingIntent.getActivity(this, 0, startIntent, 0);

        // WearOS doesn't allow background images or custom layouts in notifications anymore,
        // user has to open the main activity to control the slideshow.
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(NOTIFICATION_TITLE)
                        .setContentText(SlideShowData.getInstance().getCount())
                        .setOngoing(true)
                        .setContentIntent(startPendingIntent);
        if(SlideShowData.getInstance().hasPreview()){
            notificationBuilder.extend(new NotificationCompat.WearableExtender()
                    .setDisplayIntent(notificationPendingIntent).setBackground(SlideShowData.getInstance().getPreview()));
        }else{
            notificationBuilder.extend(new NotificationCompat.WearableExtender()
                    .setDisplayIntent(notificationPendingIntent));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void cancelLocalNotification(){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFICATION_ID);
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

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(5000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }

        return BitmapFactory.decodeStream(assetInputStream);
    }

}