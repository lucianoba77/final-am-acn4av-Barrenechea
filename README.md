# Control de Medicamentos - Aplicación Android

## Descripción General del Proyecto

**Control de Medicamentos** es una aplicación móvil desarrollada para Android que permite a los usuarios gestionar de manera eficiente sus medicamentos y tratamientos médicos. La aplicación ofrece un sistema completo de recordatorios, seguimiento de tomas, gestión de stock y sincronización con Google Calendar.

La aplicación está diseñada para ayudar a los usuarios a mantener la adherencia a sus tratamientos médicos mediante notificaciones programadas, visualización clara de horarios de toma, y seguimiento del historial de medicamentos consumidos.

## Funcionalidades Principales

### Autenticación y Seguridad
- **Autenticación con Email/Contraseña**: Registro e inicio de sesión mediante Firebase Authentication
- **Autenticación con Google**: Inicio de sesión rápido y seguro usando Google Sign-In
- **Recuperación de Contraseña**: Sistema de recuperación mediante email
- **Seguridad de Datos**: Todos los datos se almacenan de forma segura en Firebase Firestore con reglas de seguridad que garantizan que cada usuario solo puede acceder a sus propios datos

### Gestión de Medicamentos
- **Creación de Medicamentos**: Registro completo de medicamentos con información detallada (nombre, presentación, afección, stock, horarios)
- **Edición de Medicamentos**: Modificación de información de medicamentos existentes
- **Eliminación de Medicamentos**: Eliminación segura de medicamentos del sistema
- **Gestión de Stock**: Control de stock inicial y actual, con alertas cuando el stock está bajo
- **Colores Personalizados**: Asignación automática de colores únicos para facilitar la identificación visual

### Recordatorios y Notificaciones
- **Alarmas Programadas**: Sistema de alarmas que programa recordatorios para los próximos 30 días
- **Notificaciones Push**: Notificaciones locales que alertan al usuario sobre las tomas programadas
- **Reprogramación Automática**: Las alarmas se reprograman automáticamente después de reiniciar el dispositivo
- **Configuración de Notificaciones**: Control de volumen, vibración y sonido de las notificaciones

### Sincronización con Google Calendar
- **Integración OAuth 2.0**: Autenticación segura con Google Calendar mediante flujo OAuth implícito
- **Creación Automática de Eventos**: Los eventos de toma de medicamentos se crean automáticamente en Google Calendar
- **Actualización de Eventos**: Los eventos se actualizan cuando se modifican los horarios de los medicamentos
- **Eliminación de Eventos**: Los eventos se eliminan cuando se elimina un medicamento
- **Recordatorios en Calendar**: Los eventos incluyen recordatorios 15 y 5 minutos antes de cada toma

## Descripción de Activities

### LoginActivity
**Propósito**: Pantalla inicial de autenticación del usuario.

**Funcionalidades**:
- Inicio de sesión con email y contraseña
- Registro de nuevos usuarios
- Inicio de sesión con Google (Google Sign-In)
- Recuperación de contraseña mediante email
- Validación de campos de entrada
- Verificación de conexión a internet antes de realizar operaciones
- Redirección automática a MainActivity si el usuario ya está autenticado

**Interacciones**:
- El usuario ingresa sus credenciales y presiona "Iniciar Sesión" o "Registrarse"
- Para Google Sign-In, se abre el selector de cuenta de Google
- El botón "¿Olvidaste tu contraseña?" abre un diálogo para ingresar el email y recibir el enlace de recuperación
- Tras autenticación exitosa, se redirige a MainActivity

### MainActivity
**Propósito**: Pantalla principal (dashboard) que muestra los medicamentos activos del día.

**Funcionalidades**:
- Visualización de medicamentos activos con tomas diarias programadas
- Lista ordenada por horario más próximo
- Indicadores visuales de estado (colores personalizados, barras de progreso)
- Alertas de stock bajo
- Navegación a otras secciones de la aplicación
- Actualización en tiempo real mediante listeners de Firestore
- Filtrado automático: solo muestra medicamentos activos con horarios configurados

**Interacciones**:
- Al hacer clic en un medicamento, se puede ver más información (funcionalidad pendiente)
- Los botones de navegación en la parte inferior permiten cambiar entre secciones
- La lista se actualiza automáticamente cuando hay cambios en los medicamentos

### NuevaMedicinaActivity
**Propósito**: Formulario para crear o editar medicamentos.

**Funcionalidades**:
- Creación de nuevos medicamentos con todos sus datos
- Edición de medicamentos existentes
- Selección de presentación (pastilla, cápsula, jarabe, etc.)
- Configuración de horarios de toma (primera toma del día)
- Configuración de tomas diarias
- Selección de color personalizado
- Configuración de fecha de vencimiento
- Configuración de stock inicial y días de tratamiento
- Validación de campos obligatorios
- Asignación automática de color si no se selecciona uno

