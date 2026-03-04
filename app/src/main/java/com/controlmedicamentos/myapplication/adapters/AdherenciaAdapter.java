package com.controlmedicamentos.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.AdherenciaIntervalo;
import com.controlmedicamentos.myapplication.models.AdherenciaResumen;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.utils.AdherenciaCalculator;
import com.controlmedicamentos.myapplication.utils.EstadoAdherencia;
import java.util.List;

/**
 * Adapter para la lista "Adherencia por medicamento (Total)" (paridad con web).
 */
public class AdherenciaAdapter extends RecyclerView.Adapter<AdherenciaAdapter.AdherenciaViewHolder> {

    private final Context context;
    private List<Medicamento> medicamentos;
    private List<Toma> tomasUsuario;

    public AdherenciaAdapter(Context context, List<Medicamento> medicamentos, List<Toma> tomasUsuario) {
        this.context = context;
        this.medicamentos = medicamentos != null ? medicamentos : new java.util.ArrayList<>();
        this.tomasUsuario = tomasUsuario != null ? tomasUsuario : new java.util.ArrayList<>();
    }

    @NonNull
    @Override
    public AdherenciaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_adherencia_medicamento, parent, false);
        return new AdherenciaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdherenciaViewHolder holder, int position) {
        Medicamento med = medicamentos.get(position);
        List<Toma> tomasMed = AdherenciaCalculator.filtrarTomasPorMedicamento(tomasUsuario, med.getId());
        holder.bind(med, tomasMed);
    }

    @Override
    public int getItemCount() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    public void actualizarDatos(List<Medicamento> medicamentos, List<Toma> tomasUsuario) {
        this.medicamentos = medicamentos != null ? medicamentos : new java.util.ArrayList<>();
        this.tomasUsuario = tomasUsuario != null ? tomasUsuario : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    class AdherenciaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombreMedicamento;
        private final TextView tvBadgeCronico;
        private final TextView tvPorcentaje;
        private final ProgressBar progressAdherencia;
        private final TextView tvTotalTomas;
        private final TextView tvMensualSemanal;
        private final TextView tvMensajeEstado;

        AdherenciaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreMedicamento = itemView.findViewById(R.id.tvNombreMedicamento);
            tvBadgeCronico = itemView.findViewById(R.id.tvBadgeCronico);
            tvPorcentaje = itemView.findViewById(R.id.tvPorcentaje);
            progressAdherencia = itemView.findViewById(R.id.progressAdherencia);
            tvTotalTomas = itemView.findViewById(R.id.tvTotalTomas);
            tvMensualSemanal = itemView.findViewById(R.id.tvMensualSemanal);
            tvMensajeEstado = itemView.findViewById(R.id.tvMensajeEstado);
        }

        void bind(Medicamento medicamento, List<Toma> tomasMedicamento) {
            tvNombreMedicamento.setText(medicamento.getNombre());
            boolean cronico = medicamento.getDiasTratamiento() == -1;
            tvBadgeCronico.setVisibility(cronico ? View.VISIBLE : View.GONE);

            AdherenciaResumen resumen = AdherenciaCalculator.calcularResumenGeneral(medicamento, tomasMedicamento);
            float porcentaje = resumen.getPorcentaje();
            EstadoAdherencia estado = AdherenciaCalculator.obtenerEstadoAdherencia(porcentaje);

            tvPorcentaje.setText(String.format("%d%%", Math.round(porcentaje)));
            tvPorcentaje.setTextColor(estado.getColor());

            progressAdherencia.setProgress(Math.round(porcentaje));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                progressAdherencia.setProgressTintList(android.content.res.ColorStateList.valueOf(estado.getColor()));
            }

            tvTotalTomas.setText(context.getString(R.string.adhesion_total_tomas,
                resumen.getTomasRealizadas(),
                resumen.getTomasEsperadas(),
                resumen.getDiasSeguimiento()));

            List<AdherenciaIntervalo> semanal = AdherenciaCalculator.calcularAdherenciaSemanal(medicamento, tomasMedicamento);
            List<AdherenciaIntervalo> mensual = AdherenciaCalculator.calcularAdherenciaMensual(medicamento, tomasMedicamento);
            int semReal = 0, semEsp = 0, menReal = 0, menEsp = 0;
            for (AdherenciaIntervalo i : semanal) {
                semReal += i.getTomasRealizadas();
                semEsp += i.getTomasEsperadas();
            }
            for (AdherenciaIntervalo i : mensual) {
                menReal += i.getTomasRealizadas();
                menEsp += i.getTomasEsperadas();
            }
            int pctSem = semEsp == 0 ? 0 : Math.min(100, Math.round((semReal * 100f) / semEsp));
            int pctMen = menEsp == 0 ? 0 : Math.min(100, Math.round((menReal * 100f) / menEsp));
            tvMensualSemanal.setText(context.getString(R.string.adhesion_mensual_semanal,
                pctMen, menReal, menEsp, pctSem, semReal, semEsp));

            tvMensajeEstado.setText(estado.getMensaje());
            tvMensajeEstado.setTextColor(estado.getColor());
        }
    }
}
