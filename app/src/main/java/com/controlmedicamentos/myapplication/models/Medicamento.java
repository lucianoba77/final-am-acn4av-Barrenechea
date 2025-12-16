package com.controlmedicamentos.myapplication.models;

import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.utils.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Medicamento {
    private String id;
    private String nombre;
    private String presentacion; // comprimidos, jarabe, crema, etc.
    private int tomasDiarias;
    private String horarioPrimeraToma; // formato "HH:mm"
    private String afeccion;
    private int stockInicial;
    private int stockActual;
    private int color; // color del medicamento
    private int diasTratamiento; // -1 para crónico
    private boolean activo;
    private String detalles;
    private List<String> horariosTomas; // lista de horarios de todas las tomas
    private int iconoPresentacion; // ícono según la presentación

    // Nuevas propiedades para gestión avanzada
    private Date fechaVencimiento; // fecha de caducidad
    private Date fechaInicioTratamiento; // cuando comenzó el tratamiento
    private TipoStock tipoStock; // tipo de gestión de stock
    private int diasEstimadosDuracion; // días estimados de duración
    private int diasRestantesDuracion; // días restantes de duración
    private boolean pausado; // si está pausado (tratamiento completado pero no eliminado)

    // Enum para tipos de stock
    public enum TipoStock {
        UNIDADES_CONTABLES,    // Comprimidos, cápsulas (se puede contar exactamente)
        UNIDADES_APROXIMADAS,  // Jarabes, inyectables en frasco (estimación por días)
        TOPICO_DIAS,          // Cremas, pomadas (solo días estimados)
        LIQUIDO_ML            // Gotas, jarabes (por mililitros)
    }

    public Medicamento() {
        this.horariosTomas = new ArrayList<>();
        this.activo = true;
        this.pausado = false;
        this.tipoStock = TipoStock.UNIDADES_CONTABLES;
        this.fechaInicioTratamiento = new Date();
    }

    public Medicamento(String id, String nombre, String presentacion,
                       int tomasDiarias, String horarioPrimeraToma,
                       String afeccion, int stockInicial, int color, int diasTratamiento) {
        this.id = id;
        this.nombre = nombre;
        this.presentacion = presentacion;
        this.tomasDiarias = tomasDiarias;
        this.horarioPrimeraToma = horarioPrimeraToma;
        this.afeccion = afeccion;
        this.stockInicial = stockInicial;
        this.stockActual = stockInicial;
        this.color = color;
        this.diasTratamiento = diasTratamiento;
        this.activo = true;
        this.pausado = false;
        this.horariosTomas = new ArrayList<>();
        this.iconoPresentacion = android.R.drawable.ic_menu_info_details;
        this.fechaInicioTratamiento = new Date();

        // Asignar tipo de stock según presentación
        asignarTipoStock();

        // Configurar ícono según presentación
        asignarIconoPresentacion();

        // Generar horarios de tomas
        generarHorariosTomas();
    }

    // Asigna tipo de stock según la presentación
    private void asignarTipoStock() {
        switch (presentacion.toLowerCase()) {
            case "comprimidos":
            case "pastillas":
            case "cápsulas":
                this.tipoStock = TipoStock.UNIDADES_CONTABLES;
                break;
            case "jarabe":
            case "gotas":
                this.tipoStock = TipoStock.LIQUIDO_ML;
                break;
            case "crema":
            case "pomada":
            case "gel":
            case "ungüento":
                this.tipoStock = TipoStock.TOPICO_DIAS;
                break;
            case "inyección":
                // Para inyectables, asumimos que pueden ser contables o aproximados
                // Se puede configurar manualmente en el botiquín
                this.tipoStock = TipoStock.UNIDADES_APROXIMADAS;
                break;
            case "spray nasal":
            case "spray":
                this.tipoStock = TipoStock.UNIDADES_APROXIMADAS;
                break;
            default:
                this.tipoStock = TipoStock.UNIDADES_APROXIMADAS;
        }
    }

    // Configurar ícono según la presentación
    private void asignarIconoPresentacion() {
        switch (presentacion.toLowerCase()) {
            case "comprimidos":
            case "pastillas":
                this.iconoPresentacion = android.R.drawable.ic_menu_edit;
                break;
            case "jarabe":
                this.iconoPresentacion = android.R.drawable.ic_menu_help;
                break;
            case "crema":
                this.iconoPresentacion = android.R.drawable.ic_menu_preferences;
                break;
            case "spray nasal":
                this.iconoPresentacion = android.R.drawable.ic_menu_help;
                break;
            case "inyección":
                this.iconoPresentacion = android.R.drawable.ic_menu_send;
                break;
            default:
                this.iconoPresentacion = android.R.drawable.ic_menu_info_details;
        }
    }

    // Generar horarios de tomas basado en tomas diarias
    private void generarHorariosTomas() {
        horariosTomas.clear();

        // Si no hay tomas diarias, no generar horarios (medicamento ocasional)
        if (tomasDiarias <= 0) {
            return;
        }
        
        // Verificar que horarioPrimeraToma no sea null o vacío
        if (horarioPrimeraToma == null || horarioPrimeraToma.isEmpty()) {
            // Usar valor por defecto si no está establecido
            horarioPrimeraToma = Constants.HORARIO_INVALIDO;
        }

        // Calcular intervalo entre tomas (24 horas / tomas diarias)
        int intervaloHoras = 24 / tomasDiarias;

        // Parsear hora inicial
        try {
            String[] partesHora = horarioPrimeraToma.split(":");
            if (partesHora.length < 2) {
                // Si el formato no es correcto, usar valor por defecto
                partesHora = new String[]{"00", "00"};
            }
            int horaInicial = Integer.parseInt(partesHora[0]);
            int minutoInicial = Integer.parseInt(partesHora[1]);

            // Generar horarios
            for (int i = 0; i < tomasDiarias; i++) {
                int hora = (horaInicial + (i * intervaloHoras)) % 24;
                String horario = String.format("%02d:%02d", hora, minutoInicial);
                horariosTomas.add(horario);
            }
        } catch (Exception e) {
            // Si hay algún error al parsear, usar valor por defecto
            horarioPrimeraToma = Constants.HORARIO_INVALIDO;
            int horaInicial = 0;
            int minutoInicial = 0;
            for (int i = 0; i < tomasDiarias; i++) {
                int hora = (horaInicial + (i * intervaloHoras)) % 24;
                String horario = String.format("%02d:%02d", hora, minutoInicial);
                horariosTomas.add(horario);
            }
        }
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
        asignarIconoPresentacion();
        asignarTipoStock();
    }

    public int getTomasDiarias() {
        return tomasDiarias;
    }

    public void setTomasDiarias(int tomasDiarias) {
        this.tomasDiarias = tomasDiarias;
        generarHorariosTomas();
    }

    public String getHorarioPrimeraToma() {
        return horarioPrimeraToma;
    }

    public void setHorarioPrimeraToma(String horarioPrimeraToma) {
        this.horarioPrimeraToma = horarioPrimeraToma;
        generarHorariosTomas();
    }

    public String getAfeccion() {
        return afeccion;
    }

    public void setAfeccion(String afeccion) {
        this.afeccion = afeccion;
    }

    public int getStockInicial() {
        return stockInicial;
    }

    public void setStockInicial(int stockInicial) {
        this.stockInicial = stockInicial;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getDiasTratamiento() {
        return diasTratamiento;
    }

    public void setDiasTratamiento(int diasTratamiento) {
        this.diasTratamiento = diasTratamiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getDetalles() {
        return detalles;
    }

    public void setDetalles(String detalles) {
        this.detalles = detalles;
    }

    public List<String> getHorariosTomas() {
        return horariosTomas;
    }

    public void setHorariosTomas(List<String> horariosTomas) {
        this.horariosTomas = horariosTomas;
    }

    // Método para asignar icono según presentación
    public int getIconoPresentacion() {
        switch (presentacion.toLowerCase()) {
            case "comprimidos":
            case "cápsulas":
                return R.drawable.ic_pills;
            case "jarabe":
                return R.drawable.ic_syrup;
            case "crema":
            case "pomada":
                return R.drawable.ic_cream;
            case "spray nasal":
                return R.drawable.ic_spray;
            case "inyección":
                return R.drawable.ic_injection;
            case "gotas":
                return R.drawable.ic_drops;
            case "parche":
                return R.drawable.ic_patch;
            default:
                return R.drawable.ic_pills;
        }
    }

    public void setIconoPresentacion(int iconoPresentacion) {
        this.iconoPresentacion = iconoPresentacion;
    }

    // Nuevos getters y setters
    public Date getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(Date fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Date getFechaInicioTratamiento() {
        return fechaInicioTratamiento;
    }

    public void setFechaInicioTratamiento(Date fechaInicioTratamiento) {
        this.fechaInicioTratamiento = fechaInicioTratamiento;
    }

    public TipoStock getTipoStock() {
        return tipoStock;
    }

    public void setTipoStock(TipoStock tipoStock) {
        this.tipoStock = tipoStock;
    }

    public int getDiasEstimadosDuracion() {
        return diasEstimadosDuracion;
    }

    public void setDiasEstimadosDuracion(int diasEstimadosDuracion) {
        this.diasEstimadosDuracion = diasEstimadosDuracion;
    }

    public int getDiasRestantesDuracion() {
        return diasRestantesDuracion;
    }

    public void setDiasRestantesDuracion(int diasRestantesDuracion) {
        this.diasRestantesDuracion = diasRestantesDuracion;
    }

    public boolean isPausado() {
        return pausado;
    }

    public void setPausado(boolean pausado) {
        this.pausado = pausado;
    }

    // Métodos útiles
    public boolean esCronico() {
        return diasTratamiento == -1;
    }

    public int getDiasRestantesTratamiento() {
        if (esCronico()) {
            return -1; // Crónico, no tiene fin
        }
        // Aquí podrías implementar lógica para calcular días restantes
        // Por ahora retornamos un valor por defecto
        return diasTratamiento;
    }

    public int getPorcentajeStock() {
        switch (tipoStock) {
            case UNIDADES_CONTABLES:
                if (stockInicial <= 0) return 0;
                return (stockActual * 100) / stockInicial;
            case UNIDADES_APROXIMADAS:
            case TOPICO_DIAS:
            case LIQUIDO_ML:
                if (diasEstimadosDuracion <= 0) return 0;
                return (diasRestantesDuracion * 100) / diasEstimadosDuracion;
            default:
                return 0;
        }
    }

    public boolean necesitaReposicion() {
        return getPorcentajeStock() <= 20; // Menos del 20% de stock
    }

    public void consumirDosis() {
        switch (tipoStock) {
            case UNIDADES_CONTABLES:
                if (stockActual > 0) {
                    stockActual--;
                }
                break;
            case UNIDADES_APROXIMADAS:
            case TOPICO_DIAS:
            case LIQUIDO_ML:
                if (diasRestantesDuracion > 0) {
                    diasRestantesDuracion--;
                }
                break;
        }
    }

    public void agregarStock(int cantidad) {
        switch (tipoStock) {
            case UNIDADES_CONTABLES:
                stockActual += cantidad;
                break;
            case UNIDADES_APROXIMADAS:
            case TOPICO_DIAS:
            case LIQUIDO_ML:
                diasRestantesDuracion += cantidad;
                break;
        }
    }

    public boolean estaAgotado() {
        switch (tipoStock) {
            case UNIDADES_CONTABLES:
                return stockActual <= 0;
            case UNIDADES_APROXIMADAS:
            case TOPICO_DIAS:
            case LIQUIDO_ML:
                return diasRestantesDuracion <= 0;
            default:
                return false;
        }
    }

    public boolean estaVencido() {
        if (fechaVencimiento == null) {
            return false;
        }
        return new Date().after(fechaVencimiento);
    }

    public boolean tieneTomasPendientes() {
        // Lógica para verificar si tiene tomas pendientes
        // Por ahora retornam true si está activo
        return activo && !pausado && !estaVencido();
    }

    public void pausarMedicamento() {
        this.pausado = true;
        this.activo = false;
    }

    public void reanudarMedicamento() {
        this.pausado = false;
        this.activo = true;
    }

    public String getInfoStock() {
        switch (tipoStock) {
            case UNIDADES_CONTABLES:
                return String.format("%d/%d %s", stockActual, stockInicial, presentacion.toLowerCase());
            case UNIDADES_APROXIMADAS:
                return String.format("%d días restantes (estimado)", diasRestantesDuracion);
            case TOPICO_DIAS:
                return String.format("%d/%d días restantes", diasRestantesDuracion, diasEstimadosDuracion);
            case LIQUIDO_ML:
                return String.format("%d días restantes (ml)", diasRestantesDuracion);
            default:
                return "Stock no disponible";
        }
    }

    public String getEstadoTexto() {
        if (estaVencido()) {
            return "VENCIDO";
        }
        if (pausado) {
            return "PAUSADO";
        }
        if (!activo) {
            return "INACTIVO";
        }
        if (estaAgotado()) {
            return "AGOTADO";
        }
        if (necesitaReposicion()) {
            return "STOCK BAJO";
        }
        return "ACTIVO";
    }

    public String getUnidadStock() {
        switch (tipoStock) {
            case UNIDADES_CONTABLES:
                return presentacion.toLowerCase();
            case UNIDADES_APROXIMADAS:
                return "días (estimado)";
            case TOPICO_DIAS:
                return "días";
            case LIQUIDO_ML:
                return "días (ml)";
            default:
                return "unidades";
        }
    }
}