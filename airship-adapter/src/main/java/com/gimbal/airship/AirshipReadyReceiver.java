/*
 * Copyright 2018 Urban Airship and Contributors
 */

package com.gimbal.airship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.urbanairship.UAirship;

/**
 * Broadcast receiver for Airship Ready events.
 */
public class AirshipReadyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // We have a crash report from a customer that indicates this is somehow
        // called before Airship is ready. We believe its a stale intent being
        // delivered late. Check for ready to be safe.
        if (UAirship.isFlying() || UAirship.isTakingOff()) {
            AirshipAdapter.shared(context).onAirshipReady();
        }
    }
}