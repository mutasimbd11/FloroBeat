package com.florosoft.florobeat.update

import com.florosoft.florobeat.data.update.ApkVerifier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for GitHub Releases payload parsing and asset discovery.
 */
class ReleaseParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val mockReleaseJson = """
    {
      "tag_name": "v1.2.0",
      "name": "FloroBeat v1.2.0",
      "draft": false,
      "prerelease": false,
      "html_url": "https://github.com/mutasimbd11/FloroBeat/releases/tag/v1.2.0",
      "body": "## What's New\n- High-resolution audio engine\n- GitHub release update system\n\n## Technical\nVersion: 1.2.0\nSHA-256: 4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945",
      "assets": [
        {
          "name": "FloroBeat-1.2.0.apk",
          "size": 15420310,
          "state": "uploaded",
          "browser_download_url": "https://github.com/mutasimbd11/FloroBeat/releases/download/v1.2.0/FloroBeat-1.2.0.apk"
        },
        {
          "name": "FloroBeat-1.2.0.apk.sha256",
          "size": 65,
          "state": "uploaded",
          "browser_download_url": "https://github.com/mutasimbd11/FloroBeat/releases/download/v1.2.0/FloroBeat-1.2.0.apk.sha256"
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `parses release tag and notes successfully`() {
        val element = json.parseToJsonElement(mockReleaseJson) as JsonObject
        val tag = element["tag_name"]?.jsonPrimitive?.contentOrNull
        val name = element["name"]?.jsonPrimitive?.contentOrNull
        val isDraft = element["draft"]?.jsonPrimitive?.contentOrNull == "true"
        val isPrerelease = element["prerelease"]?.jsonPrimitive?.contentOrNull == "true"

        assertEquals("v1.2.0", tag)
        assertEquals("FloroBeat v1.2.0", name)
        assertEquals(false, isDraft)
        assertEquals(false, isPrerelease)
    }

    @Test
    fun `discovers APK and SHA256 assets from asset list`() {
        val element = json.parseToJsonElement(mockReleaseJson) as JsonObject
        val assets = element["assets"]?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()

        val apkAsset = assets.firstOrNull { asset ->
            asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true &&
                asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
        }
        assertNotNull(apkAsset)
        val apkUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
        assertEquals(
            "https://github.com/mutasimbd11/FloroBeat/releases/download/v1.2.0/FloroBeat-1.2.0.apk",
            apkUrl,
        )

        val sha256Asset = assets.firstOrNull { asset ->
            asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".sha256", ignoreCase = true) == true &&
                asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
        }
        assertNotNull(sha256Asset)
        val sha256Url = sha256Asset?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
        assertEquals(
            "https://github.com/mutasimbd11/FloroBeat/releases/download/v1.2.0/FloroBeat-1.2.0.apk.sha256",
            sha256Url,
        )
    }

    @Test
    fun `extracts SHA-256 hash from release notes body`() {
        val element = json.parseToJsonElement(mockReleaseJson) as JsonObject
        val body = element["body"]?.jsonPrimitive?.contentOrNull
        assertNotNull(body)

        val hashLine = body!!.lines().firstOrNull { it.contains("SHA-256:", ignoreCase = true) }
        assertNotNull(hashLine)
        val rawHash = hashLine!!.substringAfter("SHA-256:").trim()
        val extracted = ApkVerifier.extractSha256(rawHash)

        assertEquals("4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945", extracted)
    }

    @Test
    fun `handles release without APK asset gracefully`() {
        val emptyReleaseJson = """
        {
          "tag_name": "v1.2.0",
          "assets": []
        }
        """.trimIndent()

        val element = json.parseToJsonElement(emptyReleaseJson) as JsonObject
        val assets = element["assets"]?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()
        val apk = assets.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }

        assertNull(apk)
    }
}
