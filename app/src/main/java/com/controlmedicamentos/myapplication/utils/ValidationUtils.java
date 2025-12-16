package com.controlmedicamentos.myapplication.utils;

import android.text.TextUtils;
import android.util.Log;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import java.util.regex.Pattern;

/**
 * Utilidades para validación de datos en toda la aplicación.
 * Proporciona validación consistente y reutilizable.
 */
public class ValidationUtils {
    private static final String TAG = "ValidationUtils";

    // Patrón para validar email
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Patrón para validar horario (HH:mm)
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"
    );

    /**
     * Valida si un medicamento es válido.
     * 
     * @param medicamento Medicamento a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidMedicamento(Medicamento medicamento) {
        if (medicamento == null) {
            Log.w(TAG, "Medicamento es null");
            return false;
        }

        if (TextUtils.isEmpty(medicamento.getId())) {
            Log.w(TAG, "ID del medicamento es null o vacío");
            return false;
        }

        if (TextUtils.isEmpty(medicamento.getNombre())) {
            Log.w(TAG, "Nombre del medicamento es null o vacío");
            return false;
        }

        return true;
    }

    /**
     * Valida si una toma es válida.
     * 
     * @param toma Toma a validar
     * @return true si es válida, false en caso contrario
     */
    public static boolean isValidToma(Toma toma) {
        if (toma == null) {
            Log.w(TAG, "Toma es null");
            return false;
        }

        if (TextUtils.isEmpty(toma.getMedicamentoId())) {
            Log.w(TAG, "ID del medicamento en la toma es null o vacío");
            return false;
        }

        if (toma.getFechaHoraTomada() == null && toma.getFechaHoraProgramada() == null) {
            Log.w(TAG, "Toma no tiene fecha programada ni fecha de toma");
            return false;
        }

        return true;
    }

    /**
     * Valida el formulario de medicamento y retorna mensaje de error si hay alguno.
     * 
     * @param medicamento Medicamento a validar
     * @return null si es válido, mensaje de error en caso contrario
     */
    public static String validateMedicamentoForm(Medicamento medicamento) {
        if (medicamento == null) {
            return "El medicamento no puede ser nulo";
        }

        // Validar nombre
        if (TextUtils.isEmpty(medicamento.getNombre()) || 
            medicamento.getNombre().trim().isEmpty()) {
            return "El nombre del medicamento es requerido";
        }

        // Validar presentación
        if (TextUtils.isEmpty(medicamento.getPresentacion())) {
            return "La presentación es requerida";
        }

        // Validar tomas diarias
        if (medicamento.getTomasDiarias() < 0) {
            return "Las tomas diarias no pueden ser negativas";
        }

        // Si tiene tomas diarias, debe tener horario
        if (medicamento.getTomasDiarias() > 0) {
            if (TextUtils.isEmpty(medicamento.getHorarioPrimeraToma())) {
                return "El horario de la primera toma es requerido";
            }

            if (!isValidTime(medicamento.getHorarioPrimeraToma())) {
                return "El horario debe tener el formato HH:mm (ej: 08:00)";
            }
        }

        // Validar afección
        if (TextUtils.isEmpty(medicamento.getAfeccion()) || 
            medicamento.getAfeccion().trim().isEmpty()) {
            return "La afección es requerida";
        }

        // Validar stock
        if (medicamento.getStockInicial() < 0) {
            return "El stock inicial no puede ser negativo";
        }

        if (medicamento.getStockActual() < 0) {
            return "El stock actual no puede ser negativo";
        }

        // Validar días de tratamiento
        if (medicamento.getDiasTratamiento() < -1) {
            return "Los días de tratamiento no pueden ser menores a -1 (crónico)";
        }

        return null; // Válido
    }

    /**
     * Valida un email.
     * 
     * @param email Email a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Valida una contraseña.
     * 
     * @param password Contraseña a validar
     * @return null si es válida, mensaje de error en caso contrario
     */
    public static String validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return "La contraseña es requerida";
        }

        if (password.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres";
        }

        return null; // Válida
    }

    /**
     * Valida un horario en formato HH:mm.
     * 
     * @param time Horario a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidTime(String time) {
        if (TextUtils.isEmpty(time)) {
            return false;
        }
        return TIME_PATTERN.matcher(time.trim()).matches();
    }

    /**
     * Valida un número entero positivo.
     * 
     * @param value Valor a validar
     * @param fieldName Nombre del campo (para mensaje de error)
     * @return null si es válido, mensaje de error en caso contrario
     */
    public static String validatePositiveInteger(int value, String fieldName) {
        if (value < 0) {
            return fieldName + " no puede ser negativo";
        }
        return null;
    }

    /**
     * Valida un número entero positivo mayor a cero.
     * 
     * @param value Valor a validar
     * @param fieldName Nombre del campo (para mensaje de error)
     * @return null si es válido, mensaje de error en caso contrario
     */
    public static String validatePositiveIntegerGreaterThanZero(int value, String fieldName) {
        if (value <= 0) {
            return fieldName + " debe ser mayor a cero";
        }
        return null;
    }

    /**
     * Valida que una cadena no esté vacía.
     * 
     * @param value Cadena a validar
     * @param fieldName Nombre del campo (para mensaje de error)
     * @return null si es válida, mensaje de error en caso contrario
     */
    public static String validateNotEmpty(String value, String fieldName) {
        if (TextUtils.isEmpty(value) || value.trim().isEmpty()) {
            return fieldName + " es requerido";
        }
        return null;
    }
}

