package com.phaprodevelop.klipsilo

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.phaprodevelop.klipsilo.data.settings.ThemeMode
import com.phaprodevelop.klipsilo.ui.navigation.AppRoot
import com.phaprodevelop.klipsilo.ui.theme.KlipSiloTheme
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: ClipboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPhonePortraitLock()
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme =
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
            KlipSiloTheme(darkTheme = darkTheme) {
                AppRoot(viewModel)
            }
        }
        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.isShareSend()) {
            viewModel.saveSharedContent(intent)
        }
    }
}

internal fun Intent.isShareSend(): Boolean = action == Intent.ACTION_SEND
