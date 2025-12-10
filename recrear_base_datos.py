"""
Script para recrear todas las tablas de la base de datos
Ejecutar: python recrear_base_datos.py
"""

import sqlite3
from datetime import datetime
from werkzeug.security import generate_password_hash

# Configuración
DB_NAME = 'basecorreos.db'
PASSWORD_HASH_METHOD = 'pbkdf2:sha256'
PASSWORD_SALT_LENGTH = 16

def recrear_tablas():
    print("=" * 70)
    print("  RECREANDO BASE DE DATOS")
    print("=" * 70)
    print()
    
    # Conectar a la base de datos
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    
    # Eliminar tablas si existen
    print("🗑️  Eliminando tablas antiguas...")
    cursor.execute('DROP TABLE IF EXISTS recordatorios')
    cursor.execute('DROP TABLE IF EXISTS contactos')
    cursor.execute('DROP TABLE IF EXISTS usuario')
    print("✅ Tablas eliminadas")
    print()
    
    # Crear tabla usuario
    print("📝 Creando tabla 'usuario'...")
    cursor.execute('''
        CREATE TABLE usuario (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre VARCHAR(100) NOT NULL,
            correo VARCHAR(120) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            intentos_fallidos INTEGER DEFAULT 0,
            bloqueado_hasta DATETIME,
            fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    print("✅ Tabla 'usuario' creada")
    
    # Crear tabla contactos
    print("📝 Creando tabla 'contactos'...")
    cursor.execute('''
        CREATE TABLE contactos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre VARCHAR(100) NOT NULL,
            telefono_hash VARCHAR(255) NOT NULL,
            empresa VARCHAR(100),
            usuario_id INTEGER NOT NULL,
            fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
        )
    ''')
    print("✅ Tabla 'contactos' creada")
    
    # Crear tabla recordatorios
    print("📝 Creando tabla 'recordatorios'...")
    cursor.execute('''
        CREATE TABLE recordatorios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre VARCHAR(100) NOT NULL,
            contacto_id INTEGER NOT NULL,
            requisiciones TEXT,
            fecha DATE NOT NULL,
            hora TIME NOT NULL,
            notificado BOOLEAN DEFAULT 0,
            usuario_id INTEGER NOT NULL,
            fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (contacto_id) REFERENCES contactos(id) ON DELETE CASCADE,
            FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
        )
    ''')
    print("✅ Tabla 'recordatorios' creada")
    print()
    
    # Crear usuario de prueba
    print("👤 Creando usuario de prueba...")
    password_hash = generate_password_hash(
        'Juan123!',
        method=PASSWORD_HASH_METHOD,
        salt_length=PASSWORD_SALT_LENGTH
    )
    
    cursor.execute('''
        INSERT INTO usuario (nombre, correo, password_hash, intentos_fallidos, fecha_creacion)
        VALUES (?, ?, ?, 0, ?)
    ''', ('Juan Pérez', 'juan.perez@gmail.com', password_hash, datetime.utcnow()))
    
    print("✅ Usuario creado: juan.perez@gmail.com / Juan123!")
    print()
    
    # Confirmar cambios
    conn.commit()
    conn.close()
    
    print("=" * 70)
    print("  ✅ BASE DE DATOS RECREADA EXITOSAMENTE")
    print("=" * 70)
    print()
    print("Tablas creadas:")
    print("  1. usuario")
    print("  2. contactos")
    print("  3. recordatorios")
    print()
    print("Usuario de prueba:")
    print("  📧 Correo: juan.perez@gmail.com")
    print("  🔑 Contraseña: Juan123!")
    print()
    print("Ahora ejecuta: python app.py")
    print()

if __name__ == '__main__':
    try:
        recrear_tablas()
    except Exception as e:
        print(f"\n❌ Error: {e}\n")