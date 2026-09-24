package com.yuji.androidadmincommon.security

import java.security.MessageDigest
import java.security.SecureRandom

private val secureRandom = SecureRandom()

fun String.adminSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.digitsOnly(): String = filter(Char::isDigit)

data class SaltedPasswordHash(
    val salt: String,
    val hash: String,
)

object AdminPasswordHasher {
    fun create(password: String): SaltedPasswordHash {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        val saltText = salt.joinToString("") { "%02x".format(it) }
        return SaltedPasswordHash(
            salt = saltText,
            hash = hash(password = password, salt = saltText),
        )
    }

    fun verify(password: String, salt: String, hash: String): Boolean {
        if (salt.isBlank() || hash.isBlank()) return false
        return hash(password = password, salt = salt) == hash
    }

    fun hash(password: String, salt: String): String = "$salt:$password".adminSha256()
}

