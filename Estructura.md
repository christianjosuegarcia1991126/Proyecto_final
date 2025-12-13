# **Estructura de Base de Datos - Aplicación Android**

Este documento detalla el esquema completo de la base de datos utilizada en la aplicación de gestión de recordatorios y contactos.

---

## **📊 Diagrama de Relaciones**

```
┌─────────────┐
│   usuario   │
└──────┬──────┘
       │
       │ 1:N
       ├──────────────┐
       │              │
       ▼              ▼
┌─────────────┐  ┌──────────────┐
│  contacto   │  │ recordatorio │
└──────┬──────┘  └──────┬───────┘
       │                │
       └────────┬───────┘
                │ N:1
                ▼
         (relación FK)
```

---

## **🗂️ Tabla: usuario**

Almacena la información de credenciales y estado de seguridad de los usuarios registrados.

### **Esquema de Columnas**

| Columna | Tipo de Dato | Restricciones | Descripción |
|---------|--------------|---------------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT | Identificador único del usuario |
| `nombre` | VARCHAR(100) | NOT NULL | Nombre completo del usuario |
| `correo` | VARCHAR(120) | UNIQUE, NOT NULL | Email usado como identificador de acceso |
| `password_hash` | VARCHAR(255) | NOT NULL | Hash de la contraseña (PBKDF2/SHA256) |
| `intentos_fallidos` | INTEGER | DEFAULT 0 | Contador de intentos de login erróneos |
| `bloqueado_hasta` | DATETIME | NULLABLE | Timestamp hasta cuando la cuenta está bloqueada |
| `fecha_creacion` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Fecha y hora de registro (UTC) |

### **Sentencia SQL de Creación**

```sql
CREATE TABLE usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    intentos_fallidos INTEGER DEFAULT 0,
    bloqueado_hasta DATETIME,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### **Lógica de Seguridad**

- **Máximo de intentos:** 3 intentos fallidos consecutivos
- **Bloqueo temporal:** 15 segundos después del tercer intento fallido
- **Reinicio de contador:** Se resetea `intentos_fallidos` a 0 después de un login exitoso
- **Hash de contraseña:** Utiliza PBKDF2-SHA256 con salt único por usuario

### **Ejemplo de Registro**

```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "correo": "juan.perez@gmail.com",
  "password_hash": "$pbkdf2-sha256$29000$...",
  "intentos_fallidos": 0,
  "bloqueado_hasta": null,
  "fecha_creacion": "2025-12-07 18:30:00"
}
```

---

## **👥 Tabla: contacto**

Almacena los contactos de cada usuario para asociarlos con recordatorios.

### **Esquema de Columnas**

| Columna | Tipo de Dato | Restricciones | Descripción |
|---------|--------------|---------------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT | Identificador único del contacto |
| `nombre` | VARCHAR(100) | NOT NULL | Nombre del contacto |
| `telefono` | VARCHAR(20) | NULLABLE | Número telefónico (formato libre) |
| `empresa` | VARCHAR(100) | NULLABLE | Empresa u organización del contacto |
| `usuario_id` | INTEGER | NOT NULL, FOREIGN KEY | Referencia al usuario propietario |
| `fecha_creacion` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Fecha de creación del contacto |

### **Sentencia SQL de Creación**

```sql
CREATE TABLE contacto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    empresa VARCHAR(100),
    usuario_id INTEGER NOT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);
```

### **Índices Recomendados**

```sql
CREATE INDEX idx_contacto_usuario ON contacto(usuario_id);
```

### **Ejemplo de Registro**

```json
{
  "id": 5,
  "nombre": "Christian López",
  "telefono": "+52 55 1234 5678",
  "empresa": "Tech Solutions SA",
  "usuario_id": 1,
  "fecha_creacion": "2025-12-07 19:00:00"
}
```

---

## **📅 Tabla: recordatorio**

Almacena los recordatorios programados asociados a contactos.

### **Esquema de Columnas**

| Columna | Tipo de Dato | Restricciones | Descripción |
|---------|--------------|---------------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT | Identificador único del recordatorio |
| `nombre` | VARCHAR(150) | NOT NULL | Título o nombre del recordatorio |
| `fecha` | DATE | NOT NULL | Fecha del recordatorio (YYYY-MM-DD) |
| `hora` | TIME | NOT NULL | Hora del recordatorio (HH:MM) |
| `requisiciones` | TEXT | NULLABLE | Notas o descripción adicional |
| `contacto_id` | INTEGER | NOT NULL, FOREIGN KEY | Referencia al contacto asociado |
| `usuario_id` | INTEGER | NOT NULL, FOREIGN KEY | Referencia al usuario propietario |
| `notificado` | BOOLEAN | DEFAULT 0 | Indica si ya se envió la notificación |
| `fecha_creacion` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Fecha de creación del recordatorio |

### **Sentencia SQL de Creación**

```sql
CREATE TABLE recordatorio (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    requisiciones TEXT,
    contacto_id INTEGER NOT NULL,
    usuario_id INTEGER NOT NULL,
    notificado BOOLEAN DEFAULT 0,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contacto_id) REFERENCES contacto(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);
