package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister, btnGoogleSignIn;
    private TextView tvOlvidoPassword;
    private ProgressBar progressBar;
    private AuthService authService;
    private boolean isLoginMode = true; // true = login, false = registro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado ANTES de setContentView
        configurarBarraEstado();
        
        setContentView(R.layout.activity_login);
        
        // Asegurar que la barra de estado sea visible después de setContentView
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                configurarBarraEstado();
            }
        });

        // Inicializar servicio de autenticación
        authService = new AuthService();
        
        // Inicializar Google Sign-In
        String webClientId = getString(R.string.default_web_client_id);
        authService.initializeGoogleSignIn(this, webClientId);

        // Verificar si ya hay un usuario autenticado
        verificarSesionExistente();

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();
    }

    private void verificarSesionExistente() {
        if (authService.isUserLoggedIn()) {
            // Usuario ya autenticado, ir a MainActivity
            irAMainActivity();
        }
    }

    private void inicializarVistas() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvOlvidoPassword = findViewById(R.id.tvOlvidoPassword);
        
        // Intentar encontrar ProgressBar en el layout (si existe)
        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void configurarListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = true;
                realizarLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = false;
                realizarRegistro();
            }
        });

        // Listener para recuperar contraseña
        if (tvOlvidoPassword != null) {
            tvOlvidoPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarDialogoRecuperarPassword();
                }
            });
        }
        
        // Listener para Google Sign-In
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    realizarLoginGoogle();
                }
            });
        }
    }

    private void realizarLogin() {
        // Obtener datos de los campos
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar campos
        if (!validarCampos(email, password)) {
            return;
        }

        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar progreso
        mostrarProgreso(true);

        // Realizar login con Firebase
        authService.loginUser(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                mostrarProgreso(false);
                Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                irAMainActivity();
            }

            @Override
            public void onError(Exception exception) {
                mostrarProgreso(false);
                
                if (exception != null) {
                    Log.e(TAG, "Error en login", exception);
                    String mensaje = obtenerMensajeErrorLogin(exception);
                    Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Error desconocido al iniciar sesión", 
                        Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void realizarRegistro() {
        // Obtener datos de los campos
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar campos
        if (!validarCampos(email, password)) {
            return;
        }

        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Validar que la contraseña sea más segura para registro
        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        // Mostrar progreso
        mostrarProgreso(true);

        // Realizar registro con Firebase
        authService.registerUser(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                mostrarProgreso(false);
                Toast.makeText(LoginActivity.this, "¡Registro exitoso! Bienvenido", Toast.LENGTH_SHORT).show();
                irAMainActivity();
            }

            @Override
            public void onError(Exception exception) {
                mostrarProgreso(false);
                String mensaje = obtenerMensajeErrorRegistro(exception);
                Log.e(TAG, "Error en registro: ", exception);
                Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Cerrar LoginActivity
    }

    private void mostrarProgreso(boolean mostrar) {
        if (progressBar != null) {
            progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!mostrar);
        btnRegister.setEnabled(!mostrar);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setEnabled(!mostrar);
        }
        etEmail.setEnabled(!mostrar);
        etPassword.setEnabled(!mostrar);
    }
    
    /**
     * Inicia el flujo de login con Google
     */
    private void realizarLoginGoogle() {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Verificar que Google Play Services esté disponible
        if (!authService.isGooglePlayServicesAvailable(this)) {
            Toast.makeText(this, 
                "Google Play Services no está disponible. " +
                "Por favor actualiza Google Play Services o usa un dispositivo con Google Play Store.",
                Toast.LENGTH_LONG).show();
            Log.w(TAG, "Google Play Services no disponible para Google Sign-In");
            return;
        }
        
        // Mostrar progreso mientras se prepara el selector de cuenta
        mostrarProgreso(true);
        
        // Obtener Intent con selector de cuenta (hace signOut primero para limpiar cuenta previa)
        authService.getGoogleSignInIntentWithAccountSelector(new AuthService.GoogleSignInIntentCallback() {
            @Override
            public void onSuccess(Intent signInIntent) {
                mostrarProgreso(false);
                if (signInIntent != null) {
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                } else {
                    Toast.makeText(LoginActivity.this, "Error al inicializar Google Sign-In", Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onError(Exception exception) {
                mostrarProgreso(false);
                Log.e(TAG, "Error al obtener Intent de Google Sign-In", exception);
                Toast.makeText(LoginActivity.this, 
                    "Error al inicializar Google Sign-In: " + 
                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_SIGN_IN) {
            mostrarProgreso(true);
            authService.handleGoogleSignInResult(data, new AuthService.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    mostrarProgreso(false);
                    Log.d(TAG, "Login con Google exitoso. Usuario UID: " + (user != null ? user.getUid() : "null"));
                    Toast.makeText(LoginActivity.this, "¡Bienvenido con Google!", Toast.LENGTH_SHORT).show();
                    
                    // Verificar si el usuario ya existe en Firestore, si no, crearlo
                    verificarYCrearUsuarioEnFirestore(user);
                    
                    irAMainActivity();
                }
                
                @Override
                public void onError(Exception exception) {
                    mostrarProgreso(false);
                    String mensaje = "Error al iniciar sesión con Google";
                    if (exception != null && exception.getMessage() != null) {
                        if (exception.getMessage().contains("12500")) {
                            mensaje = "No se pudo iniciar sesión. Verifica que Google Play Services esté actualizado.";
                        } else if (exception.getMessage().contains("10")) {
                            mensaje = "Desarrollador error. Verifica la configuración de Google Sign-In.";
                        } else {
                            mensaje = "Error: " + exception.getMessage();
                        }
                    }
                    Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error en Google Sign-In", exception);
                }
            });
        }
    }
    
    /**
     * Verifica si el usuario existe en Firestore y lo crea si no existe
     */
    private void verificarYCrearUsuarioEnFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            return;
        }
        
        com.controlmedicamentos.myapplication.services.FirebaseService firebaseService = 
            new com.controlmedicamentos.myapplication.services.FirebaseService();
        
        firebaseService.obtenerUsuarioActual(new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Usuario ya existe, no hacer nada
                Log.d(TAG, "Usuario ya existe en Firestore");
            }
            
            @Override
            public void onError(Exception exception) {
                // Usuario no existe, crearlo
                Log.d(TAG, "Usuario no existe en Firestore, creándolo...");
                com.controlmedicamentos.myapplication.models.Usuario usuario = 
                    new com.controlmedicamentos.myapplication.models.Usuario();
                usuario.setNombre(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Usuario Google");
                usuario.setEmail(firebaseUser.getEmail());
                
                firebaseService.guardarUsuario(usuario, new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "Usuario de Google creado en Firestore exitosamente");
                    }
                    
                    @Override
                    public void onError(Exception exception) {
                        Log.e(TAG, "Error al crear usuario en Firestore", exception);
                    }
                });
            }
        });
    }

    private boolean validarCampos(String email, String password) {
        boolean valido = true;

        // Validar email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu email");
            valido = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un email válido");
            valido = false;
        } else {
            etEmail.setError(null);
        }

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            valido = false;
        } else if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            valido = false;
        } else {
            etPassword.setError(null);
        }

        return valido;
    }

    /**
     * Obtiene un mensaje de error amigable para errores de login
     */
    private String obtenerMensajeErrorLogin(Exception exception) {
        if (exception == null) {
            return "Error al iniciar sesión. Intenta nuevamente.";
        }

        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            String errorCode = authException.getErrorCode();
            
            Log.d(TAG, "Procesando error de Firebase. Código: " + errorCode);

            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "El formato del email es incorrecto";
                case "ERROR_WRONG_PASSWORD":
                    return "Contraseña incorrecta. Verifica que no tengas espacios al inicio o final.";
                case "ERROR_USER_NOT_FOUND":
                    return "No existe una cuenta con este email. Verifica el email o crea una cuenta.";
                case "ERROR_USER_DISABLED":
                    return "Esta cuenta ha sido deshabilitada. Contacta al soporte.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Demasiados intentos fallidos. Espera unos minutos e intenta nuevamente.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Esta operación no está permitida. Contacta al soporte.";
                case "ERROR_INVALID_CREDENTIAL":
                    return "Email o contraseña incorrectos. Verifica tus credenciales.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Error de conexión. Verifica tu internet.";
                case "ERROR_WEAK_PASSWORD":
                    return "La contraseña es muy débil.";
                default:
                    Log.w(TAG, "Código de error no reconocido: " + errorCode);
                    return "Error al iniciar sesión. Código: " + errorCode;
            }
        }

        // Si no es un FirebaseAuthException, buscar en el mensaje
        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            errorMessage = errorMessage.toLowerCase();
            if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
                return "Email o contraseña incorrectos";
            } else if (errorMessage.contains("user-not-found")) {
                return "No existe una cuenta con este email";
            } else if (errorMessage.contains("invalid-email")) {
                return "El formato del email es incorrecto";
            } else if (errorMessage.contains("network")) {
                return "Error de conexión. Verifica tu internet.";
            }
        }

        return "Error al iniciar sesión. Verifica tus credenciales.";
    }

    /**
     * Obtiene un mensaje de error amigable para errores de registro
     */
    private String obtenerMensajeErrorRegistro(Exception exception) {
        if (exception == null) {
            return "Error al registrar usuario. Intenta nuevamente.";
        }

        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            String errorCode = authException.getErrorCode();

            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "El formato del email es incorrecto";
                case "ERROR_WEAK_PASSWORD":
                    return "La contraseña es muy débil. Debe tener al menos 6 caracteres";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Este email ya está registrado. Inicia sesión en su lugar.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Esta operación no está permitida";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Error de conexión. Verifica tu internet.";
                default:
                    return "Error al registrar: " + errorCode;
            }
        }

        // Si no es un FirebaseAuthException, buscar en el mensaje
        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            errorMessage = errorMessage.toLowerCase();
            if (errorMessage.contains("email-already-in-use")) {
                return "Este email ya está registrado. Inicia sesión en su lugar.";
            } else if (errorMessage.contains("invalid-email")) {
                return "El formato del email es incorrecto";
            } else if (errorMessage.contains("weak-password")) {
                return "La contraseña es muy débil. Debe tener al menos 6 caracteres";
            } else if (errorMessage.contains("network")) {
                return "Error de conexión. Verifica tu internet.";
            }
        }

        return "Error al registrar usuario. Verifica los datos e intenta nuevamente.";
    }

    /**
     * Muestra un diálogo para recuperar la contraseña
     */
    private void mostrarDialogoRecuperarPassword() {
        // Crear un diálogo personalizado con un campo de texto
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_password_dialog_title);
        builder.setMessage(R.string.reset_password_dialog_message);

        // Crear un EditText para el email
        final TextInputLayout textInputLayout = new TextInputLayout(this);
        final TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
        editText.setHint(getString(R.string.login_email_hint));
        editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Pre-llenar con el email del campo de login si existe
        String emailActual = etEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(emailActual)) {
            editText.setText(emailActual);
        }
        
        textInputLayout.addView(editText);
        textInputLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            0
        );

        builder.setView(textInputLayout);

        builder.setPositiveButton("Enviar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String email = editText.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Por favor ingresa tu email", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(LoginActivity.this, "Ingresa un email válido", Toast.LENGTH_SHORT).show();
                } else {
                    enviarEmailRecuperacion(email);
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
     * Envía un email de recuperación de contraseña
     */
    private void enviarEmailRecuperacion(String email) {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar progreso (opcional, ya que es rápido)
        authService.resetPassword(email, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(LoginActivity.this, 
                    getString(R.string.reset_password_success), 
                    Toast.LENGTH_LONG).show();
                Log.d(TAG, "Email de recuperación enviado a: " + email);
            }

            @Override
            public void onError(Exception exception) {
                String mensaje = getString(R.string.reset_password_error);
                if (exception != null && exception.getMessage() != null) {
                    if (exception.getMessage().contains("user-not-found")) {
                        mensaje = "No existe una cuenta con este email";
                    } else if (exception.getMessage().contains("invalid-email")) {
                        mensaje = "El formato del email es incorrecto";
                    } else {
                        mensaje = exception.getMessage();
                    }
                }
                Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error al enviar email de recuperación", exception);
            }
        });
    }

    /**
     * Configura la barra de estado para que sea visible
     */
    private void configurarBarraEstado() {
        android.view.Window window = getWindow();
        
        // Asegurar que la barra de estado sea visible
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Limpiar TODOS los flags que puedan ocultar la barra de estado
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            
            // Habilitar dibujo de la barra de estado
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            
            // Establecer color de la barra de estado
            window.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
        
        // Configurar apariencia de la barra de estado
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // API 30+
            WindowInsetsControllerCompat controller = 
                WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(false);
                // Asegurar que la barra de estado sea visible
                controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                controller.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // API 23-29
            int flags = window.getDecorView().getSystemUiVisibility();
            // Limpiar TODOS los flags que oculten la barra de estado
            flags &= ~android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // API 21-22
            int flags = window.getDecorView().getSystemUiVisibility();
            flags &= ~android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

}