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
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
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
            aSlideViewHolder.mSlideIndex.setBackgroundColor(ContextCompat.getColor(
                    aViewGroup.getContext(), R.color.background_grid_slide_index_active));
            aSlideViewHolder.mSlidePreview.setBackgroundResource(R.drawable.bg_grid_slide_active);
        } else {
            // However we need to 'reset' the view to be non-selected otherwise: i.e. if we change
            // slides (on the server), then the grid-view will be invalidated, but the existing
            // views are reused -- meaning the previously (but no-longer) selected slide will
            // still have a view with highlighted borders -- hence we need to de-highlight it.
            // TODO: this whole class should have better state 'caching', and keep track of previews,
            // to avoid needless updating here unless something has actually changed.
            aSlideViewHolder.mSlideIndex.setBackgroundColor(ContextCompat.getColor(
                    aViewGroup.getContext(), R.color.background_grid_slide_index_inactive));
            aSlideViewHolder.mSlidePreview.setBackgroundResource(R.drawable.bg_grid_slide_inactive);
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
        ImageViewCompat.setImageTintList(aSlideViewHolder.mSlidePreview, null);
    }

    private void setUpUnknownSlidePreview(ViewHolder aSlideViewHolder) {
        aSlideViewHolder.mSlidePreview.setImageResource(R.drawable.bg_slide_unknown);
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
