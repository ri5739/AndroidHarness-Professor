package com.androidharness.app.data.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class MultilingualTranscriptionTest {
    @Test
    fun deepgramNovaModelsEnableMultilingualRecognition() {
        assertEquals("&language=multi", DeepgramVoiceClient.multilingualLanguageParameter("nova-3"))
        assertEquals("&language=multi", DeepgramVoiceClient.multilingualLanguageParameter("nova-2"))
        assertEquals("&language=multi", DeepgramVoiceClient.multilingualLanguageParameter("NOVA-3-general"))
    }

    @Test
    fun unknownDeepgramModelsKeepTheirExistingLanguageDefaults() {
        assertEquals("", DeepgramVoiceClient.multilingualLanguageParameter("whisper-large"))
        assertEquals("", DeepgramVoiceClient.multilingualLanguageParameter("custom-model"))
    }
}
