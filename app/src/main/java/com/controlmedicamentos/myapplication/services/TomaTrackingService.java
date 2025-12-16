package com.controlmedicamentos.myapplication.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para rastrear y gestionar el estado de las tomas programadas
 */
public class TomaTrackingService {
    private static final String TAG = "TomaTrackingService";
    private static final String PREF_TOMAS_PROGRAMADAS = "tomas_programadas";
    private static final String PREF_POSPOSICIONES = "posposiciones";
    
    private Context context;
    private SharedPreferences preferences;
    private Map<String, List<TomaProgramada>> tomasPorMedicamento;
    
    public TomaTrackingService(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("ControlMedicamentos", Context.MODE_PRIVATE);
        this.tomasPorMedicamento = new HashMap<>();
        cargarTomasProgramadas();
    }
    
    /**
     * Inicializa las tomas programadas para un medicamento en el día actual
     * Siempre limpia las tomas del día anterior y genera nuevas para el día actual
     */
    public void inicializarTomasDia(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            Log.w(TAG, "inicializarTomasDia: medicamento o ID es null");
            return;
        }
        
        List<String> horarios = medicamento.getHorariosTomas();
        if (horarios == null || horarios.isEmpty()) {
            Log.w(TAG, "inicializarTomasDia: horarios vacíos para medicamento " + medicamento.getId() + 
                  ", tomasDiarias=" + medicamento.getTomasDiarias() + 
                  ", horarioPrimeraToma=" + medicamento.getHorarioPrimeraToma());
            // Limpiar tomas anteriores incluso si no hay horarios
            tomasPorMedicamento.remove(medicamento.getId());
            guardarTomasProgramadas();
            return;
        }
        
        Log.d(TAG, "inicializarTomasDia: inicializando " + horarios.size() + " tomas para medicamento " + medicamento.getId());
        