```

### **Índices Recomendados**

```sql
CREATE INDEX idx_recordatorio_usuario ON recordatorio(usuario_id);
CREATE INDEX idx_recordatorio_contacto ON recordatorio(contacto_id);
CREATE INDEX idx_recordatorio_fecha_hora ON recordatorio(fecha, hora);
```

### **Ejemplo de Registro**

```json
{
  "id": 2,
  "nombre": "Reunión de proyecto",
  "fecha": "2025-12-08",
  "hora": "14:30",
  "requisiciones": "Revisar presupuesto y timeline",
  "contacto_id": 5,
  "usuario_id": 1,
  "notificado": false,
  "fecha_creacion": "2025-12-07 20:00:00"
}
```

---

## **🔄 Funcionamiento de la Aplicación**

### **1. Sistema de Autenticación**

#### **Flujo de Login:**
1. Usuario ingresa `correo` y `password`
2. La aplicación consulta la tabla `usuario` por el correo
3. Verifica si `bloqueado_hasta` es NULL o ya expiró
4. Compara el hash de la password ingresada con `password_hash`
5. **Si es correcto:**
    - Resetea `intentos_fallidos` a 0
    - Navega a `HomeActivity`
6. **Si es incorrecto:**
    - Incrementa `intentos_fallidos`
    - Si llega a 3, establece `bloqueado_hasta` = ahora + 15 segundos
    - Muestra intentos restantes

#### **Respuestas del Backend:**

**Login exitoso:**
```json
{
  "mensaje": "Login exitoso",
  "usuario": {
    "id": 1,
    "nombre": "Juan Pérez",
    "correo": "juan.perez@gmail.com"
  }
}
```

**Login fallido:**
```json
{
  "success": false,
  "mensaje": "Contraseña incorrecta",
  "intentosRestantes": 2,
  "bloqueado": false
}
```

**Usuario bloqueado:**
```json
{
  "success": false,
  "mensaje": "Cuenta bloqueada por 15 minutos",
  "bloqueado": true
}
```

---

### **2. Gestión de Contactos**

#### **Operaciones disponibles:**

**Listar contactos del usuario:**
```
GET /contactos?usuario_id=1
```

**Crear nuevo contacto:**
```
POST /contactos
Body: {
  "nombre": "María García",
  "telefono": "+52 55 9876 5432",
  "empresa": "Consulting Group",
  "usuario_id": 1
}
```

**Eliminar contacto:**
```
DELETE /contactos/{id}
```

#### **Validaciones:**
- Nombre es obligatorio (no vacío)
- Teléfono y empresa son opcionales
- Un contacto solo puede ser accedido por su usuario propietario

---

### **3. Sistema de Recordatorios**

#### **Flujo completo:**

1. **Creación del recordatorio:**
    - Usuario selecciona un contacto existente
    - Ingresa: nombre, fecha, hora, requisiciones (opcional)
    - La app valida que fecha/hora sean futuras
    - Se crea el registro en la BD

2. **Programación de alarma:**
    - `NotificationHelper.scheduleNotification()` programa una alarma exacta
    - Se usa `AlarmManager.setExactAndAllowWhileIdle()`
    - Se crea un `PendingIntent` hacia `NotificationReceiver`

3. **Ejecución de la alarma:**
    - Cuando llega la hora, Android despierta `NotificationReceiver`
    - Se obtienen los datos del recordatorio
    - Se construye y muestra la notificación
    - (Opcional) Se marca `notificado = 1` en la BD

4. **Visualización:**
    - La app muestra lista de recordatorios con:
        - Título del recordatorio
        - Fecha y hora
        - Nombre del contacto asociado
        - Notas/requisiciones

#### **Operaciones de la API:**

**Listar recordatorios:**
```
GET /recordatorios?usuario_id=1
Response: [
  {
    "id": 2,
    "nombre": "Reunión",
    "fecha": "2025-12-08",
    "hora": "14:30",
    "requisiciones": "Notas importantes",
    "contacto_nombre": "Christian López"
  }
]
```

**Crear recordatorio:**
```
POST /recordatorios
Body: {
  "nombre": "Llamada importante",
  "contacto_id": 5,
  "requisiciones": "Confirmar detalles",
  "fecha": "2025-12-10",
  "hora": "09:00",
  "usuario_id": 1
}
Response: {
  "id": 3,
  "nombre": "Llamada importante",
  "fecha": "2025-12-10",
  "hora": "09:00",
  "requisiciones": "Confirmar detalles",
  "contacto_nombre": "Christian López"
}
```

**Eliminar recordatorio:**
```
DELETE /recordatorios/{id}
```

---

## **🔔 Sistema de Notificaciones**

### **Componentes:**

1. **`NotificationHelper`:** Gestiona canales y programación de alarmas
2. **`NotificationReceiver`:** BroadcastReceiver que procesa alarmas
3. **`AlarmManager`:** Sistema de Android para alarmas exactas

### **Flujo técnico:**

```
Usuario crea recordatorio
    ↓