**Interacciones**:
- El usuario completa el formulario con la información del medicamento
- Al seleccionar "Seleccionar Hora", se abre un TimePicker
- Al seleccionar "Seleccionar Color", se muestra un diálogo con opciones de color
- Al presionar "Guardar", se valida la información y se guarda en Firebase
- Si Google Calendar está conectado, se crean automáticamente los eventos correspondientes
- Se programan las alarmas para los próximos 30 días

### BotiquinActivity
**Propósito**: Visualización de todos los medicamentos organizados por categorías.

**Funcionalidades**:
- Visualización de medicamentos en tratamiento (con tomas diarias > 0)
- Visualización de medicamentos ocasionales (con tomas diarias = 0)
- Acción rápida "Tomé una" para registrar una toma manual
- Edición de medicamentos desde la lista
- Eliminación de medicamentos
- Actualización en tiempo real de la lista

**Interacciones**:
- El usuario puede ver todos sus medicamentos organizados en dos secciones
- Al hacer clic en "Tomé una", se registra una toma y se actualiza el stock
- Al hacer clic en un medicamento, se puede editar o eliminar
- Los botones de navegación permiten cambiar de sección

### HistorialActivity
**Propósito**: Visualización de estadísticas y historial de tratamientos.

**Funcionalidades**:
- Gráfico de barras mostrando adherencia al tratamiento
- Estadísticas generales (total de medicamentos, tratamientos concluidos)
- Lista de tratamientos concluidos
- Cálculo de porcentaje de adherencia (funcionalidad básica implementada)

**Interacciones**:
- El usuario puede visualizar sus estadísticas de adherencia
- El gráfico muestra información visual sobre el cumplimiento de los tratamientos
- La lista de tratamientos concluidos muestra medicamentos que ya no están activos

### AjustesActivity
**Propósito**: Configuración de perfil de usuario y preferencias de la aplicación.

**Funcionalidades**:
- Edición de perfil de usuario (nombre, email, teléfono, edad)
- Configuración de notificaciones (activar/desactivar, vibración, sonido)
- Configuración de volumen y repeticiones de notificaciones
- Configuración de días de antelación para alertas de stock
- Conexión y desconexión de Google Calendar
- Cerrar sesión
- Eliminación de cuenta (funcionalidad en desarrollo)

**Interacciones**:
- El usuario puede modificar su información personal y guardar los cambios
- Los switches permiten activar/desactivar diferentes tipos de notificaciones
- Los SeekBars permiten ajustar volumen y número de repeticiones
- El botón "Conectar Google Calendar" inicia el flujo OAuth
- El botón "Desconectar" elimina la conexión con Google Calendar
- El botón "Cerrar Sesión" cierra la sesión actual y redirige a LoginActivity

### GoogleCalendarCallbackActivity
**Propósito**: Maneja el callback del flujo OAuth 2.0 para Google Calendar.

**Funcionalidades**:
- Captura del access_token del fragment de la URL de redirección
- Almacenamiento seguro del token en Firestore
- Redirección de vuelta a AjustesActivity

**Interacciones**:
- Esta actividad se ejecuta automáticamente después de la autorización de Google
- El usuario no interactúa directamente con esta pantalla
- Procesa el token y redirige automáticamente

## Integración de APIs

### Firebase Authentication
**Uso**: Autenticación de usuarios mediante email/contraseña y Google Sign-In.

**Implementación**:
- Se utiliza `FirebaseAuth` para gestionar la autenticación
- Los tokens de Google Sign-In se intercambian por credenciales de Firebase
- La sesión se mantiene automáticamente entre reinicios de la aplicación

**Seguridad**:
- Las contraseñas se almacenan de forma segura en Firebase (nunca en texto plano)
- Firebase maneja automáticamente la encriptación y seguridad de las credenciales
- Se valida la conexión a internet antes de realizar operaciones de autenticación

### Firebase Firestore
**Uso**: Almacenamiento de datos de usuarios, medicamentos y tomas.

**Implementación**:
- Se utiliza `FirebaseFirestore` para todas las operaciones CRUD
- Los datos se organizan en colecciones: `usuarios`, `medicamentos`, `tomas`, `googleTokens`
- Se implementan listeners en tiempo real para actualización automática de la UI

**Seguridad**:
- Las reglas de Firestore garantizan que cada usuario solo puede leer/escribir sus propios datos
- Se valida la autenticación antes de cada operación
- Los tokens de Google Calendar se almacenan de forma segura asociados al userId

### Google Calendar API
**Uso**: Sincronización de eventos de toma de medicamentos con Google Calendar.

**Implementación**:
- Se utiliza OAuth 2.0 flujo implícito para obtener el `access_token`
- Se realizan peticiones HTTP REST a la API de Google Calendar usando OkHttp
- Los eventos se crean, actualizan y eliminan mediante peticiones POST, PUT y DELETE

