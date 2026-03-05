package com.controlmedicamentos.myapplication.utils;

import com.controlmedicamentos.myapplication.models.Medicamento;

import java.util.Calendar;
import java.util.Date;

/**
 * Utilidades estáticas para medicamentos (filtrado y estado).
 * Paridad con la lógica web (medicamentoUtils, adherencia).
 */
public final class MedicamentoUtils {

    private MedicamentoUtils() {
    }

    /**
     * Indica si el medicamento es "ocasional" (sin seguimiento de adherencia).
     * En Android, crónico = diasTratamiento == -1.
     * Ocasional si: (!crónico && diasTratamiento == 0) || tomasDiarias == 0.
     */
    public static boolean esMedicamentoOcasional(Medicamento med) {
        if (med == null) return true;
        boolean cronico = med.getDiasTratamiento() == -1;
        return (!cronico && med.getDiasTratamiento() == 0) || med.getTomasDiarias() == 0;
    }

    /**
     * Indica si el medicamento tiene tomas programadas hoy (para dashboard/filtros del día).
     */
    public static boolean tieneTomasProgramadas(Medicamento med) {
        if (med == null || esMedicamentoOcasional(med)) return false;
        java.util.List<String> horarios = med.getHorariosTomasHoy();
        return horarios != null && !horarios.isEmpty();
    }

    /**
     * Indica si el medicamento tiene tomas programadas en alguna parte de la semana (seguimiento de adherencia).
     * Incluye: programación por día con al menos un día con horarios, o lista de horarios, o tomas diarias + primera hora.
     * Los únicos que quedan fuera del seguimiento son los que no tienen tomas programadas en toda la semana.
     */
    public static boolean tieneTomasProgramadasEnLaSemana(Medicamento med) {
        if (med == null) return false;
        if (med.tieneProgramacionConHorarios()) return true;
        if (med.getHorariosTomas() != null && !med.getHorariosTomas().isEmpty()) return true;
        if (med.getTomasDiarias() > 0 && med.getHorarioPrimeraToma() != null && !med.getHorarioPrimeraToma().isEmpty()) return true;
        return false;
    }

    /**
     * Indica si el medicamento está vigente para poder marcar como tomado:
     * activo, no pausado, no vencido y con stock.
     */
    public static boolean esActivoVigente(Medicamento med) {
        if (med == null) return false;
        return med.isActivo()
            && !med.isPausado()
            && !estaVencido(med)
            && (med.getStockActual() > 0);
    }

    /**
     * Indica si el medicamento está vencido (fecha de vencimiento &lt; hoy).
     * Compara solo la fecha (sin hora). Si no tiene fecha de vencimiento, no está vencido.
     */
    public static boolean estaVencido(Medicamento med) {
        if (med == null || med.getFechaVencimiento() == null) return false;
        Date hoy = truncarSoloFecha(new Date());
        Date vencimiento = truncarSoloFecha(med.getFechaVencimiento());
        return vencimiento.before(hoy);
    }

    private static Date truncarSoloFecha(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
