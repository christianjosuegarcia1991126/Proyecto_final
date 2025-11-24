from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta

app = Flask(__name__)

# 🌐 Configuración CORS
CORS(app, origins="*", methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"])

# 📊 Configuración de base de datos
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///basecorreos.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# 🧠 Modelos
class Usuario(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False)
    correo = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    intentos_fallidos = db.Column(db.Integer, default=0)
    bloqueado_hasta = db.Column(db.DateTime, nullable=True)
    fecha_creacion = db.Column(db.DateTime, default=datetime.utcnow)
    
    def verificar_password(self, password):
        return check_password_hash(self.password_hash, password)
    
    def esta_bloqueado(self):
        if self.bloqueado_hasta:
            return datetime.utcnow() < self.bloqueado_hasta
        return False

# 🔐 Rutas de Autenticación
@app.route('/auth/login', methods=['POST'])
def login():
    try:
        data = request.json
        if not data or 'correo' not in data or 'password' not in data:
            return jsonify({'error': 'Correo y contraseña requeridos'}), 400
        
        correo = data['correo'].lower().strip()
        password = data['password']
        
        # Buscar usuario
        usuario = Usuario.query.filter_by(correo=correo).first()
        
        if not usuario:
            return jsonify({'error': 'Correo inválido'}), 401
        
        # Verificar si está bloqueado
        if usuario.esta_bloqueado():
            tiempo_restante = (usuario.bloqueado_hasta - datetime.utcnow()).seconds // 60
            return jsonify({
                'error': 'Usuario bloqueado',
                'mensaje': f'Intente más tarde. Tiempo restante: {tiempo_restante} minutos'
            }), 403
        
        # Verificar contraseña
        if usuario.verificar_password(password):
            # Login exitoso - resetear intentos
            usuario.intentos_fallidos = 0
            usuario.bloqueado_hasta = None
            db.session.commit()
            
            return jsonify({
                'mensaje': 'Login exitoso',
                'usuario': {
                    'id': usuario.id,
                    'nombre': usuario.nombre,
                    'correo': usuario.correo
                }
            }), 200
        else:
            # Contraseña incorrecta
            usuario.intentos_fallidos += 1
            
            if usuario.intentos_fallidos >= 3:
                # Bloquear por 15 minutos
                usuario.bloqueado_hasta = datetime.utcnow() + timedelta(minutes=15)
                db.session.commit()
                return jsonify({
                    'error': 'Usuario bloqueado',
                    'mensaje': 'Demasiados intentos fallidos. Intente en 15 minutos'
                }), 403
            
            db.session.commit()
            intentos_restantes = 3 - usuario.intentos_fallidos
            return jsonify({
                'error': 'Contraseña incorrecta',
                'intentos_restantes': intentos_restantes
            }), 401
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/auth/registro', methods=['POST'])
def registro():
    try:
        data = request.json
        if not data or 'nombre' not in data or 'correo' not in data or 'password' not in data:
            return jsonify({'error': 'Datos incompletos'}), 400
        
        correo = data['correo'].lower().strip()
        
        # Verificar si el usuario ya existe
        if Usuario.query.filter_by(correo=correo).first():
            return jsonify({'error': 'El correo ya está registrado'}), 409
        
        # Crear nuevo usuario
        nuevo_usuario = Usuario(
            nombre=data['nombre'],
            correo=correo,
            password_hash = generate_password_hash(
    data['password'],
    method='pbkdf2:sha256',
    salt_length=16
)
        )
        
        db.session.add(nuevo_usuario)
        db.session.commit()
        
        return jsonify({
            'mensaje': 'Usuario registrado exitosamente',
            'usuario': {
                'id': nuevo_usuario.id,
                'nombre': nuevo_usuario.nombre,
                'correo': nuevo_usuario.correo
            }
        }), 201
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/usuarios', methods=['GET'])
def obtener_usuarios():
    try:
        usuarios = Usuario.query.all()
        resultado = []
        for usuario in usuarios:
            resultado.append({
                'id': usuario.id,
                'nombre': usuario.nombre,
                'correo': usuario.correo,
                'fecha_creacion': usuario.fecha_creacion.isoformat()
            })
        return jsonify(resultado), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/usuarios/<int:id>', methods=['GET'])
def obtener_usuario(id):
    usuario = Usuario.query.get(id)
    if usuario:
        return jsonify({
            'id': usuario.id,
            'nombre': usuario.nombre,
            'correo': usuario.correo,
            'fecha_creacion': usuario.fecha_creacion.isoformat()
        })
    return jsonify({'error': 'Usuario no encontrado'}), 404

@app.route('/usuarios/<int:id>', methods=['DELETE'])
def eliminar_usuario(id):
    usuario = Usuario.query.get(id)
    if usuario:
        db.session.delete(usuario)
        db.session.commit()
        return jsonify({'mensaje': 'Usuario eliminado'})
    return jsonify({'error': 'Usuario no encontrado'}), 404

# 🔧 Middleware adicional
@app.after_request
def after_request(response):
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization')
    response.headers.add('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS')
    response.headers.add('Content-Type', 'application/json; charset=utf-8')
    return response

# 🟢 Inicializar base de datos
def crear_tablas():
    with app.app_context():
        db.create_all()

if __name__ == '__main__':
    crear_tablas()
    app.run(debug=False, port=5001, host='0.0.0.0')