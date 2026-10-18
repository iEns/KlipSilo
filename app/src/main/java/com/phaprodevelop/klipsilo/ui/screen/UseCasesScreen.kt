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

private data class UseCaseSection(val title: String, val body: String)

@Composable
fun UseCasesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sections =
        listOf(
            UseCaseSection(context.getString(R.string.use_1_t), context.getString(R.string.use_1_b)),
            UseCaseSection(context.getString(R.string.use_2_t), context.getString(R.string.use_2_b)),
            UseCaseSection(context.getString(R.string.use_3_t), context.getString(R.string.use_3_b)),
            UseCaseSection(context.getString(R.string.use_4_t), context.getString(R.string.use_4_b)),
            UseCaseSection(context.getString(R.string.use_5_t), context.getString(R.string.use_5_b)),
            UseCaseSection(context.getString(R.string.use_6_t), context.getString(R.string.use_6_b)),
            UseCaseSection(context.getString(R.string.use_7_t), context.getString(R.string.use_7_b)),
            UseCaseSection(context.getString(R.string.use_8_t), context.getString(R.string.use_8_b)),
            UseCaseSection(context.getString(R.string.use_9_t), context.getString(R.string.use_9_b)),
            UseCaseSection(context.getString(R.string.use_10_t), context.getString(R.string.use_10_b)),
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // The growing back-only bar (#62): the fixed 64dp Material
            // bar broke a long single-word title mid-word or clipped its
            // second line at font scale 2.0.
            FitBackBar(
                title = context.getString(R.string.tool_use_cases),
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
                text = context.getString(R.string.use_cases_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            sections.forEach { section ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        DisplayText(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.size(4.dp))
                        DisplayText(
                            text = section.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}
