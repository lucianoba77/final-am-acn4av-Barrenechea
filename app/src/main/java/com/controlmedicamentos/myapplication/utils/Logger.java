package com.controlmedicamentos.myapplication.utils;

import android.util.Log;

/**
 * Clase de logging que solo muestra logs de debug en modo desarrollo.
 * En producción, solo se muestran errores y warnings.
 */
public class Logger {
    // BuildConfig se genera en tiempo de compilación, usar true para desarrollo
    // En producción, cambiar a false o usar BuildConfig.DEBUG si está disponible
    private static final boolean DEBUG = true; // Cambiar a false en producción o usar BuildConfig.DEBUG

    /**
     * Log de debug. Solo se muestra en modo desarrollo.
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     */
    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(tag, message);
        }
    }

    /**
     * Log de debug con formato. Solo se muestra en modo desarrollo.
     * 
     * @param tag Tag para identificar el origen del log
     * @param format Formato del mensaje (como String.format)
     * @param args Argumentos para el formato
     */
    public static void d(String tag, String format, Object... args) {
        if (DEBUG) {
            Log.d(tag, String.format(format, args));
        }
    }

    /**
     * Log de información. Solo se muestra en modo desarrollo.
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     */
    public static void i(String tag, String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }

    /**
     * Log de warning. Siempre se muestra (incluso en producción).
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     */
    public static void w(String tag, String message) {
        Log.w(tag, message);
    }

    /**
     * Log de warning con excepción. Siempre se muestra.
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     * @param throwable Excepción asociada
     */
    public static void w(String tag, String message, Throwable throwable) {
        Log.w(tag, message, throwable);
    }

    /**
     * Log de error. Siempre se muestra (incluso en producción).
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     */
    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    /**
     * Log de error con excepción. Siempre se muestra.
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     * @param throwable Excepción asociada
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }

    /**
     * Log de error solo con excepción. Siempre se muestra.
     * 
     * @param tag Tag para identificar el origen del log
     * @param throwable Excepción asociada
     */
    public static void e(String tag, Throwable throwable) {
        Log.e(tag, "Error", throwable);
    }

    /**
     * Log verbose. Solo se muestra en modo desarrollo.
     * 
     * @param tag Tag para identificar el origen del log
     * @param message Mensaje a loguear
     */
    public static void v(String tag, String message) {
        if (DEBUG) {
            Log.v(tag, message);
        }
    }
}

