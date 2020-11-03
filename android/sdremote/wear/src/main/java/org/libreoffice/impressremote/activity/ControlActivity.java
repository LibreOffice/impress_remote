/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;
import static org.libreoffice.impressremote.communication.Commands.*;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.DataLayerListenerService;
import org.libreoffice.impressremote.util.SlideShowData;

public abstract class ControlActivity extends Activity {
    private static final String TAG = "ControlActivity";
    private IntentsReceiver mIntentsReceiver;
    private boolean paused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        registerIntentsReceiver();
        this.startService(new Intent(this, DataLayerListenerService.class));
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        unregisterIntentsReceiver();
        this.stopService(new Intent(this, DataLayerListenerService.class));
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop(){
        Log.v(TAG, "onStop");
        super.onStop();
    }

    public void onButtonClickedPrevious(@SuppressWarnings("UnusedParameters") View target) {
        if(!paused){
            DataLayerListenerService.commandPrevious();
        }

    }

    public void onButtonClickedNext(@SuppressWarnings("UnusedParameters") View target) {
        if(!paused){
            DataLayerListenerService.commandNext();
        }
    }

    public void onButtonClickedPauseResume(@SuppressWarnings("UnusedParameters") View target) {
        DataLayerListenerService.commandPauseResume();
    }

    public void changeSlideCount(String count){
        TextView textView;
        textView = (TextView) findViewById(R.id.textView_counter);
        textView.setText(count);
    }

    public void reset(){
        changeSlideCount(NULL_STRING_COUNT);
    }

    public void changePreview(Bitmap bitmap){
        Log.v(TAG,"changePreview");
        if(!SlideShowData.getInstance().isFullscreen()){
            return;
        }
        ImageView imageView;
        imageView=(ImageView) findViewById(R.id.preview);
        if(imageView==null){
            Log.v(TAG,"Null imageview");
            return;
        }
        if(bitmap==null){
            imageView.setImageDrawable(new ColorDrawable(Color.WHITE));
        }else{
            imageView.setImageBitmap(bitmap);
        }
    }

    private void registerIntentsReceiver() {
        mIntentsReceiver = new IntentsReceiver(this);
        IntentFilter aIntentFilter = new IntentFilter();
        aIntentFilter.addAction(INTENT_UPDATE);
        aIntentFilter.addAction(COMMAND_PRESENTATION_STOPPED);
        aIntentFilter.addAction(COMMAND_PRESENTATION_PAUSED);
        aIntentFilter.addAction(COMMAND_PRESENTATION_RESUMED);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mIntentsReceiver, aIntentFilter);
    }

    private void unregisterIntentsReceiver() {
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mIntentsReceiver);
        } catch (IllegalArgumentException ignored) {

        }
    }

    private static final class IntentsReceiver extends BroadcastReceiver {
        private final ControlActivity activity;
        private IntentsReceiver(ControlActivity aControlActivity) {
            activity = aControlActivity;
        }
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(INTENT_UPDATE)){
                activity.changeSlideCount(SlideShowData.getInstance().getCount());
                if(SlideShowData.getInstance().hasPreview()){
                    activity.changePreview(SlideShowData.getInstance().getPreview());
                }
            }else if(intent.getAction().equals(COMMAND_PRESENTATION_STOPPED)){
                activity.reset();
            }else if(intent.getAction().equals(COMMAND_PRESENTATION_PAUSED)){
                activity.setPaused(true);
            }
            else if(intent.getAction().equals(COMMAND_PRESENTATION_RESUMED)){
                activity.setPaused(false);
            }
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        ImageButton imageButton=(ImageButton) findViewById(R.id.button_pauseStart);
        if(paused){
            imageButton.setImageResource(R.drawable.ic_action_play);
        }else{
            imageButton.setImageResource(R.drawable.ic_action_pause);
        }
    }
}
