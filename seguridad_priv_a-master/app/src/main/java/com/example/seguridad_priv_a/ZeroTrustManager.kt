package com.example.seguridad_priv_a.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ZeroTrustManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zero_trust_prefs", Context.MODE_PRIVATE)

    private val sessionDuration = 60_000L // 1 minuto
    private var sessionToken: String? = null
    private var sessionStartTime: Long = 0

    private val sensitiveOperations = listOf("view_logs", "clear_data", "simulate_threat")
    private val operationTimestamps = mutableMapOf<String, Long>()

    private fun generateRandomToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun generateSessionToken() {
        sessionToken = generateRandomToken()
        sessionStartTime = SystemClock.elapsedRealtime()
        prefs.edit()
            .putString("session_token", sessionToken)
            .putLong("session_start", sessionStartTime)
            .apply()
    }

    fun isSessionValid(): Boolean {
        val token = prefs.getString("session_token", null)
        val start = prefs.getLong("session_start", 0L)
        val now = SystemClock.elapsedRealtime()
        return token != null && (now - start) < sessionDuration
    }

    fun invalidateSessionToken() {
        sessionToken = null
        prefs.edit().remove("session_token").remove("session_start").apply()
    }

    fun validateSensitiveOperation(operation: String): Boolean {
        if (!sensitiveOperations.contains(operation)) return false
        if (!isSessionValid()) return false

        val now = SystemClock.elapsedRealtime()
        val lastTime = operationTimestamps[operation] ?: 0L
        if (now - lastTime < 3000L) return false // evitar uso repetido en < 3s

        operationTimestamps[operation] = now
        return true
    }

    fun simulateSuspiciousActivity() {
        prefs.edit().putBoolean("suspicious_flag", true).apply()
    }

    fun isSuspiciousActivityDetected(): Boolean {
        return prefs.getBoolean("suspicious_flag", false)
    }

    fun clearSuspiciousFlag() {
        prefs.edit().remove("suspicious_flag").apply()
    }

    fun verifyAppIntegrity(): Boolean {
        return try {
            val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val actualSig = appInfo.signatures?.firstOrNull()?.toCharsString() ?: return false
            val digest = MessageDigest.getInstance("SHA-256").digest(actualSig.toByteArray())
            val hashBase64 = Base64.encodeToString(digest, Base64.NO_WRAP)

            // Esto es solo un ejemplo de hash. Puedes reemplazarlo por uno de confianza.
            val expectedPrefix = "gA4+"
            hashBase64.startsWith(expectedPrefix)
        } catch (e: Exception) {
            false
        }
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    // 🔹 MÉTODO FALTANTE: performAppAttestation()
    fun performAppAttestation(): Boolean {
        return verifyAppIntegrity()
    }
}
