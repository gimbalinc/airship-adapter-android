/*
 * Copyright 2018 Urban Airship and Contributors
 */

package com.gimbal.airship;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gimbal.android.Attributes;
import com.gimbal.android.DeviceAttributesManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import com.urbanairship.UAirship;
import com.urbanairship.analytics.CustomEvent;
import com.urbanairship.analytics.location.RegionEvent;
import com.urbanairship.channel.AirshipChannelListener;
import com.urbanairship.util.DateUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GimbalAdapter interfaces Gimbal SDK functionality with Urban Airship services.
 */
@SuppressWarnings({"unused"})
public class AirshipAdapter {
    private static final String PREFERENCE_NAME = "com.urbanairship.gimbal.preferences";
    private static final String API_KEY_PREFERENCE = "com.urbanairship.gimbal.api_key";
    private static final String TRACK_CUSTOM_ENTRY_PREFERENCE_KEY = "com.gimbal.track_custom_entry";
    private static final String TRACK_CUSTOM_EXIT_PREFERENCE_KEY = "com.gimbal.track_custom_exit";
    private static final String TRACK_REGION_EVENT_PREFERENCE_KEY = "com.gimbal.track_region_event";
    private static final String STARTED_PREFERENCE = "com.urbanairship.gimbal.is_started";

    private static final String TAG = "GimbalAdapter";
    private static final String SOURCE = "Gimbal";

    // UA to Gimbal Device Attributes
    private static final String GIMBAL_UA_NAMED_USER_ID = "ua.nameduser.id";
    private static final String GIMBAL_UA_CHANNEL_ID = "ua.channel.id";

    // Gimbal to UA Device Attributes
    private static final String UA_GIMBAL_APPLICATION_INSTANCE_ID = "com.urbanairship.gimbal.aii";

    // CustomEvent names
    private static final String CUSTOM_ENTRY_EVENT_NAME = "gimbal_custom_entry_event";
    private static final String CUSTOM_EXIT_EVENT_NAME = "gimbal_custom_exit_event";

    private final SharedPreferences preferences;
    private static AirshipAdapter instance;
    private final Context context;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private boolean isAdapterStarted = false;
    private final LinkedList<CachedVisit> cachedVisits = new LinkedList<>();
    
    /**
     * Adapter listener.
     */
    public interface Listener {

        /**
         * Called when a Urban Airship Region enter event is created from a Gimbal Visit.
         *
         * @param event The Urban Airship event.
         * @param visit The Gimbal visit.
         */
        void onRegionEntered(@NonNull RegionEvent event, @NonNull Visit visit);

        /**
         * Called when a Urban Airship Region exit event is created from a Gimbal Visit.
         *
         * @param event The Urban Airship event.
         * @param visit The Gimbal visit.
         */
        void onRegionExited(@NonNull RegionEvent event, @NonNull Visit visit);

        /**
         * Called when a Urban Airship CustomEvent entry is created from a Gimbal Visit.
         *
         * @param event The Urban Airship event.
         * @param visit The Gimbal visit.
         */
        void onCustomRegionEntry(@NonNull CustomEvent event, @NonNull Visit visit);

        /**
         * Called when a Urban Airship CustomEvent exit is created from a Gimbal Visit.
         *
         * @param event The Urban Airship event.
         * @param visit The Gimbal visit.
         */
        void onCustomRegionExit(@NonNull CustomEvent event, @NonNull Visit visit);
    }

