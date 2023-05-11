package com.gimbal.airship.sample

import android.content.Context
import androidx.room.Room
import com.gimbal.airship.AirshipAdapter
import com.gimbal.airship.sample.data.PlaceEventRepositoryImpl
import com.gimbal.airship.sample.domain.GimbalIntegration
import com.gimbal.airship.sample.domain.PlaceEventRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideOkHttpClient() = if (BuildConfig.DEBUG) {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    } else {
        OkHttpClient
            .Builder()
            .build()
    }

    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl("https://example.com") //TODO change this
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "GMBL_airship_sample_database"
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun placeEventsRepository(
        appDatabase: AppDatabase,
    ): PlaceEventRepository {
        return PlaceEventRepositoryImpl(appDatabase.getPlaceEventDao())
    }

    @Provides
    fun provideAirshipAdapter(
        @ApplicationContext context: Context
    ): AirshipAdapter = AirshipAdapter.shared(context)

    @Provides
    @Singleton
    fun provideGimbalIntegration(
        airshipAdapter: AirshipAdapter
    ): GimbalIntegration {
        return GimbalIntegration(airshipAdapter)
    }
}
