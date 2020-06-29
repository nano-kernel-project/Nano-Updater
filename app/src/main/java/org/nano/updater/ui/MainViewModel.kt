package org.nano.updater.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainViewModel @Inject constructor() : ViewModel() {
    private val currentDestination = MutableLiveData<Int>()

    init {
        currentDestination.value = 0
    }

    fun getCurrentDestination(): LiveData<Int> = currentDestination

    fun setCurrentDestination(currentFragment: Int) {
        this.currentDestination.value = currentFragment
    }
}