scheduleNotification() programa alarma
    ↓
AlarmManager.setExactAndAllowWhileIdle()
    ↓
Cuando llega la hora...
    ↓
NotificationReceiver.onReceive()
    ↓
mostrarNotificacion()
    ↓
Usuario ve notificación push
```

### **Permisos requeridos (AndroidManifest.xml):**

```xml
<!-- Notificaciones básicas -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Alarmas exactas (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Vibración -->
<uses-permission android:name="android.permission.VIBRATE" />
```

### **Configuración del Receiver:**

```xml
<receiver
    android:name=".loginapp.notifications.NotificationReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.example.proyectofinal.NOTIFICATION_ACTION" />
    </intent-filter>
</receiver>
```

---

## **🔐 Consideraciones de Seguridad**

### **Contraseñas:**
- Nunca se almacenan en texto plano
- Se usa PBKDF2-SHA256 con 29,000 iteraciones
- Cada usuario tiene un salt único

### **API REST:**
- Validación de `usuario_id` en todas las operaciones
- Un usuario solo puede ver/modificar sus propios datos
- Protección contra inyección SQL mediante queries parametrizadas

### **Control de Acceso:**
- Bloqueo automático después de 3 intentos fallidos
- Timeout de 15 segundos
- Registro de `intentos_fallidos` para auditoría

---

## **📱 Tecnologías Utilizadas**

### **Frontend (Android):**
- **Lenguaje:** Kotlin
- **UI:** View Binding + Material Design 3
- **Networking:** OkHttp + Gson
- **Notificaciones:** NotificationCompat + AlarmManager

### **Backend:**
- **Framework:** Flask (Python) / Node.js
- **Base de datos:** SQLite
- **Hashing:** PBKDF2-SHA256

---

## **🚀 Endpoints de la API**

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/auth/login` | Login de usuario | No |
| GET | `/contactos` | Listar contactos | Usuario ID |
| POST | `/contactos` | Crear contacto | Usuario ID |
| DELETE | `/contactos/{id}` | Eliminar contacto | Usuario ID |
| GET | `/recordatorios` | Listar recordatorios | Usuario ID |
| POST | `/recordatorios` | Crear recordatorio | Usuario ID |
| DELETE | `/recordatorios/{id}` | Eliminar recordatorio | Usuario ID |

---

## **📝 Notas de Implementación**

### **Integridad referencial:**
- Las relaciones usan `ON DELETE CASCADE`
- Eliminar un usuario borra automáticamente sus contactos y recordatorios
- Eliminar un contacto borra sus recordatorios asociados

### **Optimización:**
- Se recomienda crear índices en columnas de búsqueda frecuente
- Las consultas filtran por `usuario_id` para segmentar datos

### **Manejo de zonas horarias:**
- Todas las fechas en BD están en UTC
- La app convierte a zona horaria local al mostrar
- Las alarmas usan `System.currentTimeMillis()` (epoch en milisegundos)

---

## **🔄 Versión de la Base de Datos**

**Versión actual:** 1.0  
**Última actualización:** Diciembre 2025  
**Compatibilidad:** Android 8.0+ (API 26+)