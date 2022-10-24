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
   implementation 'com.gimbal.android.v4:airship-adapter:1.0.0'
```

## Start the adapter

To start the adapter call:

```java
   AirshipAdapter.shared(context).start("## PLACE YOUR GIMBAL API KEY HERE ##");
```

Once the adapter is started, it will automatically resume its last state if
the application is restarted in the background. You only need to call start
once.

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

When automatic initialization is disabled, the app should then invoke
`AirshipAdapter.shared(context).restore()` in `Application.onCreate()` so that the Gimbal SDK can
process Gimbal Place Events reliably when the app is restarted from terminated state.

### Android Marshmallow Permissions

Before the adapter is able to be started on Android M, it must request the location permission
`ACCESS_FINE_LOCATION`. The adapter has convenience methods that you can use to request permissions
while  starting the adapter:

```java
    AirshipAdapter.shared(context).startWithPermissionPrompt("## PLACE YOUR GIMBAL API KEY HERE ##");
```

Alternatively you can follow [requesting runtime permissions](https://developer.android.com/training/permissions/requesting.html)
to manually request the proper permissions. Then once the permissions are granted, call start on
the adapter.

Note: You will need `ACCESS_BACKGROUND_LOCATION` permissions to use Gimbal's background features

## Enabling Event Tracking
By default, event tracking is disabled, and thus must be explicitly enabled as described below.

### RegionEvents
To enable or disable the tracking of Airship RegionEvent objects, use the shouldTrackRegionEvents property:

AirshipAdapter.shared.shouldTrackRegionEvents = true // enabled
AirshipAdapter.shared.shouldTrackRegionEvents = false // disabled

### CustomEvents
To enable or disable the tracking of Airship CustomEvent objects, use the shouldTrackCustomEntryEvents and shouldTrackCustomExitEvents properties to track events upon place entry and exit, as shown below. For more information regarding Airship Custom Events, see the documentation here.

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

## Stopping the adapter

Adapter can be stopped at anytime by calling:

```java
   AirshipAdapter.shared(context).stop();
```

Once `stop()` is called, Gimbal location event processing will not restart upon subsequent app
starts.

## AirshipGimbalAdapter Migration

update gradle dependency to `com.gimbal.android.v4:airship-adapter:1.0.0`
update all references to the `AirshipGimbalAdapter` class should be changed to `AirshipAdapter`
