/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.libreoffice.impressremote.fragment.ComputersFragment;

public class ComputersPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGER_SIZE = 2;

    public static final class PagesIndices {
        private PagesIndices() {
        }

        public static final int BLUETOOTH = 0;
        public static final int WIFI = 1;
    }

    public ComputersPagerAdapter(FragmentManager aFragmentManager) {
        super(aFragmentManager);
    }

    @Override
    public Fragment getItem(int aPosition) {
        switch (aPosition) {
            case PagesIndices.BLUETOOTH:
                return ComputersFragment.newInstance(ComputersFragment.Type.BLUETOOTH);

            case PagesIndices.WIFI:
                return ComputersFragment.newInstance(ComputersFragment.Type.WIFI);

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return PAGER_SIZE;
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
