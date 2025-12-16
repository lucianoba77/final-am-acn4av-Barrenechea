package com.controlmedicamentos.myapplication.utils;

/**
 * Clase de constantes centralizadas para evitar strings mágicos y valores hardcodeados
 * en el código. Facilita el mantenimiento y la consistencia de la aplicación.
 */
public class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // ==================== HORARIOS ====================
    
    /**
     * Horario inválido por defecto. Se usa cuando un medicamento no tiene
     * un horario válido configurado.
     */
    public static final String HORARIO_INVALIDO = "00:00";

    // ==================== POSPOSICIONES ====================
    
    /**
     * Número máximo de posposiciones permitidas para una toma.
     * Cada posposición agrega 10 minutos al horario original.
     */
    public static final int MAX_POSPOSICIONES = 3;
    
    /**
     * Minutos que se agregan al horario cuando se pospone una toma.
     */
    public static final int MINUTOS_POSPOSICION = 10;

    // ==================== ALERTAS ====================
    
    /**
     * Minutos antes del horario programado para activar la alerta amarilla.
     */
    public static final int MINUTOS_ALERTA_AMARILLA = 10;
    
    /**
     * Minutos después del horario programado para considerar la toma en retraso.
     */
    public static final int MINUTOS_RETRASO = 10;
    
    /**
     * Horas después del horario programado para considerar la toma omitida.
     */
    public static final int HORAS_OMITIDA = 1;

    // ==================== SHARED PREFERENCES KEYS ====================
    
    /**
     * Clave para almacenar las tomas programadas en SharedPreferences.
     */
    public static final String PREF_TOMAS_PROGRAMADAS = "tomas_programadas";
    
    /**
     * Clave para almacenar las posposiciones en SharedPreferences.
     */
    public static final String PREF_POSPOSICIONES = "posposiciones";
    
    /**
     * Clave para almacenar los días de antelación para alertas de stock.
     */
    public static final String PREF_DIAS_ANTELACION_STOCK = "dias_antelacion_stock";
    
    /**
     * Clave para almacenar el número de repeticiones de alarmas.
     */
    public static final String PREF_REPETICIONES_ALARMA = "repeticiones";
    
    /**
     * Nombre del archivo de SharedPreferences principal.
     */
    public static final String PREF_NAME = "ControlMedicamentos";

    // ==================== STOCK ====================
    
    /**
     * Días por defecto de antelación para alertas de stock bajo.
     */
    public static final int DIAS_ANTELACION_STOCK_DEFAULT = 3;
    
    /**
     * Días mínimos restantes para activar alerta crítica de stock.
     */
    public static final int DIAS_STOCK_CRITICO = 3;
    
    /**
     * Días mínimos restantes para activar alerta de stock bajo.
     */
    public static final int DIAS_STOCK_BAJO = 7;

    // ==================== TRATAMIENTOS ====================
    
    /**
     * Valor que indica que un tratamiento es crónico (sin fecha de fin).
     */
    public static final int TRATAMIENTO_CRONICO = -1;

    // ==================== NOTIFICACIONES ====================
    
    /**
     * Número de repeticiones por defecto para las alarmas de notificaciones.
     */
    public static final int REPETICIONES_ALARMA_DEFAULT = 3;
    
    /**
     * Tipo de notificación: Alerta amarilla (10 minutos antes).
     */
    public static final int TIPO_ALERTA_AMARILLA = 1;
    
    /**
     * Tipo de notificación: Alerta roja (horario exacto).
     */
    public static final int TIPO_ALERTA_ROJA = 2;

    // ==================== LÍMITES Y VALIDACIONES ====================
    
    /**
     * Límite máximo de tomas que se pueden obtener de Firestore en una consulta.
     */
    public static final int LIMITE_TOMAS_FIRESTORE = 500;
    
    /**
     * Límite máximo de tomas por medicamento en una consulta.
     */
    public static final int LIMITE_TOMAS_POR_MEDICAMENTO = 200;
    
    /**
     * Tiempo de espera en milisegundos antes de actualizar la UI después de una operación.
     */
    public static final int DELAY_ACTUALIZACION_UI_MS = 300;
    
    // ==================== ANIMACIONES Y UI ====================
    
    /**
     * Intervalo de parpadeo en milisegundos para barras de alerta roja.
     */
    public static final int INTERVALO_PARPADEO_MS = 500;
    
    // ==================== GOOGLE CALENDAR ====================
    
    /**
     * Duración en minutos de los eventos de Google Calendar para tomas de medicamentos.
     */
    public static final int DURACION_EVENTO_CALENDAR_MINUTOS = 15;
    
    /**
     * Minutos antes del evento para el recordatorio de Google Calendar.
     */
    public static final int RECORDATORIO_CALENDAR_MINUTOS = 15;
    
    // ==================== NOTIFICACIONES Y VIBRACIÓN ====================
    
    /**
     * Patrón de vibración estándar: esperar 0ms, vibrar 500ms, esperar 500ms, vibrar 500ms.
     */
    public static final long[] PATRON_VIBRACION_ESTANDAR = {0, 500, 500, 500};
    
    /**
     * Patrón de vibración intenso: esperar 0ms, vibrar 500ms, esperar 200ms, vibrar 500ms, esperar 200ms, vibrar 500ms.
     */
    public static final long[] PATRON_VIBRACION_INTENSO = {0, 500, 200, 500, 200, 500};
    
    // ==================== STOCK POR DEFECTO ====================
    
    /**
     * Stock inicial por defecto para comprimidos/cápsulas.
     */
    public static final int STOCK_DEFAULT_COMPRIMIDOS = 30;
    
    /**
     * Días estimados de duración por defecto para jarabes/inyecciones.
     */
    public static final int DIAS_DEFAULT_JARABE = 15;
    
    /**
     * Días estimados de duración por defecto para cremas/pomadas/parches.
     */
    public static final int DIAS_DEFAULT_CREMA = 20;
    
    /**
     * Días estimados de duración por defecto para sprays/gotas.
     */
    public static final int DIAS_DEFAULT_SPRAY = 10;
    
    // ==================== ALARMAS ====================
    
    /**
     * Días máximos para programar alarmas futuras (para evitar exceder límite de 500 alarmas).
     */
    public static final int DIAS_MAXIMOS_ALARMAS = 30;
    
    /**
     * Límite máximo de alarmas concurrentes de Android.
     */
    public static final int LIMITE_ALARMAS_ANDROID = 500;
    
    // ==================== PARSING Y VALIDACIÓN ====================
    
    /**
     * Formato de fecha para caché: YYYY-MM-DD
     */
    public static final String FORMATO_FECHA_CACHE = "yyyy-MM-dd";
    
    /**
     * Formato de hora: HH:mm
     */
    public static final String FORMATO_HORA = "HH:mm";
    
    /**
     * Separador de hora y minutos en formato HH:mm
     */
    public static final String SEPARADOR_HORA = ":";
    
    /**
     * Número mínimo de partes esperadas al dividir un horario (hora:minuto = 2 partes).
     */
    public static final int PARTES_HORARIO_ESPERADAS = 2;
}

