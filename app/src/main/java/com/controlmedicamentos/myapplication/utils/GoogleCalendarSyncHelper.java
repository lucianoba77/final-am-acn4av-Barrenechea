package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarService;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Clase helper para gestionar la sincronización de eventos de Google Calendar
 * con medicamentos (crear, eliminar, reactivar).
 */
public class GoogleCalendarSyncHelper {

    private final Context context;
    private final GoogleCalendarAuthService googleCalendarAuthService;
    private final GoogleCalendarService googleCalendarService;
    private final FirebaseService firebaseService;

    /**
     * Constructor.
     * 
     * @param context El contexto de la aplicación.
     */
    public GoogleCalendarSyncHelper(Context context) {
        this.context = context;
        this.googleCalendarAuthService = new GoogleCalendarAuthService(context);
        this.googleCalendarService = new GoogleCalendarService();
        this.firebaseService = new FirebaseService();
    }

    /**
     * Obtiene los IDs de eventos de Google Calendar asociados a un medicamento.
     * 
     * @param medicamentoId ID del medicamento.
     * @param callback Callback con la lista de eventoIds (puede estar vacía).
     */
    public void obtenerEventoIds(String medicamentoId, EventoIdsCallback callback) {
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    // Los eventoIds se guardan en Firestore, no en el modelo
                    // Necesitamos obtenerlos directamente del documento
                    obtenerEventoIdsDesdeFirestore(medicamentoId, callback);
                } else {
                    if (callback != null) {
                        callback.onSuccess(new ArrayList<>());
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                Logger.w("GoogleCalendarSyncHelper", "Error al obtener medicamento para eventoIds", exception);
                if (callback != null) {
                    callback.onSuccess(new ArrayList<>());
                }
            }
        });
    }

    /**
     * Obtiene los eventoIds directamente desde Firestore.
     */
    private void obtenerEventoIdsDesdeFirestore(String medicamentoId, EventoIdsCallback callback) {
        Logger.d("GoogleCalendarSyncHelper", "obtenerEventoIdsDesdeFirestore: Obteniendo eventoIds para medicamento: " + medicamentoId);
        firebaseService.obtenerMedicamentoDocumento(medicamentoId, new FirebaseService.FirestoreDocumentCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                List<String> eventoIds = new ArrayList<>();
                if (document != null && document.exists()) {
                    Object eventoIdsObj = document.get("eventoIdsGoogleCalendar");
                    Logger.d("GoogleCalendarSyncHelper", 
                        "obtenerEventoIdsDesdeFirestore: eventoIdsObj tipo: " + 
                        (eventoIdsObj != null ? eventoIdsObj.getClass().getSimpleName() : "null"));
                    
                    if (eventoIdsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> eventoIdsList = (List<Object>) eventoIdsObj;
                        Logger.d("GoogleCalendarSyncHelper", 
                            "obtenerEventoIdsDesdeFirestore: Lista encontrada con " + eventoIdsList.size() + " elementos");
                        for (Object eventoIdObj : eventoIdsList) {
                            if (eventoIdObj instanceof String) {
                                eventoIds.add((String) eventoIdObj);
                            } else {
                                Logger.w("GoogleCalendarSyncHelper", 
                                    "obtenerEventoIdsDesdeFirestore: Elemento no es String: " + 
                                    (eventoIdObj != null ? eventoIdObj.getClass().getSimpleName() : "null"));
                            }
                        }
                    } else if (eventoIdsObj != null) {
                        Logger.w("GoogleCalendarSyncHelper", 
                            "obtenerEventoIdsDesdeFirestore: eventoIdsGoogleCalendar no es una List, es: " + 
                            eventoIdsObj.getClass().getSimpleName());
                    } else {
                        Logger.d("GoogleCalendarSyncHelper", 
                            "obtenerEventoIdsDesdeFirestore: eventoIdsGoogleCalendar es null o no existe");
                    }
                } else {
                    Logger.w("GoogleCalendarSyncHelper", 
                        "obtenerEventoIdsDesdeFirestore: Documento no existe para medicamento: " + medicamentoId);
                }
                
                Logger.d("GoogleCalendarSyncHelper", 
                    "obtenerEventoIdsDesdeFirestore: Total eventoIds obtenidos: " + eventoIds.size());
                if (callback != null) {
                    callback.onSuccess(eventoIds);
                }
            }

            @Override
            public void onError(Exception exception) {
                Logger.w("GoogleCalendarSyncHelper", "Error al obtener documento para eventoIds", exception);
                if (callback != null) {
                    callback.onSuccess(new ArrayList<>());
                }
            }
        });
    }

    /**
     * Elimina todos los eventos de Google Calendar asociados a un medicamento.
     * 
     * @param medicamentoId ID del medicamento.
     * @param callback Callback para notificar el resultado.
     */
    public void eliminarEventosMedicamento(String medicamentoId, SyncCallback callback) {
        // Verificar si Google Calendar está conectado
        googleCalendarAuthService.tieneGoogleCalendarConectado(
            new GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    boolean conectado = result != null && (Boolean) result;
                    if (!conectado) {
                        Logger.d("GoogleCalendarSyncHelper", 
                            "Google Calendar no está conectado, omitiendo eliminación de eventos");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        return;
                    }

                    // Obtener token de acceso
                    googleCalendarAuthService.obtenerTokenGoogle(
                        new GoogleCalendarAuthService.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object tokenResult) {
                                if (tokenResult != null && tokenResult instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> tokenData = (Map<String, Object>) tokenResult;
                                    String accessToken = (String) tokenData.get("access_token");
                                    
                                    if (accessToken != null && !accessToken.isEmpty()) {
                                        // Obtener eventoIds del medicamento
                                        obtenerEventoIds(medicamentoId, new EventoIdsCallback() {
                                            @Override
                                            public void onSuccess(List<String> eventoIds) {
                                                if (eventoIds.isEmpty()) {
                                                    Logger.d("GoogleCalendarSyncHelper", 
                                                        "No hay eventos de Google Calendar para eliminar");
                                                    if (callback != null) {
                                                        callback.onSuccess();
                                                    }
                                                    return;
                                                }

                                                // Eliminar todos los eventos
                                                googleCalendarService.eliminarEventos(
                                                    accessToken, 
                                                    eventoIds,
                                                    new GoogleCalendarService.MultipleEventsCallback() {
                                                        @Override
                                                        public void onSuccess(List<String> eventoIdsEliminados) {
                                                            Logger.d("GoogleCalendarSyncHelper", 
                                                                "Eventos eliminados de Google Calendar: " + 
                                                                eventoIdsEliminados.size());
                                                            // Limpiar los eventoIds del medicamento
                                                            limpiarEventoIdsEnMedicamento(medicamentoId);
                                                            if (callback != null) {
                                                                callback.onSuccess();
                                                            }
                                                        }

                                                        @Override
                                                        public void onPartialSuccess(
                                                            List<String> eventoIdsEliminados, 
                                                            List<Exception> errores) {
                                                            Logger.w("GoogleCalendarSyncHelper", 
                                                                "Algunos eventos se eliminaron: " + 
                                                                eventoIdsEliminados.size() + 
                                                                ", errores: " + errores.size());
                                                            // Limpiar los eventoIds del medicamento (aunque algunos fallaron)
                                                            limpiarEventoIdsEnMedicamento(medicamentoId);
                                                            if (callback != null) {
                                                                callback.onSuccess();
                                                            }
                                                        }

                                                        @Override
                                                        public void onError(Exception exception) {
                                                            Logger.w("GoogleCalendarSyncHelper", 
                                                                "Error al eliminar eventos de Google Calendar", exception);
                                                            // No es crítico, continuar
                                                            if (callback != null) {
                                                                callback.onSuccess();
                                                            }
                                                        }
                                                    }
                                                );
                                            }
                                        });
                                    } else {
                                        Logger.w("GoogleCalendarSyncHelper", 
                                            "Token de acceso no disponible");
                                        if (callback != null) {
                                            callback.onSuccess();
                                        }
                                    }
                                } else {
                                    Logger.w("GoogleCalendarSyncHelper", 
                                        "Token de Google Calendar no disponible");
                                    if (callback != null) {
                                        callback.onSuccess();
                                    }
                                }
                            }

                            @Override
                            public void onError(Exception exception) {
                                Logger.w("GoogleCalendarSyncHelper", 
                                    "Error al obtener token de Google Calendar", exception);
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }
                        }
                    );
                }

                @Override
                public void onError(Exception exception) {
                    Logger.w("GoogleCalendarSyncHelper", 
                        "Error al verificar conexión de Google Calendar", exception);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }
            }
        );
    }

    /**
     * Crea eventos de Google Calendar para un medicamento.
     * 
     * @param medicamento El medicamento para el cual crear eventos.
     * @param callback Callback para notificar el resultado.
     */
    public void crearEventosMedicamento(Medicamento medicamento, SyncCallback callback) {
        // Verificar si Google Calendar está conectado
        googleCalendarAuthService.tieneGoogleCalendarConectado(
            new GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    boolean conectado = result != null && (Boolean) result;
                    if (!conectado) {
                        Logger.d("GoogleCalendarSyncHelper", 
                            "Google Calendar no está conectado, omitiendo creación de eventos");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        return;
                    }

                    // Obtener token de acceso
                    googleCalendarAuthService.obtenerTokenGoogle(
                        new GoogleCalendarAuthService.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object tokenResult) {
                                if (tokenResult != null && tokenResult instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> tokenData = (Map<String, Object>) tokenResult;
                                    String accessToken = (String) tokenData.get("access_token");
                                    
                                    if (accessToken != null && !accessToken.isEmpty()) {
                                        // Crear eventos recurrentes
                                        googleCalendarService.crearEventosRecurrentes(
                                            accessToken,
                                            medicamento,
                                            new GoogleCalendarService.RecurrentEventsCallback() {
                                                @Override
                                                public void onSuccess(List<String> eventoIds) {
                                                    Logger.d("GoogleCalendarSyncHelper", 
                                                        "Eventos creados en Google Calendar: " + eventoIds.size());
                                                    
                                                    // Guardar los eventoIds en el medicamento
                                                    if (!eventoIds.isEmpty() && medicamento.getId() != null) {
                                                        guardarEventoIdsEnMedicamento(medicamento.getId(), eventoIds);
                                                    }
                                                    
                                                    if (callback != null) {
                                                        callback.onSuccess();
                                                    }
                                                }

                                                @Override
                                                public void onError(Exception exception) {
                                                    Logger.w("GoogleCalendarSyncHelper", 
                                                        "Error al crear eventos en Google Calendar", exception);
                                                    // No es crítico, continuar
                                                    if (callback != null) {
                                                        callback.onSuccess();
                                                    }
                                                }
                                            }
                                        );
                                    } else {
                                        Logger.w("GoogleCalendarSyncHelper", 
                                            "Token de acceso no disponible");
                                        if (callback != null) {
                                            callback.onSuccess();
                                        }
                                    }
                                } else {
                                    Logger.w("GoogleCalendarSyncHelper", 
                                        "Token de Google Calendar no disponible");
                                    if (callback != null) {
                                        callback.onSuccess();
                                    }
                                }
                            }

                            @Override
                            public void onError(Exception exception) {
                                Logger.w("GoogleCalendarSyncHelper", 
                                    "Error al obtener token de Google Calendar", exception);
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }
                        }
                    );
                }

                @Override
                public void onError(Exception exception) {
                    Logger.w("GoogleCalendarSyncHelper", 
                        "Error al verificar conexión de Google Calendar", exception);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }
            }
        );
    }

    /**
     * Guarda los IDs de eventos en el medicamento en Firestore.
     */
    private void guardarEventoIdsEnMedicamento(String medicamentoId, List<String> eventoIds) {
        firebaseService.obtenerMedicamentoDocumento(medicamentoId, 
            new FirebaseService.FirestoreDocumentCallback() {
                @Override
                public void onSuccess(DocumentSnapshot document) {
                    if (document != null && document.exists()) {
                        document.getReference().update("eventoIdsGoogleCalendar", eventoIds)
                            .addOnSuccessListener(aVoid -> {
                                Logger.d("GoogleCalendarSyncHelper", 
                                    "EventoIds guardados en medicamento: " + medicamentoId);
                            })
                            .addOnFailureListener(e -> {
                                Logger.w("GoogleCalendarSyncHelper", 
                                    "Error al guardar eventoIds en medicamento", e);
                            });
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Logger.w("GoogleCalendarSyncHelper", 
                        "Error al obtener documento para guardar eventoIds", exception);
                }
            }
        );
    }

    /**
     * Limpia los eventoIds del medicamento en Firestore después de eliminar eventos.
     */
    private void limpiarEventoIdsEnMedicamento(String medicamentoId) {
        firebaseService.obtenerMedicamentoDocumento(medicamentoId, 
            new FirebaseService.FirestoreDocumentCallback() {
                @Override
                public void onSuccess(DocumentSnapshot document) {
                    if (document != null && document.exists()) {
                        document.getReference().update("eventoIdsGoogleCalendar", new ArrayList<>())
                            .addOnSuccessListener(aVoid -> {
                                Logger.d("GoogleCalendarSyncHelper", 
                                    "EventoIds limpiados en medicamento: " + medicamentoId);
                            })
                            .addOnFailureListener(e -> {
                                Logger.w("GoogleCalendarSyncHelper", 
                                    "Error al limpiar eventoIds en medicamento", e);
                            });
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Logger.w("GoogleCalendarSyncHelper", 
                        "Error al obtener documento para limpiar eventoIds", exception);
                }
            }
        );
    }
    
    /**
     * Elimina eventos y limpia los eventoIds del medicamento.
     */
    public void eliminarEventosYLimpiarIds(String medicamentoId, SyncCallback callback) {
        eliminarEventosMedicamento(medicamentoId, new SyncCallback() {
            @Override
            public void onSuccess() {
                limpiarEventoIdsEnMedicamento(medicamentoId);
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        });
    }

    /**
     * Callback para obtener eventoIds.
     */
    public interface EventoIdsCallback {
        void onSuccess(List<String> eventoIds);
    }

    /**
     * Callback para operaciones de sincronización.
     */
    public interface SyncCallback {
        void onSuccess();
        default void onError(Exception exception) {
            Logger.w("GoogleCalendarSyncHelper", "Error en sincronización", exception);
        }
    }
}

