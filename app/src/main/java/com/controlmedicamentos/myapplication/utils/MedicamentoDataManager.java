package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Clase de utilidad para gestionar la carga y actualización de datos de medicamentos.
 * Centraliza la lógica de carga desde Firebase, listeners en tiempo real y ordenamiento.
 */
public class MedicamentoDataManager {

    private final FirebaseService firebaseService;
    private final TomaTrackingService tomaTrackingService;
    private final Context context;
    private ListenerRegistration medicamentosListener;
    private volatile boolean listenerYaActualizo = false; // volatile para evitar race conditions
    
    // Cache para ordenamiento: lista de medicamentos -> lista ordenada
    private List<Medicamento> ultimaListaOrdenada = null;
    private String ultimaFechaOrdenamiento = null;
    private int ultimosMinutosActuales = -1;

    /**
     * Callback para notificar cambios en los datos.
     */
    public interface DataCallback {
        /**
         * Se llama cuando los datos se cargan exitosamente.
         * 
         * @param medicamentos Lista de medicamentos filtrados para el dashboard.
         * @param todosLosMedicamentos Lista completa de medicamentos (para alertas de stock).
         */
        void onDataLoaded(List<Medicamento> medicamentos, List<Medicamento> todosLosMedicamentos);
        
        /**
         * Se llama cuando ocurre un error al cargar los datos.
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
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public MedicamentoDataManager(Context context, 
                                  FirebaseService firebaseService,
                                  TomaTrackingService tomaTrackingService) {
        if (context == null) {
            throw new IllegalArgumentException("Context no puede ser null");
        }
        if (firebaseService == null) {
            throw new IllegalArgumentException("FirebaseService no puede ser null");
        }
        if (tomaTrackingService == null) {
            throw new IllegalArgumentException("TomaTrackingService no puede ser null");
        }
        this.context = context;
        this.firebaseService = firebaseService;
        this.tomaTrackingService = tomaTrackingService;
    }

    /**
     * Carga los medicamentos activos desde Firebase.
     * 
     * @param progressBar El ProgressBar a ocultar cuando termine la carga (puede ser null).
     * @param callback El callback para notificar el resultado.
     */
    public void cargarMedicamentos(ProgressBar progressBar, DataCallback callback) {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, context.getString(R.string.msg_no_internet), Toast.LENGTH_LONG).show();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (callback != null) {
                callback.onError(new Exception(context.getString(R.string.msg_no_internet)));
            }
            return;
        }

        // Cargar medicamentos activos y tomas del usuario para sincronizar estado "tomada" con la DB
        Logger.d("MedicamentoDataManager", "Iniciando carga de medicamentos desde Firebase");
        firebaseService.obtenerMedicamentosActivos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                final List<Medicamento> todosLosMedicamentos = new ArrayList<>();
                if (result != null) {
                    for (Object o : result) {
                        if (o instanceof Medicamento) {
                            todosLosMedicamentos.add((Medicamento) o);
                        }
                    }
                }
                Logger.d("MedicamentoDataManager", "Medicamentos cargados: " + todosLosMedicamentos.size() + ", obteniendo tomas del usuario para sincronizar...");
                firebaseService.obtenerTomasUsuario(new FirebaseService.FirestoreListCallback() {
                    @Override
                    public void onSuccess(List<?> tomasResult) {
                        try {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            List<Toma> tomasUsuario = new ArrayList<>();
                            if (tomasResult != null) {
                                for (Object o : tomasResult) {
                                    if (o instanceof Toma) tomasUsuario.add((Toma) o);
                                }
                            }
                            Calendar hoy = Calendar.getInstance();
                            List<Toma> tomasHoyTomadas = new ArrayList<>();
                            for (Toma t : tomasUsuario) {
                                if (t.getEstado() != Toma.EstadoToma.TOMADA) continue;
                                Date f = t.getFechaHoraTomada() != null ? t.getFechaHoraTomada() : t.getFechaHoraProgramada();
                                if (f == null) continue;
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(f);
                                if (cal.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)) {
                                    tomasHoyTomadas.add(t);
                                }
                            }
                            Logger.d("MedicamentoDataManager", "Tomas de hoy ya tomadas en DB: " + tomasHoyTomadas.size());

                            List<Medicamento> medicamentosActivos = new ArrayList<>();
                            for (Medicamento med : todosLosMedicamentos) {
                                if (med.isActivo() && !med.isPausado()) {
                                    medicamentosActivos.add(med);
                                }
                            }
                            for (Medicamento med : medicamentosActivos) {
                                tomaTrackingService.inicializarTomasDia(med);
                            }
                            tomaTrackingService.sincronizarTomasTomadasDesdeFirestore(tomasHoyTomadas);
                            tomaTrackingService.marcarTomasOmitidasDespuesDe0101();

                            List<Medicamento> medicamentosParaDashboard = MedicamentoFilter.filtrarParaDashboard(
                                medicamentosActivos, tomaTrackingService
                            );
                            Logger.d("MedicamentoDataManager", "Dashboard: " + medicamentosParaDashboard.size() + " medicamentos (sincronizado con DB)");
                            if (callback != null) {
                                callback.onDataLoaded(medicamentosParaDashboard, todosLosMedicamentos);
                            }
                        } catch (Exception e) {
                            Logger.e("MedicamentoDataManager", "Error al procesar medicamentos/tomas", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Logger.e("MedicamentoDataManager", "Error al cargar tomas del usuario", exception);
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Logger.e("MedicamentoDataManager", "Error al cargar medicamentos", exception);
                if (callback != null) {
                    callback.onError(exception);
                }
            }
        });
    }

