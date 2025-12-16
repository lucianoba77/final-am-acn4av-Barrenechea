package com.controlmedicamentos.myapplication.services;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import androidx.annotation.NonNull;

/**
 * Servicio para manejar la autenticación con Firebase
 */
public class AuthService {
    private static final String TAG = "AuthService";
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    public AuthService() {
        mAuth = FirebaseAuth.getInstance();
    }
    
    /**
     * Verifica si Google Play Services está disponible y actualizado
     * @param context Contexto de la aplicación
     * @return true si Google Play Services está disponible, false en caso contrario
     */
    public boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }
    
    /**
     * Inicializa el cliente de Google Sign-In
     * @param context Contexto de la aplicación
     * @param webClientId ID del cliente OAuth 2.0 de tipo Web obtenido de Firebase Console
     */
    public void initializeGoogleSignIn(Context context, String webClientId) {
        // Verificar que Google Play Services esté disponible antes de inicializar
        if (!isGooglePlayServicesAvailable(context)) {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
            Log.w(TAG, "Google Play Services no está disponible. Código: " + resultCode);
            // No impedimos la inicialización, pero el login con Google fallará
            // El usuario verá un mensaje de error apropiado
        }
        
        // Configurar para que siempre muestre el selector de cuenta
        // No especificar accountName para permitir seleccionar cualquier cuenta
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    /**
     * Obtiene el Intent de Google Sign-In, forzando el selector de cuenta.
     * Primero hace signOut para limpiar cualquier cuenta previamente seleccionada.
     * @param callback Callback para notificar cuando el Intent está listo
     */
    public void getGoogleSignInIntentWithAccountSelector(final GoogleSignInIntentCallback callback) {
        if (googleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient no inicializado. Llama a initializeGoogleSignIn primero.");
            if (callback != null) {
                callback.onError(new Exception("GoogleSignInClient no inicializado"));
            }
            return;
        }
        
        // Primero hacer signOut para limpiar cualquier cuenta previamente seleccionada
        // Esto fuerza que se muestre el selector de cuenta
        googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Incluso si signOut falla, continuar con el Intent
                // El signOut puede fallar si no hay sesión activa, lo cual está bien
                Log.d(TAG, "SignOut completado antes de mostrar selector de cuenta");
                
                // Ahora obtener el Intent que mostrará el selector de cuenta
                android.content.Intent signInIntent = googleSignInClient.getSignInIntent();
                if (callback != null) {
                    callback.onSuccess(signInIntent);
                }
            }
        });
    }
    
    /**
     * Obtiene el Intent de Google Sign-In (método legacy, mantiene compatibilidad)
     * @return Intent para iniciar el flujo de Google Sign-In
     */
    public android.content.Intent getGoogleSignInIntent() {
        if (googleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient no inicializado. Llama a initializeGoogleSignIn primero.");
            return null;
        }
        return googleSignInClient.getSignInIntent();
    }
    
    /**
     * Callback para obtener el Intent de Google Sign-In con selector de cuenta
     */
    public interface GoogleSignInIntentCallback {
        void onSuccess(android.content.Intent signInIntent);
        void onError(Exception exception);
    }
    
    /**
     * Inicia sesión con Google usando el resultado del Intent
     * @param idToken Token ID de Google
     * @param callback Callback para manejar el resultado
     */
    public void signInWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Inicio de sesión con Google exitoso");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.e(TAG, "Error al iniciar sesión con Google", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }
    
    /**
     * Maneja el resultado del Intent de Google Sign-In
     * @param data Intent resultante del flujo de Google Sign-In
     * @param callback Callback para manejar el resultado
     */
    public void handleGoogleSignInResult(android.content.Intent data, AuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                signInWithGoogle(account.getIdToken(), callback);
            } else {
                Log.e(TAG, "No se pudo obtener el token ID de Google");
                if (callback != null) {
                    callback.onError(new Exception("No se pudo obtener el token de Google"));
                }
            }
        } catch (ApiException e) {
            Log.e(TAG, "Error en Google Sign-In", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Cierra la sesión de Google y revoca el acceso para permitir seleccionar otra cuenta
     */
    public void signOutGoogle() {
        if (googleSignInClient != null) {
            // Primero revocar acceso para limpiar la cuenta seleccionada
            googleSignInClient.revokeAccess().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    // Luego hacer signOut
                    googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "Sesión de Google cerrada y acceso revocado");
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Inicializa Google Sign-In específicamente para Google Calendar
     * Usa requestServerAuthCode para obtener un auth code que se intercambia por access token
     * @param context Context de la aplicación
     * @param webClientId Web Client ID de Google Cloud Console
     * @return GoogleSignInClient configurado para Calendar, o null si Google Play Services no está disponible
     */
    public GoogleSignInClient initializeGoogleSignInForCalendar(Context context, String webClientId) {
        // Verificar que Google Play Services esté disponible
        if (!isGooglePlayServicesAvailable(context)) {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
            Log.w(TAG, "Google Play Services no está disponible para Google Calendar. Código: " + resultCode);
            
            // Mostrar mensaje de error si es necesario
            if (resultCode != ConnectionResult.SUCCESS) {
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    // El error es resuelto por el usuario (como actualizar Play Services)
                    Log.w(TAG, "Error resuelto por el usuario: " + apiAvailability.getErrorString(resultCode));
                }
            }
            
            // Retornar null en lugar de intentar crear el cliente
            return null;
        }
        
        try {
            // Configurar Google Sign-In con scope de Calendar y requestServerAuthCode
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(webClientId) // Obtener auth code en lugar de id token
                .requestScopes(new com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar.events"))
                .build();
            
            return GoogleSignIn.getClient(context, gso);
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar Google Sign-In para Calendar", e);
            return null;
        }
    }
    
    /**
     * Obtiene el usuario actual autenticado
     * @return FirebaseUser si hay usuario autenticado, null en caso contrario
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Verifica si hay un usuario autenticado
     * @return true si hay usuario autenticado, false en caso contrario
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Registra un nuevo usuario con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Usuario registrado exitosamente");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.e(TAG, "Error al registrar usuario", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Inicia sesión con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    /**
     * Inicia sesión con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "Error al autenticar usuario", exception);
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                }
            });
    }

    /**
     * Envía un email para recuperar la contraseña
     * @param email Email del usuario
     * @param callback Callback para manejar el resultado
     */
    public void resetPassword(String email, AuthCallback callback) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email de recuperación enviado");
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error al enviar email de recuperación", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Cierra la sesión del usuario actual
     */
    public void logout() {
        mAuth.signOut();
        Log.d(TAG, "Usuario cerró sesión");
    }

    /**
     * Interfaz para callbacks de autenticación
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception exception);
    }
}

