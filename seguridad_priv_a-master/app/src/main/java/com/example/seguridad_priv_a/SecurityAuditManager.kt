package com.example.seguridad_priv_a.security

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SecurityAuditManager(private val context: Context) {

    private val eventLog: MutableList<JSONObject> = Collections.synchronizedList(mutableListOf())

    private val operationTimestamps: MutableMap<String, MutableList<Long>> = ConcurrentHashMap()

    private val rateLimitWindowMs = 10_000L  // 10 segundos
    private val rateLimitMaxAttempts = 3

    private val keyPair: KeyPair by lazy {
        generateKeyPair() // Esta clave se renueva cada vez. Puedes persistir si lo deseas.
    }

    init {
        Log.i("SecurityAudit", "Inicializando auditoría de seguridad")
    }

    /**
     * Verifica si una operación está permitida bajo rate limit.
     */
    fun allowOperation(key: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = operationTimestamps.getOrPut(key) { mutableListOf() }

        timestamps.removeAll { now - it > rateLimitWindowMs }

        return if (timestamps.size < rateLimitMaxAttempts) {
            timestamps.add(now)
            true
        } else {
            Log.w("SecurityAudit", "🔒 Operación bloqueada por rate limit: $key")
            false
        }
    }

    /**
     * Registra un evento con metadatos personalizados.
     */
    fun recordEvent(type: String, metadata: Map<String, String>) {
        val event = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("type", type)
            put("metadata", JSONObject(metadata))
        }

        eventLog.add(event)
        Log.i("SecurityAudit", "📌 Evento registrado: $event")
    }

    /**
     * Versión sobrecargada: solo tipo de evento sin metadatos.
     */
    fun recordEvent(type: String) {
        recordEvent(type, emptyMap())
    }

    /**
     * Exporta logs como JSON firmado digitalmente (RSA + SHA256).
     */
    fun exportLogs(): String {
        val logArray = JSONArray(eventLog)
        val dataToSign = logArray.toString()

        val signatureBytes = signData(dataToSign, keyPair.private)
        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        val exportJson = JSONObject().apply {
            put("log", logArray)
            put("signature", signatureBase64)
        }

        Log.i("SecurityAudit", "✅ Logs exportados con firma")
        return exportJson.toString(2) // Formateado con indentación
    }

    /**
     * Firma digital del contenido usando clave privada RSA.
     */
    private fun signData(data: String, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())
        return signature.sign()
    }

    /**
     * Genera un par de claves RSA de 2048 bits (no persistente por defecto).
     */
    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }
}