    /**
     * Listener for Gimbal place events. Creates an analytics event
     * corresponding to boundary event type, and Event type preference.
     */
    private final PlaceEventListener placeEventListener = new PlaceEventListener() {
        @Override
        public void onVisitStart(@NonNull final Visit visit) {
            Log.i(TAG, "Entered place: " + visit.getPlace().getName() + "Entrance date: " +
                    DateUtils.createIso8601TimeStamp(visit.getArrivalTimeInMillis()));

            synchronized (cachedVisits) {
                if (isAirshipReady()) {
                    createAirshipEvent(visit, RegionEvent.BOUNDARY_EVENT_ENTER);
                } else {
                    cachedVisits.add(new CachedVisit(visit, RegionEvent.BOUNDARY_EVENT_ENTER));
                }
            }
        }

        @Override
        public void onVisitEnd(@NonNull final Visit visit) {
            Log.i(TAG, "Exited place: " + visit.getPlace().getName() + "Entrance date: " +
                    DateUtils.createIso8601TimeStamp(visit.getArrivalTimeInMillis()) + "Exit date:" +
                    DateUtils.createIso8601TimeStamp(visit.getDepartureTimeInMillis()));

            synchronized (cachedVisits) {
                if (isAirshipReady()) {
                    createAirshipEvent(visit, RegionEvent.BOUNDARY_EVENT_EXIT);
                } else {
                    cachedVisits.add(new CachedVisit(visit, RegionEvent.BOUNDARY_EVENT_EXIT));
                }
            }
        }
    };

