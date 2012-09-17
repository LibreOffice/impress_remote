package org.libreoffice.impressremote;

import org.libreoffice.impressremote.communication.CommunicationService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * This class is used to centralise the processing of Broadcasts concerning
 * presentation/connection status changes which result in a change of activity.
 *
 * I.e. this will switch to the correct activity and correctly set up the
 * activity backstack when switching activity.
 *
 * To use create this on activity startup, and pass messages from your
 * BroadcastReceiver's onReceive.
 *
 */
public class ActivityChangeBroadcastProcessor {

    private Activity mActivity;

    public ActivityChangeBroadcastProcessor(Activity aActivity) {
        mActivity = aActivity;
    }

    public void addToFilter(IntentFilter aFilter) {
        aFilter.addAction(CommunicationService.STATUS_CONNECTED_NOSLIDESHOW);
        aFilter.addAction(CommunicationService.STATUS_CONNECTED_SLIDESHOW_RUNNING);
    }

    public void onReceive(Context aContext, Intent aIntent) {
        if (aIntent.getAction().equals(
                        CommunicationService.STATUS_CONNECTED_NOSLIDESHOW)) {
            Intent nIntent = new Intent(mActivity,
                            StartPresentationActivity.class);
            nIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            mActivity.startActivity(nIntent);
        } else if (aIntent
                        .getAction()
                        .equals(CommunicationService.STATUS_CONNECTED_SLIDESHOW_RUNNING)) {
            Intent nIntent = new Intent(mActivity, PresentationActivity.class);
            nIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            mActivity.startActivity(nIntent);
        }
    }

}
