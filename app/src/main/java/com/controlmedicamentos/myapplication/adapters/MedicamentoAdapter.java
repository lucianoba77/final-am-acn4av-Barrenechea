package com.controlmedicamentos.myapplication.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import java.util.List;

public class MedicamentoAdapter extends RecyclerView.Adapter<MedicamentoAdapter.MedicamentoViewHolder> {

    private Context context;
    private List<Medicamento> medicamentos;
    private OnMedicamentoClickListener listener;

    // Interface para manejar clicks
    public interface OnMedicamentoClickListener {
        void onTomadoClick(Medicamento medicamento);
        void onMedicamentoClick(Medicamento medicamento);
        void onPosponerClick(Medicamento medicamento);
    }

    public MedicamentoAdapter(Context context, List<Medicamento> medicamentos) {
        this.context = context;
        this.medicamentos = medicamentos;
    }

    public void setOnMedicamentoClickListener(OnMedicamentoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicamento, parent, false);
        return new MedicamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicamentoViewHolder holder, int position) {
        Medicamento medicamento = medicamentos.get(position);
        holder.bind(medicamento);
    }

    @Override
    public int getItemCount() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    @Override
    public void onViewRecycled(@NonNull MedicamentoViewHolder holder) {
        super.onViewRecycled(holder);
        // Limpiar recursos del ViewHolder cuando se recicla
        holder.onViewRecycled();
    }

    public void actualizarMedicamentos(List<Medicamento> nuevosMedicamentos) {
        this.medicamentos = nuevosMedicamentos;
        notifyDataSetChanged();
    }

    class MedicamentoViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivIconoMedicamento;
        private TextView tvNombreMedicamento;
        private TextView tvInfoMedicamento;
        private LinearLayout llBarrasTomas;
        private TextView tvStockInfo;
        private MaterialButton btnTomado;
        private MaterialButton btnPosponer;
        private Handler handler; // Handler para animaciones, se limpia en onViewRecycled

        public MedicamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIconoMedicamento = itemView.findViewById(R.id.ivIconoMedicamento);
            tvNombreMedicamento = itemView.findViewById(R.id.tvNombreMedicamento);
            tvInfoMedicamento = itemView.findViewById(R.id.tvInfoMedicamento);
            llBarrasTomas = itemView.findViewById(R.id.llBarrasTomas);
            tvStockInfo = itemView.findViewById(R.id.tvStockInfo);
            btnTomado = itemView.findViewById(R.id.btnTomado);
            btnPosponer = itemView.findViewById(R.id.btnPosponer);
            // Inicializar Handler con Looper explícito para evitar memory leaks
            handler = new Handler(Looper.getMainLooper());
        }
        
        /**
         * Limpia recursos cuando el ViewHolder se recicla
         */
        public void onViewRecycled() {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        }

        public void bind(Medicamento medicamento) {
            // Configurar información básica
            tvNombreMedicamento.setText(medicamento.getNombre());
            tvInfoMedicamento.setText(medicamento.getPresentacion() + " • " +
                    medicamento.getTomasDiarias() + " tomas diarias");
            tvStockInfo.setText("Stock: " + medicamento.getInfoStock());

            // Configurar ícono
            ivIconoMedicamento.setImageResource(medicamento.getIconoPresentacion());

            // Configurar barras de progreso
            boolean tieneTomasEnAlerta = configurarBarrasTomas(medicamento);

            // Mostrar/ocultar botón de posponer según el estado de las tomas
            if (btnPosponer != null) {
                btnPosponer.setVisibility(tieneTomasEnAlerta ? View.VISIBLE : View.GONE);
                btnPosponer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onPosponerClick(medicamento);
                        }
                    }
                });
            }

            // Configurar botón Tomado
            btnTomado.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onTomadoClick(medicamento);
                    }
                }
            });

            // Configurar click en el item completo
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onMedicamentoClick(medicamento);
                    }
                }
            });
        }

        /**
         * Configura las barras de tomas y retorna true si hay tomas en estado de alerta
         */
        private boolean configurarBarrasTomas(Medicamento medicamento) {
            // Limpiar barras existentes
            llBarrasTomas.removeAllViews();

            // Obtener estado de las tomas programadas
            // Nota: No inicializamos aquí para evitar reinicializaciones innecesarias.
            // La inicialización se hace en MedicamentoDataManager cuando se cargan los datos.
            // Si las tomas no existen, se inicializarán automáticamente cuando sea necesario.
            TomaTrackingService trackingService = new TomaTrackingService(context);
            // Solo inicializar si no hay tomas para este medicamento (optimización)
            List<TomaProgramada> tomasProgramadas = trackingService.obtenerTomasMedicamento(medicamento.getId());
            if (tomasProgramadas == null || tomasProgramadas.isEmpty()) {
                trackingService.inicializarTomasDia(medicamento);
                tomasProgramadas = trackingService.obtenerTomasMedicamento(medicamento.getId());
            }

            // Crear barras según tomas diarias
            int tomasDiarias = medicamento.getTomasDiarias();
            List<String> horarios = medicamento.getHorariosTomas();
            boolean tieneTomasEnAlerta = false;
            
            for (int i = 0; i < tomasDiarias && i < horarios.size(); i++) {
                String horario = horarios.get(i);
                ProgressBar barra = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
                barra.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        context.getResources().getDimensionPixelSize(R.dimen.progress_bar_height),
                        1.0f
                ));
                barra.setMax(100);
                barra.setProgress(100); // Llenar la barra para mostrar el color

                // Obtener estado de esta toma específica
                TomaProgramada.EstadoTomaProgramada estado = TomaProgramada.EstadoTomaProgramada.PENDIENTE;
                for (TomaProgramada toma : tomasProgramadas) {
                    if (toma.getHorario().equals(horario)) {
                        estado = toma.getEstado();
                        break;
                    }
                }

                // Configurar color según el estado
                int colorResId = obtenerColorEstado(estado);
                barra.setProgressTintList(ContextCompat.getColorStateList(context, colorResId));

                // Si está en estado ALERTA_ROJA, hacer parpadear
                if (estado == TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA) {
                    iniciarParpadeo(barra);
                }
                
                // Verificar si hay tomas en alerta (ALERTA_ROJA o RETRASO)
                if (estado == TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA || 
                    estado == TomaProgramada.EstadoTomaProgramada.RETRASO) {
                    tieneTomasEnAlerta = true;
                }

                // Agregar margen entre barras
                if (i > 0) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) barra.getLayoutParams();
                    params.setMargins(context.getResources().getDimensionPixelSize(R.dimen.margin_small), 0, 0, 0);
                    barra.setLayoutParams(params);
                }

                llBarrasTomas.addView(barra);
            }
            
            return tieneTomasEnAlerta;
        }

        /**
         * Obtiene el color correspondiente al estado de la toma
         */
        private int obtenerColorEstado(TomaProgramada.EstadoTomaProgramada estado) {
            switch (estado) {
                case PENDIENTE:
                    return R.color.barra_pendiente;
                case ALERTA_AMARILLA:
                    return R.color.barra_alerta_amarilla;
                case ALERTA_ROJA:
                case RETRASO:
                    return R.color.barra_alerta_roja;
                case OMITIDA:
                    return R.color.barra_omitida;
                default:
                    return R.color.barra_pendiente;
            }
        }

        /**
         * Inicia el efecto de parpadeo para la barra roja
         * Usa el Handler del ViewHolder para evitar memory leaks
         */
        private void iniciarParpadeo(ProgressBar barra) {
            if (handler == null || barra == null) {
                return;
            }
            
            // Limpiar cualquier callback previo para esta barra
            handler.removeCallbacksAndMessages(null);
            
            Runnable runnable = new Runnable() {
                boolean visible = true;
                @Override
                public void run() {
                    // Verificar que la barra aún existe y es visible
                    if (barra != null && barra.getVisibility() == View.VISIBLE && handler != null) {
                        barra.setAlpha(visible ? 1.0f : 0.3f);
                        visible = !visible;
                        handler.postDelayed(this, 500); // Parpadear cada 500ms
                    }
                }
            };
            handler.post(runnable);
        }
    }
}
