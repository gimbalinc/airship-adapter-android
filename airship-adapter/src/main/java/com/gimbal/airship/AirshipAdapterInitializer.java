package com.gimbal.airship;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

public class AirshipAdapterInitializer implements Initializer<AirshipAdapter> {
   @NonNull
   @Override
   public AirshipAdapter create(@NonNull Context context) {
      AirshipAdapter adapter = AirshipAdapter.shared(context);
      adapter.restore();
      return adapter;
   }

   @NonNull
   @Override
   public List<Class<? extends Initializer<?>>> dependencies() {
      return Collections.emptyList();
   }
}
