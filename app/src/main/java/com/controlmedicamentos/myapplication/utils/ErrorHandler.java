package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Clase centralizada para manejo de errores en toda la aplicación.
 * Proporciona mensajes de error consistentes y amigables para el usuario.
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    /**
     * Maneja un error y muestra un mensaje apropiado al usuario.
     * 
     * @param context Contexto de la aplicación
     * @param error Excepción que se produjo
     * @param tag Tag para logging (usualmente el nombre de la clase)
     */
    public static void handleError(Context context, Exception error, String tag) {
        if (context == null || error == null) {
            Log.e(TAG, "Context o error es null");
            return;
        }

        // Log del error para debugging
        Log.e(tag != null ? tag : TAG, "Error: ", error);

        // Obtener mensaje amigable para el usuario
        String mensaje = getErrorMessage(error);

        // Mostrar Toast al usuario
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();

        // Opcional: Reportar a Crashlytics en producción
        // FirebaseCrashlytics.getInstance().recordException(error);
    }

    /**
     * Maneja un error sin mostrar Toast (solo logging).
     * Útil para errores no críticos que no requieren notificar al usuario.
     * 
     * @param error Excepción que se produjo
     * @param tag Tag para logging
     */
    public static void logError(Exception error, String tag) {
        if (error == null) {
            return;
        }
        Log.e(tag != null ? tag : TAG, "Error: ", error);
    }

    /**
     * Obtiene un mensaje de error amigable para el usuario basado en el tipo de excepción.
     * 
     * @param error Excepción que se produjo
     * @return Mensaje de error amigable
     */
    private static String getErrorMessage(Exception error) {
        if (error == null) {
            return "Ocurrió un error desconocido. Intenta nuevamente.";
        }

        // Errores de red
        if (error instanceof FirebaseNetworkException || 
            error instanceof java.net.UnknownHostException ||
            error instanceof java.net.SocketTimeoutException) {
            return "Error de conexión. Verifica tu conexión a internet e intenta nuevamente.";
        }

        // Errores de Firebase Auth
        if (error instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) error;
            String errorCode = authException.getErrorCode();
            
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "El correo electrónico no es válido.";
                case "ERROR_WRONG_PASSWORD":
                    return "La contraseña es incorrecta.";
                case "ERROR_USER_NOT_FOUND":
                    return "No existe una cuenta con este correo electrónico.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Este correo electrónico ya está registrado.";
                case "ERROR_WEAK_PASSWORD":
                    return "La contraseña es muy débil. Debe tener al menos 6 caracteres.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Error de conexión. Verifica tu internet.";
                default:
                    return "Error de autenticación: " + error.getMessage();
            }
        }

        // Errores de Firestore
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                    return "No tienes permisos para realizar esta acción.";
                case UNAVAILABLE:
                    return "El servicio no está disponible. Intenta más tarde.";
                case DEADLINE_EXCEEDED:
                    return "La operación tardó demasiado. Intenta nuevamente.";
                default:
                    return "Error al conectar con el servidor. Intenta nuevamente.";
            }
        }

        // Errores de Firebase genéricos
        if (error instanceof FirebaseException) {
            return "Error al conectar con el servidor. Verifica tu conexión.";
        }

        // Error genérico
        String errorMessage = error.getMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            // Si el mensaje es muy técnico, usar mensaje genérico
            if (errorMessage.contains("Exception") || errorMessage.contains("Error")) {
                return "Ocurrió un error. Intenta nuevamente.";
            }
            return errorMessage;
        }

        return "Ocurrió un error inesperado. Intenta nuevamente.";
    }

    /**
     * Maneja un error con un mensaje personalizado.
     * 
     * @param context Contexto de la aplicación
     * @param error Excepción que se produjo
     * @param tag Tag para logging
     * @param customMessage Mensaje personalizado para mostrar al usuario
     */
    public static void handleErrorWithCustomMessage(
            Context context, 
            Exception error, 
            String tag, 
            String customMessage) {
        
        if (context == null) {
            return;
        }

        // Log del error
        if (error != null) {
            Log.e(tag != null ? tag : TAG, "Error: ", error);
        }

        // Mostrar mensaje personalizado
        Toast.makeText(context, customMessage, Toast.LENGTH_LONG).show();
    }
}

