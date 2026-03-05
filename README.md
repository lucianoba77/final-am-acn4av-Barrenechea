# MiMedicina - Aplicación Android

**Nombre oficial de la app:** MiMedicina (Control de Medicamentos). El nombre visible en el dispositivo es el definido en `app_name` (strings.xml).

Aplicación móvil Android para gestionar medicamentos y tratamientos: recordatorios, seguimiento de tomas, gestión de stock, adhesión al tratamiento y sincronización con Google Calendar. Los datos se almacenan en Firebase Firestore.

**Web Client ID (OAuth):** El Client ID público de Google se define en `BuildConfig.WEB_CLIENT_ID` (véase `app/build.gradle`). Opcionalmente se puede sobrescribir con la propiedad `WEB_CLIENT_ID` en `local.properties`. Es un identificador público, no un secreto.

**Compatibilidad con la versión web (MiMedicina):** La app está alineada con la versión web en datos y lógica. Las tomas marcadas en web o en Android se sincronizan vía Firestore (colección `tomas`). Destacan la misma pantalla de adhesión (estadísticas + lista por medicamento), programación personalizada por día de la semana y asignación automática de color.

---

## Funcionalidades principales

- **Autenticación:** Email/contraseña y Google Sign-In (Firebase). Recuperación de contraseña.
- **Medicamentos:** Crear, editar y eliminar. Stock y alertas. Programación por día de la semana (opcional). Color automático por índice. Crónicos, programados y ocasionales.
- **Recordatorios:** Alarmas 30 días, notificaciones con acciones (tomar / posponer). Posposición hasta 3 veces.
- **Adhesión:** Pantalla «Adhesión» con tres cifras (Con seguimiento, Activos vigentes, No vigentes) y lista «Adherencia por medicamento (Total)» con porcentaje, barra y mensaje de estado. Solo medicamentos con programación diaria (se excluyen ocasionales y vencidos).
- **Google Calendar:** OAuth, creación/actualización/eliminación de eventos con recordatorios. Opcional desde Ajustes.

---

## Pantallas (Activities)

| Pantalla | Descripción |
|----------|-------------|
| **LoginActivity** | Inicio de sesión (email, Google), registro y recuperación de contraseña. |
| **MainActivity** | Dashboard: medicamentos del día ordenados por horario, barras de progreso, stock. Actualización en tiempo real. |
| **NuevaMedicinaActivity** | Formulario crear/editar: presentación, horarios, tomas diarias, programación personalizada por día, vencimiento, stock. Color automático. |
| **BotiquinActivity** | Lista de medicamentos (tratamientos programados y ocasionales). Editar, eliminar, «Tomé una». |
| **HistorialActivity** | **Adhesión:** tres cifras (Con seguimiento / Activos vigentes / No vigentes) y lista de adherencia por medicamento (porcentaje, total/mensual/semanal, estado). |
| **DetallesMedicamentoActivity** | Detalle del medicamento, historial de tomas y gráfico de adherencia semanal. |
| **AjustesActivity** | Perfil, notificaciones, stock, Google Calendar, cerrar sesión, eliminar cuenta. |
| **GoogleCalendarCallbackActivity** | Callback OAuth para Google Calendar (no interactivo). |

---

## APIs e integración

- **Firebase Auth:** Email/contraseña y Google Sign-In. Sesión persistente.
- **Firestore:** Colecciones `usuarios`, `medicamentos`, `tomas`, `googleTokens`. Listeners en tiempo real. Reglas por usuario.
- **Google Calendar API:** OkHttp + OAuth. Crear/actualizar/eliminar eventos. Token en Firestore. Modo desarrollo con usuarios de prueba.

---

## Seguridad

- Autenticación obligatoria en pantallas protegidas. Datos por usuario (reglas Firestore). Tokens de Calendar en Firestore. HTTPS y validación de red. Permisos mínimos (internet, notificaciones, alarmas).

---

## Estructura del proyecto

```
app/src/main/java/.../myapplication/
├── (Activities en raíz: Login, Main, NuevaMedicina, Botiquin, Historial, Detalles, Ajustes, GoogleCalendarCallback)
├── adapters/     MedicamentoAdapter, BotiquinAdapter, HistorialAdapter, AdherenciaAdapter, TomaAdapter
├── models/       Medicamento, Usuario, Toma, TomaProgramada, AdherenciaIntervalo, AdherenciaResumen
├── services/     AuthService, FirebaseService, GoogleCalendar*, NotificationService, TomaTrackingService, TomaStateCheckerService
├── receivers/    AlarmReceiver, BootReceiver, TomaActionReceiver
└── utils/        AlarmScheduler, AdherenciaCalculator, MedicamentoUtils, EstadoAdherencia, ColorUtils, NetworkUtils, StockAlertUtils, TomaActionHandler
```

---

## Tecnologías

Android SDK, Java, Firebase (Auth + Firestore), Google Sign-In, Google Calendar API, OkHttp, MPAndroidChart, Material Design, SharedPreferences.

**Requisitos:** Android 10 (API 29) mínimo, target 36. Internet y Google Play Services para Auth y Calendar.

---

## Configuración para compilar

El archivo **`google-services.json`** no está en el repo. Descargarlo desde [Firebase Console](https://console.firebase.google.com/) → tu proyecto → Configuración → Tus apps → app Android (`com.controlmedicamentos.myapplication`) y colocar en **`app/google-services.json`**.

**Error "Desarrollador error" con Google Sign-In:** Registrar la **SHA-1** (y SHA-256) del keystore de debug en Firebase (Configuración → Tus apps → Huellas digitales). Obtener huellas con:

```bash
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Luego volver a descargar `google-services.json` y recompilar.

---

## Notas de desarrollo

- Listeners Firestore en tiempo real. Alarmas a 30 días. Eventos de Calendar al guardar medicamentos.
- Tomas: estado pendiente/tomada/omitida en SharedPreferences y servicio en segundo plano. Notificaciones con botones tomar/posponer.
- Google Calendar en desarrollo: mensaje de advertencia de Google es normal; usuarios de prueba pueden continuar.
