package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R

private data class WhySection(val title: String, val body: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sections =
        listOf(
            WhySection(context.getString(R.string.why_1_t), context.getString(R.string.why_1_b)),
            WhySection(context.getString(R.string.why_2_t), context.getString(R.string.why_2_b)),
            WhySection(context.getString(R.string.why_3_t), context.getString(R.string.why_3_b)),
            WhySection(context.getString(R.string.why_4_t), context.getString(R.string.why_4_b)),
            WhySection(context.getString(R.string.why_5_t), context.getString(R.string.why_5_b)),
            WhySection(context.getString(R.string.why_6_t), context.getString(R.string.why_6_b)),
            WhySection(context.getString(R.string.why_7_t), context.getString(R.string.why_7_b)),
            WhySection(context.getString(R.string.why_8_t), context.getString(R.string.why_8_b)),
            WhySection(context.getString(R.string.why_9_t), context.getString(R.string.why_9_b)),
            WhySection(context.getString(R.string.why_10_t), context.getString(R.string.why_10_b)),
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.tool_why)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
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
                text = context.getString(R.string.why_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            sections.forEach { section ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
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
