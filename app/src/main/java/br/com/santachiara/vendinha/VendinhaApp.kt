package br.com.santachiara.vendinha

import android.app.Application

class VendinhaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
