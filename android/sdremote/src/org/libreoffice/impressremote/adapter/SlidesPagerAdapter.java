package org.libreoffice.impressremote.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.SlideShow;

public class SlidesPagerAdapter extends PagerAdapter {
    private final LayoutInflater mLayoutInflater;

    private final SlideShow mSlideShow;

    public SlidesPagerAdapter(Context aContext, SlideShow aSlideShow) {
        mLayoutInflater = buildLayoutInflater(aContext);

        mSlideShow = aSlideShow;
    }

    private LayoutInflater buildLayoutInflater(Context aContext) {
        return (LayoutInflater) aContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return mSlideShow.getSlidesCount();
    }

    @Override
    public Object instantiateItem(ViewGroup aViewGroup, int aPosition) {
        ImageView aSlideView = (ImageView) getView(aViewGroup);

        aSlideView.setImageBitmap(mSlideShow.getSlidePreview(aPosition));

        aViewGroup.addView(aSlideView);

        return aSlideView;
    }

    private View getView(ViewGroup aViewGroup) {
        return mLayoutInflater.inflate(R.layout.view_pager_slide, aViewGroup, false);
    }

    @Override
    public void destroyItem(ViewGroup aViewGroup, int aPosition, Object aObject) {
        View aView = (View) aObject;

        aViewGroup.removeView(aView);
    }

    @Override
    public boolean isViewFromObject(View aView, Object aObject) {
        return aView == aObject;
    }

    @Override
    public int getItemPosition(Object aObject) {
        return POSITION_NONE;
    }
}
