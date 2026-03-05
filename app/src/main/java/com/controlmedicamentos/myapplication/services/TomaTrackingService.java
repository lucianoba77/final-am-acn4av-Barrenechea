package com.controlmedicamentos.myapplication.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.utils.Constants;
import com.controlmedicamentos.myapplication.utils.ValidationUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio para rastrear y gestionar el estado de las tomas programadas.
 * <ul>
 *   <li><b>Persistencia:</b> El estado "tomada" se guarda en Firestore (colección tomas) al marcar "Tomado";
 *   el stock se actualiza en el documento del medicamento en la misma acción.</li>
 *   <li><b>Al cargar/reiniciar:</b> MedicamentoDataManager obtiene tomas del usuario y llama
 *   {@link #sincronizarTomasTomadasDesdeFirestore(List)} para que el dashboard refleje lo ya tomado.</li>
 *   <li><b>Cambio de horario:</b> Si hoy ya hay una toma TOMADA y el usuario cambia el horario, la segunda
 *   pasada de la sincronización marca las tomas del día actual como tomada para no avisar de nuevo.</li>
 * </ul>
 */
public class TomaTrackingService {
    private static final String TAG = "TomaTrackingService";
    
    private Context context;
    private SharedPreferences preferences;
    private Map<String, List<TomaProgramada>> tomasPorMedicamento;
    // Cache para evitar reinicializaciones innecesarias: medicamentoId -> fecha de última inicialización
    private Map<String, String> ultimaInicializacionPorMedicamento;
    
    public TomaTrackingService(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        this.tomasPorMedicamento = new HashMap<>();
        this.ultimaInicializacionPorMedicamento = new HashMap<>();
        cargarTomasProgramadas();
    }
    
    /**
     * Inicializa las tomas programadas para un medicamento en el día actual.
     * Siempre limpia las tomas del día anterior y genera nuevas para el día actual.
     * 
     * @param medicamento El medicamento para el cual inicializar las tomas. No debe ser null.
     *                    Si el medicamento no tiene horarios válidos, no se crearán tomas.
     */
    public void inicializarTomasDia(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            Log.w(TAG, "inicializarTomasDia: medicamento o ID es null");
            return;
        }
        
        String medicamentoId = medicamento.getId();
        String fechaHoy = obtenerFechaHoy();
        
        // Verificar si ya se inicializó hoy para este medicamento
        String ultimaInicializacion = ultimaInicializacionPorMedicamento.get(medicamentoId);
        if (fechaHoy.equals(ultimaInicializacion)) {
            // Ya se inicializó hoy, verificar que las tomas existan
            List<TomaProgramada> tomasExistentes = tomasPorMedicamento.get(medicamentoId);
            if (tomasExistentes != null && !tomasExistentes.isEmpty()) {
                Log.d(TAG, "inicializarTomasDia: ya inicializado hoy para " + medicamentoId + ", omitiendo");
                return;
            }
            // Si las tomas fueron eliminadas, continuar con la inicialización
        }
        
        List<String> horarios = medicamento.getHorariosTomasHoy();
        if (horarios == null || horarios.isEmpty()) {
            // Medicamentos ocasionales (tomasDiarias <= 0) no tienen horarios de toma; es esperado
            if (medicamento.getTomasDiarias() <= 0) {
                Log.d(TAG, "inicializarTomasDia: medicamento ocasional " + medicamentoId + ", sin horarios de toma (esperado)");
                tomasPorMedicamento.remove(medicamentoId);
                ultimaInicializacionPorMedicamento.remove(medicamentoId);
                guardarTomasProgramadas();
                return;
            }
            Log.w(TAG, "inicializarTomasDia: horarios vacíos para medicamento " + medicamentoId + 
                  ", tomasDiarias=" + medicamento.getTomasDiarias() + 
                  ", horarioPrimeraToma=" + medicamento.getHorarioPrimeraToma());
            // Limpiar tomas anteriores incluso si no hay horarios
            tomasPorMedicamento.remove(medicamentoId);
            ultimaInicializacionPorMedicamento.remove(medicamentoId);
            guardarTomasProgramadas();
            return;
        }
        
        Log.d(TAG, "inicializarTomasDia: inicializando " + horarios.size() + " tomas para medicamento " + medicamentoId);
        
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
            if (horario == null || horario.trim().isEmpty()) {
                Log.w(TAG, "Horario null o vacío, omitiendo");
                continue;
            }
            
            try {
                // Validar formato antes de parsear
                if (!ValidationUtils.isValidTime(horario)) {
                    Log.w(TAG, "Formato de horario inválido: " + horario);
                    continue;
                }
                
                String[] partes = horario.split(Constants.SEPARADOR_HORA);
                if (partes.length != Constants.PARTES_HORARIO_ESPERADAS) {
                    Log.w(TAG, "Horario no tiene formato correcto (esperado HH:mm): " + horario);
                    continue;
                }
                
                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);
                
                // Validar rango de hora y minuto
                if (hora < 0 || hora > 23) {
                    Log.w(TAG, "Hora fuera de rango (0-23): " + hora);
                    continue;
                }
                if (minuto < 0 || minuto > 59) {
                    Log.w(TAG, "Minuto fuera de rango (0-59): " + minuto);
                    continue;
                }
                
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
        
        tomasPorMedicamento.put(medicamentoId, tomas);
        // Marcar que se inicializó hoy
        ultimaInicializacionPorMedicamento.put(medicamentoId, fechaHoy);
        guardarTomasProgramadas();
        
        Log.d(TAG, "inicializarTomasDia: " + tomas.size() + " tomas inicializadas para medicamento " + medicamentoId);
    }
    
    /**
     * Obtiene la fecha de hoy en formato YYYY-MM-DD para usar como clave de caché
     * @return String con la fecha de hoy
     */
    private String obtenerFechaHoy() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
    
    /**
     * Verifica si una fecha corresponde al día actual.
     * 
     * @param fechaToma La fecha de la toma a verificar.
     * @param ahora La fecha/hora actual para comparar.
     * @return true si la fecha corresponde al día actual, false en caso contrario.
     */
    private boolean esTomaDelDia(Calendar fechaToma, Calendar ahora) {
        return fechaToma.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
               fechaToma.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Sincroniza el estado "tomada" desde las tomas guardadas en Firestore (hoy, estado TOMADA).
     * Así al reiniciar la app el dashboard refleja lo que ya se marcó en la base de datos.
     *
     * @param tomasTomadasHoy Lista de Toma del usuario con estado TOMADA y fecha de hoy.
     */
    public void sincronizarTomasTomadasDesdeFirestore(List<Toma> tomasTomadasHoy) {
        if (tomasTomadasHoy == null || tomasTomadasHoy.isEmpty()) {
            return;
        }
        Calendar hoy = Calendar.getInstance();
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.US);
        java.util.Set<String> medicamentosConTomaHoy = new java.util.HashSet<>();
        java.util.Set<String> medicamentosConMatchPorHorario = new java.util.HashSet<>();

        // 1) Marcar por coincidencia exacta de horario (toma a las 17:00 -> TomaProgramada 17:00)
        for (Toma toma : tomasTomadasHoy) {
            if (toma.getMedicamentoId() == null || toma.getEstado() != Toma.EstadoToma.TOMADA) {
                continue;
            }
            Date fechaProg = toma.getFechaHoraProgramada() != null ? toma.getFechaHoraProgramada() : toma.getFechaHoraTomada();
            if (fechaProg == null) {
                continue;
            }
            Calendar calProg = Calendar.getInstance();
            calProg.setTime(fechaProg);
            if (!esTomaDelDia(calProg, hoy)) {
                continue;
            }
            medicamentosConTomaHoy.add(toma.getMedicamentoId());
            String horarioStr = sdfHora.format(fechaProg);
            List<TomaProgramada> list = tomasPorMedicamento.get(toma.getMedicamentoId());
            if (list == null) continue;
            for (TomaProgramada tp : list) {
                if (horarioStr.equals(tp.getHorario())) {
                    tp.setTomada(true);
                    medicamentosConMatchPorHorario.add(toma.getMedicamentoId());
                    Log.d(TAG, "Sincronizado desde Firestore: " + toma.getMedicamentoId() + " " + horarioStr + " -> tomada");
                    break;
                }
            }
        }

        // 2) Solo si el usuario cambió el horario: hoy tiene una Toma TOMADA pero ninguna TomaProgramada
        //    coincidió (ej. tomó a las 17:00 y ahora el horario es 20:00). Marcar todas las tomas de hoy
        //    de ese medicamento como tomada para no avisar de nuevo.
        for (String medicamentoId : medicamentosConTomaHoy) {
            if (medicamentosConMatchPorHorario.contains(medicamentoId)) {
                continue;
            }
            List<TomaProgramada> list = tomasPorMedicamento.get(medicamentoId);
            if (list == null) continue;
            for (TomaProgramada tp : list) {
                if (!tp.isTomada()) {
                    tp.setTomada(true);
                    Log.d(TAG, "Sincronizado (toma del día ya registrada, horario cambiado): " + medicamentoId + " " + tp.getHorario() + " -> tomada");
                }
            }
        }
    }

    /**
     * Obtiene el estado actual de una toma específica.
     * 
     * @param medicamentoId El ID del medicamento.
     * @param horario El horario de la toma en formato "HH:mm".
     * @return El estado actual de la toma, o PENDIENTE si no se encuentra.
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
     * Obtiene todas las tomas programadas de un medicamento.
     * Los estados de las tomas se actualizan automáticamente antes de retornarlas.
     * 
     * @param medicamentoId El ID del medicamento.
     * @return Lista de tomas programadas del medicamento. Retorna lista vacía si no hay tomas.
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
     * Actualiza el estado de una toma según el tiempo actual.
     * Los estados posibles son: PENDIENTE, ALERTA_AMARILLA, ALERTA_ROJA, RETRASO, OMITIDA.
     * 
     * @param toma La toma cuyo estado se actualizará. No debe ser null.
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
            // Permitir marcar como tomada si todavía estamos dentro de la ventana (hasta 1 h después del horario).
            // Así se evita bloquear al usuario cuando la toma se marcó omitida por error (ej. hora del dispositivo incorrecta).
            if (tomaEncontrada.getFechaHoraProgramada() != null) {
                Calendar fechaToma = Calendar.getInstance();
                fechaToma.setTime(tomaEncontrada.getFechaHoraProgramada());
                Calendar limiteOmitida = (Calendar) fechaToma.clone();
                limiteOmitida.add(Calendar.HOUR_OF_DAY, Constants.HORAS_OMITIDA);
                if (ahora.before(limiteOmitida)) {
                    return null; // Todavía dentro de ventana: se puede marcar como tomada
                }
            }
            return "Esta toma ya fue marcada como omitida y no se puede marcar como tomada";
        }
        
        // Validar que no se marque antes de la hora programada
        if (tomaEncontrada.getFechaHoraProgramada() != null) {
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(tomaEncontrada.getFechaHoraProgramada());
            
            // Permitir marcar hasta 10 minutos antes (ventana de alerta amarilla)
            Calendar fechaLimiteAntes = (Calendar) fechaToma.clone();
            fechaLimiteAntes.add(Calendar.MINUTE, -Constants.MINUTOS_ALERTA_AMARILLA);
            
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
                fechaLimiteDespues.add(Calendar.MINUTE, tomaEncontrada.getPosposiciones() * Constants.MINUTOS_POSPOSICION);
                fechaLimiteDespues.add(Calendar.HOUR_OF_DAY, Constants.HORAS_OMITIDA);
            }
            
            if (ahora.after(fechaLimiteDespues)) {
                return "Ya pasó más de 1 hora desde la hora programada. Esta toma se considera omitida y no se puede marcar como tomada";
            }
        }
        
        return null; // Se puede marcar
    }
    
    /**
     * Obtiene la toma programada más próxima que se puede marcar como tomada.
     * La toma debe estar dentro de la ventana válida (10 minutos antes hasta 1 hora después).
     * 
     * @param medicamentoId ID del medicamento.
     * @return La toma programada más próxima válida, o null si no hay ninguna disponible.
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
            if (toma.isTomada()) {
                continue;
            }
            // Incluir OMITIDA solo si todavía estamos dentro de la ventana (hasta 1 h después)
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                if (toma.getFechaHoraProgramada() == null) {
                    continue;
                }
                Calendar fechaTomaOmit = Calendar.getInstance();
                fechaTomaOmit.setTime(toma.getFechaHoraProgramada());
                Calendar limiteOmit = (Calendar) fechaTomaOmit.clone();
                limiteOmit.add(Calendar.HOUR_OF_DAY, Constants.HORAS_OMITIDA);
                if (ahora.after(limiteOmit)) {
                    continue; // Ya pasó la ventana, no considerar esta toma
                }
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
            
            // Verificar que no haya pasado más de 1 hora (para no-OMITIDA; OMITIDA ya se filtró arriba)
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                Calendar fechaLimite = (Calendar) fechaToma.clone();
                fechaLimite.add(Calendar.HOUR_OF_DAY, Constants.HORAS_OMITIDA);
                if (toma.getPosposiciones() > 0) {
                    fechaLimite.add(Calendar.MINUTE, toma.getPosposiciones() * Constants.MINUTOS_POSPOSICION);
                }
                if (ahora.after(fechaLimite)) {
                    continue; // Ya pasó más de 1 hora
                }
            }
            
            // Verificar que no sea más de 10 minutos antes
            Calendar fechaLimiteAntes = (Calendar) fechaToma.clone();
            fechaLimiteAntes.add(Calendar.MINUTE, -Constants.MINUTOS_ALERTA_AMARILLA);
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
     * Marca una toma como tomada.
     * 
     * @param medicamentoId El ID del medicamento.
     * @param horario El horario de la toma en formato "HH:mm".
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
     * Pospone una toma reprogramándola 10 minutos después.
     * Solo se puede posponer hasta un máximo de 3 veces. Si se alcanza el máximo,
     * la toma se marca como omitida.
     * 
     * @param medicamentoId El ID del medicamento.
     * @param horario El horario de la toma en formato "HH:mm".
     * @return true si se pudo posponer, false si ya se alcanzó el máximo de posposiciones.
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
                    cal.add(Calendar.MINUTE, Constants.MINUTOS_POSPOSICION);
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
     * Verifica si un medicamento tiene tomas omitidas.
     * 
     * @param medicamentoId El ID del medicamento.
     * @return true si el medicamento tiene al menos una toma omitida, false en caso contrario.
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
     * Indica si la próxima acción para el medicamento es mostrar "Omitido" (gris):
     * hay tomas pendientes pero todas están omitidas y ya pasó la ventana para marcarlas como tomada.
     */
    public boolean tieneProximaOmitidaComoUnicaPendiente(String medicamentoId) {
        if (obtenerTomaProximaValida(medicamentoId) != null) {
            return false; // Hay una toma que se puede marcar como tomada
        }
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null || tomas.isEmpty()) {
            return false;
        }
        for (TomaProgramada toma : tomas) {
            if (!toma.isTomada()) {
                return true; // Hay al menos una no tomada (y no hay ninguna recuperable)
            }
        }
        return false;
    }
    
    /**
     * Verifica si un medicamento completó todas sus tomas del día.
     * 
     * @param medicamentoId El ID del medicamento.
     * @return true si todas las tomas del día fueron marcadas como tomadas, false en caso contrario.
     *         Retorna false si no hay tomas programadas.
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
     * Verifica si un medicamento tiene tomas que pueden posponerse.
     * Ventana: desde 30 minutos antes del horario hasta 1 hora después del horario.
     * No aplica a tomas ya tomadas ni a tomas ya omitidas.
     *
     * @param medicamentoId El ID del medicamento.
     * @return true si hay al menos una toma en la ventana de posponer.
     */
    public boolean tieneTomasPosponibles(String medicamentoId) {
        return obtenerTomaPosponible(medicamentoId) != null;
    }

    /**
     * Obtiene la toma programada que puede posponerse (la más próxima en la ventana).
     * Ventana: desde (horario - 30 min) hasta (horario + 1 hora). No tomadas ni omitidas.
     *
     * @param medicamentoId El ID del medicamento.
     * @return La toma que puede posponerse, o null si no hay ninguna.
     */
    public TomaProgramada obtenerTomaPosponible(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null || tomas.isEmpty()) {
            return null;
        }
        Calendar ahora = Calendar.getInstance();
        TomaProgramada candidata = null;
        long minutosMinimos = Long.MAX_VALUE;
        for (TomaProgramada toma : tomas) {
            if (toma.isTomada()) {
                continue;
            }
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                continue; // Ya omitida, no se puede posponer
            }
            Date fechaProgramada = toma.getFechaHoraProgramada();
            if (fechaProgramada == null) {
                continue;
            }
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(fechaProgramada);
            if (fechaToma.get(Calendar.YEAR) != ahora.get(Calendar.YEAR) ||
                fechaToma.get(Calendar.DAY_OF_YEAR) != ahora.get(Calendar.DAY_OF_YEAR)) {
                continue;
            }
            Calendar ventanaDesde = (Calendar) fechaToma.clone();
            ventanaDesde.add(Calendar.MINUTE, -Constants.MINUTOS_POSPONER_ANTES);
            Calendar ventanaHasta = (Calendar) fechaToma.clone();
            ventanaHasta.add(Calendar.HOUR_OF_DAY, Constants.HORAS_OMITIDA);
            if (toma.getPosposiciones() > 0) {
                ventanaHasta.add(Calendar.MINUTE, toma.getPosposiciones() * Constants.MINUTOS_POSPOSICION);
            }
            if (!ahora.before(ventanaDesde) && !ahora.after(ventanaHasta)) {
                long diff = Math.abs(ahora.getTimeInMillis() - fechaToma.getTimeInMillis());
                if (diff < minutosMinimos) {
                    minutosMinimos = diff;
                    candidata = toma;
                }
            }
        }
        return candidata;
    }
    
    /**
     * Marca automáticamente como omitidas las tomas que pasaron las 01:01hs sin ser tomadas.
     * Solo marca tomas del día actual que YA PASARON y no fueron tomadas.
     * NO marca tomas futuras.
     * 
     * Este método se utiliza para limpiar tomas del día anterior después de las 01:01hs.
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
     * Limpia las tomas del día anterior.
     * Elimina todas las tomas programadas cuya fecha es anterior al día actual.
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

