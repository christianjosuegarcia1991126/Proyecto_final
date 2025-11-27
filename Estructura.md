# **Estructura de Base de Datos Android**

Este documento detalla el esquema de la base de datos SQLite utilizada en la aplicación (basecorreos.db).

## **Tabla: usuario**

Esta tabla almacena la información de credenciales y estado de seguridad de los usuarios registrados en la aplicación.

### **Esquema de Columnas**

| Columna | Tipo de Dato | Restricciones (Constraints) | Descripción |
| :---- | :---- | :---- | :---- |
| id | INTEGER | PRIMARY KEY, AUTOINCREMENT | Identificador único del usuario. |
| nombre | VARCHAR(100) | NOT NULL | Nombre completo del usuario. |
| correo | VARCHAR(120) | UNIQUE, NOT NULL | Dirección de correo electrónico. Se utiliza como identificador de acceso. |
| password\_hash | VARCHAR(255) | NOT NULL | Hash de la contraseña (PBKDF2/SHA256). No almacena texto plano. |
| intentos\_fallidos | INTEGER | DEFAULT 0 | Contador para controlar intentos de acceso erróneos (seguridad activa). |
| bloqueado\_hasta | DATETIME | NULLABLE | Marca de tiempo que indica hasta cuándo la cuenta está bloqueada temporalmente. |
| fecha\_creacion | DATETIME | DEFAULT CURRENT\_TIMESTAMP | Fecha y hora de registro del usuario (UTC). |

### **Sentencia SQL de Creación**

CREATE TABLE usuario (  
    id INTEGER PRIMARY KEY AUTOINCREMENT,  
    nombre VARCHAR(100) NOT NULL,  
    correo VARCHAR(120) UNIQUE NOT NULL,  
    password\_hash VARCHAR(255) NOT NULL,  
    intentos\_fallidos INTEGER DEFAULT 0,  
    bloqueado\_hasta DATETIME,  
    fecha\_creacion DATETIME DEFAULT CURRENT\_TIMESTAMP  
);

### **Notas de Implementación**

* **Seguridad:** El campo password\_hash está diseñado para almacenar cadenas generadas por algoritmos seguros (actualmente *PBKDF2-SHA256*).  
* **Bloqueo:** La lógica de la aplicación debe comparar la fecha actual con bloqueado\_hasta para permitir o denegar el acceso.