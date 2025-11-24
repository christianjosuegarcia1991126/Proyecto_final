"""
Script para poblar la base de datos usando el endpoint /auth/registro
Este método garantiza compatibilidad total con el login.

BASADO EN: API_DOCS.md - Método de "Crear un cuestionario completo"
FUNCIONAMIENTO: Similar a como se crean preguntas/respuestas, 
                creamos usuarios usando el endpoint correcto.

REQUISITOS:
- El servidor Flask debe estar corriendo (python app.py)
- Tener config.py configurado con tu IP

EJECUCIÓN:
    python poblar_base_datos_FINAL.py
"""

import requests
import time
import sys

# Importar configuración
try:
    from config import API_BASE_URL, IP_Computer, PORT
    print(f"✅ Configuración cargada: {API_BASE_URL}")
except ImportError:
    print("⚠️  ADVERTENCIA: No se encontró config.py")
    print("   Usando configuración por defecto")
    API_BASE_URL = "http://192.168.1.66:5001"
    IP_Computer = "192.168.1.66"
    PORT = 5001

# ========================================
# USUARIOS A CREAR
# ========================================

usuarios = [
    {
        'nombre': 'Juan Pérez',
        'correo': 'juan.perez@gmail.com',
        'password': 'Juan123!'
    },
    {
        'nombre': 'María García',
        'correo': 'maria.garcia@hotmail.com',
        'password': 'Maria456*'
    },
    {
        'nombre': 'Carlos López',
        'correo': 'carlos.lopez@outlook.com',
        'password': 'Carlos789#'
    },
    {
        'nombre': 'Ana Martínez',
        'correo': 'ana.martinez@yahoo.com',
        'password': 'Ana2024$'
    },
    {
        'nombre': 'Luis Rodríguez',
        'correo': 'luis.rodriguez@gmail.com',
        'password': 'Luis555&'
    },
    {
        'nombre': 'Laura Sánchez',
        'correo': 'laura.sanchez@gmail.com',
        'password': 'Laura888@'
    },
    {
        'nombre': 'Pedro Ramírez',
        'correo': 'pedro.ramirez@yahoo.com',
        'password': 'Pedro999!'
    }
]

# ========================================
# FUNCIONES AUXILIARES
# ========================================

