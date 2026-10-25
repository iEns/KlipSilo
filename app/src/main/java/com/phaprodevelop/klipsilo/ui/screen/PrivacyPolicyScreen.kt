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

/**
 * Raw privacy policy resource for a [LocaleHelper.getLanguage] tag (#87).
 * Indonesian picker tag is `id` but the raw file is `privacy_in` (Android values-in).
 */
internal fun privacyRawResId(languageTag: String): Int =
    when (languageTag) {
        "da" -> R.raw.privacy_da
        "de" -> R.raw.privacy_de
        "es" -> R.raw.privacy_es
        "fr" -> R.raw.privacy_fr
        "id" -> R.raw.privacy_in
        "it" -> R.raw.privacy_it
        "ja" -> R.raw.privacy_ja
        "ko" -> R.raw.privacy_ko
        "pt-BR" -> R.raw.privacy_pt_br
        "ru" -> R.raw.privacy_ru
        "tr" -> R.raw.privacy_tr
        "zh-Hans" -> R.raw.privacy_zh_hans
        else -> R.raw.privacy_en
    }

@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val body =
        remember {
            val resId = privacyRawResId(LocaleHelper.getLanguage(context))
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
