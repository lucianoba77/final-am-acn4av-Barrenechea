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
    
    private final OkHttpClient httpClient;
    
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
     * 
     * @deprecated Este método no se usa actualmente. Se mantiene para compatibilidad futura.
     */
    @SuppressWarnings("unused")
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
        
        final int[] eventosPendientes = {eventoIds.size()};
        final List<String> eventosEliminadosList = new ArrayList<>();
        final List<Exception> errores = new ArrayList<>();
        
        for (String eventoId : eventoIds) {
            eliminarEventoToma(accessToken, eventoId, new CalendarCallback() {
                @Override
                public void onSuccess(String eventoId, Object evento) {
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
     * Crea eventos recurrentes para todas las tomas de un medicamento.
     * 
     * ESTRATEGIA: Primero intenta usar RRULE (más eficiente). Si falla, crea eventos individuales.
     * 
     * Límites para apps no verificadas:
     * - 60 requests por minuto por usuario
     * - 1,000,000 requests por día por proyecto
     * - Rate limiting: 1.2 segundos entre requests (~50 requests/minuto)
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
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                () -> crearEventosRecurrentes(accessToken, medicamento, callback), waitTime);
            return;
        }
        
        List<String> eventoIds = new ArrayList<>();
        
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
            horasToma.add(String.format(Locale.US, "%02d:%02d", hora, minuto));
        }
        
        // Limitar número de eventos recurrentes por medicamento
        int maxEventos = Math.min(horasToma.size(), GoogleCalendarQuotaManager.MAX_EVENTS_PER_MEDICATION);
        final int[] eventosCreados = {0};
        final int[] eventosPendientes = {maxEventos};
        final List<Exception> errores = new ArrayList<>();
        
        Logger.d(TAG, String.format(Locale.US,
            "crearEventosRecurrentes: Iniciando creación de %d eventos para medicamento: %s (Tomas diarias: %d, Horarios: %s)",
            maxEventos,
            medicamento.getNombre() != null ? medicamento.getNombre() : "SIN_NOMBRE",
            medicamento.getTomasDiarias(),
            horasToma));
        
        // Crear eventos de forma asíncrona secuencial usando Handler
        // Esto evita bloquear el hilo y permite crear múltiples eventos correctamente
        final int[] indiceActual = {0};
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        // Función recursiva para crear eventos secuencialmente
        // Usar array para permitir referencia dentro de la definición
        final Runnable[] crearSiguienteEventoRef = new Runnable[1];
        Runnable crearSiguienteEvento = new Runnable() {
            @Override
            public void run() {
                if (indiceActual[0] >= maxEventos) {
                    // Ya se crearon todos los eventos
                    return;
                }
                
                final int indice = indiceActual[0];
                final String horaToma = horasToma.get(indice);
                Logger.d(TAG, String.format(Locale.US,
                    "crearEventosRecurrentes: Creando evento %d/%d para hora: %s",
                    indice + 1, maxEventos, horaToma));
                
                crearEventoRecurrente(accessToken, medicamento, horaToma, 
                    new CalendarCallback() {
                        @Override
                        public void onSuccess(String eventoId, Object evento) {
                            Logger.d(TAG, String.format(Locale.US,
                                "crearEventosRecurrentes: Evento creado exitosamente - ID: %s para hora: %s (Creados: %d/%d)",
                                eventoId, horaToma, eventosCreados[0] + 1, maxEventos));
                            eventoIds.add(eventoId);
                            eventosCreados[0]++;
                            eventosPendientes[0]--;
                            GoogleCalendarQuotaManager.recordRequest(userId);
                            
                            Logger.d(TAG, String.format(Locale.US,
                                "crearEventosRecurrentes: Pendientes: %d, Creados: %d",
                                eventosPendientes[0], eventosCreados[0]));
                            
                            // Crear el siguiente evento después de un delay para rate limiting
                            indiceActual[0]++;
                            if (indiceActual[0] < maxEventos) {
                                long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                                handler.postDelayed(crearSiguienteEventoRef[0], waitTime);
                            } else {
                                // Todos los eventos fueron procesados
                                // Verificar si todos los eventos fueron procesados (exitosos o con error)
                                if (eventosCreados[0] + errores.size() >= maxEventos || eventosPendientes[0] == 0) {
                                    Logger.d(TAG, String.format(Locale.US,
                                        "crearEventosRecurrentes: Todos los eventos completados. Total creados: %d, Errores: %d",
                                        eventoIds.size(), errores.size()));
                                    if (callback != null) {
                                        if (errores.isEmpty()) {
                                            callback.onSuccess(eventoIds);
                                        } else {
                                            // Algunos eventos se crearon, otros fallaron
                                            Logger.w(TAG, String.format(Locale.US,
                                                "Algunos eventos recurrentes no se pudieron crear: %d errores de %d eventos",
                                                errores.size(), maxEventos));
                                            callback.onSuccess(eventoIds); // Retornar los que se crearon
                                        }
                                    }
                                }
                            }
                        }
                    
                    @Override
                    public void onError(Exception exception) {
                        Logger.e(TAG, String.format(Locale.US,
                            "crearEventosRecurrentes: Error al crear evento para hora %s (Pendientes: %d/%d)",
                            horaToma, eventosPendientes[0] - 1, maxEventos), exception);
                        
                        // Si es error de RRULE inválida, intentar crear eventos individuales como fallback
                        String errorMsg = exception.getMessage() != null ? exception.getMessage() : "";
                        if (errorMsg.contains("Invalid recurrence rule") || (errorMsg.contains("400") && errorMsg.contains("recurrence"))) {
                            Logger.w(TAG, "Error de RRULE inválida, intentando crear eventos individuales como fallback...");
                            // Usar un callback wrapper para manejar el resultado del fallback
                            crearEventosIndividualesComoFallback(accessToken, medicamento, horaToma, new CalendarCallback() {
                                @Override
                                public void onSuccess(String eventoId, Object evento) {
                                    // El fallback retorna el primer eventoId en el primer parámetro
                                    // y la lista completa de eventoIds en el segundo parámetro (si es una List)
                                    List<String> todosLosEventoIds = new ArrayList<>();
                                    if (eventoId != null) {
                                        todosLosEventoIds.add(eventoId);
                                    }
                                    // Si el segundo parámetro es una List, agregar todos los IDs
                                    if (evento instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<String> eventoIdsList = (List<String>) evento;
                                        todosLosEventoIds.addAll(eventoIdsList);
                                    }
                                    
                                    // Agregar todos los eventoIds a la lista principal
                                    eventoIds.addAll(todosLosEventoIds);
                                    eventosCreados[0] += todosLosEventoIds.size();
                                    eventosPendientes[0]--;
                                    GoogleCalendarQuotaManager.recordRequest(userId);
                                    
                                    Logger.d(TAG, "Fallback: " + todosLosEventoIds.size() + 
                                        " eventos individuales creados. Pendientes: " + eventosPendientes[0]);
                                    
                                    // Continuar con el siguiente evento después del fallback
                                    indiceActual[0]++;
                                    if (indiceActual[0] < maxEventos) {
                                        long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                                        handler.postDelayed(crearSiguienteEventoRef[0], waitTime);
                                    } else {
                                        // Todos los eventos fueron procesados
                                        if (eventosPendientes[0] == 0) {
                                            if (callback != null) {
                                                callback.onSuccess(eventoIds);
                                            }
                                        }
                                    }
                                }
                                
                                @Override
                                public void onError(Exception exception) {
                                    eventosPendientes[0]--;
                                    errores.add(exception);
                                    GoogleCalendarQuotaManager.recordRequest(userId);
                                    
                                    Logger.w(TAG, String.format(Locale.US, "Fallback también falló para %s", horaToma));
                                    
                                    // Continuar con el siguiente evento aunque el fallback falló
                                    indiceActual[0]++;
                                    if (indiceActual[0] < maxEventos) {
                                        long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                                        handler.postDelayed(crearSiguienteEventoRef[0], waitTime);
                                    } else {
                                        // Todos los eventos fueron procesados
                                        if (eventosPendientes[0] == 0) {
                                            if (callback != null) {
                                                if (eventoIds.isEmpty()) {
                                                    callback.onError(new Exception("No se pudieron crear los eventos"));
                                                } else {
                                                    callback.onSuccess(eventoIds);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                            return; // No decrementar contadores aquí, el fallback lo hará
                        }
                        
                        eventosPendientes[0]--;
                        errores.add(exception);
                        GoogleCalendarQuotaManager.recordRequest(userId);
                        
                        // Continuar con el siguiente evento aunque hubo un error
                        indiceActual[0]++;
                        if (indiceActual[0] < maxEventos) {
                            long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                            handler.postDelayed(crearSiguienteEventoRef[0], waitTime);
                        } else {
                            // Todos los eventos fueron procesados
                            if (eventosPendientes[0] == 0) {
                                Logger.d(TAG, String.format(Locale.US,
                                    "crearEventosRecurrentes: Todos los eventos procesados (con errores). Creados: %d, Errores: %d",
                                    eventoIds.size(), errores.size()));
                                if (callback != null) {
                                    if (eventoIds.isEmpty()) {
                                        callback.onError(new Exception("No se pudieron crear los eventos recurrentes"));
                                    } else {
                                    Logger.w(TAG, String.format(Locale.US,
                                        "crearEventosRecurrentes: Retornando %d eventos creados (algunos fallaron)",
                                        eventoIds.size()));
                                        callback.onSuccess(eventoIds); // Retornar los que se crearon
                                    }
                                }
                            }
                        }
                    }
                });
            }
        };
        
        // Asignar la referencia para poder usarla dentro de la definición
        crearSiguienteEventoRef[0] = crearSiguienteEvento;
        
        // Iniciar la creación del primer evento
        handler.post(crearSiguienteEvento);
    }
    
    /**
     * Crea un evento recurrente individual usando RRULE.
     * 
     * @param accessToken Token de acceso
     * @param medicamento El medicamento
     * @param horaToma Hora de la toma (formato HH:mm)
     * @param callback Callback para notificar resultado
     */
    private void crearEventoRecurrente(String accessToken, Medicamento medicamento, 
                                      String horaToma, CalendarCallback callback) {
        try {
            // Parsear hora
            String[] partesHora = horaToma.split(":");
            int hora = Integer.parseInt(partesHora[0]);
            int minuto = Integer.parseInt(partesHora[1]);
            
            // Fecha de inicio: hoy a la hora especificada en zona horaria de Argentina
            java.util.TimeZone tzArgentina = java.util.TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
            Calendar fechaInicio = Calendar.getInstance(tzArgentina);
            fechaInicio.set(Calendar.HOUR_OF_DAY, hora);
            fechaInicio.set(Calendar.MINUTE, minuto);
            fechaInicio.set(Calendar.SECOND, 0);
            fechaInicio.set(Calendar.MILLISECOND, 0);
            
            // Comparar con la hora actual en la misma zona horaria
            Calendar ahora = Calendar.getInstance(tzArgentina);
            
            // Si la hora ya pasó hoy, empezar mañana
            if (fechaInicio.before(ahora)) {
                fechaInicio.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            // Fecha de fin del evento (15 minutos después)
            Calendar fechaFin = (Calendar) fechaInicio.clone();
            fechaFin.add(Calendar.MINUTE, 15);
            
            // Calcular fecha de fin de la recurrencia
            String rrule;
            
            if (medicamento.getDiasTratamiento() == -1) {
                // Medicamento crónico: recurrencia diaria sin fecha de fin
                rrule = "FREQ=DAILY";
                Logger.d(TAG, "RRULE generada: " + rrule + " (medicamento crónico)");
            } else {
                // Medicamento con fin: usar COUNT en lugar de UNTIL para evitar problemas de formato
                // COUNT especifica el número de ocurrencias del evento
                // Si el tratamiento dura N días, habrá N ocurrencias (una por día)
                int count = medicamento.getDiasTratamiento();
                rrule = "FREQ=DAILY;COUNT=" + count;
                Logger.d(TAG, "RRULE generada: " + rrule + " (días tratamiento: " + medicamento.getDiasTratamiento() + 
                    ", count: " + count + ")");
            }
            
            // Usar zona horaria de Argentina (America/Argentina/Buenos_Aires, UTC-3)
            String timeZone = "America/Argentina/Buenos_Aires";
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone(timeZone));
            
            // Verificar que el medicamento tenga nombre
            String nombreMedicamento = medicamento.getNombre();
            if (nombreMedicamento == null || nombreMedicamento.isEmpty()) {
                nombreMedicamento = "Medicamento sin nombre";
                Logger.w(TAG, "crearEventoRecurrente: Medicamento sin nombre, usando valor por defecto");
            }
            
            Logger.d(TAG, "crearEventoRecurrente: Creando evento para medicamento: '" + nombreMedicamento + 
                "' a las " + horaToma);
            
            // Crear objeto JSON del evento recurrente
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + nombreMedicamento);
            evento.put("description", "Toma de " + nombreMedicamento + "\n" +
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
            
            // Agregar recurrencia (RRULE) - El prefijo "RRULE:" es requerido por Google Calendar API
            JSONArray recurrence = new JSONArray();
            recurrence.put("RRULE:" + rrule);
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
            String jsonEvento = evento.toString();
            Logger.d(TAG, String.format(Locale.US,
                "crearEventoRecurrente: JSON del evento a enviar (summary: '%s'): %s",
                evento.optString("summary", "NO_SUMMARY"), jsonEvento));
            RequestBody body = RequestBody.create(jsonEvento, JSON);
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
                        int code = response.code();
                        Log.e(TAG, "Error al crear evento recurrente: " + code + " - " + errorBody);
                        
                        // Si es error 401 (Unauthorized), el token puede estar expirado
                        // No intentar renovar aquí, solo reportar el error
                        Exception exception = new Exception("Error al crear evento recurrente: " + code + " - " + errorBody);
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
     * Crea eventos individuales como fallback cuando las RRULE fallan.
     * Crea un evento por cada día del tratamiento con rate limiting adecuado.
     */
    private void crearEventosIndividualesComoFallback(String accessToken, Medicamento medicamento, 
                                                      String horaToma, CalendarCallback callback) {
        Logger.w(TAG, "crearEventosIndividualesComoFallback: Creando eventos individuales para " + 
            horaToma + " (días: " + medicamento.getDiasTratamiento() + ")");
        
        java.util.TimeZone tzArgentina = java.util.TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
        Calendar fechaInicio = Calendar.getInstance(tzArgentina);
        
        // Parsear hora
        String[] partesHora = horaToma.split(":");
        int hora = Integer.parseInt(partesHora[0]);
        int minuto = Integer.parseInt(partesHora[1]);
        
        fechaInicio.set(Calendar.HOUR_OF_DAY, hora);
        fechaInicio.set(Calendar.MINUTE, minuto);
        fechaInicio.set(Calendar.SECOND, 0);
        fechaInicio.set(Calendar.MILLISECOND, 0);
        
        Calendar ahora = Calendar.getInstance(tzArgentina);
        if (fechaInicio.before(ahora)) {
            fechaInicio.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Calcular número de eventos a crear (final para usar en clase interna)
        final int numEventos = Math.min(
            medicamento.getDiasTratamiento() == -1 ? 365 : medicamento.getDiasTratamiento(),
            100 // Limitar a 100 eventos máximo para no exceder cuota
        );
        
        final int[] eventosCreados = {0};
        final int[] eventosPendientes = {numEventos};
        final List<String> eventoIds = new ArrayList<>();
        final String userId = obtenerUserId();
        final List<Exception> errores = new ArrayList<>();
        
        // Crear eventos de forma asíncrona secuencial usando Handler
        // Esto evita bloquear el hilo principal
        final int[] indiceActual = {0};
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        // Función recursiva para crear eventos secuencialmente
        final Runnable[] crearSiguienteEventoRef = new Runnable[1];
        Runnable crearSiguienteEvento = new Runnable() {
            @Override
            public void run() {
                if (indiceActual[0] >= numEventos) {
                    // Ya se crearon todos los eventos
                    return;
                }
                
                final int dia = indiceActual[0];
                final Calendar fechaEvento = (Calendar) fechaInicio.clone();
                fechaEvento.add(Calendar.DAY_OF_YEAR, dia);
                
                Logger.d(TAG, String.format(Locale.US,
                    "crearEventosIndividualesComoFallback: Creando evento %d/%d para fecha: %s",
                    dia + 1, numEventos,
                    new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(fechaEvento.getTime())));
                
                // Crear evento individual
                crearEventoIndividual(accessToken, medicamento, fechaEvento, new CalendarCallback() {
                    @Override
                    public void onSuccess(String eventoId, Object evento) {
                        eventoIds.add(eventoId);
                        eventosCreados[0]++;
                        eventosPendientes[0]--;
                        GoogleCalendarQuotaManager.recordRequest(userId);
                        
                        Logger.d(TAG, String.format(Locale.US,
                            "crearEventosIndividualesComoFallback: Evento creado exitosamente - ID: %s (Creados: %d/%d)",
                            eventoId, eventosCreados[0], numEventos));
                        
                        // Crear el siguiente evento después de un delay para rate limiting
                        indiceActual[0]++;
                        if (indiceActual[0] < numEventos) {
                            long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                            handler.postDelayed(crearSiguienteEventoRef[0], waitTime);
                        } else {
                            // Todos los eventos fueron procesados
                            if (eventosPendientes[0] == 0) {
                                Logger.d(TAG, "crearEventosIndividualesComoFallback: Todos los eventos completados. Total creados: " + eventoIds.size());
                                if (callback != null) {
                                    if (errores.isEmpty()) {
                                        // Retornar solo el primer evento ID para mantener compatibilidad
                                        callback.onSuccess(eventoIds.isEmpty() ? null : eventoIds.get(0), eventoIds);
                                    } else {
                                        // Algunos eventos se crearon, otros fallaron
                                        Logger.w(TAG, String.format(Locale.US,
                                            "Algunos eventos individuales no se pudieron crear: %d errores de %d eventos",
                                            errores.size(), numEventos));
                                        callback.onSuccess(eventoIds.isEmpty() ? null : eventoIds.get(0), eventoIds);
                                    }
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Exception exception) {
                        eventosPendientes[0]--;
                        errores.add(exception);
                        GoogleCalendarQuotaManager.recordRequest(userId);
                        Logger.w(TAG, "Error al crear evento individual día " + dia, exception);
                        
                        // Continuar con el siguiente evento aunque hubo un error
                        indiceActual[0]++;
                        if (indiceActual[0] < numEventos) {
                            long waitTime = GoogleCalendarQuotaManager.getWaitTimeMs(userId);
                            handler.postDelayed(crearSiguienteEventoRef[0], waitTime);
                        } else {
                            // Todos los eventos fueron procesados
                            if (eventosPendientes[0] == 0) {
                                Logger.d(TAG, String.format(Locale.US,
                                    "crearEventosIndividualesComoFallback: Todos los eventos procesados (con errores). Creados: %d, Errores: %d",
                                    eventoIds.size(), errores.size()));
                                if (callback != null) {
                                    if (eventoIds.isEmpty()) {
                                        callback.onError(new Exception("No se pudieron crear eventos individuales"));
                                    } else {
                                        callback.onSuccess(eventoIds.get(0), eventoIds);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        };
        
        // Asignar la referencia para poder usarla dentro de la definición
        crearSiguienteEventoRef[0] = crearSiguienteEvento;
        
        // Iniciar la creación del primer evento
        handler.post(crearSiguienteEvento);
    }
    
    /**
     * Crea un evento individual (sin recurrencia) para una fecha específica.
     */
    private void crearEventoIndividual(String accessToken, Medicamento medicamento, Calendar fechaEvento, 
                                      CalendarCallback callback) {
        try {
            Calendar fechaFin = (Calendar) fechaEvento.clone();
            fechaFin.add(Calendar.MINUTE, 15);
            
            String timeZone = "America/Argentina/Buenos_Aires";
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone(timeZone));
            
            String nombreMedicamento = medicamento.getNombre();
            if (nombreMedicamento == null || nombreMedicamento.isEmpty()) {
                nombreMedicamento = "Medicamento sin nombre";
            }
            
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + nombreMedicamento);
            evento.put("description", "Toma de " + nombreMedicamento + "\n" +
                       "Presentación: " + medicamento.getPresentacion() + "\n" +
                       "Condición: " + (medicamento.getAfeccion() != null ? medicamento.getAfeccion() : "N/A") + "\n" +
                       "Stock: " + medicamento.getStockActual() + "/" + 
                       (medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : medicamento.getStockInicial()));
            
            JSONObject start = new JSONObject();
            start.put("dateTime", isoFormat.format(fechaEvento.getTime()));
            start.put("timeZone", timeZone);
            evento.put("start", start);
            
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
                    Log.e(TAG, "Error al crear evento individual", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                        Log.e(TAG, "Error al crear evento individual: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onError(new Exception("Error al crear evento: " + errorBody));
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "{}";
                        JSONObject eventoCreado = new JSONObject(responseBody);
                        String eventoId = eventoCreado.getString("id");
                        
                        Log.d(TAG, "Evento individual creado exitosamente: " + eventoId);
                        if (callback != null) {
                            callback.onSuccess(eventoId, eventoCreado);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear respuesta", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al crear evento individual", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Reintenta crear un evento recurrente con backoff exponencial.
     * 
     * @deprecated Este método no se usa actualmente. El retry se maneja automáticamente en el flujo principal.
     */
    @SuppressWarnings("unused")
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
        Logger.d(TAG, String.format(Locale.US,
            "Reintentando crear evento recurrente en %d segundos (intento %d)",
            delay / 1000, attemptNumber));
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            () -> crearEventoRecurrente(accessToken, medicamento, horaToma, callback), delay);
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
        String colorHex = String.format(Locale.US, "#%06X", (0xFFFFFF & colorInt));
        
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

