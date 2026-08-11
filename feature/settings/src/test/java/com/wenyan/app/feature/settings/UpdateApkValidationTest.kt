package com.wenyan.app.feature.settings

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateApkValidationTest {

    @Test
    fun `apk zip requires AndroidManifest entry`() {
        val apk = createZip("AndroidManifest.xml", "binary manifest")
        val ordinaryZip = createZip("readme.txt", "not an apk")
        try {
            assertTrue(apk.isValidApkZip())
            assertFalse(ordinaryZip.isValidApkZip())
        } finally {
            apk.delete()
            ordinaryZip.delete()
        }
    }

    @Test
    fun `invalid zip is rejected`() {
        val invalid = File.createTempFile("wenyan-invalid-", ".apk")
        invalid.writeText("not a zip")
        try {
            assertFalse(invalid.isValidApkZip())
        } finally {
            invalid.delete()
        }
    }

    private fun createZip(name: String, content: String): File {
        val file = File.createTempFile("wenyan-apk-", ".apk")
        ZipOutputStream(FileOutputStream(file)).use { output ->
            output.putNextEntry(ZipEntry(name))
            output.write(content.toByteArray())
            output.closeEntry()
        }
        return file
    }
}
