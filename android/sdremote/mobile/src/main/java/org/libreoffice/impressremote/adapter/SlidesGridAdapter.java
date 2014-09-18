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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.SlideShow;
import org.libreoffice.impressremote.util.ImageLoader;

public class SlidesGridAdapter extends BaseAdapter {
    private final LayoutInflater mLayoutInflater;
    private final ImageLoader mImageLoader;

    private final SlideShow mSlideShow;

    public SlidesGridAdapter(Context aContext, SlideShow aSlideShow) {
        mLayoutInflater = LayoutInflater.from(aContext);
        mImageLoader = new ImageLoader(aContext.getResources(), R.drawable.bg_slide_unknown);

        mSlideShow = aSlideShow;
    }

    @Override
    public int getCount() {
        return mSlideShow.getSlidesCount();
    }

    @Override
    public Object getItem(int aPosition) {
        return mSlideShow.getSlidePreviewBytes(aPosition);
    }

    @Override
    public long getItemId(int aPosition) {
        return aPosition;
    }

    @Override
    public View getView(int aPosition, View aConvertView, ViewGroup aViewGroup) {
        View aSlideView = getView(aConvertView, aViewGroup);
        ViewHolder aSlideViewHolder = getViewHolder(aSlideView);

        if (isSlidePreviewAvailable(aPosition)) {
            setUpSlidePreview(aSlideViewHolder, aPosition);
        } else {
            setUpUnknownSlidePreview(aSlideViewHolder);
        }

        // Highlight the currently selected slide for obviousness.
        if (aPosition == mSlideShow.getCurrentSlideIndex()) {
            aSlideViewHolder.mSlideIndex.setBackgroundColor(
                    aViewGroup.getResources().getColor(R.color.background_grid_slide_index_active));
            aSlideViewHolder.mSlidePreview.setBackgroundResource(R.drawable.bg_grid_slide_active);
        }

        aSlideViewHolder.mSlideIndex.setText(Integer.toString(aPosition + 1));

        return aSlideView;
    }

    private View getView(View aConvertView, ViewGroup aViewGroup) {
        if (aConvertView != null) {
            return aConvertView;
        }

        return mLayoutInflater.inflate(R.layout.view_grid_slide, aViewGroup, false);
    }

    private ViewHolder getViewHolder(View aView) {
        if (aView.getTag() == null) {
            aView.setTag(buildViewHolder(aView));
        }

        return (ViewHolder) aView.getTag();
    }

    private static final class ViewHolder {
        public ImageView mSlidePreview;
        public TextView mSlideIndex;
    }

    private ViewHolder buildViewHolder(View aView) {
        ViewHolder aViewHolder = new ViewHolder();

        aViewHolder.mSlidePreview = (ImageView) aView.findViewById(R.id.image_slide_preview);
        aViewHolder.mSlideIndex = (TextView) aView.findViewById(R.id.text_slide_index);

        return aViewHolder;
    }

    private boolean isSlidePreviewAvailable(int aSlideIndex) {
        return mSlideShow.getSlidePreviewBytes(aSlideIndex) != null;
    }

    private void setUpSlidePreview(ViewHolder aSlideViewHolder, int aPosition) {
        byte[] aSlidePreviewBytes = mSlideShow.getSlidePreviewBytes(aPosition);

        mImageLoader.loadImage(aSlideViewHolder.mSlidePreview, aSlidePreviewBytes);
    }

    private void setUpUnknownSlidePreview(ViewHolder aSlideViewHolder) {
        aSlideViewHolder.mSlidePreview.setImageResource(R.drawable.bg_slide_unknown);
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
