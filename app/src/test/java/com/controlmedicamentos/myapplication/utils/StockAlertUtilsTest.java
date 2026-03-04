package com.controlmedicamentos.myapplication.utils;

import com.controlmedicamentos.myapplication.models.Medicamento;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests unitarios para StockAlertUtils.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class StockAlertUtilsTest {

    @Before
    public void setUp() {
        StockAlertUtils.limpiarAlertas();
    }

    @Test
    public void verificarStock_listaVacia_noDisparaListener() {
        final boolean[] called = { false };
        StockAlertUtils.verificarStock(new ArrayList<>(), new StockAlertUtils.StockAlertListener() {
            @Override
            public void onStockAgotado(Medicamento medicamento) {
                called[0] = true;
            }
            @Override
            public void onStockBajo(Medicamento medicamento, int diasRestantes, String mensaje) {
                called[0] = true;
            }
        }, 7);
        assertFalse(called[0]);
    }

    @Test
    public void verificarStock_listaNull_noLanzaExcepcion() {
        StockAlertUtils.verificarStock(null, new StockAlertUtils.StockAlertListener() {
            @Override
            public void onStockAgotado(Medicamento medicamento) {}
            @Override
            public void onStockBajo(Medicamento medicamento, int diasRestantes, String mensaje) {}
        }, 7);
    }

    @Test
    public void verificarStock_medicamentoInactivo_noDisparaAlerta() {
        Medicamento m = new Medicamento();
        m.setId("med1");
        m.setNombre("Test");
        m.setActivo(false);
        m.setStockActual(0);
        m.setTomasDiarias(1);
        List<Medicamento> lista = new ArrayList<>();
        lista.add(m);
        final boolean[] called = { false };
        StockAlertUtils.verificarStock(lista, new StockAlertUtils.StockAlertListener() {
            @Override
            public void onStockAgotado(Medicamento medicamento) {
                called[0] = true;
            }
            @Override
            public void onStockBajo(Medicamento medicamento, int diasRestantes, String mensaje) {
                called[0] = true;
            }
        }, 7);
        assertFalse(called[0]);
    }

    @Test
    public void limpiarAlertas_noLanzaExcepcion() {
        StockAlertUtils.limpiarAlertas();
    }
}
