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

    // Keeps the main app session open until the user leaves WhatsApp.
    fun authorizeMainLaunch() {
        prefs.edit().putBoolean("main_launch_authorized", true).apply()
    }

    fun isMainLaunchAuthorized(): Boolean =
        prefs.getBoolean("main_launch_authorized", false)

    fun clearMainLaunchAuthorization() {
        prefs.edit().putBoolean("main_launch_authorized", false).apply()
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
