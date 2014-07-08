package org.libreoffice.impressremote.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

// this class needed to create ViewPager without swiping.
public class PointerViewPager extends ViewPager {

    public PointerViewPager(Context aContext) {
        super(aContext);
    }

    public PointerViewPager(Context aContext, AttributeSet aAttributs) {
        super(aContext, aAttributs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between slides
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between slides
        return false;
    }
}
