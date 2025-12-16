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
     * Renueva el token de acceso usando el refresh_token
     * Consistente con React: calendarService.js - renovarTokenGoogle()
     */
    private void renovarTokenGoogle(Map<String, Object> tokenDataAntiguo, FirestoreCallback callback) {
        Object refreshTokenObj = tokenDataAntiguo.get("refresh_token");
        if (refreshTokenObj == null) {
            Log.d(TAG, "No hay refresh_token disponible para renovar");
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }
        
        String refreshToken = refreshTokenObj.toString();
        
        // Obtener client_id y client_secret desde SharedPreferences o recursos
        SharedPreferences prefs = context.getSharedPreferences("ControlMedicamentos", Context.MODE_PRIVATE);
        String clientId = prefs.getString("google_calendar_client_id", null);
        String clientSecret = prefs.getString("google_calendar_client_secret", null);
        
        if (clientId == null || clientSecret == null) {
            Log.e(TAG, "Client ID o Client Secret no configurados");
            if (callback != null) {
                callback.onError(new Exception("Configuración de Google Calendar incompleta"));
            }
            return;
        }
        
        // Realizar petición para renovar el token usando OkHttpClient (ya usado en GoogleCalendarService)
        // Esto evita crear threads manuales y maneja mejor el ciclo de vida
        okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient();
        
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
        String postData;
        try {
            postData = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8") +
                       "&client_secret=" + java.net.URLEncoder.encode(clientSecret, "UTF-8") +
                       "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8") +
                       "&grant_type=refresh_token";
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(TAG, "Error al codificar datos para renovar token", e);
            if (callback != null) {
                callback.onError(new Exception("Error al preparar petición de renovación de token", e));
            }
            return;
        }
        
        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, postData);
        okhttp3.Request request = new okhttp3.Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(body)
            .build();
        
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "Error al renovar token de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
            
            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        
                        // Parsear respuesta JSON
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                        String newAccessToken = jsonResponse.getString("access_token");
                        long expiresIn = jsonResponse.optLong("expires_in", 3600);
                        
                        // Crear nuevo token data
                        Map<String, Object> nuevoTokenData = new HashMap<>(tokenDataAntiguo);
                        nuevoTokenData.put("access_token", newAccessToken);
                        nuevoTokenData.put("expires_in", expiresIn);
                        
                        // Actualizar fecha de obtención
                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        nuevoTokenData.put("fechaObtencion", isoFormat.format(new Date()));
                        
                        // Guardar el nuevo token
                        guardarTokenGoogle(nuevoTokenData, new FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Log.d(TAG, "Token renovado y guardado exitosamente");
                                if (callback != null) {
                                    callback.onSuccess(nuevoTokenData);
                                }
                            }
                            
                            @Override
                            public void onError(Exception exception) {
                                Log.e(TAG, "Error al guardar token renovado", exception);
                                if (callback != null) {
                                    callback.onError(exception);
                                }
                            }
                        });
                    } else {
                        String errorMessage = "Error al renovar token: " + response.code();
                        if (response.body() != null) {
                            try {
                                errorMessage += " - " + response.body().string();
                            } catch (Exception e) {
                                // Ignorar error al leer body
                            }
                        }
                        Log.e(TAG, errorMessage);
                        if (callback != null) {
                            callback.onError(new Exception(errorMessage));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al procesar respuesta de renovación de token", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                } finally {
                    if (response != null && response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
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
            if (callback != null) {
                callback.onSuccess(false);
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        // Verificar directamente en Firestore si existe un token
        // sin intentar renovarlo (para evitar eliminarlo si está expirado)
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    boolean conectado = document != null && 
                                      document.exists() && 
                                      document.getData() != null &&
                                      document.getData().containsKey("access_token");
                    
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

