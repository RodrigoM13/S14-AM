package com.example.seguridad_priv_a

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.seguridad_priv_a.databinding.ActivityContactsBinding
import com.example.seguridad_priv_a.security.DataProtectionManager
import com.example.seguridad_priv_a.security.SecurityAuditManager

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding

    private val dataProtectionManager by lazy {
        (application as PermissionsApplication).dataProtectionManager
    }

    private val auditManager by lazy {
        SecurityAuditManager(this)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updatePermissionStatus()
        if (isGranted) {
            dataProtectionManager.logAccess("CONTACTS_PERMISSION", "Permiso otorgado")
            Toast.makeText(this, "Permiso de contactos otorgado", Toast.LENGTH_SHORT).show()
        } else {
            dataProtectionManager.logAccess("CONTACTS_PERMISSION", "Permiso denegado")
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updatePermissionStatus()
        dataProtectionManager.logAccess("NAVIGATION", "ContactsActivity abierta")
    }

    private fun setupUI() {
        binding.btnViewContacts.setOnClickListener {
            if (hasPermission()) {
                loadContacts()
            } else {
                requestContactsPermission()
            }
        }

        binding.btnRequestPermission.setOnClickListener {
            requestContactsPermission()
        }

        binding.btnSimulateSuspiciousEvent.setOnClickListener {
            val resultado = StringBuilder()
            repeat(10) { index ->
                auditManager.recordEvent("sensitive_contact_access")
                val allowed = auditManager.allowOperation("sensitive_contact_access")
                resultado.append("Intento #${index + 1}: ${if (allowed) "✅ Permitido" else "❌ Denegado"}\n")
            }
            binding.tvContactsList.text = resultado.toString()
        }

        binding.btnExportAuditLogs.setOnClickListener {
            val jsonLogs = auditManager.exportLogs()
            binding.tvContactsList.text = "🧾 Logs exportados:\n\n$jsonLogs"
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionStatus() {
        val granted = hasPermission()
        binding.tvContactsStatus.text = if (granted) {
            "✅ Permiso de contactos otorgado"
        } else {
            "❌ Permiso requerido"
        }

        binding.btnViewContacts.isEnabled = granted
        binding.btnRequestPermission.visibility = if (granted) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun requestContactsPermission() {
        when {
            hasPermission() -> updatePermissionStatus()

            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                showPermissionRationaleDialog()
            }

            else -> {
                dataProtectionManager.logAccess("CONTACTS_PERMISSION", "Solicitando permiso")
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Contactos Necesario")
            .setMessage("Se requiere acceso a los contactos para demostrar la lectura protegida. Los datos serán anonimizados.")
            .setPositiveButton("Otorgar") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Denegado")
            .setMessage("El permiso fue denegado. Ve a Configuración > Aplicaciones para otorgarlo manualmente.")
            .setPositiveButton("Ir a Configuración") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
        dataProtectionManager.logAccess("NAVIGATION", "Configuración abierta")
    }

    private fun loadContacts() {
        if (!auditManager.allowOperation("LOAD_CONTACTS")) {
            binding.tvContactsList.text = "⏱️ Acceso denegado por límite de frecuencia. Intenta más tarde."
            dataProtectionManager.logAccess("CONTACTS_RATE_LIMIT", "Bloqueado por exceso de acceso")
            return
        }

        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            val contactsList = StringBuilder()
            var contactCount = 0

            cursor?.use {
                while (it.moveToNext() && contactCount < 10) {
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                    val anonName = dataProtectionManager.anonymizeData(name)
                    val anonPhone = dataProtectionManager.anonymizeData(number)

                    contactsList.append("📞 $anonName: $anonPhone\n\n")
                    contactCount++
                }
            }

            if (contactCount > 0) {
                binding.tvContactsList.text =
                    "Contactos encontrados (anonimizados):\n\n$contactsList\n(Mostrando primeros $contactCount contactos)"
                dataProtectionManager.logAccess("CONTACTS_ACCESS", "$contactCount contactos leídos")
                dataProtectionManager.storeSecureData("last_contacts_access", System.currentTimeMillis().toString())
                auditManager.recordEvent("CONTACTS_READ", mapOf("total" to contactCount.toString()))
            } else {
                binding.tvContactsList.text = "📱 No se encontraron contactos"
                dataProtectionManager.logAccess("CONTACTS_ACCESS", "Lista vacía")
            }

        } catch (e: Exception) {
            binding.tvContactsList.text = "❌ Error al cargar contactos: ${e.message}"
            dataProtectionManager.logAccess("CONTACTS_ERROR", "Error al leer contactos: ${e.message}")
            auditManager.recordEvent("CONTACTS_ERROR", mapOf("error" to e.message.orEmpty()))
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
