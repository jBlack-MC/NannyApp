package com.nannyapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nannyapp.data.preferences.AppSettings
import com.nannyapp.data.preferences.ThemeMode
import com.nannyapp.ui.navigation.NannyNavGraph
import com.nannyapp.ui.theme.NannyAppTheme
import com.nannyapp.work.scheduleNotificationSync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appSettings: AppSettings

    // The chosen language (Settings → Language) applies to every screen's resources.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        scheduleNotificationSync(applicationContext, appSettings.backgroundSyncEnabled.value)
        setContent {
            val themeMode by appSettings.themeMode.collectAsState()
            NannyAppTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                NannyNavGraph()
            }
        }
    }
}
