/**
 * Firebase Cloud Functions para Google Calendar
 * Intercambia auth_code por tokens y renueva tokens
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { google } = require('googleapis');

// Inicializar Firebase Admin si no está inicializado
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const COLECCION_TOKENS = 'googleTokens';

/**
 * Intercambia un auth_code por access_token y refresh_token
 * Función callable desde la app Android
 */
exports.intercambiarGoogleCalendarToken = functions.https.onCall(async (data, context) => {
  try {
    // Verificar autenticación
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'El usuario debe estar autenticado'
      );
    }

    const userId = context.auth.uid;
    const { authCode } = data;

    if (!authCode) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'El authCode es requerido'
      );
    }

    // Obtener client_id y client_secret desde la configuración de Firebase Functions
    const config = functions.config();
    const clientId = config.google?.client_id;
    const clientSecret = config.google?.client_secret;

    if (!clientId || !clientSecret) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Client ID o Client Secret no configurados en Firebase Functions'
      );
    }

    // Intercambiar auth_code por tokens usando Google OAuth2
    // Para requestServerAuthCode en Android, el redirect_uri debe coincidir con uno de los URIs autorizados
    // Usamos el URI HTTPS configurado en Google Cloud Console
    const redirectUri = 'https://mimedicinaapp.firebaseapp.com/googlecalendar/callback';
    const oauth2Client = new google.auth.OAuth2(
      clientId,
      clientSecret,
      redirectUri
    );

    const { tokens } = await oauth2Client.getToken(authCode);

    if (!tokens.access_token) {
      throw new functions.https.HttpsError(
        'internal',
        'No se pudo obtener el access_token'
      );
    }

    // Preparar datos del token para guardar
    const tokenData = {
      access_token: tokens.access_token,
      token_type: tokens.token_type || 'Bearer',
      expires_in: tokens.expiry_date 
        ? Math.floor((tokens.expiry_date - Date.now()) / 1000)
        : tokens.expires_in || 3600,
      refresh_token: tokens.refresh_token || null,
      scope: tokens.scope || 'https://www.googleapis.com/auth/calendar.events',
      fechaObtencion: new Date().toISOString(),
      fechaActualizacion: new Date().toISOString(),
    };

    // Guardar token en Firestore
    await db.collection(COLECCION_TOKENS).doc(userId).set(tokenData, { merge: true });

    // Retornar confirmación de éxito
    // El token ya está guardado en Firestore con refresh_token incluido
    return {
      success: true,
      message: 'Token guardado exitosamente en Firestore',
      access_token: tokenData.access_token, // Solo para confirmación, ya está en Firestore
      expires_in: tokenData.expires_in,
      fechaObtencion: tokenData.fechaObtencion,
    };
  } catch (error) {
    console.error('Error al intercambiar token:', error);
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    throw new functions.https.HttpsError(
      'internal',
      'Error al intercambiar el token: ' + error.message
    );
  }
});

/**
 * Renueva el access_token usando el refresh_token
 * Función callable desde la app Android
 */
exports.refrescarGoogleCalendarToken = functions.https.onCall(async (data, context) => {
  try {
    // Verificar autenticación
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'El usuario debe estar autenticado'
      );
    }

    const userId = context.auth.uid;
    const { refreshToken } = data;

    if (!refreshToken) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'El refreshToken es requerido'
      );
    }

    // Obtener client_id y client_secret desde la configuración
    const config = functions.config();
    const clientId = config.google?.client_id;
    const clientSecret = config.google?.client_secret;

    if (!clientId || !clientSecret) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Client ID o Client Secret no configurados en Firebase Functions'
      );
    }

    // Renovar token usando Google OAuth2
    // Para renovación, usamos el mismo redirect_uri que en el intercambio inicial
    const redirectUri = 'https://mimedicinaapp.firebaseapp.com/googlecalendar/callback';
    const oauth2Client = new google.auth.OAuth2(
      clientId,
      clientSecret,
      redirectUri
    );

    oauth2Client.setCredentials({
      refresh_token: refreshToken,
    });

    const { credentials } = await oauth2Client.refreshAccessToken();

    if (!credentials.access_token) {
      throw new functions.https.HttpsError(
        'internal',
        'No se pudo renovar el access_token'
      );
    }

    // Obtener token actual de Firestore para preservar refresh_token
    const tokenDoc = await db.collection(COLECCION_TOKENS).doc(userId).get();
    const tokenDataActual = tokenDoc.exists ? tokenDoc.data() : {};

    // Preparar datos actualizados
    const tokenDataActualizado = {
      ...tokenDataActual,
      access_token: credentials.access_token,
      token_type: credentials.token_type || 'Bearer',
      expires_in: credentials.expiry_date
        ? Math.floor((credentials.expiry_date - Date.now()) / 1000)
        : credentials.expires_in || 3600,
      // Preservar refresh_token si existe
      refresh_token: credentials.refresh_token || tokenDataActual.refresh_token || refreshToken,
      fechaActualizacion: new Date().toISOString(),
    };

    // Guardar token actualizado en Firestore
    await db.collection(COLECCION_TOKENS).doc(userId).set(tokenDataActualizado, { merge: true });

    // Retornar datos del token renovado
    return {
      access_token: tokenDataActualizado.access_token,
      token_type: tokenDataActualizado.token_type,
      expires_in: tokenDataActualizado.expires_in,
      fechaActualizacion: tokenDataActualizado.fechaActualizacion,
    };
  } catch (error) {
    console.error('Error al renovar token:', error);
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    throw new functions.https.HttpsError(
      'internal',
      'Error al renovar el token: ' + error.message
    );
  }
});
