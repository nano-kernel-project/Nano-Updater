package org.nano.updater.di.component

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import org.nano.updater.di.module.APIServiceModule
import org.nano.updater.di.module.DatabaseModule
import org.nano.updater.di.module.NetworkModule
import org.nano.updater.network.ActionReceiver
import org.nano.updater.network.DownloadTask
import org.nano.updater.repository.ReportRepository
import org.nano.updater.repository.UpdateRepository
import org.nano.updater.service.FlashService
import org.nano.updater.ui.MainActivity
import org.nano.updater.ui.about.AboutFragment
import org.nano.updater.ui.flash.FlashFragment
import org.nano.updater.ui.home.HomeFragment
import org.nano.updater.ui.home.SupportFragment
import org.nano.updater.ui.nav.BottomNavDrawerFragment
import org.nano.updater.ui.report.ReportCollectionFragment
import org.nano.updater.ui.report.ReportFragment
import org.nano.updater.ui.settings.SettingsFragment
import org.nano.updater.ui.update.UpdateFragment
import org.nano.updater.util.FlashUtils
import org.nano.updater.worker.UpdateCheckWorker
import javax.inject.Singleton

@Singleton
@Component(modules = [NetworkModule::class, DatabaseModule::class, APIServiceModule::class])
interface AppComponent {

    // Factory to create instances of the AppComponent
    @Component.Factory
    interface Factory {
        // Pass context to the graph
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: MainActivity)
    fun inject(bottomNavFragment: BottomNavDrawerFragment)
    fun inject(aboutFragment: AboutFragment)
    fun inject(reportFragment: ReportFragment)
    fun inject(homeFragment: HomeFragment)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(reportCollectionFragment: ReportCollectionFragment)
    fun inject(updateFragment: UpdateFragment)
    fun inject(flashFragment: FlashFragment)
    fun inject(flashUtils: FlashUtils)
    fun inject(updateCheckWorker: UpdateCheckWorker)
    fun inject(flashService: FlashService)
    fun inject(receiver: ActionReceiver)
    fun inject(supportFragment: SupportFragment)
    fun inject(updateRepository: UpdateRepository)
    fun inject(reportRepository: ReportRepository)
    fun inject(downloadTask: DownloadTask)
}
