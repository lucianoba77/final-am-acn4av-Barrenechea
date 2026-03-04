package com.controlmedicamentos.myapplication.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.receivers.AlarmReceiver;

import java.util.Calendar;
import java.util.List;

/**
 * Utilidad para programar y cancelar alarmas de medicamentos
 */
public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private Context context;
    private AlarmManager alarmManager;
    
    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Programa todas las alarmas para un medicamento
     */
    public void programarAlarmasMedicamento(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            Log.e(TAG, "Medicamento inválido para programar alarmas");
            return;
        }
        
        if (!medicamento.isActivo() || medicamento.isPausado()) {
            Log.d(TAG, "Medicamento no activo o pausado: " + medicamento.getNombre());
            return;
        }
        // Comprobar si hay algún horario en algún día (programación personalizada o fija)
        boolean tieneAlgunHorario = false;
        for (int d = 0; d <= 6; d++) {
            if (!medicamento.getHorariosParaDiaSemana(d).isEmpty()) {
                tieneAlgunHorario = true;
                break;
            }
        }
        if (!tieneAlgunHorario) {
            Log.d(TAG, "Medicamento sin horarios en ningún día: " + medicamento.getNombre());
            return;
        }
        
        cancelarAlarmasMedicamento(medicamento);
        
        Calendar ahora = Calendar.getInstance();
        final int DIAS_PROGRAMADOS = 7;
        
        for (int diaOffset = 0; diaOffset <= DIAS_PROGRAMADOS; diaOffset++) {
            Calendar fecha = Calendar.getInstance();
            fecha.add(Calendar.DAY_OF_YEAR, diaOffset);
            int diaSemana = fecha.get(Calendar.DAY_OF_WEEK) - 1; // 0=Domingo .. 6=Sábado
            List<String> horariosDia = medicamento.getHorariosParaDiaSemana(diaSemana);
            if (horariosDia == null || horariosDia.isEmpty()) continue;
            
            for (int i = 0; i < horariosDia.size(); i++) {
                String horario = horariosDia.get(i);
                String[] partes = horario.split(":");
                if (partes.length != 2) {
                    Log.e(TAG, "Formato de horario inválido: " + horario);
                    continue;
                }
                try {
                    int hora = Integer.parseInt(partes[0]);
                    int minuto = Integer.parseInt(partes[1]);
                    Calendar horarioToma = (Calendar) fecha.clone();
                    horarioToma.set(Calendar.HOUR_OF_DAY, hora);
                    horarioToma.set(Calendar.MINUTE, minuto);
                    horarioToma.set(Calendar.SECOND, 0);
                    horarioToma.set(Calendar.MILLISECOND, 0);
                    if (diaOffset == 0 && horarioToma.before(ahora)) continue;
                    
                    Calendar horarioAlertaAmarilla = (Calendar) horarioToma.clone();
                    horarioAlertaAmarilla.add(Calendar.MINUTE, -10);
                    boolean programarAmarilla = (diaOffset > 0) || !horarioAlertaAmarilla.before(ahora);
                    if (programarAmarilla) {
                        Intent intentAmarilla = AlarmReceiver.createIntent(
                            context, medicamento.getId(), horario, AlarmReceiver.TIPO_ALERTA_AMARILLA);
                        int requestCodeAmarilla = generarRequestCode(medicamento.getId(), i, diaOffset, true);
                        PendingIntent pendingIntentAmarilla = PendingIntent.getBroadcast(
                            context, requestCodeAmarilla, intentAmarilla,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        try {
                            programarAlarma(horarioAlertaAmarilla.getTimeInMillis(), pendingIntentAmarilla);
                        } catch (Exception e) {
                            if (esLimiteAlarmasAlcanzado(e)) break;
                            Log.e(TAG, "Error al programar alerta amarilla", e);
                        }
                    }
                    
                    Intent intentRoja = AlarmReceiver.createIntent(
                        context, medicamento.getId(), horario, AlarmReceiver.TIPO_ALERTA_ROJA);
                    int requestCodeRoja = generarRequestCode(medicamento.getId(), i, diaOffset, false);
                    PendingIntent pendingIntentRoja = PendingIntent.getBroadcast(
                        context, requestCodeRoja, intentRoja,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    try {
                        programarAlarma(horarioToma.getTimeInMillis(), pendingIntentRoja);
                    } catch (Exception e) {
                        if (esLimiteAlarmasAlcanzado(e)) break;
                        Log.e(TAG, "Error al programar alarma roja", e);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error al parsear horario: " + horario, e);
                }
            }
        }
    }
    
    /**
     * Programa alarmas recurrentes para los próximos días
     * OPTIMIZADO: Solo programa alarmas para los próximos 30 días para evitar exceder el límite de 500 alarmas
     */
    private void programarAlarmasRecurrentes(Medicamento medicamento, String horario, 
                                            int indiceHorario, Calendar primeraAlarma) {
        // LIMITAR: Solo programar alarmas para los próximos 7 días
        // Cuando se ejecute una alarma, se programará la siguiente en el ciclo
        // Esto evita exceder el límite de 500 alarmas concurrentes de Android
        // REDUCIDO de 30 a 7 días para evitar el límite con muchos medicamentos
        final int DIAS_PROGRAMADOS = 7;
        
        // Si el medicamento tiene días de tratamiento definidos y no es crónico
        if (medicamento.getDiasTratamiento() > 0) {
            // Calcular cuántos días programar (el mínimo entre los días restantes y el límite)
            int diasRestantes = medicamento.getDiasTratamiento();
            
            // Calcular días desde el inicio del tratamiento hasta hoy
            if (medicamento.getFechaInicioTratamiento() != null) {
                Calendar hoy = Calendar.getInstance();
                Calendar inicio = Calendar.getInstance();
                inicio.setTime(medicamento.getFechaInicioTratamiento());
                long diffMillis = hoy.getTimeInMillis() - inicio.getTimeInMillis();
                int diasTranscurridos = (int) (diffMillis / (1000 * 60 * 60 * 24));
                diasRestantes = Math.max(0, medicamento.getDiasTratamiento() - diasTranscurridos);
            }
            
            int diasProgramar = Math.min(diasRestantes, DIAS_PROGRAMADOS);
            
            // Solo programar si quedan días de tratamiento
            if (diasProgramar <= 0) {
                Log.d(TAG, "No quedan días de tratamiento para programar alarmas: " + medicamento.getNombre());
                return;
            }
            
            for (int dia = 1; dia <= diasProgramar; dia++) {
                Calendar horarioToma = (Calendar) primeraAlarma.clone();
                horarioToma.add(Calendar.DAY_OF_YEAR, dia);
                
                // Programar alarma 10 minutos antes (alerta amarilla)
                Calendar horarioAlertaAmarilla = (Calendar) horarioToma.clone();
                horarioAlertaAmarilla.add(Calendar.MINUTE, -10);
                
                Intent intentAmarilla = AlarmReceiver.createIntent(
                    context, medicamento.getId(), horario, AlarmReceiver.TIPO_ALERTA_AMARILLA);
                int requestCodeAmarilla = generarRequestCode(medicamento.getId(), indiceHorario, dia, true);
                PendingIntent pendingIntentAmarilla = PendingIntent.getBroadcast(
                    context,
                    requestCodeAmarilla,
                    intentAmarilla,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                try {
                    programarAlarma(horarioAlertaAmarilla.getTimeInMillis(), pendingIntentAmarilla);
                } catch (Exception e) {
                    Log.e(TAG, "Error al programar alerta amarilla para día " + dia + ": " + medicamento.getNombre(), e);
                    // Si hay error (por ejemplo, límite alcanzado), detener la programación
                    if (esLimiteAlarmasAlcanzado(e)) {
                        Log.w(TAG, "Límite de alarmas alcanzado. Deteniendo programación de alarmas adicionales.");
                        break;
                    }
                }
                
                // Programar alarma en el horario exacto (alerta roja)
                Intent intentRoja = AlarmReceiver.createIntent(
                    context, medicamento.getId(), horario, AlarmReceiver.TIPO_ALERTA_ROJA);
                int requestCodeRoja = generarRequestCode(medicamento.getId(), indiceHorario, dia, false);
                PendingIntent pendingIntentRoja = PendingIntent.getBroadcast(
                    context,
                    requestCodeRoja,
                    intentRoja,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                try {
                    programarAlarma(horarioToma.getTimeInMillis(), pendingIntentRoja);
                } catch (Exception e) {
                    Log.e(TAG, "Error al programar alarma para día " + dia + ": " + medicamento.getNombre(), e);
                    // Si hay error (por ejemplo, límite alcanzado), detener la programación
                    if (esLimiteAlarmasAlcanzado(e)) {
                        Log.w(TAG, "Límite de alarmas alcanzado. Deteniendo programación de alarmas adicionales.");
                        break;
                    }
                }
            }
        } else {
            // Medicamento crónico: programar solo para los próximos 30 días
            // Cuando se ejecute una alarma, se programará automáticamente la siguiente
            for (int dia = 1; dia <= DIAS_PROGRAMADOS; dia++) {
                Calendar horarioToma = (Calendar) primeraAlarma.clone();
                horarioToma.add(Calendar.DAY_OF_YEAR, dia);
                
                // Programar alarma 10 minutos antes (alerta amarilla)
                Calendar horarioAlertaAmarilla = (Calendar) horarioToma.clone();
                horarioAlertaAmarilla.add(Calendar.MINUTE, -10);
                
                Intent intentAmarilla = AlarmReceiver.createIntent(
                    context, medicamento.getId(), horario, AlarmReceiver.TIPO_ALERTA_AMARILLA);
                int requestCodeAmarilla = generarRequestCode(medicamento.getId(), indiceHorario, dia, true);
                PendingIntent pendingIntentAmarilla = PendingIntent.getBroadcast(
                    context,
                    requestCodeAmarilla,
                    intentAmarilla,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                try {
                    programarAlarma(horarioAlertaAmarilla.getTimeInMillis(), pendingIntentAmarilla);
                } catch (Exception e) {
                    Log.e(TAG, "Error al programar alerta amarilla para día " + dia + ": " + medicamento.getNombre(), e);
                    // Si hay error (por ejemplo, límite alcanzado), detener la programación
                    if (esLimiteAlarmasAlcanzado(e)) {
                        Log.w(TAG, "Límite de alarmas alcanzado. Deteniendo programación de alarmas adicionales.");
                        break;
                    }
                }
                
                // Programar alarma en el horario exacto (alerta roja)
                Intent intentRoja = AlarmReceiver.createIntent(
                    context, medicamento.getId(), horario, AlarmReceiver.TIPO_ALERTA_ROJA);
                int requestCodeRoja = generarRequestCode(medicamento.getId(), indiceHorario, dia, false);
                PendingIntent pendingIntentRoja = PendingIntent.getBroadcast(
                    context,
                    requestCodeRoja,
                    intentRoja,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                try {
                    programarAlarma(horarioToma.getTimeInMillis(), pendingIntentRoja);
                } catch (Exception e) {
                    Log.e(TAG, "Error al programar alarma para día " + dia + ": " + medicamento.getNombre(), e);
                    // Si hay error (por ejemplo, límite alcanzado), detener la programación
                    if (esLimiteAlarmasAlcanzado(e)) {
                        Log.w(TAG, "Límite de alarmas alcanzado. Deteniendo programación de alarmas adicionales.");
                        break;
                    }
                }
            }
        }
        
        Log.d(TAG, "Alarmas recurrentes programadas (" + DIAS_PROGRAMADOS + " días) para: " + medicamento.getNombre() + 
              " horario " + horario);
    }
    
    /**
     * Cancela todas las alarmas de un medicamento
     */
    public void cancelarAlarmasMedicamento(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            return;
        }
        
        int maxHorarios = 0;
        for (int d = 0; d <= 6; d++) {
            int n = medicamento.getHorariosParaDiaSemana(d).size();
            if (n > maxHorarios) maxHorarios = n;
        }
        if (maxHorarios == 0 && medicamento.getHorariosTomas() != null) {
            maxHorarios = medicamento.getHorariosTomas().size();
        }
        if (maxHorarios == 0) maxHorarios = 8;
        
        final int MAX_DIAS_CANCELAR = 31;
        List<String> horariosRef = medicamento.getHorariosTomas();
        if (horariosRef == null) horariosRef = new java.util.ArrayList<>();
        
        for (int i = 0; i < maxHorarios; i++) {
            String horarioRef = (i < horariosRef.size()) ? horariosRef.get(i) : "00:00";
            for (int dia = 0; dia <= MAX_DIAS_CANCELAR; dia++) {
                int requestCodeAmarilla = generarRequestCode(medicamento.getId(), i, dia, true);
                Intent intentAmarilla = AlarmReceiver.createIntent(
                    context, medicamento.getId(), horarioRef, AlarmReceiver.TIPO_ALERTA_AMARILLA);
                PendingIntent pendingIntentAmarilla = PendingIntent.getBroadcast(
                    context,
                    requestCodeAmarilla,
                    intentAmarilla,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntentAmarilla);
                pendingIntentAmarilla.cancel();
                
                int requestCodeRoja = generarRequestCode(medicamento.getId(), i, dia, false);
                Intent intentRoja = AlarmReceiver.createIntent(
                    context, medicamento.getId(), horarioRef, AlarmReceiver.TIPO_ALERTA_ROJA);
                PendingIntent pendingIntentRoja = PendingIntent.getBroadcast(
                    context,
                    requestCodeRoja,
                    intentRoja,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntentRoja);
                pendingIntentRoja.cancel();
            }
        }
        
        Log.d(TAG, "Alarmas canceladas para: " + medicamento.getNombre());
        
        // También cancelar notificaciones pendientes
        com.controlmedicamentos.myapplication.services.NotificationService notificationService = 
            new com.controlmedicamentos.myapplication.services.NotificationService(context);
        notificationService.cancelarNotificacionesMedicamento(medicamento);
    }
    
    /**
     * Verifica si una excepción indica que se alcanzó el límite de alarmas del sistema.
     * Verifica tanto la excepción directa como su causa (si está envuelta en RuntimeException).
     * 
     * @param e La excepción a verificar
     * @return true si la excepción indica que se alcanzó el límite de alarmas
     */
    private boolean esLimiteAlarmasAlcanzado(Exception e) {
        if (e == null) {
            return false;
        }
        
        // Verificar el mensaje de la excepción directa
        String mensaje = e.getMessage();
        if (mensaje != null && mensaje.contains("Maximum limit")) {
            return true;
        }
        
        // Si es RuntimeException, verificar la causa (excepción original)
        if (e instanceof RuntimeException) {
            Throwable causa = e.getCause();
            if (causa != null) {
                String mensajeCausa = causa.getMessage();
                if (mensajeCausa != null && mensajeCausa.contains("Maximum limit")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Programa una alarma usando el método apropiado según la versión de Android
     */
    private void programarAlarma(long timeInMillis, PendingIntent pendingIntent) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                );
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                );
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                );
            }
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Maximum limit")) {
                Log.e(TAG, "❌ LÍMITE DE ALARMAS ALCANZADO (500). No se pueden programar más alarmas.", e);
                throw e; // Re-lanzar para que el código que llama pueda manejarlo
            } else {
                Log.e(TAG, "Error al programar alarma", e);
                throw e;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado al programar alarma", e);
            // Si es un error de límite de alarmas, re-lanzarlo como IllegalStateException
            // para que sea detectado correctamente por el código que llama
            if (e.getMessage() != null && e.getMessage().contains("Maximum limit")) {
                IllegalStateException limiteException = new IllegalStateException("Maximum limit of concurrent alarms 500 reached", e);
                throw limiteException;
            }
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Genera un código único para el requestCode de la alarma
     */
    private int generarRequestCode(String medicamentoId, int indiceHorario) {
        return generarRequestCode(medicamentoId, indiceHorario, 0, false);
    }
    
    /**
     * Genera un código único para el requestCode de la alarma (con día)
     */
    private int generarRequestCode(String medicamentoId, int indiceHorario, int dia) {
        return generarRequestCode(medicamentoId, indiceHorario, dia, false);
    }
    
    /**
     * Genera un código único para el requestCode de la alarma (con día y tipo)
     */
    private int generarRequestCode(String medicamentoId, int indiceHorario, int dia, boolean esAlertaAmarilla) {
        // Usar hash del ID del medicamento + índice del horario + día + tipo
        int hash = medicamentoId.hashCode();
        int base = Math.abs(hash) + (indiceHorario * 10000) + (dia * 100);
        return esAlertaAmarilla ? base + 1 : base; // +1 para alerta amarilla
    }
}

