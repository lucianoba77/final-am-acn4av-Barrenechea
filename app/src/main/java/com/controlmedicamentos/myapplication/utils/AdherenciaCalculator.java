package com.controlmedicamentos.myapplication.utils;

import com.controlmedicamentos.myapplication.models.AdherenciaIntervalo;
import com.controlmedicamentos.myapplication.models.AdherenciaResumen;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utilidad para calcular métricas de adherencia en diferentes rangos.
 */
public final class AdherenciaCalculator {

    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private AdherenciaCalculator() {
    }

    public static AdherenciaResumen calcularResumenGeneral(Medicamento medicamento, List<Toma> tomas) {
        Date ahora = new Date();
        Date fechaInicio = medicamento.getFechaInicioTratamiento() != null
            ? medicamento.getFechaInicioTratamiento()
            : obtenerFechaMasAntigua(tomas, ahora);

        if (fechaInicio.after(ahora)) {
            fechaInicio = ahora;
        }

        Date fechaFin;
        if (medicamento.getDiasTratamiento() > 0) {
            fechaFin = sumarDias(fechaInicio, medicamento.getDiasTratamiento() - 1);
            if (fechaFin.after(ahora)) {
                fechaFin = ahora;
            }
        } else {
            fechaFin = ahora;
        }

        int diasSeguimiento = Math.max(1, diasEntre(fechaInicio, fechaFin) + 1);
        boolean esOcasional = medicamento.getTomasDiarias() == 0;

        int tomasEsperadas = esOcasional ? contarTomasEnRango(tomas, fechaInicio, fechaFin) :
            medicamento.getTomasDiarias() * diasSeguimiento;
        int tomasRealizadas = contarTomasEnRango(tomas, fechaInicio, fechaFin);

        float porcentaje;
        if (tomasEsperadas == 0) {
            porcentaje = tomasRealizadas > 0 ? 100f : 0f;
        } else {
            porcentaje = Math.min(100f, (tomasRealizadas * 100f) / (float) tomasEsperadas);
        }

        return new AdherenciaResumen(
            medicamento.getId(),
            medicamento.getNombre(),
            tomasEsperadas,
            tomasRealizadas,
            porcentaje,
            medicamento.getDiasTratamiento() == -1
        );
    }

    public static List<AdherenciaIntervalo> calcularAdherenciaSemanal(Medicamento medicamento, List<Toma> tomas) {
        List<AdherenciaIntervalo> resultado = new ArrayList<>();
        Date hoy = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncarFecha(sumarDias(hoy, -6)));

        for (int i = 0; i < 7; i++) {
            Date inicio = cal.getTime();
            Date fin = finDeDia(inicio);

            boolean esOcasional = medicamento.getTomasDiarias() == 0;
            int esperadas = esOcasional ? 1 : medicamento.getTomasDiarias();
            int realizadas = contarTomasEnRango(tomas, inicio, fin);
            if (esOcasional && realizadas == 0) {
                esperadas = 1; // se usa como factor para mostrar 0%
            }
            float porcentaje = esperadas == 0 ? 0f : Math.min(100f, (realizadas * 100f) / (float) esperadas);
            String etiqueta = obtenerNombreCortoDia(cal.get(Calendar.DAY_OF_WEEK));

            resultado.add(new AdherenciaIntervalo(etiqueta, esperadas, realizadas, porcentaje));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return resultado;
    }

    public static List<AdherenciaIntervalo> calcularAdherenciaMensual(Medicamento medicamento, List<Toma> tomas) {
        List<AdherenciaIntervalo> resultado = new ArrayList<>();
        Date hoy = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncarFecha(sumarDias(hoy, -27)));

        for (int semana = 0; semana < 4; semana++) {
            Date inicio = cal.getTime();
            Date fin = finDeDia(sumarDias(inicio, 6));
            boolean esOcasional = medicamento.getTomasDiarias() == 0;
            int diasIntervalo = diasEntre(inicio, fin) + 1;
            int esperadas = esOcasional ? Math.max(1, contarTomasEnRango(tomas, inicio, fin))
                : medicamento.getTomasDiarias() * diasIntervalo;
            int realizadas = contarTomasEnRango(tomas, inicio, fin);
            float porcentaje = esperadas == 0 ? 0f : Math.min(100f, (realizadas * 100f) / (float) esperadas);
            resultado.add(new AdherenciaIntervalo(
                "Sem " + (semana + 1),
                esperadas,
                realizadas,
                porcentaje
            ));
            cal.add(Calendar.DAY_OF_YEAR, 7);
        }

