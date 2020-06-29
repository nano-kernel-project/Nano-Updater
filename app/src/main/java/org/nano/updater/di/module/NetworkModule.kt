package org.nano.updater.di.module

import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.nano.updater.util.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton


@Module
class NetworkModule @Inject constructor() {

    @Singleton
    @Provides
    fun provideRetrofitInstance(): Retrofit {
        // Add base url for repositories
        // baseUrl will be changed in repositories as required
        return Retrofit.Builder()
            .baseUrl(Constants.UPDATER_BASE_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient()
    }
}
