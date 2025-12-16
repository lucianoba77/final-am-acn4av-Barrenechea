package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.adapters.BotiquinAdapter;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.controlmedicamentos.myapplication.utils.AlarmScheduler;
import com.controlmedicamentos.myapplication.utils.GoogleCalendarSyncHelper;
import com.controlmedicamentos.myapplication.utils.Logger;
import com.controlmedicamentos.myapplication.utils.NavigationHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BotiquinActivity extends AppCompatActivity implements BotiquinAdapter.OnMedicamentoClickListener {

    private static final String TAG = "BotiquinActivity";
    
    // RecyclerViews para cada sección
    private RecyclerView rvMedicamentosTratamiento;
    private RecyclerView rvMedicamentosOcasionales;
    private TextView tvTituloTratamiento;
    private TextView tvTituloOcasionales;
    
    // Adapters
    private BotiquinAdapter adapterTratamiento;
    private BotiquinAdapter adapterOcasionales;
    
    // Listas de medicamentos
    private List<Medicamento> medicamentosTratamiento = new ArrayList<>();
    private List<Medicamento> medicamentosOcasionales = new ArrayList<>();
    
    // Botones de navegación
    private MaterialButton btnNavHome;
    private MaterialButton btnNavNuevaMedicina;
    private MaterialButton btnNavBotiquin;
    private MaterialButton btnNavHistorial;
    private MaterialButton btnNavAjustes;
    
    private AuthService authService;
    private FirebaseService firebaseService;
    private GoogleCalendarSyncHelper googleCalendarSyncHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado para que sea visible
        configurarBarraEstado();
        
        setContentView(R.layout.activity_botiquin);
        
        // Aplicar window insets al header para respetar la barra de estado
        // Usar post para asegurar que se ejecute después del layout
        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            headerLayout.post(() -> aplicarWindowInsets(headerLayout));
        }

        // Ocultar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inicializar servicios
        authService = new AuthService();
        firebaseService = new FirebaseService();
        googleCalendarSyncHelper = new GoogleCalendarSyncHelper(this);

        // Verificar autenticación
        if (!authService.isUserLoggedIn()) {
            finish();
            return;
        }

        inicializarVistas();
        configurarRecyclerViews();
        cargarMedicamentos();
        configurarNavegacion();
    }

    private void inicializarVistas() {
        rvMedicamentosTratamiento = findViewById(R.id.rvMedicamentosTratamiento);
        rvMedicamentosOcasionales = findViewById(R.id.rvMedicamentosOcasionales);
        tvTituloTratamiento = findViewById(R.id.tvTituloTratamiento);
        tvTituloOcasionales = findViewById(R.id.tvTituloOcasionales);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavHistorial = findViewById(R.id.btnNavHistorial);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);
    }

    private void configurarRecyclerViews() {
        // Adapter para medicamentos con tratamiento
        adapterTratamiento = new BotiquinAdapter(this, medicamentosTratamiento);
        adapterTratamiento.setOnMedicamentoClickListener(this);
        rvMedicamentosTratamiento.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentosTratamiento.setAdapter(adapterTratamiento);
        
        // Adapter para medicamentos ocasionales
        adapterOcasionales = new BotiquinAdapter(this, medicamentosOcasionales);
        adapterOcasionales.setOnMedicamentoClickListener(this);
        rvMedicamentosOcasionales.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentosOcasionales.setAdapter(adapterOcasionales);
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

    private void cargarMedicamentos() {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Cargar todos los medicamentos desde Firebase
        firebaseService.obtenerMedicamentos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                List<Medicamento> todosLosMedicamentos = new ArrayList<>();
                if (result != null) {
                    todosLosMedicamentos = (List<Medicamento>) result;
                }
                
                // Separar medicamentos por tipo
                separarMedicamentos(todosLosMedicamentos);
                
                // Actualizar adapters
                adapterTratamiento.actualizarMedicamentos(medicamentosTratamiento);
                adapterOcasionales.actualizarMedicamentos(medicamentosOcasionales);
                
                // Mostrar/ocultar secciones según corresponda
                actualizarVisibilidadSecciones();
                
                Log.d(TAG, "Medicamentos cargados: " + medicamentosTratamiento.size() + " con tratamiento, " + 
                      medicamentosOcasionales.size() + " ocasionales");
                
                if (todosLosMedicamentos.isEmpty()) {
                    Toast.makeText(BotiquinActivity.this, "No tienes medicamentos registrados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(BotiquinActivity.this, 
                    "Error al cargar medicamentos: " + 
                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void separarMedicamentos(List<Medicamento> todosLosMedicamentos) {
        medicamentosTratamiento.clear();
        medicamentosOcasionales.clear();
        
        for (Medicamento medicamento : todosLosMedicamentos) {
            // Medicamentos con tratamiento: tomasDiarias > 0
            if (medicamento.getTomasDiarias() > 0) {
                medicamentosTratamiento.add(medicamento);
            } else {
                // Medicamentos ocasionales: tomasDiarias = 0
                medicamentosOcasionales.add(medicamento);
            }
        }
    }

    private void actualizarVisibilidadSecciones() {
        // Sección de tratamientos
        if (medicamentosTratamiento.isEmpty()) {
            tvTituloTratamiento.setVisibility(View.GONE);
            rvMedicamentosTratamiento.setVisibility(View.GONE);
        } else {
            tvTituloTratamiento.setVisibility(View.VISIBLE);
            rvMedicamentosTratamiento.setVisibility(View.VISIBLE);
        }
        
        // Sección de ocasionales
        if (medicamentosOcasionales.isEmpty()) {
            tvTituloOcasionales.setVisibility(View.GONE);
            rvMedicamentosOcasionales.setVisibility(View.GONE);
        } else {
            tvTituloOcasionales.setVisibility(View.VISIBLE);
            rvMedicamentosOcasionales.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditarClick(Medicamento medicamento) {
        Intent intent = new Intent(BotiquinActivity.this, NuevaMedicinaActivity.class);
        intent.putExtra("medicamento_id", medicamento.getId());
        startActivity(intent);
    }

    @Override
    public void onEliminarClick(Medicamento medicamento) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Medicamento")
                .setMessage("¿Estás seguro de que quieres eliminar " + medicamento.getNombre() + "?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!NetworkUtils.isNetworkAvailable(BotiquinActivity.this)) {
                            Toast.makeText(BotiquinActivity.this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Cancelar alarmas antes de eliminar
                        AlarmScheduler alarmScheduler = new AlarmScheduler(BotiquinActivity.this);
                        alarmScheduler.cancelarAlarmasMedicamento(medicamento);
                        
                        // Eliminar eventos de Google Calendar antes de eliminar el medicamento
                        // Nota: La eliminación del medicamento debe ocurrir incluso si falla la sincronización
                        googleCalendarSyncHelper.eliminarEventosMedicamento(medicamento.getId(), 
                            new GoogleCalendarSyncHelper.SyncCallback() {
                                @Override
                                public void onSuccess() {
                                    // Después de eliminar eventos (o si no hay eventos), eliminar el medicamento
                                    eliminarMedicamentoDeFirestore(medicamento.getId());
                                }

                                @Override
                                public void onError(Exception exception) {
                                    // Si falla la sincronización con Google Calendar, aún así eliminar el medicamento
                                    // La sincronización con Google Calendar no es crítica para la eliminación
                                    Logger.w("BotiquinActivity", 
                                        "Error al eliminar eventos de Google Calendar, continuando con eliminación del medicamento", 
                                        exception);
                                    eliminarMedicamentoDeFirestore(medicamento.getId());
                                }
                            }
                        );
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Elimina un medicamento de Firestore.
     * Este método se llama después de intentar eliminar los eventos de Google Calendar,
     * independientemente de si la sincronización fue exitosa o falló.
     * 
     * @param medicamentoId ID del medicamento a eliminar.
     */
    private void eliminarMedicamentoDeFirestore(String medicamentoId) {
        firebaseService.eliminarMedicamento(medicamentoId, 
            new FirebaseService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(BotiquinActivity.this, 
                        "Medicamento eliminado", Toast.LENGTH_SHORT).show();
                    cargarMedicamentos();
                }

                @Override
                public void onError(Exception exception) {
                    String errorMessage = (exception != null && exception.getMessage() != null) 
                        ? exception.getMessage() 
                        : "Error desconocido";
                    Toast.makeText(BotiquinActivity.this, 
                        "Error al eliminar medicamento: " + errorMessage, 
                        Toast.LENGTH_LONG).show();
                    Logger.e("BotiquinActivity", "Error al eliminar medicamento de Firestore", exception);
                }
            }
        );
    }

    @Override
    public void onTomeUnaClick(Medicamento medicamento) {
        if (medicamento == null) {
            return;
        }

        if (medicamento.getTomasDiarias() != 0) {
            Toast.makeText(this, "Esta acción solo está disponible para medicamentos ocasionales", Toast.LENGTH_SHORT).show();
            return;
        }

        if (medicamento.getStockActual() <= 0) {
            Toast.makeText(this, "No hay stock disponible para registrar la toma", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        if (medicamento.getId() == null || medicamento.getId().isEmpty()) {
            Toast.makeText(this, "Medicamento sin identificador válido", Toast.LENGTH_SHORT).show();
            return;
        }

        final int stockAnterior = medicamento.getStockActual();
        final int diasRestantesAnteriores = medicamento.getDiasRestantesDuracion();
        final boolean estabaPausado = medicamento.isPausado();

        medicamento.consumirDosis();
        final boolean tratamientoCompletado = medicamento.estaAgotado();
        if (tratamientoCompletado) {
            medicamento.pausarMedicamento();
        }

        Toma toma = new Toma();
        toma.setMedicamentoId(medicamento.getId());
        toma.setMedicamentoNombre(medicamento.getNombre());
        Date ahora = new Date();
        toma.setFechaHoraProgramada(ahora);
        toma.setFechaHoraTomada(ahora);
        toma.setEstado(Toma.EstadoToma.TOMADA);
        toma.setObservaciones("Registrada manualmente desde el botiquín");

        firebaseService.guardarToma(toma, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Pasar contexto para gestionar Google Calendar si se pausa
                firebaseService.actualizarMedicamento(medicamento, BotiquinActivity.this, 
                    new FirebaseService.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object updateResult) {
                            Toast.makeText(BotiquinActivity.this,
                                    "Toma registrada. Stock actualizado.",
                                    Toast.LENGTH_SHORT).show();
                            if (tratamientoCompletado) {
                                Toast.makeText(BotiquinActivity.this,
                                        "Tratamiento de " + medicamento.getNombre() + " completado.",
                                        Toast.LENGTH_LONG).show();
                            }
                            cargarMedicamentos();
                        }

                        @Override
                        public void onError(Exception exception) {
                            revertirCambiosMedicamento(medicamento, stockAnterior, diasRestantesAnteriores, estabaPausado);
                            Toast.makeText(BotiquinActivity.this,
                                    "Error al actualizar el medicamento",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                );
            }

            @Override
            public void onError(Exception exception) {
                revertirCambiosMedicamento(medicamento, stockAnterior, diasRestantesAnteriores, estabaPausado);
                Toast.makeText(BotiquinActivity.this,
                        "Error al registrar la toma",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void revertirCambiosMedicamento(Medicamento medicamento,
                                            int stockAnterior,
                                            int diasRestantesAnteriores,
                                            boolean estabaPausado) {
        medicamento.setStockActual(stockAnterior);
        medicamento.setDiasRestantesDuracion(diasRestantesAnteriores);
        medicamento.setPausado(estabaPausado);
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

    @Override
    protected void onResume() {
        super.onResume();
        cargarMedicamentos();
    }
}
