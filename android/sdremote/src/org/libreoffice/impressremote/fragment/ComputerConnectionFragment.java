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
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.libreoffice.impressremote.util.Intents;
import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.CommunicationService;
import org.libreoffice.impressremote.communication.Server;

public class ComputerConnectionFragment extends SherlockFragment implements ServiceConnection {
    private Server mComputer;

    private CommunicationService mCommunicationService;
    private BroadcastReceiver mIntentsReceiver;

    public static ComputerConnectionFragment newInstance(Server aComputer) {
        ComputerConnectionFragment aFragment = new ComputerConnectionFragment();

        aFragment.setArguments(buildArguments(aComputer));

        return aFragment;
    }

    private static Bundle buildArguments(Server aComputer) {
        Bundle aArguments = new Bundle();

        aArguments.putParcelable("COMPUTER", aComputer);

        return aArguments;
    }

    @Override
    public void onCreate(Bundle aSavedInstance) {
        super.onCreate(aSavedInstance);

        mComputer = getArguments().getParcelable("COMPUTER");

        setUpActionBarMenu();
    }

    private void setUpActionBarMenu() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedInstance) {
        return aInflater.inflate(R.layout.fragment_computer_connection, aContainer, false);
    }

    @Override
    public void onViewStateRestored(Bundle aSavedInstanceState) {
        super.onViewStateRestored(aSavedInstanceState);

        if (aSavedInstanceState == null) {
            return;
        }

        loadLayout(aSavedInstanceState);
    }

    private void loadLayout(Bundle aSavedInstanceState) {
        int aLayoutIndex = aSavedInstanceState.getInt("LAYOUT");

        getViewAnimator().setDisplayedChild(aLayoutIndex);
    }

    private ViewAnimator getViewAnimator() {
        return (ViewAnimator) getView().findViewById(R.id.view_animator);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        bindService();
    }

    private void bindService() {
        Intent aServiceIntent = Intents.buildCommunicationServiceIntent(getActivity());
        getActivity().bindService(aServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName aComponentName, IBinder aBinder) {
        CommunicationService.CBinder aServiceBinder = (CommunicationService.CBinder) aBinder;
        mCommunicationService = aServiceBinder.getService();

        connectToComputer();
    }

    private void connectToComputer() {
        if (!isServiceBound()) {
            return;
        }

        mCommunicationService.connectTo(mComputer);
    }

    private boolean isServiceBound() {
        return mCommunicationService != null;
    }

    @Override
    public void onServiceDisconnected(ComponentName aComponentName) {
        mCommunicationService = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        registerIntentsReceiver();
    }

    private void registerIntentsReceiver() {
        mIntentsReceiver = new IntentsReceiver(this);
        IntentFilter aIntentFilter = buildIntentsReceiverFilter();

        getBroadcastManager().registerReceiver(mIntentsReceiver, aIntentFilter);
    }

    private static class IntentsReceiver extends BroadcastReceiver {
        private final ComputerConnectionFragment mComputerConnectionFragment;

        public IntentsReceiver(ComputerConnectionFragment aComputerConnectionFragment) {
            mComputerConnectionFragment = aComputerConnectionFragment;
        }

        @Override
        public void onReceive(Context aContext, Intent aIntent) {
            if (Intents.Actions.PAIRING_VALIDATION.equals(aIntent.getAction())) {
                String aPin = aIntent.getStringExtra(Intents.Extras.PIN);

                mComputerConnectionFragment.setUpPinValidationInstructions(aPin);
                mComputerConnectionFragment.refreshActionBarMenu();

                return;
            }

            if (Intents.Actions.PAIRING_SUCCESSFUL.equals(aIntent.getAction())) {
                mComputerConnectionFragment.setUpPresentation();
                mComputerConnectionFragment.refreshActionBarMenu();

                return;
            }

            if (Intents.Actions.CONNECTION_FAILED.equals(aIntent.getAction())) {
                mComputerConnectionFragment.setUpErrorMessage();
                mComputerConnectionFragment.refreshActionBarMenu();
            }
        }
    }

    private IntentFilter buildIntentsReceiverFilter() {
        IntentFilter aIntentFilter = new IntentFilter();
        aIntentFilter.addAction(Intents.Actions.PAIRING_VALIDATION);
        aIntentFilter.addAction(Intents.Actions.PAIRING_SUCCESSFUL);
        aIntentFilter.addAction(Intents.Actions.CONNECTION_FAILED);

        return aIntentFilter;
    }

    private LocalBroadcastManager getBroadcastManager() {
        Context aContext = getActivity().getApplicationContext();

        return LocalBroadcastManager.getInstance(aContext);
    }

    private void setUpPinValidationInstructions(String aPin) {
        TextView aPinTextView = (TextView) getView().findViewById(R.id.text_pin);
        aPinTextView.setText(aPin);

        showPinValidationLayout();
    }

    private void showPinValidationLayout() {
        ViewAnimator aViewAnimator = getViewAnimator();
        LinearLayout aValidationLayout = (LinearLayout) getView().findViewById(R.id.layout_pin_validation);

        aViewAnimator.setDisplayedChild(aViewAnimator.indexOfChild(aValidationLayout));
    }

    private void setUpPresentation() {
        Intent aIntent = Intents.buildSlideShowIntent(getActivity());
        startActivity(aIntent);

        getActivity().finish();
    }

    private void setUpErrorMessage() {
        TextView aSecondaryMessageTextView = (TextView) getView().findViewById(R.id.text_secondary_error_message);
        aSecondaryMessageTextView.setText(buildSecondaryErrorMessage());

        showErrorMessageLayout();
    }

    private String buildSecondaryErrorMessage() {
        switch (mComputer.getProtocol()) {
            case BLUETOOTH:
                return getString(R.string.message_impress_pairing_check);

            case TCP:
                return getString(R.string.message_impress_wifi_enabling);

            default:
                return "";
        }
    }

    private void showErrorMessageLayout() {
        ViewAnimator aViewAnimator = getViewAnimator();
        LinearLayout aMessageLayout = (LinearLayout) getView().findViewById(R.id.layout_error_message);

        aViewAnimator.setDisplayedChild(aViewAnimator.indexOfChild(aMessageLayout));
    }

    private void refreshActionBarMenu() {
        getSherlockActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu aMenu, MenuInflater aMenuInflater) {
        if (!shouldActionBarMenuBeDisplayed()) {
            aMenu.clear();
            return;
        }

        aMenuInflater.inflate(R.menu.menu_action_bar_computer_connection, aMenu);
    }

    private boolean shouldActionBarMenuBeDisplayed() {
        if (getView() == null) {
            return false;
        }

        return getViewAnimator().getCurrentView().getId() == R.id.layout_error_message;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        switch (aMenuItem.getItemId()) {
            case R.id.menu_reconnect:
                showProgressBar();
                connectToComputer();
                refreshActionBarMenu();
                return true;

            default:
                return super.onOptionsItemSelected(aMenuItem);
        }
    }

    private void showProgressBar() {
        ViewAnimator aViewAnimator = getViewAnimator();
        ProgressBar aProgressBar = (ProgressBar) getView().findViewById(R.id.progress_bar);

        aViewAnimator.setDisplayedChild(aViewAnimator.indexOfChild(aProgressBar));
    }

    @Override
    public void onStop() {
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
    public void onSaveInstanceState(Bundle aOutState) {
        super.onSaveInstanceState(aOutState);

        saveLayout(aOutState);
    }

    private void saveLayout(Bundle aOutState) {
        int aLayoutIndex = getViewAnimator().getDisplayedChild();

        aOutState.putInt("LAYOUT", aLayoutIndex);
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
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
