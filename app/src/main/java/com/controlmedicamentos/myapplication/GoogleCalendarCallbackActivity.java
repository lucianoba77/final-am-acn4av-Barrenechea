package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity que maneja el callback de OAuth de Google Calendar
 * Se ejecuta cuando Google redirige de vuelta después de la autorización
 * usando el flujo OAuth 2.0 implícito (response_type=token)
 */
public class GoogleCalendarCallbackActivity extends AppCompatActivity {
    private static final String TAG = "GoogleCalendarCallback";
    
    private AuthService authService;
    private GoogleCalendarAuthService googleCalendarAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ocultar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        authService = new AuthService();
        googleCalendarAuthService = new GoogleCalendarAuthService(this);
        
        // Procesar el callback OAuth
        procesarCallback();
    }
    
    private void procesarCallback() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data == null) {
            Log.e(TAG, "No se recibió data en el callback");
            mostrarErrorYRegresar("No se pudo obtener la información de Google Calendar");
            return;
        }
        
        String accessToken = null;
        String expiresIn = null;
        String tokenType = null;
        
        // Primero intentar obtener del query (viene desde la página HTML de Firebase Hosting)
        accessToken = data.getQueryParameter("access_token");
        expiresIn = data.getQueryParameter("expires_in");
        tokenType = data.getQueryParameter("token_type");
        
        // Si no está en query, intentar del fragment (OAuth implícito directo)
        if (accessToken == null || accessToken.isEmpty()) {
            String fragment = data.getFragment();
            
            if (fragment != null && !fragment.isEmpty()) {
                Map<String, String> params = parseFragment(fragment);
                accessToken = params.get("access_token");
                expiresIn = params.get("expires_in");
                tokenType = params.get("token_type");
            }
        }
        
        // Si aún no hay token, verificar si hay error
        if (accessToken == null || accessToken.isEmpty()) {
            String error = data.getQueryParameter("error");
            if (error == null) {
                error = data.getFragment() != null ? parseFragment(data.getFragment()).get("error") : null;
            }
            
            if (error != null) {
                String errorDescription = data.getQueryParameter("error_description");
                if (errorDescription == null && data.getFragment() != null) {
                    errorDescription = parseFragment(data.getFragment()).get("error_description");
                }
                String mensajeError = errorDescription != null ? errorDescription : "Error desconocido";
                Log.e(TAG, "Error en OAuth: " + error + " - " + mensajeError);
                mostrarErrorYRegresar("No se pudo conectar con Google Calendar: " + mensajeError);
                return;
            }
            
            Log.e(TAG, "No se encontró access_token en la URL");
            mostrarErrorYRegresar("No se pudo obtener el token de acceso de Google. Intenta nuevamente.");
            return;
        }
        
        // Verificar que el usuario esté autenticado
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado");
            mostrarErrorYRegresar("Sesión no disponible. Por favor, inicia sesión nuevamente.");
            return;
        }
        
        String userId = currentUser.getUid();
        
        // Preparar los datos del token para guardar
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("access_token", accessToken);
        tokenData.put("token_type", tokenType != null ? tokenType : "Bearer");
        tokenData.put("expires_in", expiresIn != null ? Integer.parseInt(expiresIn) : 3600);
        tokenData.put("fechaObtencion", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));
        
        // Calcular fecha de expiración
        int expiresInSeconds = expiresIn != null ? Integer.parseInt(expiresIn) : 3600;
        long expirationTime = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        tokenData.put("fechaExpiracion", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date(expirationTime)));
        
        // Guardar el token en Firestore
        googleCalendarAuthService.guardarTokenGoogle(tokenData, 
            new GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Token de Google Calendar guardado exitosamente");
                    
                    // Esperar un momento para asegurar que el token se haya guardado en Firestore
                    // antes de regresar a AjustesActivity
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GoogleCalendarCallbackActivity.this, 
                                "Google Calendar conectado exitosamente", 
                                Toast.LENGTH_SHORT).show();
                            
                            // Regresar a AjustesActivity con flag para indicar que se conectó exitosamente
                            Intent ajustesIntent = new Intent(GoogleCalendarCallbackActivity.this, AjustesActivity.class);
                            ajustesIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            ajustesIntent.putExtra("google_calendar_conectado", true);
                            startActivity(ajustesIntent);
                            finish();
                        }
                    }, 500); // Esperar 500ms para que Firestore complete la escritura
                }
                
                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error al guardar token de Google Calendar", exception);
                    mostrarErrorYRegresar("Error al guardar el token: " + 
                        (exception != null ? exception.getMessage() : "Error desconocido"));
                }
            });
    }
    
    /**
     * Parsea el fragment de la URL para extraer los parámetros OAuth
     */
    private Map<String, String> parseFragment(String fragment) {
        Map<String, String> params = new HashMap<>();
        
        if (fragment == null || fragment.isEmpty()) {
            return params;
        }
        
        // El fragment tiene formato: access_token=xxx&token_type=Bearer&expires_in=3600
        String[] pairs = fragment.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = Uri.decode(keyValue[0]);
                String value = Uri.decode(keyValue[1]);
                params.put(key, value);
            }
        }
        
        return params;
    }
    
    /**
     * Muestra un error y regresa a AjustesActivity
     */
    private void mostrarErrorYRegresar(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        
        Intent ajustesIntent = new Intent(this, AjustesActivity.class);
        ajustesIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(ajustesIntent);
        finish();
    }
}