        Calendar ahora = Calendar.getInstance();
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);
        
        // Limpiar tomas del día anterior para este medicamento
        List<TomaProgramada> tomasExistentes = tomasPorMedicamento.get(medicamento.getId());
        if (tomasExistentes != null) {
            tomasExistentes.removeIf(toma -> {
                if (toma.getFechaHoraProgramada() == null) {
                    return true; // Eliminar tomas sin fecha
                }
                Calendar fechaToma = Calendar.getInstance();
                fechaToma.setTime(toma.getFechaHoraProgramada());
                // Eliminar tomas que no son del día actual
                return !esTomaDelDia(fechaToma, ahora);
            });
        }
        
        // Generar nuevas tomas para el día actual
        List<TomaProgramada> tomas = new ArrayList<>();
        
        for (String horario : horarios) {
            try {
                String[] partes = horario.split(":");
                if (partes.length != 2) {
                    continue;
                }
                
                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);
                
                Calendar fechaToma = (Calendar) hoy.clone();
                fechaToma.set(Calendar.HOUR_OF_DAY, hora);
                fechaToma.set(Calendar.MINUTE, minuto);
                
                boolean esDelDiaActual = esTomaDelDia(fechaToma, ahora);
                
                // Solo incluir tomas del día actual
                if (!esDelDiaActual) {
                    continue; // No es del día actual, no incluir
                }
                
                // Verificar si ya existe una toma para este horario del día actual
                boolean yaExiste = false;
                if (tomasExistentes != null) {
                    for (TomaProgramada tomaExistente : tomasExistentes) {
                        if (tomaExistente.getHorario().equals(horario)) {
                            Calendar fechaTomaExistente = Calendar.getInstance();
                            fechaTomaExistente.setTime(tomaExistente.getFechaHoraProgramada());
                            if (esTomaDelDia(fechaTomaExistente, ahora)) {
                                // Ya existe una toma para este horario del día actual, mantenerla
                                tomas.add(tomaExistente);
                                yaExiste = true;
                                break;
                            }
                        }
                    }
                }
                
                if (yaExiste) {
                    continue; // Ya existe, no crear duplicado
                }
                
                // Si es del día actual, incluirla SIEMPRE (futura o pasada)
                TomaProgramada toma = new TomaProgramada(
                    medicamento.getId(),
                    horario,
                    fechaToma.getTime()
                );
                
                // Si la toma ya pasó, marcarla como omitida solo si es después de las 01:01hs
                // Esto permite que medicamentos creados después de las 23hs aparezcan hasta las 01:01hs
                if (fechaToma.before(ahora)) {
                    int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
                    int minutoActual = ahora.get(Calendar.MINUTE);
                    // Si es después de las 01:01hs, marcar como omitida
                    if (horaActual >= 1 && minutoActual >= 1) {
                        toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                        toma.setFechaHoraOmitida(ahora.getTime());
                    }
                }
                
                tomas.add(toma);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error al parsear horario: " + horario, e);
            }
        }
        
        // Si había tomas existentes, agregar las nuevas a la lista existente
        if (tomasExistentes != null && !tomasExistentes.isEmpty()) {
            tomas.addAll(tomasExistentes);
        }
        
        tomasPorMedicamento.put(medicamento.getId(), tomas);
        guardarTomasProgramadas();
        
        Log.d(TAG, "inicializarTomasDia: " + tomas.size() + " tomas inicializadas para medicamento " + medicamento.getId());
    }
    
    /**
     * Verifica si una fecha es del día actual
     */
    private boolean esTomaDelDia(Calendar fechaToma, Calendar ahora) {
        return fechaToma.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
               fechaToma.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * Obtiene el estado actual de una toma específica
     */
    public TomaProgramada.EstadoTomaProgramada obtenerEstadoToma(
            String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return TomaProgramada.EstadoTomaProgramada.PENDIENTE;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario)) {
                actualizarEstadoToma(toma);
                return toma.getEstado();
            }
        }
        
        return TomaProgramada.EstadoTomaProgramada.PENDIENTE;
    }
    
    /**
     * Obtiene todas las tomas programadas de un medicamento
     */
    public List<TomaProgramada> obtenerTomasMedicamento(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return new ArrayList<>();
        }
        
        // Actualizar estados antes de retornar
        for (TomaProgramada toma : tomas) {
            actualizarEstadoToma(toma);
        }
        
        return tomas;
    }
    
    /**
     * Actualiza el estado de una toma según el tiempo actual
     */
    private void actualizarEstadoToma(TomaProgramada toma) {
        if (toma == null || toma.isTomada()) {
            return; // Si ya fue tomada, no actualizar
        }
        
        Date ahora = new Date();
        Date fechaProgramada = toma.getFechaHoraProgramada();
        
        if (fechaProgramada == null) {
            return;
        }
        
        // Calcular fechas de transición
        Date fechaAlertaAmarilla = toma.calcularFechaAlertaAmarilla();
        Date fechaRetraso = toma.calcularFechaRetraso();
        Date fechaOmitida = toma.calcularFechaOmitida();
        
        // Actualizar estado según el tiempo
        if (ahora.after(fechaOmitida)) {
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                toma.setFechaHoraOmitida(ahora);
                guardarTomasProgramadas();
            }
        } else if (ahora.after(fechaRetraso)) {
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.RETRASO &&
                toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.RETRASO);
                if (toma.getFechaHoraRetraso() == null) {
                    toma.setFechaHoraRetraso(ahora);
                }
                guardarTomasProgramadas();
            }
        } else if (ahora.after(fechaProgramada)) {
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA &&
                toma.getEstado() != TomaProgramada.EstadoTomaProgramada.RETRASO &&
                toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA);
                if (toma.getFechaHoraAlertaRoja() == null) {
                    toma.setFechaHoraAlertaRoja(ahora);
                }
                guardarTomasProgramadas();
            }
        } else if (fechaAlertaAmarilla != null && ahora.after(fechaAlertaAmarilla)) {
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.PENDIENTE) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.ALERTA_AMARILLA);
                if (toma.getFechaHoraAlertaAmarilla() == null) {
                    toma.setFechaHoraAlertaAmarilla(ahora);
                }
                guardarTomasProgramadas();
            }
        }
    }
    
    /**
     * Verifica si se puede marcar una toma como tomada
     * @param medicamentoId ID del medicamento
     * @param horario Horario de la toma
     * @return null si se puede marcar, o mensaje de error si no se puede
     */
    public String validarPuedeMarcarToma(String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null || tomas.isEmpty()) {
            return "No se encontraron tomas programadas para este medicamento";
        }
        
        Calendar ahora = Calendar.getInstance();
        TomaProgramada tomaEncontrada = null;
        
        // Buscar la toma correspondiente
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario)) {
                tomaEncontrada = toma;
                break;
            }
        }
        
        if (tomaEncontrada == null) {
            return "No se encontró la toma programada para este horario";
        }
        
        if (tomaEncontrada.isTomada()) {
            return "Esta toma ya fue marcada como tomada";
        }
        
        if (tomaEncontrada.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
            return "Esta toma ya fue marcada como omitida y no se puede marcar como tomada";
        }
        
        // Validar que no se marque antes de la hora programada
        if (tomaEncontrada.getFechaHoraProgramada() != null) {
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(tomaEncontrada.getFechaHoraProgramada());
            
            // Permitir marcar hasta 10 minutos antes (ventana de alerta amarilla)
            Calendar fechaLimiteAntes = (Calendar) fechaToma.clone();
            fechaLimiteAntes.add(Calendar.MINUTE, -10);
            
            if (ahora.before(fechaLimiteAntes)) {
                return "No se puede marcar como tomado antes de la hora programada. La toma está programada para " + 
                       String.format("%02d:%02d", fechaToma.get(Calendar.HOUR_OF_DAY), fechaToma.get(Calendar.MINUTE));
            }
            
            // Validar que no se marque después de más de 1 hora de posponer
            // Si la toma fue pospuesta, verificar que no haya pasado más de 1 hora desde la hora original
            Calendar fechaLimiteDespues = (Calendar) fechaToma.clone();
            fechaLimiteDespues.add(Calendar.HOUR_OF_DAY, 1);
            
            // Si tiene posposiciones, ajustar el límite
            if (tomaEncontrada.getPosposiciones() > 0) {
                // Cada posposición agrega 10 minutos, máximo 3 = 30 minutos
                // El límite es 1 hora desde la hora original + tiempo de posposiciones
                fechaLimiteDespues = (Calendar) fechaToma.clone();
                fechaLimiteDespues.add(Calendar.MINUTE, tomaEncontrada.getPosposiciones() * 10);
                fechaLimiteDespues.add(Calendar.HOUR_OF_DAY, 1);
            }
            
            if (ahora.after(fechaLimiteDespues)) {
                return "Ya pasó más de 1 hora desde la hora programada. Esta toma se considera omitida y no se puede marcar como tomada";
            }
        }
        
        return null; // Se puede marcar
    }
    
    /**
     * Obtiene la toma programada más próxima que se puede marcar como tomada
     * @param medicamentoId ID del medicamento
     * @return TomaProgramada más próxima válida, o null si no hay ninguna
     */
    public TomaProgramada obtenerTomaProximaValida(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null || tomas.isEmpty()) {
            return null;
        }
        
        Calendar ahora = Calendar.getInstance();
        TomaProgramada tomaProxima = null;
        long minutosMinimos = Long.MAX_VALUE;
        
        for (TomaProgramada toma : tomas) {
            if (toma.isTomada() || toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                continue;
            }
            
            if (toma.getFechaHoraProgramada() == null) {
                continue;
            }
            
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(toma.getFechaHoraProgramada());
            
            // Verificar que sea del día actual
            if (fechaToma.get(Calendar.YEAR) != ahora.get(Calendar.YEAR) ||
                fechaToma.get(Calendar.DAY_OF_YEAR) != ahora.get(Calendar.DAY_OF_YEAR)) {
                continue;
            }
            
            // Verificar que no haya pasado más de 1 hora
            Calendar fechaLimite = (Calendar) fechaToma.clone();
            fechaLimite.add(Calendar.HOUR_OF_DAY, 1);
            if (toma.getPosposiciones() > 0) {
                fechaLimite.add(Calendar.MINUTE, toma.getPosposiciones() * 10);
            }
            
            if (ahora.after(fechaLimite)) {
                continue; // Ya pasó más de 1 hora
            }
            
            // Verificar que no sea más de 10 minutos antes
            Calendar fechaLimiteAntes = (Calendar) fechaToma.clone();
            fechaLimiteAntes.add(Calendar.MINUTE, -10);
            if (ahora.before(fechaLimiteAntes)) {
                continue; // Aún es muy temprano
            }
            
            // Calcular minutos hasta la toma
            long minutosHasta = Math.abs(fechaToma.getTimeInMillis() - ahora.getTimeInMillis()) / (60 * 1000);
            if (minutosHasta < minutosMinimos) {
                minutosMinimos = minutosHasta;
                tomaProxima = toma;
            }
        }
        
        return tomaProxima;
    }
    
    /**
     * Marca una toma como tomada
     */
    public void marcarTomaComoTomada(String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario) && !toma.isTomada()) {
                toma.setTomada(true);
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.PENDIENTE);
                guardarTomasProgramadas();
                break;
            }
        }
    }
    
    /**
     * Pospone una toma (máximo 3 veces)
     */
    public boolean posponerToma(String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return false;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario) && !toma.isTomada()) {
                boolean pospuesta = toma.posponer();
                if (pospuesta) {
                    // Reprogramar la toma 10 minutos después
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(toma.getFechaHoraProgramada());
                    cal.add(Calendar.MINUTE, 10);
                    toma.setFechaHoraProgramada(cal.getTime());
                    toma.setEstado(TomaProgramada.EstadoTomaProgramada.PENDIENTE);
                    guardarTomasProgramadas();
                    return true;
                } else {
                    // Ya se pospuso 3 veces, marcar como omitida
                    toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                    toma.setFechaHoraOmitida(new Date());
                    guardarTomasProgramadas();
                    return false;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si un medicamento tiene tomas omitidas
     */
    public boolean tieneTomasOmitidas(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return false;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si un medicamento completó todas sus tomas del día
     */
    public boolean completoTodasLasTomasDelDia(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null || tomas.isEmpty()) {
            return false; // Si no hay tomas, no está completo
        }
        
        // Verificar que todas las tomas del día estén tomadas
        for (TomaProgramada toma : tomas) {
            if (!toma.isTomada()) {
                return false; // Hay al menos una toma no tomada
            }
        }
        
        return true; // Todas las tomas fueron tomadas
    }
    
    /**
     * Verifica si un medicamento tiene tomas que pueden posponerse
     * (dentro de la ventana de 1 hora después de la hora programada)
     * Solo aplica después de las 00:00hs para tomas programadas a las 00:00hs
     */
    public boolean tieneTomasPosponibles(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null || tomas.isEmpty()) {
            return false;
        }
        
        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);
        
        // Solo verificar después de las 00:00hs
        if (horaActual == 0 && minutoActual < 1) {
            return false; // Aún no es después de las 00:00hs
        }
        
        // Verificar si hay tomas de las 00:00hs que pueden posponerse
        // (no tomadas y dentro de la ventana de 1 hora después de las 00:00hs, hasta las 01:00hs)
        for (TomaProgramada toma : tomas) {
            if (toma.isTomada()) {
                continue; // Ya fue tomada
            }
            
            Date fechaProgramada = toma.getFechaHoraProgramada();
            if (fechaProgramada == null) {
                continue;
            }
            
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(fechaProgramada);
            
            // Solo considerar tomas programadas a las 00:00hs
            if (fechaToma.get(Calendar.HOUR_OF_DAY) != 0 || fechaToma.get(Calendar.MINUTE) != 0) {
                continue; // No es una toma de las 00:00hs
            }
            
            // Calcular la fecha límite (1 hora después de las 00:00hs = 01:00hs)
            Calendar fechaLimite = (Calendar) fechaToma.clone();
            fechaLimite.add(Calendar.HOUR_OF_DAY, 1);
            fechaLimite.set(Calendar.MINUTE, 0);
            fechaLimite.set(Calendar.SECOND, 0);
            fechaLimite.set(Calendar.MILLISECOND, 0);
            
            // Si estamos antes de las 01:00hs, la toma puede posponerse
            if (ahora.before(fechaLimite)) {
                return true; // Hay una toma de las 00:00hs que puede posponerse
            }
        }
        
        return false;
    }
    
    /**
     * Marca automáticamente como omitidas las tomas que pasaron las 01:01hs sin ser tomadas
     * Solo marca tomas del día actual que YA PASARON y no fueron tomadas
     * NO marca tomas futuras
     */
    public void marcarTomasOmitidasDespuesDe0101() {
        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);
        
        // Solo procesar después de las 01:01hs
        if (horaActual < 1 || (horaActual == 1 && minutoActual < 1)) {
            return; // Aún no es después de las 01:01hs
        }
        
        Calendar fechaLimite = Calendar.getInstance();
        fechaLimite.set(Calendar.HOUR_OF_DAY, 1);
        fechaLimite.set(Calendar.MINUTE, 1);
        fechaLimite.set(Calendar.SECOND, 0);
        fechaLimite.set(Calendar.MILLISECOND, 0);
        
        // Si ya pasó las 01:01hs, marcar SOLO las tomas que YA PASARON (no futuras) como omitidas
        if (ahora.after(fechaLimite) || ahora.equals(fechaLimite)) {
            for (List<TomaProgramada> tomas : tomasPorMedicamento.values()) {
                for (TomaProgramada toma : tomas) {
                    if (!toma.isTomada() && 
                        toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                        Date fechaProgramada = toma.getFechaHoraProgramada();
                        if (fechaProgramada != null) {
                            Calendar fechaToma = Calendar.getInstance();
                            fechaToma.setTime(fechaProgramada);
                            
                            // Verificar si la toma es del día actual Y YA PASÓ (no es futura)
                            if (esTomaDelDia(fechaToma, ahora) && fechaToma.before(ahora)) {
                                // Solo marcar como omitida si la toma YA PASÓ
                                toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                                toma.setFechaHoraOmitida(ahora.getTime());
                            }
                        }
                    }
                }
            }
            guardarTomasProgramadas();
        }
    }
    
    /**
     * Guarda las tomas programadas en SharedPreferences
     */
    private void guardarTomasProgramadas() {
        // Por simplicidad, guardamos solo los IDs y horarios
        // En producción, se podría usar JSON o una base de datos
        // Por ahora, las tomas se recalculan cada vez que se inicia la app
    }
    
    /**
     * Carga las tomas programadas desde SharedPreferences
     */
    private void cargarTomasProgramadas() {
        // Por simplicidad, las tomas se inicializan cuando se cargan los medicamentos
        // En producción, se podría cargar desde SharedPreferences o base de datos
    }
    
    /**
     * Limpia las tomas del día anterior
     */
    public void limpiarTomasAnteriores() {
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);
        
        for (List<TomaProgramada> tomas : tomasPorMedicamento.values()) {
            tomas.removeIf(toma -> {
                if (toma.getFechaHoraProgramada() == null) {
                    return true;
                }
                Calendar fechaToma = Calendar.getInstance();
                fechaToma.setTime(toma.getFechaHoraProgramada());
                return fechaToma.before(hoy);
            });
        }
        
        guardarTomasProgramadas();
    }
}

