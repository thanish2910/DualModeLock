package com.duallock

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local PIN verifier for this prototype. Stores salted PBKDF2 hashes, never raw PINs.
 * For production, add stronger throttling, biometric/device-credential integration,
 * security review, and encrypted preferences for sensitive configuration.
 */
class PinStore(context: Context) {
    private val prefs = context.getSharedPreferences("dual_lock_pins", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    enum class Result { MAIN, DECOY, INVALID, NOT_CONFIGURED }

    fun isConfigured(): Boolean =
        prefs.contains("main_salt") && prefs.contains("main_hash") &&
            prefs.contains("decoy_salt") && prefs.contains("decoy_hash")

    // One-shot grace period so the foreground watcher does not immediately relock
    // the official app launched after a correct main PIN.
    fun authorizeMainLaunch() {
        prefs.edit().putLong("main_launch_authorized_until", System.currentTimeMillis() + 5000L).apply()
    }

    fun isMainLaunchAuthorized(): Boolean =
        System.currentTimeMillis() <= prefs.getLong("main_launch_authorized_until", 0L)

    fun clearMainLaunchAuthorization() {
        prefs.edit().remove("main_launch_authorized_until").apply()
    }

    fun savePins(mainPin: String, decoyPin: String) {
        val mainSalt = ByteArray(16).also(random::nextBytes)
        val decoySalt = ByteArray(16).also(random::nextBytes)
        prefs.edit()
            .putString("main_salt", encode(mainSalt))
            .putString("main_hash", encode(hash(mainPin, mainSalt)))
            .putString("decoy_salt", encode(decoySalt))
            .putString("decoy_hash", encode(hash(decoyPin, decoySalt)))
            .apply()
    }

    fun verify(pin: String): Result {
        if (!isConfigured()) return Result.NOT_CONFIGURED
        val mainOk = matches(pin, "main")
        val decoyOk = matches(pin, "decoy")
        return when {
            mainOk -> Result.MAIN
            decoyOk -> Result.DECOY
            else -> Result.INVALID
        }
    }

    private fun matches(pin: String, prefix: String): Boolean {
        val salt = decode(prefs.getString("${prefix}_salt", null) ?: return false)
        val expected = decode(prefs.getString("${prefix}_hash", null) ?: return false)
        val actual = hash(pin, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}
