package org.nano.updater

import android.app.Application
import org.nano.updater.di.component.AppComponent
import org.nano.updater.di.component.DaggerAppComponent

class NanoApplication : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
    }
}