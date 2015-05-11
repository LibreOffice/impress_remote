/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.communication;

public class Commands {

    public static final String INTENT_COUNT="COUNT";
    public static final String INTENT_PREVIEW="PREVIEW";
    public static final String INTENT_HAS_ASSET="HASASSET";

//    public static final String INTENT_FILTER_ALL_INFO="CPFILTER";
    public static final String INTENT_UPDATE="UPDATE";

    public static final String COMMAND_PRESENTATION_STOPPED="/wearableStop";
    public static final String COMMAND_PRESENTATION_PAUSED="/pause";
    public static final String COMMAND_PRESENTATION_RESUMED="/resume";
    public static final String COMMAND_NOTIFY="/notification";

    public static final String COMMAND_NEXT="/next";
    public static final String COMMAND_PREVIOUS="/previous";
    public static final String COMMAND_PAUSERESUME="/pauseResume";
    public static final String COMMAND_CONNECT="/connect";
//    public static final String COMMAND_APP_PAUSED="/appPaused";

    public static final String NULL_STRING_COUNT="0/0";

}
