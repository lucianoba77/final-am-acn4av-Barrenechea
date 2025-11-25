package com.controlmedicamentos.myapplication.services;

import android.util.Log;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * Guarda un nuevo medicamento en Firestore
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
                    Log.d(TAG, "Medicamento guardado con ID: " + documentReference.getId());
                    medicamento.setId(documentReference.getId());
                    if (callback != null) {
                        callback.onSuccess(medicamento);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al guardar medicamento", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Actualiza un medicamento existente
     * Consistente con React: medicamentosService.js - actualizarMedicamento()
     */
    public void actualizarMedicamento(Medicamento medicamento, FirestoreCallback callback) {
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
                    if (medicamentoActualData != null) {
                        // Obtener valores actuales
                        Object stockInicialActualObj = medicamentoActualData.get("stockInicial");
                        Object stockActualActualObj = medicamentoActualData.get("stockActual");
                        
                        int stockInicialActual = 0;
                        int stockActualActual = 0;
                        
                        if (stockInicialActualObj instanceof Number) {
                            stockInicialActual = ((Number) stockInicialActualObj).intValue();
                        }
                        if (stockActualActualObj instanceof Number) {
                            stockActualActual = ((Number) stockActualActualObj).intValue();
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
                            Log.d(TAG, "Medicamento actualizado exitosamente");
                            if (callback != null) {
                                callback.onSuccess(medicamento);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al actualizar medicamento", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        });
                } else {
                    Log.e(TAG, "Error al obtener medicamento para actualizar", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
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
                            Log.d(TAG, "Stock restado exitosamente. Nuevo stock: " + nuevoStock);
                            if (callback != null) {
                                Map<String, Object> resultado = new HashMap<>();
                                resultado.put("stockActual", nuevoStock);
                                resultado.put("tomaRegistrada", esOcasional);
                                callback.onSuccess(resultado);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al restar stock", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        });
                } else {
                    Log.e(TAG, "Error al obtener medicamento para restar stock", task.getException());
                    if (callback != null) {
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Elimina un medicamento
     */
    public void eliminarMedicamento(String medicamentoId, FirestoreCallback callback) {
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamentoId)
            .delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Medicamento eliminado exitosamente");
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al eliminar medicamento", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
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

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Medicamento> medicamentos = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Medicamento medicamento = mapToMedicamento(document);
                            medicamentos.add(medicamento);
                        }
                        if (callback != null) {
                            callback.onSuccess(medicamentos);
                        }
                    } else {
                        Log.e(TAG, "Error al obtener medicamentos", task.getException());
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
                    Log.e(TAG, "Error al obtener medicamento", task.getException());
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

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("userId", firebaseUser.getUid())
            .whereEqualTo("activo", true)
            .whereEqualTo("pausado", false)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Medicamento> medicamentos = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Medicamento medicamento = mapToMedicamento(document);
                            medicamentos.add(medicamento);
                        }
                        if (callback != null) {
                            callback.onSuccess(medicamentos);
                        }
                    } else {
                        Log.e(TAG, "Error al obtener medicamentos activos", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    // ==================== TOMAS ====================

    /**
     * Guarda una toma en Firestore
     */
    public void guardarToma(Toma toma, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        Map<String, Object> tomaMap = tomaToMap(toma);
        tomaMap.put("userId", firebaseUser.getUid());
        tomaMap.put("fechaCreacion", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_TOMAS)
            .add(tomaMap)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "Toma guardada con ID: " + documentReference.getId());
                    toma.setId(documentReference.getId());
                    if (callback != null) {
                        callback.onSuccess(toma);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al guardar toma", e);
                    if (callback != null) {
                        callback.onError(e);
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
        map.put("horarioPrimeraToma", horario.isEmpty() ? "00:00" : horario);
        
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
            medicamento.setHorarioPrimeraToma(primeraToma != null && !primeraToma.isEmpty() ? primeraToma : "00:00");
        } else {
            // Medicamento ocasional: mantener vacío
            medicamento.setHorarioPrimeraToma(primeraToma != null && !primeraToma.isEmpty() ? primeraToma : "");
        }
        
        // Ahora establecer tomasDiarias después de horarioPrimeraToma
        medicamento.setTomasDiarias(tomasDiarias);
        
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
                medicamento.setStockActual(((Number) stockObj).intValue());
            }
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
        medicamento.setPausado(document.getBoolean("pausado") != null ? document.getBoolean("pausado") : false);
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
        return medicamento;
    }

    private Map<String, Object> tomaToMap(Toma toma) {
        Map<String, Object> map = new HashMap<>();
        map.put("medicamentoId", toma.getMedicamentoId());
        if (toma.getFechaHoraProgramada() != null) {
            map.put("fechaHoraProgramada", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(toma.getFechaHoraProgramada().getTime())));
        }
        if (toma.getFechaHoraTomada() != null) {
            map.put("fechaHoraTomada", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(toma.getFechaHoraTomada().getTime())));
        }
        map.put("estado", toma.getEstado().name());
        map.put("observaciones", toma.getObservaciones());
        return map;
    }


    // ==================== INTERFACES ====================

    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onError(Exception exception);
    }

    public interface FirestoreListCallback {
        void onSuccess(List<?> result);
        void onError(Exception exception);
    }
}

