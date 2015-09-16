/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.communication;

// stub version for non-wearable remote (minimal flavor)

public class CommunicationServiceWear  {
    public static void presentationPaused(){}
    public static void presentationResumed(){}
    public static void syncData(@SuppressWarnings("unused") final String count,
                                @SuppressWarnings("unused") final byte[] preview) {}
    public static void ignoreNextSync(){}
}