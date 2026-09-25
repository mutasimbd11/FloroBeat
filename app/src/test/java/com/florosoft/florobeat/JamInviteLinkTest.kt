package com.florosoft.florobeat

import com.florosoft.florobeat.data.listentogether.JamInviteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JamInviteLinkTest {

    @Test
    fun `parses and normalizes a public invite`() {
        assertEquals(
            "A1B2C3",
            JamInviteLink.parse("https://florobeat.florosoft.com/invite/a1b2c3"),
        )
    }

    @Test
    fun `accepts query parameters without making them part of the code`() {
        assertEquals(
            "ABC123",
            JamInviteLink.parse("https://florobeat.florosoft.com/invite/ABC123?from=share"),
        )
    }

    @Test
    fun `rejects other hosts schemes paths and malformed codes`() {
        assertNull(JamInviteLink.parse("http://florobeat.florosoft.com/invite/ABC123"))
        assertNull(JamInviteLink.parse("https://example.com/invite/ABC123"))
        assertNull(JamInviteLink.parse("https://florobeat.florosoft.com/"))
        assertNull(JamInviteLink.parse("https://florobeat.florosoft.com/download"))
        assertNull(JamInviteLink.parse("https://florobeat.florosoft.com/invite/ABC123/"))
        assertNull(JamInviteLink.parse("https://florobeat.florosoft.com/invite/ABC123/extra"))
        assertNull(JamInviteLink.parse("https://florobeat.florosoft.com/invite/TOO-LONG"))
    }

    @Test
    fun `builds the canonical share URL`() {
        assertEquals(
            "https://florobeat.florosoft.com/invite/ABC123",
            JamInviteLink.url("abc123"),
        )
    }
}
