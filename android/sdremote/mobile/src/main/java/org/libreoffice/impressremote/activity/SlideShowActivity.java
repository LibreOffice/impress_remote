/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.CommunicationService;
import org.libreoffice.impressremote.communication.CommunicationServiceWear;
import org.libreoffice.impressremote.communication.SlideShow;
import org.libreoffice.impressremote.communication.Timer;
import org.libreoffice.impressremote.fragment.TimerEditingDialog;
import org.libreoffice.impressremote.fragment.TimerSettingDialog;
import org.libreoffice.impressremote.fragment.slides.EmptySlideFragment;
import org.libreoffice.impressremote.fragment.slides.PointerFragment;
import org.libreoffice.impressremote.fragment.slides.SlidesGridFragment;
import org.libreoffice.impressremote.fragment.slides.SlidesPagerFragment;
import org.libreoffice.impressremote.util.Fragments;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.util.Preferences;
import org.libreoffice.impressremote.util.SavedStates;

public class SlideShowActivity extends AppCompatActivity implements ServiceConnection {
    public enum Mode {
        PAGER, GRID, EMPTY, STARTPOINTER, STOPPOINTER
    }

    private Mode mMode;

    private int mRingerMode;

    private CommunicationService mCommunicationService;
    private IntentsReceiver mIntentsReceiver;


    @Override
    protected void onCreate(Bundle aSavedInstanceState) {
        super.onCreate(aSavedInstanceState);

        mMode = loadMode(aSavedInstanceState);

        setUpFragment();
        setUpKeepingScreenOn();

        saveRingerMode(aSavedInstanceState);
        enableQuietMode();

        bindCommunicationService();
    }

    private Mode loadMode(Bundle aSavedInstanceState) {
        if (aSavedInstanceState == null) {
            return Mode.PAGER;
        }

        return (Mode) aSavedInstanceState.getSerializable(SavedStates.Keys.MODE);
    }


    private void setUpFragment() {
        Fragments.Operator.replaceAnimated(this, buildFragment());
    }

    private Fragment buildFragment() {
        switch (mMode) {
            case PAGER:
                return SlidesPagerFragment.newInstance();

            case GRID:
                return SlidesGridFragment.newInstance();

            case EMPTY:
                return EmptySlideFragment.newInstance();

            case STARTPOINTER:
                return PointerFragment.newInstance();

            default:
                return SlidesPagerFragment.newInstance();
        }
    }

    private void setUpKeepingScreenOn() {
        findViewById(android.R.id.content).setKeepScreenOn(isKeepingScreenOnRequired());
    }

    private boolean isKeepingScreenOnRequired() {
        Preferences aPreferences = Preferences.getSettingsInstance(this);

        return aPreferences.getBoolean(Preferences.Keys.KEEP_SCREEN_ON);
    }

    private void saveRingerMode(Bundle aSavedInstanceState) {
        if (aSavedInstanceState == null) {
            mRingerMode = getAudioManager().getRingerMode();
        } else {
            mRingerMode = aSavedInstanceState.getInt(SavedStates.Keys.RINGER_MODE);
        }
    }

