@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** Locale tag → privacy raw resource (#87). */
class PrivacyPolicyRawTest {
    @Test
    fun mapsEveryPickerTagToItsRawResource() {
        assertEquals(R.raw.privacy_da, privacyRawResId("da"))
        assertEquals(R.raw.privacy_de, privacyRawResId("de"))
        assertEquals(R.raw.privacy_es, privacyRawResId("es"))
        assertEquals(R.raw.privacy_fr, privacyRawResId("fr"))
        assertEquals(R.raw.privacy_in, privacyRawResId("id"))
        assertEquals(R.raw.privacy_it, privacyRawResId("it"))
        assertEquals(R.raw.privacy_ja, privacyRawResId("ja"))
        assertEquals(R.raw.privacy_ko, privacyRawResId("ko"))
        assertEquals(R.raw.privacy_pt_br, privacyRawResId("pt-BR"))
        assertEquals(R.raw.privacy_ru, privacyRawResId("ru"))
        assertEquals(R.raw.privacy_tr, privacyRawResId("tr"))
        assertEquals(R.raw.privacy_zh_hans, privacyRawResId("zh-Hans"))
        assertEquals(R.raw.privacy_en, privacyRawResId("en"))
    }

    @Test
    fun unknownTagFallsBackToEnglish() {
        assertEquals(R.raw.privacy_en, privacyRawResId("nl"))
        assertEquals(R.raw.privacy_en, privacyRawResId(""))
        assertEquals(R.raw.privacy_en, privacyRawResId("pt-PT"))
        assertEquals(R.raw.privacy_en, privacyRawResId("zh-TW"))
    }
}
