package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.adapters.TomaAdapter;
import com.controlmedicamentos.myapplication.models.AdherenciaIntervalo;
import com.controlmedicamentos.myapplication.models.AdherenciaResumen;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.AdherenciaCalculator;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity para mostrar los detalles completos de un medicamento
 */
public class DetallesMedicamentoActivity extends AppCompatActivity {
    private static final String TAG = "DetallesMedicamento";
    private static final String EXTRA_MEDICAMENTO_ID = "medicamento_id";
    
    private ImageView ivIconoMedicamento;
    private TextView tvNombreMedicamento;
    private TextView tvPresentacion;
    private TextView tvAfeccion;
    private TextView tvHorarios;
    private TextView tvStock;
    private TextView tvEstado;
    private TextView tvTipoTratamiento;
    private TextView tvResumenAdherencia;
    private TextView tvTomasRealizadas;
    private BarChart chartAdherenciaSemanal;
    private RecyclerView rvHistorialTomas;
    private TextView tvEmptyHistorial;
    private MaterialButton btnVolver;
    private MaterialButton btnEditar;
    
    private Medicamento medicamento;
    private List<Toma> tomasMedicamento;
    private TomaAdapter tomaAdapter;
    private AuthService authService;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado para que sea visible
        configurarBarraEstado();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_detalles_medicamento);
        
        // Aplicar window insets al header para respetar la barra de estado
        // Usar post para asegurar que se ejecute después del layout
        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            headerLayout.post(() -> aplicarWindowInsets(headerLayout));
        }
        
        // Inicializar servicios
        authService = new AuthService();
        firebaseService = new FirebaseService();
        
        // Verificar autenticación
        if (!authService.isUserLoggedIn()) {
            finish();
            return;
        }
        
        // Obtener ID del medicamento desde el Intent
        String medicamentoId = getIntent().getStringExtra(EXTRA_MEDICAMENTO_ID);
        if (medicamentoId == null || medicamentoId.isEmpty()) {
            Log.e(TAG, "No se proporcionó ID de medicamento");
            finish();
            return;
        }
        
        inicializarVistas();
        configurarRecyclerView();
        configurarGrafico();
        configurarListeners();
        
        // Cargar datos del medicamento
        cargarMedicamento(medicamentoId);
    }
    
    private void inicializarVistas() {
        ivIconoMedicamento = findViewById(R.id.ivIconoMedicamento);
        tvNombreMedicamento = findViewById(R.id.tvNombreMedicamento);
        tvPresentacion = findViewById(R.id.tvPresentacion);
        tvAfeccion = findViewById(R.id.tvAfeccion);
        tvHorarios = findViewById(R.id.tvHorarios);
        tvStock = findViewById(R.id.tvStock);
        tvEstado = findViewById(R.id.tvEstado);
        tvTipoTratamiento = findViewById(R.id.tvTipoTratamiento);
        tvResumenAdherencia = findViewById(R.id.tvResumenAdherencia);
        tvTomasRealizadas = findViewById(R.id.tvTomasRealizadas);
        chartAdherenciaSemanal = findViewById(R.id.chartAdherenciaSemanal);
        rvHistorialTomas = findViewById(R.id.rvHistorialTomas);
        tvEmptyHistorial = findViewById(R.id.tvEmptyHistorial);
        btnVolver = findViewById(R.id.btnVolver);
        btnEditar = findViewById(R.id.btnEditar);
    }
    
    private void configurarRecyclerView() {
        tomasMedicamento = new ArrayList<>();
        tomaAdapter = new TomaAdapter(this, tomasMedicamento);
        rvHistorialTomas.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialTomas.setAdapter(tomaAdapter);
    }
    
    private void configurarGrafico() {
        chartAdherenciaSemanal.getDescription().setEnabled(false);
        chartAdherenciaSemanal.setDrawGridBackground(false);
        chartAdherenciaSemanal.setDrawBarShadow(false);
        chartAdherenciaSemanal.setDrawValueAboveBar(true);
        
        XAxis xAxis = chartAdherenciaSemanal.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        chartAdherenciaSemanal.getAxisLeft().setDrawGridLines(true);
        chartAdherenciaSemanal.getAxisRight().setEnabled(false);
        chartAdherenciaSemanal.getLegend().setEnabled(false);
        chartAdherenciaSemanal.animateY(800);
    }
    
    private void configurarListeners() {
        btnVolver.setOnClickListener(v -> finish());
        
        btnEditar.setOnClickListener(v -> {
            if (medicamento != null) {
                // Abrir actividad de edición (por ahora NuevaMedicinaActivity con modo edición)
                Intent intent = new Intent(this, NuevaMedicinaActivity.class);
                intent.putExtra("medicamento_id", medicamento.getId());
                intent.putExtra("modo_edicion", true);
                startActivity(intent);
            }
        });
    }
    
    private void cargarMedicamento(String medicamentoId) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.e(TAG, "No hay conexión a internet");
            return;
        }
        
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    medicamento = (Medicamento) result;
                    mostrarInformacionMedicamento();
                    cargarTomasMedicamento();
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al cargar medicamento", exception);
            }
        });
    }
    
    private void mostrarInformacionMedicamento() {
        if (medicamento == null) {
            return;
        }
        
        // Información básica
        tvNombreMedicamento.setText(medicamento.getNombre());
        tvPresentacion.setText(medicamento.getPresentacion());
        ivIconoMedicamento.setImageResource(medicamento.getIconoPresentacion());
        
        // Afección
        if (medicamento.getAfeccion() != null && !medicamento.getAfeccion().isEmpty()) {
            tvAfeccion.setText(getString(R.string.condition_label, medicamento.getAfeccion()));
            tvAfeccion.setVisibility(View.VISIBLE);
        } else {
            tvAfeccion.setVisibility(View.GONE);
        }
        
        // Horarios
        List<String> horarios = medicamento.getHorariosTomas();
        if (horarios != null && !horarios.isEmpty()) {
            String horariosStr = String.join(", ", horarios);
            tvHorarios.setText(getString(R.string.schedules_label, horariosStr));
        } else {
            tvHorarios.setText(getString(R.string.schedules_label, getString(R.string.occasional_medicine)));
        }
        
        // Stock
        tvStock.setText(getString(R.string.stock_label, medicamento.getInfoStock()));
        
        // Estado
        tvEstado.setText(getString(R.string.status_label, medicamento.getEstadoTexto()));
        
        // Tipo de tratamiento
        if (medicamento.esCronico()) {
            tvTipoTratamiento.setText(getString(R.string.treatment_type_chronic));
        } else if (medicamento.getDiasTratamiento() > 0) {
            tvTipoTratamiento.setText(getString(R.string.treatment_type_programmed, 
                medicamento.getDiasTratamiento()));
        } else {
            tvTipoTratamiento.setText(getString(R.string.treatment_type_occasional));
        }
    }
    
    private void cargarTomasMedicamento() {
        if (medicamento == null || medicamento.getId() == null) {
            return;
        }
        
        firebaseService.obtenerTomasPorMedicamento(medicamento.getId(), 
            new FirebaseService.FirestoreListCallback() {
                @Override
                public void onSuccess(List<?> result) {
                    tomasMedicamento = result != null ? (List<Toma>) result : new ArrayList<>();
                    actualizarHistorial();
                    calcularYMostrarAdherencia();
                }
                
                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error al cargar tomas del medicamento", exception);
                    tomasMedicamento = new ArrayList<>();
                    actualizarHistorial();
                }
            });
    }
    
    private void actualizarHistorial() {
        if (tomasMedicamento == null || tomasMedicamento.isEmpty()) {
            tvEmptyHistorial.setVisibility(View.VISIBLE);
            rvHistorialTomas.setVisibility(View.GONE);
        } else {
            tvEmptyHistorial.setVisibility(View.GONE);
            rvHistorialTomas.setVisibility(View.VISIBLE);
            tomaAdapter.actualizarTomas(tomasMedicamento);
        }
    }
    
    private void calcularYMostrarAdherencia() {
        if (medicamento == null) {
            return;
        }
        
        // Calcular resumen de adherencia
        AdherenciaResumen resumen = AdherenciaCalculator.calcularResumenGeneral(
            medicamento, tomasMedicamento);
        
        int porcentaje = Math.round(resumen.getPorcentaje());
        tvResumenAdherencia.setText(getString(R.string.adherence_percentage, porcentaje));
        tvTomasRealizadas.setText(getString(R.string.takes_summary, 
            resumen.getTomasRealizadas(), resumen.getTomasEsperadas()));
        
        // Calcular y mostrar gráfico semanal
        List<AdherenciaIntervalo> datosSemanales = AdherenciaCalculator.calcularAdherenciaSemanal(
            medicamento, tomasMedicamento);
        
        actualizarChartSemanal(datosSemanales);
    }
    
    private void actualizarChartSemanal(List<AdherenciaIntervalo> datos) {
        if (datos == null || datos.isEmpty()) {
            chartAdherenciaSemanal.clear();
            chartAdherenciaSemanal.invalidate();
            return;
        }
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < datos.size(); i++) {
            AdherenciaIntervalo intervalo = datos.get(i);
            entries.add(new BarEntry(i, intervalo.getPorcentaje()));
            labels.add(intervalo.getEtiqueta());
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "% cumplimiento");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(10f);
        
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        
        chartAdherenciaSemanal.setData(data);
        chartAdherenciaSemanal.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartAdherenciaSemanal.invalidate();
    }
    
    /**
     * Crea un Intent para abrir esta Activity con un medicamento específico
     * @param context Contexto de la aplicación
     * @param medicamentoId ID del medicamento a mostrar
     * @return Intent configurado para abrir DetallesMedicamentoActivity
     */
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
     * Crea un Intent para abrir esta Activity con un medicamento específico
     * @param context Contexto de la aplicación
     * @param medicamentoId ID del medicamento a mostrar
     * @return Intent configurado para abrir DetallesMedicamentoActivity
     */
    public static Intent createIntent(android.content.Context context, String medicamentoId) {
        Intent intent = new Intent(context, DetallesMedicamentoActivity.class);
        intent.putExtra(EXTRA_MEDICAMENTO_ID, medicamentoId);
        return intent;
    }
}

