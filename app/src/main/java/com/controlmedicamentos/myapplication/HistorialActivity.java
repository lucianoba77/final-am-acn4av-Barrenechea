package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.controlmedicamentos.myapplication.adapters.HistorialAdapter;
import com.controlmedicamentos.myapplication.models.AdherenciaIntervalo;
import com.controlmedicamentos.myapplication.models.AdherenciaResumen;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.AdherenciaCalculator;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private BarChart chartAdherencia;
    private RecyclerView rvTratamientosConcluidos;
    private RecyclerView rvMedicamentosOcasionales;
    private TextView tvEstadisticasGenerales;
    private TextView tvEmptyOcasionales;
    private MaterialButton btnVolver;
    // Botones de navegación
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavHistorial, btnNavAjustes;
    private HistorialAdapter adapter;
    private HistorialAdapter adapterOcasionales;
    private List<Medicamento> tratamientosConcluidos = new ArrayList<>();
    private List<Medicamento> medicamentosOcasionales = new ArrayList<>();
    private List<Medicamento> todosLosMedicamentos = new ArrayList<>();
    private List<Toma> tomasUsuario = new ArrayList<>();
    private AuthService authService;
    private FirebaseService firebaseService;

    // Historial completo de adherencia del paciente
    private TextView tvResumenAdherenciaGeneral;
    private BarChart chartAdherenciaGeneralSemanal;
    private BarChart chartAdherenciaGeneralMensual;

    // Plan de adherencia
    private TextInputLayout tilMedicamentosAdherencia;
    private AutoCompleteTextView actvMedicamentosAdherencia;
    private TextView tvResumenPlanAdherencia;
    private TextView tvEmptyPlanAdherencia;
    private LinearLayout layoutPlanCharts;
    private BarChart chartAdherenciaSemanal;
    private BarChart chartAdherenciaMensual;
    private View cardPlanAdherencia;
    private View cardMedicamentosOcasionales;
    private List<Medicamento> medicamentosPlan = new ArrayList<>();
    private ArrayAdapter<String> planAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar barra de estado para que sea visible
        configurarBarraEstado();
        
        // Ocultar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_historial);
        
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

        inicializarVistas();
        configurarGraficos();
        configurarRecyclerView();
        cargarDatos();
        configurarListeners();
        configurarNavegacion();
    }

    private void inicializarVistas() {
        chartAdherencia = findViewById(R.id.chartAdherencia);
        rvTratamientosConcluidos = findViewById(R.id.rvTratamientosConcluidos);
        rvMedicamentosOcasionales = findViewById(R.id.rvMedicamentosOcasionales);
        tvEstadisticasGenerales = findViewById(R.id.tvEstadisticasGenerales);
        tvEmptyOcasionales = findViewById(R.id.tvEmptyOcasionales);
        btnVolver = findViewById(R.id.btnVolver);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavHistorial = findViewById(R.id.btnNavHistorial);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);

        // Historial completo de adherencia
        tvResumenAdherenciaGeneral = findViewById(R.id.tvResumenAdherenciaGeneral);
        chartAdherenciaGeneralSemanal = findViewById(R.id.chartAdherenciaGeneralSemanal);
        chartAdherenciaGeneralMensual = findViewById(R.id.chartAdherenciaGeneralMensual);

        tilMedicamentosAdherencia = findViewById(R.id.tilMedicamentosAdherencia);
        actvMedicamentosAdherencia = findViewById(R.id.actvMedicamentosAdherencia);
        tvResumenPlanAdherencia = findViewById(R.id.tvResumenPlanAdherencia);
        tvEmptyPlanAdherencia = findViewById(R.id.tvEmptyPlanAdherencia);
        layoutPlanCharts = findViewById(R.id.layoutPlanCharts);
        chartAdherenciaSemanal = findViewById(R.id.chartAdherenciaSemanal);
        chartAdherenciaMensual = findViewById(R.id.chartAdherenciaMensual);
        cardPlanAdherencia = findViewById(R.id.cardPlanAdherencia);
        cardMedicamentosOcasionales = findViewById(R.id.cardMedicamentosOcasionales);
    }

    private void configurarGraficos() {
        configurarBarChart(chartAdherencia);
        configurarBarChart(chartAdherenciaSemanal);
        configurarBarChart(chartAdherenciaMensual);
        configurarBarChart(chartAdherenciaGeneralSemanal);
        configurarBarChart(chartAdherenciaGeneralMensual);
    }

    private void configurarBarChart(BarChart chart) {
        if (chart == null) {
            return;
        }
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setNoDataText(getString(R.string.adherence_plan_empty));
        chart.setScaleEnabled(false);
        chart.getAxisLeft().setAxisMaximum(100f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        chart.getLegend().setEnabled(false);
        chart.animateY(800);
    }

    private void configurarRecyclerView() {
        adapter = new HistorialAdapter(this, tratamientosConcluidos);
        rvTratamientosConcluidos.setLayoutManager(new LinearLayoutManager(this));
        rvTratamientosConcluidos.setAdapter(adapter);
        
        adapterOcasionales = new HistorialAdapter(this, medicamentosOcasionales);
        rvMedicamentosOcasionales.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentosOcasionales.setAdapter(adapterOcasionales);
    }

    private void cargarDatos() {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            tvEstadisticasGenerales.setText("No hay conexión a internet");
            return;
        }

        // Cargar todos los medicamentos desde Firebase
        firebaseService.obtenerMedicamentos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                todosLosMedicamentos = result != null
                    ? (List<Medicamento>) result
                    : new ArrayList<>();
                cargarTomasUsuario();
            }

            @Override
            public void onError(Exception exception) {
                tvEstadisticasGenerales.setText("Error al cargar datos");
            }
        });
    }

    private void cargarTomasUsuario() {
        firebaseService.obtenerTomasUsuario(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                tomasUsuario = result != null ? (List<Toma>) result : new ArrayList<>();
                // Procesar información incluso si no hay tomas
                procesarInformacion();
            }

            @Override
            public void onError(Exception exception) {
                android.util.Log.e("HistorialActivity", "Error al obtener tomas del usuario", exception);
                // Continuar procesando con lista vacía de tomas
                tomasUsuario = new ArrayList<>();
                procesarInformacion();
                // Mostrar mensaje informativo en lugar de error
                if (todosLosMedicamentos.isEmpty()) {
                    tvEstadisticasGenerales.setText("No hay medicamentos registrados");
                } else {
                    tvEstadisticasGenerales.setText("No se pudieron cargar las tomas. Mostrando medicamentos disponibles.");
                }
            }
        });
    }

    private void procesarInformacion() {
        if (todosLosMedicamentos.isEmpty()) {
            tvEstadisticasGenerales.setText("No hay medicamentos registrados");
            adapter.actualizarMedicamentos(new ArrayList<>());
            return;
        }

        int totalMedicamentos = todosLosMedicamentos.size();
        int medicamentosActivos = 0;
        int medicamentosPausados = 0;

        for (Medicamento medicamento : todosLosMedicamentos) {
            if (medicamento.isActivo() && !medicamento.isPausado()) {
                medicamentosActivos++;
            }
            if (medicamento.isPausado()) {
                medicamentosPausados++;
            }
        }

        tvEstadisticasGenerales.setText(String.format(
            Locale.getDefault(),
            "Medicamentos Activos: %d\nMedicamentos Pausados: %d\nTotal Medicamentos: %d",
            medicamentosActivos,
            medicamentosPausados,
            totalMedicamentos
        ));

        List<AdherenciaResumen> resumenes = new ArrayList<>();
        tratamientosConcluidos = new ArrayList<>();
        medicamentosOcasionales = new ArrayList<>();

        for (Medicamento medicamento : todosLosMedicamentos) {
            List<Toma> tomasMedicamento = AdherenciaCalculator.filtrarTomasPorMedicamento(
                tomasUsuario,
                medicamento.getId());
            boolean esOcasional = medicamento.getTomasDiarias() == 0;
            
            // Separar medicamentos ocasionales con tomas
            if (esOcasional) {
                if (!tomasMedicamento.isEmpty()) {
                    // Medicamento ocasional que fue consumido - agregar a lista separada
                    medicamentosOcasionales.add(medicamento);
                }
                // No incluir en adherencia ni en tratamientos concluidos
                continue;
            }

            // Calcular resumen incluso si no hay tomas (mostrará 0% de adherencia)
            resumenes.add(AdherenciaCalculator.calcularResumenGeneral(medicamento, tomasMedicamento));

            // Incluir medicamentos pausados o finalizados en tratamientos concluidos
            if (medicamento.isPausado() || !medicamento.isActivo()) {
                tratamientosConcluidos.add(medicamento);
            }
        }
        
        // Si no hay tratamientos concluidos pero hay medicamentos activos, mostrar todos (excepto ocasionales)
        if (tratamientosConcluidos.isEmpty() && !todosLosMedicamentos.isEmpty()) {
            for (Medicamento medicamento : todosLosMedicamentos) {
                if (medicamento.getTomasDiarias() > 0) {
                    tratamientosConcluidos.add(medicamento);
                }
            }
        }

        adapter.actualizarMedicamentos(tratamientosConcluidos);
        adapterOcasionales.actualizarMedicamentos(medicamentosOcasionales);
        
        // Mostrar/ocultar card de medicamentos ocasionales
        if (cardMedicamentosOcasionales != null) {
            cardMedicamentosOcasionales.setVisibility(
                medicamentosOcasionales.isEmpty() ? View.GONE : View.VISIBLE);
        }
        
        // Mostrar/ocultar mensaje vacío para ocasionales
        if (tvEmptyOcasionales != null) {
            tvEmptyOcasionales.setVisibility(
                medicamentosOcasionales.isEmpty() ? View.VISIBLE : View.GONE);
        }
        
        cargarGraficoAdherencia(resumenes);
        cargarHistorialCompletoAdherencia();
        configurarPlanAdherencia();
    }

    private void cargarGraficoAdherencia(List<AdherenciaResumen> resumenes) {
        if (resumenes == null || resumenes.isEmpty()) {
            chartAdherencia.clear();
            chartAdherencia.invalidate();
            return;
        }

        Collections.sort(resumenes, Comparator.comparing(AdherenciaResumen::getPorcentaje).reversed());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int limite = Math.min(5, resumenes.size());
        for (int i = 0; i < limite; i++) {
            AdherenciaResumen resumen = resumenes.get(i);
            entries.add(new BarEntry(i, resumen.getPorcentaje()));
            labels.add(resumen.getMedicamentoNombre());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Adherencia (%)");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chartAdherencia.setData(barData);
        chartAdherencia.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartAdherencia.invalidate();
    }

    /**
     * Carga y muestra el historial completo de adherencia del paciente
     */
    private void cargarHistorialCompletoAdherencia() {
        if (todosLosMedicamentos.isEmpty()) {
            tvResumenAdherenciaGeneral.setText("No hay medicamentos registrados");
            return;
        }
        
        // Mostrar datos incluso si no hay tomas (mostrará 0% de adherencia)
        if (tomasUsuario.isEmpty()) {
            tvResumenAdherenciaGeneral.setText("No hay tomas registradas. Adherencia: 0%");
            // Limpiar gráficos
            if (chartAdherenciaGeneralSemanal != null) {
                chartAdherenciaGeneralSemanal.clear();
                chartAdherenciaGeneralSemanal.invalidate();
            }
            if (chartAdherenciaGeneralMensual != null) {
                chartAdherenciaGeneralMensual.clear();
                chartAdherenciaGeneralMensual.invalidate();
            }
            return;
        }

        // Calcular adherencia general del paciente
        AdherenciaResumen resumenGeneral = AdherenciaCalculator.calcularAdherenciaGeneralPaciente(
            todosLosMedicamentos, tomasUsuario);

        int porcentaje = Math.round(resumenGeneral.getPorcentaje());
        tvResumenAdherenciaGeneral.setText(getString(
            R.string.patient_adherence_summary,
            porcentaje,
            resumenGeneral.getTomasRealizadas(),
            resumenGeneral.getTomasEsperadas()
        ));

        // Calcular y mostrar gráficos de adherencia general
        List<AdherenciaIntervalo> datosSemanales = AdherenciaCalculator.calcularAdherenciaGeneralSemanal(
            todosLosMedicamentos, tomasUsuario);
        List<AdherenciaIntervalo> datosMensuales = AdherenciaCalculator.calcularAdherenciaGeneralMensual(
            todosLosMedicamentos, tomasUsuario);

        actualizarChartIntervalos(chartAdherenciaGeneralSemanal, datosSemanales);
        actualizarChartIntervalos(chartAdherenciaGeneralMensual, datosMensuales);
    }

    private void configurarPlanAdherencia() {
        medicamentosPlan.clear();

        for (Medicamento medicamento : todosLosMedicamentos) {
            if (medicamento.getTomasDiarias() > 0 && medicamento.isActivo()) {
                medicamentosPlan.add(medicamento);
            }
        }

        if (medicamentosPlan.isEmpty()) {
            cardPlanAdherencia.setVisibility(View.GONE);
            return;
        }

        cardPlanAdherencia.setVisibility(View.VISIBLE);
        List<String> nombres = new ArrayList<>();
        for (Medicamento medicamento : medicamentosPlan) {
            nombres.add(medicamento.getNombre());
        }

        planAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres);
        actvMedicamentosAdherencia.setAdapter(planAdapter);
        actvMedicamentosAdherencia.setText("");
        actvMedicamentosAdherencia.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < medicamentosPlan.size()) {
                actualizarPlanAdherencia(medicamentosPlan.get(position));
            }
        });

        // Seleccionar el primer medicamento por defecto
        if (!medicamentosPlan.isEmpty()) {
            actvMedicamentosAdherencia.setText(medicamentosPlan.get(0).getNombre(), false);
            actualizarPlanAdherencia(medicamentosPlan.get(0));
        }
    }

    private void actualizarPlanAdherencia(Medicamento medicamento) {
        if (medicamento == null) {
            tvResumenPlanAdherencia.setText(getString(R.string.adherence_plan_summary_placeholder));
            tvEmptyPlanAdherencia.setVisibility(View.VISIBLE);
            layoutPlanCharts.setVisibility(View.GONE);
            return;
        }

        List<Toma> tomasMedicamento = AdherenciaCalculator.filtrarTomasPorMedicamento(
            tomasUsuario,
            medicamento.getId());

        AdherenciaResumen resumen = AdherenciaCalculator.calcularResumenGeneral(medicamento, tomasMedicamento);
        int porcentaje = Math.round(resumen.getPorcentaje());
        tvResumenPlanAdherencia.setText(getString(
            R.string.adherence_plan_summary,
            porcentaje,
            resumen.getTomasRealizadas(),
            resumen.getTomasEsperadas()
        ));

        List<AdherenciaIntervalo> datosSemanales = AdherenciaCalculator.calcularAdherenciaSemanal(medicamento, tomasMedicamento);
        List<AdherenciaIntervalo> datosMensuales = AdherenciaCalculator.calcularAdherenciaMensual(medicamento, tomasMedicamento);

        boolean sinDatos = datosSemanales.isEmpty() && datosMensuales.isEmpty();
        tvEmptyPlanAdherencia.setVisibility(sinDatos ? View.VISIBLE : View.GONE);
        layoutPlanCharts.setVisibility(sinDatos ? View.GONE : View.VISIBLE);

        actualizarChartIntervalos(chartAdherenciaSemanal, datosSemanales);
        actualizarChartIntervalos(chartAdherenciaMensual, datosMensuales);
    }

    private void actualizarChartIntervalos(BarChart chart, List<AdherenciaIntervalo> datos) {
        if (chart == null) return;

        if (datos == null || datos.isEmpty()) {
            chart.clear();
            chart.invalidate();
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

        chart.setData(data);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.invalidate();
    }

    private void configurarListeners() {
        if (btnVolver != null) {
            btnVolver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
    
    private void configurarNavegacion() {
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavNuevaMedicina != null) {
            btnNavNuevaMedicina.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, NuevaMedicinaActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnNavBotiquin != null) {
            btnNavBotiquin.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, BotiquinActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavHistorial != null) {
            btnNavHistorial.setOnClickListener(v -> {
                // Ya estamos en historial, no hacer nada
            });
        }
        
        if (btnNavAjustes != null) {
            btnNavAjustes.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, AjustesActivity.class);
                startActivity(intent);
                finish();
            });
        }
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
        cargarDatos(); // Recargar datos al volver
    }
}