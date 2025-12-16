package com.controlmedicamentos.myapplication.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import androidx.core.app.NotificationCompat;

import com.controlmedicamentos.myapplication.MainActivity;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.receivers.TomaActionReceiver;
import com.controlmedicamentos.myapplication.utils.Constants;

import java.util.Calendar;

/**
 * Servicio para manejar notificaciones de medicamentos
 */
public class NotificationService {
    private static final String CHANNEL_ID = "medicamentos_channel";
    private static final String CHANNEL_NAME = "Recordatorios de Medicamentos";
    private static final String CHANNEL_DESCRIPTION = "Notificaciones para recordar tomar medicamentos";
    
    private Context context;
    private NotificationManager notificationManager;
    private SharedPreferences preferences;
    
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = context.getSharedPreferences("ControlMedicamentos", Context.MODE_PRIVATE);
        createNotificationChannel();
    }
    
    /**
     * Crea el canal de notificaciones (requerido para Android 8+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            // Configurar sonido según preferencias
            boolean sonidoHabilitado = preferences.getBoolean("sonido", true);
            if (sonidoHabilitado) {
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
                channel.setSound(defaultSoundUri, audioAttributes);
            } else {
                channel.setSound(null, null);
            }
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Envía una notificación de recordatorio para tomar un medicamento
     */
    public void enviarNotificacionMedicamento(Medicamento medicamento, String horario) {
        // Verificar si las notificaciones están habilitadas
        boolean notificacionesHabilitadas = preferences.getBoolean("notificaciones", true);
        if (!notificacionesHabilitadas) {
            return;
        }
        
        // Crear intent para abrir MainActivity cuando se toque la notificación
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medicamento)
            .setContentTitle("Es hora de tomar tu medicamento")
            .setContentText(medicamento.getNombre() + " - " + horario)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Recordatorio: " + medicamento.getNombre() + "\n" +
                        "Presentación: " + medicamento.getPresentacion() + "\n" +
                        "Hora: " + horario))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(medicamento.getColor());
        
        // Configurar vibración
        boolean vibracionHabilitada = preferences.getBoolean("vibracion", true);
        if (vibracionHabilitada && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    if (vibrator != null && vibrator.hasVibrator()) {
                        // Patrón de vibración estándar
                        long[] pattern = Constants.PATRON_VIBRACION_ESTANDAR;
                        builder.setVibrate(pattern);
                    }
                }
            } catch (Exception e) {
                // Si no hay vibrator disponible, continuar sin vibración
            }
        } else if (vibracionHabilitada) {
            // Para versiones anteriores
            long[] pattern = {0, 500, 500, 500};
            builder.setVibrate(pattern);
        } else {
            builder.setVibrate(null);
        }
        
        // Configurar sonido
        boolean sonidoHabilitado = preferences.getBoolean("sonido", true);
        if (sonidoHabilitado) {
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(defaultSoundUri);
        } else {
            builder.setSound(null);
        }
        
        // Obtener número de repeticiones
        int repeticiones = preferences.getInt("repeticiones", 3);
        
        // Crear un ID único para la notificación (usando el ID del medicamento)
        int notificationId = medicamento.getId() != null ? medicamento.getId().hashCode() : 
                            (int) System.currentTimeMillis();
        
        // Enviar notificación
        notificationManager.notify(notificationId, builder.build());
        
        // Repetir notificación si está configurado
        if (repeticiones > 1) {
            repetirNotificacion(medicamento, horario, notificationId, repeticiones - 1, 0);
        }
    }
    
    /**
     * Repite una notificación después de un delay
     */
    private void repetirNotificacion(Medicamento medicamento, String horario, 
                                     int notificationId, int repeticionesRestantes, 
                                     int intentoActual) {
        if (repeticionesRestantes <= 0) {
            return;
        }
        
        // Esperar 5 minutos entre repeticiones
        long delayMillis = 5 * 60 * 1000; // 5 minutos
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Verificar si las notificaciones siguen habilitadas
            boolean notificacionesHabilitadas = preferences.getBoolean("notificaciones", true);
            if (!notificacionesHabilitadas) {
                return;
            }
            
            // Enviar notificación repetida
            enviarNotificacionMedicamento(medicamento, horario);
            
            // Continuar repitiendo si quedan repeticiones
            if (repeticionesRestantes > 1) {
                repetirNotificacion(medicamento, horario, notificationId, 
                                  repeticionesRestantes - 1, intentoActual + 1);
            }
        }, delayMillis);
    }
    
    /**
     * Envía una notificación de alerta amarilla (10 minutos antes)
     */
    public void enviarNotificacionAlertaAmarilla(Medicamento medicamento, String horario) {
        boolean notificacionesHabilitadas = preferences.getBoolean("notificaciones", true);
        if (!notificacionesHabilitadas) {
            return;
        }
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medicamento)
            .setContentTitle("Próxima toma en 10 minutos")
            .setContentText(medicamento.getNombre() + " - " + horario)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Recordatorio: " + medicamento.getNombre() + "\n" +
                        "Hora programada: " + horario + "\n" +
                        "Quedan 10 minutos"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFFEB3B); // Amarillo
        
        int notificationId = (medicamento.getId() != null ? medicamento.getId().hashCode() : 
                            (int) System.currentTimeMillis()) + 1000; // +1000 para diferenciar de alerta roja
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * Envía una notificación de alerta roja (horario exacto)
     */
    public void enviarNotificacionAlertaRoja(Medicamento medicamento, String horario) {
        boolean notificacionesHabilitadas = preferences.getBoolean("notificaciones", true);
        if (!notificacionesHabilitadas) {
            return;
        }
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medicamento)
            .setContentTitle("¡Es hora de tomar tu medicamento!")
            .setContentText(medicamento.getNombre() + " - " + horario)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Es hora de tomar: " + medicamento.getNombre() + "\n" +
                        "Hora: " + horario + "\n" +
                        "Por favor, marca la toma como completada"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFF0000); // Rojo
        
        // Agregar acciones: Posponer y Marcar como tomada
        Intent posponerIntent = new Intent(context, TomaActionReceiver.class);
        posponerIntent.setAction(TomaActionReceiver.ACTION_POSPONER);
        posponerIntent.putExtra(TomaActionReceiver.EXTRA_MEDICAMENTO_ID, medicamento.getId());
        posponerIntent.putExtra(TomaActionReceiver.EXTRA_HORARIO, horario);
        PendingIntent posponerPendingIntent = PendingIntent.getBroadcast(
            context,
            (medicamento.getId() != null ? medicamento.getId().hashCode() : 0) + 1,
            posponerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Intent marcarIntent = new Intent(context, TomaActionReceiver.class);
        marcarIntent.setAction(TomaActionReceiver.ACTION_MARCAR_TOMADA);
        marcarIntent.putExtra(TomaActionReceiver.EXTRA_MEDICAMENTO_ID, medicamento.getId());
        marcarIntent.putExtra(TomaActionReceiver.EXTRA_HORARIO, horario);
        PendingIntent marcarPendingIntent = PendingIntent.getBroadcast(
            context,
            (medicamento.getId() != null ? medicamento.getId().hashCode() : 0) + 2,
            marcarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.ic_medicamento, "Posponer (10 min)", posponerPendingIntent);
        builder.addAction(R.drawable.ic_medicamento, "Marcar como tomada", marcarPendingIntent);
        
        // Configurar vibración más intensa para alerta roja
        boolean vibracionHabilitada = preferences.getBoolean("vibracion", true);
        if (vibracionHabilitada) {
            long[] pattern = Constants.PATRON_VIBRACION_INTENSO;
            builder.setVibrate(pattern);
        }
        
        int notificationId = medicamento.getId() != null ? medicamento.getId().hashCode() : 
                            (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * Cancela todas las notificaciones de un medicamento
     */
    public void cancelarNotificacionesMedicamento(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            return;
        }
        int notificationId = medicamento.getId().hashCode();
        notificationManager.cancel(notificationId);
        notificationManager.cancel(notificationId + 1000); // También cancelar alerta amarilla
    }
}