def verificar_servidor():
    """Verifica que el servidor Flask esté corriendo"""
    try:
        print(f"🔍 Verificando servidor en {API_BASE_URL}...", end=" ")
        response = requests.get(
            f"{API_BASE_URL}/usuarios", 
            timeout=5
        )
        
        if response.status_code == 200:
            print("✅ Conectado")
            return True
        else:
            print(f"❌ Código {response.status_code}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ No se puede conectar")
        return False
    except requests.exceptions.Timeout:
        print("❌ Timeout")
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def crear_usuario_via_registro(nombre, correo, password):
    """
    Crea un usuario usando el endpoint /auth/registro
    Este es el MISMO método que funciona con curl en API_DOCS.md
    
    Similar a:
    curl -X POST http://127.0.0.1:5001/auth/registro \
      -H "Content-Type: application/json" \
      -d '{"nombre": "Juan Perez", "correo": "juan@gmail.com", "password": "Juan123!"}'
    """
    try:
        response = requests.post(
            f"{API_BASE_URL}/auth/registro",
            json={
                "nombre": nombre,
                "correo": correo,
                "password": password
            },
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        return response.status_code, response.json()
    
    except requests.exceptions.RequestException as e:
        return None, {"error": str(e)}

def mostrar_banner():
    """Muestra banner de inicio"""
    print()
    print("=" * 80)
    print("  POBLACIÓN DE BASE DE DATOS - MÉTODO API")
    print("=" * 80)
    print()
    print("  📋 Método: Endpoint /auth/registro (igual que API_DOCS.md)")
    print("  🔐 Seguridad: Hash compatible con login")
    print("  ✅ Garantía: Los usuarios creados funcionarán con el login")
    print()
    print("=" * 80)
    print()

def verificar_usuarios_actuales():
    """Muestra cuántos usuarios hay actualmente"""
    try:
        response = requests.get(f"{API_BASE_URL}/usuarios", timeout=5)
        if response.status_code == 200:
            usuarios_actuales = response.json()
            print(f"📊 Usuarios actuales en la base de datos: {len(usuarios_actuales)}")
            if len(usuarios_actuales) > 0:
                print(f"   Correos existentes:")
                for u in usuarios_actuales:
                    print(f"   • {u['correo']}")
            print()
            return len(usuarios_actuales)
        return 0
    except:
        return 0

def poblar_base_datos():
    """Función principal para poblar la base de datos"""
    
    mostrar_banner()
    
    # Verificar que el servidor esté corriendo
    if not verificar_servidor():
        print()
        print("❌ ERROR: No se puede conectar al servidor Flask")
        print()
        print("📋 SOLUCIÓN:")
        print("   1. Abre otra terminal/CMD")
        print("   2. Ve a la carpeta del proyecto:")
        print(f"      cd {sys.path[0]}")
        print("   3. Ejecuta el servidor:")
        print("      python app.py")
        print("   4. Espera a ver:")
        print(f"      * Running on http://{IP_Computer}:{PORT}")
        print("   5. Vuelve a ejecutar este script")
        print()
        input("Presiona Enter para salir...")
        return False
    
    print()
    
    # Mostrar usuarios actuales
    usuarios_iniciales = verificar_usuarios_actuales()
    
    # Crear usuarios
    print("=" * 80)
    print("  CREANDO USUARIOS")
    print("=" * 80)
    print()
    
    usuarios_creados = 0
    usuarios_existentes = 0
    errores = 0
    
    for i, usuario in enumerate(usuarios, 1):
        print(f"[{i}/{len(usuarios)}] Creando: {usuario['nombre']} ({usuario['correo']})")
        print(f"        Contraseña: {usuario['password']}")
        print(f"        ", end="")
        
        status_code, response = crear_usuario_via_registro(
            usuario['nombre'],
            usuario['correo'],
            usuario['password']
        )
        
        if status_code == 201:
            print("✅ USUARIO CREADO EXITOSAMENTE")
            usuarios_creados += 1
        elif status_code == 409:
            print("⚠️  EL USUARIO YA EXISTE")
            usuarios_existentes += 1
        elif status_code == 400:
            print(f"❌ ERROR: Datos incompletos o inválidos")
            print(f"        Detalle: {response.get('error', 'Desconocido')}")
            errores += 1
        else:
            print(f"❌ ERROR: {response.get('error', 'Desconocido')}")
            errores += 1
        
        print()
        time.sleep(0.3)  # Pausa breve entre creaciones
    
    # Resumen final
    print("=" * 80)
    print("  RESUMEN DEL PROCESO")
    print("=" * 80)
    print()
    print(f"  📊 Usuarios al inicio:    {usuarios_iniciales}")
    print(f"  ✅ Usuarios creados:      {usuarios_creados}")
    print(f"  ⚠️  Ya existían:           {usuarios_existentes}")
    print(f"  ❌ Errores:               {errores}")
    print(f"  📈 Total ahora:           {usuarios_iniciales + usuarios_creados}")
    print()
    
    # Verificar usuarios finales
    usuarios_finales = verificar_usuarios_actuales()
    
    # Mostrar credenciales
    if usuarios_creados > 0 or usuarios_existentes > 0:
        print("=" * 80)
        print("  📋 CREDENCIALES PARA PROBAR EL LOGIN")
        print("=" * 80)
        print()
        
        for usuario in usuarios:
            print(f"  📧 Correo:     {usuario['correo']}")
            print(f"  🔑 Contraseña: {usuario['password']}")
            print(f"  👤 Nombre:     {usuario['nombre']}")
            print("  " + "-" * 76)
        
        print()
    
    # Mensaje de éxito
    if usuarios_creados > 0:
        print("=" * 80)
        print("  🎉 ¡BASE DE DATOS POBLADA EXITOSAMENTE!")
        print("=" * 80)
        print()
        print("  ✅ Los usuarios creados están listos para usar")
        print("  ✅ Los hashes son compatibles con el login")
        print("  ✅ Puedes probar en la app Android ahora")
        print()
        print(f"  💡 Verifica en: {API_BASE_URL}/usuarios")
        print()
    elif usuarios_existentes == len(usuarios):
        print("=" * 80)
        print("  ℹ️  TODOS LOS USUARIOS YA EXISTÍAN")
        print("=" * 80)
        print()
        print("  ✅ La base de datos ya está poblada")
        print("  ✅ Puedes usar cualquiera de las credenciales de arriba")
        print()
    else:
        print("=" * 80)
        print("  ⚠️  PROCESO COMPLETADO CON ERRORES")
        print("=" * 80)
        print()
        print("  ⚠️  Algunos usuarios no se pudieron crear")
        print("  💡 Revisa los errores de arriba")
        print()
    
    return True

# ========================================
# FUNCIÓN PARA ELIMINAR TODOS LOS USUARIOS
# ========================================

def limpiar_base_datos():
    """Elimina todos los usuarios de la base de datos"""
    print()
    print("=" * 80)
    print("  ⚠️  LIMPIEZA DE BASE DE DATOS")
    print("=" * 80)
    print()
    
    confirmacion = input("¿Estás seguro de eliminar TODOS los usuarios? (escribe 'SI' para confirmar): ")
    
    if confirmacion.strip().upper() != 'SI':
        print("❌ Operación cancelada")
        return
    
    try:
        response = requests.get(f"{API_BASE_URL}/usuarios", timeout=5)
        if response.status_code == 200:
            usuarios_actuales = response.json()
            print(f"\n🗑️  Eliminando {len(usuarios_actuales)} usuarios...")
            print()
            
            eliminados = 0
            for usuario in usuarios_actuales:
                print(f"   Eliminando: {usuario['correo']}...", end=" ")
                try:
                    del_response = requests.delete(
                        f"{API_BASE_URL}/usuarios/{usuario['id']}", 
                        timeout=5
                    )
                    if del_response.status_code == 200:
                        print("✅")
                        eliminados += 1
                    else:
                        print(f"❌ Error {del_response.status_code}")
                except:
                    print("❌ Error de conexión")
            
            print()
            print(f"✅ {eliminados} usuarios eliminados")
            print()
    except Exception as e:
        print(f"❌ Error: {e}")

# ========================================
# MENÚ PRINCIPAL
# ========================================

def menu_principal():
    """Menú interactivo"""
    while True:
        print()
        print("=" * 80)
        print("  POBLACIÓN DE BASE DE DATOS - MENÚ")
        print("=" * 80)
        print()
        print("  1. Poblar base de datos (crear usuarios)")
        print("  2. Ver usuarios actuales")
        print("  3. Limpiar base de datos (eliminar todos)")
        print("  4. Salir")
        print()
        
        opcion = input("Selecciona una opción (1-4): ").strip()
        
        if opcion == "1":
            poblar_base_datos()
            input("\nPresiona Enter para continuar...")
        elif opcion == "2":
            print()
            verificar_usuarios_actuales()
            input("\nPresiona Enter para continuar...")
        elif opcion == "3":
            limpiar_base_datos()
            input("\nPresiona Enter para continuar...")
        elif opcion == "4":
            print("\n👋 ¡Hasta luego!\n")
            break
        else:
            print("\n❌ Opción inválida")

# ========================================
# EJECUCIÓN
# ========================================

if __name__ == '__main__':
    try:
        # Verificar si se pasa argumento para ejecución directa
        if len(sys.argv) > 1:
            if sys.argv[1] == "--auto":
                poblar_base_datos()
            elif sys.argv[1] == "--clean":
                limpiar_base_datos()
            else:
                print("Argumentos disponibles:")
                print("  --auto   : Poblar automáticamente")
                print("  --clean  : Limpiar base de datos")
        else:
            # Modo interactivo
            menu_principal()
    except KeyboardInterrupt:
        print("\n\n👋 Proceso interrumpido por el usuario\n")
    except Exception as e:
        print(f"\n❌ Error inesperado: {e}\n")