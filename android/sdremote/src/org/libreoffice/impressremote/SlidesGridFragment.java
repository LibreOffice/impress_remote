/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;
import org.libreoffice.impressremote.communication.CommunicationService;

public class SlidesGridFragment extends SherlockFragment implements ServiceConnection, AdapterView.OnItemClickListener {
    private CommunicationService mCommunicationService;
    private BroadcastReceiver mIntentsReceiver;

    public static SlidesGridFragment newInstance() {
        return new SlidesGridFragment();
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedInstanceState) {
        return aInflater.inflate(R.layout.fragment_slides_grid, aContainer, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        bindService();
    }

    private void bindService() {
        Intent aServiceIntent = new Intent(getActivity(), CommunicationService.class);

        getActivity().bindService(aServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName aComponentName, IBinder aBinder) {
        CommunicationService.CBinder aServiceBinder = (CommunicationService.CBinder) aBinder;

        mCommunicationService = aServiceBinder.getService();

        mCommunicationService.getTransmitter().startPresentation();

        setUpSlidesGrid();
    }

    private void setUpSlidesGrid() {
        SlidesGridAdapter aSlidesGridAdapter = new SlidesGridAdapter(getActivity(),
            mCommunicationService.getSlideShow());

        getSlidesGrid().setAdapter(aSlidesGridAdapter);
        getSlidesGrid().setOnItemClickListener(this);
    }

    private GridView getSlidesGrid() {
        return (GridView) getView().findViewById(R.id.grid_slides);
    }

    @Override
    public void onServiceDisconnected(ComponentName aComponentName) {
        mCommunicationService = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        registerIntentsReceiver();
    }

    private void registerIntentsReceiver() {
        mIntentsReceiver = new IntentsReceiver(this);
        IntentFilter aIntentFilter = buildIntentsReceiverFilter();

        getBroadcastManager().registerReceiver(mIntentsReceiver, aIntentFilter);
    }

    @Override
    public void onItemClick(AdapterView<?> aAdapterView, View aView, int aPosition, long aId) {
        mCommunicationService.getTransmitter().setCurrentSlide(aPosition);
    }

    private static final class IntentsReceiver extends BroadcastReceiver {
        private final SlidesGridFragment mSlidesGridFragment;

        private IntentsReceiver(SlidesGridFragment aSlidesGridFragment) {
            mSlidesGridFragment = aSlidesGridFragment;
        }

        @Override
        public void onReceive(Context aContext, Intent aIntent) {
            if (Intents.Actions.SLIDE_PREVIEW.equals(aIntent.getAction())) {
                mSlidesGridFragment.refreshSlidesGrid();
            }
        }
    }

    private IntentFilter buildIntentsReceiverFilter() {
        IntentFilter aIntentFilter = new IntentFilter();
        aIntentFilter.addAction(Intents.Actions.SLIDE_PREVIEW);

        return aIntentFilter;
    }

    private LocalBroadcastManager getBroadcastManager() {
        Context aContext = getActivity().getApplicationContext();

        return LocalBroadcastManager.getInstance(aContext);
    }

    private void refreshSlidesGrid() {
        getSlidesGrid().invalidateViews();
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService();
    }

    private void unbindService() {
        if (!isServiceBound()) {
            return;
        }

        getActivity().unbindService(this);
    }

    private boolean isServiceBound() {
        return mCommunicationService != null;
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
