/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.fragment.slides;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import org.libreoffice.impressremote.util.Intents;

/**
 * Should be used by all fragments that show the current slide in some way. AbstractSlideFragment
 * will notify it's subclasses about various changes that could require the UI to be updated.
 */
public abstract class AbstractSlideFragment extends Fragment {

    private BroadcastReceiver mIntentsReceiver;

    abstract void slideShowStateChanged();

    abstract void slideChanged();

    abstract void previewUpdated(int nSlideIndex);

    abstract void notesUpdated(int nSlideIndex);


    @Override
    public void onResume() {
        super.onResume();

        registerIntentsReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterIntentsReceiver();
    }

    private void unregisterIntentsReceiver() {
        try {
            getBroadcastManager().unregisterReceiver(mIntentsReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered.
            // Fixed in Honeycomb: Android’s issue #6191.
        }
    }

    private void registerIntentsReceiver() {
        mIntentsReceiver = new IntentsReceiver(this);
        IntentFilter aIntentFilter = buildIntentsReceiverFilter();

        getBroadcastManager().registerReceiver(mIntentsReceiver, aIntentFilter);
    }

    private static final class IntentsReceiver extends BroadcastReceiver {
        private final AbstractSlideFragment mSlidesPagerFragment;

        private IntentsReceiver(AbstractSlideFragment aSlidesPagerFragment) {
            mSlidesPagerFragment = aSlidesPagerFragment;
        }

        @Override
        public void onReceive(Context aContext, Intent aIntent) {
            if (aIntent.getAction().equals(Intents.Actions.SLIDE_SHOW_RUNNING) ||
                    aIntent.getAction().equals(Intents.Actions.SLIDE_SHOW_STOPPED))
            {
                mSlidesPagerFragment.slideShowStateChanged();
            } else if (aIntent.getAction().equals(Intents.Actions.SLIDE_CHANGED)) {
                mSlidesPagerFragment.slideChanged();
            } else if (aIntent.getAction().equals(Intents.Actions.SLIDE_PREVIEW)) {
                int nSlideIndex = aIntent.getIntExtra(Intents.Extras.SLIDE_INDEX, 0);
                mSlidesPagerFragment.previewUpdated(nSlideIndex);
            } else if (aIntent.getAction().equals(Intents.Actions.SLIDE_NOTES)) {
                int nSlideIndex = aIntent.getIntExtra(Intents.Extras.SLIDE_INDEX, 0);
                mSlidesPagerFragment.previewUpdated(nSlideIndex);
            }
        }
    }

    private IntentFilter buildIntentsReceiverFilter() {
        IntentFilter aIntentFilter = new IntentFilter();
        aIntentFilter.addAction(Intents.Actions.SLIDE_SHOW_RUNNING);
        aIntentFilter.addAction(Intents.Actions.SLIDE_SHOW_STOPPED);
        aIntentFilter.addAction(Intents.Actions.SLIDE_CHANGED);
        aIntentFilter.addAction(Intents.Actions.SLIDE_PREVIEW);
        aIntentFilter.addAction(Intents.Actions.SLIDE_NOTES);

        return aIntentFilter;
    }

    private LocalBroadcastManager getBroadcastManager() {
        Context aContext = getActivity().getApplicationContext();

        return LocalBroadcastManager.getInstance(aContext);
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
