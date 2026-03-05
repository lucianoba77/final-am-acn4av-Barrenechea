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
import com.google.android.material.card.MaterialCardView;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import com.controlmedicamentos.myapplication.utils.Constants;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MedicamentoAdapter extends RecyclerView.Adapter<MedicamentoAdapter.MedicamentoViewHolder> {

    private Context context;
    private List<Medicamento> medicamentos;
    private OnMedicamentoClickListener listener;
    /** Mismo servicio que MainActivity para que el estado "tomada" se refleje al marcar una toma. */
    private TomaTrackingService tomaTrackingService;

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

    /** Usar el mismo TomaTrackingService que la actividad para que barras y botón reflejen tomada/omitida. */
    public void setTomaTrackingService(TomaTrackingService service) {
        this.tomaTrackingService = service;
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
        private TextView tvTomasHoy;
        private TextView tvProximaToma;
        private TextView tvVence;
        private LinearLayout llAvisoVencido;
        private LinearLayout llFilaBotones;
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
            tvTomasHoy = itemView.findViewById(R.id.tvTomasHoy);
            tvProximaToma = itemView.findViewById(R.id.tvProximaToma);
            tvVence = itemView.findViewById(R.id.tvVence);
            llAvisoVencido = itemView.findViewById(R.id.llAvisoVencido);
            llFilaBotones = itemView.findViewById(R.id.llFilaBotones);
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
            // Color de la card igual al asignado en el Botiquín
            if (itemView instanceof MaterialCardView) {
                ((MaterialCardView) itemView).setCardBackgroundColor(medicamento.getColor());
            }
            // Configurar información básica
            tvNombreMedicamento.setText(medicamento.getNombre());
            tvInfoMedicamento.setText(medicamento.getPresentacion() + " • " +
                    medicamento.getTomasDiarias() + " tomas diarias");
            tvStockInfo.setText(context.getString(R.string.label_stock_info, medicamento.getInfoStock()));

            // Próxima(s) toma(s) y tomas hoy: mostrar todos los horarios del día (ordenados)
            List<String> horarios = medicamento.getHorariosTomasHoy();
            if (horarios != null && !horarios.isEmpty()) {
                if (horarios.size() == 1) {
                    tvTomasHoy.setText(context.getString(R.string.medicine_one_dose_today));
                } else {
                    tvTomasHoy.setText(context.getString(R.string.medicine_doses_today, horarios.size()));
                }
                tvTomasHoy.setVisibility(View.VISIBLE);
                // Mostrar todos los horarios (ej. "Próxima: 06:20, 16:20")
                StringBuilder sb = new StringBuilder();
                sb.append(context.getString(R.string.medicine_next_dose)).append(" ");
                for (int i = 0; i < horarios.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(horarios.get(i));
                }
                tvProximaToma.setText(sb.toString());
                tvProximaToma.setVisibility(View.VISIBLE);
            } else {
                tvTomasHoy.setVisibility(View.GONE);
                tvProximaToma.setText(context.getString(R.string.medicine_no_scheduled));
                tvProximaToma.setVisibility(View.VISIBLE);
            }

            // Fecha de vencimiento (Vence: dd/MM/yyyy)
            if (medicamento.getFechaVencimiento() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvVence.setText(context.getString(R.string.medicine_vence, dateFormat.format(medicamento.getFechaVencimiento())));
                tvVence.setVisibility(View.VISIBLE);
            } else {
                tvVence.setVisibility(View.GONE);
            }

            // Configurar ícono
            ivIconoMedicamento.setImageResource(medicamento.getIconoPresentacion());

            // Usar el mismo TomaTrackingService que MainActivity para que "tomada" y botón se actualicen
            TomaTrackingService trackingService = tomaTrackingService != null ? tomaTrackingService : new TomaTrackingService(context);
            List<TomaProgramada> tomasProgramadas = trackingService.obtenerTomasMedicamento(medicamento.getId());
            if (tomasProgramadas == null || tomasProgramadas.isEmpty()) {
                trackingService.inicializarTomasDia(medicamento);
                trackingService.obtenerTomasMedicamento(medicamento.getId());
            }
            boolean tieneTomasPosponibles = trackingService.tieneTomasPosponibles(medicamento.getId());
            boolean mostrarOmitido = trackingService.tieneProximaOmitidaComoUnicaPendiente(medicamento.getId());
            boolean todasTomadas = trackingService.completoTodasLasTomasDelDia(medicamento.getId());

            configurarBarrasTomas(medicamento, trackingService);

            boolean estaVencido = medicamento.estaVencido();

            if (estaVencido) {
                // Medicamento vencido: mostrar aviso, ocultar botones Tomado y Posponer
                if (llAvisoVencido != null) {
                    llAvisoVencido.setVisibility(View.VISIBLE);
                }
                if (llFilaBotones != null) {
                    llFilaBotones.setVisibility(View.GONE);
                }
                btnTomado.setOnClickListener(null);
                btnTomado.setEnabled(false);
            } else {
                // No vencido: ocultar aviso, mostrar fila de botones
                if (llAvisoVencido != null) {
                    llAvisoVencido.setVisibility(View.GONE);
                }
                if (llFilaBotones != null) {
                    llFilaBotones.setVisibility(View.VISIBLE);
                }

                // Botón Posponer: visible 30 min antes hasta 1 h después de la toma
                if (btnPosponer != null) {
                    btnPosponer.setVisibility(tieneTomasPosponibles ? View.VISIBLE : View.GONE);
                    btnPosponer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) {
                                listener.onPosponerClick(medicamento);
                            }
                        }
                    });
                }

                // Botón principal: "Tomado" (verde activo), "Omitido" (gris desactivado) o Tomado desactivado (todas tomadas)
                if (todasTomadas) {
                    btnTomado.setText(R.string.medicine_taken);
                    btnTomado.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.button_omitido));
                    btnTomado.setEnabled(false);
                    btnTomado.setOnClickListener(null);
                } else if (mostrarOmitido) {
                    btnTomado.setText(R.string.medicine_omitted);
                    btnTomado.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.button_omitido));
                    btnTomado.setEnabled(false);
                    btnTomado.setOnClickListener(null);
                } else {
                    btnTomado.setText(R.string.medicine_taken);
                    btnTomado.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.success));
                    btnTomado.setEnabled(true);
                    btnTomado.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) {
                                listener.onTomadoClick(medicamento);
                            }
                        }
                    });
                }
            }

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
         * Configura una barra horizontal por cada hora de toma: cada fila muestra el horario (ej. 08:00) y la barra (verde/rojo/gris).
         */
        private void configurarBarrasTomas(Medicamento medicamento, TomaTrackingService trackingService) {
            llBarrasTomas.removeAllViews();
            llBarrasTomas.setOrientation(LinearLayout.VERTICAL);
            List<TomaProgramada> tomasProgramadas = trackingService.obtenerTomasMedicamento(medicamento.getId());
            if (tomasProgramadas == null) {
                tomasProgramadas = new java.util.ArrayList<>();
            }

            List<String> horarios = medicamento.getHorariosTomasHoy();
            int marginSmall = context.getResources().getDimensionPixelSize(R.dimen.margin_small);
            int barHeight = context.getResources().getDimensionPixelSize(R.dimen.progress_bar_height);
            for (int i = 0; i < horarios.size(); i++) {
                String horario = horarios.get(i);
                // Fila: [horario] [barra]
                LinearLayout fila = new LinearLayout(context);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams paramsFila = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                if (i > 0) {
                    paramsFila.topMargin = marginSmall;
                }
                fila.setLayoutParams(paramsFila);

                TextView tvHorario = new TextView(context);
                tvHorario.setText(horario);
                tvHorario.setTextSize(12);
                tvHorario.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
                LinearLayout.LayoutParams paramsHorario = new LinearLayout.LayoutParams(
                        context.getResources().getDimensionPixelSize(R.dimen.dashboard_horario_width),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                tvHorario.setLayoutParams(paramsHorario);

                ProgressBar barra = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
                LinearLayout.LayoutParams paramsBarra = new LinearLayout.LayoutParams(
                        0,
                        barHeight,
                        1.0f
                );
                paramsBarra.setMargins(marginSmall, 0, 0, 0);
                barra.setLayoutParams(paramsBarra);
                barra.setMax(100);
                barra.setProgress(100);

                TomaProgramada tomaParaBarra = null;
                for (TomaProgramada toma : tomasProgramadas) {
                    if (toma.getHorario().equals(horario)) {
                        tomaParaBarra = toma;
                        break;
                    }
                }
                int colorResId = obtenerColorEstadoToma(tomaParaBarra);
                barra.setProgressTintList(ContextCompat.getColorStateList(context, colorResId));

                fila.addView(tvHorario);
                fila.addView(barra);
                llBarrasTomas.addView(fila);
            }
        }

        /**
         * Color de la barra: verde si la toma fue marcada como tomada;
         * rojo solo si está omitida; gris (pendiente) en el resto.
         */
        private int obtenerColorEstadoToma(TomaProgramada toma) {
            if (toma == null) {
                return R.color.barra_pendiente;
            }
            if (toma.isTomada()) {
                return R.color.barra_tomada;
            }
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                return R.color.barra_omitida;
            }
            return R.color.barra_pendiente;
        }

        /**
         * Inicia el efecto de parpadeo para la barra
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
                        handler.postDelayed(this, Constants.INTERVALO_PARPADEO_MS);
                    }
                }
            };
            handler.post(runnable);
        }
    }
}
