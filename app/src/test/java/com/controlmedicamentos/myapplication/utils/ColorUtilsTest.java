package com.controlmedicamentos.myapplication.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitarios para ColorUtils.
 */
public class ColorUtilsTest {

    @Test
    public void obtenerColorPorIndice_retornaColoresCiclicos() {
        assertEquals("#FFB6C1", ColorUtils.obtenerColorPorIndice(0));
        assertEquals("#ADD8E6", ColorUtils.obtenerColorPorIndice(1));
        assertEquals("#B0E0E6", ColorUtils.obtenerColorPorIndice(2));
        assertEquals("#FFFACD", ColorUtils.obtenerColorPorIndice(3));
        assertEquals("#E6E6FA", ColorUtils.obtenerColorPorIndice(4));
        // Ciclo
        assertEquals("#FFB6C1", ColorUtils.obtenerColorPorIndice(5));
        assertEquals("#ADD8E6", ColorUtils.obtenerColorPorIndice(6));
    }

    @Test
    public void hexToInt_conPrefijoNumeral() {
        int color = ColorUtils.hexToInt("#FFB6C1");
        assertEquals(0xFFFFB6C1, color);
    }

    @Test
    public void hexToInt_sinPrefijoNumeral() {
        int color = ColorUtils.hexToInt("ADD8E6");
        assertEquals(0xFFADD8E6, color);
    }

    @Test
    public void hexToInt_invalidoRetornaDefault() {
        int color = ColorUtils.hexToInt("invalid");
        assertEquals(0xFFADD8E6, color);
    }

    @Test
    public void intToHex_convierteCorrectamente() {
        String hex = ColorUtils.intToHex(0xFFFFB6C1);
        assertEquals("#FFB6C1", hex);
    }

    @Test
    public void intToHex_roundTrip() {
        String original = "#B0E0E6";
        int asInt = ColorUtils.hexToInt(original);
        String back = ColorUtils.intToHex(asInt);
        assertEquals(original, back);
    }
}
