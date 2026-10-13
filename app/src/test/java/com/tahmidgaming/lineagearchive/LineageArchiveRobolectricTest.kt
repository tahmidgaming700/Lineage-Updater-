package com.tahmidgaming.lineagearchive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LineageArchiveRobolectricTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Lineage Updater & Downloader", appName)
    }

    @Test
    fun testThemePreferencesPersistence() {
        ThemePreferences.set(context, ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemePreferences.get(context))

        ThemePreferences.set(context, ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, ThemePreferences.get(context))

        ThemePreferences.set(context, ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, ThemePreferences.get(context))
    }

    @Test
    fun testDownloadStoreAddAndUpdate() {
        val id = DownloadStore.newId()
        val item = DownloadStore.Item(
            id = id,
            filename = "lineage-21.0-20241010-nightly-cheeseburger-signed.zip",
            device = "cheeseburger",
            version = "21.0",
            expectedSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            expectedSize = 1048576L,
            url = "https://download.lineageos.org/test"
        )

        DownloadStore.add(context, item)
        val loaded = DownloadStore.items(context).firstOrNull { it.id == id }
        assertNotNull(loaded)
        assertEquals("Queued", loaded?.status)

        DownloadStore.update(context, id) {
            it.copy(status = "Downloaded • SHA-256 PASS", verified = true)
        }
        val updated = DownloadStore.items(context).firstOrNull { it.id == id }
        assertEquals("Downloaded • SHA-256 PASS", updated?.status)
        assertEquals(true, updated?.verified)
    }

    @Test
    fun testDeviceDetectorMatching() {
        val devices = listOf(
            LineageDevice(model = "cheeseburger", oem = "OnePlus", name = "OnePlus 5"),
            LineageDevice(model = "dumpling", oem = "OnePlus", name = "OnePlus 5T"),
            LineageDevice(model = "bacon", oem = "OnePlus", name = "OnePlus One")
        )

        // Exact codename match
        val info1 = DeviceDetector.Info(manufacturer = "OnePlus", model = "ONEPLUS A5000", device = "cheeseburger", product = "OnePlus5")
        val match1 = DeviceDetector.match(info1, devices)
        assertEquals("cheeseburger", match1?.model)

        // Product match
        val info2 = DeviceDetector.Info(manufacturer = "OnePlus", model = "Unknown", device = "other", product = "dumpling")
        val match2 = DeviceDetector.match(info2, devices)
        assertEquals("dumpling", match2?.model)

        // No match
        val info3 = DeviceDetector.Info(manufacturer = "Google", model = "Pixel 8", device = "shiba", product = "shiba")
        val match3 = DeviceDetector.match(info3, devices)
        assertNull(match3)
    }

    @Test
    fun testGsiCompatibilityDetectorDefaults() {
        val arch = GsiCompatibilityDetector.defaultArchitecture()
        assertTrue(arch in listOf("arm64", "arm", "x86_64", "x86", "All"))
    }

    @Test
    fun testVersionComparisonLogic() {
        assertTrue(compareVersions("2.0.0", "1.0.36") > 0)
        assertTrue(compareVersions("1.0.36", "1.0.35") > 0)
        assertEquals(0, compareVersions("2.0.0", "2.0.0"))
        assertTrue(compareVersions("1.0.36", "2.0.0") < 0)
    }

    @Test
    fun testRootTestingModeSimulation() {
        FlashManager.setRootTestingMode(context, true)
        assertTrue(FlashManager.isRootTestingMode(context))
        val capability = FlashManager.detect(context)
        assertTrue(capability.rooted)
        assertTrue(capability.twrpDetected)
        assertTrue(capability.orangeFoxDetected)
        assertTrue(capability.isSimulation)

        val flashResult = FlashManager.flashVerifiedZip(context, "test-update.zip", FlashManager.Recovery.TWRP)
        assertTrue(flashResult.isSuccess)
        assertTrue(flashResult.getOrNull()?.contains("Safety Simulation Mode") == true)

        val ddResult = FlashManager.flashImageWithDd(context, "boot.img", "boot_a")
        assertTrue(ddResult.isSuccess)
        assertTrue(ddResult.getOrNull()?.contains("Safety Simulation Mode") == true)
    }
}
