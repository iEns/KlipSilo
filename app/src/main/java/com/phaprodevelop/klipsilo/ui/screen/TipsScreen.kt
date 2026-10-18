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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.ui.components.DisplayText
import com.phaprodevelop.klipsilo.ui.components.FitBackBar

private data class Tip(val title: String, val body: String)

private data class TipCategory(val title: String, val tips: List<Tip>)

@Composable
fun TipsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val categories =
        listOf(
            TipCategory(
                context.getString(R.string.tips_cat_clips),
                listOf(
                    Tip(context.getString(R.string.tip_clips_auto_t), context.getString(R.string.tip_clips_auto_b)),
                    Tip(context.getString(R.string.tip_clips_search_t), context.getString(R.string.tip_clips_search_b)),
                    Tip(context.getString(R.string.tip_clips_tap_t), context.getString(R.string.tip_clips_tap_b)),
                    Tip(context.getString(R.string.tip_clips_pin_t), context.getString(R.string.tip_clips_pin_b)),
                    Tip(context.getString(R.string.tip_clips_rename_t), context.getString(R.string.tip_clips_rename_b)),
                    Tip(
                        context.getString(R.string.tip_clips_duplicate_t),
                        context.getString(R.string.tip_clips_duplicate_b),
                    ),
                    Tip(context.getString(R.string.tip_clips_lock_t), context.getString(R.string.tip_clips_lock_b)),
                    Tip(context.getString(R.string.tip_clips_delete_t), context.getString(R.string.tip_clips_delete_b)),
                ),
            ),
            TipCategory(
                context.getString(R.string.tips_cat_sheet),
                listOf(
                    Tip(context.getString(R.string.tip_sheet_open_t), context.getString(R.string.tip_sheet_open_b)),
                    Tip(context.getString(R.string.tip_sheet_links_t), context.getString(R.string.tip_sheet_links_b)),
                    Tip(
                        context.getString(R.string.tip_sheet_convert_t),
                        context.getString(R.string.tip_sheet_convert_b),
                    ),
                ),
            ),
            TipCategory(
                context.getString(R.string.tips_cat_pdf),
                listOf(
                    Tip(context.getString(R.string.tip_pdf_zoom_t), context.getString(R.string.tip_pdf_zoom_b)),
                    Tip(context.getString(R.string.tip_pdf_pages_t), context.getString(R.string.tip_pdf_pages_b)),
                    Tip(context.getString(R.string.tip_pdf_qr_t), context.getString(R.string.tip_pdf_qr_b)),
                    Tip(context.getString(R.string.tip_pdf_ocr_t), context.getString(R.string.tip_pdf_ocr_b)),
                ),
            ),
            TipCategory(
                context.getString(R.string.tips_cat_tools),
                listOf(
                    Tip(context.getString(R.string.tip_tools_text_t), context.getString(R.string.tip_tools_text_b)),
                    Tip(context.getString(R.string.tip_tools_pos_t), context.getString(R.string.tip_tools_pos_b)),
                    Tip(
                        context.getString(R.string.tip_tools_combine_t),
                        context.getString(R.string.tip_tools_combine_b),
                    ),
                    Tip(
                        context.getString(R.string.tip_tools_annotate_t),
                        context.getString(R.string.tip_tools_annotate_b),
                    ),
                    Tip(
                        context.getString(R.string.tip_tools_maintenance_t),
                        context.getString(R.string.tip_tools_maintenance_b),
                    ),
                ),
            ),
            TipCategory(
                context.getString(R.string.tips_cat_camera),
                listOf(
                    Tip(context.getString(R.string.tip_camera_scan_t), context.getString(R.string.tip_camera_scan_b)),
                ),
            ),
            TipCategory(
                context.getString(R.string.tips_cat_settings),
                listOf(
                    Tip(
                        context.getString(R.string.tip_settings_lang_t),
                        context.getString(R.string.tip_settings_lang_b),
                    ),
                    Tip(context.getString(R.string.tip_settings_pdf_t), context.getString(R.string.tip_settings_pdf_b)),
                ),
            ),
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // The growing back-only bar (#62): the fixed 64dp Material
            // bar broke a long single-word title mid-word or clipped its
            // second line at font scale 2.0.
            FitBackBar(
                title = context.getString(R.string.tips),
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
            categories.forEach { category ->
                DisplayText(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                category.tips.forEach { tip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            DisplayText(
                                text = tip.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.size(4.dp))
                            DisplayText(
                                text = tip.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}
