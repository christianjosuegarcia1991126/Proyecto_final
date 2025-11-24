"""
Archivo de configuración de ejemplo
📋 INSTRUCCIONES PARA CONFIGURAR:

1. Copia este archivo y renómbralo a: config.py
2. Edita config.py y cambia IP_Computer por tu IP local
3. Para obtener tu IP:
   - Windows: abre CMD y escribe: ipconfig
   - Mac: abre Terminal y escribe: ipconfig getifaddr en0
   - Linux: abre Terminal y escribe: hostname -I
4. Guarda los cambios en config.py

⚠️ IMPORTANTE: 
- NO modifiques este archivo (config.example.py)
- Solo modifica tu copia (config.py)
- config.py NO se subirá a GitHub (está en .gitignore)
"""

# ========================================
# CONFIGURACIÓN DE RED
# ========================================

# Tu IP local (CAMBIAR por tu IP real)
IP_Computer = "192.168.1.XXX"  # Ejemplo: "192.168.1.100"

# Puerto del servidor
PORT = 5001

# URL completa de la API
API_BASE_URL = f"http://{IP_Computer}:{PORT}"

# ========================================
# CONFIGURACIÓN DE SEGURIDAD
# ========================================

# Método de hash para contraseñas
PASSWORD_HASH_METHOD = 'pbkdf2:sha256'
PASSWORD_SALT_LENGTH = 16

# ========================================
# CONFIGURACIÓN DE BASE DE DATOS
# ========================================

# Nombre de la base de datos
DB_NAME = 'basecorreos.db'