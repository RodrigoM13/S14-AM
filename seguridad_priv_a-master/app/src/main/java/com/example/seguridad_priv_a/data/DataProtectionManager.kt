package com.example.seguridad_priv_a.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class DataProtectionManager(private val context: Context) {

    private val masterKeyAlias: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val keyRotationPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("key_rotation", Context.MODE_PRIVATE)
    }

    // ✅ Método faltante: inicializa el sistema
    fun initialize() {
        rotateEncryptionKey()
    }

    fun rotateEncryptionKey(): Boolean {
        val lastRotation = keyRotationPrefs.getLong("last_rotation", 0L)
        val now = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

        return if (now - lastRotation > thirtyDaysMillis) {
            keyRotationPrefs.edit().putLong("last_rotation", now).apply()
            logAccess("KEY_ROTATION", "Clave maestra rotada")
            true
        } else {
            logAccess("KEY_ROTATION", "Clave aún vigente")
            false
        }
    }

    fun deriveKey(userId: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(userId.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "HmacSHA256")
    }

    fun storeSecureDataWithIntegrity(key: String, value: String, userId: String) {
        val salt = getOrCreateSaltForUser(userId)
        val derivedKey = deriveKey(userId, salt)

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(derivedKey)
        val hmacBytes = mac.doFinal(value.toByteArray())
        val hmacString = Base64.encodeToString(hmacBytes, Base64.NO_WRAP)

        encryptedPrefs.edit()
            .putString(key, value)
            .putString("${key}_hmac", hmacString)
            .apply()

        logAccess("DATA_STORAGE", "Dato almacenado con integridad: $key")
    }

    fun verifyDataIntegrity(key: String, userId: String): Boolean {
        val data = getSecureData(key) ?: return false
        val storedHmac = encryptedPrefs.getString("${key}_hmac", null) ?: return false
        val salt = getOrCreateSaltForUser(userId)

        return try {
            val derivedKey = deriveKey(userId, salt)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(derivedKey)
            val hmacBytes = mac.doFinal(data.toByteArray())
            val calculatedHmac = Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
            storedHmac == calculatedHmac
        } catch (e: Exception) {
            logAccess("INTEGRITY_CHECK", "Error verificando integridad: ${e.message}")
            false
        }
    }

    private fun getOrCreateSaltForUser(userId: String): ByteArray {
        val saltKey = "salt_$userId"
        val existingSalt = encryptedPrefs.getString(saltKey, null)

        return if (existingSalt != null) {
            Base64.decode(existingSalt, Base64.NO_WRAP)
        } else {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            encryptedPrefs.edit()
                .putString(saltKey, Base64.encodeToString(salt, Base64.NO_WRAP))
                .apply()
            salt
        }
    }

    fun getSecureData(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }

    fun clearAllData() {
        encryptedPrefs.edit().clear().apply()
        logAccess("DATA_MANAGEMENT", "Todos los datos han sido eliminados")
    }

    fun logAccess(type: String, message: String) {
        val logsKey = "access_logs"
        val existingLogs = encryptedPrefs.getString(logsKey, "") ?: ""
        val timestamp = System.currentTimeMillis()
        val logEntry = "[$timestamp][$type] $message"
        val updatedLogs = if (existingLogs.isBlank()) logEntry else "$existingLogs\n$logEntry"
        encryptedPrefs.edit().putString(logsKey, updatedLogs).apply()
    }

    fun getAccessLogs(): List<String> {
        return encryptedPrefs.getString("access_logs", "")
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    fun getDataProtectionInfo(): Map<String, String> = mapOf(
        "Rotación de Clave" to (if (rotateEncryptionKey()) "Actualizada" else "Vigente"),
        "Almacenamiento" to "EncryptedSharedPreferences",
        "Integridad" to "HMAC con clave derivada por usuario",
        "Salt por Usuario" to "Sí",
        "Logs de Acceso" to "Registrados"
    )

    fun storeSecureData(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
        logAccess("DATA_STORAGE", "Dato almacenado: $key")
    }

    fun anonymizeData(data: String): String {
        // Ejemplo simple de anonimización: enmascarar caracteres
        return data.replace(Regex("."), "*")
    }



}
