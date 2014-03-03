/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.libreoffice.impressremote.communication.SlideShow;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.adapter.SlidesPagerAdapter;
import org.libreoffice.impressremote.communication.CommunicationService;

public class PointerFragment extends Fragment implements ServiceConnection, View.OnTouchListener {
    private CommunicationService mCommunicationService;
    private BroadcastReceiver mIntentsReceiver;

    public static PointerFragment newInstance() {
        return new PointerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedInstanceState) {
        return aInflater.inflate(R.layout.fragment_pointer, aContainer, false);
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

        setUpSlidesPager();
    }

    private void setUpSlidesPager() {
        if (!isServiceBound()) {
            return;
        }

        if (!isAdded()) {
            return;
        }

        ViewPager aSlidesPager = getSlidesPager();

        aSlidesPager.setAdapter(buildSlidesAdapter());
        aSlidesPager.setPageMargin(getSlidesMargin());

        setUpCurrentSlide();
    }

    private ViewPager getSlidesPager() {
        return (ViewPager) getView().findViewById(R.id.pointer_pager_slides);
    }

    private PagerAdapter buildSlidesAdapter() {
        SlideShow aSlideShow = mCommunicationService.getSlideShow();

        return new SlidesPagerAdapter(getActivity(), aSlideShow, this);
    }

    private int getSlidesMargin() {
        return getResources().getDimensionPixelSize(R.dimen.margin_slide);
    }

    private void setUpCurrentSlide() {
        if (!isServiceBound()) {
            return;
        }

        SlideShow aSlideShow = mCommunicationService.getSlideShow();

        getSlidesPager().setCurrentItem(aSlideShow.getCurrentSlideIndex());
    }

    @Override
    public void onServiceDisconnected(ComponentName aComponentName) {
        mCommunicationService = null;
    }

    private LocalBroadcastManager getBroadcastManager() {
        Context aContext = getActivity().getApplicationContext();

        return LocalBroadcastManager.getInstance(aContext);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        float x = event.getX() / v.getWidth();
        float y = event.getY() / v.getHeight();

        // get masked action
        int aMaskedAction = event.getActionMasked();

        switch (aMaskedAction)
        {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
        // a pointer start
            mCommunicationService.getCommandsTransmitter().startPointer(x,y);
        break;
        case MotionEvent.ACTION_MOVE:
        // a pointer was moved
            mCommunicationService.getCommandsTransmitter().movePointer(x,y);
        break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_CANCEL:
        // a pointer was removed
            mCommunicationService.getCommandsTransmitter().stopPointer();
        break;
        }
        return true;
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
