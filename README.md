# Gimbal Airship Adapter

The Gimbal Airship Adapter is a drop-in class that allows users to integrate Gimbal Place events
with the Airship SDK for Android.

## Resources
- [Gimbal Developer Guide](https://gimbal.com/doc/android/v4/devguide.html)
- [Gimbal Manager Portal](https://manager.gimbal.com)
- [Airship Getting Started guide](https://docs.airship.com/platform/android/getting-started/)
- [Airship and Gimbal Integration guide](https://docs.airship.com/partners/gimbal/)

## Installation

To install it add the following dependency to your application's build.gradle file:

```groovy
   implementation 'com.gimbal.android:airship-adapter:2.0.1'
```

## Start the adapter

To start the adapter call:

```java
   AirshipAdapter.shared(context).start("## PLACE YOUR GIMBAL API KEY HERE ##");
```

Once the adapter is started, it will automatically resume its last state when the app is restarted,
including if started in the background. The API key and the started status are persisted between
app starts -- you only need to call `start`  once.

Typically this will be called when the user has opted-in to a feature that benefits from
location-triggered Airship notifications, after the appropriate permissions are granted by the user.
It is also possible to call `start` every time in your `Application.onCreate` but note that the
first time that permissions are granted, Gimbal will not request location updates until the next
app restart.

### Restoring the adapter

By default, the adapter will restore itself upon app start via the `AirshipAdapterInitializer`
App Start Initializer.

To disable this behavior, the initializer may be prevented from merging into your app manifest:

```xml
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <meta-data android:name="com.gimbal.airship.AirshipAdapterInitializer"
            tools:node="remove" />
    </provider>
```

When automatic initialization is disabled, the app must then invoke
`AirshipAdapter.shared(context).restore()` in `Application.onCreate()` (or previous to it in a
custom `Initializer`.  This makes it so that the Gimbal SDK can process Gimbal Place Events reliably
when the app is restarted from terminated state.

## Android Marshmallow+ Permissions

This Adapter does not make requests on behalf of the app, as location permission flow has gotten
far too complex -- it can't presume to know how or when any particular app should make its requests.
If granted, Gimbal will use fine, coarse and background location permissions, as well as Bluetooth
scan permission, to be as location-aware as it can. 

Before the adapter is able to request location updates on Android API 23 or newer, the app must
request the location permission `ACCESS_FINE_LOCATION` (and `ACCESS_COARSE_LOCATION` on Android API 31+).
Technically the Gimbal SDK will still operate when granted only `ACCESS_COARSE_LOCATION` but only
very large, region-sized geofences will trigger geofence place entries.

Please refer to [Request location access at runtime](https://developer.android.com/training/location/permissions#request-location-access-runtime).
Once the permissions are granted, then call this adapter's `start()` method.  It is possible to
start the adapter prior to acceptance of permissions, but then Gimbal may not be able to request
location updates to trigger Airship events until the next app start.

Note: The app will need `ACCESS_BACKGROUND_LOCATION` permissions Gimbal's to process place events
while the app is in the background, if this functionality is desired.  Please refer to
[Request background location if necessary](https://developer.android.com/training/location/permissions#request-background-location).

### Sample app permissions

The sample app in this repository makes a best effort to request all permissions required for full
Gimbal SDK functionality, according to the guidelines provided by Android's Location Permission
training docs linked above.  This includes background location, Bluetooth scanning (for beacon
Place Events), and notification permissions.  It first posts requests without providing a rationale
to the user.  If a request is denied, the app will make a second attempt after providing a
rationale.  Background location permissions always require a rationale to be provided.

This flow is atypical from a real customer-facing app in that `ACCESS_BACKGROUND_LOCATION` is
requested on the first run of the app, after foreground location  permission is granted by the user.
Typically background permission should be requested when a specific app feature is enabled by the
user -- where it would be beneficial to enable Airship notifications as triggered by Gimbal Place
Events while the app is backgrounded.

Feel free to use any of this code as appropriate to help you integrate permissions into your app.

## Enabling Event Tracking
By default, event tracking is disabled, and thus must be explicitly enabled as described below.
New apps should use CustomEvents rather than RegionEvents.  These preferences are persisted
across app starts.

### CustomEvents
To enable or disable the creation and tracking of Airship `CustomEvent`s, use the
`shouldTrackCustomEntryEvents` and `shouldTrackCustomExitEvents` preferences to track events upon
place entry and departure, respectively.  Place entry events are named `gimbal_custom_entry_event`
and departure events are named `gimbal_custom_exit_event`.

Each `CustomEvent` is populated with the following properties:

- `visitID` - a UUID for the Gimbal Visit. This is common for the visit's entry and departure.
- `placeIdentifier` - a UUID for the Gimbal Place
- `placeName` - the human readable place name as entered in Gimbal Manager. Not necessarily unique!
- `source` - always Gimbal
- `boundaryEvent` - an enumeration of `1` for entry and `2` for exit/departure

If there are any Place Attributes key-value pairs (as set on the triggering place in Gimbal Manager)
present on the place that triggered the event, they will also be added to the `CustomEvent`
properties.  They are prefixed with `GMBL_PA_`, e.g. `DMA:825` becomes `GMBL_PA_DMA:825`.

For more information regarding Airship Custom Events, see the Airship
[Custom Event](https://docs.airship.com/guides/messaging/user-guide/data/custom-events/index.html)
documentation.

```java
    // To enable CustomEvent tracking for place exits
    AirshipAdapter.shared(context).setShouldTrackCustomExitEvent(true);

    // To disable CustomEvent tracking for place exits
    AirshipAdapter.shared(context).setShouldTrackCustomExitEvent(false);
    
    // To enable CustomEvent tracking for place entries
    AirshipAdapter.shared(context).setShouldTrackCustomEntryEvent(true);
    
    // To disable CustomEvent tracking for place entries
    AirshipAdapter.shared(context).setShouldTrackCustomEntryEvent(false);
```

### RegionEvents
To enable or disable the tracking of Airship `RegionEvent` objects, use the `shouldTrackRegionEvents`
preference, similar to the above `CustomEvents`.  When enabled, `RegionEvents` are created and
tracked for both Place entries AND departures.

```kotlin
    AirshipAdapter.shared.shouldTrackRegionEvents = true // enabled
    AirshipAdapter.shared.shouldTrackRegionEvents = false // disabled
```

## Stopping the adapter

Adapter can be stopped at anytime by calling:

```java
   AirshipAdapter.shared(context).stop();
```

Once `stop()` is called, Gimbal location event processing will not restart upon subsequent app
starts, until `start()` is called again.

## Running the Sample App

- Create an Android app in Gimbal Manager.  Out of the box, the package/application ID is
  `com.gimbal.airship.android` but this may be modified in `sample-app/build.gradle`.
  Copy the new API key and paste it into the value of `GimbalIntegration.GIMBAL_API_KEY`.
- Set up your Firebase project with a new app [reference](https://firebase.google.com/docs/android/setup).
  Download the `google-services.json` to the `sample-app` directory.  The rest of the dependencies
  and plugins should already be taken care of.
- Set up your Airship app as you would normally.  Paste its development key and secret into
  `src/main/assets/airshipconfig.properties`.  This Sample App does not use a custom Autopilot but
  it is definitely possible to add your own.  Add your Firebase app's server key to the Android
  configuration and enter the package ID.
- Set up an Airship Automation with the `gimbal_custom_entry_event` Custom Event trigger.

## AirshipGimbalAdapter Migration

* update gradle dependency to `com.gimbal.android:airship-adapter:2.0.0` -- note the group change
* update all references to the `AirshipGimbalAdapter` class to `AirshipAdapter`
* add any Airship dependencies to your app `build.gradle` (if not already present)
* replace any calls to `startWithPermissionPrompt()` with `start()`
  * implement permissions requests in your app -- refer to this repository's Sample app for example code
* remove any calls to `restore()` from your app's initialization, unless manual initialization is desired (see above)