        return resultado;
    }

    public static List<Toma> filtrarTomasPorMedicamento(List<Toma> tomas, String medicamentoId) {
        List<Toma> resultado = new ArrayList<>();
        if (tomas == null || medicamentoId == null) {
            return resultado;
        }
        for (Toma toma : tomas) {
            if (toma != null && medicamentoId.equals(toma.getMedicamentoId())) {
                resultado.add(toma);
            }
        }
        return resultado;
    }

    private static Date truncarFecha(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date finDeDia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private static Date sumarDias(Date fecha, int dias) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.add(Calendar.DAY_OF_YEAR, dias);
        return cal.getTime();
    }

    private static int diasEntre(Date inicio, Date fin) {
        long diff = finDeDia(fin).getTime() - truncarFecha(inicio).getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    private static int contarTomasEnRango(List<Toma> tomas, Date inicio, Date fin) {
        if (tomas == null || tomas.isEmpty()) {
            return 0;
        }
        int contador = 0;
        for (Toma toma : tomas) {
            if (toma == null) continue;
            
            // Solo contar tomas con estado TOMADA (no PERDIDA ni PENDIENTE)
            if (toma.getEstado() != null && toma.getEstado() != Toma.EstadoToma.TOMADA) {
                continue;
            }
            
            Date fecha = toma.getFechaHoraTomada() != null ? toma.getFechaHoraTomada() : toma.getFechaHoraProgramada();
            if (fecha == null) continue;
            if (!fecha.before(inicio) && !fecha.after(fin)) {
                contador++;
            }
        }
        return contador;
    }

    private static Date obtenerFechaMasAntigua(List<Toma> tomas, Date fallback) {
        if (tomas == null || tomas.isEmpty()) {
            return fallback;
        }
        Date min = fallback;
        for (Toma toma : tomas) {
            if (toma == null) continue;
            Date fecha = toma.getFechaHoraTomada() != null ? toma.getFechaHoraTomada() : toma.getFechaHoraProgramada();
            if (fecha != null && fecha.before(min)) {
                min = fecha;
            }
        }
        return min;
    }

    private static String obtenerNombreCortoDia(int diaSemana) {
        String[] nombres = new DateFormatSymbols(LOCALE_ES).getShortWeekdays();
        String nombre = nombres[diaSemana];
        if (nombre == null || nombre.isEmpty()) {
            return " ";
        }
        return nombre.substring(0, 1).toUpperCase(LOCALE_ES) + nombre.substring(1).toLowerCase(LOCALE_ES);
    }

    /**
     * Calcula el resumen general de adherencia del paciente considerando todos sus medicamentos
     * @param medicamentos Lista de todos los medicamentos del paciente
     * @param todasLasTomas Lista de todas las tomas del paciente
     * @return Resumen de adherencia general
     */
    public static AdherenciaResumen calcularAdherenciaGeneralPaciente(
            List<Medicamento> medicamentos, List<Toma> todasLasTomas) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return new AdherenciaResumen("", "General", 0, 0, 0f, false);
        }

        int totalTomasEsperadas = 0;
        int totalTomasRealizadas = 0;
        Date fechaInicioGeneral = null;
        Date fechaFinGeneral = new Date();

        // Calcular tomas esperadas y realizadas para cada medicamento activo
        for (Medicamento medicamento : medicamentos) {
            if (!medicamento.isActivo() || medicamento.isPausado()) {
                continue;
            }

            List<Toma> tomasMedicamento = filtrarTomasPorMedicamento(todasLasTomas, medicamento.getId());
            AdherenciaResumen resumen = calcularResumenGeneral(medicamento, tomasMedicamento);
            
            totalTomasEsperadas += resumen.getTomasEsperadas();
            totalTomasRealizadas += resumen.getTomasRealizadas();

            // Determinar fecha de inicio más antigua
            if (medicamento.getFechaInicioTratamiento() != null) {
                if (fechaInicioGeneral == null || 
                    medicamento.getFechaInicioTratamiento().before(fechaInicioGeneral)) {
                    fechaInicioGeneral = medicamento.getFechaInicioTratamiento();
                }
            }
        }

        float porcentaje = totalTomasEsperadas == 0 ? 0f : 
            Math.min(100f, (totalTomasRealizadas * 100f) / (float) totalTomasEsperadas);

        return new AdherenciaResumen("", "Adherencia General", 
            totalTomasEsperadas, totalTomasRealizadas, porcentaje, false);
    }

    /**
     * Calcula la adherencia general del paciente por semana (últimas 4 semanas)
     * @param medicamentos Lista de todos los medicamentos del paciente
     * @param todasLasTomas Lista de todas las tomas del paciente
     * @return Lista de intervalos semanales con adherencia
     */
    public static List<AdherenciaIntervalo> calcularAdherenciaGeneralSemanal(
            List<Medicamento> medicamentos, List<Toma> todasLasTomas) {
        List<AdherenciaIntervalo> resultado = new ArrayList<>();
        Date hoy = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncarFecha(sumarDias(hoy, -27))); // Últimas 4 semanas

        for (int semana = 0; semana < 4; semana++) {
            Date inicio = cal.getTime();
            Date fin = finDeDia(sumarDias(inicio, 6));

            int esperadas = 0;
            int realizadas = 0;

            // Sumar tomas esperadas y realizadas de todos los medicamentos activos
            for (Medicamento medicamento : medicamentos) {
                if (!medicamento.isActivo() || medicamento.isPausado()) {
                    continue;
                }

                List<Toma> tomasMedicamento = filtrarTomasPorMedicamento(todasLasTomas, medicamento.getId());
                boolean esOcasional = medicamento.getTomasDiarias() == 0;
                int diasIntervalo = diasEntre(inicio, fin) + 1;
                
                int esperadasMed = esOcasional ? 
                    Math.max(1, contarTomasEnRango(tomasMedicamento, inicio, fin)) :
                    medicamento.getTomasDiarias() * diasIntervalo;
                
                int realizadasMed = contarTomasEnRango(tomasMedicamento, inicio, fin);
                
                esperadas += esperadasMed;
                realizadas += realizadasMed;
            }

            float porcentaje = esperadas == 0 ? 0f : 
                Math.min(100f, (realizadas * 100f) / (float) esperadas);

            resultado.add(new AdherenciaIntervalo(
                "Sem " + (semana + 1),
                esperadas,
                realizadas,
                porcentaje
            ));
            cal.add(Calendar.DAY_OF_YEAR, 7);
        }

        return resultado;
    }

    /**
     * Calcula la adherencia general del paciente por mes (últimos 6 meses)
     * @param medicamentos Lista de todos los medicamentos del paciente
     * @param todasLasTomas Lista de todas las tomas del paciente
     * @return Lista de intervalos mensuales con adherencia
     */
    public static List<AdherenciaIntervalo> calcularAdherenciaGeneralMensual(
            List<Medicamento> medicamentos, List<Toma> todasLasTomas) {
        List<AdherenciaIntervalo> resultado = new ArrayList<>();
        Date hoy = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncarFecha(hoy));
        cal.add(Calendar.MONTH, -5); // Últimos 6 meses
        cal.set(Calendar.DAY_OF_MONTH, 1); // Primer día del mes

        for (int mes = 0; mes < 6; mes++) {
            Date inicio = cal.getTime();
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date fin = finDeDia(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);

            int esperadas = 0;
            int realizadas = 0;

            // Sumar tomas esperadas y realizadas de todos los medicamentos activos
            for (Medicamento medicamento : medicamentos) {
                if (!medicamento.isActivo() || medicamento.isPausado()) {
                    continue;
                }

                List<Toma> tomasMedicamento = filtrarTomasPorMedicamento(todasLasTomas, medicamento.getId());
                boolean esOcasional = medicamento.getTomasDiarias() == 0;
                int diasIntervalo = diasEntre(inicio, fin) + 1;
                
                int esperadasMed = esOcasional ? 
                    Math.max(1, contarTomasEnRango(tomasMedicamento, inicio, fin)) :
                    medicamento.getTomasDiarias() * diasIntervalo;
                
                int realizadasMed = contarTomasEnRango(tomasMedicamento, inicio, fin);
                
                esperadas += esperadasMed;
                realizadas += realizadasMed;
            }

            float porcentaje = esperadas == 0 ? 0f : 
                Math.min(100f, (realizadas * 100f) / (float) esperadas);

            String[] nombresMeses = new DateFormatSymbols(LOCALE_ES).getShortMonths();
            String nombreMes = nombresMeses[cal.get(Calendar.MONTH)];
            if (nombreMes == null || nombreMes.isEmpty()) {
                nombreMes = String.valueOf(cal.get(Calendar.MONTH) + 1);
            }

            resultado.add(new AdherenciaIntervalo(
                nombreMes.substring(0, 1).toUpperCase(LOCALE_ES) + nombreMes.substring(1).toLowerCase(LOCALE_ES),
                esperadas,
                realizadas,
                porcentaje
            ));
        }

        return resultado;
    }
}

