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
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.Html;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextSwitcher;

import org.libreoffice.impressremote.communication.SlideShow;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.adapter.SlidesPagerAdapter;
import org.libreoffice.impressremote.communication.CommunicationService;

public class SlidesPagerFragment extends AbstractSlideFragment
        implements ServiceConnection, ViewPager.OnPageChangeListener, View.OnTouchListener {
    private CommunicationService mCommunicationService;
    private GestureDetectorCompat mDetector;

    public static SlidesPagerFragment newInstance() {
        return new SlidesPagerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedInstanceState) {
        mDetector = new GestureDetectorCompat(aContainer.getContext(), new MyGestureListener());
        return aInflater.inflate(R.layout.fragment_slides_pager, aContainer, false);
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
        aSlidesPager.addOnPageChangeListener(this);

        setUpCurrentSlide();
        setUpCurrentSlideNotes();
    }

    private ViewPager getSlidesPager() {
        return (ViewPager) getView().findViewById(R.id.pager_slides);
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

    private void setUpCurrentSlideNotes() {
        SlideShow aSlideShow = mCommunicationService.getSlideShow();

        setUpSlideNotes(aSlideShow.getCurrentSlideIndex());
    }

    @Override
    public void onPageSelected(int aPosition) {
        if (mCommunicationService.getSlideShow().getCurrentSlideIndex() != aPosition) {
            mCommunicationService.getCommandsTransmitter().setCurrentSlide(aPosition);
        }

        setUpSlideNotes(aPosition);
    }

    private void setUpSlideNotes(int aSlideIndex) {
        if (!isSlideNotesLayoutAvailable()) {
            return;
        }

        if (!isSlideVisible(aSlideIndex)) {
            return;
        }

        if (!areSlideNotesAvailable(aSlideIndex)) {
            hideSlideNotes();
            return;
        }

        showSlideNotes(aSlideIndex);
        scrollSlideNotes();
    }

    private boolean isSlideNotesLayoutAvailable() {
        ViewGroup aSlideNotesLayout = (ViewGroup) getView().findViewById(R.id.layout_notes);

        return aSlideNotesLayout != null;
    }

    private boolean isSlideVisible(int aSlideIndex) {
        return aSlideIndex == getSlidesPager().getCurrentItem();
    }

    private boolean areSlideNotesAvailable(int aSlideIndex) {
        String aSlideNotes = mCommunicationService.getSlideShow().getSlideNotes(aSlideIndex);

        return !TextUtils.isEmpty(Html.fromHtml(aSlideNotes).toString().trim());
    }

    private void showSlideNotes(int aSlideIndex) {
        TextSwitcher aSlideNotesSwitcher = getSlideNotesSwitcher();
        String aSlideNotes = mCommunicationService.getSlideShow().getSlideNotes(aSlideIndex);

        aSlideNotesSwitcher.setText(Html.fromHtml(aSlideNotes));
    }

    private TextSwitcher getSlideNotesSwitcher() {
        return (TextSwitcher) getView().findViewById(R.id.text_switcher_notes);
    }

    private void scrollSlideNotes() {
        ScrollView aSlideNotesScroll = (ScrollView) getView().findViewById(R.id.scroll_notes);

        aSlideNotesScroll.scrollTo(0, 0);
    }

    private void hideSlideNotes() {
        TextSwitcher aSlideNotesSwitcher = getSlideNotesSwitcher();

        aSlideNotesSwitcher.setText(getString(R.string.message_notes_empty));
    }

    @Override
    public void onPageScrolled(int aPosition, float aPositionOffset, int aPositionOffsetPixels) {
    }

    @Override
    public void onPageScrollStateChanged(int aState) {
    }

    @Override
    public void onServiceDisconnected(ComponentName aComponentName) {
        mCommunicationService = null;
    }

    @Override
    void slideShowStateChanged() {
        setUpSlidesPager();
    }

    @Override
    void slideChanged() {
        setUpCurrentSlide();
    }

    @Override
    void previewUpdated(int nSlideIndex) {
        refreshSlide(nSlideIndex);
    }

    @Override
    void notesUpdated(int nSlideIndex) {
        setUpSlideNotes(nSlideIndex);
    }

    private void refreshSlide(int aSlideIndex) {
        // Refresh only loaded slides to avoid images blinking on large slides count.
        // There is no way to invalidate only a certain slide.

        // TODO: we probably want to implement what's needed (using a map) so that there is a way
        // to "invalidate only a certain slide".

        int aCurrentSlideIndex = mCommunicationService.getSlideShow().getCurrentSlideIndex();

        if (aSlideIndex == aCurrentSlideIndex) {
            refreshSlidesPager();
            return;
        }

        int aSlidesOffscreenCount = getSlidesPager().getOffscreenPageLimit();

        if (aSlideIndex < aCurrentSlideIndex - aSlidesOffscreenCount) {
            return;
        }

        if (aSlideIndex > aCurrentSlideIndex + aSlidesOffscreenCount) {
            return;
        }

        refreshSlidesPager();
    }

    private void refreshSlidesPager() {
        getSlidesPager().getAdapter().notifyDataSetChanged();
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
        return mDetector.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            // down is the start for everything, we want that..
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            mCommunicationService.getCommandsTransmitter().performNextTransition();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            mCommunicationService.getCommandsTransmitter().performPreviousTransition();
            return true;
        }
    }

}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
