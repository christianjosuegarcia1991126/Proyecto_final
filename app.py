from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta

try:
    from config import IP_Computer, PORT, DB_NAME, PASSWORD_HASH_METHOD
except ImportError:
    print("❌ ERROR: No se encontró config.py. Por favor, créalo a partir de config.example.py")
    exit(1)

app = Flask(__name__)
CORS(app)
app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{DB_NAME}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# --- Modelos de Base de Datos ---
class Usuario(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False)
    correo = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    intentos_fallidos = db.Column(db.Integer, default=0)
    bloqueado_hasta = db.Column(db.DateTime, nullable=True)
    
    def verificar_password(self, password):
        return check_password_hash(self.password_hash, password)
    
    def esta_bloqueado(self):
        return self.bloqueado_hasta and datetime.utcnow() < self.bloqueado_hasta

class Contacto(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), nullable=False)
    nombre = db.Column(db.String(100), nullable=False)
    telefono = db.Column(db.String(20))
    empresa = db.Column(db.String(100))
    usuario = db.relationship('Usuario', backref=db.backref('contactos', lazy=True, cascade="all, delete-orphan"))

class Recordatorio(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), nullable=False)
    contacto_id = db.Column(db.Integer, db.ForeignKey('contacto.id'), nullable=False)
    nombre = db.Column(db.String(200), nullable=False)
    requisiciones = db.Column(db.Text)
    fecha = db.Column(db.String(10), nullable=False) # YYYY-MM-DD
    hora = db.Column(db.String(5), nullable=False)   # HH:MM
    usuario = db.relationship('Usuario', backref=db.backref('recordatorios', lazy=True, cascade="all, delete-orphan"))
    contacto = db.relationship('Contacto', backref=db.backref('recordatorios', lazy=True, cascade="all, delete-orphan"))

# --- Rutas ---
@app.route('/auth/login', methods=['POST'])
def login():
    data = request.json
    correo = data.get('correo', '').lower().strip()
    password = data.get('password', '')

    # Validar que se enviaron los datos
    if not correo or not password:
        return jsonify({
            'success': False,
            'mensaje': 'Correo y contraseña son requeridos'
        }), 400

    # Buscar usuario por correo
    usuario = Usuario.query.filter_by(correo=correo).first()

    # Si el usuario no existe
    if not usuario:
        return jsonify({
            'success': False,
            'mensaje': 'Correo no registrado'
        }), 401

    # Verificar si el usuario está bloqueado
    if usuario.esta_bloqueado():
        tiempo_restante = (usuario.bloqueado_hasta - datetime.utcnow()).total_seconds()
        minutos = int(tiempo_restante // 60)
        segundos = int(tiempo_restante % 60)

        return jsonify({
            'success': False,
            'mensaje': f'Cuenta bloqueada. Intenta de nuevo en {minutos}m {segundos}s',
            'bloqueado': True,
            'bloqueado_hasta': usuario.bloqueado_hasta.isoformat()
        }), 403

    # Verificar contraseña
    if not usuario.verificar_password(password):
        # Incrementar intentos fallidos
        usuario.intentos_fallidos += 1
        intentos_restantes = 3 - usuario.intentos_fallidos

        # Si alcanzó 3 intentos, bloquear cuenta
        if usuario.intentos_fallidos >= 3:
            # Bloquear por 15 segundos
            usuario.bloqueado_hasta = datetime.utcnow() + timedelta(seconds=15)  # 15 segundos

            usuario.intentos_fallidos = 0  # Resetear para el próximo ciclo
            db.session.commit()

            return jsonify({
                'success': False,
                'mensaje': 'Contraseña incorrecta. Cuenta bloqueada por 15 minutos',
                'bloqueado': True,
                'intentosRestantes': 0
            }), 403

        # Guardar intentos fallidos
        db.session.commit()

        return jsonify({
            'success': False,
            'mensaje': f'Contraseña incorrecta',
            'intentosRestantes': intentos_restantes,
            'bloqueado': False
        }), 401

    # ✅ Login exitoso - Resetear intentos fallidos
    usuario.intentos_fallidos = 0
    usuario.bloqueado_hasta = None
    db.session.commit()

    return jsonify({
        'success': True,
        'mensaje': 'Login exitoso',
        'usuario': {
            'id': usuario.id,
            'nombre': usuario.nombre,
            'correo': usuario.correo
        }
    }), 200


@app.route('/contactos', methods=['GET'])
def obtener_contactos():
    usuario_id = request.args.get('usuario_id')
    contactos = Contacto.query.filter_by(usuario_id=usuario_id).all()
    return jsonify([{'id': c.id, 'nombre': c.nombre, 'telefono': c.telefono, 'empresa': c.empresa} for c in contactos]), 200

@app.route('/contactos', methods=['POST'])
def crear_contacto():
    data = request.json
    nuevo = Contacto(usuario_id=data['usuario_id'], nombre=data['nombre'], telefono=data.get('telefono'), empresa=data.get('empresa'))
    db.session.add(nuevo)
    db.session.commit()
    return jsonify({'mensaje': 'Contacto creado'}), 201

@app.route('/contactos/<int:id>', methods=['DELETE'])
def eliminar_contacto(id):
    contacto = Contacto.query.get_or_404(id)
    db.session.delete(contacto)
    db.session.commit()
    return jsonify({'mensaje': 'Contacto eliminado'}), 200

# --- RUTAS DE RECORDATORIOS (CORREGIDAS Y COMPLETAS) ---
@app.route('/recordatorios', methods=['GET'])
def obtener_recordatorios():
    usuario_id = request.args.get('usuario_id')
    # Hacemos un JOIN para obtener el nombre del contacto
    recordatorios = db.session.query(Recordatorio, Contacto.nombre).join(Contacto).filter(Recordatorio.usuario_id == usuario_id).all()
    resultado = [
        {
            'id': r.id, 
            'nombre': r.nombre, 
            'fecha': r.fecha, 
            'hora': r.hora, 
            'requisiciones': r.requisiciones,
            'contacto_nombre': contacto_nombre
        } for r, contacto_nombre in recordatorios
    ]
    return jsonify(resultado), 200

@app.route('/recordatorios', methods=['POST'])
def crear_recordatorio():
    data = request.json
    nuevo = Recordatorio(
        usuario_id=data['usuario_id'],
        contacto_id=data['contacto_id'],
        nombre=data['nombre'],
        requisiciones=data.get('requisiciones'),
        fecha=data['fecha'],
        hora=data['hora']
    )
    db.session.add(nuevo)
    db.session.commit()
    # Devolvemos el objeto completo con su ID
    return jsonify({
        'id': nuevo.id,
        'nombre': nuevo.nombre,
        'fecha': nuevo.fecha,
        'hora': nuevo.hora,
        'requisiciones': nuevo.requisiciones,
        'contacto_nombre': nuevo.contacto.nombre # Obtenemos el nombre del contacto relacionado
    }), 201

@app.route('/recordatorios/<int:id>', methods=['DELETE'])
def eliminar_recordatorio(id):
    recordatorio = Recordatorio.query.get_or_404(id)
    db.session.delete(recordatorio)
    db.session.commit()
    return jsonify({'mensaje': 'Recordatorio eliminado'}), 200

# --- Inicialización ---
if __name__ == '__main__':
    with app.app_context():
        db.create_all()
    print(f"🚀 Servidor iniciando en http://{IP_Computer}:{PORT}")
    app.run(debug=True, port=PORT, host='0.0.0.0')
