package com.boltbattle.app

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = getString(R.string.launchUrl)
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(false)
            .build()
        intent.launchUrl(this, Uri.parse(url))
        finish()
    }
}
