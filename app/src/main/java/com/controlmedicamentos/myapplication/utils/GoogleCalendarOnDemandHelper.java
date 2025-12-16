package com.controlmedicamentos.myapplication.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import androidx.browser.customtabs.CustomTabsIntent;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarService;
import com.google.firebase.firestore.DocumentSnapshot;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper para autorización OAuth bajo demanda de Google Calendar
 * No guarda tokens permanentemente, solo los usa temporalmente para crear/eliminar eventos
 * Usa SharedPreferences para guardar acciones pendientes entre Activities
 */
public class GoogleCalendarOnDemandHelper {
    private static final String TAG = "GoogleCalendarOnDemand";
    private static final String PREFS_NAME = "GoogleCalendarOnDemand";
    private static final String KEY_PENDING_ACTION = "pending_action";
    private static final String KEY_PENDING_MEDICAMENTO_ID = "pending_medicamento_id";
    private static final String KEY_PENDING_EVENTO_IDS = "pending_evento_ids";
    private static final String KEY_PENDING_ELIMINAR_MEDICAMENTO = "pending_eliminar_medicamento";
    
    private final Activity activity;
    private final GoogleCalendarService googleCalendarService;
    private final FirebaseService firebaseService;
    private final SharedPreferences prefs;
    
    public GoogleCalendarOnDemandHelper(Activity activity) {
        this.activity = activity;
        this.googleCalendarService = new GoogleCalendarService();
        this.firebaseService = new FirebaseService();
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
    }
    
    /**
     * Pregunta al usuario si quiere agregar eventos a Google Calendar
     * Si acepta, inicia el flujo OAuth
     */
    public void preguntarYCrearEventos(Medicamento medicamento) {
        new android.app.AlertDialog.Builder(activity)
            .setTitle("Agregar a Google Calendar")
            .setMessage("¿Deseas agregar los recordatorios de este medicamento a tu Google Calendar?")
            .setPositiveButton("Sí, agregar", (dialog, which) -> {
                // Guardar acción pendiente
                guardarAccionPendiente("create", medicamento.getId(), null);
                // Iniciar OAuth
                iniciarOAuth("create");
            })
            .setNegativeButton("No", (dialog, which) -> {
                Toast.makeText(activity, 
                    "Medicamento guardado sin eventos de calendario", 
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    /**
     * Pregunta al usuario si quiere eliminar eventos de Google Calendar
     * Si acepta, inicia el flujo OAuth
     * @param medicamentoId ID del medicamento que se está eliminando
     * @param eventoIds Lista de IDs de eventos a eliminar
     * @param onComplete Callback que se ejecuta cuando se completa (ya sea eliminando eventos o no)
     */
    public void preguntarYEliminarEventos(String medicamentoId, List<String> eventoIds, Runnable onComplete) {
        if (eventoIds == null || eventoIds.isEmpty()) {
            // No hay eventos, ejecutar callback directamente
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        new android.app.AlertDialog.Builder(activity)
            .setTitle("Eliminar de Google Calendar")
            .setMessage("Este medicamento tiene eventos en Google Calendar. ¿Deseas eliminarlos también?")
            .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                // Guardar que se debe eliminar el medicamento después
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_PENDING_ELIMINAR_MEDICAMENTO, medicamentoId);
                editor.apply();
                
                // Guardar acción pendiente
                guardarAccionPendiente("delete", medicamentoId, eventoIds);
                // Iniciar OAuth
                iniciarOAuth("delete");
            })
            .setNegativeButton("No", (dialog, which) -> {
                Toast.makeText(activity, 
                    "Los eventos permanecerán en tu calendario", 
                    Toast.LENGTH_SHORT).show();
                // Ejecutar callback directamente sin eliminar eventos
                if (onComplete != null) {
                    onComplete.run();
                }
            })
            .show();
    }
    
    /**
     * Inicia el flujo OAuth 2.0 implícito
     */
    private void iniciarOAuth(String action) {
        try {
            String clientId = activity.getString(activity.getResources()
                .getIdentifier("default_web_client_id", "string", activity.getPackageName()));
            
            if (clientId == null || clientId.isEmpty()) {
                Toast.makeText(activity, 
                    "Error: Client ID no configurado", 
                    Toast.LENGTH_LONG).show();
                limpiarAccionPendiente();
                return;
            }
            
            String redirectUri = "com.controlmedicamentos.myapplication://googlecalendar";
            String scope = "https://www.googleapis.com/auth/calendar.events";
            
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8") +
                "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8") +
                "&response_type=token" +
                "&scope=" + java.net.URLEncoder.encode(scope, "UTF-8") +
                "&include_granted_scopes=true" +
                "&state=" + java.net.URLEncoder.encode(action, "UTF-8");
            
            Log.d(TAG, "Iniciando OAuth para acción: " + action);
            
            Uri uri = Uri.parse(authUrl);
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(activity, uri);
            
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar OAuth", e);
            Toast.makeText(activity, 
                "Error al conectar con Google Calendar: " + 
                (e.getMessage() != null ? e.getMessage() : "Error desconocido"),
                Toast.LENGTH_LONG).show();
            limpiarAccionPendiente();
        }
    }
    
    /**
     * Procesa el callback de OAuth y ejecuta la acción pendiente
     * Llamado desde GoogleCalendarCallbackActivity
     */
    public static void procesarCallbackOAuth(Activity activity, String accessToken, String state) {
        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token vacío en callback");
            Toast.makeText(activity, 
                "No se pudo obtener autorización de Google Calendar", 
                Toast.LENGTH_LONG).show();
            regresarAActividadAnterior(activity);
            return;
        }
        
        GoogleCalendarOnDemandHelper helper = new GoogleCalendarOnDemandHelper(activity);
        String action = helper.obtenerAccionPendiente();
        
        if (action == null) {
            // Si no hay acción pendiente, verificar si el usuario está logueado con Google
            // y mostrar mensaje informativo
            Toast.makeText(activity, 
                "Si deseas agregar eventos a tu calendario, autoriza cuando se te pregunte", 
                Toast.LENGTH_SHORT).show();
            regresarAActividadAnterior(activity);
            return;
        }
        
        Log.d(TAG, "Procesando callback OAuth para acción: " + action);
        
        if ("create".equals(action)) {
            helper.crearEventos(accessToken);
        } else if ("delete".equals(action)) {
            helper.eliminarEventos(accessToken);
        } else {
            Log.e(TAG, "Acción pendiente inválida: " + action);
            regresarAActividadAnterior(activity);
        }
    }
    
