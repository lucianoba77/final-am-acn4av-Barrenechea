package com.controlmedicamentos.myapplication.utils;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests unitarios para ValidationUtils.
 * Usa Robolectric para soportar dependencias Android (TextUtils, etc.).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class ValidationUtilsTest {

    @Test
    public void isValidEmail_vacioRetornaFalse() {
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("   "));
    }

    @Test
    public void isValidEmail_formatosValidos() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("test+tag@gmail.com"));
        assertTrue(ValidationUtils.isValidEmail("a@b.co"));
    }

    @Test
    public void isValidEmail_formatosInvalidos() {
        assertFalse(ValidationUtils.isValidEmail("invalid"));
        assertFalse(ValidationUtils.isValidEmail("@nodomain.com"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("user@.com"));
    }

    @Test
    public void isValidTime_formatosValidos() {
        assertTrue(ValidationUtils.isValidTime("08:00"));
        assertTrue(ValidationUtils.isValidTime("23:59"));
        assertTrue(ValidationUtils.isValidTime("00:00"));
        assertTrue(ValidationUtils.isValidTime("9:30"));
    }

    @Test
    public void isValidTime_formatosInvalidos() {
        assertFalse(ValidationUtils.isValidTime(null));
        assertFalse(ValidationUtils.isValidTime(""));
        assertFalse(ValidationUtils.isValidTime("25:00"));
        assertFalse(ValidationUtils.isValidTime("12:60"));
    }

    @Test
    public void validatePassword_vaciaRetornaMensaje() {
        assertNotNull(ValidationUtils.validatePassword(null));
        assertNotNull(ValidationUtils.validatePassword(""));
        assertTrue(ValidationUtils.validatePassword("").contains("requerida"));
    }

    @Test
    public void validatePassword_cortaRetornaMensaje() {
        String msg = ValidationUtils.validatePassword("12345");
        assertNotNull(msg);
        assertTrue(msg.contains("6 caracteres"));
    }

    @Test
    public void validatePassword_validaRetornaNull() {
        assertNull(ValidationUtils.validatePassword("123456"));
        assertNull(ValidationUtils.validatePassword("password"));
    }

    @Test
    public void validateNotEmpty_vacioRetornaMensaje() {
        assertNotNull(ValidationUtils.validateNotEmpty(null, "Campo"));
        assertNotNull(ValidationUtils.validateNotEmpty("", "Campo"));
        assertNotNull(ValidationUtils.validateNotEmpty("   ", "Campo"));
        assertTrue(ValidationUtils.validateNotEmpty("", "Nombre").contains("Nombre"));
    }

    @Test
    public void validateNotEmpty_validoRetornaNull() {
        assertNull(ValidationUtils.validateNotEmpty("texto", "Campo"));
    }

    @Test
    public void isValidMedicamento_nullRetornaFalse() {
        assertFalse(ValidationUtils.isValidMedicamento(null));
    }

    @Test
    public void isValidMedicamento_sinIdRetornaFalse() {
        Medicamento m = new Medicamento();
        m.setNombre("Test");
        m.setId("");
        assertFalse(ValidationUtils.isValidMedicamento(m));
    }

    @Test
    public void isValidMedicamento_sinNombreRetornaFalse() {
        Medicamento m = new Medicamento();
        m.setId("id1");
        m.setNombre("");
        assertFalse(ValidationUtils.isValidMedicamento(m));
    }

    @Test
    public void isValidMedicamento_validoRetornaTrue() {
        Medicamento m = new Medicamento();
        m.setId("id1");
        m.setNombre("Aspirina");
        assertTrue(ValidationUtils.isValidMedicamento(m));
    }

    @Test
    public void validateMedicamentoForm_nullRetornaMensaje() {
        assertNotNull(ValidationUtils.validateMedicamentoForm(null));
    }

    @Test
    public void validateMedicamentoForm_sinNombreRetornaMensaje() {
        Medicamento m = new Medicamento();
        m.setNombre("");
        m.setPresentacion("comprimidos");
        m.setAfeccion("Dolor");
        m.setTomasDiarias(1);
        m.setHorarioPrimeraToma("08:00");
        m.setStockInicial(10);
        m.setStockActual(10);
        m.setDiasTratamiento(7);
        String msg = ValidationUtils.validateMedicamentoForm(m);
        assertNotNull(msg);
        assertTrue(msg.toLowerCase().contains("nombre"));
    }

    @Test
    public void validatePositiveInteger_negativoRetornaMensaje() {
        assertNotNull(ValidationUtils.validatePositiveInteger(-1, "Stock"));
        assertTrue(ValidationUtils.validatePositiveInteger(-1, "Stock").contains("negativo"));
    }

    @Test
    public void validatePositiveInteger_positivoRetornaNull() {
        assertNull(ValidationUtils.validatePositiveInteger(0, "Stock"));
        assertNull(ValidationUtils.validatePositiveInteger(5, "Stock"));
    }
}
