package com.nutrisnap.app.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nutrisnap.app.BuildConfig
import com.nutrisnap.app.data.local.AppDatabase
import com.nutrisnap.app.data.local.FastingDao
import com.nutrisnap.app.data.local.FoodLogDao
import com.nutrisnap.app.data.local.WaterDao
import com.nutrisnap.app.data.remote.BackendApi
import com.nutrisnap.app.data.remote.OpenFoodFactsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton @Named("off")
    fun offRetrofit(client: OkHttpClient, json: Json) =
        retrofit(BuildConfig.OFF_BASE_URL, client, json)

    @Provides @Singleton @Named("backend")
    fun backendRetrofit(client: OkHttpClient, json: Json) =
        retrofit(BuildConfig.BACKEND_BASE_URL, client, json)

    @Provides @Singleton
    fun offApi(@Named("off") r: Retrofit): OpenFoodFactsApi = r.create(OpenFoodFactsApi::class.java)

    @Provides @Singleton
    fun backendApi(@Named("backend") r: Retrofit): BackendApi = r.create(BackendApi::class.java)

    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "nutrisnap.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun foodLogDao(db: AppDatabase): FoodLogDao = db.foodLogDao()
    @Provides fun fastingDao(db: AppDatabase): FastingDao = db.fastingDao()
    @Provides fun waterDao(db: AppDatabase): WaterDao = db.waterDao()
}
