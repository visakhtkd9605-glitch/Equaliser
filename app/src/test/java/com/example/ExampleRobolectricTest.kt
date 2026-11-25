package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.CanonicalEqBands
import com.example.data.db.PresetEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SonicEQ", appName)
    }

    @Test
    fun `canonical band interpolation samples center and midpoints accurately`() {
        val gains = listOf(6f, 4f, 0f, -2f, 0f, 2f, 4f, 6f, 3f, 0f)
        assertEquals(6f, CanonicalEqBands.sampleGainAtHz(gains, 31f), 0.01f)
        assertEquals(0f, CanonicalEqBands.sampleGainAtHz(gains, 16000f), 0.01f)
        // Geometric midpoint of 1000 Hz (2dB) and 2000 Hz (4dB) is ~1414.2 Hz -> ~3dB
        assertEquals(3f, CanonicalEqBands.sampleGainAtHz(gains, 1414.2f), 0.1f)
    }

    @Test
    fun `preset entity parses 10 band csv safely`() {
        val preset = PresetEntity(
            name = "Test",
            genreCategoryId = 1L,
            genreName = "Rock",
            bandLevelsCsv = "4.5,3.5,2.0,0.5,-1.5,-1.0,1.5,3.5,4.5,4.0"
        )
        val parsed = preset.parsedBandGainsDb()
        assertEquals(10, parsed.size)
        assertEquals(4.5f, parsed.first(), 0.01f)
        assertEquals(4.0f, parsed.last(), 0.01f)
    }
}
