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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BotiquinActivity extends AppCompatActivity implements BotiquinAdapter.OnMedicamentoClickListener {

    private static final String TAG = "BotiquinActivity";
    
    // RecyclerViews para cada sección
    private RecyclerView rvMedicamentosTratamiento;
    private RecyclerView rvMedicamentosTratamientoSinStock;
    private RecyclerView rvMedicamentosOcasionales;
    private TextView tvTituloTratamiento;
    private TextView tvTituloTratamientoSinStock;
    private TextView tvTituloOcasionales;
    
    // Adapters
    private BotiquinAdapter adapterTratamiento;
    private BotiquinAdapter adapterTratamientoSinStock;
    private BotiquinAdapter adapterOcasionales;
    
    // Listas de medicamentos
    private final List<Medicamento> medicamentosTratamiento = new ArrayList<>();
    private final List<Medicamento> medicamentosTratamientoSinStock = new ArrayList<>();
    private final List<Medicamento> medicamentosOcasionales = new ArrayList<>();
    
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
        rvMedicamentosTratamientoSinStock = findViewById(R.id.rvMedicamentosTratamientoSinStock);
        rvMedicamentosOcasionales = findViewById(R.id.rvMedicamentosOcasionales);
        tvTituloTratamiento = findViewById(R.id.tvTituloTratamiento);
        tvTituloTratamientoSinStock = findViewById(R.id.tvTituloTratamientoSinStock);
        tvTituloOcasionales = findViewById(R.id.tvTituloOcasionales);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavHistorial = findViewById(R.id.btnNavHistorial);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);
    }

    private void configurarRecyclerViews() {
        // Adapter para medicamentos con tratamiento (con stock)
        adapterTratamiento = new BotiquinAdapter(this, medicamentosTratamiento);
        adapterTratamiento.setOnMedicamentoClickListener(this);
        rvMedicamentosTratamiento.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentosTratamiento.setAdapter(adapterTratamiento);
        
        // Adapter para medicamentos con tratamiento (sin stock)
        adapterTratamientoSinStock = new BotiquinAdapter(this, medicamentosTratamientoSinStock);
        adapterTratamientoSinStock.setOnMedicamentoClickListener(this);
        rvMedicamentosTratamientoSinStock.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentosTratamientoSinStock.setAdapter(adapterTratamientoSinStock);
        
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
        Log.d(TAG, "cargarMedicamentos: ========== INICIANDO CARGA DE MEDICAMENTOS ==========");
        Logger.d(TAG, "cargarMedicamentos: ========== INICIANDO CARGA DE MEDICAMENTOS ==========");
        
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.w(TAG, "cargarMedicamentos: ⚠️ No hay conexión a internet");
            Logger.w(TAG, "cargarMedicamentos: ⚠️ No hay conexión a internet");
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }
        
        Log.d(TAG, "cargarMedicamentos: Conexión a internet disponible, iniciando consulta a Firestore");
        Logger.d(TAG, "cargarMedicamentos: Conexión a internet disponible, iniciando consulta a Firestore");

        // Cargar todos los medicamentos desde Firebase
        firebaseService.obtenerMedicamentos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                Logger.d(TAG, "cargarMedicamentos: onSuccess recibido. result != null: " + (result != null));
                
                List<Medicamento> todosLosMedicamentos = new ArrayList<>();
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    List<Medicamento> resultList = (List<Medicamento>) result;
                    todosLosMedicamentos = resultList;
                    Logger.d(TAG, "cargarMedicamentos: Total medicamentos recibidos de Firestore: " + todosLosMedicamentos.size());
                    
                    // Log detallado de cada medicamento
                    for (int i = 0; i < todosLosMedicamentos.size(); i++) {
                        Medicamento m = todosLosMedicamentos.get(i);
                        Logger.d(TAG, String.format("cargarMedicamentos: [%d] ID=%s, Nombre=%s, TomasDiarias=%d, StockActual=%d, Pausado=%s", 
                            i, m.getId(), m.getNombre(), m.getTomasDiarias(), m.getStockActual(), m.isPausado()));
                    }
                } else {
                    Logger.w(TAG, "cargarMedicamentos: ⚠️ result es null");
                }
                
                // Verificar duplicados por ID
                Set<String> idsVistos = new HashSet<>();
                List<String> idsDuplicados = new ArrayList<>();
                for (Medicamento m : todosLosMedicamentos) {
                    if (idsVistos.contains(m.getId())) {
                        Logger.w(TAG, "cargarMedicamentos: ⚠️ DUPLICADO EN LISTA - ID: " + m.getId() + ", Nombre: " + m.getNombre());
                        idsDuplicados.add(m.getId());
                    } else {
                        idsVistos.add(m.getId());
                    }
                }
                
                if (!idsDuplicados.isEmpty()) {
                    Logger.e(TAG, "cargarMedicamentos: ❌ ERROR - Medicamentos duplicados en la lista: " + idsDuplicados.toString());
                }
                
                // Separar medicamentos por tipo
                Logger.d(TAG, "cargarMedicamentos: Separando medicamentos...");
                separarMedicamentos(todosLosMedicamentos);
                
                Logger.d(TAG, String.format("cargarMedicamentos: Separación completada - Tratamiento (con stock): %d, Tratamiento (sin stock): %d, Ocasionales: %d",
                    medicamentosTratamiento.size(), medicamentosTratamientoSinStock.size(), medicamentosOcasionales.size()));
                
                // Log detallado de cada sección
                Logger.d(TAG, "=== TRATAMIENTOS (CON STOCK) ===");
                for (int i = 0; i < medicamentosTratamiento.size(); i++) {
                    Medicamento m = medicamentosTratamiento.get(i);
                    Logger.d(TAG, String.format("  [%d] %s (ID: %s, Stock: %d)", i, m.getNombre(), m.getId(), m.getStockActual()));
                }
                
                Logger.d(TAG, "=== TRATAMIENTOS (SIN STOCK) ===");
                for (int i = 0; i < medicamentosTratamientoSinStock.size(); i++) {
                    Medicamento m = medicamentosTratamientoSinStock.get(i);
                    Logger.d(TAG, String.format("  [%d] %s (ID: %s, Stock: %d)", i, m.getNombre(), m.getId(), m.getStockActual()));
                }
                
                Logger.d(TAG, "=== OCASIONALES ===");
                for (int i = 0; i < medicamentosOcasionales.size(); i++) {
                    Medicamento m = medicamentosOcasionales.get(i);
                    Logger.d(TAG, String.format("  [%d] %s (ID: %s, Stock: %d)", i, m.getNombre(), m.getId(), m.getStockActual()));
                }
                
                // Actualizar adapters en el hilo principal
                Logger.d(TAG, "cargarMedicamentos: Actualizando adapters en hilo principal...");
                runOnUiThread(() -> {
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] Actualizando adapterTratamiento con " + medicamentosTratamiento.size() + " medicamentos");
                    adapterTratamiento.actualizarMedicamentos(medicamentosTratamiento);
                    
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] Actualizando adapterTratamientoSinStock con " + medicamentosTratamientoSinStock.size() + " medicamentos");
                    adapterTratamientoSinStock.actualizarMedicamentos(medicamentosTratamientoSinStock);
                    
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] Actualizando adapterOcasionales con " + medicamentosOcasionales.size() + " medicamentos");
                    adapterOcasionales.actualizarMedicamentos(medicamentosOcasionales);
                    
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] Adapters actualizados, verificando RecyclerViews...");
                    
                    // Verificar que los RecyclerViews estén correctamente configurados
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] rvMedicamentosTratamiento != null: " + (rvMedicamentosTratamiento != null) + 
                        ", adapter != null: " + (rvMedicamentosTratamiento != null && rvMedicamentosTratamiento.getAdapter() != null));
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] rvMedicamentosTratamientoSinStock != null: " + (rvMedicamentosTratamientoSinStock != null) + 
                        ", adapter != null: " + (rvMedicamentosTratamientoSinStock != null && rvMedicamentosTratamientoSinStock.getAdapter() != null));
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] rvMedicamentosOcasionales != null: " + (rvMedicamentosOcasionales != null) + 
                        ", adapter != null: " + (rvMedicamentosOcasionales != null && rvMedicamentosOcasionales.getAdapter() != null));
                    
                    // Mostrar/ocultar secciones según corresponda
                    actualizarVisibilidadSecciones();
                    
                    Logger.d(TAG, "cargarMedicamentos: [UI Thread] Actualización de UI completada");
                });
                
                Log.d(TAG, "Medicamentos cargados: " + medicamentosTratamiento.size() + " con tratamiento (con stock), " + 
                      medicamentosTratamientoSinStock.size() + " con tratamiento (sin stock), " +
                      medicamentosOcasionales.size() + " ocasionales");
                
                if (todosLosMedicamentos.isEmpty()) {
                    Toast.makeText(BotiquinActivity.this, "No tienes medicamentos registrados", Toast.LENGTH_SHORT).show();
                } else if (medicamentosTratamiento.size() + medicamentosTratamientoSinStock.size() + medicamentosOcasionales.size() != todosLosMedicamentos.size()) {
                    Logger.e(TAG, "cargarMedicamentos: ❌ ERROR - La suma de secciones no coincide con el total. Total: " + 
                        todosLosMedicamentos.size() + ", Suma secciones: " + 
                        (medicamentosTratamiento.size() + medicamentosTratamientoSinStock.size() + medicamentosOcasionales.size()));
                }
                
                Logger.d(TAG, "cargarMedicamentos: ========== CARGA COMPLETADA ==========");
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
        Logger.d(TAG, "separarMedicamentos: Iniciando separación de " + todosLosMedicamentos.size() + " medicamentos");
        
        medicamentosTratamiento.clear();
        medicamentosTratamientoSinStock.clear();
        medicamentosOcasionales.clear();
        
        for (Medicamento medicamento : todosLosMedicamentos) {
            if (medicamento == null) {
                Logger.w(TAG, "separarMedicamentos: ⚠️ Medicamento null encontrado, saltando");
                continue;
            }
            
            String nombre = medicamento.getNombre() != null ? medicamento.getNombre() : "SIN_NOMBRE";
            int tomasDiarias = medicamento.getTomasDiarias();
            int stockActual = medicamento.getStockActual();
            boolean pausado = medicamento.isPausado();
            
            Logger.d(TAG, String.format("separarMedicamentos: Procesando '%s' - TomasDiarias=%d, StockActual=%d, Pausado=%s", 
                nombre, tomasDiarias, stockActual, pausado));
            
            // Medicamentos con tratamiento: tomasDiarias > 0
            if (tomasDiarias > 0) {
                // Separar por stock: si tiene stock (stockActual > 0) va a tratamiento, si no tiene stock va a tratamiento sin stock
                if (stockActual > 0) {
                    medicamentosTratamiento.add(medicamento);
                    Logger.d(TAG, String.format("separarMedicamentos: '%s' -> TRATAMIENTO (con stock)", nombre));
                } else {
                    medicamentosTratamientoSinStock.add(medicamento);
                    Logger.d(TAG, String.format("separarMedicamentos: '%s' -> TRATAMIENTO (sin stock)", nombre));
                }
            } else {
                // Medicamentos ocasionales: tomasDiarias = 0
                medicamentosOcasionales.add(medicamento);
                Logger.d(TAG, String.format("separarMedicamentos: '%s' -> OCASIONAL", nombre));
            }
        }
        
        Logger.d(TAG, String.format("separarMedicamentos: Separación completada - Tratamiento (con stock): %d, Tratamiento (sin stock): %d, Ocasionales: %d",
            medicamentosTratamiento.size(), medicamentosTratamientoSinStock.size(), medicamentosOcasionales.size()));
    }

    private void actualizarVisibilidadSecciones() {
        Logger.d(TAG, "actualizarVisibilidadSecciones: ========== ACTUALIZANDO VISIBILIDAD ==========");
        Logger.d(TAG, String.format("actualizarVisibilidadSecciones: Tratamiento (con stock): %d, Tratamiento (sin stock): %d, Ocasionales: %d",
            medicamentosTratamiento.size(), medicamentosTratamientoSinStock.size(), medicamentosOcasionales.size()));
        
        // Sección de tratamientos con stock
        if (medicamentosTratamiento.isEmpty()) {
            Logger.d(TAG, "actualizarVisibilidadSecciones: Ocultando sección Tratamiento (con stock) - vacía");
            if (tvTituloTratamiento != null) {
                tvTituloTratamiento.setVisibility(View.GONE);
            }
            if (rvMedicamentosTratamiento != null) {
                rvMedicamentosTratamiento.setVisibility(View.GONE);
            }
        } else {
            Logger.d(TAG, "actualizarVisibilidadSecciones: Mostrando sección Tratamiento (con stock) - " + medicamentosTratamiento.size() + " medicamentos");
            if (tvTituloTratamiento != null) {
                tvTituloTratamiento.setVisibility(View.VISIBLE);
                Logger.d(TAG, "actualizarVisibilidadSecciones: tvTituloTratamiento.setVisibility(VISIBLE) llamado");
            }
            if (rvMedicamentosTratamiento != null) {
                rvMedicamentosTratamiento.setVisibility(View.VISIBLE);
                Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosTratamiento.setVisibility(VISIBLE) llamado");
                Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosTratamiento.getAdapter() != null: " + 
                    (rvMedicamentosTratamiento.getAdapter() != null));
                if (rvMedicamentosTratamiento.getAdapter() != null) {
                    Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosTratamiento.getAdapter().getItemCount()=" + 
                        rvMedicamentosTratamiento.getAdapter().getItemCount());
                }
            }
        }
        
        // Sección de tratamientos sin stock
        if (medicamentosTratamientoSinStock.isEmpty()) {
            Logger.d(TAG, "actualizarVisibilidadSecciones: Ocultando sección Tratamiento (sin stock) - vacía");
            if (tvTituloTratamientoSinStock != null) {
                tvTituloTratamientoSinStock.setVisibility(View.GONE);
            }
            if (rvMedicamentosTratamientoSinStock != null) {
                rvMedicamentosTratamientoSinStock.setVisibility(View.GONE);
            }
        } else {
            Logger.d(TAG, "actualizarVisibilidadSecciones: Mostrando sección Tratamiento (sin stock) - " + medicamentosTratamientoSinStock.size() + " medicamentos");
            if (tvTituloTratamientoSinStock != null) {
                tvTituloTratamientoSinStock.setVisibility(View.VISIBLE);
                Logger.d(TAG, "actualizarVisibilidadSecciones: tvTituloTratamientoSinStock.setVisibility(VISIBLE) llamado");
            }
            if (rvMedicamentosTratamientoSinStock != null) {
                rvMedicamentosTratamientoSinStock.setVisibility(View.VISIBLE);
                Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosTratamientoSinStock.setVisibility(VISIBLE) llamado");
                Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosTratamientoSinStock.getAdapter() != null: " + 
                    (rvMedicamentosTratamientoSinStock.getAdapter() != null));
                if (rvMedicamentosTratamientoSinStock.getAdapter() != null) {
                    Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosTratamientoSinStock.getAdapter().getItemCount()=" + 
                        rvMedicamentosTratamientoSinStock.getAdapter().getItemCount());
                }
            }
        }
        
        // Sección de ocasionales
        if (medicamentosOcasionales.isEmpty()) {
            Logger.d(TAG, "actualizarVisibilidadSecciones: Ocultando sección Ocasionales - vacía");
            if (tvTituloOcasionales != null) {
                tvTituloOcasionales.setVisibility(View.GONE);
            }
            if (rvMedicamentosOcasionales != null) {
                rvMedicamentosOcasionales.setVisibility(View.GONE);
            }
        } else {
            Logger.d(TAG, "actualizarVisibilidadSecciones: Mostrando sección Ocasionales - " + medicamentosOcasionales.size() + " medicamentos");
            if (tvTituloOcasionales != null) {
                tvTituloOcasionales.setVisibility(View.VISIBLE);
                Logger.d(TAG, "actualizarVisibilidadSecciones: tvTituloOcasionales.setVisibility(VISIBLE) llamado");
            }
            if (rvMedicamentosOcasionales != null) {
                rvMedicamentosOcasionales.setVisibility(View.VISIBLE);
                Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosOcasionales.setVisibility(VISIBLE) llamado");
                Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosOcasionales.getAdapter() != null: " + 
                    (rvMedicamentosOcasionales.getAdapter() != null));
                if (rvMedicamentosOcasionales.getAdapter() != null) {
                    Logger.d(TAG, "actualizarVisibilidadSecciones: rvMedicamentosOcasionales.getAdapter().getItemCount()=" + 
                        rvMedicamentosOcasionales.getAdapter().getItemCount());
                }
            }
        }
        
        Logger.d(TAG, "actualizarVisibilidadSecciones: ========== VISIBILIDAD ACTUALIZADA ==========");
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
                .setPositiveButton("Eliminar", (dialog, which) -> {
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
        // minSdk es 29, así que LOLLIPOP (21) siempre será true
        // Limpiar cualquier flag que pueda ocultar la barra de estado
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Habilitar dibujo de la barra de estado
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        
        // Establecer color de la barra de estado (usando ContextCompat para compatibilidad)
        window.setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_dark));
        
        // Configurar apariencia de la barra de estado
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // API 30+
            WindowInsetsControllerCompat controller = 
                WindowCompat.getInsetsController(window, window.getDecorView());
            // controller nunca es null según WindowCompat.getInsetsController
            controller.setAppearanceLightStatusBars(false);
            // Asegurar que la barra de estado sea visible
            controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars());
        } else {
            // API 29 (minSdk es 29, así que M (23) siempre será true)
            int flags = window.getDecorView().getSystemUiVisibility();
            // Limpiar flags que oculten la barra de estado
            flags &= ~android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
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
