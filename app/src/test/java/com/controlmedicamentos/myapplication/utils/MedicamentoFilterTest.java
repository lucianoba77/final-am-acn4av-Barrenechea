package com.controlmedicamentos.myapplication.utils;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para MedicamentoFilter.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class MedicamentoFilterTest {

    @Test
    public void filtrarParaDashboard_listaNullRetornaVacia() {
        TomaTrackingService tracking = mock(TomaTrackingService.class);
        List<Medicamento> result = MedicamentoFilter.filtrarParaDashboard(null, tracking);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filtrarParaDashboard_listaVaciaRetornaVacia() {
        TomaTrackingService tracking = mock(TomaTrackingService.class);
        List<Medicamento> result = MedicamentoFilter.filtrarParaDashboard(new ArrayList<>(), tracking);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filtrarParaDashboard_medicamentoInactivoExcluido() {
        Medicamento m = crearMedicamentoValido("med1", "Test", 2, "08:00");
        m.setActivo(false);
        List<Medicamento> entrada = new ArrayList<>();
        entrada.add(m);
        TomaTrackingService tracking = mock(TomaTrackingService.class);
        when(tracking.obtenerTomasMedicamento(anyString())).thenReturn(new ArrayList<>());
        List<Medicamento> result = MedicamentoFilter.filtrarParaDashboard(entrada, tracking);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filtrarParaDashboard_medicamentoSinTomasIncluido() {
        Medicamento m = crearMedicamentoValido("med1", "Aspirina", 2, "08:00");
        List<Medicamento> entrada = new ArrayList<>();
        entrada.add(m);
        TomaTrackingService tracking = mock(TomaTrackingService.class);
        when(tracking.obtenerTomasMedicamento("med1")).thenReturn(new ArrayList<>());
        List<Medicamento> result = MedicamentoFilter.filtrarParaDashboard(entrada, tracking);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("med1", result.get(0).getId());
    }

    @Test
    public void filtrarParaDashboard_medicamentoOcasionalExcluido() {
        Medicamento m = crearMedicamentoValido("med1", "Test", 0, "08:00");
        List<Medicamento> entrada = new ArrayList<>();
        entrada.add(m);
        TomaTrackingService tracking = mock(TomaTrackingService.class);
        List<Medicamento> result = MedicamentoFilter.filtrarParaDashboard(entrada, tracking);
        assertTrue(result.isEmpty());
    }

    private static Medicamento crearMedicamentoValido(String id, String nombre, int tomasDiarias, String horario) {
        Medicamento m = new Medicamento();
        m.setId(id);
        m.setNombre(nombre);
        m.setActivo(true);
        m.setTomasDiarias(tomasDiarias);
        m.setHorarioPrimeraToma(horario);
        if ("00:00".equals(horario)) {
            m.setHorarioPrimeraToma("08:00");
        }
        return m;
    }
}
