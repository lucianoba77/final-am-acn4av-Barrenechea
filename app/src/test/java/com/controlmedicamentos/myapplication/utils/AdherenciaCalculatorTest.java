package com.controlmedicamentos.myapplication.utils;

import com.controlmedicamentos.myapplication.models.AdherenciaResumen;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests unitarios para AdherenciaCalculator.
 * Usa Robolectric por dependencias Android en Medicamento (R, etc.).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class AdherenciaCalculatorTest {

    @Test
    public void filtrarTomasPorMedicamento_nullListaRetornaVacia() {
        List<Toma> result = AdherenciaCalculator.filtrarTomasPorMedicamento(null, "med1");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filtrarTomasPorMedicamento_nullMedicamentoIdRetornaVacia() {
        List<Toma> tomas = new ArrayList<>();
        Toma t = new Toma();
        t.setMedicamentoId("med1");
        tomas.add(t);
        List<Toma> result = AdherenciaCalculator.filtrarTomasPorMedicamento(tomas, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filtrarTomasPorMedicamento_filtraPorId() {
        List<Toma> tomas = new ArrayList<>();
        Toma t1 = new Toma();
        t1.setMedicamentoId("med1");
        Toma t2 = new Toma();
        t2.setMedicamentoId("med2");
        Toma t3 = new Toma();
        t3.setMedicamentoId("med1");
        tomas.add(t1);
        tomas.add(t2);
        tomas.add(t3);
        List<Toma> result = AdherenciaCalculator.filtrarTomasPorMedicamento(tomas, "med1");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("med1", result.get(0).getMedicamentoId());
        assertEquals("med1", result.get(1).getMedicamentoId());
    }

    @Test
    public void filtrarTomasPorMedicamento_ignoraTomasNull() {
        List<Toma> tomas = new ArrayList<>();
        tomas.add(null);
        Toma t = new Toma();
        t.setMedicamentoId("med1");
        tomas.add(t);
        List<Toma> result = AdherenciaCalculator.filtrarTomasPorMedicamento(tomas, "med1");
        assertEquals(1, result.size());
    }

    @Test
    public void calcularResumenGeneral_medicamentoNullOListaVacia() {
        Medicamento m = crearMedicamentoBasico("med1", "Test", 2, 10);
        AdherenciaResumen r = AdherenciaCalculator.calcularResumenGeneral(m, new ArrayList<>());
        assertNotNull(r);
        assertEquals("med1", r.getMedicamentoId());
        assertEquals("Test", r.getMedicamentoNombre());
        assertTrue(r.getTomasEsperadas() >= 0);
        assertTrue(r.getTomasRealizadas() >= 0);
    }

    @Test
    public void calcularResumenGeneral_conTomasRealizadas() {
        Medicamento m = crearMedicamentoBasico("med1", "Aspirina", 2, 7);
        m.setFechaInicioTratamiento(diasAtras(10));
        List<Toma> tomas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Toma t = new Toma();
            t.setMedicamentoId("med1");
            t.setEstado(Toma.EstadoToma.TOMADA);
            t.setFechaHoraTomada(diasAtras(3 - i));
            tomas.add(t);
        }
        AdherenciaResumen r = AdherenciaCalculator.calcularResumenGeneral(m, tomas);
        assertNotNull(r);
        assertEquals("med1", r.getMedicamentoId());
        assertTrue(r.getTomasRealizadas() >= 0);
        assertTrue(r.getPorcentaje() >= 0f && r.getPorcentaje() <= 100f);
    }

    private static Medicamento crearMedicamentoBasico(String id, String nombre, int tomasDiarias, int diasTratamiento) {
        Medicamento m = new Medicamento();
        m.setId(id);
        m.setNombre(nombre);
        m.setTomasDiarias(tomasDiarias);
        m.setDiasTratamiento(diasTratamiento);
        m.setActivo(true);
        m.setFechaInicioTratamiento(new Date());
        return m;
    }

    private static Date diasAtras(int dias) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -dias);
        return c.getTime();
    }
}
