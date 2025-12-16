package com.controlmedicamentos.myapplication.services;

import android.util.Log;
import com.controlmedicamentos.myapplication.utils.Constants;
import com.controlmedicamentos.myapplication.utils.Logger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.models.Usuario;
import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Servicio para manejar operaciones CRUD con Firebase Firestore
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private FirebaseFirestore db;
    private AuthService authService;

    // Nombres de colecciones
    private static final String COLLECTION_USUARIOS = "usuarios";
    private static final String COLLECTION_MEDICAMENTOS = "medicamentos";
    private static final String COLLECTION_TOMAS = "tomas";
    private static final String COLLECTION_CONFIGURACIONES = "configuraciones";

    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
        authService = new AuthService();
    }

    // ==================== USUARIOS ====================

    /**
     * Guarda o actualiza un usuario en Firestore
     */
    public void guardarUsuario(Usuario usuario, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        Map<String, Object> usuarioMap = usuarioToMap(usuario);
        usuarioMap.put("fechaActualizacion", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_USUARIOS)
            .document(firebaseUser.getUid())
            .set(usuarioMap)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Usuario guardado exitosamente");
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al guardar usuario", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Obtiene el usuario actual desde Firestore
     */
    public void obtenerUsuarioActual(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_USUARIOS)
            .document(firebaseUser.getUid())
            .get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Usuario usuario = mapToUsuario(document);
                            if (callback != null) {
                                callback.onSuccess(usuario);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(new Exception("Usuario no encontrado"));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error al obtener usuario", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    // ==================== MEDICAMENTOS ====================

    /**
     * Guarda un nuevo medicamento en Firestore.
     * 
     * @param medicamento El medicamento a guardar. No debe ser null.
     * @param callback Callback para manejar el resultado. Puede ser null.
     *                 onSuccess recibe el medicamento con su ID asignado.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void guardarMedicamento(Medicamento medicamento, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        Map<String, Object> medicamentoMap = medicamentoToMap(medicamento);
        medicamentoMap.put("userId", firebaseUser.getUid());
        
        // Guardar fechas como string ISO (consistente con React)
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String fechaActual = isoFormat.format(new Date());
        medicamentoMap.put("fechaCreacion", fechaActual);
        medicamentoMap.put("fechaActualizacion", fechaActual);

        db.collection(COLLECTION_MEDICAMENTOS)
            .add(medicamentoMap)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Logger.d(TAG, "Medicamento guardado con ID: " + documentReference.getId());
                    medicamento.setId(documentReference.getId());
                    if (callback != null) {
                        callback.onSuccess(medicamento);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Logger.e(TAG, "Error al guardar medicamento", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Actualiza un medicamento existente en Firestore.
     * Actualiza la fecha de actualización automáticamente.
     * Consistente con React: medicamentosService.js - actualizarMedicamento()
     * 
     * @param medicamento El medicamento con los datos actualizados. Debe tener un ID válido.
     * @param callback Callback para manejar el resultado. Puede ser null.
     */
    public void actualizarMedicamento(Medicamento medicamento, FirestoreCallback callback) {
        actualizarMedicamento(medicamento, null, callback);
    }

    /**
     * Actualiza un medicamento existente en Firestore.
     * Si se proporciona contexto, gestiona eventos de Google Calendar cuando se pausa/reactiva.
     * 
     * @param medicamento El medicamento con los datos actualizados. Debe tener un ID válido.
     * @param context Contexto de la aplicación (opcional, para gestionar Google Calendar).
     * @param callback Callback para manejar el resultado. Puede ser null.
     */
    public void actualizarMedicamento(Medicamento medicamento, android.content.Context context, FirestoreCallback callback) {
        // Primero obtener el medicamento actual para verificar cambios
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamento.getId())
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document == null || !document.exists()) {
                        if (callback != null) {
                            callback.onError(new Exception("Medicamento no encontrado"));
                        }
                        return;
                    }

                    // Asegurar que stockInicial y stockActual sean números
                    // Lógica consistente con React: medicamentosService.js líneas 197-210
                    Map<String, Object> medicamentoActualData = document.getData();
                    boolean estabaPausado = false;
                    boolean ahoraEstaPausado = medicamento.isPausado();
                    
                    if (medicamentoActualData != null) {
                        // Obtener valores actuales
                        Object stockInicialActualObj = medicamentoActualData.get("stockInicial");
                        Object stockActualActualObj = medicamentoActualData.get("stockActual");
                        Object pausadoObj = medicamentoActualData.get("pausado");
                        
                        int stockInicialActual = 0;
                        int stockActualActual = 0;
                        
                        if (stockInicialActualObj instanceof Number) {
                            stockInicialActual = ((Number) stockInicialActualObj).intValue();
                        }
                        if (stockActualActualObj instanceof Number) {
                            stockActualActual = ((Number) stockActualActualObj).intValue();
                        }
                        if (pausadoObj instanceof Boolean) {
                            estabaPausado = (Boolean) pausadoObj;
                        }
                        
                        // Si se actualiza stockInicial y no se está actualizando stockActual explícitamente,
                        // actualizar stockActual al nuevo valor de stockInicial (lógica de React)
                        // Pero si el stockActual del objeto es diferente al actual, significa que se actualizó explícitamente
                        if (medicamento.getStockInicial() != stockInicialActual) {
                            // Si stockActual no se actualizó explícitamente, usar el nuevo stockInicial
                            if (medicamento.getStockActual() == stockActualActual) {
                                // El stockActual no cambió explícitamente, mantener el valor actual
                                // (no actualizar al nuevo stockInicial, como en React al editar)
                                medicamento.setStockActual(stockActualActual);
                            }
                            // Si stockActual sí cambió, usar el nuevo valor que viene en el objeto
                        }
                    }

                    // Detectar cambio en estado de pausa para gestionar Google Calendar
                    boolean cambioEstadoPausa = estabaPausado != ahoraEstaPausado;
                    
                    // Si se pausó el medicamento, eliminar eventos de Google Calendar
                    if (cambioEstadoPausa && ahoraEstaPausado && context != null) {
                        eliminarEventosGoogleCalendarAlPausar(medicamento.getId(), context, () -> {
                            continuarActualizacion(medicamento, callback);
                        });
                        return;
                    }
                    
                    // Si se reactivó el medicamento, crear eventos de Google Calendar
                    if (cambioEstadoPausa && !ahoraEstaPausado && context != null) {
                        crearEventosGoogleCalendarAlReactivar(medicamento, context, () -> {
                            continuarActualizacion(medicamento, callback);
                        });
                        return;
                    }

                    // Continuar con la actualización normal
                    continuarActualizacion(medicamento, callback);
                } else {
                    Logger.e(TAG, "Error al obtener medicamento para actualizar", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Continúa con la actualización del medicamento en Firestore.
     */
    private void continuarActualizacion(Medicamento medicamento, FirestoreCallback callback) {
        // Preparar mapa de actualización
        Map<String, Object> medicamentoMap = medicamentoToMap(medicamento);
        
        // Actualizar fechaActualizacion como string ISO (consistente con React)
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        medicamentoMap.put("fechaActualizacion", isoFormat.format(new Date()));

        // Actualizar en Firebase
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamento.getId())
            .update(medicamentoMap)
            .addOnSuccessListener(aVoid -> {
                Logger.d(TAG, "Medicamento actualizado exitosamente");
                if (callback != null) {
                    callback.onSuccess(medicamento);
                }
            })
            .addOnFailureListener(e -> {
                Logger.e(TAG, "Error al actualizar medicamento", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }

    /**
     * Elimina eventos de Google Calendar cuando se pausa un medicamento.
     */
    private void eliminarEventosGoogleCalendarAlPausar(String medicamentoId, android.content.Context context, Runnable onComplete) {
        com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper syncHelper = 
            new com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper(context);
        
        syncHelper.eliminarEventosMedicamento(medicamentoId, 
            new com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper.SyncCallback() {
                @Override
                public void onSuccess() {
                    Logger.d(TAG, "Eventos de Google Calendar eliminados al pausar medicamento");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        );
    }

    /**
     * Crea eventos de Google Calendar cuando se reactiva un medicamento.
     */
    private void crearEventosGoogleCalendarAlReactivar(Medicamento medicamento, android.content.Context context, Runnable onComplete) {
        com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper syncHelper = 
            new com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper(context);
        
        syncHelper.crearEventosMedicamento(medicamento, 
            new com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper.SyncCallback() {
                @Override
                public void onSuccess() {
                    Logger.d(TAG, "Eventos de Google Calendar creados al reactivar medicamento");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        );
    }

    /**
     * Resta una unidad del stock de un medicamento
     * Para medicamentos ocasionales, también registra la toma en tomasRealizadas
     * Consistente con React: medicamentosService.js - restarStockMedicamento()
     */
    public void restarStockMedicamento(String medicamentoId, FirestoreCallback callback) {
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamentoId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document == null || !document.exists()) {
                        if (callback != null) {
                            callback.onError(new Exception("Medicamento no encontrado"));
                        }
                        return;
                    }

                    Map<String, Object> medicamentoData = document.getData();
                    if (medicamentoData == null) {
                        if (callback != null) {
                            callback.onError(new Exception("Datos del medicamento no disponibles"));
                        }
                        return;
                    }

                    // Obtener stock actual
                    Object stockActualObj = medicamentoData.get("stockActual");
                    int stockActual = 0;
                    if (stockActualObj instanceof Number) {
                        stockActual = ((Number) stockActualObj).intValue();
                    }

                    if (stockActual <= 0) {
                        if (callback != null) {
                            callback.onError(new Exception("No hay stock disponible para restar"));
                        }
                        return;
                    }

                    int nuevoStock = Math.max(0, stockActual - 1);

                    // Verificar si es medicamento ocasional (tomasDiarias === 0)
                    Object tomasDiariasObj = medicamentoData.get("tomasDiarias");
                    int tomasDiarias = 0;
                    if (tomasDiariasObj instanceof Number) {
                        tomasDiarias = ((Number) tomasDiariasObj).intValue();
                    }
                    boolean esOcasional = (tomasDiarias == 0);

                    // Preparar datos actualizados
                    Map<String, Object> datosActualizados = new HashMap<>();
                    datosActualizados.put("stockActual", nuevoStock);
                    
                    // Actualizar fechaActualizacion como string ISO
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    datosActualizados.put("fechaActualizacion", isoFormat.format(new Date()));

                    // Si es medicamento ocasional, registrar la toma
                    if (esOcasional) {
                        // Obtener tomasRealizadas existentes
                        List<Map<String, Object>> tomasRealizadas = new ArrayList<>();
                        Object tomasObj = medicamentoData.get("tomasRealizadas");
                        if (tomasObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> tomasList = (List<Object>) tomasObj;
                            for (Object tomaObj : tomasList) {
                                if (tomaObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> toma = (Map<String, Object>) tomaObj;
                                    tomasRealizadas.add(toma);
                                }
                            }
                        }

                        // Crear nueva toma
                        Calendar cal = Calendar.getInstance();
                        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
                        String horaActual = new SimpleDateFormat("HH:mm", Locale.US).format(cal.getTime());

                        Map<String, Object> nuevaToma = new HashMap<>();
                        nuevaToma.put("fecha", fechaHoy);
                        nuevaToma.put("hora", horaActual);
                        nuevaToma.put("tomada", true);
                        nuevaToma.put("tipo", "ocasional");

                        tomasRealizadas.add(nuevaToma);
                        datosActualizados.put("tomasRealizadas", tomasRealizadas);
                    }

                    // Actualizar en Firebase
                    db.collection(COLLECTION_MEDICAMENTOS)
                        .document(medicamentoId)
                        .update(datosActualizados)
                        .addOnSuccessListener(aVoid -> {
                            Logger.d(TAG, "Stock restado exitosamente. Nuevo stock: " + nuevoStock);
                            if (callback != null) {
                                Map<String, Object> resultado = new HashMap<>();
                                resultado.put("stockActual", nuevoStock);
                                resultado.put("tomaRegistrada", esOcasional);
                                callback.onSuccess(resultado);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Logger.e(TAG, "Error al restar stock", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        });
                } else {
                    Logger.e(TAG, "Error al obtener medicamento para restar stock", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    // ==================== TOMAS ====================

    /**
     * Guarda una toma en Firestore.
     * Asigna automáticamente el userId del usuario actual y establece fechas por defecto si no están presentes.
     * 
     * @param toma La toma a guardar. Debe tener medicamentoId. No debe ser null.
     * @param callback Callback para manejar el resultado. Puede ser null.
     *                 onSuccess recibe la toma con su ID asignado.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void guardarToma(Toma toma, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        if (toma == null) {
            if (callback != null) {
                callback.onError(new Exception("Datos de la toma no válidos"));
            }
            return;
        }

        if (toma.getMedicamentoId() == null || toma.getMedicamentoId().isEmpty()) {
            if (callback != null) {
                callback.onError(new Exception("El ID del medicamento es obligatorio"));
            }
            return;
        }

        toma.setUserId(firebaseUser.getUid());
        Date ahora = new Date();
        if (toma.getFechaHoraTomada() == null) {
            toma.setFechaHoraTomada(ahora);
        }
        if (toma.getFechaHoraProgramada() == null) {
            toma.setFechaHoraProgramada(toma.getFechaHoraTomada());
        }
        if (toma.getEstado() == null) {
            toma.setEstado(Toma.EstadoToma.TOMADA);
        }

        Map<String, Object> tomaMap = tomaToMap(toma);
        db.collection(COLLECTION_TOMAS)
            .add(tomaMap)
            .addOnSuccessListener(documentReference -> {
                Logger.d(TAG, "Toma registrada con ID: " + documentReference.getId());
                toma.setId(documentReference.getId());
                if (callback != null) {
                    callback.onSuccess(toma);
                }
            })
            .addOnFailureListener(e -> {
                Logger.e(TAG, "Error al registrar la toma", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }

    /**
     * Obtiene las tomas registradas para un medicamento específico.
     * Retorna las últimas 200 tomas ordenadas por fecha descendente.
     * 
     * @param medicamentoId ID del medicamento. No debe ser null ni vacío.
     * @param callback Callback para manejar el resultado. No debe ser null.
     *                 onSuccess recibe una List de Toma ordenadas por fecha descendente.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void obtenerTomasPorMedicamento(String medicamentoId, FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        // Limitar a últimas 200 tomas del medicamento para optimizar rendimiento
        db.collection(COLLECTION_TOMAS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .whereEqualTo("medicamentoId", medicamentoId)
            .orderBy("fechaHoraTomada", Query.Direction.DESCENDING)
            .limit(200)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Toma> tomas = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        tomas.add(mapToToma(document));
                    }
                    if (callback != null) {
                        callback.onSuccess(tomas);
                    }
                } else {
                    Logger.e(TAG, "Error al obtener tomas del medicamento", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Obtiene todas las tomas del usuario actual.
     * Retorna las últimas 500 tomas ordenadas por fecha descendente para optimizar rendimiento.
     * 
     * @param callback Callback para manejar el resultado. No debe ser null.
     *                 onSuccess recibe una List de Toma ordenadas por fecha descendente.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void obtenerTomasUsuario(FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        // Intentar obtener con orderBy primero, si falla, obtener sin orderBy
        // Limitar a últimas 500 tomas para optimizar rendimiento
        db.collection(COLLECTION_TOMAS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .orderBy("fechaHoraTomada", Query.Direction.DESCENDING)
            .limit(500)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Toma> tomas = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Toma toma = mapToToma(document);
                        if (toma != null) {
                            tomas.add(toma);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(tomas);
                    }
                } else {
                    // Si falla por falta de índice, intentar sin orderBy
                    Logger.w(TAG, "Error al obtener tomas con orderBy, intentando sin orden", task.getException());
                    db.collection(COLLECTION_TOMAS)
                        .whereEqualTo("userId", firebaseUser.getUid())
                        .get()
                        .addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                List<Toma> tomas = new ArrayList<>();
                                for (DocumentSnapshot document : task2.getResult()) {
                                    Toma toma = mapToToma(document);
                                    if (toma != null) {
                                        tomas.add(toma);
                                    }
                                }
                                // Ordenar manualmente por fecha
                                tomas.sort((t1, t2) -> {
                                    if (t1.getFechaHoraTomada() == null || t2.getFechaHoraTomada() == null) {
                                        return 0;
                                    }
                                    return t2.getFechaHoraTomada().compareTo(t1.getFechaHoraTomada());
                                });
                    if (callback != null) {
                                    callback.onSuccess(tomas);
                                }
                            } else {
                                Logger.e(TAG, "Error al obtener tomas del usuario", task2.getException());
                                if (callback != null) {
                                    callback.onError(task2.getException());
                    }
                            }
                        });
                }
            });
    }

    /**
     * Elimina un medicamento de Firestore.
     * Antes de eliminar, elimina los eventos asociados en Google Calendar si están conectados.
     * 
     * @param medicamentoId ID del medicamento a eliminar. No debe ser null ni vacío.
     * @param callback Callback para manejar el resultado. Puede ser null.
     *                 onSuccess se llama cuando la eliminación es exitosa.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void eliminarMedicamento(String medicamentoId, FirestoreCallback callback) {
        // Primero eliminar eventos de Google Calendar si existen
        eliminarEventosGoogleCalendarAntesDeEliminar(medicamentoId, () -> {
            // Luego eliminar el medicamento de Firestore
            db.collection(COLLECTION_MEDICAMENTOS)
                .document(medicamentoId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Logger.d(TAG, "Medicamento eliminado exitosamente");
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Logger.e(TAG, "Error al eliminar medicamento", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });
        });
    }

    /**
     * Elimina los eventos de Google Calendar antes de eliminar un medicamento.
     * Nota: Este método requiere que se llame desde una Activity con contexto.
     * Si no hay contexto disponible, simplemente continúa con la eliminación.
     */
    private void eliminarEventosGoogleCalendarAntesDeEliminar(String medicamentoId, Runnable onComplete) {
        // Por ahora, continuar sin eliminar eventos si no hay contexto
        // Las Activities que llaman a eliminarMedicamento deben manejar esto
        Logger.d(TAG, "Nota: Eliminar eventos de Google Calendar debe manejarse desde la Activity");
        if (onComplete != null) {
            onComplete.run();
        }
    }

    /**
     * Obtiene todos los medicamentos del usuario actual
     */
    public void obtenerMedicamentos(FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        String userId = firebaseUser.getUid();
        Logger.d(TAG, "obtenerMedicamentos: Iniciando consulta para userId: " + userId);

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Logger.d(TAG, "obtenerMedicamentos: Consulta exitosa. Documentos encontrados: " + 
                            (querySnapshot != null ? querySnapshot.size() : 0));
                        
                        List<Medicamento> medicamentos = new ArrayList<>();
                        Set<String> idsVistos = new HashSet<>(); // Para detectar duplicados
                        List<String> idsDuplicados = new ArrayList<>();
                        
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot) {
                                String docId = document.getId();
                                Logger.d(TAG, "obtenerMedicamentos: Procesando documento ID: " + docId);
                                
                                // Verificar duplicados
                                if (idsVistos.contains(docId)) {
                                    Logger.w(TAG, "obtenerMedicamentos: ⚠️ DUPLICADO DETECTADO - ID: " + docId);
                                    idsDuplicados.add(docId);
                                    continue; // Saltar duplicados
                                }
                                idsVistos.add(docId);
                                
                                Medicamento medicamento = mapToMedicamento(document);
                                if (medicamento != null) {
                                    Logger.d(TAG, "obtenerMedicamentos: Medicamento mapeado - ID: " + medicamento.getId() + 
                                        ", Nombre: " + medicamento.getNombre() + 
                                        ", TomasDiarias: " + medicamento.getTomasDiarias() + 
                                        ", StockActual: " + medicamento.getStockActual() +
                                        ", Pausado: " + medicamento.isPausado());
                                    medicamentos.add(medicamento);
                                } else {
                                    Logger.e(TAG, "obtenerMedicamentos: ⚠️ ERROR - Medicamento null para documento ID: " + docId);
                                }
                            }
                        }
                        
                        Logger.d(TAG, "obtenerMedicamentos: Total medicamentos procesados: " + medicamentos.size());
                        if (!idsDuplicados.isEmpty()) {
                            Logger.w(TAG, "obtenerMedicamentos: ⚠️ IDs duplicados encontrados: " + idsDuplicados.toString());
                        }
                        
                        Logger.d(TAG, "obtenerMedicamentos: ========== LISTA COMPLETA DE MEDICAMENTOS ==========");
                        for (int i = 0; i < medicamentos.size(); i++) {
                            Medicamento m = medicamentos.get(i);
                            Logger.d(TAG, String.format("obtenerMedicamentos: [%d] %s (ID: %s, TomasDiarias: %d, StockActual: %d, Activo: %s, Pausado: %s)", 
                                i, m.getNombre(), m.getId(), m.getTomasDiarias(), m.getStockActual(), m.isActivo(), m.isPausado()));
                        }
                        Logger.d(TAG, "obtenerMedicamentos: ===================================================");
                        
                        if (callback != null) {
                            callback.onSuccess(medicamentos);
                        }
                    } else {
                        Logger.e(TAG, "Error al obtener medicamentos", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Obtiene un medicamento por ID
     */
    public void obtenerMedicamento(String medicamentoId, FirestoreCallback callback) {
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamentoId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Medicamento medicamento = mapToMedicamento(document);
                        medicamento.setId(document.getId());
                        if (callback != null) {
                            callback.onSuccess(medicamento);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(new Exception("Medicamento no encontrado"));
                        }
                    }
                } else {
                    Logger.e(TAG, "Error al obtener medicamento", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Obtiene medicamentos activos del usuario actual
     */
    public void obtenerMedicamentosActivos(FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        String userId = firebaseUser.getUid();
        Logger.d(TAG, "obtenerMedicamentosActivos: Iniciando consulta para userId: " + userId);
        Logger.d(TAG, "obtenerMedicamentosActivos: Filtros aplicados - activo=true, pausado=false");

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("activo", true)
            .whereEqualTo("pausado", false)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Logger.d(TAG, "obtenerMedicamentosActivos: Consulta exitosa. Documentos encontrados: " + 
                            (querySnapshot != null ? querySnapshot.size() : 0));
                        
                        List<Medicamento> medicamentos = new ArrayList<>();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot) {
                                String docId = document.getId();
                                Logger.d(TAG, "obtenerMedicamentosActivos: Procesando documento ID: " + docId);
                                
                                Medicamento medicamento = mapToMedicamento(document);
                                if (medicamento != null) {
                                    Logger.d(TAG, "obtenerMedicamentosActivos: Medicamento mapeado - ID: " + medicamento.getId() + 
                                        ", Nombre: " + medicamento.getNombre() + 
                                        ", TomasDiarias: " + medicamento.getTomasDiarias() + 
                                        ", StockActual: " + medicamento.getStockActual() +
                                        ", Activo: " + medicamento.isActivo() +
                                        ", Pausado: " + medicamento.isPausado());
                                    medicamentos.add(medicamento);
                                } else {
                                    Logger.e(TAG, "obtenerMedicamentosActivos: ⚠️ ERROR - Medicamento null para documento ID: " + docId);
                                }
                            }
                        }
                        
                        Logger.d(TAG, "obtenerMedicamentosActivos: Total medicamentos activos procesados: " + medicamentos.size());
                        Logger.d(TAG, "obtenerMedicamentosActivos: ========== LISTA DE MEDICAMENTOS ACTIVOS ==========");
                        for (int i = 0; i < medicamentos.size(); i++) {
                            Medicamento m = medicamentos.get(i);
                            Logger.d(TAG, String.format("obtenerMedicamentosActivos: [%d] %s (ID: %s, TomasDiarias: %d, StockActual: %d)", 
                                i, m.getNombre(), m.getId(), m.getTomasDiarias(), m.getStockActual()));
                        }
                        Logger.d(TAG, "obtenerMedicamentosActivos: =================================================");
                        
                        if (callback != null) {
                            callback.onSuccess(medicamentos);
                        }
                    } else {
                        Log.e(TAG, "Error al obtener medicamentos activos", task.getException());
                        Logger.e(TAG, "obtenerMedicamentosActivos: Error en consulta", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    // ==================== LISTENERS EN TIEMPO REAL ====================

    /**
     * Agrega un listener para cambios en tiempo real de medicamentos
     */
    public com.google.firebase.firestore.ListenerRegistration agregarListenerMedicamentos(
            FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return null;
        }

        return db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error en listener de medicamentos", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                    return;
                }

                if (snapshot != null) {
                    List<Medicamento> medicamentos = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Medicamento medicamento = mapToMedicamento(document);
                        medicamentos.add(medicamento);
                    }
                    if (callback != null) {
                        callback.onSuccess(medicamentos);
                    }
                }
            });
    }

    // ==================== CONVERSIÓN DE OBJETOS ====================

    private Map<String, Object> usuarioToMap(Usuario usuario) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre", usuario.getNombre());
        map.put("email", usuario.getEmail());
        map.put("telefono", usuario.getTelefono());
        map.put("edad", usuario.getEdad());
        map.put("medicamentosIds", usuario.getMedicamentosIds());
        if (usuario.getRole() != null) {
            map.put("role", usuario.getRole());
        }
        return map;
    }

    private Usuario mapToUsuario(DocumentSnapshot document) {
        Usuario usuario = new Usuario();
        usuario.setId(document.getId());
        usuario.setNombre(document.getString("nombre"));
        usuario.setEmail(document.getString("email"));
        usuario.setTelefono(document.getString("telefono"));
        if (document.get("edad") != null) {
            usuario.setEdad(document.getLong("edad").intValue());
        }
        if (document.get("medicamentosIds") != null) {
            usuario.setMedicamentosIds((List<String>) document.get("medicamentosIds"));
        }
        if (document.getString("role") != null) {
            usuario.setRole(document.getString("role"));
        }
        return usuario;
    }

    private Map<String, Object> tomaToMap(Toma toma) {
        Map<String, Object> map = new HashMap<>();
        map.put("medicamentoId", toma.getMedicamentoId());
        map.put("medicamentoNombre", toma.getMedicamentoNombre());
        map.put("userId", toma.getUserId());
        map.put("estado", toma.getEstado() != null ? toma.getEstado().name() : Toma.EstadoToma.TOMADA.name());
        map.put("observaciones", toma.getObservaciones() != null ? toma.getObservaciones() : "");
        map.put("fechaHoraProgramada", toma.getFechaHoraProgramada());
        map.put("fechaHoraTomada", toma.getFechaHoraTomada());
        map.put("createdAt", new Date());
        return map;
    }

    private Toma mapToToma(DocumentSnapshot document) {
        Toma toma = new Toma();
        toma.setId(document.getId());
        toma.setMedicamentoId(document.getString("medicamentoId"));
        toma.setMedicamentoNombre(document.getString("medicamentoNombre"));
        toma.setUserId(document.getString("userId"));
        toma.setObservaciones(document.getString("observaciones"));
        toma.setFechaHoraProgramada(document.getDate("fechaHoraProgramada"));
        toma.setFechaHoraTomada(document.getDate("fechaHoraTomada"));

        String estado = document.getString("estado");
        if (estado != null) {
            try {
                toma.setEstado(Toma.EstadoToma.valueOf(estado));
            } catch (IllegalArgumentException e) {
                toma.setEstado(Toma.EstadoToma.TOMADA);
            }
        } else {
            toma.setEstado(Toma.EstadoToma.TOMADA);
        }

        return toma;
    }

    private Map<String, Object> medicamentoToMap(Medicamento medicamento) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre", medicamento.getNombre());
        map.put("presentacion", medicamento.getPresentacion());
        map.put("tomasDiarias", medicamento.getTomasDiarias());
        
        // Guardar como "primeraToma" (formato usado en React)
        // Si tomasDiarias = 0, guardar como string vacío "" (medicamento ocasional)
        // Si tomasDiarias > 0, guardar el horario real
        String horario = "";
        if (medicamento.getTomasDiarias() > 0) {
            horario = medicamento.getHorarioPrimeraToma() != null && !medicamento.getHorarioPrimeraToma().isEmpty() 
                ? medicamento.getHorarioPrimeraToma() 
                : "";
        }
        // Guardar como "primeraToma" para compatibilidad con React
        map.put("primeraToma", horario);
        // También guardar como "horarioPrimeraToma" para compatibilidad con versión anterior de la app
        map.put("horarioPrimeraToma", horario.isEmpty() ? Constants.HORARIO_INVALIDO : horario);
        
        map.put("afeccion", medicamento.getAfeccion());
        map.put("stockInicial", medicamento.getStockInicial());
        map.put("stockActual", medicamento.getStockActual());
        
        // Guardar color como string hexadecimal para compatibilidad con la web
        int colorInt = medicamento.getColor();
        String colorHex = String.format("#%06X", (0xFFFFFF & colorInt));
        map.put("color", colorHex);
        
        // Guardar días de tratamiento y si es crónico
        int diasTratamiento = medicamento.getDiasTratamiento();
        map.put("diasTratamiento", diasTratamiento);
        map.put("esCronico", diasTratamiento == -1);
        
        map.put("activo", medicamento.isActivo());
        map.put("pausado", medicamento.isPausado());
        map.put("detalles", medicamento.getDetalles() != null ? medicamento.getDetalles() : "");
        map.put("alarmasActivas", true); // Por defecto activas, consistente con React
        
        if (medicamento.getHorariosTomas() != null) {
            map.put("horariosTomas", medicamento.getHorariosTomas());
        }
        
        // Campos adicionales para compatibilidad con React
        map.put("tomasRealizadas", new ArrayList<>()); // Lista vacía por defecto
        map.put("eventoIdsGoogleCalendar", new ArrayList<>()); // Lista vacía por defecto
        
        // Guardar fecha de vencimiento como string ISO (formato web) y Timestamp (formato app)
        if (medicamento.getFechaVencimiento() != null) {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            map.put("fechaVencimiento", isoFormat.format(medicamento.getFechaVencimiento()));
            map.put("fechaVencimientoTimestamp", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(medicamento.getFechaVencimiento().getTime())));
        }
        
        if (medicamento.getFechaInicioTratamiento() != null) {
            map.put("fechaInicioTratamiento", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(medicamento.getFechaInicioTratamiento().getTime())));
        }
        
        if (medicamento.getTipoStock() != null) {
            map.put("tipoStock", medicamento.getTipoStock().name());
        }
        
        map.put("diasEstimadosDuracion", medicamento.getDiasEstimadosDuracion());
        map.put("diasRestantesDuracion", medicamento.getDiasRestantesDuracion());
        
        // Campos de fecha para compatibilidad con React (formato ISO string)
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        
        if (medicamento.getFechaInicioTratamiento() != null) {
            map.put("fechaCreacion", isoFormat.format(medicamento.getFechaInicioTratamiento()));
        } else {
            map.put("fechaCreacion", isoFormat.format(new Date()));
        }
        map.put("fechaActualizacion", isoFormat.format(new Date()));
        
        return map;
    }

    private Medicamento mapToMedicamento(DocumentSnapshot document) {
        Medicamento medicamento = new Medicamento();
        medicamento.setId(document.getId());
        medicamento.setNombre(document.getString("nombre"));
        medicamento.setPresentacion(document.getString("presentacion"));
        
        // Leer tomasDiarias primero para determinar el comportamiento
        int tomasDiarias = 0;
        if (document.get("tomasDiarias") != null) {
            Object tomasObj = document.get("tomasDiarias");
            if (tomasObj instanceof Number) {
                tomasDiarias = ((Number) tomasObj).intValue();
            }
        }
        
        // IMPORTANTE: Establecer horarioPrimeraToma ANTES de setTomasDiarias
        // porque setTomasDiarias() llama a generarHorariosTomas() que necesita horarioPrimeraToma
        // Leer primero "primeraToma" (formato React), luego "horarioPrimeraToma" (formato app anterior)
        String primeraToma = document.getString("primeraToma");
        if (primeraToma == null || primeraToma.isEmpty()) {
            primeraToma = document.getString("horarioPrimeraToma");
        }
        
        // Si tomasDiarias = 0, mantener primeraToma como "" (vacío) para medicamentos ocasionales
        // Si tomasDiarias > 0 y no hay horario, usar "00:00" como valor por defecto
        if (tomasDiarias > 0) {
            medicamento.setHorarioPrimeraToma(primeraToma != null && !primeraToma.isEmpty() ? primeraToma : Constants.HORARIO_INVALIDO);
        } else {
            // Medicamento ocasional: mantener vacío
            medicamento.setHorarioPrimeraToma(primeraToma != null && !primeraToma.isEmpty() ? primeraToma : "");
        }
        
        // Ahora establecer tomasDiarias después de horarioPrimeraToma
        medicamento.setTomasDiarias(tomasDiarias);
        
        Logger.d(TAG, "mapToMedicamento: TomasDiarias establecido: " + tomasDiarias + 
            ", HorarioPrimeraToma: " + medicamento.getHorarioPrimeraToma());
        
        medicamento.setAfeccion(document.getString("afeccion"));
        
        if (document.get("stockInicial") != null) {
            Object stockObj = document.get("stockInicial");
            if (stockObj instanceof Number) {
                medicamento.setStockInicial(((Number) stockObj).intValue());
            }
        }
        if (document.get("stockActual") != null) {
            Object stockObj = document.get("stockActual");
            if (stockObj instanceof Number) {
                int stockActual = ((Number) stockObj).intValue();
                medicamento.setStockActual(stockActual);
                Logger.d(TAG, "mapToMedicamento: StockActual establecido: " + stockActual);
            } else {
                Logger.w(TAG, "mapToMedicamento: ⚠️ stockActual no es Number, es: " + 
                    (stockObj != null ? stockObj.getClass().getName() : "null"));
            }
        } else {
            Logger.w(TAG, "mapToMedicamento: ⚠️ stockActual es null en documento");
        }
        if (document.get("color") != null) {
            try {
                Object colorObj = document.get("color");
                if (colorObj instanceof Number) {
                    medicamento.setColor(((Number) colorObj).intValue());
                } else if (colorObj instanceof String) {
                    // Intentar parsear como string
                    String colorStr = (String) colorObj;
                    if (colorStr.startsWith("#")) {
                        // Si es un color hexadecimal como "#2196F3", convertirlo a int ARGB
                        // Añadir alpha FF al principio si no está presente
                        String hexColor = colorStr.substring(1);
                        if (hexColor.length() == 6) {
                            hexColor = "FF" + hexColor; // Añadir alpha
                        }
                        medicamento.setColor((int) Long.parseLong(hexColor, 16));
                    } else {
                        // Si es un número como string
                        medicamento.setColor(Integer.parseInt(colorStr));
                    }
                } else {
                    Log.w(TAG, "Tipo de color no reconocido: " + colorObj.getClass().getName());
                    // Valor por defecto: color azul #2196F3 (ARGB: 0xFF2196F3)
                    medicamento.setColor(0xFF2196F3);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al convertir color a int", e);
                // Valor por defecto: color azul #2196F3 (ARGB: 0xFF2196F3)
                medicamento.setColor(0xFF2196F3);
            }
        } else {
            // Si no hay color, usar el valor por defecto
            medicamento.setColor(0xFF2196F3);
        }
        
        // Manejar días de tratamiento: puede venir como número o como esCronico booleano
        if (document.get("esCronico") != null && document.getBoolean("esCronico")) {
            medicamento.setDiasTratamiento(-1);
        } else if (document.get("diasTratamiento") != null) {
            Object diasObj = document.get("diasTratamiento");
            if (diasObj instanceof Number) {
                medicamento.setDiasTratamiento(((Number) diasObj).intValue());
            }
        }
        medicamento.setActivo(document.getBoolean("activo") != null ? document.getBoolean("activo") : true);
        boolean pausado = document.getBoolean("pausado") != null ? document.getBoolean("pausado") : false;
        medicamento.setPausado(pausado);
        Logger.d(TAG, "mapToMedicamento: Pausado establecido: " + pausado);
        medicamento.setDetalles(document.getString("detalles"));
        
        // Leer alarmasActivas (por defecto true, consistente con React)
        // Este campo no está en el modelo Android pero se lee para compatibilidad
        
        if (document.get("horariosTomas") != null) {
            medicamento.setHorariosTomas((List<String>) document.get("horariosTomas"));
        }
        
        // Leer campos adicionales para compatibilidad con React
        // tomasRealizadas y eventoIdsGoogleCalendar no están en el modelo Android
        // pero se leen del documento para mantener consistencia
        
        // Manejar fechaVencimiento: puede venir como Timestamp o como string ISO
        // IMPORTANTE: Verificar primero el tipo del objeto antes de intentar obtenerlo como Timestamp
        Object fechaVencimientoObj = document.get("fechaVencimiento");
        if (fechaVencimientoObj != null) {
            try {
                if (fechaVencimientoObj instanceof com.google.firebase.Timestamp) {
                    // Si es un Timestamp de Firebase
                    medicamento.setFechaVencimiento(((com.google.firebase.Timestamp) fechaVencimientoObj).toDate());
                } else if (fechaVencimientoObj instanceof String) {
                    // Si es un string ISO "yyyy-MM-dd"
                    try {
                        String fechaStr = (String) fechaVencimientoObj;
                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        medicamento.setFechaVencimiento(isoFormat.parse(fechaStr));
                    } catch (ParseException e) {
                        Log.e(TAG, "Error al parsear fechaVencimiento: " + fechaVencimientoObj, e);
                    }
                } else {
                    Log.w(TAG, "Tipo de fechaVencimiento no reconocido: " + fechaVencimientoObj.getClass().getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar fechaVencimiento", e);
            }
        }
        
        // También verificar fechaVencimientoTimestamp si existe
        Object fechaVencimientoTimestampObj = document.get("fechaVencimientoTimestamp");
        if (fechaVencimientoTimestampObj != null && fechaVencimientoTimestampObj instanceof com.google.firebase.Timestamp) {
            medicamento.setFechaVencimiento(((com.google.firebase.Timestamp) fechaVencimientoTimestampObj).toDate());
        }
        
        // Manejar fechaInicioTratamiento: puede venir como Timestamp o como string ISO
        Object fechaInicioObj = document.get("fechaInicioTratamiento");
        if (fechaInicioObj != null) {
            try {
                if (fechaInicioObj instanceof com.google.firebase.Timestamp) {
                    medicamento.setFechaInicioTratamiento(((com.google.firebase.Timestamp) fechaInicioObj).toDate());
                } else if (fechaInicioObj instanceof String) {
                    try {
                        String fechaStr = (String) fechaInicioObj;
                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        medicamento.setFechaInicioTratamiento(isoFormat.parse(fechaStr));
                    } catch (ParseException e) {
                        Log.e(TAG, "Error al parsear fechaInicioTratamiento: " + fechaInicioObj, e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar fechaInicioTratamiento", e);
            }
        }
        if (document.getString("tipoStock") != null) {
            medicamento.setTipoStock(Medicamento.TipoStock.valueOf(document.getString("tipoStock")));
        }
        if (document.get("diasEstimadosDuracion") != null) {
            medicamento.setDiasEstimadosDuracion(document.getLong("diasEstimadosDuracion").intValue());
        }
        if (document.get("diasRestantesDuracion") != null) {
            medicamento.setDiasRestantesDuracion(document.getLong("diasRestantesDuracion").intValue());
        }
        
        Logger.d(TAG, String.format("mapToMedicamento: ✅ Mapeo completado para '%s' (ID: %s) - TomasDiarias: %d, StockActual: %d, Pausado: %s", 
            medicamento.getNombre(), medicamento.getId(), medicamento.getTomasDiarias(), 
            medicamento.getStockActual(), medicamento.isPausado()));
        Logger.d(TAG, "mapToMedicamento: ========== MAPEO FINALIZADO ==========");
        
        return medicamento;
    }

    /**
     * Elimina todos los medicamentos del usuario actual.
     * Usa operaciones batch para eficiencia.
     * 
     * @param callback Callback para manejar el resultado. Puede ser null.
     *                 onSuccess se llama cuando todos los medicamentos son eliminados.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void eliminarTodosLosMedicamentos(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        deleteTasks.add(document.getReference().delete());
                    }
                    
                    if (deleteTasks.isEmpty()) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                        return;
                    }
                    
                    com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
                        .addOnSuccessListener(aVoid -> {
                            Logger.d(TAG, "Todos los medicamentos eliminados exitosamente");
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Logger.e(TAG, "Error al eliminar medicamentos", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        });
                } else {
                    Logger.e(TAG, "Error al obtener medicamentos para eliminar", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Elimina todas las tomas del usuario actual.
     * Usa operaciones batch para eficiencia.
     * 
     * @param callback Callback para manejar el resultado. Puede ser null.
     *                 onSuccess se llama cuando todas las tomas son eliminadas.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void eliminarTodasLasTomas(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_TOMAS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        deleteTasks.add(document.getReference().delete());
                    }
                    
                    if (deleteTasks.isEmpty()) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                        return;
                    }
                    
                    com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
                        .addOnSuccessListener(aVoid -> {
                            Logger.d(TAG, "Todas las tomas eliminadas exitosamente");
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Logger.e(TAG, "Error al eliminar tomas", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        });
                } else {
                    Logger.e(TAG, "Error al obtener tomas para eliminar", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Elimina el documento de usuario en Firestore.
     * 
     * @param callback Callback para manejar el resultado. Puede ser null.
     *                 onSuccess se llama cuando el usuario es eliminado.
     *                 onError recibe la excepción si ocurre un error.
     */
    public void eliminarUsuario(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_USUARIOS)
            .document(firebaseUser.getUid())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Logger.d(TAG, "Usuario eliminado de Firestore exitosamente");
                if (callback != null) {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                Logger.e(TAG, "Error al eliminar usuario de Firestore", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }

    // ==================== INTERFACES ====================

    /**
     * Obtiene el documento de un medicamento directamente desde Firestore.
     * Útil para acceder a campos que no están en el modelo (como eventoIdsGoogleCalendar).
     * 
     * @param medicamentoId ID del medicamento.
     * @param callback Callback con el DocumentSnapshot.
     */
    public void obtenerMedicamentoDocumento(String medicamentoId, FirestoreDocumentCallback callback) {
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamentoId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (callback != null) {
                        callback.onSuccess(document);
                    }
                } else {
                    Logger.e(TAG, "Error al obtener documento de medicamento", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onError(Exception exception);
    }

    public interface FirestoreListCallback {
        void onSuccess(List<?> result);
        void onError(Exception exception);
    }

    public interface FirestoreDocumentCallback {
        void onSuccess(DocumentSnapshot document);
        void onError(Exception exception);
    }
}

