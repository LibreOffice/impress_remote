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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.fragment.ComputersFragment;

public class ComputersPagerAdapter extends FragmentPagerAdapter {
    private int pager_size = 0;
    private ComputersFragment.Type tabs[] = new ComputersFragment.Type[2];
    private Context context;

    public ComputersPagerAdapter(FragmentManager aFragmentManager, Context c) {
        super(aFragmentManager);
        context = c;
    }

    public void addFragment(ComputersFragment.Type type) {
        tabs[pager_size] = type;
        pager_size++;
    }

    public void removeFragment(ComputersFragment.Type type) {
        switch(type) {
            case WIFI:  tabs[1] = null; break;
            case BLUETOOTH: tabs[0] = ComputersFragment.Type.WIFI; break;
        }
        pager_size--;
        notifyDataSetChanged();
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
        if(ComputersFragment.Type.WIFI == tabs[position]) {
            return context.getString(R.string.title_wifi);
        } else {
            return context.getString(R.string.title_bluetooth);
        }
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */