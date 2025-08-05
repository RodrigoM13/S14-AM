# 🛡️ Android Security Suite App

Una aplicación Android avanzada enfocada en la **protección de datos personales**, **seguridad Zero Trust**, **anonimización de información sensible**, y **análisis forense digital**. Ideal para entornos con altos requerimientos de privacidad, cumplimiento normativo y defensa contra ingeniería inversa.

---

## 🚀 Funcionalidades Principales

### 🔐 1. **Gestión de Protección de Datos**
* Cifrado de datos sensibles con `EncryptedSharedPreferences`
* Derivación de claves con salt único por usuario (PBKDF2)
* HMAC para verificación de integridad
* Rotación automática de claves cada 30 días
* Registro de accesos con marca de tiempo
* Permite guardar, verificar, ver y eliminar datos cifrados

---

### 🧠 2. **Zero Trust Manager**
* Autenticación biométrica y por PIN
* Generación de tokens de sesión temporales para operaciones sensibles
* Verificación de integridad de la app (attestation)
* Validación por operación basada en permisos y contexto

---

### 👥 3. **Gestión de Contactos**
* Lectura de hasta 10 contactos del dispositivo
* Anonimización automática usando el `AdvancedAnonymizer`
* Registro de acceso y fecha de consulta segura

---

### 🧪 4. **Framework de Anonimización Avanzado**
Implementado en `AdvancedAnonymizer.kt`:
* `k-Anonymity` y `l-Diversity` para generalización de datos
* `Differential Privacy` para datos numéricos
* `Data Masking` contextual por tipo de dato
* Políticas de retención configurables por tipo de información

---

### 🕵️‍♂️ 5. **Análisis Forense y Compliance**
* Mantenimiento de cadena de custodia para evidencias digitales
* Logs inmutables y tamper-evident con blockchain local
* Generación automática de reportes de cumplimiento GDPR y CCPA
* Herramientas internas de investigación de incidentes

---

### 🛑 6. **Protección Contra Ingeniería Inversa**
* Detección de debugging activo, emuladores y hooking frameworks
* Obfuscación de constantes criptográficas
* Verificación de firma digital de la aplicación en runtime
* Implementación futura de `Certificate Pinning` para APIs externas

---

## 🧱 Arquitectura

* `DataProtectionManager`: núcleo de protección de datos
* `ZeroTrustManager`: autorización contextual y segura
* `AdvancedAnonymizer`: módulo especializado de anonimización
* `SecurityAuditManager`: logging, rate limiting, exportación de eventos
* `ForensicManager`: generación de evidencias, auditoría y reportes

---

## 🧩 Módulos y Archivos Clave

| Módulo | Archivo |
|--------|--------|
| Protección de datos | `DataProtectionManager.kt` |
| Seguridad Zero Trust | `ZeroTrustManager.kt` |
| Anonimización avanzada | `AdvancedAnonymizer.kt`, `MaskingPolicy.kt` |
| Seguridad y auditoría | `SecurityAuditManager.kt`, `ForensicManager.kt` |
| Aplicación base | `PermissionsApplication.kt` |
| Actividades | `ContactsActivity.kt`, `DataProtectionActivity.kt` |

---

## ⚙️ Requisitos Técnicos

* Android 8.0+
* Kotlin 1.9+
* Biometric API
* `EncryptedSharedPreferences`
* Permisos de lectura de contactos
* Fingerprint o PIN habilitado en el dispositivo

---

## 📦 Dependencias Sugeridas

* `com.google.crypto.tink` para cifrado adicional (futuro)
* `androidx.security` para almacenamiento seguro
* `androidx.biometric` para autenticación biométrica
* `web3j` para firma y manejo de blockchain local (opcional)

---

Lasimosamente no pude adjuntar por problemas del emulador de android studio y no me detectaba mi celular tampoco
