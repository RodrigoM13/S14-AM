package com.example.seguridad_priv_a

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.seguridad_priv_a.databinding.ActivityDataProtectionBinding
import java.util.concurrent.Executor

class DataProtectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataProtectionBinding
    private val dataProtectionManager by lazy {
        (application as PermissionsApplication).dataProtectionManager
    }
    private val zeroTrustManager by lazy {
        (application as PermissionsApplication).zeroTrustManager
    }

    private var isAuthenticated = false
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val sessionTimeout: Long = 60_000 // 1 minuto
    private val handler = Handler(Looper.getMainLooper())
    private val sessionRunnable = Runnable {
        showSessionExpiredDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataProtectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!zeroTrustManager.verifyAppIntegrity()) {
            Toast.makeText(this, "⚠️ Integridad comprometida. Cerrando app.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupBiometricPrompt()
        authenticateUser("Autenticación requerida para acceder a los datos")
        setupUI()

        dataProtectionManager.logAccess("NAVIGATION", "DataProtectionActivity abierta")
    }

    private fun setupUI() {
        binding.btnViewLogs.setOnClickListener {
            if (isAuthenticated && zeroTrustManager.validateSensitiveOperation("view_logs")) {
                loadAccessLogs()
                Toast.makeText(this, "Logs actualizados", Toast.LENGTH_SHORT).show()
                resetSessionTimer()
            } else {
                authenticateUser("Necesitas autenticarte para ver los logs")
            }
        }

        binding.btnClearData.setOnClickListener {
            if (isAuthenticated && zeroTrustManager.validateSensitiveOperation("clear_data")) {
                showClearDataDialog()
                resetSessionTimer()
            } else {
                authenticateUser("Necesitas autenticarte para borrar los datos")
            }
        }

        binding.btnSimulateThreat.setOnClickListener {
            if (isAuthenticated && zeroTrustManager.validateSensitiveOperation("simulate_threat")) {
                zeroTrustManager.simulateSuspiciousActivity()
                Toast.makeText(this, "Evento sospechoso simulado", Toast.LENGTH_SHORT).show()
                resetSessionTimer()
            } else {
                authenticateUser("Necesitas autenticarte para simular eventos")
            }
        }
    }

    private fun setupBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticated = true
                    zeroTrustManager.generateSessionToken()
                    resetSessionTimer()
                    loadDataProtectionInfo()
                    loadAccessLogs()
                    Toast.makeText(applicationContext, "Autenticación exitosa", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación Requerida")
            .setSubtitle("Usa tu huella digital")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun authenticateUser(reason: String) {
        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Biometría no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetSessionTimer() {
        handler.removeCallbacks(sessionRunnable)
        handler.postDelayed(sessionRunnable, sessionTimeout)
    }

    private fun showSessionExpiredDialog() {
        isAuthenticated = false
        zeroTrustManager.invalidateSessionToken()
        AlertDialog.Builder(this)
            .setTitle("Sesión expirada")
            .setMessage("Por seguridad, debes volver a autenticarte.")
            .setCancelable(false)
            .setPositiveButton("Autenticar") { _, _ -> authenticateUser("Reautenticación requerida") }
            .show()
    }

    private fun loadDataProtectionInfo() {
        val info = dataProtectionManager.getDataProtectionInfo()
        val infoText = StringBuilder()

        infoText.append("🔐 INFORMACIÓN DE SEGURIDAD\n\n")
        info.forEach { (key, value) ->
            infoText.append("• $key: $value\n")
        }

        infoText.append("\n📊 EVIDENCIAS DE PROTECCIÓN:\n")
        infoText.append("• Encriptación AES-256-GCM activa\n")
        infoText.append("• Todos los accesos registrados\n")
        infoText.append("• Datos anonimizados automáticamente\n")
        infoText.append("• Almacenamiento local seguro\n")
        infoText.append("• No hay compartición de datos\n")

        binding.tvDataProtectionInfo.text = infoText.toString()

        dataProtectionManager.logAccess("DATA_PROTECTION", "Información de protección mostrada")
    }

    private fun loadAccessLogs() {
        val logs = dataProtectionManager.getAccessLogs()

        binding.tvAccessLogs.text = if (logs.isNotEmpty()) {
            logs.joinToString("\n")
        } else {
            "No hay logs disponibles"
        }

        dataProtectionManager.logAccess("DATA_ACCESS", "Logs de acceso consultados")
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Borrar Todos los Datos")
            .setMessage("¿Estás seguro de que deseas borrar todos los datos almacenados y logs de acceso? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearAllData() {
        dataProtectionManager.clearAllData()
        zeroTrustManager.invalidateSessionToken()

        binding.tvAccessLogs.text = "Todos los datos han sido borrados"
        binding.tvDataProtectionInfo.text =
            "🔐 DATOS BORRADOS DE FORMA SEGURA\n\nTodos los datos personales y logs han sido eliminados del dispositivo."

        Toast.makeText(this, "Datos borrados de forma segura", Toast.LENGTH_LONG).show()

        dataProtectionManager.logAccess("DATA_MANAGEMENT", "Todos los datos borrados por el usuario")
    }

    override fun onResume() {
        super.onResume()
        if (isAuthenticated) loadAccessLogs()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(sessionRunnable)
    }
}