    /**
     * Configura un listener en tiempo real para actualizaciones de medicamentos.
     * 
     * @param callback El callback para notificar cambios.
     * @return El ListenerRegistration para poder removerlo después.
     */
    public ListenerRegistration configurarListenerTiempoReal(DataCallback callback) {
        try {
            Logger.d("MedicamentoDataManager", "Configurando listener de tiempo real");
            medicamentosListener = firebaseService.agregarListenerMedicamentos(
                new FirebaseService.FirestoreListCallback() {
                    @Override
                    public void onSuccess(List<?> result) {
                        try {
                            Logger.d("MedicamentoDataManager", "Listener: medicamentos recibidos: " + 
                                    (result != null ? result.size() : 0));
                            final List<Medicamento> todosLosMedicamentos = new ArrayList<>();
                            if (result != null) {
                                for (Object o : result) {
                                    if (o instanceof Medicamento) {
                                        todosLosMedicamentos.add((Medicamento) o);
                                    }
                                }
                            }
                            // Filtrar medicamentos activos: solo activos y no pausados
                            final List<Medicamento> medicamentosActivos = new ArrayList<>();
                            for (Medicamento med : todosLosMedicamentos) {
                                if (med.isActivo() && !med.isPausado()) {
                                    medicamentosActivos.add(med);
                                }
                            }
                            Logger.d("MedicamentoDataManager", "Listener: Medicamentos activos y no pausados: " + medicamentosActivos.size() + 
                                " de " + todosLosMedicamentos.size());
                            
                            // Inicializar tomas del día para cada medicamento activo
                            for (Medicamento med : medicamentosActivos) {
                                tomaTrackingService.inicializarTomasDia(med);
                            }
                            // Sincronizar con tomas ya registradas en Firestore (hoy, TOMADA)
                            firebaseService.obtenerTomasUsuario(new FirebaseService.FirestoreListCallback() {
                                @Override
                                public void onSuccess(List<?> tomasResult) {
                                    try {
                                        List<Toma> tomasUsuario = new ArrayList<>();
                                        if (tomasResult != null) {
                                            for (Object o : tomasResult) {
                                                if (o instanceof Toma) tomasUsuario.add((Toma) o);
                                            }
                                        }
                                        Calendar hoyCal = Calendar.getInstance();
                                        List<Toma> tomasHoyTomadas = new ArrayList<>();
                                        for (Toma t : tomasUsuario) {
                                            if (t.getEstado() != Toma.EstadoToma.TOMADA) continue;
                                            Date f = t.getFechaHoraTomada() != null ? t.getFechaHoraTomada() : t.getFechaHoraProgramada();
                                            if (f == null) continue;
                                            Calendar cal = Calendar.getInstance();
                                            cal.setTime(f);
                                            if (cal.get(Calendar.YEAR) == hoyCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == hoyCal.get(Calendar.DAY_OF_YEAR)) {
                                                tomasHoyTomadas.add(t);
                                            }
                                        }
                                        tomaTrackingService.sincronizarTomasTomadasDesdeFirestore(tomasHoyTomadas);
                                    } catch (Exception ignored) { }
                                    tomaTrackingService.marcarTomasOmitidasDespuesDe0101();
                                    List<Medicamento> medicamentosParaDashboard = MedicamentoFilter.filtrarParaDashboard(
                                        medicamentosActivos, tomaTrackingService
                                    );
                                    Logger.d("MedicamentoDataManager", "Listener: Dashboard después del filtro: " + medicamentosParaDashboard.size());
                                    listenerYaActualizo = true;
                                    if (callback != null) {
                                        callback.onDataLoaded(medicamentosParaDashboard, todosLosMedicamentos);
                                    }
                                }
                                @Override
                                public void onError(Exception exception) {
                                    tomaTrackingService.marcarTomasOmitidasDespuesDe0101();
                                    List<Medicamento> medicamentosParaDashboard = MedicamentoFilter.filtrarParaDashboard(
                                        medicamentosActivos, tomaTrackingService
                                    );
                                    listenerYaActualizo = true;
                                    if (callback != null) {
                                        callback.onDataLoaded(medicamentosParaDashboard, todosLosMedicamentos);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Logger.e("MedicamentoDataManager", "Error en callback del listener", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Logger.w("MedicamentoDataManager", "Error en listener de tiempo real", exception);
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                }
            );
            
            if (medicamentosListener == null) {
                Logger.w("MedicamentoDataManager", "Listener de medicamentos retornó null (usuario no autenticado?)");
            }
            
            return medicamentosListener;
        } catch (Exception e) {
            Logger.e("MedicamentoDataManager", "Error al configurar listener de tiempo real", e);
            return null;
        }
    }

    /**
     * Ordena los medicamentos por la próxima toma programada.
     * Los medicamentos con la toma más próxima aparecen primero.
     * Los medicamentos con tomas omitidas van al final.
     * 
     * @param medicamentos La lista de medicamentos a ordenar.
     * @return La lista ordenada.
     */
    public List<Medicamento> ordenarPorHorario(List<Medicamento> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return medicamentos;
        }

        // Obtener hora actual
        Calendar cal = Calendar.getInstance();
        int horaActual = cal.get(Calendar.HOUR_OF_DAY);
        int minutoActual = cal.get(Calendar.MINUTE);
        int minutosActuales = horaActual * 60 + minutoActual;
        
        // Verificar caché: si la lista es la misma y estamos en el mismo minuto, retornar caché
        String fechaHoy = obtenerFechaHoy();
        if (ultimaListaOrdenada != null &&
            Objects.equals(fechaHoy, ultimaFechaOrdenamiento) &&
            ultimosMinutosActuales == minutosActuales &&
            listasSonIguales(medicamentos, ultimaListaOrdenada)) {
            Logger.d("MedicamentoDataManager", "Usando caché de ordenamiento");
            return new ArrayList<>(ultimaListaOrdenada); // Retornar copia para evitar modificación
        }

        List<Medicamento> medicamentosOrdenados = crearListaOrdenadaPorHorario(medicamentos, minutosActuales);
        ultimaListaOrdenada = new ArrayList<>(medicamentosOrdenados);
        ultimaFechaOrdenamiento = fechaHoy;
        ultimosMinutosActuales = minutosActuales;
        return medicamentosOrdenados;
    }

    /**
     * Crea una copia de la lista de medicamentos ordenada por próxima toma (omitidas al final).
     */
    private List<Medicamento> crearListaOrdenadaPorHorario(List<Medicamento> medicamentos, int minutosActuales) {
        List<Medicamento> copia = new ArrayList<>(medicamentos);
        copia.sort((med1, med2) -> {
            boolean tieneOmitidas1 = tomaTrackingService.tieneTomasOmitidas(med1.getId());
            boolean tieneOmitidas2 = tomaTrackingService.tieneTomasOmitidas(med2.getId());
            if (tieneOmitidas1 && !tieneOmitidas2) return 1;
            if (!tieneOmitidas1 && tieneOmitidas2) return -1;
            String horario1 = obtenerHorarioProximaToma(med1);
            String horario2 = obtenerHorarioProximaToma(med2);
            if (horario1 == null && horario2 == null) return 0;
            if (horario1 == null) return 1;
            if (horario2 == null) return -1;
            int minutos1 = calcularMinutosHastaToma(horario1, minutosActuales);
            int minutos2 = calcularMinutosHastaToma(horario2, minutosActuales);
            return Integer.compare(minutos1, minutos2);
        });
        return copia;
    }
    
    /**
     * Compara dos listas de medicamentos para verificar si son iguales (mismos identificadores y orden).
     * @param lista1 Primera lista
     * @param lista2 Segunda lista
     * @return true si las listas tienen los mismos medicamentos en el mismo orden
     */
    private boolean listasSonIguales(List<Medicamento> lista1, List<Medicamento> lista2) {
        if (lista1 == null || lista2 == null) {
            return lista1 == lista2;
        }
        if (lista1.size() != lista2.size()) {
            return false;
        }
        for (int i = 0; i < lista1.size(); i++) {
            Medicamento m1 = lista1.get(i);
            Medicamento m2 = lista2.get(i);
            if (m1 == null || m2 == null) {
                if (m1 != m2) {
                    return false;
                }
            } else if (!m1.getId().equals(m2.getId())) {
                return false;
            }
        }
        return true;
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
     * Obtiene el horario de la próxima toma de un medicamento.
     */
    private String obtenerHorarioProximaToma(Medicamento medicamento) {
        List<com.controlmedicamentos.myapplication.models.TomaProgramada> tomas = 
            tomaTrackingService.obtenerTomasMedicamento(medicamento.getId());
        
        if (tomas == null || tomas.isEmpty()) {
            return null;
        }
        
        Calendar ahora = Calendar.getInstance();
        String horarioProximo = null;
        long minutosMinimos = Long.MAX_VALUE;
        
        for (com.controlmedicamentos.myapplication.models.TomaProgramada toma : tomas) {
            if (toma.isTomada() || 
                toma.getEstado() == com.controlmedicamentos.myapplication.models.TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                continue;
            }
            
            if (toma.getFechaHoraProgramada() == null) {
                continue;
            }
            
            Calendar fechaToma = Calendar.getInstance();
            fechaToma.setTime(toma.getFechaHoraProgramada());
            
            // Solo considerar tomas del día actual
            if (fechaToma.get(Calendar.YEAR) != ahora.get(Calendar.YEAR) ||
                fechaToma.get(Calendar.DAY_OF_YEAR) != ahora.get(Calendar.DAY_OF_YEAR)) {
                continue;
            }
            
            int hora = fechaToma.get(Calendar.HOUR_OF_DAY);
            int minuto = fechaToma.get(Calendar.MINUTE);
            int minutosHorario = hora * 60 + minuto;
            
            int minutosActuales = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE);
            
            long diferencia;
            if (minutosHorario >= minutosActuales) {
                diferencia = minutosHorario - minutosActuales;
            } else {
                diferencia = (24 * 60) - minutosActuales + minutosHorario;
            }
            
            if (diferencia < minutosMinimos) {
                minutosMinimos = diferencia;
                horarioProximo = toma.getHorario();
            }
        }
        
        return horarioProximo;
    }

    /**
     * Calcula los minutos hasta una toma.
     */
    private int calcularMinutosHastaToma(String horario, int minutosActuales) {
        try {
            String[] partes = horario.split(":");
            if (partes.length != 2) {
                return Integer.MAX_VALUE;
            }
            
            int hora = Integer.parseInt(partes[0]);
            int minuto = Integer.parseInt(partes[1]);
            int minutosHorario = hora * 60 + minuto;
            
            if (minutosHorario >= minutosActuales) {
                return minutosHorario - minutosActuales;
            } else {
                return (24 * 60) - minutosActuales + minutosHorario;
            }
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Remueve el listener de tiempo real.
     */
    public void removerListener() {
        if (medicamentosListener != null) {
            medicamentosListener.remove();
            medicamentosListener = null;
            listenerYaActualizo = false;
        }
    }

    /**
     * Verifica si el listener ya actualizó los datos.
     */
    public boolean isListenerYaActualizo() {
        return listenerYaActualizo;
    }
}

