package com.controlmedicamentos.myapplication.utils;

import android.graphics.Color;

/**
 * Resultado de obtenerEstadoAdherencia(porcentaje): color y mensaje.
 * Paridad con web: 90+ excelente, 70+ buena, 50+ regular, sino baja.
 */
public final class EstadoAdherencia {
    private final int color;
    private final String mensaje;

    public EstadoAdherencia(int color, String mensaje) {
        this.color = color;
        this.mensaje = mensaje;
    }

    public int getColor() {
        return color;
    }

    public String getMensaje() {
        return mensaje;
    }

    /** Colores web: #4CAF50, #8BC34A, #FF9800, #F44336 */
    public static final int COLOR_EXCELENTE = Color.parseColor("#4CAF50");
    public static final int COLOR_BUENA = Color.parseColor("#8BC34A");
    public static final int COLOR_REGULAR = Color.parseColor("#FF9800");
    public static final int COLOR_BAJA = Color.parseColor("#F44336");
}
