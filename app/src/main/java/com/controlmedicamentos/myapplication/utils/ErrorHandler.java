package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.controlmedicamentos.myapplication.R;
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

        Log.e(tag != null ? tag : TAG, "Error: ", error);
        String mensaje = getErrorMessage(context, error);
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
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
     * Obtiene un mensaje de error amigable desde recursos (strings.xml).
     * Usar este método cuando se disponga de Context.
     *
     * @param context Contexto para acceder a recursos
     * @param error Excepción que se produjo
     * @return Mensaje de error amigable
     */
    public static String getErrorMessage(Context context, Exception error) {
        if (context == null) {
            return getErrorMessage(error);
        }
        if (error == null) {
            return context.getString(R.string.error_unknown);
        }

        if (error instanceof FirebaseNetworkException
                || error instanceof java.net.UnknownHostException
                || error instanceof java.net.SocketTimeoutException) {
            return context.getString(R.string.error_network);
        }

        if (error instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) error;
            String errorCode = authException.getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return context.getString(R.string.error_auth_invalid_email);
                case "ERROR_WRONG_PASSWORD":
                    return context.getString(R.string.error_auth_wrong_password);
                case "ERROR_USER_NOT_FOUND":
                    return context.getString(R.string.error_auth_user_not_found);
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return context.getString(R.string.error_auth_email_in_use);
                case "ERROR_WEAK_PASSWORD":
                    return context.getString(R.string.error_auth_weak_password);
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return context.getString(R.string.error_network_short);
                default:
                    return context.getString(R.string.error_auth_generic,
                            error.getMessage() != null ? error.getMessage() : "");
            }
        }

        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                    return context.getString(R.string.error_firestore_permission_denied);
                case UNAVAILABLE:
                    return context.getString(R.string.error_firestore_unavailable);
                case DEADLINE_EXCEEDED:
                    return context.getString(R.string.error_firestore_deadline);
                default:
                    return context.getString(R.string.error_firestore_default);
            }
        }

        if (error instanceof FirebaseException) {
            return context.getString(R.string.error_firebase_generic);
        }

        String errorMessage = error.getMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            if (errorMessage.contains("Exception") || errorMessage.contains("Error")) {
                return context.getString(R.string.error_generic);
            }
            return errorMessage;
        }

        return context.getString(R.string.error_unexpected);
    }

    /**
     * Obtiene un mensaje de error amigable sin Context (usa literales).
     * Mantenido para tests unitarios que no inyectan Context. Preferir getErrorMessage(Context, Exception).
     *
     * @param error Excepción que se produjo
     * @return Mensaje de error amigable
     */
    public static String getErrorMessage(Exception error) {
        if (error == null) {
            return "Ocurrió un error desconocido. Intenta nuevamente.";
        }
        if (error instanceof FirebaseNetworkException
                || error instanceof java.net.UnknownHostException
                || error instanceof java.net.SocketTimeoutException) {
            return "Error de conexión. Verifica tu conexión a internet e intenta nuevamente.";
        }
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
                    return "Error de autenticación: " + (error.getMessage() != null ? error.getMessage() : "");
            }
        }
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
        if (error instanceof FirebaseException) {
            return "Error al conectar con el servidor. Verifica tu conexión.";
        }
        String errorMessage = error.getMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
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