**Endpoints utilizados**:
- `POST /calendar/v3/calendars/primary/events` - Crear evento
- `PUT /calendar/v3/calendars/primary/events/{eventId}` - Actualizar evento
- `DELETE /calendar/v3/calendars/primary/events/{eventId}` - Eliminar evento

**Seguridad**:
- El `access_token` se almacena de forma segura en Firestore
- Se verifica la expiración del token antes de realizar peticiones
- Las peticiones incluyen el header `Authorization: Bearer {access_token}`
- Se manejan errores de autenticación y se solicita re-autenticación cuando es necesario

**Flujo OAuth**:
1. El usuario presiona "Conectar Google Calendar" en AjustesActivity
2. Se abre Custom Tabs con la URL de autorización de Google
3. El usuario autoriza la aplicación
4. Google redirige a `GoogleCalendarCallbackActivity` con el `access_token` en el fragment
5. Se extrae el token y se guarda en Firestore
6. Se redirige de vuelta a AjustesActivity

## Seguridad Implementada

### Autenticación
- **Firebase Authentication**: Sistema robusto de autenticación con encriptación de contraseñas
- **Validación de Sesión**: Verificación de autenticación antes de acceder a cualquier pantalla protegida
- **Google Sign-In**: Autenticación segura mediante OAuth 2.0 con Google

### Almacenamiento de Datos
- **Firestore Security Rules**: Reglas que garantizan que cada usuario solo accede a sus propios datos
- **Validación de Usuario**: Todas las operaciones verifican que el usuario esté autenticado
- **Tokens Seguros**: Los tokens de Google Calendar se almacenan asociados al userId en Firestore

### Comunicación
- **HTTPS**: Todas las comunicaciones con APIs externas se realizan mediante HTTPS
- **Validación de Red**: Verificación de conexión a internet antes de operaciones que requieren red
- **Manejo de Errores**: Manejo robusto de errores de red y autenticación

### Permisos
- **Permisos Mínimos**: Solo se solicitan los permisos necesarios (internet, notificaciones, alarmas)
- **Permisos Declarados**: Todos los permisos están declarados en AndroidManifest.xml

## Estructura del Proyecto

```
app/src/main/java/com/controlmedicamentos/myapplication/
├── activities/
│   ├── LoginActivity.java
│   ├── MainActivity.java
│   ├── NuevaMedicinaActivity.java
│   ├── BotiquinActivity.java
│   ├── HistorialActivity.java
│   ├── AjustesActivity.java
│   └── GoogleCalendarCallbackActivity.java
├── adapters/
│   ├── MedicamentoAdapter.java
│   ├── BotiquinAdapter.java
│   └── HistorialAdapter.java
├── models/
│   ├── Medicamento.java
│   ├── Usuario.java
│   └── Toma.java
├── services/
│   ├── AuthService.java
│   ├── FirebaseService.java
│   ├── GoogleCalendarAuthService.java
│   ├── GoogleCalendarService.java
│   └── NotificationService.java
├── receivers/
│   ├── AlarmReceiver.java
│   └── BootReceiver.java
└── utils/
    ├── AlarmScheduler.java
    ├── ColorUtils.java
    ├── NetworkUtils.java
    └── StockAlertUtils.java
```

## Tecnologías Utilizadas

- **Android SDK**: Desarrollo nativo para Android
- **Java**: Lenguaje de programación principal
- **Firebase Authentication**: Autenticación de usuarios
- **Firebase Firestore**: Base de datos NoSQL en la nube
- **Google Sign-In**: Autenticación con Google
- **Google Calendar API**: Sincronización de eventos
- **OkHttp**: Cliente HTTP para peticiones a APIs
- **MPAndroidChart**: Librería para gráficos
- **Material Design**: Componentes de UI modernos

## Requisitos del Sistema

- **Android**: Versión mínima 10.0 (API 29)
- **Target SDK**: 36
- **Conexión a Internet**: Requerida para autenticación y sincronización
- **Google Play Services**: Requerido para Google Sign-In y Google Calendar

## Configuración

1. Descargar el archivo `google-services.json` desde Firebase Console
2. Colocarlo en `app/google-services.json`
3. Configurar las reglas de Firestore según `INSTRUCCIONES_FIREBASE.md`
4. Habilitar Authentication en Firebase Console
5. Configurar OAuth 2.0 en Google Cloud Console para Google Calendar

## Notas de Desarrollo

- La aplicación utiliza listeners en tiempo real de Firestore para actualización automática
- Las alarmas se programan para los próximos 30 días para evitar exceder límites del sistema
- Los eventos de Google Calendar se crean automáticamente al guardar medicamentos
- La sincronización con Google Calendar es opcional y se puede activar/desactivar desde Ajustes