    /**
     * Crea eventos en Google Calendar usando el token temporal
     */
    private void crearEventos(String accessToken) {
        String medicamentoId = obtenerMedicamentoIdPendiente();
        if (medicamentoId == null) {
            Log.e(TAG, "No hay medicamentoId pendiente para crear eventos");
            regresarAActividadAnterior();
            return;
        }
        
        // Obtener el medicamento desde Firestore
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    Medicamento medicamento = (Medicamento) result;
                    googleCalendarService.crearEventosRecurrentes(
                        accessToken,
                        medicamento,
                        new GoogleCalendarService.RecurrentEventsCallback() {
                            @Override
                            public void onSuccess(List<String> eventoIds) {
                                Log.d(TAG, "Eventos creados exitosamente: " + eventoIds.size());
                                Toast.makeText(activity, 
                                    "Eventos agregados a Google Calendar", 
                                    Toast.LENGTH_SHORT).show();
                                
                                // Guardar eventoIds en el medicamento
                                if (!eventoIds.isEmpty()) {
                                    guardarEventoIdsEnMedicamento(medicamentoId, eventoIds);
                                }
                                
                                limpiarAccionPendiente();
                                regresarAActividadAnterior();
                            }
                            
                            @Override
                            public void onError(Exception exception) {
                                Log.e(TAG, "Error al crear eventos", exception);
                                Toast.makeText(activity, 
                                    "Error al crear eventos en Google Calendar", 
                                    Toast.LENGTH_SHORT).show();
                                limpiarAccionPendiente();
                                regresarAActividadAnterior();
                            }
                        }
                    );
                } else {
                    Log.e(TAG, "No se pudo obtener el medicamento");
                    limpiarAccionPendiente();
                    regresarAActividadAnterior();
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al obtener medicamento", exception);
                limpiarAccionPendiente();
                regresarAActividadAnterior();
            }
        });
    }
    
    /**
     * Elimina eventos de Google Calendar usando el token temporal
     */
    private void eliminarEventos(String accessToken) {
        String medicamentoId = obtenerMedicamentoIdPendiente();
        List<String> eventoIds = obtenerEventoIdsPendientes();
        
        if (medicamentoId == null || eventoIds == null || eventoIds.isEmpty()) {
            Log.d(TAG, "No hay eventos para eliminar");
            limpiarAccionPendiente();
            regresarAActividadAnterior();
            return;
        }
        
        googleCalendarService.eliminarEventos(
            accessToken,
            eventoIds,
            new GoogleCalendarService.DeleteEventsCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Eventos eliminados exitosamente");
                    Toast.makeText(activity, 
                        "Eventos eliminados de Google Calendar", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Limpiar eventoIds del medicamento
                    limpiarEventoIdsDelMedicamento(medicamentoId);
                    
                    limpiarAccionPendiente();
                    regresarAActividadAnterior();
                }
                
                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error al eliminar eventos", exception);
                    Toast.makeText(activity, 
                        "Error al eliminar eventos de Google Calendar", 
                        Toast.LENGTH_SHORT).show();
                    limpiarAccionPendiente();
                    regresarAActividadAnterior();
                }
            }
        );
    }
    
    /**
     * Guarda los eventoIds en el medicamento en Firestore
     */
    private void guardarEventoIdsEnMedicamento(String medicamentoId, List<String> eventoIds) {
        firebaseService.obtenerMedicamentoDocumento(medicamentoId, new FirebaseService.FirestoreDocumentCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (document != null && document.exists()) {
                    document.getReference().update("eventoIdsGoogleCalendar", eventoIds)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "EventoIds guardados en medicamento: " + medicamentoId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al guardar eventoIds", e);
                        });
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al obtener documento para guardar eventoIds", exception);
            }
        });
    }
    
    /**
     * Limpia los eventoIds del medicamento en Firestore
     */
    private void limpiarEventoIdsDelMedicamento(String medicamentoId) {
        firebaseService.obtenerMedicamentoDocumento(medicamentoId, new FirebaseService.FirestoreDocumentCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (document != null && document.exists()) {
                    document.getReference().update("eventoIdsGoogleCalendar", new ArrayList<String>())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "EventoIds limpiados del medicamento: " + medicamentoId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al limpiar eventoIds", e);
                        });
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al obtener documento para limpiar eventoIds", exception);
            }
        });
    }
    
    /**
     * Guarda la acción pendiente en SharedPreferences
     */
    private void guardarAccionPendiente(String action, String medicamentoId, List<String> eventoIds) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PENDING_ACTION, action);
        editor.putString(KEY_PENDING_MEDICAMENTO_ID, medicamentoId);
        if (eventoIds != null && !eventoIds.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray();
                for (String eventoId : eventoIds) {
                    jsonArray.put(eventoId);
                }
                editor.putString(KEY_PENDING_EVENTO_IDS, jsonArray.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error al guardar eventoIds", e);
                editor.remove(KEY_PENDING_EVENTO_IDS);
            }
        } else {
            editor.remove(KEY_PENDING_EVENTO_IDS);
        }
        editor.apply();
    }
    
    /**
     * Obtiene la acción pendiente
     */
    private String obtenerAccionPendiente() {
        return prefs.getString(KEY_PENDING_ACTION, null);
    }
    
    /**
     * Obtiene el medicamentoId pendiente
     */
    private String obtenerMedicamentoIdPendiente() {
        return prefs.getString(KEY_PENDING_MEDICAMENTO_ID, null);
    }
    
    /**
     * Obtiene los eventoIds pendientes
     */
    private List<String> obtenerEventoIdsPendientes() {
        String json = prefs.getString(KEY_PENDING_EVENTO_IDS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = new JSONArray(json);
            List<String> eventoIds = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                eventoIds.add(jsonArray.getString(i));
            }
            return eventoIds;
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear eventoIds", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Limpia la acción pendiente
     */
    private void limpiarAccionPendiente() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_PENDING_ACTION);
        editor.remove(KEY_PENDING_MEDICAMENTO_ID);
        editor.remove(KEY_PENDING_EVENTO_IDS);
        editor.apply();
    }
    
    /**
     * Regresa a la actividad anterior
     */
    private void regresarAActividadAnterior() {
        activity.finish();
    }
    
    /**
     * Regresa a la actividad anterior (método estático)
     */
    private static void regresarAActividadAnterior(Activity activity) {
        activity.finish();
    }
}

