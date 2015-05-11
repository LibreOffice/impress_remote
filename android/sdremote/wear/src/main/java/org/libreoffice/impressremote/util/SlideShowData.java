/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.util;

import android.graphics.Bitmap;

import org.libreoffice.impressremote.communication.Commands;

public class SlideShowData {
    private static SlideShowData instance=new SlideShowData();
    private String count;
    private Bitmap preview;
    private boolean hasPreview;
    private boolean fullscreen;

    private SlideShowData(){
        count= Commands.NULL_STRING_COUNT;
        hasPreview=false;
        fullscreen=false;
    }
    public static SlideShowData getInstance(){
        if(instance==null){
            instance=new SlideShowData();
        }
        return instance;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public void setPreview(Bitmap preview) {
        this.preview = preview;
    }

    public boolean hasPreview() {
        return hasPreview;
    }

    public void setHasPreview(boolean hasPreview) {
        this.hasPreview = hasPreview;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }
}
