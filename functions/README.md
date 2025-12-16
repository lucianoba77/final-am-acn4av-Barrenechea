# Firebase Cloud Functions - Google Calendar Integration

Este directorio contiene las Firebase Cloud Functions necesarias para la integración con Google Calendar en la app Android.

## Funciones Disponibles

### 1. `intercambiarGoogleCalendarToken`
**Tipo:** Callable Function  
**Descripción:** Intercambia un `auth_code` (obtenido mediante Google Sign-In) por `access_token` y `refresh_token`.

**Parámetros:**
```javascript
{
  authCode: string  // Código de autorización obtenido de Google Sign-In
}
```

**Retorna:**
```javascript
{
  access_token: string,
  token_type: string,
  expires_in: number,
  scope: string,
  fechaObtencion: string
}
```

**Uso desde Android:**
```java
Map<String, Object> data = new HashMap<>();
data.put("authCode", serverAuthCode);

functions.getHttpsCallable("intercambiarGoogleCalendarToken")
    .call(data)
    .continueWith(task -> {
        // Manejar resultado
    });
```

### 2. `refrescarGoogleCalendarToken`
**Tipo:** Callable Function  
**Descripción:** Renueva el `access_token` usando el `refresh_token` almacenado.

**Parámetros:**
```javascript
{
  refreshToken: string  // Refresh token almacenado en Firestore
}
```

**Retorna:**
```javascript
{
  access_token: string,
  token_type: string,
  expires_in: number,
  fechaActualizacion: string
}
```

**Uso desde Android:**
```java
Map<String, Object> data = new HashMap<>();
data.put("refreshToken", refreshToken);

functions.getHttpsCallable("refrescarGoogleCalendarToken")
    .call(data)
    .continueWith(task -> {
        // Manejar resultado
    });
```

## Configuración

### Variables de Entorno Requeridas

Las funciones requieren que se configuren las credenciales de Google OAuth:

```bash
firebase functions:config:set google.client_id="TU_CLIENT_ID"
firebase functions:config:set google.client_secret="TU_CLIENT_SECRET"
```

**Importante:**
- El `client_id` debe ser el mismo Web Client ID que se usa en la app Android (`default_web_client_id`)
- El `client_secret` solo debe estar en Firebase Functions, nunca en la app móvil

## Instalación

```bash
cd functions
npm install
```

## Despliegue

```bash
# Desplegar todas las funciones
firebase deploy --only functions

# Desplegar una función específica
firebase deploy --only functions:intercambiarGoogleCalendarToken
firebase deploy --only functions:refrescarGoogleCalendarToken
```

## Estructura de Datos en Firestore

Las funciones guardan los tokens en la colección `googleTokens` con la siguiente estructura:

```javascript
googleTokens/{userId} {
  access_token: string,
  token_type: string,
  expires_in: number,
  refresh_token: string,
  scope: string,
  fechaObtencion: string,
  fechaActualizacion: string
}
```

## Seguridad

- ✅ Las funciones verifican que el usuario esté autenticado (`context.auth`)
- ✅ El `client_secret` nunca se expone a la app móvil
- ✅ Los tokens se almacenan de forma segura en Firestore
- ✅ Solo el usuario propietario puede acceder a sus tokens

## Troubleshooting

### Error: "Client ID o Client Secret no configurados"
Verifica la configuración:
```bash
firebase functions:config:get
```

### Error: "El usuario debe estar autenticado"
Asegúrate de que el usuario haya iniciado sesión en Firebase Auth antes de llamar a las funciones.

### Ver logs
```bash
firebase functions:log
```

