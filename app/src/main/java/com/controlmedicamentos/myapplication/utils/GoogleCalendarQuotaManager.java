package com.controlmedicamentos.myapplication.utils;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gestor de cuotas para Google Calendar API.
 * Maneja los límites de solicitudes para aplicaciones no verificadas.
 * 
 * Límites para apps no verificadas:
 * - 60 requests por minuto por usuario
 * - 1,000,000 requests por día por proyecto (pero puede ser menor)
 * - Los eventos recurrentes (RRULE) son más eficientes que eventos individuales
 */
public class GoogleCalendarQuotaManager {
    
    private static final String TAG = "GoogleCalendarQuota";
    
    // Límites de cuota para apps no verificadas
    public static final int MAX_REQUESTS_PER_MINUTE = 50; // Usar 50 para dejar margen de seguridad
    public static final int MAX_REQUESTS_PER_HOUR = 2000; // Límite conservador
    public static final int MAX_EVENTS_PER_MEDICATION = 10; // Máximo de eventos recurrentes por medicamento
    
    // Tiempo de espera entre solicitudes (en milisegundos)
    public static final long MIN_DELAY_BETWEEN_REQUESTS_MS = 1200; // 1.2 segundos = ~50 requests/minuto
    
    // Contador de solicitudes por usuario (userId -> contador)
    private static final ConcurrentHashMap<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();
    
    /**
     * Contador de solicitudes con ventana de tiempo
     */
    private static class RequestCounter {
        private final AtomicInteger requestsLastMinute = new AtomicInteger(0);
        private final AtomicInteger requestsLastHour = new AtomicInteger(0);
        private final AtomicLong lastMinuteReset = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong lastHourReset = new AtomicLong(System.currentTimeMillis());
        
        /**
         * Verifica si se puede hacer una solicitud
         */
        public boolean canMakeRequest() {
            long now = System.currentTimeMillis();
            
            // Resetear contador del minuto si pasó 1 minuto
            if (now - lastMinuteReset.get() > 60000) {
                requestsLastMinute.set(0);
                lastMinuteReset.set(now);
            }
            
            // Resetear contador de la hora si pasó 1 hora
            if (now - lastHourReset.get() > 3600000) {
                requestsLastHour.set(0);
                lastHourReset.set(now);
            }
            
            // Verificar límites
            if (requestsLastMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            
            if (requestsLastHour.get() >= MAX_REQUESTS_PER_HOUR) {
                return false;
            }
            
            return true;
        }
        
        /**
         * Registra una solicitud
         */
        public void recordRequest() {
            requestsLastMinute.incrementAndGet();
            requestsLastHour.incrementAndGet();
        }
        
        /**
         * Obtiene el tiempo de espera necesario antes de la próxima solicitud
         */
        public long getWaitTimeMs() {
            long now = System.currentTimeMillis();
            long timeSinceLastMinuteReset = now - lastMinuteReset.get();
            
            if (requestsLastMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
                // Esperar hasta que se resetee el contador del minuto
                return 60000 - timeSinceLastMinuteReset + 1000; // +1 segundo de margen
            }
            
            // Si no hay límite inmediato, usar el delay mínimo
            return MIN_DELAY_BETWEEN_REQUESTS_MS;
        }
    }
    
    /**
     * Verifica si se puede hacer una solicitud para un usuario
     * 
     * @param userId ID del usuario
     * @return true si se puede hacer la solicitud, false si se excedió la cuota
     */
    public static boolean canMakeRequest(String userId) {
        if (userId == null || userId.isEmpty()) {
            return true; // Sin usuario, permitir (aunque no debería pasar)
        }
        
        RequestCounter counter = requestCounters.computeIfAbsent(userId, k -> new RequestCounter());
        return counter.canMakeRequest();
    }
    
    /**
     * Registra una solicitud realizada
     * 
     * @param userId ID del usuario
     */
    public static void recordRequest(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        RequestCounter counter = requestCounters.computeIfAbsent(userId, k -> new RequestCounter());
        counter.recordRequest();
    }
    
    /**
     * Obtiene el tiempo de espera necesario antes de la próxima solicitud
     * 
     * @param userId ID del usuario
     * @return Tiempo de espera en milisegundos
     */
    public static long getWaitTimeMs(String userId) {
        if (userId == null || userId.isEmpty()) {
            return MIN_DELAY_BETWEEN_REQUESTS_MS;
        }
        
        RequestCounter counter = requestCounters.computeIfAbsent(userId, k -> new RequestCounter());
        return counter.getWaitTimeMs();
    }
    
    /**
     * Verifica si un error es relacionado con cuota excedida
     * 
     * @param exception La excepción a verificar
     * @return true si es un error de cuota
     */
    public static boolean isQuotaExceededError(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }
        
        String message = exception.getMessage().toLowerCase();
        return message.contains("quota") || 
               message.contains("rate limit") ||
               message.contains("429") ||
               message.contains("403") && message.contains("exceeded");
    }
    
    /**
     * Calcula el tiempo de espera para retry con backoff exponencial
     * 
     * @param attemptNumber Número de intento (1, 2, 3...)
     * @return Tiempo de espera en milisegundos
     */
    public static long calculateBackoffDelay(int attemptNumber) {
        // Backoff exponencial: 2^attemptNumber segundos, máximo 60 segundos
        long delaySeconds = Math.min((long) Math.pow(2, attemptNumber), 60);
        return delaySeconds * 1000;
    }
}

