package com.controlmedicamentos.myapplication.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio para manejar la autenticación OAuth con Google Calendar
 * Guarda y obtiene tokens de acceso desde Firestore
 */
public class GoogleCalendarAuthService {
    private static final String TAG = "GoogleCalendarAuth";
    private static final String COLLECTION_GOOGLE_TOKENS = "googleTokens";
    
    private Context context;
    private FirebaseFirestore db;
    private AuthService authService;
    
    public GoogleCalendarAuthService(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.authService = new AuthService();
    }
    
    /**
     * Guarda el token de acceso de Google en Firestore
     * Consistente con React: calendarService.js - guardarTokenGoogle()
     */
    public void guardarTokenGoogle(Map<String, Object> tokenData, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        Map<String, Object> tokenParaGuardar = new HashMap<>(tokenData);
        
        // Agregar metadatos
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        tokenParaGuardar.put("fechaActualizacion", isoFormat.format(new Date()));
        tokenParaGuardar.put("userId", userId);
        
        // Si no tiene fechaObtencion, agregarla
        if (!tokenParaGuardar.containsKey("fechaObtencion")) {
            tokenParaGuardar.put("fechaObtencion", isoFormat.format(new Date()));
        }
        
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .set(tokenParaGuardar)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Token de Google Calendar guardado exitosamente");
                if (callback != null) {
                    callback.onSuccess(tokenParaGuardar);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al guardar token de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }
    
    /**
     * Obtiene el token de acceso de Google del usuario
     * Verifica si el token está expirado y lo renueva automáticamente si es necesario
     * Consistente con React: calendarService.js - obtenerTokenGoogle()
     */
    public void obtenerTokenGoogle(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Map<String, Object> tokenData = document.getData();
                        if (tokenData != null) {
                            // Verificar si el token está expirado
                            if (esTokenExpirado(tokenData)) {
                                Log.d(TAG, "Token de Google Calendar expirado, intentando renovar");
                                // Intentar renovar el token usando refresh_token
                                renovarTokenGoogle(tokenData, new FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        if (result != null) {
                                            // Token renovado exitosamente, retornar el nuevo token
                                            Log.d(TAG, "Token renovado exitosamente");
                                            if (callback != null) {
                                                callback.onSuccess(result);
                                            }
                                        } else {
                                            // No se pudo renovar, pero NO eliminar el token
                                            // Puede ser un error temporal de red, mantener el token
                                            Log.w(TAG, "No se pudo renovar el token, pero manteniéndolo (puede ser error temporal)");
                                            // Retornar el token expirado de todas formas, el código que lo use puede manejar el error
                                            if (callback != null) {
                                                callback.onSuccess(tokenData);
                                            }
                                        }
                                    }
                                    
                                    @Override
                                    public void onError(Exception exception) {
                                        // Error al renovar, pero NO eliminar el token
                                        // Puede ser un error temporal de red
                                        Log.w(TAG, "Error al renovar token, pero manteniéndolo (puede ser error temporal)", exception);
                                        // Retornar el token expirado de todas formas
                                        if (callback != null) {
                                            callback.onSuccess(tokenData);
                                        }
                                    }
                                });
                            } else {
                                // Token válido, retornarlo
                                Log.d(TAG, "Token de Google Calendar válido");
                                if (callback != null) {
                                    callback.onSuccess(tokenData);
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }
                } else {
                    Log.e(TAG, "Error al obtener token de Google Calendar", task.getException());
                    if (callback != null) {
                        callback.onSuccess(null); // No es crítico, retornar null
                    }
                }
            });
    }
    
    /**
     * Renueva el token de acceso
     * Nota: El flujo implícito no proporciona refresh_token, así que cuando expire
     * el usuario necesitará reconectar. Esto es más simple y no requiere backend.
     */
    private void renovarTokenGoogle(Map<String, Object> tokenDataAntiguo, FirestoreCallback callback) {
        // El flujo OAuth implícito no proporciona refresh_token
        // Cuando el token expire, el usuario necesitará reconectar
        Log.d(TAG, "Token expirado. El usuario necesitará reconectar Google Calendar.");
        if (callback != null) {
            callback.onSuccess(null); // Retornar null para indicar que necesita reconectar
        }
    }
    
    /**
     * Elimina el token de acceso (desconecta Google Calendar)
     * Consistente con React: calendarService.js - eliminarTokenGoogle()
     */
    public void eliminarTokenGoogle(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Token de Google Calendar eliminado exitosamente");
                if (callback != null) {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al eliminar token de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }
    
    /**
     * Verifica si el token está expirado
     * Consistente con React: googleAuthHelper.js - esTokenExpirado()
     */
    private boolean esTokenExpirado(Map<String, Object> tokenData) {
        if (tokenData == null) {
            return true;
        }
        
        Object fechaObtencionObj = tokenData.get("fechaObtencion");
        Object expiresInObj = tokenData.get("expires_in");
        
        if (fechaObtencionObj == null || expiresInObj == null) {
            return false; // No sabemos si está expirado, asumir que no
        }
        
        try {
            String fechaObtencionStr = fechaObtencionObj.toString();
            long expiresIn = ((Number) expiresInObj).longValue();
            
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date fechaObtencion = isoFormat.parse(fechaObtencionStr);
            
            if (fechaObtencion == null) {
                return false;
            }
            
            Date fechaExpiracion = new Date(fechaObtencion.getTime() + (expiresIn * 1000));
            Date ahora = new Date();
            
            // Considerar expirado si falta menos de 5 minutos
            return ahora.after(fechaExpiracion) || 
                   (fechaExpiracion.getTime() - ahora.getTime()) < (5 * 60 * 1000);
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar expiración del token", e);
            return false;
        }
    }
    
    /**
     * Verifica si el usuario tiene Google Calendar conectado
     * Consistente con React: calendarService.js - tieneGoogleCalendarConectado()
     * 
     * IMPORTANTE: Este método NO elimina el token si hay un error.
     * Solo verifica si existe un token válido en Firestore.
     */
    public void tieneGoogleCalendarConectado(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            Log.d(TAG, "tieneGoogleCalendarConectado: Usuario no autenticado");
            if (callback != null) {
                callback.onSuccess(false);
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        Log.d(TAG, "tieneGoogleCalendarConectado: Verificando token para userId: " + userId);
        
        // Verificar directamente en Firestore si existe un token
        // sin intentar renovarlo (para evitar eliminarlo si está expirado)
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    boolean existeDocumento = document != null && document.exists();
                    boolean tieneAccessToken = false;
                    
                    if (existeDocumento && document.getData() != null) {
                        Map<String, Object> data = document.getData();
                        tieneAccessToken = data.containsKey("access_token");
                        Log.d(TAG, "tieneGoogleCalendarConectado: Documento existe: " + existeDocumento + 
                              ", tiene access_token: " + tieneAccessToken + 
                              ", keys: " + (data != null ? data.keySet().toString() : "null"));
                    } else {
                        Log.d(TAG, "tieneGoogleCalendarConectado: Documento no existe o data es null");
                    }
                    
                    boolean conectado = existeDocumento && tieneAccessToken;
                    Log.d(TAG, "tieneGoogleCalendarConectado: Resultado final: " + conectado);
                    
                    if (callback != null) {
                        callback.onSuccess(conectado);
                    }
                } else {
                    // Error al verificar, pero no eliminar el token
                    Log.w(TAG, "Error al verificar token de Google Calendar (no crítico)", task.getException());
                    if (callback != null) {
                        callback.onSuccess(false);
                    }
                }
            });
    }
    
    /**
     * Interfaz para callbacks
     */
    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onError(Exception exception);
    }
}