    /**
     * Hidden to support the singleton pattern.
     *
     * @param context The application context.
     */
    AirshipAdapter(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * GimbalAdapter shared instance.
     */
    public synchronized static AirshipAdapter shared(@NonNull Context context) {
        if (instance == null) {
            instance = new AirshipAdapter(context.getApplicationContext());
        }

        return instance;
    }

    void processCachedVisits() {
        synchronized (cachedVisits) {
            for (CachedVisit cachedVisit : cachedVisits) {
                createAirshipEvent(cachedVisit.visit, cachedVisit.regionEvent);
            }
            cachedVisits.clear();
        }
    }

    private boolean isAirshipReady() {
        return UAirship.isFlying() || UAirship.isTakingOff();
    }

    private void createAirshipEvent(Visit visit, int regionEvent) {
        if (!isAirshipReady()) {
            Log.w(TAG, "Airship is not ready");
            return;
        }
        UAirship airship = UAirship.shared();

        if (regionEvent == RegionEvent.BOUNDARY_EVENT_ENTER) {
            if (preferences.getBoolean(TRACK_REGION_EVENT_PREFERENCE_KEY, false)) {
                RegionEvent event = createRegionEvent(visit, RegionEvent.BOUNDARY_EVENT_ENTER);

                airship.getAnalytics().addEvent(event);

                for (Listener listener : listeners) {
                    listener.onRegionEntered(event, visit);
                }
            }

            if (preferences.getBoolean(TRACK_CUSTOM_ENTRY_PREFERENCE_KEY, false)) {
                CustomEvent event = createCustomEvent(CUSTOM_ENTRY_EVENT_NAME, visit, RegionEvent.BOUNDARY_EVENT_ENTER);

                airship.getAnalytics().addEvent(event);

                for (Listener listener : listeners) {
                    listener.onCustomRegionEntry(event, visit);
                }
            }
        }

        if (regionEvent == RegionEvent.BOUNDARY_EVENT_EXIT) {
            if (preferences.getBoolean(TRACK_REGION_EVENT_PREFERENCE_KEY, false)) {
                RegionEvent event = createRegionEvent(visit, RegionEvent.BOUNDARY_EVENT_EXIT);

                airship.getAnalytics().addEvent(event);

                for (Listener listener : listeners) {
                    listener.onRegionExited(event, visit);
                }
            }

            if (preferences.getBoolean(TRACK_CUSTOM_EXIT_PREFERENCE_KEY, false)) {
                CustomEvent event = createCustomEvent(CUSTOM_EXIT_EVENT_NAME, visit, RegionEvent.BOUNDARY_EVENT_EXIT);

                airship.getAnalytics().addEvent(event);

                for (Listener listener : listeners) {
                    listener.onCustomRegionExit(event, visit);
                }
            }
        }
    }

    private RegionEvent createRegionEvent(Visit visit, int boundaryEvent) {
        return RegionEvent.newBuilder()
                .setBoundaryEvent(boundaryEvent)
                .setSource(SOURCE)
                .setRegionId(visit.getPlace().getIdentifier())
                .build();
    }

    private CustomEvent createCustomEvent(final String eventName, final Visit visit, final int boundaryEvent) {
        if (boundaryEvent == RegionEvent.BOUNDARY_EVENT_ENTER) {
            return createCustomEventBuilder(eventName, visit, boundaryEvent).build();
        } else {
            return createCustomEventBuilder(eventName, visit, boundaryEvent)
                    .addProperty("dwellTimeInSeconds", visit.getDwellTimeInMillis() / 1000)
                    .build();
        }
    }

    private CustomEvent.Builder createCustomEventBuilder(String eventName, Visit visit, final int boundaryEvent) {
        Attributes placeAttributes = visit.getPlace().getAttributes();

        CustomEvent.Builder builder = CustomEvent.newBuilder(eventName);
        if (placeAttributes != null) {
            for (String key : placeAttributes.getAllKeys()) {
                builder.addProperty("GMBL_PA_" + key, placeAttributes.getValue(key));
            }
        }

        return builder
                .addProperty("visitID", visit.getVisitID())
                .addProperty("placeIdentifier", visit.getPlace().getIdentifier())
                .addProperty("placeName", visit.getPlace().getName())
                .addProperty("source", SOURCE)
                .addProperty("boundaryEvent", boundaryEvent);
    }

    /**
     * Adds an adapter listener.
     *
     * @param listener The listener.
     */
    public void addListener(@NonNull Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an adapter listener.
     *
     * @param listener The listener.
     */
    public void removeListener(@NonNull Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Restores the last run state. If previously started it will start listening.
     * This should be called early during app initialization in order to
     * reliably process background location or beacon events. Called automatically by
     * <code>AirshipAdapterInitializer</code> but may be called in `Application.onCreate()` if
     * manual initialization is desired.
     */
    public void restore() {
        String gimbalApiKey = preferences.getString(API_KEY_PREFERENCE, null);
        boolean previouslyStarted = preferences.getBoolean(STARTED_PREFERENCE, false);
        if (gimbalApiKey != null && previouslyStarted) {
            Log.i(TAG, "Restoring Gimbal Adapter");
            startAdapter(gimbalApiKey);
            if (isStarted()) {
                Log.i(TAG, "Gimbal adapter restored");
            } else {
                Log.e(TAG, "Failed to restore Gimbal adapter");
            }
        }
    }

    /**
     * Starts the adapter.
     * <p>
     * b>Note:</b> The adapter will fail to listen for places if the application does not have proper
     * permissions.
     *
     * @param gimbalApiKey The Gimbal API key.
     * @return {@code true} if the adapter started, otherwise {@code false}.
     */
    public boolean start(@NonNull String gimbalApiKey) {
        startAdapter(gimbalApiKey);
        return isStarted();
    }

    private void startAdapter(@NonNull String gimbalApiKey) {
        if (isAdapterStarted) {
            return;
        }

        preferences.edit()
                .putString(API_KEY_PREFERENCE, gimbalApiKey)
                .putBoolean(STARTED_PREFERENCE, true)
                .apply();

        try {
            Gimbal.setApiKey((Application)context.getApplicationContext(), gimbalApiKey);
            Gimbal.start();
            PlaceManager.getInstance().addListener(placeEventListener);

            Log.i(TAG, String.format("Gimbal Adapter started. Gimbal.isStarted: %b, Gimbal application instance identifier: %s",
                    Gimbal.isStarted(), Gimbal.getApplicationInstanceIdentifier()));
            isAdapterStarted = true;

            UAirship.shared(this::onAirshipReady);
        } catch (Exception e) {
            isAdapterStarted = false;
            Log.e(TAG,"Failed to start Gimbal.", e);
        }
    }

    /**
     * Stops the adapter.
     */
    public void stop() {
        if (!isStarted()) {
            Log.w(TAG, "stop() called when adapter was not started");
            return;
        }

        preferences.edit()
                .putBoolean(STARTED_PREFERENCE, false)
                .apply();

        try {
            Gimbal.stop();
            PlaceManager.getInstance().removeListener(placeEventListener);
            isAdapterStarted = false;
            Log.i(TAG, "Adapter Stopped");
        } catch (Exception e) {
            Log.w(TAG,"Caught exception stopping Gimbal. ", e);
        }
    }

    /**
     * Check if the adapter is started or not.
     */
    public boolean isStarted() {
        try {
            return isAdapterStarted && Gimbal.isStarted();
        } catch (Exception e) {
            Log.w(TAG,"Unable to check Gimbal.isStarted().", e);
            return false;
        }
    }

    private void onAirshipReady(@NonNull UAirship airship) {
        if (!isStarted() || !(UAirship.isFlying() || UAirship.isTakingOff())) {
            Log.w(TAG, "onAirshipReady called when adapter or Airship is not actually ready");
            return;
        }

        updateDeviceAttributes(airship);

        airship.getChannel().addChannelListener(new AirshipChannelListener() {
            @Override
            public void onChannelCreated(@NonNull String channelId) {
                updateDeviceAttributes(airship);
            }

            @Override
            public void onChannelUpdated(@NonNull String channelId) {
                updateDeviceAttributes(airship);
            }
        });

        processCachedVisits();
    }

    /**
     * Set whether the adapter should create a CustomEvent upon Gimbal Place entry.
     * */
    public void setShouldTrackCustomEntryEvent(Boolean shouldTrackCustomEntryEvent) {
        preferences.edit()
                .putBoolean(TRACK_CUSTOM_ENTRY_PREFERENCE_KEY, shouldTrackCustomEntryEvent)
                .apply();
    }

    /**
     * Set whether the adapter should create a CustomEvent upon Gimbal Place exit.
     * */
    public void setShouldTrackCustomExitEvent(Boolean shouldTrackCustomExitEvent) {
        preferences.edit()
                .putBoolean(TRACK_CUSTOM_EXIT_PREFERENCE_KEY, shouldTrackCustomExitEvent)
                .apply();
    }

    /**
     * Set whether the adapter should create a CustomEvent upon Gimbal Place exit.
     * */
    public void setShouldTrackRegionEvent(Boolean shouldTrackRegionEvent) {
        preferences.edit()
                .putBoolean(TRACK_REGION_EVENT_PREFERENCE_KEY, shouldTrackRegionEvent)
                .apply();
    }

    /**
     * Updates Gimbal and Urban Airship device attributes.
     */
    private void updateDeviceAttributes(@NonNull UAirship airship) {
        DeviceAttributesManager deviceAttributesManager = DeviceAttributesManager.getInstance();
        if (deviceAttributesManager == null) {
            return;
        }

        String namedUserId = airship.getContact().getNamedUserId();
        deviceAttributesManager.setDeviceAttribute(GIMBAL_UA_NAMED_USER_ID, namedUserId);

        String channelId = airship.getChannel().getId();
        deviceAttributesManager.setDeviceAttribute(GIMBAL_UA_CHANNEL_ID, channelId);

        String gimbalInstanceId = Gimbal.getApplicationInstanceIdentifier();
        if (gimbalInstanceId != null) {
            airship.getAnalytics().editAssociatedIdentifiers()
                    .addIdentifier(UA_GIMBAL_APPLICATION_INSTANCE_ID, gimbalInstanceId).apply();
        }
    }

    private static class CachedVisit {
        Visit visit;
        int regionEvent;

        CachedVisit(Visit visit, int event) {
            this.visit = visit;
            this.regionEvent = event;
        }
    }
}
