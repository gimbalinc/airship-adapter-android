/*
 * Copyright 2018 Urban Airship and Contributors
 */

package com.gimbal.airship;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gimbal.android.Attributes;
import com.gimbal.android.DeviceAttributesManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import com.urbanairship.Airship;
import com.urbanairship.analytics.CustomEvent;
import com.urbanairship.analytics.location.RegionEvent;
import com.urbanairship.channel.AirshipChannelListener;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final boolean TRACK_CUSTOM_ENTRY_DEFAULT = false;
    private static final boolean TRACK_CUSTOM_EXIT_DEFAULT = false;
    private static final boolean TRACK_REGION_EVENT_DEFAULT = false;

    private static final String TAG = "GimbalAdapter";
    private static final String SOURCE = "Gimbal";
    private static final DateTimeFormatter ISO_INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

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
    private final AtomicReference<AirshipChannelListener> airshipChannelListener = new AtomicReference<>();

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
     * Hidden to support the singleton pattern
     *
     * @param context The application context
     */
    private AirshipAdapter(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * GimbalAdapter shared instance
     */
    public synchronized static AirshipAdapter shared(@NonNull Context context) {
        if (instance == null) {
            instance = new AirshipAdapter(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Restores the last run state from a previous app lifecycle.
     *
     * This should be called early during app initialization in order to reliably process
     * background location or beacon events.  To this end, {@code restore()} is Called
     * automatically by {@code AirshipAdapterInitializer} but may be called in `Application
     * .onCreate()` if manual initialization is desired, after disabling the initializer.
     */
    public void restore() {
        String apiKeyPreference = getApiKeyPreference();
        boolean previouslyStarted = getStartedPreference();

        if (apiKeyPreference != null && previouslyStarted) {
            Log.i(TAG, "Restoring Gimbal-Airship Adapter");
            startAdapter(apiKeyPreference);
            if (isAdapterStarted) {
                Log.i(TAG, "Gimbal Airship adapter restored");
            } else {
                Log.e(TAG, "Failed to restore Gimbal Airship adapter");
            }
        } else if (!previouslyStarted) {
            Log.d(TAG, "Gimbal Airship adapter not previously started, nothing to restore");
        }
    }

    /**
     * Starts the adapter -- starts Gimbal and enables forwarding of events to Airship
     * <p>
     * If the adapter is already started with the same API key, no action is taken.  This only
     * needs to be called one time -- the started state and the API key are persisted between app
     * restarts.  It must be called after Airship has been initialized, i.e. is taking off or
     * flying.
     * <p>
     * <b>Note:</b> If  API key is changed, Gimbal will re-register itself when the app
     * is restarted.  Any analytic events generated before re-registration will belong to the
     * previous Gimbal app.
     * <p>
     * <b>Note:</b> The adapter will start, but fail to listen for places if the application does
     * not have the requisite permission(s).
     *
     * @param gimbalApiKey Gimbal API key String
     * @return {@code true} if the adapter started, otherwise {@code false}.
     */
    public boolean start(@NonNull String gimbalApiKey) {
        if (gimbalApiKey.trim().isEmpty()) {
            Log.w(TAG, "Cannot start Gimbal with empty API key");
            return isAdapterStarted;
        }

        String apiKeyPreference = getApiKeyPreference();
        if (isAdapterStarted && !gimbalApiKey.equals(apiKeyPreference)) {
            Log.w(TAG, String.format("Detected API key change '%s' -> '%s...'",
                    apiKeyPreference == null ? "<null>" : apiKeyPreference.substring(0, 8) + "...",
                    gimbalApiKey.substring(0, 8)));
            setApiKeyPreference(gimbalApiKey);
            Gimbal.setApiKey((Application)context.getApplicationContext(), gimbalApiKey);
            Log.w(TAG, "Gimbal will use new API key upon next app start");
        } else {
            startAdapter(gimbalApiKey);
        }
        return isAdapterStarted;
    }

    /**
     * Stops the adapter -- stops Gimbal from monitoring location for Place Events
     * <p>
     * If the adapter is not already started then this has no effect.
     */
    public void stop() {
        if (!isAdapterStarted) {
            return;
        }

        try {
            setStartedPreference(false);
            Gimbal.stop();
            isAdapterStarted = false;
            PlaceManager.getInstance().removeListener(placeEventListener);
            if (isAirshipReady()) {
                Airship.onReady(airship -> {
                    synchronized (airshipChannelListener) {
                        if (airshipChannelListener.get() != null) {
                            airship.getChannel().removeChannelListener(
                                    airshipChannelListener.getAndSet(null));
                        }
                    }
                });
            } else {
                Log.w(TAG, "Airship not taking off or flying - unable to remove channel listener");
            }
            Log.i(TAG, "Adapter stopped");
        } catch (Exception e) {
            Log.w(TAG,"Caught exception stopping Gimbal", e);
        }
    }

    /**
     * Check if the adapter is started or not.
     */
    public boolean isStarted() {
        return isAdapterStarted;
    }

    /**
     * Sets whether this Adapter should create a CustomEvent upon Gimbal Place entry.
     * Defaults to `false`.  The value set here is persisted across app restarts.
     *
     * @param shouldTrackCustomEntryEvent specifies whether to track place entries as
     *                                    Airship CustomEvents.
     */
    public void setShouldTrackCustomEntryEvent(boolean shouldTrackCustomEntryEvent) {
        preferences.edit()
                .putBoolean(TRACK_CUSTOM_ENTRY_PREFERENCE_KEY, shouldTrackCustomEntryEvent)
                .apply();
    }

    /**
     * Sets whether this Adapter should create a CustomEvent upon Gimbal Place departure.
     * Defaults to `false`.  The value set here is persisted across app restarts.
     *
     * @param shouldTrackCustomExitEvent specifies whether to track place departures as
     *                                    Airship CustomEvents.
     */
    public void setShouldTrackCustomExitEvent(boolean shouldTrackCustomExitEvent) {
        preferences.edit()
                .putBoolean(TRACK_CUSTOM_EXIT_PREFERENCE_KEY, shouldTrackCustomExitEvent)
                .apply();
    }

    /**
     * Sets whether this Adapter should create a RegionEvent upon Gimbal Place entry or departure.
     * Defaults to `false`.  The value set here is persisted across app restarts.
     *
     * @param shouldTrackRegionEvent specifies whether to track place entries AND departures as
     *                               Airship RegionEvents.
     */
    public void setShouldTrackRegionEvent(boolean shouldTrackRegionEvent) {
        preferences.edit()
                .putBoolean(TRACK_REGION_EVENT_PREFERENCE_KEY, shouldTrackRegionEvent)
                .apply();
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

    private void startAdapter(@NonNull String gimbalApiKey) {
        if (isAdapterStarted) {
            Log.w(TAG, "Calling start when adapter is already started has no effect");
            return;
        } else if (!isAirshipReady()) {
            Log.w(TAG, "Unable to start adapter when Airship is not taking off or flying");
            return;
        }

        try {
            setStartedPreference(true);
            setApiKeyPreference(gimbalApiKey);
            Gimbal.setApiKey((Application)context.getApplicationContext(), gimbalApiKey);
            Gimbal.start();
            isAdapterStarted = Gimbal.isStarted();
            PlaceManager.getInstance().addListener(placeEventListener);
            Airship.onReady(this::onAirshipReady);

            Log.i(TAG, String.format("Gimbal Adapter started. Gimbal.isStarted: %b, Gimbal application instance identifier: %s",
                    Gimbal.isStarted(), Gimbal.getApplicationInstanceIdentifier()));
        } catch (Exception e) {
            isAdapterStarted = false;
            Log.e(TAG,"Failed to start Gimbal.", e);
        }
    }

    @Nullable
    private String getApiKeyPreference() {
        return preferences.getString(API_KEY_PREFERENCE, null);
    }

    private void setApiKeyPreference(@NonNull String gimbalApiKey) {
        preferences.edit()
                .putString(API_KEY_PREFERENCE, gimbalApiKey)
                .apply();
    }

    private boolean getStartedPreference() {
        return preferences.getBoolean(STARTED_PREFERENCE, false);
    }

    private void setStartedPreference(boolean started) {
        preferences.edit()
                .putBoolean(STARTED_PREFERENCE, started)
                .apply();
    }

    private synchronized void onAirshipReady(@NonNull Airship airship) {
        if (!isAdapterStarted || !isAirshipReady()) {
            Log.w(TAG, "OnReadyCallback invoked when adapter or Airship is not actually ready");
            return;
        }

        updateDeviceAttributes(airship);

        synchronized (airshipChannelListener) {
            airshipChannelListener.set(new AirshipChannelListener() {
                @Override
                public void onChannelCreated(@NonNull String channelId) {
                    updateDeviceAttributes(airship);
                }
            });
            airship.getChannel().addChannelListener(airshipChannelListener.get());
        }

        processCachedVisits();
    }

    /**
     * Updates Gimbal and Urban Airship device attributes.
     */
    private void updateDeviceAttributes(@NonNull Airship airship) {
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

    private void processCachedVisits() {
        synchronized (cachedVisits) {
            for (CachedVisit cachedVisit : cachedVisits) {
                createAirshipEvent(cachedVisit.visit, cachedVisit.regionEvent);
            }
            cachedVisits.clear();
        }
    }

    /**
     * Listener for Gimbal place events. Creates an analytics event
     * corresponding to boundary event type, and Event type preference.
     */
    private final PlaceEventListener placeEventListener = new PlaceEventListener() {
        @Override
        public void onVisitStart(@NonNull final Visit visit) {
            Log.i(TAG, "Entered place: " + visit.getPlace().getName() + " date: " +
                    ISO_INSTANT_FORMATTER.format(Instant.ofEpochMilli(visit.getArrivalTimeInMillis())));

            synchronized (cachedVisits) {
                if (isAirshipReady()) {
                    createAirshipEvent(visit, RegionEvent.Boundary.ENTER);
                } else {
                    cachedVisits.add(new CachedVisit(visit, RegionEvent.Boundary.ENTER));
                }
            }
        }

        @Override
        public void onVisitEnd(@NonNull final Visit visit) {
            Log.i(TAG, "Exited place: " + visit.getPlace().getName() + " date: " +
                    ISO_INSTANT_FORMATTER.format(Instant.ofEpochMilli(visit.getArrivalTimeInMillis())) + "Exit date:" +
                    ISO_INSTANT_FORMATTER.format(Instant.ofEpochMilli(visit.getDepartureTimeInMillis())));

            synchronized (cachedVisits) {
                if (isAirshipReady()) {
                    createAirshipEvent(visit, RegionEvent.Boundary.EXIT);
                } else {
                    cachedVisits.add(new CachedVisit(visit, RegionEvent.Boundary.EXIT));
                }
            }
        }
    };


    private void createAirshipEvent(Visit visit, RegionEvent.Boundary regionEvent) {
        if (!isAirshipReady()) {
            Log.w(TAG, "Airship is not ready");
            return;
        }
        // Use onReady to get Airship instance and call instance methods
        // This avoids RestrictedApi lint errors by using instance methods within the callback
        Airship.onReady(airship -> {
            synchronized (listeners) {
                if (regionEvent == RegionEvent.Boundary.ENTER) {
                    if (preferences.getBoolean(TRACK_REGION_EVENT_PREFERENCE_KEY, TRACK_REGION_EVENT_DEFAULT)) {
                        RegionEvent event = createRegionEvent(visit, RegionEvent.Boundary.ENTER);

                        airship.getAnalytics().addEvent(event);

                        for (Listener listener : listeners) {
                            listener.onRegionEntered(event, visit);
                        }
                    }

                    if (preferences.getBoolean(TRACK_CUSTOM_ENTRY_PREFERENCE_KEY, TRACK_CUSTOM_ENTRY_DEFAULT)) {
                        CustomEvent event = createCustomEvent(CUSTOM_ENTRY_EVENT_NAME, visit, RegionEvent.Boundary.ENTER);

                        airship.getAnalytics().addEvent(event);

                        for (Listener listener : listeners) {
                            listener.onCustomRegionEntry(event, visit);
                        }
                    }
                }

                if (regionEvent == RegionEvent.Boundary.EXIT) {
                    if (preferences.getBoolean(TRACK_REGION_EVENT_PREFERENCE_KEY, TRACK_REGION_EVENT_DEFAULT)) {
                        RegionEvent event = createRegionEvent(visit, RegionEvent.Boundary.EXIT);

                        airship.getAnalytics().addEvent(event);

                        for (Listener listener : listeners) {
                            listener.onRegionExited(event, visit);
                        }
                    }

                    if (preferences.getBoolean(TRACK_CUSTOM_EXIT_PREFERENCE_KEY, TRACK_CUSTOM_EXIT_DEFAULT)) {
                        CustomEvent event = createCustomEvent(CUSTOM_EXIT_EVENT_NAME, visit, RegionEvent.Boundary.EXIT);

                        airship.getAnalytics().addEvent(event);

                        for (Listener listener : listeners) {
                            listener.onCustomRegionExit(event, visit);
                        }
                    }
                }
            }
        });
    }

    private boolean isAirshipReady() {
        return Airship.isFlying() || Airship.isTakingOff();
    }

    private RegionEvent createRegionEvent(Visit visit, RegionEvent.Boundary boundaryEvent) {
        return RegionEvent.newBuilder(visit.getPlace().getIdentifier(), boundaryEvent)
                .build();
    }

    private CustomEvent createCustomEvent(final String eventName, final Visit visit, final RegionEvent.Boundary boundaryEvent) {
        if (boundaryEvent == RegionEvent.Boundary.ENTER) {
            return createCustomEventBuilder(eventName, visit, boundaryEvent)
                    .build();
        } else {
            return createCustomEventBuilder(eventName, visit, boundaryEvent)
                    .addProperty("dwellTimeInSeconds", visit.getDwellTimeInMillis() / 1000)
                    .build();
        }
    }

    private CustomEvent.Builder createCustomEventBuilder(String eventName, Visit visit, final RegionEvent.Boundary boundaryEvent) {
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
                .addProperty("boundaryEvent", boundaryToInt(boundaryEvent));
    }

    private static int boundaryToInt(RegionEvent.Boundary boundary) {
        return boundary == RegionEvent.Boundary.ENTER ? 0 : 1;
    }

    private static class CachedVisit {
        Visit visit;
        RegionEvent.Boundary regionEvent;

        CachedVisit(Visit visit, RegionEvent.Boundary event) {
            this.visit = visit;
            this.regionEvent = event;
        }
    }
}
