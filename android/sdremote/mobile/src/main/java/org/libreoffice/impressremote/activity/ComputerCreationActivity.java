/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.util.Intents;

public class ComputerCreationActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {
    @Override
    protected void onCreate(Bundle aSavedInstanceState) {
        super.onCreate(aSavedInstanceState);
        // enable use of vector drawables via indirection e.g. through layer drawables
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_computer_creation);
        // action bar setup and listeners
        ActionBar aActionBar = getSupportActionBar();
        assert aActionBar != null;
        aActionBar.setCustomView(R.layout.action_bar_computer_creation);
        aActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        View aActionBarView = aActionBar.getCustomView();
        aActionBarView.findViewById(R.id.button_cancel).setOnClickListener(this);
        aActionBarView.findViewById(R.id.button_save).setOnClickListener(this);
        // input field
        getNameEdit().setOnEditorActionListener(this);
    }

    @Override
    public void onClick(View aView) {
        int id = aView.getId();
        if (id == R.id.button_cancel) {
            cancelCreation();
        } else if (id == R.id.button_save) {
            saveServer();
        }
    }

    private void cancelCreation() {
        finish();
    }

    private void saveServer() {
        String aIpAddress = getText(getIpAddressEdit());
        String aName = getText(getNameEdit());

        if (!isIpAddressValid(aIpAddress)) {
            setUpIpAddressErrorMessage();
            return;
        }

        if (TextUtils.isEmpty(aName)) {
            aName = aIpAddress;
        }

        finish(aIpAddress, aName);
    }

    private String getText(EditText aEdit) {
        return aEdit.getText().toString().trim();
    }

    private EditText getIpAddressEdit() {
        return (EditText) findViewById(R.id.edit_ip_address);
    }

    private EditText getNameEdit() {
        return (EditText) findViewById(R.id.edit_name);
    }

    private boolean isIpAddressValid(String aIpAddress) {
        return Patterns.IP_ADDRESS.matcher(aIpAddress).matches();
    }

    private void setUpIpAddressErrorMessage() {
        TextInputLayout aIpEntry = (TextInputLayout) findViewById(R.id.edit_ip_address_layout);
        assert aIpEntry != null;
        aIpEntry.setError(getString(R.string.message_ip_address_validation));
        aIpEntry.requestFocus();
    }

    private void finish(String aIpAddress, String aName) {
        Intent aIntent = Intents.buildComputerCreationResultIntent(aIpAddress, aName);
        setResult(Activity.RESULT_OK, aIntent);

        finish();
    }

    @Override
    public boolean onEditorAction(TextView aTextView, int aActionId, KeyEvent aKeyEvent) {
        switch (aActionId) {
            case EditorInfo.IME_ACTION_DONE:
                saveServer();
                break;

            default:
                break;
        }

        return false;
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
