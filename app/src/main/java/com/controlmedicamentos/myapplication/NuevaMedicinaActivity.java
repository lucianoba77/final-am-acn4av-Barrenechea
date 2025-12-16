package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarService;
import com.controlmedicamentos.myapplication.utils.Constants;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.controlmedicamentos.myapplication.utils.ColorUtils;
import com.controlmedicamentos.myapplication.utils.AlarmScheduler;
import com.controlmedicamentos.myapplication.utils.Logger;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class NuevaMedicinaActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etAfeccion, etDetalles;
    private TextInputLayout tilNombre, tilAfeccion;
    private MaterialButton btnGuardar, btnSeleccionarColor, btnFechaVencimiento, btnCancelarAccion;
    private MaterialButton btnSeleccionarHora;
    // Botones de navegación
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavHistorial, btnNavAjustes;
    private android.widget.Spinner spinnerPresentacion;
    private TextInputEditText etTomasDiarias, etStockInicial, etDiasTratamiento;
    private TextInputLayout tilTomasDiarias, tilStockInicial, tilDiasTratamiento;

    private String colorSeleccionadoHex = "#FFB6C1"; // Color por defecto (rosa pastel, índice 0)
    private Calendar fechaVencimiento = null;
    private String horaSeleccionada = "08:00";
    private AuthService authService;
    private FirebaseService firebaseService;
    private GoogleCalendarAuthService googleCalendarAuthService;
    private GoogleCalendarService googleCalendarService;
    private Medicamento medicamentoEditar = null; // Medicamento que se está editando (null si es creación)
    private boolean esEdicion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado para que sea visible
        configurarBarraEstado();
        
        // Ocultar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_nueva_medicina);
        
        // Aplicar window insets al header para respetar la barra de estado
        // Usar post para asegurar que se ejecute después del layout
        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            headerLayout.post(() -> aplicarWindowInsets(headerLayout));
        }

        // Inicializar servicios
        authService = new AuthService();
        firebaseService = new FirebaseService();
        googleCalendarAuthService = new GoogleCalendarAuthService(this);
        googleCalendarService = new GoogleCalendarService();

        // Verificar autenticación
        if (!authService.isUserLoggedIn()) {
            finish(); // Cerrar si no hay usuario autenticado
            return;
        }

        inicializarVistas();
        configurarSpinner();
        configurarListeners();
        configurarNavegacion();
        
        // Verificar si se está editando un medicamento
        String medicamentoId = getIntent().getStringExtra("medicamento_id");
        if (medicamentoId != null && !medicamentoId.isEmpty()) {
            esEdicion = true;
            cargarMedicamentoParaEditar(medicamentoId);
        } else {
            cargarCantidadMedicamentosParaColor();
        }
    }

    private void inicializarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etAfeccion = findViewById(R.id.etAfeccion);
        etDetalles = findViewById(R.id.etDetalles);
        tilNombre = findViewById(R.id.tilNombre);
        tilAfeccion = findViewById(R.id.tilAfeccion);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelarAccion = findViewById(R.id.btnCancelarAccion);
        btnSeleccionarColor = findViewById(R.id.btnSeleccionarColor);
        btnFechaVencimiento = findViewById(R.id.btnFechaVencimiento);
        btnSeleccionarHora = findViewById(R.id.btnSeleccionarHora);

        spinnerPresentacion = findViewById(R.id.spinnerPresentacion);
        etTomasDiarias = findViewById(R.id.etTomasDiarias);
        etStockInicial = findViewById(R.id.etStockInicial);
        etDiasTratamiento = findViewById(R.id.etDiasTratamiento);

        tilTomasDiarias = findViewById(R.id.tilTomasDiarias);
        tilStockInicial = findViewById(R.id.tilStockInicial);
        tilDiasTratamiento = findViewById(R.id.tilDiasTratamiento);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavHistorial = findViewById(R.id.btnNavHistorial);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);
    }

    private void configurarListeners() {
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarMedicamento();
            }
        });

        btnCancelarAccion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSeleccionarColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorColor();
            }
        });

        btnSeleccionarHora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorHora();
            }
        });

        btnFechaVencimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorFecha();
            }
        });
    }
    
    private void configurarNavegacion() {
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                Intent intent = new Intent(NuevaMedicinaActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavNuevaMedicina != null) {
            btnNavNuevaMedicina.setOnClickListener(v -> {
                // Ya estamos en nueva medicina
            });
        }
        
        if (btnNavBotiquin != null) {
            btnNavBotiquin.setOnClickListener(v -> {
                Intent intent = new Intent(NuevaMedicinaActivity.this, BotiquinActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavHistorial != null) {
            btnNavHistorial.setOnClickListener(v -> {
                Intent intent = new Intent(NuevaMedicinaActivity.this, HistorialActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavAjustes != null) {
            btnNavAjustes.setOnClickListener(v -> {
                Intent intent = new Intent(NuevaMedicinaActivity.this, AjustesActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
    
    private void configurarSpinner() {
        String[] presentaciones = {
                "Comprimidos", "Cápsulas", "Jarabe", "Crema",
                "Pomada", "Spray nasal", "Inyección", "Gotas", "Parche"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, presentaciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresentacion.setAdapter(adapter);

        // Listener para cambiar el hint según la presentación
        spinnerPresentacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String presentacion = presentaciones[position];
                actualizarHintStock(presentacion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void actualizarHintStock(String presentacion) {
        switch (presentacion) {
            case "Comprimidos":
            case "Cápsulas":
                tilStockInicial.setHint("Cantidad de comprimidos");
                etStockInicial.setHint(String.valueOf(Constants.STOCK_DEFAULT_COMPRIMIDOS));
                break;
            case "Jarabe":
            case "Inyección":
                tilStockInicial.setHint("Días estimados de duración");
                etStockInicial.setHint(String.valueOf(Constants.DIAS_DEFAULT_JARABE));
                break;
            case "Crema":
            case "Pomada":
            case "Parche":
                tilStockInicial.setHint("Días estimados de duración");
                etStockInicial.setHint(String.valueOf(Constants.DIAS_DEFAULT_CREMA));
                break;
            case "Spray nasal":
            case "Gotas":
                tilStockInicial.setHint("Días estimados de duración");
                etStockInicial.setHint(String.valueOf(Constants.DIAS_DEFAULT_SPRAY));
                break;
        }
    }
    private void guardarMedicamento() {
        if (validarFormulario()) {
            // Verificar conexión a internet
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
                return;
            }

            Medicamento medicamento = crearMedicamento();

            if (esEdicion && medicamentoEditar != null) {
                // Actualizar medicamento existente
                medicamento.setId(medicamentoEditar.getId());
                
                // En edición, si el usuario cambió el stock (campo Stock Inicial),
                // mantener el stockActual como está si no se cambió explícitamente
                // Pero si se cambió stockInicial, actualizar stockActual al nuevo valor
                // (lógica de React: si se actualiza stockInicial y stockActual no está definido, usar stockInicial)
                int nuevoStockInicial = medicamento.getStockInicial();
                int stockInicialAnterior = medicamentoEditar.getStockInicial();
                
                if (nuevoStockInicial != stockInicialAnterior) {
                    // Si se cambió stockInicial, y el stockActual es el mismo que antes,
                    // significa que no se actualizó explícitamente, mantener el stockActual actual
                    if (medicamento.getStockActual() == medicamentoEditar.getStockActual()) {
                        // Mantener el stockActual como está (no actualizar al nuevo stockInicial)
                        medicamento.setStockActual(medicamentoEditar.getStockActual());
                    }
                    // Si stockActual cambió, usar el nuevo valor
                } else {
                    // Si no se cambió stockInicial, mantener stockActual igual
                    medicamento.setStockActual(medicamentoEditar.getStockActual());
                }
                
                // Pasar contexto para gestionar Google Calendar si se pausa/reactiva
                firebaseService.actualizarMedicamento(medicamento, NuevaMedicinaActivity.this, new FirebaseService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        // Programar alarmas para el medicamento actualizado
                        if (result instanceof Medicamento) {
                            Medicamento medicamentoActualizado = (Medicamento) result;
                            AlarmScheduler alarmScheduler = new AlarmScheduler(NuevaMedicinaActivity.this);
                            alarmScheduler.programarAlarmasMedicamento(medicamentoActualizado);
                        }
                        Toast.makeText(NuevaMedicinaActivity.this, "Medicamento actualizado exitosamente", Toast.LENGTH_SHORT).show();
                        finish(); // Cerrar actividad
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(NuevaMedicinaActivity.this, 
                            "Error al actualizar medicamento: " + 
                            (exception != null ? exception.getMessage() : "Error desconocido"), 
                            Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Crear nuevo medicamento
                firebaseService.guardarMedicamento(medicamento, new FirebaseService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        // Programar alarmas para el nuevo medicamento
                        if (result instanceof Medicamento) {
                            Medicamento medicamentoGuardado = (Medicamento) result;
                            AlarmScheduler alarmScheduler = new AlarmScheduler(NuevaMedicinaActivity.this);
                            alarmScheduler.programarAlarmasMedicamento(medicamentoGuardado);
                            
                            // Sincronizar con Google Calendar si está conectado
                            sincronizarConGoogleCalendar(medicamentoGuardado);
                        }
                        Toast.makeText(NuevaMedicinaActivity.this, "Medicamento guardado exitosamente", Toast.LENGTH_SHORT).show();
                        finish(); // Cerrar actividad
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(NuevaMedicinaActivity.this, 
                            "Error al guardar medicamento: " + 
                            (exception != null ? exception.getMessage() : "Error desconocido"), 
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }
    
    /**
     * Carga un medicamento para editarlo
     */
    private void cargarMedicamentoParaEditar(String medicamentoId) {
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    medicamentoEditar = (Medicamento) result;
                    llenarFormularioConMedicamento(medicamentoEditar);
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(NuevaMedicinaActivity.this, 
                    "Error al cargar medicamento: " + 
                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
                finish(); // Cerrar si no se puede cargar
            }
        });
    }
    
    /**
     * Llena el formulario con los datos del medicamento a editar
     */
    private void llenarFormularioConMedicamento(Medicamento medicamento) {
        // Actualizar título de la actividad
        setTitle("Editar Medicamento");
        
        // Actualizar texto del botón
        btnGuardar.setText("Guardar Cambios");
        
        etNombre.setText(medicamento.getNombre());
        etAfeccion.setText(medicamento.getAfeccion());
        etDetalles.setText(medicamento.getDetalles());
        etTomasDiarias.setText(String.valueOf(medicamento.getTomasDiarias()));
        etStockInicial.setText(String.valueOf(medicamento.getStockActual())); // Mostrar stock actual, no inicial
        etDiasTratamiento.setText(medicamento.getDiasTratamiento() == -1 ? "" : String.valueOf(medicamento.getDiasTratamiento()));
        
        // Seleccionar presentación en el spinner
        String presentacion = medicamento.getPresentacion();
        String[] presentaciones = {
            "Comprimidos", "Cápsulas", "Jarabe", "Crema",
            "Pomada", "Spray nasal", "Inyección", "Gotas", "Parche"
        };
        for (int i = 0; i < presentaciones.length; i++) {
            if (presentaciones[i].equalsIgnoreCase(presentacion)) {
                spinnerPresentacion.setSelection(i);
                break;
            }
        }
        
        // Configurar horario (si tiene tomas diarias > 0)
        if (medicamento.getTomasDiarias() > 0 && medicamento.getHorarioPrimeraToma() != null 
            && !medicamento.getHorarioPrimeraToma().isEmpty() 
            && !medicamento.getHorarioPrimeraToma().equals(Constants.HORARIO_INVALIDO)) {
            horaSeleccionada = medicamento.getHorarioPrimeraToma();
            btnSeleccionarHora.setText(horaSeleccionada);
        } else if (medicamento.getTomasDiarias() == 0) {
            // Si es medicamento ocasional, deshabilitar selector de hora
            btnSeleccionarHora.setText("No aplica (medicamento ocasional)");
            btnSeleccionarHora.setEnabled(false);
        }
        
        // Configurar color
        colorSeleccionadoHex = ColorUtils.intToHex(medicamento.getColor());
        actualizarBotonColor();
        
        // Configurar fecha de vencimiento
        if (medicamento.getFechaVencimiento() != null) {
            fechaVencimiento = Calendar.getInstance();
            fechaVencimiento.setTime(medicamento.getFechaVencimiento());
            actualizarBotonFechaVencimiento();
        }
    }
    
    /**
     * Actualiza el botón de fecha de vencimiento con la fecha seleccionada
     */
    private void actualizarBotonFechaVencimiento() {
        if (fechaVencimiento != null) {
            java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            btnFechaVencimiento.setText(formato.format(fechaVencimiento.getTime()));
        }
    }

    private boolean validarFormulario() {
        boolean valido = true;

        if (TextUtils.isEmpty(etNombre.getText())) {
            tilNombre.setError("El nombre es requerido");
            valido = false;
        } else {
            tilNombre.setError(null);
        }

        if (TextUtils.isEmpty(etAfeccion.getText())) {
            tilAfeccion.setError("La afección es requerida");
            valido = false;
        } else {
            tilAfeccion.setError(null);
        }

        if (TextUtils.isEmpty(etTomasDiarias.getText())) {
            tilTomasDiarias.setError("Las tomas diarias son requeridas");
            valido = false;
        } else {
            tilTomasDiarias.setError(null);
        }

        // Validar horario solo si tomas diarias > 0
        int tomasDiarias = 0;
        if (!TextUtils.isEmpty(etTomasDiarias.getText())) {
            try {
                tomasDiarias = Integer.parseInt(etTomasDiarias.getText().toString());
            } catch (NumberFormatException e) {
                // Ya se validará arriba
            }
        }

        if (tomasDiarias > 0) {
            // Si tiene tomas diarias, requiere horario
            if (TextUtils.isEmpty(btnSeleccionarHora.getText()) || 
                btnSeleccionarHora.getText().toString().equals("Seleccionar hora")) {
                Toast.makeText(this, "Debe seleccionar una hora para medicamentos con tomas diarias", Toast.LENGTH_SHORT).show();
                valido = false;
            }
        }
        // Si tomas diarias = 0, no requiere horario (medicamento ocasional)

        if (TextUtils.isEmpty(etStockInicial.getText())) {
            tilStockInicial.setError("El stock inicial es requerido");
            valido = false;
        } else {
            tilStockInicial.setError(null);
            
            // Validar fecha de vencimiento si stock > 0
            try {
                int stockInicial = Integer.parseInt(etStockInicial.getText().toString());
                if (stockInicial > 0) {
                    // Si tiene stock, DEBE tener fecha de vencimiento
                    if (fechaVencimiento == null) {
                        Toast.makeText(this, "Los medicamentos con stock deben tener fecha de vencimiento", Toast.LENGTH_LONG).show();
                        valido = false;
                    }
                }
                // Si stock = 0, no requiere fecha de vencimiento (para recordar comprar)
            } catch (NumberFormatException e) {
                // Ya se validará arriba
            }
        }

        return valido;
    }

    private Medicamento crearMedicamento() {
        String nombre = etNombre.getText().toString();
        String afeccion = etAfeccion.getText().toString();
        String detalles = etDetalles.getText().toString();
        String presentacion = spinnerPresentacion.getSelectedItem().toString();
        int tomasDiarias = Integer.parseInt(etTomasDiarias.getText().toString());
        
        // Si tomas diarias = 0, usar string vacío "" (medicamento ocasional)
        // Esto es consistente con React: cuando tomasDiarias = 0, primeraToma = ""
        String horarioPrimeraToma = (tomasDiarias > 0) ? horaSeleccionada : "";
        
        int stockInicial = Integer.parseInt(etStockInicial.getText().toString());
        String diasTratamientoStr = etDiasTratamiento.getText().toString();
        int diasTratamiento = diasTratamientoStr.isEmpty() ? -1 : Integer.parseInt(diasTratamientoStr);

        // Si es edición, usar el ID del medicamento existente
        // Si es creación, el ID se generará automáticamente en Firebase
        String id = esEdicion && medicamentoEditar != null 
            ? medicamentoEditar.getId() 
            : "temp_" + System.currentTimeMillis();

        // Convertir color hexadecimal a int ARGB
        int colorInt = ColorUtils.hexToInt(colorSeleccionadoHex);
        
        Medicamento medicamento = new Medicamento(
                id, nombre, presentacion, tomasDiarias, horarioPrimeraToma,
                afeccion, stockInicial, colorInt, diasTratamiento
        );

        medicamento.setDetalles(detalles);

        // Fecha de vencimiento (ya validada en validarFormulario si stock > 0)
        if (fechaVencimiento != null) {
            medicamento.setFechaVencimiento(fechaVencimiento.getTime());
        }

        return medicamento;
    }

    /**
     * Carga la cantidad de medicamentos para asignar el color automáticamente
     * Consistente con React: obtenerColorPorIndice(medicamentos.length)
     */
    private void cargarCantidadMedicamentosParaColor() {
        firebaseService.obtenerMedicamentos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                int cantidadMedicamentos = result != null ? result.size() : 0;
                // Asignar color automáticamente según la cantidad de medicamentos
                colorSeleccionadoHex = ColorUtils.obtenerColorPorIndice(cantidadMedicamentos);
                // Actualizar el botón de color
                actualizarBotonColor();
            }

            @Override
            public void onError(Exception exception) {
                // En caso de error, usar el color por defecto (índice 0)
                colorSeleccionadoHex = ColorUtils.obtenerColorPorIndice(0);
                actualizarBotonColor();
            }
        });
    }
    
    /**
     * Actualiza el botón de color con el color seleccionado
     */
    private void actualizarBotonColor() {
        int colorInt = ColorUtils.hexToInt(colorSeleccionadoHex);
        btnSeleccionarColor.setBackgroundColor(colorInt);
        btnSeleccionarColor.setText("Color asignado");
    }

    private void mostrarSelectorColor() {
        // Los colores se asignan automáticamente, pero permitimos selección manual opcional
        String[] coloresNombres = {"Rosa pastel", "Azul pastel", "Verde pastel", "Amarillo pastel", "Lavanda pastel"};
        String[] coloresHex = ColorUtils.COLORES_HEX;

        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Color (Opcional)")
                .setItems(coloresNombres, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        colorSeleccionadoHex = coloresHex[which];
                        actualizarBotonColor();
                    }
                })
                .show();
    }

    private void mostrarSelectorHora() {
        String[] partes = horaSeleccionada.split(":");
        int hora = Integer.parseInt(partes[0]);
        int minuto = Integer.parseInt(partes[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        horaSeleccionada = String.format("%02d:%02d", hourOfDay, minute);
                        btnSeleccionarHora.setText(horaSeleccionada);
                    }
                }, hora, minuto, true);

        timePickerDialog.show();
    }

    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        fechaVencimiento = Calendar.getInstance();
                        fechaVencimiento.set(year, month, dayOfMonth);
                        btnFechaVencimiento.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
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
     * Sincroniza el medicamento con Google Calendar si está conectado
     */
    private void sincronizarConGoogleCalendar(Medicamento medicamento) {
        // Verificar si Google Calendar está conectado
        googleCalendarAuthService.tieneGoogleCalendarConectado(
            new GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    boolean conectado = result != null && (Boolean) result;
                    if (conectado) {
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
                                            // Crear eventos recurrentes en Google Calendar
                                            googleCalendarService.crearEventosRecurrentes(
                                                accessToken, 
                                                medicamento,
                                                new GoogleCalendarService.RecurrentEventsCallback() {
                                                    @Override
                                                    public void onSuccess(List<String> eventoIds) {
                                                        Logger.d("NuevaMedicinaActivity", 
                                                            "Eventos de Google Calendar creados: " + eventoIds.size());
                                                        
                                                        // Guardar los IDs de eventos en el medicamento
                                                        if (!eventoIds.isEmpty() && medicamento.getId() != null) {
                                                            guardarEventoIdsEnMedicamento(medicamento.getId(), eventoIds);
                                                        }
                                                    }
                                                    
                                                    @Override
                                                    public void onError(Exception exception) {
                                                        Logger.w("NuevaMedicinaActivity", 
                                                            "Error al crear eventos en Google Calendar", exception);
                                                        // No mostrar error al usuario, es opcional
                                                    }
                                                }
                                            );
                                        } else {
                                            Logger.w("NuevaMedicinaActivity", 
                                                "Token de Google Calendar no disponible");
                                        }
                                    }
                                }
                                
                                @Override
                                public void onError(Exception exception) {
                                    Logger.w("NuevaMedicinaActivity", 
                                        "Error al obtener token de Google Calendar", exception);
                                    // No mostrar error al usuario, es opcional
                                }
                            }
                        );
                    } else {
                        Logger.d("NuevaMedicinaActivity", 
                            "Google Calendar no está conectado, omitiendo sincronización");
                    }
                }
                
                @Override
                public void onError(Exception exception) {
                    Logger.w("NuevaMedicinaActivity", 
                        "Error al verificar conexión de Google Calendar", exception);
                    // No mostrar error al usuario, es opcional
                }
            }
        );
    }
    
    /**
     * Guarda los IDs de eventos de Google Calendar en el medicamento
     */
    private void guardarEventoIdsEnMedicamento(String medicamentoId, List<String> eventoIds) {
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    Medicamento medicamento = (Medicamento) result;
                    // Los eventoIds se guardan automáticamente en Firestore cuando se crean
                    // No necesitamos hacer nada adicional aquí
                    Logger.d("NuevaMedicinaActivity", 
                        "IDs de eventos guardados para medicamento: " + medicamentoId);
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Logger.w("NuevaMedicinaActivity", 
                    "Error al guardar IDs de eventos en medicamento", exception);
            }
        });
    }
}