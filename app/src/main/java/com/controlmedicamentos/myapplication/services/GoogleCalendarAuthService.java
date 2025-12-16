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
                                            if (callback != null) {
                                                callback.onSuccess(result);
                                            }
                                        } else {
                                            // No se pudo renovar, eliminar token
                                            Log.d(TAG, "No se pudo renovar el token, eliminando");
                                            eliminarTokenGoogle(new FirestoreCallback() {
                                                @Override
                                                public void onSuccess(Object result) {
                                                    if (callback != null) {
                                                        callback.onSuccess(null);
                                                    }
                                                }
                                                
                                                @Override
                                                public void onError(Exception exception) {
                                                    if (callback != null) {
                                                        callback.onSuccess(null);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                    
                                    @Override
                                    public void onError(Exception exception) {
                                        // Error al renovar, eliminar token
                                        Log.e(TAG, "Error al renovar token, eliminando", exception);
                                        eliminarTokenGoogle(new FirestoreCallback() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                if (callback != null) {
                                                    callback.onSuccess(null);
                                                }
                                            }
                                            
                                            @Override
                                            public void onError(Exception exception) {
                                                if (callback != null) {
                                                    callback.onSuccess(null);
                                                }
                                            }
                                        });
                                    }
                                });
                            } else {
                                // Token válido, retornarlo
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
        
        // Realizar petición para renovar el token
        new Thread(() -> {
            try {
                String url = "https://oauth2.googleapis.com/token";
                String postData = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8") +
                                 "&client_secret=" + java.net.URLEncoder.encode(clientSecret, "UTF-8") +
                                 "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8") +
                                 "&grant_type=refresh_token";
                
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                
                java.io.OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parsear respuesta JSON
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
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
                    Log.e(TAG, "Error al renovar token: " + responseCode);
                    if (callback != null) {
                        callback.onError(new Exception("Error al renovar token: " + responseCode));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al renovar token de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
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
     */
    public void tieneGoogleCalendarConectado(FirestoreCallback callback) {
        obtenerTokenGoogle(new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                boolean conectado = result != null && 
                    result instanceof Map && 
                    ((Map<?, ?>) result).containsKey("access_token");
                
                if (callback != null) {
                    callback.onSuccess(conectado);
                }
            }
            
            @Override
            public void onError(Exception exception) {
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

