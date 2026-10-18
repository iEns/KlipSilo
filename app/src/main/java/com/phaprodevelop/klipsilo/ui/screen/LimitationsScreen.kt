package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.ui.components.DisplayText
import com.phaprodevelop.klipsilo.ui.components.FitBackBar

private data class Limitation(val title: String, val body: String)

@Composable
fun LimitationsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val limitations =
        listOf(
            Limitation(
                context.getString(R.string.limitations_font_t),
                context.getString(R.string.limitations_font_b),
            ),
            Limitation(
                context.getString(R.string.limitations_break_t),
                context.getString(R.string.limitations_break_b),
            ),
            Limitation(
                context.getString(R.string.limitations_spacing_t),
                context.getString(R.string.limitations_spacing_b),
            ),
            Limitation(
                context.getString(R.string.limitations_tables_t),
                context.getString(R.string.limitations_tables_b),
            ),
            Limitation(
                context.getString(R.string.limitations_headers_t),
                context.getString(R.string.limitations_headers_b),
            ),
            Limitation(
                context.getString(R.string.limitations_floating_t),
                context.getString(R.string.limitations_floating_b),
            ),
            Limitation(
                context.getString(R.string.limitations_lists_t),
                context.getString(R.string.limitations_lists_b),
            ),
            Limitation(
                context.getString(R.string.limitations_duplicates_t),
                context.getString(R.string.limitations_duplicates_b),
            ),
            Limitation(
                context.getString(R.string.limitations_symbols_t),
                context.getString(R.string.limitations_symbols_b),
            ),
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // The growing back-only bar (#62): the fixed 64dp Material
            // bar broke a long single-word title mid-word or clipped its
            // second line at font scale 2.0.
            FitBackBar(
                title = context.getString(R.string.tool_limitations),
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
                    .padding(horizontal = 16.dp),
        ) {
            Text(
                text = context.getString(R.string.limitations_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            limitations.forEach { limitation ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        DisplayText(
                            text = limitation.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.size(4.dp))
                        DisplayText(
                            text = limitation.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            DisplayText(
                text = context.getString(R.string.limitations_summary_t),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            DisplayText(
                text = context.getString(R.string.limitations_summary_b),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}
