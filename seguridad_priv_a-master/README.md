## 🔐 1.1 Identificación de Vulnerabilidades

### ¿Qué método de encriptación se utiliza para proteger datos sensibles?

Se utiliza **AES en modo GCM (Galois/Counter Mode)**, que es un algoritmo de cifrado simétrico con autenticación de integridad incluida. Este método es adecuado para proteger datos sensibles, ya que asegura tanto la confidencialidad como la integridad de los datos.

### ¿Cuáles son al menos 2 posibles vulnerabilidades en la implementación del logging?

1. **Falta de clasificación de eventos**: El sistema de logs no clasifica los eventos por nivel de severidad (`info`, `warning`, `error`), lo cual dificulta la detección de incidentes graves o inusuales.
2. **Ausencia de control de acceso o cifrado**: Los registros de acceso no están protegidos ni cifrados, lo que podría permitir que otras partes de la aplicación, o incluso apps maliciosas, accedan a información sensible del sistema.

### ¿Qué sucede si falla la inicialización del sistema de encriptación?

La implementación actual **no maneja de forma explícita los errores** durante la inicialización. Si esta falla, algunas funciones críticas que dependen del cifrado pueden fallar silenciosamente o comportarse de manera inesperada, sin alertar al usuario ni detener la app de forma segura.

---

## 📱 1.2 Permisos y Manifiesto

### ¿Qué permisos peligrosos se declaran en el manifiesto?

- `android.permission.READ_CONTACTS`
- `android.permission.CAMERA`
- `android.permission.WRITE_EXTERNAL_STORAGE` (si está presente)

Estos permisos se consideran peligrosos porque dan acceso a datos personales o recursos críticos del dispositivo.

### ¿Qué patrón se utiliza para solicitar permisos en tiempo de ejecución?

Se utiliza el patrón moderno de Jetpack:  
**`ActivityResultContracts.RequestPermission`**,  
el cual reemplaza los métodos obsoletos como `onRequestPermissionsResult`. Este patrón es más seguro y desacoplado del ciclo de vida de la actividad.

### ¿Qué configuración de seguridad previene los backups automáticos?

En el archivo `AndroidManifest.xml`, se declara la siguiente línea:

```xml
android:allowBackup="false"

## 🗂 1.3 Gestión de Archivos

### 📤 ¿Cómo se implementa la compartición segura de archivos de imágenes?

Se utiliza un **FileProvider**, una clase de Android que permite compartir archivos entre aplicaciones utilizando **URIs del tipo `content://`**. Esto evita exponer directamente rutas del sistema de archivos, mejorando la seguridad.

La configuración incluye:

- Declarar el FileProvider en el `AndroidManifest.xml`
- Crear el archivo `res/xml/file_paths.xml` con las rutas seguras permitidas
- Obtener una URI segura con `FileProvider.getUriForFile(...)`

---

### 🏷 ¿Qué autoridad se utiliza para el FileProvider?

La autoridad usada en el proyecto es:

com.example.seguridad_priv_a.fileprovider


Este valor debe coincidir en el `AndroidManifest.xml` y en el método `FileProvider.getUriForFile(...)`, asegurando que el sistema sepa qué rutas están autorizadas para compartirse.

---

### ⚠️ ¿Por qué no se deben usar directamente URIs del tipo `file://`?

Las URIs `file://` están **prohibidas desde Android 7.0 (API 24)**. Usarlas provoca una excepción de tipo `FileUriExposedException`, ya que representan un riesgo de seguridad al exponer rutas internas del sistema de archivos.

En su lugar, se deben usar URIs del tipo `content://`, generadas por `FileProvider`, ya que:

- Son controladas por el sistema operativo
- Permiten definir permisos temporales y seguros
- No revelan la estructura interna del almacenamiento

---
