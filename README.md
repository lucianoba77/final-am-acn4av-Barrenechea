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
- **Vista Detallada**: Pantalla de detalles completa con información del medicamento, historial de tomas y gráficos de adherencia
- **Tratamientos Crónicos y Programados**: Soporte para medicamentos de uso continuo o con duración limitada
- **Medicamentos Ocasionales**: Gestión de medicamentos que no requieren horarios fijos

### Recordatorios y Notificaciones
- **Alarmas Programadas**: Sistema de alarmas que programa recordatorios para los próximos 30 días
- **Notificaciones Push**: Notificaciones locales que alertan al usuario sobre las tomas programadas
- **Reprogramación Automática**: Las alarmas se reprograman automáticamente después de reiniciar el dispositivo
- **Configuración de Notificaciones**: Control de volumen, vibración y sonido de las notificaciones
- **Acciones Rápidas desde Notificaciones**: Posibilidad de marcar tomas como realizadas o posponerlas directamente desde la notificación
- **Seguimiento de Estado de Tomas**: Sistema que rastrea el estado de cada toma programada (pendiente, tomada, omitida)
- **Posposición de Tomas**: Permite posponer una toma hasta 3 veces antes de considerarla omitida

### Seguimiento y Adherencia
- **Seguimiento de Tomas**: Sistema que rastrea el estado de cada toma programada (pendiente, tomada, omitida)
- **Cálculo de Adherencia**: Métricas avanzadas de adherencia con cálculos semanales y mensuales
- **Gráficos de Adherencia**: Visualización gráfica de la adherencia al tratamiento por medicamento
- **Resumen de Adherencia**: Porcentajes de cumplimiento y estadísticas de tomas realizadas vs esperadas
- **Historial Detallado**: Registro completo de todas las tomas con fecha, hora y estado
- **Posposición de Tomas**: Permite posponer una toma hasta 3 veces antes de considerarla omitida

### Sincronización con Google Calendar
- **Integración OAuth 2.0**: Autenticación segura con Google Calendar mediante Google Sign-In para Android
- **Creación Automática de Eventos**: Los eventos de toma de medicamentos se crean automáticamente en Google Calendar
- **Actualización de Eventos**: Los eventos se actualizan cuando se modifican los horarios de los medicamentos
- **Eliminación de Eventos**: Los eventos se eliminan cuando se elimina un medicamento
- **Recordatorios en Calendar**: Los eventos incluyen recordatorios 15 y 5 minutos antes de cada toma
- **Modo de Prueba**: La aplicación está en desarrollo con usuarios de prueba configurados. El mensaje de advertencia de Google es normal y esperado durante esta fase

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
- Cálculo de porcentaje de adherencia con métricas avanzadas
- Plan de adherencia personalizada por medicamento
- Gráficos semanales y mensuales de adherencia

**Interacciones**:
- El usuario puede visualizar sus estadísticas de adherencia
- El gráfico muestra información visual sobre el cumplimiento de los tratamientos
- La lista de tratamientos concluidos muestra medicamentos que ya no están activos
- Selección de medicamentos para ver planes de adherencia detallados

### DetallesMedicamentoActivity
**Propósito**: Pantalla detallada de un medicamento con información completa, historial y estadísticas.

**Funcionalidades**:
- Visualización completa de información del medicamento (nombre, presentación, afección, horarios, stock, estado)
- Historial de tomas con estado (tomada, omitida, pendiente)
- Gráfico de adherencia semanal
- Resumen de adherencia con porcentaje y tomas realizadas
- Navegación a edición del medicamento
- Cálculo automático de métricas de adherencia

**Interacciones**:
- El usuario puede ver todos los detalles de un medicamento específico
- El historial muestra todas las tomas registradas con fecha y hora
- El gráfico visualiza la adherencia semanal
- El botón "Editar" permite modificar el medicamento

