/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.util.SlideShowData;

public class NotificationActivity extends ControlActivity {
    private static final String TAG = "NotificationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.notification);

        TextView mTextView = (TextView) findViewById(R.id.textView_counter);
        mTextView.setText(SlideShowData.getInstance().getCount());

    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop(){
        Log.v(TAG, "onStop");
        super.onStop();
    }



}