    private AudioManager getAudioManager() {
        return (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    private void enableQuietMode() {
        if (!isQuietModeRequired()) {
            return;
        }

        getAudioManager().setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    }

    private boolean isQuietModeRequired() {
        Preferences aPreferences = Preferences.getSettingsInstance(this);

        return aPreferences.getBoolean(Preferences.Keys.QUIET_MODE);
    }

    private void bindCommunicationService() {
        Intent aIntent = Intents.buildCommunicationServiceIntent(this);
        bindService(aIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName mComponentName, IBinder aBinder) {
        CommunicationService.ServiceBinder aServiceBinder = (CommunicationService.ServiceBinder) aBinder;
        mCommunicationService = aServiceBinder.getService();

        wearableServiceConnect();

        startSlideShow();
        resumeTimer();
    }



    private void startSlideShow() {
        if (!isServiceBound()) {
            return;
        }

        if (mCommunicationService.getSlideShow().isRunning()) {
            setUpSlideShowInformation();
            return;
        }

        mCommunicationService.getCommandsTransmitter().startPresentation();
    }

    private void resumeTimer() {
        if (!isServiceBound()) {
            return;
        }

        mCommunicationService.getSlideShow().getTimer().resume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        resumeTimer();

        registerIntentsReceiver();
    }

    private void registerIntentsReceiver() {
        mIntentsReceiver = new IntentsReceiver(this);
        IntentFilter aIntentFilter = buildIntentsReceiverFilter();

        getBroadcastManager().registerReceiver(mIntentsReceiver, aIntentFilter);
    }

    private static final class IntentsReceiver extends BroadcastReceiver {
        private final SlideShowActivity mSlideShowActivity;

        private IntentsReceiver(SlideShowActivity aSlideShowActivity) {
            mSlideShowActivity = aSlideShowActivity;
        }

        @Override
        public void onReceive(Context aContext, Intent aIntent) {
            if (Intents.Actions.SLIDE_SHOW_MODE_CHANGED.equals(aIntent.getAction())) {
                Mode aMode = (Mode) aIntent.getSerializableExtra(Intents.Extras.MODE);
                mSlideShowActivity.changeMode(aMode);
                return;
            }

            if (Intents.Actions.SLIDE_CHANGED.equals(aIntent.getAction())) {
                mSlideShowActivity.setUpSlideShowInformation();
                mSlideShowActivity.slideCountMessage();
                return;
            }

            if (Intents.Actions.TIMER_UPDATED.equals(aIntent.getAction())) {
                mSlideShowActivity.setUpSlideShowInformation();
                return;
            }

            if (Intents.Actions.TIMER_STARTED.equals(aIntent.getAction())) {
                int aMinutesLength = aIntent.getIntExtra(Intents.Extras.MINUTES, 0);
                mSlideShowActivity.startTimer(aMinutesLength);
                mSlideShowActivity.setUpSlideShowInformation();
                return;
            }

            if (Intents.Actions.TIMER_RESUMED.equals(aIntent.getAction())) {
                mSlideShowActivity.resumeTimer();
                return;
            }

            if (Intents.Actions.TIMER_CHANGED.equals(aIntent.getAction())) {
                int aMinutesLength = aIntent.getIntExtra(Intents.Extras.MINUTES, 0);
                mSlideShowActivity.changeTimer(aMinutesLength);
                mSlideShowActivity.resumeTimer();
                mSlideShowActivity.setUpSlideShowInformation();
            }

            if (Intents.Actions.WEAR_NEXT.equals(aIntent.getAction())) {
                mSlideShowActivity.nextTransition();
            }
            if (Intents.Actions.WEAR_PREVIOUS.equals(aIntent.getAction())) {
                mSlideShowActivity.previousTransition();
            }
/*            if (Intents.Actions.WEAR_PAUSE.equals(aIntent.getAction())) {
                mSlideShowActivity.pausePresentation();
            }
            if (Intents.Actions.WEAR_RESUME.equals(aIntent.getAction())) {
                mSlideShowActivity.resumePresentation();
            }*/
            if (Intents.Actions.WEAR_CONNECT.equals(aIntent.getAction())) {
                mSlideShowActivity.slideCountMessage();
            }
            if (Intents.Actions.WEAR_EXIT.equals(aIntent.getAction())) {
                mSlideShowActivity.showWearNotification();
            }
            if (Intents.Actions.WEAR_PAUSE_RESUME.equals(aIntent.getAction())) {
                mSlideShowActivity.pauseResumePresentation();
            }
            if (Intents.Actions.GOOGLE_API_CONNECTED.equals(aIntent.getAction())) {
                mSlideShowActivity.showWearNotification();
            }
        }
    }

    private IntentFilter buildIntentsReceiverFilter() {
        IntentFilter aIntentFilter = new IntentFilter();
        aIntentFilter.addAction(Intents.Actions.SLIDE_SHOW_MODE_CHANGED);
        aIntentFilter.addAction(Intents.Actions.SLIDE_CHANGED);
        aIntentFilter.addAction(Intents.Actions.TIMER_UPDATED);
        aIntentFilter.addAction(Intents.Actions.TIMER_STARTED);
        aIntentFilter.addAction(Intents.Actions.TIMER_RESUMED);
        aIntentFilter.addAction(Intents.Actions.TIMER_CHANGED);
        aIntentFilter.addAction(Intents.Actions.GOOGLE_API_CONNECTED);
        aIntentFilter.addAction(Intents.Actions.WEAR_NEXT);
        aIntentFilter.addAction(Intents.Actions.WEAR_PREVIOUS);
/*        aIntentFilter.addAction(Intents.Actions.WEAR_PAUSE);
        aIntentFilter.addAction(Intents.Actions.WEAR_RESUME);*/
        aIntentFilter.addAction(Intents.Actions.WEAR_CONNECT);
        aIntentFilter.addAction(Intents.Actions.WEAR_EXIT);
        aIntentFilter.addAction(Intents.Actions.WEAR_PAUSE_RESUME);

        return aIntentFilter;
    }

    private LocalBroadcastManager getBroadcastManager() {
        return LocalBroadcastManager.getInstance(getApplicationContext());
    }

    private void setUpSlideShowInformation() {
        if (!isServiceBound()) {
            return;
        }

        ActionBar aActionBar = getSupportActionBar();

        aActionBar.setTitle(buildSlideShowProgress());
        aActionBar.setSubtitle(buildSlideShowTimerProgress());
    }

    private boolean isServiceBound() {
        return mCommunicationService != null;
    }

    private String buildSlideShowProgress() {
        SlideShow aSlideShow = mCommunicationService.getSlideShow();

        int aCurrentSlideIndex = aSlideShow.getHumanCurrentSlideIndex();
        int aSlidesCount = aSlideShow.getSlidesCount();

        return getString(R.string.mask_slide_show_progress, aCurrentSlideIndex, aSlidesCount);
    }

    private String buildSlideShowTimerProgress() {
        Timer aTimer = mCommunicationService.getSlideShow().getTimer();

        if (!aTimer.isSet()) {
            return null;
        }

        if (aTimer.isTimeUp()) {
            return getString(R.string.message_time_is_up);
        }

        int aMinutesLeft = aTimer.getMinutesLeft();

        return getResources().getQuantityString(R.plurals.mask_timer_progress, aMinutesLeft, aMinutesLeft);
    }

    private void startTimer(int aMinutesLength) {
        Timer aTimer = mCommunicationService.getSlideShow().getTimer();

        aTimer.setMinutesLength(aMinutesLength);
        aTimer.start();
    }

    private void changeTimer(int aMinutesLength) {
        Timer aTimer = mCommunicationService.getSlideShow().getTimer();

        if (aTimer.isTimeUp()) {
            aTimer.reset();
        }

        aTimer.setMinutesLength(aMinutesLength);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpSlideShowInformation();
    }

    @Override
    public boolean onKeyDown(int aKeyCode, KeyEvent aKeyEvent) {
        if (!areVolumeKeysActionsRequired()) {
            return super.onKeyDown(aKeyCode, aKeyEvent);
        }

        switch (aKeyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (!isLastSlideDisplayed()) {
                    mCommunicationService.getCommandsTransmitter().performNextTransition();
                }
                return true;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mCommunicationService.getCommandsTransmitter().performPreviousTransition();
                return true;

            default:
                return super.onKeyDown(aKeyCode, aKeyEvent);
        }
    }

    private boolean areVolumeKeysActionsRequired() {
        Preferences aPreferences = Preferences.getSettingsInstance(this);

        return aPreferences.getBoolean(Preferences.Keys.VOLUME_KEYS_ACTIONS);
    }

    private boolean isLastSlideDisplayed() {
        int aCurrentSlideIndex = mCommunicationService.getSlideShow().getHumanCurrentSlideIndex();
        int aSlidesCount = mCommunicationService.getSlideShow().getSlidesCount();

        return aCurrentSlideIndex == aSlidesCount;
    }
    private boolean isFirstSlideDisplayed(){
        return(mCommunicationService.getSlideShow().getHumanCurrentSlideIndex()==1);
    }

    @Override
    public boolean onKeyUp(int aKeyCode, KeyEvent aKeyEvent) {
        if (!areVolumeKeysActionsRequired()) {
            return super.onKeyUp(aKeyCode, aKeyEvent);
        }

        // Suppress sound of volume changing

        switch (aKeyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;

            default:
                return super.onKeyUp(aKeyCode, aKeyEvent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu aMenu) {
        getMenuInflater().inflate(R.menu.menu_action_bar_slide_show, aMenu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu aMenu) {
        MenuItem aSlidesPagerMenuItem = aMenu.findItem(R.id.menu_slides_pager);
        MenuItem aSlidesGridMenuItem = aMenu.findItem(R.id.menu_slides_grid);
        MenuItem aSlideShowResumeMenuItem = aMenu.findItem(R.id.menu_resume_slide_show);
        MenuItem aStartPointerMenuItem = aMenu.findItem(R.id.menu_start_pointer);
        MenuItem aStopPointerMenuItem = aMenu.findItem(R.id.menu_stop_pointer);

        switch (mMode) {
            case PAGER:
                setMenuItemsVisibility(aMenu, true);
                aSlidesPagerMenuItem.setVisible(false);
                aSlidesGridMenuItem.setVisible(true);
                aSlideShowResumeMenuItem.setVisible(false);
                aStopPointerMenuItem.setVisible(false);
                break;

            case GRID:
                setMenuItemsVisibility(aMenu, true);
                aSlidesPagerMenuItem.setVisible(true);
                aSlidesGridMenuItem.setVisible(false);
                aSlideShowResumeMenuItem.setVisible(false);
                aStartPointerMenuItem.setVisible(false);
                aStopPointerMenuItem.setVisible(false);
                break;

            case EMPTY:
                setMenuItemsVisibility(aMenu, false);
                aSlideShowResumeMenuItem.setVisible(true);
                break;

            case STARTPOINTER:
                setMenuItemsVisibility(aMenu, true);
                aStartPointerMenuItem.setVisible(false);
                aSlidesPagerMenuItem.setVisible(true);
                aSlidesGridMenuItem.setVisible(false);
                aSlideShowResumeMenuItem.setVisible(false);
                break;

            default:
                break;
        }

        return super.onPrepareOptionsMenu(aMenu);
    }

    private void setMenuItemsVisibility(Menu aMenu, boolean aAreItemsVisible) {
        for (int aItemIndex = 0; aItemIndex < aMenu.size(); aItemIndex++) {
            aMenu.getItem(aItemIndex).setVisible(aAreItemsVisible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        switch (aMenuItem.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;

            case R.id.menu_slides_grid:
                changeMode(Mode.GRID);
                return true;

            case R.id.menu_slides_pager:
            case R.id.menu_stop_pointer:
                changeMode(Mode.PAGER);
                return true;

            case R.id.menu_timer:
                callTimer();
                return true;

            case R.id.menu_resume_slide_show:
                changeMode(Mode.PAGER);
                setUpSlideShowInformation();
                resumeSlideShow();
                resumeTimer();
                return true;

            case R.id.menu_pause_slide_show:
                changeMode(Mode.EMPTY);
                setUpSlideShowPausedInformation();
                pauseSlideShow();
                pauseTimer();
                return true;

            case R.id.menu_stop_slide_show:
                stopSlideShow();
                return true;

            case R.id.menu_start_pointer:
                changeMode(Mode.STARTPOINTER);
                return true;

            default:
                return super.onOptionsItemSelected(aMenuItem);
        }
    }

    private void navigateUp() {
        finish();
    }

    private void changeMode(Mode aMode) {
        mMode = aMode;

        setUpFragment();
        refreshActionBarMenu();
    }
    private boolean modeIsEmpty(){
        return mMode==Mode.EMPTY;
    }

    private void refreshActionBarMenu() {
        supportInvalidateOptionsMenu();
    }

    private void callTimer() {
        Timer aTimer = mCommunicationService.getSlideShow().getTimer();

        if (aTimer.isSet()) {
            callEditingTimer(aTimer);
        } else {
            callSettingTimer();
        }
    }

    private void callEditingTimer(Timer aTimer) {
        DialogFragment aTimerDialog = buildTimerEditingDialog(aTimer);
        aTimerDialog.show(getSupportFragmentManager(), TimerEditingDialog.TAG);

        pauseTimer();
    }

    private DialogFragment buildTimerEditingDialog(Timer aTimer) {
        if (aTimer.isTimeUp()) {
            return TimerEditingDialog.newInstance(aTimer.getMinutesLength());
        } else {
            return TimerEditingDialog.newInstance(aTimer.getMinutesLeft());
        }
    }

    private void callSettingTimer() {
        DialogFragment aTimerDialog = TimerSettingDialog.newInstance();
        aTimerDialog.show(getSupportFragmentManager(), TimerSettingDialog.TAG);
    }

    private void resumeSlideShow() {
        mCommunicationService.getCommandsTransmitter().resumePresentation();
    }

    private void pauseSlideShow() {
        mCommunicationService.getCommandsTransmitter().setUpBlankScreen();
    }

    private void setUpSlideShowPausedInformation() {
        ActionBar aActionBar = getSupportActionBar();

        aActionBar.setTitle(R.string.title_slide_show);
        aActionBar.setSubtitle(R.string.message_paused);
    }

    private void pauseTimer() {
        mCommunicationService.getSlideShow().getTimer().pause();
    }

    private void stopSlideShow() {
        mCommunicationService.getCommandsTransmitter().stopPresentation();

        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

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
    protected void onSaveInstanceState(Bundle aOutState) {
        super.onSaveInstanceState(aOutState);

        saveMode(aOutState);
        rememberRingerMode(aOutState);
    }

    private void saveMode(Bundle aOutState) {
        aOutState.putSerializable(SavedStates.Keys.MODE, mMode);
    }

    private void rememberRingerMode(Bundle aOutState) {
        aOutState.putInt(SavedStates.Keys.RINGER_MODE, mRingerMode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disableQuietMode();

        if (!isServiceBound()) {
            return;
        }

        unbindService();
    }

    private void disableQuietMode() {
        if (!isQuietModeRequired()) {
            return;
        }

        getAudioManager().setRingerMode(mRingerMode);
    }

    private void unbindService() {
        unbindService(this);

        wearableServiceDisconnect();

    }

    @Override
    public void onServiceDisconnected(ComponentName aComponentName) {
        mCommunicationService = null;
    }

    /**
     * Used in Wear control
     */

    private void nextTransition(){
        if (!isLastSlideDisplayed() && !modeIsEmpty()) {
            mCommunicationService.getCommandsTransmitter().performNextTransition();
        }
    }
    private void previousTransition(){
        if(!isFirstSlideDisplayed() && !modeIsEmpty()){
            mCommunicationService.getCommandsTransmitter().performPreviousTransition();
        }
    }
    private void pausePresentation(){
        Log.d("SlideShowActivity","pausePresentation");
        changeMode(Mode.EMPTY);
        setUpSlideShowPausedInformation();
        pauseSlideShow();
        pauseTimer();
        CommunicationServiceWear.presentationPaused();
    }
    private void resumePresentation(){
        Log.d("SlideShowActivity", "resumePresentation");
        CommunicationServiceWear.ignoreNextSync();
        changeMode(Mode.PAGER);
        setUpSlideShowInformation();
        resumeSlideShow();
        resumeTimer();
        CommunicationServiceWear.presentationResumed();
    }
    private void pauseResumePresentation(){
        if(modeIsEmpty()){
            resumePresentation();
        }else{
            pausePresentation();
        }
    }

    private void slideCountMessage(){
        Log.d("SlideShowActivity", "slideCountMessage");
        CommunicationServiceWear.syncData(getSlideCount(), getSlidePreview());
    }

    private void showWearNotification(){
        Log.d("SlideShowActivity", "showWearNotification");
        CommunicationServiceWear.syncData(getSlideCount(), getSlidePreview());
    }

    private String getSlideCount(){
        return mCommunicationService.getSlideShow().getHumanCurrentSlideIndex()
                +"/"+mCommunicationService.getSlideShow().getSlidesCount();
    }

    private byte[] getSlidePreview(){
        Log.d("SlideShowActivity","getSlidePreview");
        int mCurrentSlideIndex = mCommunicationService.getSlideShow().getCurrentSlideIndex();
        return mCommunicationService.getSlideShow().getSlidePreviewBytes(mCurrentSlideIndex);
    }

    private void wearableServiceConnect() {
        // TODO add option for enabling android wear
        this.startService(new Intent(this, CommunicationServiceWear.class));
    }
    private void wearableServiceDisconnect() {
        this.stopService(new Intent(this, CommunicationServiceWear.class));
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
