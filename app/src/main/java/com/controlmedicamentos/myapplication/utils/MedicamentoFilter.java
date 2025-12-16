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
     * Solo incluye medicamentos activos con tomas válidas pendientes.
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
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);

        Logger.d(TAG, "Filtrando medicamentos. Hora actual: " + horaActual + ":" + minutoActual);

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

            // Si no hay tomas inicializadas, el medicamento es nuevo
            if (tomasMedicamento == null || tomasMedicamento.isEmpty()) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " sin tomas inicializadas, agregando al dashboard");
                resultado.add(med);
                continue;
            }

            // Verificar si completó todas las tomas del día
            if (trackingService.completoTodasLasTomasDelDia(med.getId())) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: completó todas las tomas");
                continue;
            }

            // Verificar si tiene tomas válidas según la hora actual
            if (!tieneTomasValidas(med, tomasMedicamento, trackingService, ahora, horaActual, minutoActual)) {
                continue;
            }

            // Si llegó aquí, mostrar en dashboard
            Logger.d(TAG, "Medicamento " + med.getNombre() + " agregado al dashboard");
            resultado.add(med);
        }

        Logger.d(TAG, "Filtrado completado: " + resultado.size() + " medicamentos para mostrar en dashboard");
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
     * Verifica si un medicamento tiene tomas válidas según la hora actual.
     */
    private static boolean tieneTomasValidas(
            Medicamento med,
            List<TomaProgramada> tomasMedicamento,
            TomaTrackingService trackingService,
            Calendar ahora,
            int horaActual,
            int minutoActual) {

        // Entre 00:01 y 01:00: solo mostrar si tiene tomas posponibles
        if ((horaActual == 0 && minutoActual >= 1) || (horaActual == 1 && minutoActual == 0)) {
            if (!trackingService.tieneTomasPosponibles(med.getId())) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: no tiene tomas posponibles entre 00:01-01:00");
                return false;
            }
            return true;
        }

        // Después de las 01:01hs: verificar si tiene tomas válidas (futuras o pasadas menos de 1 hora)
        if (horaActual >= 1 && minutoActual >= 1) {
            return tieneTomasValidasDespuesDe0101(med, tomasMedicamento, trackingService, ahora);
        }

        // Antes de las 01:01hs: verificar si tiene tomas válidas (futuras o pasadas menos de 1 hora)
        return tieneTomasValidasAntesDe0101(med, tomasMedicamento, trackingService, ahora);
    }

    /**
     * Verifica si tiene tomas válidas después de las 01:01hs.
     */
    private static boolean tieneTomasValidasDespuesDe0101(
            Medicamento med,
            List<TomaProgramada> tomasMedicamento,
            TomaTrackingService trackingService,
            Calendar ahora) {

        for (TomaProgramada toma : tomasMedicamento) {
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(toma.getFechaHoraProgramada());

            // Verificar que la toma sea del día actual
            boolean esDelDiaActual = fechaToma.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
                                   fechaToma.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR);

            if (!esDelDiaActual || toma.isTomada() || 
                toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                continue;
            }

            // Verificar si es futura
            boolean esFutura = fechaToma.after(ahora);

            // Si es futura, es válida
            if (esFutura) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " tiene toma válida futura: " + 
                          String.format("%02d:%02d", fechaToma.get(Calendar.HOUR_OF_DAY), fechaToma.get(Calendar.MINUTE)));
                return true;
            }

            // Si es pasada, verificar que no haya pasado más de 1 hora
            Calendar fechaLimite = (Calendar) fechaToma.clone();
            fechaLimite.add(Calendar.HOUR_OF_DAY, 1);
            if (toma.getPosposiciones() > 0) {
                                fechaLimite.add(Calendar.MINUTE, toma.getPosposiciones() * Constants.MINUTOS_POSPOSICION);
            }

            if (ahora.before(fechaLimite)) {
                Logger.d(TAG, "Medicamento " + med.getNombre() + " tiene toma válida pasada: " + 
                          String.format("%02d:%02d", fechaToma.get(Calendar.HOUR_OF_DAY), fechaToma.get(Calendar.MINUTE)));
                return true;
            }
        }

        Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: después de 01:01hs sin tomas válidas del día actual");
        return false;
    }

    /**
     * Verifica si tiene tomas válidas antes de las 01:01hs.
     */
    private static boolean tieneTomasValidasAntesDe0101(
            Medicamento med,
            List<TomaProgramada> tomasMedicamento,
            TomaTrackingService trackingService,
            Calendar ahora) {

        for (TomaProgramada toma : tomasMedicamento) {
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(toma.getFechaHoraProgramada());

            // Verificar que la toma sea del día actual
            boolean esDelDiaActual = fechaToma.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
                                   fechaToma.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR);

            if (!esDelDiaActual || toma.isTomada() || 
                toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                continue;
            }

            // Verificar si es futura
            boolean esFutura = fechaToma.after(ahora);

            // Si es futura, es válida
            if (esFutura) {
                return true;
            }

            // Si es pasada, verificar que no haya pasado más de 1 hora
            Calendar fechaLimite = (Calendar) fechaToma.clone();
            fechaLimite.add(Calendar.HOUR_OF_DAY, 1);
            if (toma.getPosposiciones() > 0) {
                                fechaLimite.add(Calendar.MINUTE, toma.getPosposiciones() * Constants.MINUTOS_POSPOSICION);
            }

            if (ahora.before(fechaLimite)) {
                return true;
            }
        }

        Logger.d(TAG, "Medicamento " + med.getNombre() + " rechazado: no tiene tomas válidas del día actual");
        return false;
    }
}