### AjustesActivity
**Propósito**: Configuración de perfil de usuario y preferencias de la aplicación.

**Funcionalidades**:
- Edición de perfil de usuario (nombre, email, teléfono, edad)
- Configuración de notificaciones (activar/desactivar, vibración, sonido)
- Configuración de volumen y repeticiones de notificaciones
- Configuración de días de antelación para alertas de stock
- Conexión y desconexión de Google Calendar
- Mensaje informativo sobre el modo de prueba de Google Calendar
- Cerrar sesión
- Eliminación de cuenta con confirmación y reautenticación

**Interacciones**:
- El usuario puede modificar su información personal y guardar los cambios
- Los switches permiten activar/desactivar diferentes tipos de notificaciones
- Los SeekBars permiten ajustar volumen y número de repeticiones
- El botón "Conectar Google Calendar" inicia el flujo OAuth con Google Sign-In
- Durante el desarrollo, se muestra un mensaje informativo sobre el mensaje de advertencia de Google
- El botón "Desconectar" elimina la conexión con Google Calendar
- El botón "Cerrar Sesión" cierra la sesión actual y redirige a LoginActivity
- El botón "Eliminar Cuenta" permite eliminar permanentemente la cuenta y todos los datos asociados

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
- Se utiliza Google Sign-In para Android con scope de Calendar API
- Se obtiene un `serverAuthCode` que puede intercambiarse por `access_token` (requiere backend)
- Se realizan peticiones HTTP REST a la API de Google Calendar usando OkHttp
- Los eventos se crean, actualizan y eliminan mediante peticiones POST, PUT y DELETE

**Endpoints utilizados**:
- `POST /calendar/v3/calendars/primary/events` - Crear evento
- `PUT /calendar/v3/calendars/primary/events/{eventId}` - Actualizar evento
- `DELETE /calendar/v3/calendars/primary/events/{eventId}` - Eliminar evento

**Seguridad**:
- El `auth_code` o `access_token` se almacena de forma segura en Firestore
- Se verifica la expiración del token antes de realizar peticiones
- Las peticiones incluyen el header `Authorization: Bearer {access_token}`
- Se manejan errores de autenticación y se solicita re-autenticación cuando es necesario

**Flujo OAuth**:
1. El usuario presiona "Conectar Google Calendar" en AjustesActivity
2. Se inicia Google Sign-In con scope de Calendar API
3. El usuario autoriza la aplicación (puede aparecer mensaje de advertencia durante desarrollo)
4. Se obtiene el `serverAuthCode` del resultado de Google Sign-In
5. El código se guarda en Firestore para uso futuro
6. La UI se actualiza para mostrar el estado de conexión

**Nota sobre Desarrollo**:
- Durante el desarrollo, Google mostrará un mensaje de advertencia indicando que la app no ha sido verificada
- Esto es normal y esperado para aplicaciones en desarrollo
- Los usuarios de prueba configurados en Google Cloud Console pueden hacer clic en "Continuar" para proceder
- La aplicación incluye un mensaje informativo en la UI explicando este comportamiento

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
├── Activities/
│   ├── LoginActivity.java
│   ├── MainActivity.java
│   ├── NuevaMedicinaActivity.java
│   ├── BotiquinActivity.java
│   ├── HistorialActivity.java
│   ├── AjustesActivity.java
│   ├── DetallesMedicamentoActivity.java
│   └── GoogleCalendarCallbackActivity.java
├── adapters/
│   ├── MedicamentoAdapter.java
│   ├── BotiquinAdapter.java
│   ├── HistorialAdapter.java
│   └── TomaAdapter.java
├── models/
│   ├── Medicamento.java
│   ├── Usuario.java
│   ├── Toma.java
│   ├── TomaProgramada.java
│   ├── AdherenciaIntervalo.java
│   └── AdherenciaResumen.java
├── services/
│   ├── AuthService.java
│   ├── FirebaseService.java
│   ├── GoogleCalendarAuthService.java
│   ├── GoogleCalendarService.java
│   ├── NotificationService.java
│   ├── TomaTrackingService.java
│   └── TomaStateCheckerService.java
├── receivers/
│   ├── AlarmReceiver.java
│   ├── BootReceiver.java
│   └── TomaActionReceiver.java
└── utils/
    ├── AlarmScheduler.java
    ├── ColorUtils.java
    ├── NetworkUtils.java
    ├── StockAlertUtils.java
    └── AdherenciaCalculator.java
