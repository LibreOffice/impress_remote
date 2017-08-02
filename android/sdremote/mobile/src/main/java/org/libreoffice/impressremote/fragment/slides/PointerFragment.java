/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.fragment.slides;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.libreoffice.impressremote.communication.SlideShow;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.adapter.SlidesPagerAdapter;
import org.libreoffice.impressremote.communication.CommunicationService;

public class PointerFragment extends AbstractSlideFragment implements ServiceConnection, View.OnTouchListener {
    private CommunicationService mCommunicationService;
    private int displayheight, displaywidth, xoffset, yoffset;
    private long nextUpdate = 0;
    private static final int REFRESH_MILLIS = 40; //25 fps refresh

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

        if (!isServiceBound()) {
            return;
        }

        if (!isAdded()) {
            return;
        }

        ViewPager aSlidesPager = getSlidesPager();

        aSlidesPager.setAdapter(buildSlidesAdapter());

        setUpCurrentSlide();

        // get the real width/height of the preview
        int iH = ((ImageView) aSlidesPager.getChildAt(0)).getDrawable().getIntrinsicHeight();
        int iW = ((ImageView) aSlidesPager.getChildAt(0)).getDrawable().getIntrinsicWidth();
        int dH = aSlidesPager.getHeight();
        int dW = aSlidesPager.getWidth();
        if (dH/iH<=dW/iW) {
            //height is limiting
            displaywidth = iW*dH/iH;
            displayheight = dH;
            xoffset = (dW-displaywidth)/2;
            yoffset = 0;
        } else {
            displaywidth = dW;
            displayheight = iH*dW/iW;
            xoffset = 0;
            yoffset = (dH-displayheight)/2;
        }
    }

    private ViewPager getSlidesPager() {
        return (ViewPager) getView().findViewById(R.id.pointer_pager_slides);
    }

    private PagerAdapter buildSlidesAdapter() {
        SlideShow aSlideShow = mCommunicationService.getSlideShow();

        return new SlidesPagerAdapter(getActivity(), aSlideShow, this);
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
    void slideShowStateChanged() {
        // TODO: we should really do something special for end of slideshow, but we don't handle
        // that at all anywhere for now.
        setUpCurrentSlide();
    }

    @Override
    void slideChanged() {
        setUpCurrentSlide();
    }

    @Override
    void previewUpdated(int nSlideIndex) {
        setUpCurrentSlide();
    }

    @Override
    void notesUpdated(int nSlideIndex) {
        // We don't show notes (yet) -- ignore.
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
        float x = (event.getX() - xoffset) / displaywidth;
        float y = (event.getY() - yoffset) / displayheight;

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
            if (nextUpdate <= event.getEventTime()) {
                mCommunicationService.getCommandsTransmitter().movePointer(x,y);
                nextUpdate = event.getEventTime() + REFRESH_MILLIS;
            }
        break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_CANCEL:
        // a pointer was removed
            mCommunicationService.getCommandsTransmitter().stopPointer();
            break;
        default:
            // We specifically don't care about all other events.
            return false;
        }
        return true;
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
