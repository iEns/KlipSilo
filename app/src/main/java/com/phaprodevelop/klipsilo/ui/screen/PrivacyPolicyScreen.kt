package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.ui.components.FitBackBar

@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val body =
        remember {
            val resId =
                if (LocaleHelper.getLanguage(context) == "da") {
                    R.raw.privacy_da
                } else {
                    R.raw.privacy_en
                }
            context.resources.openRawResource(resId).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // The growing back-only bar (#62): the fixed 64dp Material
            // bar broke a long single-word title mid-word or clipped its
            // second line at font scale 2.0.
            FitBackBar(
                title = context.getString(R.string.privacy_policy),
                onBack = onNavigateBack,
                backContentDescription = context.getString(R.string.back),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
