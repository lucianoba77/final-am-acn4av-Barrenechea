package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarService;
import com.controlmedicamentos.myapplication.utils.Constants;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.controlmedicamentos.myapplication.utils.ColorUtils;
import com.controlmedicamentos.myapplication.utils.AlarmScheduler;
import com.controlmedicamentos.myapplication.utils.Logger;
import com.controlmedicamentos.myapplication.utils.NavigationHelper;
import com.controlmedicamentos.myapplication.utils.ErrorHandler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NuevaMedicinaActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etAfeccion, etDetalles;
    private TextInputLayout tilNombre, tilAfeccion;
    private MaterialButton btnGuardar, btnSeleccionarColor, btnFechaVencimiento, btnCancelarAccion;
    private MaterialButton btnSeleccionarHora;
    // Botones de navegación
    private View btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavHistorial, btnNavAjustes;
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

    private CheckBox checkUsarProgramacionPersonalizada;
    private MaterialSwitch switchEsCronico;
    private LinearLayout programacionDiasContainer;
    /** Horarios por día de la semana (0=Domingo .. 6=Sábado). Se rellena al marcar "programación personalizada". */
    private final List<List<String>> horariosPorDia = new ArrayList<>();
    /** Horarios adicionales del día (sin programación por día). La primera toma es horaSeleccionada; el resto va aquí. */
    private final List<String> horariosAdicionales = new ArrayList<>();
    private LinearLayout containerHorariosAdicionales;
    private MaterialButton btnAgregarToma;

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
        switchEsCronico = findViewById(R.id.switchEsCronico);

        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavHistorial = findViewById(R.id.btnNavHistorial);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);

        checkUsarProgramacionPersonalizada = findViewById(R.id.checkUsarProgramacionPersonalizada);
        programacionDiasContainer = findViewById(R.id.programacionDiasContainer);
        containerHorariosAdicionales = findViewById(R.id.containerHorariosAdicionales);
        btnAgregarToma = findViewById(R.id.btnAgregarToma);
        for (int i = 0; i < 7; i++) {
            horariosPorDia.add(new ArrayList<>());
        }
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

        // Color asignado automáticamente por índice; no se muestra selector

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

        checkUsarProgramacionPersonalizada.setOnCheckedChangeListener((buttonView, isChecked) -> {
            programacionDiasContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && programacionDiasContainer.getChildCount() == 0) {
                construirFilasProgramacion();
            }
        });

        switchEsCronico.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilDiasTratamiento.setEnabled(!isChecked);
            etDiasTratamiento.setEnabled(!isChecked);
            if (isChecked) {
                etDiasTratamiento.setText("");
            }
        });

        btnAgregarToma.setOnClickListener(v -> mostrarSelectorHoraAdicional());
    }

    /** Muestra el selector de hora para agregar una toma adicional (mismo día, todos los días o duración del tratamiento). */
    private void mostrarSelectorHoraAdicional() {
        TimePickerDialog dlg = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String h = String.format("%02d:%02d", hourOfDay, minute);
            if (horaSeleccionada != null && h.equals(horaSeleccionada)) {
                Toast.makeText(this, getString(R.string.msg_horario_ya_primera_toma), Toast.LENGTH_SHORT).show();
                return;
            }
            if (horariosAdicionales.contains(h)) {
                Toast.makeText(this, getString(R.string.msg_horario_ya_agregado), Toast.LENGTH_SHORT).show();
                return;
            }
            horariosAdicionales.add(h);
            Collections.sort(horariosAdicionales, String::compareTo);
            refrescarContainerHorariosAdicionales();
            actualizarTomasDiariasDesdeHorarios();
        }, 14, 0, true);
        dlg.show();
    }

    private void refrescarContainerHorariosAdicionales() {
        if (containerHorariosAdicionales == null) return;
        containerHorariosAdicionales.removeAllViews();
        for (String horario : horariosAdicionales) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_horario_adicional, containerHorariosAdicionales, false);
            TextView tvHorario = row.findViewById(R.id.tvHorarioAdicional);
            View btnQuitar = row.findViewById(R.id.btnQuitarHorario);
            if (tvHorario != null) tvHorario.setText(horario);
            String h = horario;
            if (btnQuitar != null) btnQuitar.setOnClickListener(v -> {
                horariosAdicionales.remove(h);
                refrescarContainerHorariosAdicionales();
                actualizarTomasDiariasDesdeHorarios();
            });
            containerHorariosAdicionales.addView(row);
        }
    }

    private void actualizarTomasDiariasDesdeHorarios() {
        int total = (horaSeleccionada != null && !horaSeleccionada.isEmpty() ? 1 : 0) + horariosAdicionales.size();
        if (etTomasDiarias != null && total > 0) {
            etTomasDiarias.setText(String.valueOf(total));
        }
    }

    private static final int[] IDS_NOMBRES_DIA = {
        R.string.day_domingo, R.string.day_lunes, R.string.day_martes, R.string.day_miercoles,
        R.string.day_jueves, R.string.day_viernes, R.string.day_sabado
    };

    private void construirFilasProgramacion() {
        programacionDiasContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int dia = 0; dia < 7; dia++) {
            View row = inflater.inflate(R.layout.item_programacion_dia, programacionDiasContainer, false);
            TextView tvDia = row.findViewById(R.id.tvDiaNombre);
            tvDia.setText(IDS_NOMBRES_DIA[dia]);
            LinearLayout containerHorarios = row.findViewById(R.id.containerHorariosDia);
            MaterialButton btnAgregar = row.findViewById(R.id.btnAgregarHorario);
            final int diaIndex = dia;
            btnAgregar.setOnClickListener(v -> mostrarTimePickerParaDia(diaIndex, containerHorarios));
            row.setTag(diaIndex);
            programacionDiasContainer.addView(row);
            refrescarHorariosDia(diaIndex, containerHorarios);
        }
    }

    private void mostrarTimePickerParaDia(int diaIndex, LinearLayout containerHorarios) {
        String inicial = "08:00";
        if (!horariosPorDia.get(diaIndex).isEmpty()) {
            inicial = horariosPorDia.get(diaIndex).get(horariosPorDia.get(diaIndex).size() - 1);
        }
        String[] partes = inicial.split(":");
        int hora = partes.length >= 2 ? Integer.parseInt(partes[0]) : 8;
        int minuto = partes.length >= 2 ? Integer.parseInt(partes[1]) : 0;
        TimePickerDialog dlg = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String h = String.format("%02d:%02d", hourOfDay, minute);
            if (horariosPorDia.get(diaIndex).contains(h)) {
                Toast.makeText(this, getString(R.string.programacion_horario_duplicado), Toast.LENGTH_SHORT).show();
                return;
            }
            horariosPorDia.get(diaIndex).add(h);
            Collections.sort(horariosPorDia.get(diaIndex), String::compareTo);
            refrescarHorariosDia(diaIndex, containerHorarios);
        }, hora, minuto, true);
        dlg.show();
    }

    private void refrescarHorariosDia(int diaIndex, LinearLayout containerHorarios) {
        containerHorarios.removeAllViews();
        List<String> list = horariosPorDia.get(diaIndex);
        for (String horario : list) {
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            TextView tv = new TextView(this);
            tv.setText(horario);
            tv.setTextSize(16);
            tv.setLayoutParams(paramsText);
            MaterialButton quitar = new MaterialButton(this);
            quitar.setText("✕");
            quitar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            String horarioToRemove = horario;
            quitar.setOnClickListener(v -> {
                list.remove(horarioToRemove);
                refrescarHorariosDia(diaIndex, containerHorarios);
            });
            fila.addView(tv);
            fila.addView(quitar);
            containerHorarios.addView(fila);
        }
    }
    
    private void configurarNavegacion() {
        NavigationHelper.configurarNavegacion(
            this,
            btnNavHome,
            btnNavNuevaMedicina,
            btnNavBotiquin,
            btnNavHistorial,
            btnNavAjustes
        );
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
                break;
            case "Jarabe":
            case "Inyección":
                tilStockInicial.setHint("Días estimados de duración");
                break;
            case "Crema":
            case "Pomada":
            case "Parche":
                tilStockInicial.setHint("Días estimados de duración");
                break;
            case "Spray nasal":
            case "Gotas":
                tilStockInicial.setHint("Días estimados de duración");
                break;
        }
    }
    private void guardarMedicamento() {
        if (validarFormulario()) {
            // Verificar conexión a internet
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, getString(R.string.msg_no_internet), Toast.LENGTH_LONG).show();
                return;
            }

            Medicamento medicamento;
            try {
                medicamento = crearMedicamento();
            } catch (Exception e) {
                Logger.e("NuevaMedicinaActivity", "Error al crear medicamento", e);
                Toast.makeText(this, getString(R.string.msg_error_creating_medicine,
                    e.getMessage() != null ? e.getMessage() : getString(R.string.error_unknown)),
                    Toast.LENGTH_LONG).show();
                return;
            }
            
            if (medicamento == null) {
                Logger.e("NuevaMedicinaActivity", "Medicamento es null después de crearMedicamento()");
                Toast.makeText(this, getString(R.string.msg_error_could_not_create_medicine), Toast.LENGTH_LONG).show();
                return;
            }

            if (esEdicion && medicamentoEditar != null) {
                // Actualizar medicamento existente
                medicamento.setId(medicamentoEditar.getId());
                
                // En edición, si el usuario cambió el stock (campo Stock Inicial),
                // mantener el stockActual como está si no se cambió explícitamente
                int nuevoStockInicial = medicamento.getStockInicial();
                int stockInicialAnterior = medicamentoEditar.getStockInicial();
                if (nuevoStockInicial != stockInicialAnterior) {
                    if (medicamento.getStockActual() == medicamentoEditar.getStockActual()) {
                        medicamento.setStockActual(medicamentoEditar.getStockActual());
                    }
                } else {
                    medicamento.setStockActual(medicamentoEditar.getStockActual());
                }

                // Si cambió la programación/horarios, comprobar si hoy ya tiene una toma registrada como tomada
                boolean horariosCambiaron = horariosCambiaronRespectoA(medicamento, medicamentoEditar);
                if (horariosCambiaron) {
                    firebaseService.obtenerTomasUsuario(new FirebaseService.FirestoreListCallback() {
                        @Override
                        public void onSuccess(List<?> tomasResult) {
                            boolean tieneTomaTomadaHoy = false;
                            if (tomasResult != null) {
                                Calendar hoy = Calendar.getInstance();
                                for (Object o : tomasResult) {
                                    if (!(o instanceof Toma)) continue;
                                    Toma t = (Toma) o;
                                    if (!medicamento.getId().equals(t.getMedicamentoId()) || t.getEstado() != Toma.EstadoToma.TOMADA) continue;
                                    Date f = t.getFechaHoraTomada() != null ? t.getFechaHoraTomada() : t.getFechaHoraProgramada();
                                    if (f == null) continue;
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(f);
                                    if (cal.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)) {
                                        tieneTomaTomadaHoy = true;
                                        break;
                                    }
                                }
                            }
                            if (tieneTomaTomadaHoy) {
                                Toast.makeText(NuevaMedicinaActivity.this, R.string.msg_horario_cambio_toma_ya_tomada, Toast.LENGTH_LONG).show();
                            }
                            ejecutarActualizarMedicamento(medicamento);
                        }
                        @Override
                        public void onError(Exception exception) {
                            ejecutarActualizarMedicamento(medicamento);
                        }
                    });
                } else {
                    ejecutarActualizarMedicamento(medicamento);
                }
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
                        Toast.makeText(NuevaMedicinaActivity.this, getString(R.string.msg_medicine_saved), Toast.LENGTH_SHORT).show();
                        irAMainActivity(); // Redirigir al dashboard
                    }

                    @Override
                    public void onError(Exception exception) {
                        ErrorHandler.handleError(NuevaMedicinaActivity.this, exception, "NuevaMedicinaActivity");
                    }
                });
            }
        }
    }
    
    /**
     * Indica si los horarios de toma cambiaron respecto al medicamento editado (orden o valores).
     */
    private boolean horariosCambiaronRespectoA(Medicamento nuevo, Medicamento anterior) {
        List<String> a = nuevo.getHorariosTomas() != null ? new ArrayList<>(nuevo.getHorariosTomas()) : new ArrayList<>();
        List<String> b = anterior.getHorariosTomas() != null ? new ArrayList<>(anterior.getHorariosTomas()) : new ArrayList<>();
        if (a.size() != b.size()) return true;
        Collections.sort(a, String::compareTo);
        Collections.sort(b, String::compareTo);
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) return true;
        }
        return false;
    }

    /**
     * Ejecuta la actualización del medicamento en Firestore (usado en edición).
     */
    private void ejecutarActualizarMedicamento(Medicamento medicamento) {
        firebaseService.actualizarMedicamento(medicamento, NuevaMedicinaActivity.this, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    Medicamento medicamentoActualizado = (Medicamento) result;
                    AlarmScheduler alarmScheduler = new AlarmScheduler(NuevaMedicinaActivity.this);
                    alarmScheduler.programarAlarmasMedicamento(medicamentoActualizado);
                }
                Toast.makeText(NuevaMedicinaActivity.this, getString(R.string.msg_medicine_updated), Toast.LENGTH_SHORT).show();
                irAMainActivity();
            }
            @Override
            public void onError(Exception exception) {
                ErrorHandler.handleError(NuevaMedicinaActivity.this, exception, "NuevaMedicinaActivity");
            }
        });
    }

    /**
     * Redirige al dashboard (MainActivity)
     */
    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Cerrar NuevaMedicinaActivity
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
                ErrorHandler.handleError(NuevaMedicinaActivity.this, exception, "NuevaMedicinaActivity");
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
        btnGuardar.setText(getString(R.string.btn_guardar_cambios));
        
        etNombre.setText(medicamento.getNombre());
        etAfeccion.setText(medicamento.getAfeccion());
        etDetalles.setText(medicamento.getDetalles());
        etTomasDiarias.setText(String.valueOf(medicamento.getTomasDiarias()));
        etStockInicial.setText(String.valueOf(medicamento.getStockActual())); // Mostrar stock actual, no inicial
        boolean esCronico = medicamento.getDiasTratamiento() == -1;
        switchEsCronico.setChecked(esCronico);
        tilDiasTratamiento.setEnabled(!esCronico);
        etDiasTratamiento.setEnabled(!esCronico);
        etDiasTratamiento.setText(esCronico ? "" : String.valueOf(medicamento.getDiasTratamiento()));

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
        
        // Programación personalizada por día
        if (medicamento.isUsarProgramacionPersonalizada() && medicamento.getProgramacionPersonalizada() != null) {
            for (int d = 0; d < 7; d++) {
                horariosPorDia.get(d).clear();
                List<String> list = medicamento.getProgramacionPersonalizada().get(d);
                if (list != null) horariosPorDia.get(d).addAll(list);
            }
            checkUsarProgramacionPersonalizada.setChecked(true);
            programacionDiasContainer.setVisibility(View.VISIBLE);
            horariosAdicionales.clear();
            refrescarContainerHorariosAdicionales();
            if (programacionDiasContainer.getChildCount() == 0) {
                construirFilasProgramacion();
            } else {
                for (int d = 0; d < 7; d++) {
                    View row = programacionDiasContainer.getChildAt(d);
                    refrescarHorariosDia(d, row.findViewById(R.id.containerHorariosDia));
                }
            }
        } else {
            // Sin programación por día: cargar primera toma + horarios adicionales
            horariosAdicionales.clear();
            if (medicamento.getTomasDiarias() > 0 && medicamento.getHorariosTomas() != null && !medicamento.getHorariosTomas().isEmpty()) {
                List<String> horarios = medicamento.getHorariosTomas();
                horaSeleccionada = horarios.get(0);
                btnSeleccionarHora.setText(horaSeleccionada);
                for (int i = 1; i < horarios.size(); i++) {
                    horariosAdicionales.add(horarios.get(i));
                }
                refrescarContainerHorariosAdicionales();
            } else if (medicamento.getTomasDiarias() == 0) {
                btnSeleccionarHora.setText(getString(R.string.btn_no_aplica_ocasional));
                btnSeleccionarHora.setEnabled(false);
            }
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

        // Validar tomas diarias (puede estar vacío para medicamentos ocasionales)
        int tomasDiarias = 0;
        if (!TextUtils.isEmpty(etTomasDiarias.getText())) {
            try {
                tomasDiarias = Integer.parseInt(etTomasDiarias.getText().toString());
                if (tomasDiarias < 0) {
                    tilTomasDiarias.setError("Las tomas diarias no pueden ser negativas");
                    valido = false;
                } else {
                    tilTomasDiarias.setError(null);
                }
            } catch (NumberFormatException e) {
                tilTomasDiarias.setError("Ingresa un número válido");
                valido = false;
            }
        } else {
            // Campo vacío = medicamento ocasional (tomasDiarias = 0)
            tilTomasDiarias.setError(null);
            tomasDiarias = 0;
        }

        // Validar horario solo si tomas diarias > 0

        if (tomasDiarias > 0) {
            // Si tiene tomas diarias, requiere horario
            if (TextUtils.isEmpty(btnSeleccionarHora.getText()) || 
                btnSeleccionarHora.getText().toString().equals("Seleccionar hora")) {
                Toast.makeText(this, getString(R.string.msg_select_time_daily_doses), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, getString(R.string.msg_stock_requires_expiration), Toast.LENGTH_LONG).show();
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
        String nombre = etNombre.getText() != null ? etNombre.getText().toString() : "";
        String afeccion = etAfeccion.getText() != null ? etAfeccion.getText().toString() : "";
        String detalles = etDetalles.getText() != null ? etDetalles.getText().toString() : "";
        
        // Obtener presentación del spinner (puede ser null)
        Object selectedItem = spinnerPresentacion.getSelectedItem();
        String presentacion = selectedItem != null ? selectedItem.toString() : "Comprimidos";
        
        // Construir lista de horarios: primera toma + adicionales (sin programación por día)
        List<String> listaHorarios = new ArrayList<>();
        if (horaSeleccionada != null && !horaSeleccionada.isEmpty()) {
            listaHorarios.add(horaSeleccionada);
        }
        if (horariosAdicionales != null) {
            listaHorarios.addAll(horariosAdicionales);
        }
        Collections.sort(listaHorarios, String::compareTo);

        int tomasDiarias = listaHorarios.isEmpty() ? 0 : listaHorarios.size();
        if (tomasDiarias == 0 && !TextUtils.isEmpty(etTomasDiarias.getText())) {
            try {
                tomasDiarias = Integer.parseInt(etTomasDiarias.getText().toString());
            } catch (NumberFormatException e) {
                tomasDiarias = 0;
            }
        }
        String horarioPrimeraToma = listaHorarios.isEmpty() ? "" : listaHorarios.get(0);
        
        // Obtener stock inicial (puede estar vacío, usar valor por defecto)
        int stockInicial = 0;
        if (!TextUtils.isEmpty(etStockInicial.getText())) {
            try {
                stockInicial = Integer.parseInt(etStockInicial.getText().toString());
                if (stockInicial < 0) {
                    stockInicial = 0; // No permitir valores negativos
                }
            } catch (NumberFormatException e) {
                stockInicial = 0; // Por defecto
            }
        }
        
        // Días de tratamiento: -1 si "Es crónico" está activo, sino el valor del campo
        int diasTratamiento;
        if (switchEsCronico != null && switchEsCronico.isChecked()) {
            diasTratamiento = -1; // Crónico
        } else {
            String diasTratamientoStr = etDiasTratamiento.getText() != null ? etDiasTratamiento.getText().toString().trim() : "";
            if (TextUtils.isEmpty(diasTratamientoStr)) {
                diasTratamiento = -1; // Por defecto, crónico
            } else {
                try {
                    diasTratamiento = Integer.parseInt(diasTratamientoStr);
                    if (diasTratamiento < 0) {
                        diasTratamiento = 0;
                    }
                } catch (NumberFormatException e) {
                    diasTratamiento = -1;
                }
            }
        }

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

        // Horarios del día (primera toma + adicionales): aplican todos los días o duración del tratamiento
        if (!listaHorarios.isEmpty()) {
            medicamento.setHorariosTomas(new ArrayList<>(listaHorarios));
        }

        // Fecha de vencimiento (ya validada en validarFormulario si stock > 0)
        if (fechaVencimiento != null) {
            medicamento.setFechaVencimiento(fechaVencimiento.getTime());
        }

        // Programación personalizada: si está activa y hay al menos un día con horarios
        if (checkUsarProgramacionPersonalizada != null && checkUsarProgramacionPersonalizada.isChecked()) {
            Map<Integer, List<String>> programacion = new HashMap<>();
            List<String> todosHorarios = new ArrayList<>();
            int maxTomasDia = 0;
            for (int d = 0; d < 7; d++) {
                List<String> list = horariosPorDia.get(d) != null ? new ArrayList<>(horariosPorDia.get(d)) : new ArrayList<>();
                if (!list.isEmpty()) {
                    Collections.sort(list, String::compareTo);
                    programacion.put(d, list);
                    todosHorarios.addAll(list);
                    if (list.size() > maxTomasDia) maxTomasDia = list.size();
                }
            }
            if (!programacion.isEmpty() && !todosHorarios.isEmpty()) {
                medicamento.setProgramacionPersonalizada(programacion);
                medicamento.setUsarProgramacionPersonalizada(true);
                Collections.sort(todosHorarios, String::compareTo);
                medicamento.setHorariosTomas(todosHorarios);
                medicamento.setHorarioPrimeraToma(todosHorarios.get(0));
                medicamento.setTomasDiarias(maxTomasDia);
            }
        } else {
            medicamento.setUsarProgramacionPersonalizada(false);
            medicamento.setProgramacionPersonalizada(null);
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
        if (btnSeleccionarColor == null) return;
        int colorInt = ColorUtils.hexToInt(colorSeleccionadoHex);
        btnSeleccionarColor.setBackgroundColor(colorInt);
        btnSeleccionarColor.setText(getString(R.string.btn_color_asignado));
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
     * Solo crea eventos para medicamentos con tomas diarias > 0
     */
    private void sincronizarConGoogleCalendar(Medicamento medicamento) {
        Logger.d("NuevaMedicinaActivity", "sincronizarConGoogleCalendar: Iniciando sincronización para medicamento: " + 
            (medicamento != null ? medicamento.getNombre() : "null"));
        
        // Solo crear eventos si el medicamento tiene tomas diarias programadas
        if (medicamento == null || medicamento.getTomasDiarias() <= 0) {
            Logger.d("NuevaMedicinaActivity", "sincronizarConGoogleCalendar: Medicamento ocasional o sin tomas diarias, no se crean eventos");
            return;
        }
        
        // Verificar si Google Calendar está conectado
        googleCalendarAuthService.tieneGoogleCalendarConectado(
            new GoogleCalendarAuthService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    boolean conectado = result != null && (Boolean) result;
                    Logger.d("NuevaMedicinaActivity", "sincronizarConGoogleCalendar: tieneGoogleCalendarConectado = " + conectado);
                    if (conectado) {
                        // Obtener token de acceso
                        googleCalendarAuthService.obtenerTokenGoogle(
                            new GoogleCalendarAuthService.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object tokenResult) {
                                    Logger.d("NuevaMedicinaActivity", 
                                        "obtenerTokenGoogle callback: tokenResult = " + 
                                        (tokenResult != null ? tokenResult.getClass().getSimpleName() : "null"));
                                    
                                    if (tokenResult != null && tokenResult instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> tokenData = (Map<String, Object>) tokenResult;
                                        String accessToken = (String) tokenData.get("access_token");
                                        
                                        Logger.d("NuevaMedicinaActivity", 
                                            "Access token obtenido: " + (accessToken != null && !accessToken.isEmpty() ? "SÍ" : "NO"));
                                        
                                        if (accessToken != null && !accessToken.isEmpty()) {
                                            Logger.d("NuevaMedicinaActivity", 
                                                "Token de Google Calendar obtenido, creando eventos para medicamento: " + 
                                                medicamento.getNombre() + " (Tomas diarias: " + medicamento.getTomasDiarias() + 
                                                ", Horario primera toma: " + medicamento.getHorarioPrimeraToma() + ")");
                                            // Crear eventos recurrentes en Google Calendar
                                            googleCalendarService.crearEventosRecurrentes(
                                                accessToken, 
                                                medicamento,
                                                new GoogleCalendarService.RecurrentEventsCallback() {
                                                    @Override
                                                    public void onSuccess(List<String> eventoIds) {
                                                        Logger.d("NuevaMedicinaActivity", 
                                                            "Eventos de Google Calendar creados exitosamente: " + eventoIds.size() + " eventos");
                                                        
                                                        // Guardar los IDs de eventos en el medicamento
                                                        if (!eventoIds.isEmpty() && medicamento.getId() != null) {
                                                            guardarEventoIdsEnMedicamento(medicamento.getId(), eventoIds);
                                                        } else if (eventoIds.isEmpty()) {
                                                            Logger.w("NuevaMedicinaActivity", 
                                                                "No se crearon eventos (lista vacía). ¿El medicamento tiene tomas diarias > 0?");
                                                        }
                                                    }
                                                    
                                                    @Override
                                                    public void onError(Exception exception) {
                                                        Logger.e("NuevaMedicinaActivity", 
                                                            "Error al crear eventos en Google Calendar", exception);
                                                        // No mostrar error al usuario, es opcional
                                                    }
                                                }
                                            );
                                        } else {
                                            Logger.w("NuevaMedicinaActivity", 
                                                "Token de Google Calendar no disponible o vacío. TokenData keys: " + 
                                                (tokenData != null ? tokenData.keySet().toString() : "null"));
                                        }
                                    } else {
                                        Logger.w("NuevaMedicinaActivity", 
                                            "Token de Google Calendar es null o no es un Map. Tipo: " + 
                                            (tokenResult != null ? tokenResult.getClass().getName() : "null"));
                                    }
                                }
                                
                                @Override
                                public void onError(Exception exception) {
                                    Logger.e("NuevaMedicinaActivity", 
                                        "Error al obtener token de Google Calendar", exception);
                                    // No intentar crear eventos si no se pudo obtener el token
                                    // El error ya está logueado para debugging
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
     * Guarda los IDs de eventos de Google Calendar en el medicamento en Firestore
     */
    private void guardarEventoIdsEnMedicamento(String medicamentoId, List<String> eventoIds) {
        if (medicamentoId == null || medicamentoId.isEmpty() || eventoIds == null || eventoIds.isEmpty()) {
            Logger.w("NuevaMedicinaActivity", 
                "No se pueden guardar eventoIds: medicamentoId o eventoIds inválidos");
            return;
        }
        
        Logger.d("NuevaMedicinaActivity", 
            "Guardando " + eventoIds.size() + " eventoIds en Firestore para medicamento: " + medicamentoId);
        
        // Guardar directamente en Firestore
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("medicamentos")
            .document(medicamentoId)
            .update("eventoIdsGoogleCalendar", eventoIds)
            .addOnSuccessListener(aVoid -> {
                Logger.d("NuevaMedicinaActivity", 
                    "EventoIds guardados exitosamente en Firestore para medicamento: " + medicamentoId);
            })
            .addOnFailureListener(e -> {
                Logger.e("NuevaMedicinaActivity", 
                    "Error al guardar eventoIds en Firestore para medicamento: " + medicamentoId, e);
            });
    }
}