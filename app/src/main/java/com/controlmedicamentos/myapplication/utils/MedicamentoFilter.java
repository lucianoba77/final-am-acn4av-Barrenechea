package com.controlmedicamentos.myapplication.utils;

import android.util.Log;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Utilidad para filtrar medicamentos según criterios del dashboard.
 * Centraliza la lógica de filtrado para evitar duplicación de código.
 */
public class MedicamentoFilter {
    private static final String TAG = "MedicamentoFilter";

    /**
     * Filtra medicamentos para mostrar en el dashboard.
     * Incluye todos los medicamentos activos que tienen tomas programadas para el día actual,
     * sin importar si ya se tomaron todas las tomas o se saltaron algunas.
     * 
     * @param medicamentos Lista completa de medicamentos
     * @param trackingService Servicio de tracking de tomas
     * @return Lista filtrada de medicamentos para el dashboard
     */
    public static List<Medicamento> filtrarParaDashboard(
            List<Medicamento> medicamentos,
            TomaTrackingService trackingService) {
        
        if (medicamentos == null || medicamentos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Medicamento> resultado = new ArrayList<>();
        Calendar ahora = Calendar.getInstance();

        Logger.d(TAG, "Filtrando medicamentos. Hora actual: " + ahora.get(Calendar.HOUR_OF_DAY) + ":" + ahora.get(Calendar.MINUTE));
        Logger.d(TAG, "MedicamentoFilter: Total medicamentos recibidos para filtrar: " + medicamentos.size());
        Logger.d(TAG, "MedicamentoFilter: ========== INICIANDO FILTRADO PARA DASHBOARD ==========");

        for (Medicamento med : medicamentos) {
            // Verificar condiciones básicas
            if (!cumpleCondicionesBasicas(med)) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: no cumple condiciones básicas");
                continue;
            }

            // Obtener tomas del medicamento
            List<TomaProgramada> tomasMedicamento = trackingService.obtenerTomasMedicamento(med.getId());
            Logger.d(TAG, "Medicamento " + med.getNombre() + ": " + 
                      (tomasMedicamento != null ? tomasMedicamento.size() : 0) + " tomas obtenidas");

            // Si no hay tomas inicializadas, el medicamento es nuevo y debe aparecer
            if (tomasMedicamento == null || tomasMedicamento.isEmpty()) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " sin tomas inicializadas, agregando al dashboard");
                resultado.add(med);
                continue;
            }

            // Verificar si tiene tomas programadas para el día actual (sin importar si ya se tomaron o se saltaron)
            if (tieneTomasProgramadasParaHoy(tomasMedicamento, ahora)) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " agregado al dashboard (tiene tomas programadas para hoy)");
                resultado.add(med);
            } else {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: no tiene tomas programadas para el día actual");
            }
        }

        Logger.d(TAG, "Filtrado completado: " + resultado.size() + " medicamentos para mostrar en dashboard");
        Logger.d(TAG, "MedicamentoFilter: ========== MEDICAMENTOS QUE PASARON EL FILTRO ==========");
        for (int i = 0; i < resultado.size(); i++) {
            Medicamento m = resultado.get(i);
            Logger.d(TAG, String.format("MedicamentoFilter: [%d] %s (ID: %s, TomasDiarias: %d, StockActual: %d)", 
                i, m.getNombre(), m.getId(), m.getTomasDiarias(), m.getStockActual()));
        }
        Logger.d(TAG, "MedicamentoFilter: ======================================================");
        return resultado;
    }

    /**
     * Verifica si un medicamento cumple las condiciones básicas para aparecer en el dashboard.
     */
    private static boolean cumpleCondicionesBasicas(Medicamento med) {
        if (!med.isActivo()) {
            Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: no está activo");
            return false;
        }

        if (med.getTomasDiarias() <= 0) {
            Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: tomasDiarias <= 0");
            return false;
        }

        if (med.getHorarioPrimeraToma() == null || med.getHorarioPrimeraToma().isEmpty()) {
            Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: horarioPrimeraToma vacío");
            return false;
        }

        if (med.getHorarioPrimeraToma().equals(Constants.HORARIO_INVALIDO)) {
            Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: horarioPrimeraToma es 00:00");
            return false;
        }

        return true;
    }

    /**
     * Verifica si un medicamento tiene tomas programadas para el día actual.
     * No importa si ya se tomaron todas las tomas o se saltaron algunas.
     * 
     * @param tomasMedicamento Lista de tomas programadas del medicamento
     * @param ahora Fecha/hora actual para comparar
     * @return true si tiene al menos una toma programada para el día actual
     */
    private static boolean tieneTomasProgramadasParaHoy(
            List<TomaProgramada> tomasMedicamento,
            Calendar ahora) {
        
        if (tomasMedicamento == null || tomasMedicamento.isEmpty()) {
            return false;
        }

        for (TomaProgramada toma : tomasMedicamento) {
            if (toma.getFechaHoraProgramada() == null) {
                continue;
            }
            
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(toma.getFechaHoraProgramada());

            // Verificar que la toma sea del día actual
            boolean esDelDiaActual = fechaToma.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
                                   fechaToma.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR);

            if (esDelDiaActual) {
                return true; // Tiene al menos una toma programada para hoy
            }
        }

        return false; // No tiene tomas programadas para el día actual
    }

}

