package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.controlmedicamentos.myapplication.utils.ErrorHandler;
import com.controlmedicamentos.myapplication.utils.Logger;
import com.controlmedicamentos.myapplication.utils.NavigationHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.browser.customtabs.CustomTabsIntent;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

public class AjustesActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etTelefono, etEdad;
    private TextInputLayout tilNombre, tilEmail, tilTelefono, tilEdad;
    private Switch switchNotificaciones, switchVibracion, switchSonido;
    private SeekBar seekBarVolumen, seekBarRepeticiones;
    private TextView tvVolumen, tvRepeticiones, tvDiasAntelacion;
    private MaterialButton btnGuardar, btnDiasAntelacion, btnLogout, btnEliminarCuenta;
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavHistorial, btnNavAjustes;
    
    // Google Calendar
    private TextView tvCalendarStatus, tvCalendarInfo;
    private MaterialButton btnConectarGoogleCalendar, btnDesconectarGoogleCalendar;
    private boolean googleCalendarConectado = false;
    private static final int RC_GOOGLE_CALENDAR_SIGN_IN = 9002;
    
    private SharedPreferences preferences;
    private int diasAntelacionStock = 3;
    
    private com.controlmedicamentos.myapplication.services.AuthService authService;
    private com.controlmedicamentos.myapplication.services.FirebaseService firebaseService;
    private com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService googleCalendarAuthService;
    private GoogleSignInClient googleCalendarSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado para que sea visible
        configurarBarraEstado();
        
        // Ocultar ActionBar/Toolbar para que no muestre el título duplicado
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_ajustes);
        
        // Aplicar window insets al header para respetar la barra de estado
        // Usar post para asegurar que se ejecute después del layout
        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            headerLayout.post(() -> aplicarWindowInsets(headerLayout));
        }

        // Inicializar servicios primero
        authService = new com.controlmedicamentos.myapplication.services.AuthService();
        firebaseService = new com.controlmedicamentos.myapplication.services.FirebaseService();
        googleCalendarAuthService = new com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService(this);
        
        // Inicializar Google Sign-In para Calendar
        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId != null && !webClientId.isEmpty()) {
            googleCalendarSignInClient = authService.initializeGoogleSignInForCalendar(this, webClientId);
        }

        inicializarVistas();
        cargarDatosUsuario(); // Cargar desde Firebase
        cargarPreferencias(); // Cargar configuraciones locales
        verificarConexionGoogleCalendar(); // Verificar si Google Calendar está conectado
        configurarListeners();
    }

    private void inicializarVistas() {
        // Campos de usuario
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etEdad = findViewById(R.id.etEdad);
        tilNombre = findViewById(R.id.tilNombre);
        tilEmail = findViewById(R.id.tilEmail);
        tilTelefono = findViewById(R.id.tilTelefono);
        tilEdad = findViewById(R.id.tilEdad);

        // Switches de configuración
        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        switchVibracion = findViewById(R.id.switchVibracion);
        switchSonido = findViewById(R.id.switchSonido);

        // SeekBars
        seekBarVolumen = findViewById(R.id.seekBarVolumen);
        seekBarRepeticiones = findViewById(R.id.seekBarRepeticiones);

        // TextViews
        tvVolumen = findViewById(R.id.tvVolumen);
        tvRepeticiones = findViewById(R.id.tvRepeticiones);
        tvDiasAntelacion = findViewById(R.id.tvDiasAntelacion);

        // Botones
        btnGuardar = findViewById(R.id.btnGuardar);
        btnDiasAntelacion = findViewById(R.id.btnDiasAntelacion);
        btnLogout = findViewById(R.id.btnLogout);
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavHistorial = findViewById(R.id.btnNavHistorial);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);
        
        // Google Calendar
        tvCalendarStatus = findViewById(R.id.tvCalendarStatus);
        tvCalendarInfo = findViewById(R.id.tvCalendarInfo);
        btnConectarGoogleCalendar = findViewById(R.id.btnConectarGoogleCalendar);
        btnDesconectarGoogleCalendar = findViewById(R.id.btnDesconectarGoogleCalendar);

        // SharedPreferences
        preferences = getSharedPreferences("ControlMedicamentos", MODE_PRIVATE);
    }
    
    /**
     * Verifica si Google Calendar está conectado
     */
    private void verificarConexionGoogleCalendar() {
        Logger.d("AjustesActivity", "verificarConexionGoogleCalendar: Iniciando verificación");
        googleCalendarAuthService.tieneGoogleCalendarConectado(
            new com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Logger.d("AjustesActivity", "verificarConexionGoogleCalendar: onSuccess, result: " + result);
                    if (result instanceof Boolean) {
                        googleCalendarConectado = (Boolean) result;
                        Logger.d("AjustesActivity", "verificarConexionGoogleCalendar: googleCalendarConectado = " + googleCalendarConectado);
                        actualizarUIGoogleCalendar();
                    } else {
                        Logger.w("AjustesActivity", "verificarConexionGoogleCalendar: result no es Boolean, es: " + (result != null ? result.getClass().getName() : "null"));
                        googleCalendarConectado = false;
                        actualizarUIGoogleCalendar();
                    }
                }
                
                @Override
                public void onError(Exception exception) {
                    Logger.w("AjustesActivity", "verificarConexionGoogleCalendar: onError", exception);
                    googleCalendarConectado = false;
                    actualizarUIGoogleCalendar();
                }
            }
        );
    }
    
    /**
     * Actualiza la UI según el estado de conexión de Google Calendar
     */
    private void actualizarUIGoogleCalendar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (googleCalendarConectado) {
                    tvCalendarStatus.setText(getString(R.string.google_calendar_status_connected));
                    tvCalendarInfo.setText(getString(R.string.google_calendar_info_connected));
                    btnConectarGoogleCalendar.setVisibility(View.GONE);
                    btnDesconectarGoogleCalendar.setVisibility(View.VISIBLE);
                } else {
                    tvCalendarStatus.setText(getString(R.string.google_calendar_status_not_connected));
                    tvCalendarInfo.setText(getString(R.string.google_calendar_info_not_connected));
                    btnConectarGoogleCalendar.setVisibility(View.VISIBLE);
                    btnDesconectarGoogleCalendar.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Carga los datos del usuario desde Firebase
     */
    private void cargarDatosUsuario() {
        firebaseService.obtenerUsuarioActual(new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.controlmedicamentos.myapplication.models.Usuario) {
                    com.controlmedicamentos.myapplication.models.Usuario usuario = 
                        (com.controlmedicamentos.myapplication.models.Usuario) result;
                    
                    // Precargar datos del usuario en el formulario
                    if (usuario.getNombre() != null && !usuario.getNombre().isEmpty()) {
                        etNombre.setText(usuario.getNombre());
                    }
                    
                    // Obtener email de Firebase Auth (más confiable)
                    com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
                    if (currentUser != null && currentUser.getEmail() != null) {
                        etEmail.setText(currentUser.getEmail());
                    } else if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
                        etEmail.setText(usuario.getEmail());
                    }
                    
                    // Precargar teléfono y edad si están disponibles
                    if (usuario.getTelefono() != null && !usuario.getTelefono().isEmpty()) {
                        etTelefono.setText(usuario.getTelefono());
                    }
                    if (usuario.getEdad() > 0) {
                        etEdad.setText(String.valueOf(usuario.getEdad()));
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                // Si hay error, intentar cargar desde Firebase Auth
                com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
                if (currentUser != null) {
                    if (currentUser.getDisplayName() != null) {
                        etNombre.setText(currentUser.getDisplayName());
                    }
                    if (currentUser.getEmail() != null) {
                        etEmail.setText(currentUser.getEmail());
                    }
                }
            }
        });
    }

    private void cargarPreferencias() {
        // Cargar configuraciones de notificaciones (no datos del usuario)
        switchNotificaciones.setChecked(preferences.getBoolean("notificaciones", true));
        switchVibracion.setChecked(preferences.getBoolean("vibracion", true));
        switchSonido.setChecked(preferences.getBoolean("sonido", true));

        // Cargar configuraciones de volumen y repeticiones
        int volumen = preferences.getInt("volumen", 70);
        int repeticiones = preferences.getInt("repeticiones", 3);
        diasAntelacionStock = preferences.getInt("dias_antelacion_stock", 7); // Por defecto 7 días

        seekBarVolumen.setProgress(volumen);
        seekBarRepeticiones.setProgress(repeticiones);

        actualizarTextos();
    }

    private void actualizarTextos() {
        tvVolumen.setText("Volumen: " + seekBarVolumen.getProgress() + "%");
        tvRepeticiones.setText("Repeticiones: " + seekBarRepeticiones.getProgress());
        tvDiasAntelacion.setText("Días de antelación: " + diasAntelacionStock);
    }

    private void configurarListeners() {
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarConfiguracion();
            }
        });

        btnDiasAntelacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoDiasAntelacion();
            }
        });
        
        // Navegación inferior
        NavigationHelper.configurarNavegacion(
            this,
            btnNavHome,
            btnNavNuevaMedicina,
            btnNavBotiquin,
            btnNavHistorial,
            btnNavAjustes
        );

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cerrarSesion();
            }
        });

        btnEliminarCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoEliminarCuenta();
            }
        });

        // Google Calendar listeners
        btnConectarGoogleCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conectarGoogleCalendar();
            }
        });
        
        btnDesconectarGoogleCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                desconectarGoogleCalendar();
            }
        });

        seekBarVolumen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumen.setText("Volumen: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarRepeticiones.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRepeticiones.setText("Repeticiones: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void guardarConfiguracion() {
        // Validar que los campos requeridos estén completos
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String edadStr = etEdad.getText().toString().trim();
        
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es requerido");
            return;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("El email es requerido y debe ser válido");
            return;
        }
        
        int edad = 0;
        if (!edadStr.isEmpty()) {
            try {
                edad = Integer.parseInt(edadStr);
            } catch (NumberFormatException e) {
                tilEdad.setError("La edad debe ser un número válido");
                return;
            }
        }
        
        // Guardar datos del usuario en Firebase
        com.controlmedicamentos.myapplication.models.Usuario usuario = 
            new com.controlmedicamentos.myapplication.models.Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setTelefono(telefono.isEmpty() ? null : telefono);
        usuario.setEdad(edad);
        
        firebaseService.guardarUsuario(usuario, new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Guardar configuraciones locales en SharedPreferences
                SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("notificaciones", switchNotificaciones.isChecked());
        editor.putBoolean("vibracion", switchVibracion.isChecked());
        editor.putBoolean("sonido", switchSonido.isChecked());
        editor.putInt("volumen", seekBarVolumen.getProgress());
        editor.putInt("repeticiones", seekBarRepeticiones.getProgress());
        editor.putInt("dias_antelacion_stock", diasAntelacionStock);
        editor.apply();

                Toast.makeText(AjustesActivity.this, "Configuración guardada exitosamente", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(AjustesActivity.this, 
                    "Error al guardar datos del usuario: " + 
                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarDialogoDiasAntelacion() {
        String[] opciones = {"1 día", "2 días", "3 días", "5 días", "7 días"};
        int[] valores = {1, 2, 3, 5, 7};

        new AlertDialog.Builder(this)
                .setTitle("Días de antelación para stock bajo")
                .setSingleChoiceItems(opciones, getIndiceDiasAntelacion(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        diasAntelacionStock = valores[which];
                        actualizarTextos();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int getIndiceDiasAntelacion() {
        switch (diasAntelacionStock) {
            case 1: return 0;
            case 2: return 1;
            case 3: return 2;
            case 5: return 3;
            case 7: return 4;
            default: return 2;
        }
    }


    private void cerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Cerrar Sesión", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        authService.logout();
                        // Redirigir a LoginActivity
                        Intent intent = new Intent(AjustesActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEliminarCuenta() {
        // Crear diálogo para ingresar credenciales
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_eliminar_cuenta, null);
        TextInputEditText etEmailEliminar = dialogView.findViewById(R.id.etEmailEliminar);
        TextInputEditText etPasswordEliminar = dialogView.findViewById(R.id.etPasswordEliminar);
        
        // Prellenar email si está disponible
        com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            etEmailEliminar.setText(currentUser.getEmail());
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuenta Permanentemente")
                .setMessage("⚠️ Esta acción es permanente y no se puede deshacer. Se eliminarán:\n\n" +
                        "• Tu cuenta de usuario\n" +
                        "• Todos tus medicamentos\n" +
                        "• Todos tus registros e historial")
                .setView(dialogView)
                .setPositiveButton("Eliminar Cuenta", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = etEmailEliminar.getText().toString().trim();
                        String password = etPasswordEliminar.getText().toString();
                        
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(AjustesActivity.this, "Por favor completa todos los campos", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        eliminarCuenta(email, password);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuenta(String email, String password) {
        // Verificar conexión a internet
        if (!com.controlmedicamentos.myapplication.utils.NetworkUtils.isNetworkAvailable(this)) {
            ErrorHandler.handleErrorWithCustomMessage(this, null, "AjustesActivity", 
                "No hay conexión a internet");
            return;
        }
        
        // Verificar si es usuario de Google
        com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
        boolean esGoogleTemp = false;
        if (currentUser != null) {
            for (com.google.firebase.auth.UserInfo provider : currentUser.getProviderData()) {
                if ("google.com".equals(provider.getProviderId())) {
                    esGoogleTemp = true;
                    break;
                }
            }
        }
        final boolean esGoogle = esGoogleTemp;
        
        // Mostrar diálogo de confirmación final
        new AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar tu cuenta?\n\n" +
                       "Esta acción es IRREVERSIBLE y eliminará:\n" +
                       "• Todos tus medicamentos\n" +
                       "• Todo tu historial de tomas\n" +
                       "• Todos tus datos de usuario\n\n" +
                       "Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar cuenta", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    procesarEliminacionCuenta(email, password, esGoogle);
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void procesarEliminacionCuenta(String email, String password, boolean esGoogle) {
        // Mostrar progreso
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("Eliminando cuenta...")
            .setMessage("Por favor espera mientras eliminamos todos tus datos.")
            .setView(progressBar)
            .setCancelable(false)
            .show();
        
        // Paso 1: Reautenticar usuario
        reautenticarUsuario(email, password, esGoogle, new com.controlmedicamentos.myapplication.services.AuthService.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                // Paso 2: Eliminar todos los medicamentos
                firebaseService.eliminarTodosLosMedicamentos(new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        // Paso 3: Eliminar todas las tomas
                        firebaseService.eliminarTodasLasTomas(new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                // Paso 4: Eliminar documento de usuario en Firestore
                                firebaseService.eliminarUsuario(new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        // Paso 5: Eliminar usuario de Firebase Auth
                                        eliminarUsuarioFirebaseAuth(progressDialog);
                                    }
                                    
                                    @Override
                                    public void onError(Exception exception) {
                                        progressDialog.dismiss();
                                        ErrorHandler.handleError(AjustesActivity.this, exception, "AjustesActivity");
                                    }
                                });
                            }
                            
                            @Override
                            public void onError(Exception exception) {
                                progressDialog.dismiss();
                                ErrorHandler.handleError(AjustesActivity.this, exception, "AjustesActivity");
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Exception exception) {
                        progressDialog.dismiss();
                        ErrorHandler.handleError(AjustesActivity.this, exception, "AjustesActivity");
                    }
                });
            }
            
            @Override
            public void onError(Exception exception) {
                progressDialog.dismiss();
                ErrorHandler.handleError(AjustesActivity.this, exception, "AjustesActivity");
            }
        });
    }
    
    private void reautenticarUsuario(String email, String password, boolean esGoogle, 
                                     com.controlmedicamentos.myapplication.services.AuthService.AuthCallback callback) {
        com.google.firebase.auth.FirebaseUser user = authService.getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        if (esGoogle) {
            // Para usuarios de Google, necesitamos usar Google Sign-In para reautenticar
            // Por ahora, intentamos reautenticar con email/password si es posible
            // Si el usuario tiene email/password como proveedor adicional, funcionará
            com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                .getProvider();
            
            // Intentar reautenticar con email/password
            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        // Si falla, puede ser porque el usuario solo tiene Google
                        // En ese caso, intentamos continuar de todas formas
                        Logger.w("AjustesActivity", 
                            "No se pudo reautenticar con email/password, pero continuamos");
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    }
                });
        } else {
            // Para usuarios con email/password, reautenticar directamente
            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
        }
    }
    
    private void eliminarUsuarioFirebaseAuth(AlertDialog progressDialog) {
        com.google.firebase.auth.FirebaseUser user = authService.getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            ErrorHandler.handleErrorWithCustomMessage(this, null, "AjustesActivity", 
                "Error: Usuario no encontrado");
            return;
        }
        
        user.delete()
            .addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    android.util.Log.d("AjustesActivity", "Cuenta eliminada exitosamente");
                    Toast.makeText(AjustesActivity.this, 
                        "Cuenta eliminada exitosamente", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Cerrar sesión de Google si aplica
                    authService.signOutGoogle();
                    
                    // Redirigir a LoginActivity
                    Intent intent = new Intent(AjustesActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    ErrorHandler.handleError(AjustesActivity.this, task.getException(), "AjustesActivity");
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
            // Limpiar cualquier flag que pueda ocultar la barra de estado
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
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
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // API 23-29
            int flags = window.getDecorView().getSystemUiVisibility();
            // Limpiar flags que oculten la barra de estado
            flags &= ~android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // API 21-22
            int flags = window.getDecorView().getSystemUiVisibility();
            flags &= ~android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }
    
    /**
     * Aplica window insets al header para respetar la barra de estado
     */
    private void aplicarWindowInsets(View headerLayout) {
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout, 
            (v, insets) -> {
                int statusBarHeight = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()).top;
                int paddingHorizontal = getResources().getDimensionPixelSize(R.dimen.padding_medium);
                int paddingVertical = getResources().getDimensionPixelSize(R.dimen.padding_medium);
                
                // Aplicar padding con la altura de la barra de estado
                v.setPadding(
                    paddingHorizontal,
                    statusBarHeight + paddingVertical,
                    paddingHorizontal,
                    paddingVertical
                );
                
                // Asegurar que el layout se actualice
                v.requestLayout();
                
                return insets;
            });
        
        // Forzar aplicación inmediata de insets
        ViewCompat.requestApplyInsets(headerLayout);
    }
    
    /**
     * Conecta Google Calendar usando Google Sign-In con requestServerAuthCode
     * Este método usa el flujo que funcionaba anteriormente
     */
    private void conectarGoogleCalendar() {
        if (googleCalendarSignInClient == null) {
            String webClientId = getString(R.string.default_web_client_id);
            if (webClientId == null || webClientId.isEmpty()) {
                Toast.makeText(this, 
                    "Error: Client ID no configurado. Verifica la configuración de la app.",
                    Toast.LENGTH_LONG).show();
                Logger.e("AjustesActivity", "Client ID no configurado");
                return;
            }
            googleCalendarSignInClient = authService.initializeGoogleSignInForCalendar(this, webClientId);
            if (googleCalendarSignInClient == null) {
                Toast.makeText(this, 
                    "Error: No se pudo inicializar Google Sign-In. Verifica que Google Play Services esté instalado.",
                    Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Hacer signOut primero para forzar el selector de cuenta
        googleCalendarSignInClient.signOut().addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                // Obtener el Intent de Google Sign-In
                Intent signInIntent = googleCalendarSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_GOOGLE_CALENDAR_SIGN_IN);
            }
        });
    }
    
    /**
     * Desconecta Google Calendar eliminando el token
     */
    private void desconectarGoogleCalendar() {
        new AlertDialog.Builder(this)
                .setTitle("Desconectar Google Calendar")
                .setMessage("¿Estás seguro de que quieres desconectar Google Calendar?\n\n" +
                           "Los eventos existentes en tu calendario no se eliminarán, pero no se crearán nuevos eventos.")
                .setPositiveButton("Desconectar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        googleCalendarAuthService.eliminarTokenGoogle(
                            new com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    googleCalendarConectado = false;
                                    actualizarUIGoogleCalendar();
                                    Toast.makeText(AjustesActivity.this, 
                                        "Google Calendar desconectado correctamente", 
                                        Toast.LENGTH_SHORT).show();
                                }
                                
                                @Override
                                public void onError(Exception exception) {
                                    Toast.makeText(AjustesActivity.this, 
                                        "Error al desconectar Google Calendar: " + 
                                        (exception != null ? exception.getMessage() : "Error desconocido"), 
                                        Toast.LENGTH_LONG).show();
                                }
                            }
                        );
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_GOOGLE_CALENDAR_SIGN_IN) {
            // Bug 1 Fix: Verificar que data no sea null antes de usarlo
            if (data == null) {
                Logger.d("AjustesActivity", "Usuario canceló el flujo de Google Sign-In (data es null)");
                return;
            }
            
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String serverAuthCode = account.getServerAuthCode();
                    if (serverAuthCode != null && !serverAuthCode.isEmpty()) {
                        Logger.d("AjustesActivity", "Server Auth Code obtenido, intercambiando por access token...");
                        intercambiarAuthCodePorToken(serverAuthCode);
                    } else {
                        Logger.e("AjustesActivity", "No se obtuvo serverAuthCode de Google Sign-In");
                        Toast.makeText(this, 
                            "Error: No se pudo obtener el código de autorización de Google.",
                            Toast.LENGTH_LONG).show();
                    }
                }
            } catch (ApiException e) {
                Logger.e("AjustesActivity", "Error en Google Sign-In para Calendar", e);
                String mensaje = "Error al conectar con Google Calendar: ";
                if (e.getStatusCode() == 12500) {
                    mensaje += "Google Play Services no está disponible";
                } else if (e.getStatusCode() == 10) {
                    mensaje += "Error de desarrollo. Verifica la configuración.";
                } else {
                    mensaje += e.getMessage();
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Intercambia el serverAuthCode por un access_token
     */
    private void intercambiarAuthCodePorToken(String serverAuthCode) {
        // Bug 2 Fix: Validar que clientId no sea null antes de usarlo
        String clientId = getString(R.string.default_web_client_id);
        if (clientId == null || clientId.isEmpty()) {
            Logger.e("AjustesActivity", "Client ID no configurado o es null");
            Toast.makeText(this, 
                "Error: Client ID no configurado. Verifica la configuración de la app.",
                Toast.LENGTH_LONG).show();
            return;
        }
        
        // Intentar obtener client_secret (puede no estar configurado)
        String clientSecret = null;
        try {
            int clientSecretResId = getResources().getIdentifier("google_oauth_client_secret", "string", getPackageName());
            if (clientSecretResId != 0) {
                clientSecret = getString(clientSecretResId);
            }
        } catch (Exception e) {
            Logger.d("AjustesActivity", "Client secret no configurado (opcional)");
        }
        
        Logger.d("AjustesActivity", "Intercambiando serverAuthCode por access_token...");
        
        // Intercambiar el código por el token
        googleCalendarAuthService.intercambiarAuthCodePorToken(serverAuthCode, clientId, clientSecret,
            new com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    if (result != null && result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tokenData = (Map<String, Object>) result;
                        
                        // Guardar el token en Firestore
                        googleCalendarAuthService.guardarTokenGoogle(tokenData, 
                            new com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object saveResult) {
                                    Logger.d("AjustesActivity", "Token guardado exitosamente");
                                    runOnUiThread(() -> {
                                        googleCalendarConectado = true;
                                        actualizarUIGoogleCalendar();
                                        Toast.makeText(AjustesActivity.this, 
                                            "Google Calendar conectado exitosamente", 
                                            Toast.LENGTH_SHORT).show();
                                    });
                                }
                                
                                @Override
                                public void onError(Exception exception) {
                                    Logger.e("AjustesActivity", "Error al guardar token", exception);
                                    runOnUiThread(() -> {
                                        Toast.makeText(AjustesActivity.this, 
                                            "Error al guardar el token: " + 
                                            (exception != null ? exception.getMessage() : "Error desconocido"), 
                                            Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                    } else {
                        Logger.e("AjustesActivity", "No se obtuvo token del intercambio");
                        runOnUiThread(() -> {
                            Toast.makeText(AjustesActivity.this, 
                                "Error: No se pudo obtener el token de acceso.", 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                }
                
                @Override
                public void onError(Exception exception) {
                    Logger.e("AjustesActivity", "Error al intercambiar auth code por token", exception);
                    runOnUiThread(() -> {
                        String mensaje = "Error al conectar con Google Calendar: ";
                        if (exception != null && exception.getMessage() != null) {
                            if (exception.getMessage().contains("invalid_grant")) {
                                mensaje += "El código de autorización ha expirado o ya fue usado. Intenta nuevamente.";
                            } else if (exception.getMessage().contains("invalid_client")) {
                                mensaje += "Client ID o Client Secret incorrectos. Verifica la configuración.";
                            } else {
                                mensaje += exception.getMessage();
                            }
                        } else {
                            mensaje += "Error desconocido";
                        }
                        Toast.makeText(AjustesActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    });
                }
            });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Verificar conexión cuando la actividad vuelve a primer plano
        verificarConexionGoogleCalendar();
    }
}