```

## Componentes Técnicos Principales

### Servicios
- **AuthService**: Maneja autenticación con Firebase y Google Sign-In
- **FirebaseService**: Servicio principal para operaciones CRUD con Firestore
- **GoogleCalendarAuthService**: Gestiona la autenticación OAuth con Google Calendar
- **GoogleCalendarService**: Realiza operaciones con la API de Google Calendar
- **NotificationService**: Gestiona las notificaciones push locales
- **TomaTrackingService**: Rastrea el estado de las tomas programadas usando SharedPreferences
- **TomaStateCheckerService**: Servicio en segundo plano que verifica el estado de las tomas

### Modelos de Datos
- **Medicamento**: Modelo principal con información completa del medicamento
- **Usuario**: Información del perfil de usuario
- **Toma**: Registro de una toma realizada u omitida
- **TomaProgramada**: Representa una toma programada con su estado (pendiente, tomada, omitida)
- **AdherenciaIntervalo**: Métricas de adherencia para un intervalo de tiempo específico
- **AdherenciaResumen**: Resumen general de adherencia con porcentajes y estadísticas

### Utilidades
- **AlarmScheduler**: Programa y gestiona las alarmas de recordatorios
- **AdherenciaCalculator**: Calcula métricas de adherencia en diferentes rangos temporales
- **ColorUtils**: Utilidades para asignación y gestión de colores
- **NetworkUtils**: Verificación de conectividad de red
- **StockAlertUtils**: Gestión de alertas de stock bajo

### Receivers
- **AlarmReceiver**: Recibe y procesa las alarmas programadas
- **BootReceiver**: Reprograma las alarmas después de reiniciar el dispositivo
- **TomaActionReceiver**: Maneja las acciones rápidas desde las notificaciones (tomar, posponer)

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
- **SharedPreferences**: Almacenamiento local para estado de tomas programadas

## Requisitos del Sistema

- **Android**: Versión mínima 10.0 (API 29)
- **Target SDK**: 36
- **Conexión a Internet**: Requerida para autenticación y sincronización
- **Google Play Services**: Requerido para Google Sign-In y Google Calendar


## Notas de Desarrollo

- La aplicación utiliza listeners en tiempo real de Firestore para actualización automática
- Las alarmas se programan para los próximos 30 días para evitar exceder límites del sistema
- Los eventos de Google Calendar se crean automáticamente al guardar medicamentos
- La sincronización con Google Calendar es opcional y se puede activar/desactivar desde Ajustes
- **Estado de Desarrollo**: La aplicación está actualmente en período de desarrollo y construcción
- **Usuarios de Prueba**: Se han configurado usuarios de prueba en Google Cloud Console para OAuth
- **Advertencia de Google**: Durante el desarrollo, Google mostrará un mensaje de advertencia al conectar Google Calendar. Esto es normal y esperado. Los usuarios de prueba autorizados pueden hacer clic en "Continuar" para proceder
- **Seguimiento de Tomas**: El sistema rastrea el estado de cada toma programada (pendiente, tomada, omitida) usando SharedPreferences y servicios en segundo plano
- **Cálculo de Adherencia**: Se implementó un sistema avanzado de cálculo de adherencia con métricas semanales y mensuales
- **Notificaciones Accionables**: Las notificaciones incluyen botones de acción rápida para marcar tomas o posponerlas

