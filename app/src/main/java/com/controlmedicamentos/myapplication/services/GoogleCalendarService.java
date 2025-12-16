package com.controlmedicamentos.myapplication.services;

import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.utils.Constants;
import com.controlmedicamentos.myapplication.utils.GoogleCalendarQuotaManager;
import com.controlmedicamentos.myapplication.utils.Logger;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Servicio para interactuar con la API de Google Calendar
 * Crea, actualiza y elimina eventos en Google Calendar
 */
public class GoogleCalendarService {
    private static final String TAG = "GoogleCalendarService";
    private static final String CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private OkHttpClient httpClient;
    
    public GoogleCalendarService() {
        this.httpClient = new OkHttpClient();
    }
    
    /**
     * Crea un evento en Google Calendar para una toma de medicamento
     * Consistente con React: calendarService.js - crearEventoToma()
     */
    public void crearEventoToma(String accessToken, Medicamento medicamento, String fecha, String hora, 
                                CalendarCallback callback) {
        try {
            Calendar fechaCompleta = Calendar.getInstance();
            String[] partesFecha = fecha.split("-");
            String[] partesHora = hora.split(":");
            
            if (partesFecha.length == 3 && partesHora.length == 2) {
                fechaCompleta.set(
                    Integer.parseInt(partesFecha[0]),
                    Integer.parseInt(partesFecha[1]) - 1,
                    Integer.parseInt(partesFecha[2]),
                    Integer.parseInt(partesHora[0]),
                    Integer.parseInt(partesHora[1]),
                    0
                );
            } else {
                if (callback != null) {
                    callback.onError(new Exception("Formato de fecha u hora inválido"));
                }
                return;
            }
            
            Calendar fechaFin = (Calendar) fechaCompleta.clone();
            fechaFin.add(Calendar.MINUTE, Constants.DURACION_EVENTO_CALENDAR_MINUTOS);
            
            // Obtener zona horaria del dispositivo
            String timeZone = java.util.TimeZone.getDefault().getID();
            
            // Crear objeto JSON del evento
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + medicamento.getNombre());
            evento.put("description", "Toma de " + medicamento.getNombre() + "\n" +
                       "Presentación: " + medicamento.getPresentacion() + "\n" +
                       "Condición: " + (medicamento.getAfeccion() != null ? medicamento.getAfeccion() : "N/A") + "\n" +
                       "Stock: " + medicamento.getStockActual() + "/" + 
                       (medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : medicamento.getStockInicial()));
            
            // Fecha inicio
            JSONObject start = new JSONObject();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            start.put("dateTime", isoFormat.format(fechaCompleta.getTime()));
            start.put("timeZone", timeZone);
            evento.put("start", start);
            
            // Fecha fin
            JSONObject end = new JSONObject();
            end.put("dateTime", isoFormat.format(fechaFin.getTime()));
            end.put("timeZone", timeZone);
            evento.put("end", end);
            
            // Recordatorios
            JSONObject reminders = new JSONObject();
            reminders.put("useDefault", false);
            JSONArray overrides = new JSONArray();
            JSONObject reminder1 = new JSONObject();
            reminder1.put("method", "popup");
            reminder1.put("minutes", Constants.RECORDATORIO_CALENDAR_MINUTOS);
            overrides.put(reminder1);
            JSONObject reminder2 = new JSONObject();
            reminder2.put("method", "popup");
            reminder2.put("minutes", 5); // Recordatorio 5 min antes
            overrides.put(reminder2);
            reminders.put("overrides", overrides);
            evento.put("reminders", reminders);
            
            // Color
            evento.put("colorId", obtenerColorId(medicamento.getColor()));
            
            // Propiedades extendidas
            JSONObject extendedProperties = new JSONObject();
            JSONObject privateProps = new JSONObject();
            privateProps.put("medicamentoId", medicamento.getId());
            privateProps.put("tipo", "toma_medicamento");
            extendedProperties.put("private", privateProps);
            evento.put("extendedProperties", extendedProperties);
            
            // Crear request
            RequestBody body = RequestBody.create(evento.toString(), JSON);
            Request request = new Request.Builder()
                .url(CALENDAR_API_BASE_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            // Ejecutar request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error al crear evento en Google Calendar", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                        Log.e(TAG, "Error al crear evento: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onError(new Exception("Error al crear evento: " + errorBody));
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "{}";
                        JSONObject eventoCreado = new JSONObject(responseBody);
                        String eventoId = eventoCreado.getString("id");
                        
                        Log.d(TAG, "Evento creado exitosamente en Google Calendar: " + eventoId);
                        if (callback != null) {
                            callback.onSuccess(eventoId, eventoCreado);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear respuesta de Google Calendar", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al crear evento en Google Calendar", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Actualiza un evento existente en Google Calendar
     * Consistente con React: calendarService.js - actualizarEventoToma()
     */
    public void actualizarEventoToma(String accessToken, String eventoId, Medicamento medicamento, 
                                     String fecha, String hora, CalendarCallback callback) {
        try {
            Calendar fechaCompleta = Calendar.getInstance();
            String[] partesFecha = fecha.split("-");
            String[] partesHora = hora.split(":");
            
            if (partesFecha.length == 3 && partesHora.length == 2) {
                fechaCompleta.set(
                    Integer.parseInt(partesFecha[0]),
                    Integer.parseInt(partesFecha[1]) - 1,
                    Integer.parseInt(partesFecha[2]),
                    Integer.parseInt(partesHora[0]),
                    Integer.parseInt(partesHora[1]),
                    0
                );
            } else {
                if (callback != null) {
                    callback.onError(new Exception("Formato de fecha u hora inválido"));
                }
                return;
            }
            
            Calendar fechaFin = (Calendar) fechaCompleta.clone();
            fechaFin.add(Calendar.MINUTE, 15);
            
            String timeZone = java.util.TimeZone.getDefault().getID();
            
            // Crear objeto JSON del evento
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + medicamento.getNombre());
            evento.put("description", "Toma de " + medicamento.getNombre() + "\n" +
                       "Presentación: " + medicamento.getPresentacion() + "\n" +
                       "Condición: " + (medicamento.getAfeccion() != null ? medicamento.getAfeccion() : "N/A") + "\n" +
                       "Stock: " + medicamento.getStockActual() + "/" + 
                       (medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : medicamento.getStockInicial()));
            
            JSONObject start = new JSONObject();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            start.put("dateTime", isoFormat.format(fechaCompleta.getTime()));
            start.put("timeZone", timeZone);
            evento.put("start", start);
            
            JSONObject end = new JSONObject();
            end.put("dateTime", isoFormat.format(fechaFin.getTime()));
            end.put("timeZone", timeZone);
            evento.put("end", end);
            
            JSONObject reminders = new JSONObject();
            reminders.put("useDefault", false);
            JSONArray overrides = new JSONArray();
            JSONObject reminder1 = new JSONObject();
            reminder1.put("method", "popup");
            reminder1.put("minutes", 15);
            overrides.put(reminder1);
            JSONObject reminder2 = new JSONObject();
            reminder2.put("method", "popup");
            reminder2.put("minutes", 5);
            overrides.put(reminder2);
            reminders.put("overrides", overrides);
            evento.put("reminders", reminders);
            
            evento.put("colorId", obtenerColorId(medicamento.getColor()));
            
            // Crear request
            RequestBody body = RequestBody.create(evento.toString(), JSON);
            Request request = new Request.Builder()
                .url(CALENDAR_API_BASE_URL + "/" + eventoId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .put(body)
                .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error al actualizar evento en Google Calendar", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                        Log.e(TAG, "Error al actualizar evento: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onError(new Exception("Error al actualizar evento: " + errorBody));
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "{}";
                        JSONObject eventoActualizado = new JSONObject(responseBody);
                        
                        Log.d(TAG, "Evento actualizado exitosamente en Google Calendar");
                        if (callback != null) {
                            callback.onSuccess(eventoId, eventoActualizado);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear respuesta de Google Calendar", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar evento en Google Calendar", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Elimina un evento de Google Calendar
     * Consistente con React: calendarService.js - eliminarEventoToma()
     */
    public void eliminarEventoToma(String accessToken, String eventoId, CalendarCallback callback) {
        Request request = new Request.Builder()
            .url(CALENDAR_API_BASE_URL + "/" + eventoId)
            .addHeader("Authorization", "Bearer " + accessToken)
            .delete()
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error al eliminar evento de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() && response.code() != 404) {
                    String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                    Log.e(TAG, "Error al eliminar evento: " + response.code() + " - " + errorBody);
                    if (callback != null) {
                        callback.onError(new Exception("Error al eliminar evento: " + errorBody));
                    }
                    return;
                }
                
                Log.d(TAG, "Evento eliminado exitosamente de Google Calendar: " + eventoId);
                if (callback != null) {
                    callback.onSuccess(eventoId, null);
                }
            }
        });
    }
    
    /**
     * Elimina múltiples eventos de Google Calendar
     * 
     * @param accessToken Token de acceso de Google Calendar
     * @param eventoIds Lista de IDs de eventos a eliminar
     * @param callback Callback para notificar el resultado
     */
    public void eliminarEventos(String accessToken, List<String> eventoIds, MultipleEventsCallback callback) {
        if (eventoIds == null || eventoIds.isEmpty()) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }
        
        final int[] eventosEliminados = {0};
        final int[] eventosPendientes = {eventoIds.size()};
        final List<String> eventosEliminadosList = new ArrayList<>();
        final List<Exception> errores = new ArrayList<>();
        
        for (String eventoId : eventoIds) {
            eliminarEventoToma(accessToken, eventoId, new CalendarCallback() {
                @Override
                public void onSuccess(String eventoId, Object evento) {
                    eventosEliminados[0]++;
                    eventosEliminadosList.add(eventoId);
                    eventosPendientes[0]--;
                    
                    if (eventosPendientes[0] == 0) {
                        if (callback != null) {
                            if (errores.isEmpty()) {
                                callback.onSuccess(eventosEliminadosList);
                            } else {
                                // Algunos eventos se eliminaron, otros fallaron
                                callback.onPartialSuccess(eventosEliminadosList, errores);
                            }
                        }
                    }
                }
                
                @Override
                public void onError(Exception exception) {
                    eventosPendientes[0]--;
                    errores.add(exception);
                    Log.w(TAG, "Error al eliminar evento " + eventoId, exception);
                    
                    if (eventosPendientes[0] == 0) {
                        if (callback != null) {
                            if (eventosEliminadosList.isEmpty()) {
                                // Todos fallaron
                                callback.onError(new Exception("No se pudieron eliminar los eventos"));
                            } else {
                                // Algunos se eliminaron, otros fallaron
                                callback.onPartialSuccess(eventosEliminadosList, errores);
                            }
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Crea eventos recurrentes para todas las tomas de un medicamento usando RRULE.
     * Para medicamentos crónicos, crea eventos recurrentes sin fecha de fin.
     * Para medicamentos con fin, crea eventos recurrentes hasta la fecha de fin.
     * 
     * IMPORTANTE: Usa eventos recurrentes reales (RRULE) en lugar de eventos individuales
     * para respetar los límites de cuota de Google Calendar API para apps no verificadas.
     * 
     * Límites respetados:
     * - Máximo 10 eventos recurrentes por medicamento (uno por cada horario de toma)
     * - Rate limiting: 50 requests por minuto
     * - Retry con backoff exponencial para errores de cuota
     */
    public void crearEventosRecurrentes(String accessToken, Medicamento medicamento, 
                                       RecurrentEventsCallback callback) {
        // No crear eventos para medicamentos ocasionales
        if (medicamento.getTomasDiarias() == 0) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }
        
        // Obtener userId para rate limiting
        String userId = obtenerUserId();
        
        // Verificar cuota antes de proceder
        if (!GoogleCalendarQuotaManager.canMakeRequest(userId)) {
            long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
            Logger.w(TAG, "Cuota excedida, esperando " + (waitTime / 1000) + " segundos");
            
            // Retry después del tiempo de espera
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                crearEventosRecurrentes(accessToken, medicamento, callback);
            }, waitTime);
            return;
        }
        
        List<String> eventoIds = new ArrayList<>();
        Calendar fechaHoy = Calendar.getInstance();
        
        // Calcular todas las horas de toma
        List<String> horasToma = new ArrayList<>();
        String primeraToma = medicamento.getHorarioPrimeraToma();
        if (primeraToma == null || primeraToma.isEmpty()) {
            primeraToma = Constants.HORARIO_INVALIDO;
        }
        
        String[] partes = primeraToma.split(":");
        int horaInicial = Integer.parseInt(partes[0]);
        int minutoInicial = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
        
        int intervalo = 24 / medicamento.getTomasDiarias();
        for (int i = 0; i < medicamento.getTomasDiarias(); i++) {
            int hora = (horaInicial + (i * intervalo)) % 24;
            int minuto = (i == 0) ? minutoInicial : 0;
            horasToma.add(String.format("%02d:%02d", hora, minuto));
        }
        
        // Limitar número de eventos recurrentes por medicamento
        int maxEventos = Math.min(horasToma.size(), GoogleCalendarQuotaManager.MAX_EVENTS_PER_MEDICATION);
        final int[] eventosCreados = {0};
        final int[] eventosPendientes = {maxEventos};
        final List<Exception> errores = new ArrayList<>();
        
        // Crear un evento recurrente por cada horario de toma
        for (int i = 0; i < maxEventos; i++) {
            String horaToma = horasToma.get(i);
            
            // Esperar entre solicitudes para respetar rate limiting
            if (i > 0) {
                long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            crearEventoRecurrente(accessToken, medicamento, horaToma, 
                new CalendarCallback() {
                    @Override
                    public void onSuccess(String eventoId, Object evento) {
                        eventoIds.add(eventoId);
                        eventosCreados[0]++;
                        eventosPendientes[0]--;
                        GoogleCalendarQuotaManager.recordRequest(userId);
                        
                        if (eventosPendientes[0] == 0) {
                            if (callback != null) {
                                if (errores.isEmpty()) {
                                    callback.onSuccess(eventoIds);
                                } else {
                                    // Algunos eventos se crearon, otros fallaron
                                    Logger.w(TAG, "Algunos eventos recurrentes no se pudieron crear: " + errores.size());
                                    callback.onSuccess(eventoIds); // Retornar los que se crearon
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Exception exception) {
                        eventosPendientes[0]--;
                        errores.add(exception);
                        GoogleCalendarQuotaManager.recordRequest(userId);
                        
                        // Si es error de cuota, intentar retry con backoff
                        if (GoogleCalendarQuotaManager.isQuotaExceededError(exception)) {
                            Logger.w(TAG, "Error de cuota al crear evento recurrente, reintentando...");
                            reintentarCrearEventoRecurrente(accessToken, medicamento, horaToma, 1, this);
                        } else {
                            Logger.w(TAG, "Error al crear evento recurrente para " + horaToma, exception);
                        }
                        
                        if (eventosPendientes[0] == 0) {
                            if (callback != null) {
                                if (eventoIds.isEmpty()) {
                                    callback.onError(new Exception("No se pudieron crear los eventos recurrentes"));
                                } else {
                                    callback.onSuccess(eventoIds); // Retornar los que se crearon
                                }
                            }
                        }
                    }
                }, 0); // Primer intento
        }
    }
    
    /**
     * Crea un evento recurrente individual usando RRULE.
     * 
     * @param accessToken Token de acceso
     * @param medicamento El medicamento
     * @param horaToma Hora de la toma (formato HH:mm)
     * @param callback Callback para notificar resultado
     * @param retryAttempt Número de intento (para backoff exponencial)
     */
    private void crearEventoRecurrente(String accessToken, Medicamento medicamento, 
                                      String horaToma, CalendarCallback callback, int retryAttempt) {
        try {
            // Parsear hora
            String[] partesHora = horaToma.split(":");
            int hora = Integer.parseInt(partesHora[0]);
            int minuto = Integer.parseInt(partesHora[1]);
            
            // Fecha de inicio: hoy a la hora especificada
            Calendar fechaInicio = Calendar.getInstance();
            fechaInicio.set(Calendar.HOUR_OF_DAY, hora);
            fechaInicio.set(Calendar.MINUTE, minuto);
            fechaInicio.set(Calendar.SECOND, 0);
            fechaInicio.set(Calendar.MILLISECOND, 0);
            
            // Si la hora ya pasó hoy, empezar mañana
            if (fechaInicio.before(Calendar.getInstance())) {
                fechaInicio.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            // Fecha de fin del evento (15 minutos después)
            Calendar fechaFin = (Calendar) fechaInicio.clone();
            fechaFin.add(Calendar.MINUTE, 15);
            
            // Calcular fecha de fin de la recurrencia
            Calendar fechaFinRecurrencia = null;
            String rrule;
            
            if (medicamento.getDiasTratamiento() == -1) {
                // Medicamento crónico: recurrencia diaria sin fecha de fin
                rrule = "FREQ=DAILY;INTERVAL=1";
            } else {
                // Medicamento con fin: recurrencia diaria hasta fecha de fin
                fechaFinRecurrencia = (Calendar) fechaInicio.clone();
                fechaFinRecurrencia.add(Calendar.DAY_OF_YEAR, medicamento.getDiasTratamiento());
                
                SimpleDateFormat rfcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
                rfcFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                String until = rfcFormat.format(fechaFinRecurrencia.getTime());
                
                rrule = "FREQ=DAILY;INTERVAL=1;UNTIL=" + until;
            }
            
            String timeZone = java.util.TimeZone.getDefault().getID();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            
            // Crear objeto JSON del evento recurrente
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + medicamento.getNombre());
            evento.put("description", "Toma de " + medicamento.getNombre() + "\n" +
                       "Presentación: " + medicamento.getPresentacion() + "\n" +
                       "Condición: " + (medicamento.getAfeccion() != null ? medicamento.getAfeccion() : "N/A") + "\n" +
                       "Stock: " + medicamento.getStockActual() + "/" + 
                       (medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : medicamento.getStockInicial()));
            
            JSONObject start = new JSONObject();
            start.put("dateTime", isoFormat.format(fechaInicio.getTime()));
            start.put("timeZone", timeZone);
            evento.put("start", start);
            
            JSONObject end = new JSONObject();
            end.put("dateTime", isoFormat.format(fechaFin.getTime()));
            end.put("timeZone", timeZone);
            evento.put("end", end);
            
            // Agregar recurrencia (RRULE)
            JSONArray recurrence = new JSONArray();
            recurrence.put(rrule);
            evento.put("recurrence", recurrence);
            
            // Recordatorios
            JSONObject reminders = new JSONObject();
            reminders.put("useDefault", false);
            JSONArray overrides = new JSONArray();
            JSONObject reminder1 = new JSONObject();
            reminder1.put("method", "popup");
            reminder1.put("minutes", 15);
            overrides.put(reminder1);
            JSONObject reminder2 = new JSONObject();
            reminder2.put("method", "popup");
            reminder2.put("minutes", 5);
            overrides.put(reminder2);
            reminders.put("overrides", overrides);
            evento.put("reminders", reminders);
            
            evento.put("colorId", obtenerColorId(medicamento.getColor()));
            
            // Crear request
            RequestBody body = RequestBody.create(evento.toString(), JSON);
            Request request = new Request.Builder()
                .url(CALENDAR_API_BASE_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error al crear evento recurrente en Google Calendar", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                        Log.e(TAG, "Error al crear evento recurrente: " + response.code() + " - " + errorBody);
                        
                        Exception exception = new Exception("Error al crear evento recurrente: " + errorBody);
                        if (callback != null) {
                            callback.onError(exception);
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "{}";
                        JSONObject eventoCreado = new JSONObject(responseBody);
                        String eventoId = eventoCreado.getString("id");
                        
                        Log.d(TAG, "Evento recurrente creado exitosamente en Google Calendar: " + eventoId);
                        if (callback != null) {
                            callback.onSuccess(eventoId, eventoCreado);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear respuesta de Google Calendar", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al crear evento recurrente en Google Calendar", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Reintenta crear un evento recurrente con backoff exponencial.
     */
    private void reintentarCrearEventoRecurrente(String accessToken, Medicamento medicamento, 
                                                 String horaToma, int attemptNumber, CalendarCallback callback) {
        if (attemptNumber > 3) {
            // Máximo 3 reintentos
            Logger.w(TAG, "Máximo de reintentos alcanzado para crear evento recurrente");
            if (callback != null) {
                callback.onError(new Exception("No se pudo crear el evento después de múltiples intentos"));
            }
            return;
        }
        
        long delay = GoogleCalendarQuotaManager.calculateBackoffDelay(attemptNumber);
        Logger.d(TAG, "Reintentando crear evento recurrente en " + (delay / 1000) + " segundos (intento " + attemptNumber + ")");
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            crearEventoRecurrente(accessToken, medicamento, horaToma, callback, attemptNumber);
        }, delay);
    }
    
    /**
     * Obtiene el ID del usuario actual para rate limiting.
     */
    private String obtenerUserId() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null ? user.getUid() : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }
    
    /**
     * Convierte el color del medicamento a un colorId de Google Calendar
     * Consistente con React: calendarService.js - obtenerColorId()
     */
    private String obtenerColorId(int colorInt) {
        // Convertir color ARGB a hexadecimal
        String colorHex = String.format("#%06X", (0xFFFFFF & colorInt));
        
        // Mapeo de colores hex a colorId de Google Calendar (1-11)
        switch (colorHex.toUpperCase()) {
            case "#FFFFFF":
                return "1"; // Lavanda
            case "#FFB6C1":
                return "11"; // Rosa
            case "#ADD8E6":
                return "9"; // Azul
            case "#F5F5DC":
                return "5"; // Amarillo
            case "#E6E6FA":
                return "3"; // Púrpura
            case "#90EE90":
                return "10"; // Verde
            case "#FFFF00":
                return "5"; // Amarillo
            case "#FFA500":
                return "6"; // Naranja
            case "#800080":
                return "3"; // Púrpura
            case "#00BFFF":
                return "9"; // Azul
            case "#00FF00":
                return "10"; // Verde
            case "#FF0000":
                return "11"; // Rojo
            default:
                return "1"; // Por defecto
        }
    }
    
    /**
     * Interfaz para callbacks de operaciones de calendario
     */
    public interface CalendarCallback {
        void onSuccess(String eventoId, Object evento);
        void onError(Exception exception);
    }
    
    /**
     * Interfaz para callbacks de eventos recurrentes
     */
    public interface RecurrentEventsCallback {
        void onSuccess(List<String> eventoIds);
        void onError(Exception exception);
    }
    
    /**
     * Interfaz para callbacks de múltiples eventos
     */
    public interface MultipleEventsCallback {
        void onSuccess(List<String> eventoIdsEliminados);
        void onPartialSuccess(List<String> eventoIdsEliminados, List<Exception> errores);
        void onError(Exception exception);
    }
}

