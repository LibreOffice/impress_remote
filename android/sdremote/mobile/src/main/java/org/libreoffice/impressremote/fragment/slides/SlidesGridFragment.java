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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.libreoffice.impressremote.activity.SlideShowActivity;
import org.libreoffice.impressremote.communication.SlideShow;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.adapter.SlidesGridAdapter;
import org.libreoffice.impressremote.communication.CommunicationService;

public class SlidesGridFragment extends AbstractSlideFragment implements ServiceConnection, AdapterView.OnItemClickListener {
    // We need to keep track of this in order to know which slide needs 'resetting' when we change
    // slides (i.e. the previously selected slide needs to have its highlighting removed,
    // and the new selected slide needs to be highlighted -- there is nowhere else to retrieve which
    // slide was previously selected for now).
    private int mCurrentSlideIndex = 0;

    private CommunicationService mCommunicationService;

    public static SlidesGridFragment newInstance() {
        return new SlidesGridFragment();
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedInstanceState) {
        return aInflater.inflate(R.layout.fragment_slides_grid, aContainer, false);
    }

    @Override
    public void onActivityCreated(Bundle aSavedInstanceState) {
        super.onActivityCreated(aSavedInstanceState);

        bindService();
    }

    private void bindService() {
        Intent aServiceIntent = Intents.buildCommunicationServiceIntent(getActivity());
        getActivity().bindService(aServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName aComponentName, IBinder aBinder) {
        CommunicationService.ServiceBinder aServiceBinder = (CommunicationService.ServiceBinder) aBinder;
        mCommunicationService = aServiceBinder.getService();

        setUpSlidesGrid();
    }

    private void setUpSlidesGrid() {
        if (!isAdded()) {
            return;
        }

        GridView aSlidesGrid = getSlidesGrid();

        aSlidesGrid.setAdapter(buildSlidesAdapter());
        aSlidesGrid.setOnItemClickListener(this);

        mCurrentSlideIndex = mCommunicationService.getSlideShow().getCurrentSlideIndex();
    }

    private GridView getSlidesGrid() {
        return (GridView) getView().findViewById(R.id.grid_slides);
    }

    private SlidesGridAdapter buildSlidesAdapter() {
        SlideShow aSlideShow = mCommunicationService.getSlideShow();

        return new SlidesGridAdapter(getActivity(), aSlideShow);
    }

    @Override
    public void onItemClick(AdapterView<?> aAdapterView, View aView, int aPosition, long aId) {
        changeCurrentSlide(aPosition);
        changeSlideShowMode();
    }

    private void changeCurrentSlide(int aSlideIndex) {
        mCommunicationService.getCommandsTransmitter().setCurrentSlide(aSlideIndex);
    }

    private void changeSlideShowMode() {
        Intent aIntent = Intents.buildSlideShowModeChangedIntent(SlideShowActivity.Mode.PAGER);
        getBroadcastManager().sendBroadcast(aIntent);
    }

    private LocalBroadcastManager getBroadcastManager() {
        Context aContext = getActivity().getApplicationContext();

        return LocalBroadcastManager.getInstance(aContext);
    }

    @Override
    public void onServiceDisconnected(ComponentName aComponentName) {
        mCommunicationService = null;
    }

    @Override
    void slideShowStateChanged() {
        refreshSlidesGrid();
    }

    @Override
    void slideChanged() {
        refreshSlidePreview(mCurrentSlideIndex);
        mCurrentSlideIndex = mCommunicationService.getSlideShow().getCurrentSlideIndex();
        refreshSlidePreview(mCurrentSlideIndex);
        // TODO: we should probably just make the adapter cleverer so it can tell whether or not
        // it needs to change a slide rather than brute-forcing from this end. This would also
        // avoid completely rebuilding the view, i.e. we would know whether we need to refresh
        // the preview independently of the highlighting changes.
    }

    @Override
    void previewUpdated(int nSlideIndex) {
        refreshSlidePreview(nSlideIndex);
    }

    @Override
    void notesUpdated(int nSlideIndex) {
        // We don't care about notes in the grid view.
    }

    private void refreshSlidesGrid() {
        getSlidesGrid().invalidateViews();
    }

    private void refreshSlidePreview(int aSlideIndex) {
        GridView aSlidesGrid = getSlidesGrid();
        View aSlideView = aSlidesGrid.getChildAt(aSlideIndex);

        if (aSlideView == null) {
            return;
        }

        aSlidesGrid.getAdapter().getView(aSlideIndex, aSlideView, aSlidesGrid);
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
