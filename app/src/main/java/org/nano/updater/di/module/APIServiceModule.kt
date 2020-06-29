package org.nano.updater.di.module

import dagger.Module
import dagger.Provides
import org.nano.updater.network.BotService
import org.nano.updater.network.UpdateService
import org.nano.updater.util.Constants
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class APIServiceModule {

    @Singleton
    @Provides
    fun provideUpdateService(retrofit: Retrofit): UpdateService {
        return retrofit.create(UpdateService::class.java)
    }

    @Singleton
    @Provides
    fun provideBotService(retrofit: Retrofit): BotService {
        return retrofit.newBuilder().baseUrl(Constants.TG_CHANNEL_API_URL).build().create(BotService::class.java)
    }
}