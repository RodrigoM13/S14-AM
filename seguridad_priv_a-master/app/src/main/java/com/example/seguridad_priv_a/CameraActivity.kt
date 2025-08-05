package com.example.seguridad_priv_a

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.seguridad_priv_a.databinding.ActivityCameraBinding
import com.example.seguridad_priv_a.security.DataProtectionManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    // Instancia explícita para que la importación se use
    private val dataProtectionManager: DataProtectionManager by lazy {
        (application as PermissionsApplication).dataProtectionManager
    }

    private var currentPhotoUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->  // Tipo explícito para evitar error de inferencia
        updatePermissionStatus()
        if (isGranted) {
            dataProtectionManager.logAccess("CAMERA_PERMISSION", "Permiso de cámara otorgado")
            Toast.makeText(this, "Permiso de cámara otorgado", Toast.LENGTH_SHORT).show()
        } else {
            dataProtectionManager.logAccess("CAMERA_PERMISSION", "Permiso de cámara denegado")
            showPermissionDeniedDialog()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->  // Tipo explícito para evitar error de inferencia
        if (success) {
            currentPhotoUri?.let { uri ->
                binding.ivCameraPreview.setImageURI(uri)
                dataProtectionManager.logAccess("CAMERA_ACCESS", "Foto capturada y almacenada")

                val photoPath = uri.toString()
                val userId = "usuario123" // TODO: reemplaza con ID real del usuario

                dataProtectionManager.storeSecureDataWithIntegrity("last_photo_path", photoPath, userId)
                dataProtectionManager.storeSecureDataWithIntegrity("last_photo_timestamp", System.currentTimeMillis().toString(), userId)

                Toast.makeText(this, "Foto guardada de forma segura", Toast.LENGTH_SHORT).show()
            }
        } else {
            dataProtectionManager.logAccess("CAMERA_ACCESS", "Captura de foto cancelada")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updatePermissionStatus()
        loadLastPhoto()

        dataProtectionManager.logAccess("NAVIGATION", "CameraActivity abierta")
    }

    private fun setupUI() {
        binding.btnTakePhoto.setOnClickListener {
            if (hasPermission()) {
                takePhoto()
            } else {
                requestCameraPermission()
            }
        }

        binding.btnRequestPermission.setOnClickListener {
            requestCameraPermission()
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionStatus() {
        val hasPermission = hasPermission()

        binding.tvCameraStatus.text = if (hasPermission) {
            "✅ Permiso de cámara otorgado"
        } else {
            "❌ Permiso de cámara requerido"
        }

        binding.btnTakePhoto.isEnabled = hasPermission
        binding.btnRequestPermission.visibility = if (hasPermission) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun requestCameraPermission() {
        when {
            hasPermission() -> {
                updatePermissionStatus()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                dataProtectionManager.logAccess("CAMERA_PERMISSION", "Solicitando permiso de cámara")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Cámara Necesario")
            .setMessage("Esta aplicación necesita acceso a la cámara para tomar fotos de demostración. Las fotos se almacenan de forma segura y encriptada.")
            .setPositiveButton("Otorgar") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Denegado")
            .setMessage("El permiso de cámara ha sido denegado. Para usar esta función, ve a Configuración y otorga el permiso manualmente.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
        dataProtectionManager.logAccess("NAVIGATION", "Configuración de la app abierta")
    }

    private fun takePhoto() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "com.example.seguridad_priv_a.fileprovider",
                photoFile
            )

            currentPhotoUri?.let { uri ->
                takePictureLauncher.launch(uri)
            }
            dataProtectionManager.logAccess("CAMERA_ACCESS", "Iniciando captura de foto")

        } catch (ex: IOException) {
            dataProtectionManager.logAccess("CAMERA_ERROR", "Error al crear archivo de imagen: ${ex.message}")
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(getExternalFilesDir(null), "Pictures")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, "JPEG_${timeStamp}_.jpg")
    }

    private fun loadLastPhoto() {
        val userId = "usuario123" // TODO: reemplaza con ID real del usuario
        val lastPhotoPath = dataProtectionManager.getSecureData("last_photo_path")
        lastPhotoPath?.let { path ->
            try {
                val uri = Uri.parse(path)
                binding.ivCameraPreview.setImageURI(uri)
                dataProtectionManager.logAccess("DATA_ACCESS", "Última foto cargada")
            } catch (e: Exception) {
                dataProtectionManager.logAccess("DATA_ERROR", "Error al cargar última foto: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
