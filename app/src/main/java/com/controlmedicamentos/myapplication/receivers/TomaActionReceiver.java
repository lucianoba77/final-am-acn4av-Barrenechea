package com.controlmedicamentos.myapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.controlmedicamentos.myapplication.MainActivity;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;

import java.util.Date;

/**
 * Receiver para manejar acciones de las notificaciones (posponer, marcar como tomada)
 */
public class TomaActionReceiver extends BroadcastReceiver {
    private static final String TAG = "TomaActionReceiver";
    public static final String ACTION_POSPONER = "com.controlmedicamentos.myapplication.POSPONER";
    public static final String ACTION_MARCAR_TOMADA = "com.controlmedicamentos.myapplication.MARCAR_TOMADA";
    public static final String EXTRA_MEDICAMENTO_ID = "medicamento_id";
    public static final String EXTRA_HORARIO = "horario";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String medicamentoId = intent.getStringExtra(EXTRA_MEDICAMENTO_ID);
        String horario = intent.getStringExtra(EXTRA_HORARIO);
        
        if (medicamentoId == null || horario == null) {
            Log.e(TAG, "Datos incompletos en la acción");
            return;
        }
        
        TomaTrackingService trackingService = new TomaTrackingService(context);
        
        if (ACTION_POSPONER.equals(action)) {
            Log.d(TAG, "Posponer toma: " + medicamentoId + " - " + horario);
            boolean pospuesta = trackingService.posponerToma(medicamentoId, horario);
            if (!pospuesta) {
                Log.w(TAG, "No se pudo posponer la toma (máximo 3 veces alcanzado)");
            }
        } else if (ACTION_MARCAR_TOMADA.equals(action)) {
            Log.d(TAG, "Marcar toma como tomada: " + medicamentoId + " - " + horario);
            
            // Obtener el medicamento para registrar la toma en Firestore
            FirebaseService firebaseService = new FirebaseService();
            firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    if (result instanceof Medicamento) {
                        Medicamento medicamento = (Medicamento) result;
                        
                        // Marcar como tomada en el tracking service
                        trackingService.marcarTomaComoTomada(medicamentoId, horario);
                        
                        // Registrar la toma en Firestore
                        Toma toma = new Toma();
                        toma.setMedicamentoId(medicamento.getId());
                        toma.setMedicamentoNombre(medicamento.getNombre());
                        Date ahora = new Date();
                        toma.setFechaHoraProgramada(ahora);
                        toma.setFechaHoraTomada(ahora);
                        toma.setEstado(Toma.EstadoToma.TOMADA);
                        toma.setObservaciones("Registrada desde notificación");
                        
                        firebaseService.guardarToma(toma, new FirebaseService.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                // Actualizar stock del medicamento
                                medicamento.consumirDosis();
                                if (medicamento.estaAgotado()) {
                                    medicamento.pausarMedicamento();
                                }
                                
                                // Pasar contexto para gestionar Google Calendar si se pausa
                                firebaseService.actualizarMedicamento(medicamento, context, new FirebaseService.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object updateResult) {
                                        Log.d(TAG, "Toma registrada y medicamento actualizado");
                                    }
                                    
                                    @Override
                                    public void onError(Exception exception) {
                                        Log.e(TAG, "Error al actualizar medicamento", exception);
                                    }
                                });
                            }
                            
                            @Override
                            public void onError(Exception exception) {
                                Log.e(TAG, "Error al registrar toma en Firestore", exception);
                            }
                        });
                    }
                }
                
                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error al obtener medicamento", exception);
                }
            });
            
            // Abrir MainActivity para que el usuario vea la actualización
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(mainIntent);
        }
    }
}

