/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public final class Fragments {
    private Fragments() {
    }

    public static final class Arguments {
        private Arguments() {
        }

        public static final String COMPUTER = "COMPUTER";
        public static final String MINUTES = "MINUTES";
        public static final String TYPE = "TYPE";
    }

    public static final class Operator {
        private Operator() {
        }

        public static void add(FragmentActivity aActivity, Fragment aFragment) {
            if (isAdded(aActivity)) {
                return;
            }

            aActivity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, aFragment)
                .commit();
        }

        private static boolean isAdded(FragmentActivity aActivity) {
            return aActivity.getSupportFragmentManager().findFragmentById(android.R.id.content) != null;
        }

        public static void replaceAnimated(FragmentActivity aActivity, Fragment aFragment) {
            aActivity.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(android.R.id.content, aFragment)
                .commit();
        }
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
