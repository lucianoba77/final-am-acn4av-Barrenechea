package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.controlmedicamentos.myapplication.adapters.MedicamentoAdapter;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.TomaStateCheckerService;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import com.controlmedicamentos.myapplication.utils.ErrorHandler;
import com.controlmedicamentos.myapplication.utils.Logger;
import com.controlmedicamentos.myapplication.utils.MedicamentoDataManager;
import com.controlmedicamentos.myapplication.utils.NavigationHelper;
import com.controlmedicamentos.myapplication.utils.StockAlertManager;
import com.controlmedicamentos.myapplication.utils.TomaActionHandler;
import com.controlmedicamentos.myapplication.utils.UIHelper;
import com.controlmedicamentos.myapplication.utils.ValidationUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MedicamentoAdapter.OnMedicamentoClickListener {

    private static final String TAG = "MainActivity";
    private RecyclerView rvMedicamentos;
    private View tvEmptyDashboard;
    private View btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavHistorial, btnNavAjustes;
    private ProgressBar progressBar;
    private MedicamentoAdapter adapter;
    private List<Medicamento> medicamentos;
    
    private AuthService authService;
    private FirebaseService firebaseService;
    private TomaTrackingService tomaTrackingService;
    private boolean firstResume = true;
    private MedicamentoDataManager dataManager;
    private TomaActionHandler tomaActionHandler;
    private StockAlertManager stockAlertManager;
    private ListenerRegistration medicamentosListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado
        UIHelper.configurarBarraEstado(getWindow());
        
        // Ocultar ActionBar/Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        try {
            setContentView(R.layout.activity_main);
            
            // Aplicar window insets al header
            View headerLayout = findViewById(R.id.headerLayout);
            if (headerLayout != null) {
                headerLayout.post(() -> UIHelper.aplicarWindowInsets(headerLayout, this));
            }
            // Respetar barra de navegación del sistema para que el menú inferior no quede solapado
            View barraNav = findViewById(R.id.barraNavegacion);
            if (barraNav != null) {
                UIHelper.aplicarInsetsBarraNavegacionInferior(barraNav);
            }

            // Inicializar servicios
            authService = new AuthService();
            firebaseService = new FirebaseService();
            tomaTrackingService = new TomaTrackingService(this);
            dataManager = new MedicamentoDataManager(this, firebaseService, tomaTrackingService);
            tomaActionHandler = new TomaActionHandler(this, firebaseService, tomaTrackingService);
            stockAlertManager = new StockAlertManager(this);

            // Verificar autenticación
            if (!authService.isUserLoggedIn()) {
                Logger.w(TAG, "Usuario no autenticado, redirigiendo a login");
                irALogin();
                return;
            }

            Logger.d(TAG, "Usuario autenticado, continuando con inicialización");

            // Inicializar vistas
            inicializarVistas();

            // Configurar RecyclerView
            configurarRecyclerView();

            // Cargar datos desde Firebase
            cargarDatosDesdeFirebase();

            // Configurar navegación
            configurarNavegacion();

            // Iniciar servicio de verificación de estados de tomas
            iniciarServicioVerificacionTomas();

            Logger.d(TAG, "MainActivity inicializada correctamente");
        } catch (Exception e) {
            Logger.e(TAG, "Error crítico en onCreate", e);
            Toast.makeText(this, getString(R.string.msg_error_starting_app), Toast.LENGTH_LONG).show();
            try {
                irALogin();
            } catch (Exception ex) {
                Logger.e(TAG, "Error al redirigir a login", ex);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        // Refrescar al volver de otra pantalla (p. ej. Botiquín) para que el dashboard coincida con el Botiquín
        if (dataManager != null && authService != null && authService.isUserLoggedIn()) {
            dataManager.cargarMedicamentos(null, new MedicamentoDataManager.DataCallback() {
                @Override
                public void onDataLoaded(List<Medicamento> medicamentosParaDashboard, List<Medicamento> todosLosMedicamentos) {
                    if (adapter != null) {
                        medicamentos = dataManager.ordenarPorHorario(medicamentosParaDashboard);
                        adapter.actualizarMedicamentos(medicamentos);
                        actualizarVisibilidadListaVacia();
                    }
                    if (stockAlertManager != null && todosLosMedicamentos != null) {
                        stockAlertManager.verificarAlertasStock(todosLosMedicamentos);
                    }
                }
                @Override
                public void onError(Exception exception) { /* no mostrar error en refresh en segundo plano */ }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataManager != null) {
            dataManager.removerListener();
        }
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void inicializarVistas() {
        try {
            rvMedicamentos = findViewById(R.id.rvMedicamentos);
            tvEmptyDashboard = findViewById(R.id.tvEmptyDashboard);
            btnNavHome = findViewById(R.id.btnNavHome);
            btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
            btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
            btnNavHistorial = findViewById(R.id.btnNavHistorial);
            btnNavAjustes = findViewById(R.id.btnNavAjustes);
            
            if (rvMedicamentos == null) {
                Logger.e(TAG, "rvMedicamentos es null!");
                throw new RuntimeException("RecyclerView no encontrado en el layout");
            }
            
            progressBar = findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            
            Logger.d(TAG, "Vistas inicializadas correctamente");
        } catch (Exception e) {
            Logger.e(TAG, "Error al inicializar vistas", e);
            throw e;
        }
    }

    private void configurarRecyclerView() {
        medicamentos = new ArrayList<>();
        adapter = new MedicamentoAdapter(this, medicamentos);
        adapter.setTomaTrackingService(tomaTrackingService);
        adapter.setOnMedicamentoClickListener(this);
        rvMedicamentos.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentos.setAdapter(adapter);
    }

    private void actualizarVisibilidadListaVacia() {
        if (tvEmptyDashboard != null && rvMedicamentos != null) {
            boolean vacia = medicamentos == null || medicamentos.isEmpty();
            tvEmptyDashboard.setVisibility(vacia ? View.VISIBLE : View.GONE);
            rvMedicamentos.setVisibility(vacia ? View.GONE : View.VISIBLE);
        }
    }

    private void cargarDatosDesdeFirebase() {
        dataManager.cargarMedicamentos(progressBar, new MedicamentoDataManager.DataCallback() {
            @Override
            public void onDataLoaded(List<Medicamento> medicamentosParaDashboard, List<Medicamento> todosLosMedicamentos) {
                try {
                    if (adapter != null) {
                        medicamentos = dataManager.ordenarPorHorario(medicamentosParaDashboard);
                        adapter.actualizarMedicamentos(medicamentos);
                        actualizarVisibilidadListaVacia();
                        Logger.d(TAG, "Carga inicial: dashboard actualizado con " + medicamentos.size() + " medicamentos (máx en Botiquín: " + (todosLosMedicamentos != null ? todosLosMedicamentos.size() : 0) + ")");
                    }
                    // Verificar alertas de stock
                    stockAlertManager.verificarAlertasStock(todosLosMedicamentos);
                } catch (Exception e) {
                    ErrorHandler.handleError(MainActivity.this, e, TAG);
                }
            }

            @Override
            public void onError(Exception exception) {
                ErrorHandler.handleError(MainActivity.this, exception, TAG);
            }
        });

        // Configurar listener para actualizaciones en tiempo real
        medicamentosListener = dataManager.configurarListenerTiempoReal(new MedicamentoDataManager.DataCallback() {
            @Override
            public void onDataLoaded(List<Medicamento> medicamentosParaDashboard, List<Medicamento> todosLosMedicamentos) {
                try {
                    medicamentos = dataManager.ordenarPorHorario(medicamentosParaDashboard);
                    if (adapter != null) {
                        adapter.actualizarMedicamentos(medicamentos);
                        actualizarVisibilidadListaVacia();
                        Logger.d(TAG, "Listener: dashboard actualizado con " + medicamentos.size() + " medicamentos");
                    }
                    stockAlertManager.verificarAlertasStock(todosLosMedicamentos);
                } catch (Exception e) {
                    Logger.e(TAG, "Error en callback del listener", e);
                }
            }

            @Override
            public void onError(Exception exception) {
                Logger.w(TAG, "Error en listener de tiempo real", exception);
            }
        });
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

    @Override
    public void onTomadoClick(Medicamento medicamento) {
        tomaActionHandler.marcarTomaComoTomada(medicamento, new TomaActionHandler.TomaActionCallback() {
            @Override
            public void onSuccess(Medicamento medicamento, boolean completoTodasLasTomas, boolean tratamientoCompletado) {
                        if (completoTodasLasTomas) {
                            medicamentos.remove(medicamento);
                            adapter.actualizarMedicamentos(medicamentos);
                            actualizarVisibilidadListaVacia();
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.msg_all_takes_completed, medicamento.getNombre()),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            medicamentos = dataManager.ordenarPorHorario(medicamentos);
                            adapter.actualizarMedicamentos(medicamentos);
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.msg_taken_short, medicamento.getNombre()),
                                    Toast.LENGTH_SHORT).show();
                        }
                        
                        if (tratamientoCompletado) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.msg_treatment_completed, medicamento.getNombre()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                String errorMessage = (exception != null && exception.getMessage() != null)
                    ? exception.getMessage()
                    : getString(R.string.msg_error_registering_take);
                ErrorHandler.handleErrorWithCustomMessage(MainActivity.this, exception, TAG, errorMessage);
            }
        });
    }

    @Override
    public void onMedicamentoClick(Medicamento medicamento) {
        // No se muestra pantalla de detalles del medicamento desde el dashboard (según preferencia de UX).
        // El usuario puede editar o ver detalles desde el Botiquín.
    }
    
    @Override
    public void onPosponerClick(Medicamento medicamento) {
        if (medicamento == null) {
            return;
        }
        
        TomaProgramada tomaPosponible = tomaTrackingService != null
                ? tomaTrackingService.obtenerTomaPosponible(medicamento.getId())
                : null;
        String horarioToma = (tomaPosponible != null && tomaPosponible.getHorario() != null)
                ? tomaPosponible.getHorario()
                : null;
        if (horarioToma == null) {
            ErrorHandler.handleErrorWithCustomMessage(this, null, TAG,
                "No hay tomas en ventana para posponer (30 min antes hasta 1 h después)");
            return;
        }
        
        // Intentar posponer la toma (el feedback ya se muestra en TomaActionHandler)
        boolean pospuesta = tomaActionHandler.posponerToma(medicamento, horarioToma);
        
        // Actualizar la UI independientemente del resultado para reflejar cualquier cambio
        medicamentos = dataManager.ordenarPorHorario(medicamentos);
        adapter.actualizarMedicamentos(medicamentos);
    }

    @Override
    public void onTomadoHorarioClick(Medicamento medicamento, String horario) {
        tomaActionHandler.marcarTomaComoTomada(medicamento, horario, new TomaActionHandler.TomaActionCallback() {
            @Override
            public void onSuccess(Medicamento med, boolean completoTodasLasTomas, boolean tratamientoCompletado) {
                if (completoTodasLasTomas) {
                    medicamentos.remove(med);
                    adapter.actualizarMedicamentos(medicamentos);
                    actualizarVisibilidadListaVacia();
                    Toast.makeText(MainActivity.this,
                            getString(R.string.msg_all_takes_completed, med.getNombre()),
                            Toast.LENGTH_LONG).show();
                } else {
                    medicamentos = dataManager.ordenarPorHorario(medicamentos);
                    adapter.actualizarMedicamentos(medicamentos);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.msg_taken_short, med.getNombre()),
                            Toast.LENGTH_SHORT).show();
                }
                if (tratamientoCompletado) {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.msg_treatment_completed, med.getNombre()),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onError(Exception exception) {
                String errorMessage = (exception != null && exception.getMessage() != null)
                        ? exception.getMessage()
                        : getString(R.string.msg_error_registering_take);
                ErrorHandler.handleErrorWithCustomMessage(MainActivity.this, exception, TAG, errorMessage);
            }
        });
    }

    @Override
    public void onPosponerHorarioClick(Medicamento medicamento, String horario) {
        if (medicamento == null || horario == null) return;
        tomaActionHandler.posponerToma(medicamento, horario);
        medicamentos = dataManager.ordenarPorHorario(medicamentos);
        adapter.actualizarMedicamentos(medicamentos);
    }
    
    private String obtenerHorarioTomaEnAlerta(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            Logger.w(TAG, "obtenerHorarioTomaEnAlerta: medicamento o ID es null");
            return null;
        }
        
        if (tomaTrackingService == null) {
            Logger.w(TAG, "obtenerHorarioTomaEnAlerta: tomaTrackingService es null");
            return null;
        }
        
        TomaProgramada tomaPosponible = tomaTrackingService.obtenerTomaPosponible(medicamento.getId());
        if (tomaPosponible != null && tomaPosponible.getHorario() != null) {
            return tomaPosponible.getHorario();
        }
        
        List<TomaProgramada> tomas = tomaTrackingService.obtenerTomasMedicamento(medicamento.getId());
        if (tomas == null || tomas.isEmpty()) {
            return null;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma == null) {
                continue;
            }
            
            if (!toma.isTomada() && 
                (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA ||
                 toma.getEstado() == TomaProgramada.EstadoTomaProgramada.RETRASO)) {
                String horario = toma.getHorario();
                if (horario != null && !horario.isEmpty()) {
                    return horario;
                }
            }
        }
        
        return null;
    }
    
    private void iniciarServicioVerificacionTomas() {
        Intent serviceIntent = new Intent(this, TomaStateCheckerService.class);
        startService(serviceIntent);
        Logger.d(TAG, "Servicio de verificación de tomas iniciado");
    }
}
