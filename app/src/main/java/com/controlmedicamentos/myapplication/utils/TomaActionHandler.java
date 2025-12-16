package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.widget.Toast;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import java.util.Date;
import java.util.List;

/**
 * Clase de utilidad para manejar acciones relacionadas con tomas de medicamentos.
 * Centraliza la lógica de marcar tomas como realizadas y posponer tomas.
 */
public class TomaActionHandler {

    private final Context context;
    private final FirebaseService firebaseService;
    private final TomaTrackingService tomaTrackingService;

    /**
     * Callback para notificar el resultado de acciones de tomas.
     */
    public interface TomaActionCallback {
        /**
         * Se llama cuando la acción se completa exitosamente.
         * 
         * @param medicamento El medicamento actualizado.
         * @param completoTodasLasTomas true si completó todas las tomas del día.
         * @param tratamientoCompletado true si el tratamiento se completó.
         */
        void onSuccess(Medicamento medicamento, boolean completoTodasLasTomas, boolean tratamientoCompletado);
        
        /**
         * Se llama cuando ocurre un error.
         * 
         * @param exception La excepción que ocurrió.
         */
        void onError(Exception exception);
    }

    /**
     * Constructor.
     * 
     * @param context El contexto de la aplicación.
     * @param firebaseService El servicio de Firebase.
     * @param tomaTrackingService El servicio de tracking de tomas.
     */
    public TomaActionHandler(Context context,
                            FirebaseService firebaseService,
                            TomaTrackingService tomaTrackingService) {
        this.context = context;
        this.firebaseService = firebaseService;
        this.tomaTrackingService = tomaTrackingService;
    }

    /**
     * Marca una toma como realizada.
     * 
     * @param medicamento El medicamento para el cual registrar la toma.
     * @param callback El callback para notificar el resultado.
     */
    public void marcarTomaComoTomada(Medicamento medicamento, TomaActionCallback callback) {
        if (!ValidationUtils.isValidMedicamento(medicamento)) {
            if (callback != null) {
                callback.onError(new Exception("Medicamento inválido"));
            }
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            if (callback != null) {
                callback.onError(new Exception("No hay conexión a internet"));
            }
            return;
        }

        // Obtener la toma programada más próxima válida
        TomaProgramada tomaProxima = tomaTrackingService.obtenerTomaProximaValida(medicamento.getId());
        
        if (tomaProxima == null) {
            // Intentar obtener cualquier toma programada para validar
            List<TomaProgramada> tomas = tomaTrackingService.obtenerTomasMedicamento(medicamento.getId());
            if (tomas == null || tomas.isEmpty()) {
                if (callback != null) {
                    callback.onError(new Exception("No hay tomas programadas para este medicamento"));
                }
                return;
            }
            
            // Buscar la primera toma no tomada para validar
            TomaProgramada primeraToma = null;
            for (TomaProgramada toma : tomas) {
                if (!toma.isTomada()) {
                    primeraToma = toma;
                    break;
                }
            }
            
            if (primeraToma == null) {
                if (callback != null) {
                    callback.onError(new Exception("Todas las tomas de este medicamento ya fueron marcadas"));
                }
                return;
            }
            
            // Validar si se puede marcar
            String error = tomaTrackingService.validarPuedeMarcarToma(medicamento.getId(), primeraToma.getHorario());
            if (error != null) {
                if (callback != null) {
                    callback.onError(new Exception(error));
                }
                return;
            }
            
            tomaProxima = primeraToma;
        }
        
        // Crear referencia final para usar en el callback
        final TomaProgramada tomaProximaFinal = tomaProxima;
        
        // Validar una vez más antes de proceder
        String error = tomaTrackingService.validarPuedeMarcarToma(medicamento.getId(), tomaProximaFinal.getHorario());
        if (error != null) {
            if (callback != null) {
                callback.onError(new Exception(error));
            }
            return;
        }
        
        // Guardar estado anterior para rollback
        final int stockAnterior = medicamento.getStockActual();
        final int diasRestantesAnteriores = medicamento.getDiasRestantesDuracion();
        final boolean estabaPausado = medicamento.isPausado();

        // Actualizar medicamento
        medicamento.consumirDosis();
        final boolean tratamientoCompletado = medicamento.estaAgotado();
        if (tratamientoCompletado) {
            medicamento.pausarMedicamento();
        }

        // Crear objeto Toma
        Date fechaHoraProgramada = tomaProximaFinal.getFechaHoraProgramada();
        Date ahora = new Date();
        
        Toma toma = new Toma();
        toma.setMedicamentoId(medicamento.getId());
        toma.setMedicamentoNombre(medicamento.getNombre());
        toma.setFechaHoraProgramada(fechaHoraProgramada != null ? fechaHoraProgramada : ahora);
        toma.setFechaHoraTomada(ahora);
        toma.setEstado(Toma.EstadoToma.TOMADA);
        toma.setObservaciones("Registrada desde el panel principal");

        // Guardar toma en Firestore
        firebaseService.guardarToma(toma, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Marcar la toma como tomada en el tracking service
                if (tomaProximaFinal != null && tomaProximaFinal.getHorario() != null) {
                    tomaTrackingService.marcarTomaComoTomada(medicamento.getId(), tomaProximaFinal.getHorario());
                }
                
                // Actualizar medicamento en Firestore (pasar contexto para gestionar Google Calendar)
                firebaseService.actualizarMedicamento(medicamento, context, new FirebaseService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object updateResult) {
                        // Verificar si el medicamento completó todas sus tomas del día
                        boolean completoTodasLasTomas = tomaTrackingService.completoTodasLasTomasDelDia(medicamento.getId());
                        
                        if (callback != null) {
                            callback.onSuccess(medicamento, completoTodasLasTomas, tratamientoCompletado);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        // Revertir cambios
                        revertirCambios(medicamento, stockAnterior, diasRestantesAnteriores, estabaPausado);
                        Logger.e("TomaActionHandler", "Error al actualizar medicamento", exception);
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                // Revertir cambios
                revertirCambios(medicamento, stockAnterior, diasRestantesAnteriores, estabaPausado);
                Logger.e("TomaActionHandler", "Error al registrar la toma", exception);
                if (callback != null) {
                    callback.onError(exception);
                }
            }
        });
    }

