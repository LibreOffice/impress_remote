/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.fragment.ComputersFragment;

public class ComputersPagerAdapter extends FragmentPagerAdapter {
    private int pager_size = 0;
    private ComputersFragment.Type tabs[] = new ComputersFragment.Type[2];
    private Context context;

    public ComputersPagerAdapter(FragmentManager aFragmentManager, Context context) {
        super(aFragmentManager);
        this.context = context;
    }

    public void addFragment(ComputersFragment.Type type) {
        tabs[pager_size] = type;
        pager_size++;
    }

    @Override
    public Fragment getItem(int aPosition) {
        return ComputersFragment.newInstance(tabs[aPosition]);
    }

    @Override
    public int getCount() {
        return pager_size;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.title_wifi);
            case 1:
                return context.getResources().getString(R.string.title_bluetooth);
        }
        return null;
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
