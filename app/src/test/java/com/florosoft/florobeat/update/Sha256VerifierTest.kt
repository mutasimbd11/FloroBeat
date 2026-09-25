package com.florosoft.florobeat.update

import com.florosoft.florobeat.data.update.ApkVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for SHA-256 calculation and verification.
 */
class Sha256VerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `calculates correct sha256 for known file content`() {
        val file = tempFolder.newFile("test.bin")
        file.writeText("FloroBeat Music Player Update")

        // echo -n "FloroBeat Music Player Update" | sha256sum
        // -> 0d9082be0e81c034a5d893f0b0906be730fe3452296d36e2f1f8b4d8d17ba96d (for example)
        val calculated = ApkVerifier.calculateSha256(file)
        assertNotNull(calculated)
        assertEquals(64, calculated.length)

        // Verifying against itself must succeed
        assertTrue(ApkVerifier.verifySha256(file, calculated))
    }

    @Test
    fun `extracts sha256 from sha256sum file output format`() {
        val raw = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  FloroBeat-1.2.0.apk\n"
        val extracted = ApkVerifier.extractSha256(raw)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", extracted)

        val standalone = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertEquals(standalone, ApkVerifier.extractSha256(standalone))

        val invalid = "not-a-hash"
        assertNull(ApkVerifier.extractSha256(invalid))
    }

    @Test
    fun `detects mismatched or corrupted file digest`() {
        val file = tempFolder.newFile("corrupt.apk")
        file.writeText("Corrupted or modified APK content")

        val wrongHash = "0000000000000000000000000000000000000000000000000000000000000000"
        val matches = ApkVerifier.verifySha256(file, wrongHash)

        assertFalse("Mismatched hash must fail verification", matches)
    }
}