    /**
     * Pospone una toma.
     * 
     * @param medicamento El medicamento.
     * @param horarioToma El horario de la toma a posponer.
     * @return true si se pudo posponer, false si ya se alcanzó el máximo.
     */
    public boolean posponerToma(Medicamento medicamento, String horarioToma) {
        if (medicamento == null || horarioToma == null) {
            return false;
        }
        
        boolean pospuesta = tomaTrackingService.posponerToma(medicamento.getId(), horarioToma);
        
        if (pospuesta) {
            int posposicionesRestantes = obtenerPosposicionesRestantes(medicamento, horarioToma);
            Toast.makeText(context, 
                "Toma pospuesta " + Constants.MINUTOS_POSPOSICION + " minutos. Quedan " + 
                posposicionesRestantes + " posposiciones disponibles", 
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, 
                "No se puede posponer más. Máximo " + Constants.MAX_POSPOSICIONES + 
                " posposiciones alcanzado. La toma se considera omitida.", 
                Toast.LENGTH_LONG).show();
        }
        
        return pospuesta;
    }

    /**
     * Obtiene las posposiciones restantes para una toma.
     */
    private int obtenerPosposicionesRestantes(Medicamento medicamento, String horario) {
        List<TomaProgramada> tomas = tomaTrackingService.obtenerTomasMedicamento(medicamento.getId());
        if (tomas == null) {
            return 0;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario)) {
                return Constants.MAX_POSPOSICIONES - toma.getPosposiciones();
            }
        }
        
        return 0;
    }

    /**
     * Revierte los cambios en un medicamento.
     */
    private void revertirCambios(Medicamento medicamento,
                                 int stockAnterior,
                                 int diasRestantesAnteriores,
                                 boolean estabaPausado) {
        medicamento.setStockActual(stockAnterior);
        medicamento.setDiasRestantesDuracion(diasRestantesAnteriores);
        medicamento.setPausado(estabaPausado);
    }
}

