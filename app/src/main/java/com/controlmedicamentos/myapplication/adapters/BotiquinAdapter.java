package com.controlmedicamentos.myapplication.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BotiquinAdapter extends RecyclerView.Adapter<BotiquinAdapter.BotiquinViewHolder> {

    private Context context;
    private List<Medicamento> medicamentos;
    private OnMedicamentoClickListener listener;

    public interface OnMedicamentoClickListener {
        void onEditarClick(Medicamento medicamento);
        void onEliminarClick(Medicamento medicamento);
        void onTomeUnaClick(Medicamento medicamento); // Nuevo método para "Tomé una"
    }

    public BotiquinAdapter(Context context, List<Medicamento> medicamentos) {
        this.context = context;
        this.medicamentos = medicamentos != null ? medicamentos : new java.util.ArrayList<>();
    }

    public void setOnMedicamentoClickListener(OnMedicamentoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BotiquinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicamento_botiquin, parent, false);
        return new BotiquinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BotiquinViewHolder holder, int position) {
        android.util.Log.d("BotiquinAdapter", "onBindViewHolder: position=" + position + ", medicamentos.size()=" + 
            (medicamentos != null ? medicamentos.size() : 0));
        if (medicamentos != null && position < medicamentos.size()) {
            Medicamento medicamento = medicamentos.get(position);
            android.util.Log.d("BotiquinAdapter", "onBindViewHolder: Binding medicamento '" + medicamento.getNombre() + 
                "' (ID: " + medicamento.getId() + ") en posición " + position);
            holder.bind(medicamento);
        } else {
            android.util.Log.w("BotiquinAdapter", "onBindViewHolder: ⚠️ Posición fuera de rango - position=" + position + 
                ", medicamentos.size()=" + (medicamentos != null ? medicamentos.size() : 0));
        }
    }

    @Override
    public int getItemCount() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    public void actualizarMedicamentos(List<Medicamento> nuevosMedicamentos) {
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: ========== INICIANDO ACTUALIZACIÓN ==========");
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: nuevosMedicamentos != null: " + (nuevosMedicamentos != null));
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: nuevosMedicamentos.size(): " + 
            (nuevosMedicamentos != null ? nuevosMedicamentos.size() : 0));
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: medicamentos actuales.size(): " + 
            (this.medicamentos != null ? this.medicamentos.size() : 0));
        
        this.medicamentos = nuevosMedicamentos != null ? nuevosMedicamentos : new java.util.ArrayList<>();
        
        // Log detallado de cada medicamento
        for (int i = 0; i < this.medicamentos.size(); i++) {
            Medicamento m = this.medicamentos.get(i);
            android.util.Log.d("BotiquinAdapter", String.format("actualizarMedicamentos: [%d] %s (ID: %s, TomasDiarias: %d, StockActual: %d)", 
                i, m.getNombre(), m.getId(), m.getTomasDiarias(), m.getStockActual()));
        }
        
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: Llamando notifyDataSetChanged()...");
        notifyDataSetChanged();
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: notifyDataSetChanged() completado. getItemCount()=" + getItemCount());
        android.util.Log.d("BotiquinAdapter", "actualizarMedicamentos: ========== ACTUALIZACIÓN COMPLETADA ==========");
    }

    class BotiquinViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardMedicamento;
        private ImageView ivIcono;
        private TextView tvNombre;
        private TextView tvPresentacion;
        private TextView tvStock;
        private TextView tvEstado;
        private TextView tvFechaVencimiento;
        private MaterialButton btnTomeUna;
        private MaterialButton btnEditar;
        private MaterialButton btnEliminar;

        public BotiquinViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMedicamento = itemView.findViewById(R.id.cardMedicamento);
            ivIcono = itemView.findViewById(R.id.ivIcono);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPresentacion = itemView.findViewById(R.id.tvPresentacion);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvFechaVencimiento = itemView.findViewById(R.id.tvFechaVencimiento);
            btnTomeUna = itemView.findViewById(R.id.btnTomeUna);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }

        public void bind(Medicamento medicamento) {
            // Configurar ícono del tipo de presentación
            ivIcono.setImageResource(medicamento.getIconoPresentacion());

            // Configurar nombre
            tvNombre.setText(medicamento.getNombre());

            // Configurar presentación
            tvPresentacion.setText(medicamento.getPresentacion());

            // Configurar stock (mostrar siempre, incluso si es 0)
            if (!medicamento.estaVencido()) {
                String stockText = "Stock: " + medicamento.getStockActual();
                if (medicamento.getStockInicial() > 0) {
                    stockText += "/" + medicamento.getStockInicial();
                }
                tvStock.setText(stockText);
                tvStock.setVisibility(TextView.VISIBLE);
                // Si no hay stock, mostrar en color de advertencia
                if (medicamento.getStockActual() == 0) {
                    tvStock.setTextColor(context.getColor(R.color.warning));
                } else {
                    tvStock.setTextColor(context.getColor(R.color.black));
                }
            } else {
                tvStock.setVisibility(TextView.GONE);
            }

            // Configurar estado y fecha de vencimiento
            boolean estaVencido = medicamento.estaVencido();
            boolean pausado = medicamento.isPausado();
            boolean activo = medicamento.isActivo();
            
            // Log para debugging
            android.util.Log.d("BotiquinAdapter", String.format("bind: '%s' - Vencido: %s, Pausado: %s, Activo: %s, Stock: %d, TomasDiarias: %d",
                medicamento.getNombre(), estaVencido, pausado, activo, medicamento.getStockActual(), medicamento.getTomasDiarias()));
            
            if (estaVencido) {
                tvEstado.setText("Vencido");
                tvEstado.setTextColor(context.getColor(R.color.error));
                tvFechaVencimiento.setVisibility(TextView.GONE);
                // Si está vencido, solo mostrar botón Eliminar
                btnTomeUna.setVisibility(View.GONE);
                btnEditar.setVisibility(View.VISIBLE);
                btnEliminar.setVisibility(View.VISIBLE);
            } else {
                // Si no está vencido, mostrar estado según pausado, activo y stock
                // PRIORIDAD: Pausado > Inactivo > Sin Stock > Activo
                String estadoTexto = "";
                int colorEstado;
                
                if (pausado) {
                    // Si está pausado, mostrar "Pausado" independientemente de activo
                    estadoTexto = "Pausado";
                    colorEstado = context.getColor(R.color.info); // Color azul para pausado
                } else if (!activo) {
                    // Si no está activo y no está pausado, mostrar "Inactivo"
                    estadoTexto = "Inactivo";
                    colorEstado = context.getColor(R.color.secondary_text); // Color gris para inactivo
                } else if (medicamento.getStockActual() <= 0) {
                    // Si está activo, no pausado, pero sin stock
                    estadoTexto = "Sin Stock";
                    colorEstado = context.getColor(R.color.warning);
                } else {
                    // Si está activo, no pausado, y con stock
                    estadoTexto = "Activo";
                    colorEstado = context.getColor(R.color.success);
                }
                
                tvEstado.setText(estadoTexto);
                tvEstado.setTextColor(colorEstado);
                android.util.Log.d("BotiquinAdapter", String.format("bind: '%s' - Estado mostrado: '%s'", medicamento.getNombre(), estadoTexto));
                
                // Mostrar fecha de vencimiento si existe
                if (medicamento.getFechaVencimiento() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String fechaVencimiento = dateFormat.format(medicamento.getFechaVencimiento());
                    tvFechaVencimiento.setText("Vence: " + fechaVencimiento);
                    tvFechaVencimiento.setVisibility(TextView.VISIBLE);
                } else {
                    tvFechaVencimiento.setVisibility(TextView.GONE);
                }
                
                // Mostrar botón "Tomé una" solo para medicamentos ocasionales con stock > 0, activos y no pausados
                if (medicamento.getTomasDiarias() == 0 && medicamento.getStockActual() > 0 && activo && !pausado) {
                    btnTomeUna.setVisibility(View.VISIBLE);
                } else {
                    btnTomeUna.setVisibility(View.GONE);
                }
                
                btnEditar.setVisibility(View.VISIBLE);
                btnEliminar.setVisibility(View.VISIBLE);
            }

            // Configurar color de fondo
            cardMedicamento.setCardBackgroundColor(medicamento.getColor());

            // Mejorar contraste de texto según el color de fondo
            // Usar texto más oscuro para mejor legibilidad
            int textColorDark = context.getColor(android.R.color.black);
            int textColorMedium = 0xFF2C2C2C; // Color oscuro para mejor contraste
            
            // Aplicar colores mejorados a los TextViews
            tvNombre.setTextColor(textColorDark);
            tvPresentacion.setTextColor(textColorMedium);
            tvStock.setTextColor(textColorMedium);
            tvFechaVencimiento.setTextColor(textColorMedium);
            
            // El estado ya tiene su color específico (verde para Activo, rojo para Vencido, etc.)

            // Configurar listeners
            btnTomeUna.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTomeUnaClick(medicamento);
                }
            });

            btnEditar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditarClick(medicamento);
                }
            });

            btnEliminar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEliminarClick(medicamento);
                }
            });
        }
    }
}
