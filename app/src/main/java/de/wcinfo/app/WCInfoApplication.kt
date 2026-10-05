package de.wcinfo.app

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places

class WCInfoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            val apiKey = BuildConfig.GOOGLE_API_KEY
            if (apiKey.isNotBlank() && !Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
                Log.d("WCInfoApplication", "Google Places initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("WCInfoApplication", "Failed to initialize Google Places: ${e.message}", e)
        }
    }
}
