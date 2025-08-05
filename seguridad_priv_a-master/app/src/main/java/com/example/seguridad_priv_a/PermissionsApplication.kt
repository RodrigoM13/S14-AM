package com.example.seguridad_priv_a

import android.app.Application
import com.example.seguridad_priv_a.security.DataProtectionManager
import com.example.seguridad_priv_a.security.ZeroTrustManager
import com.example.seguridad_priv_a.security.SecurityAuditManager

class PermissionsApplication : Application() {

    // Instancia de protección de datos
    val dataProtectionManager by lazy {
        DataProtectionManager(this)
    }

    // Instancia del gestor Zero Trust
    val zeroTrustManager by lazy {
        ZeroTrustManager(this)
    }

    // Instancia del gestor de auditoría de seguridad
    val securityAuditManager by lazy {
        SecurityAuditManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializa protección de datos
        dataProtectionManager.initialize()
        dataProtectionManager.logAccess("APPLICATION", "App iniciada")

        // Attestation de integridad con Zero Trust
        val appIsTrusted = zeroTrustManager.performAppAttestation()
        if (!appIsTrusted) {
            dataProtectionManager.logAccess("ZERO_TRUST", "❌ Integridad de app fallida")
            securityAuditManager.recordEvent(
                type = "APP_INTEGRITY_FAILED",
                metadata = mapOf("timestamp" to System.currentTimeMillis().toString())
            )
            // Aquí podrías bloquear funcionalidades, mostrar advertencia, etc.
        } else {
            dataProtectionManager.logAccess("ZERO_TRUST", "✅ Integridad verificada")
            securityAuditManager.recordEvent("APP_INTEGRITY_VERIFIED")
        }
    }
}
