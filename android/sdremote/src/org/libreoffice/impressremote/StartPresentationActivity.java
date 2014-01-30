package org.libreoffice.impressremote;

import org.libreoffice.impressremote.communication.CommunicationService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.SherlockActivity;

public class StartPresentationActivity extends SherlockActivity {
    private CommunicationService mCommunicationService = null;
    private boolean mIsBound = false;
    private ActivityChangeBroadcastProcessor mBroadcastProcessor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_startpresentation);
        bindService(new Intent(this, CommunicationService.class), mConnection,
                        Context.BIND_IMPORTANT);
        mIsBound = true;

        IntentFilter aFilter = new IntentFilter(
                        CommunicationService.MSG_SLIDESHOW_STARTED);

        mBroadcastProcessor = new ActivityChangeBroadcastProcessor(this);
        mBroadcastProcessor.addToFilter(aFilter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,
                        aFilter);

        findViewById(R.id.startpresentation_button).setOnClickListener(
                        mClickListener);
    }

    @Override
    public void onBackPressed() {
        Intent aIntent = new Intent(this, SelectorActivity.class);
        aIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(aIntent);
        mCommunicationService.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName aClassName,
                        IBinder aService) {
            mCommunicationService = ((CommunicationService.CBinder) aService)
                            .getService();

            if (mCommunicationService.isSlideShowRunning()) {
                Intent nIntent = new Intent(StartPresentationActivity.this,
                                PresentationActivity.class);
                startActivity(nIntent);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName aClassName) {
            mCommunicationService = null;
            mIsBound = false;
        }
    };

    private OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mCommunicationService != null) {
                mCommunicationService.getTransmitter().startPresentation();
            }
        }
    };

    private BroadcastReceiver mListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context aContext, Intent aIntent) {
            mBroadcastProcessor.onReceive(aContext, aIntent);
        }
    };
}
