package com.yuji.androidadmincommon.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPasswordTest {
    @Test
    fun adminSha256ReturnsStableHash() {
        assertEquals(
            "9af15b336e6a9619928537df30b2e6a2376569fcf9d7e773eccede65606529a0",
            "0000".adminSha256(),
        )
    }

    @Test
    fun saltedHashVerifiesOnlyMatchingPassword() {
        val stored = AdminPasswordHasher.create("1234")

        assertTrue(AdminPasswordHasher.verify("1234", stored.salt, stored.hash))
        assertFalse(AdminPasswordHasher.verify("0000", stored.salt, stored.hash))
    }

    @Test
    fun digitsOnlyRemovesNonDigitCharacters() {
        assertEquals("123４5", "a1-2 3b４5".digitsOnly())
    }
}
