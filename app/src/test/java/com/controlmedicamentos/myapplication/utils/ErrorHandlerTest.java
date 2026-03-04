package com.controlmedicamentos.myapplication.utils;

import org.junit.Test;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * Tests unitarios para el mapeo de excepciones a mensajes en ErrorHandler.
 */
public class ErrorHandlerTest {

    @Test
    public void getErrorMessage_nullRetornaMensajeDesconocido() {
        String msg = ErrorHandler.getErrorMessage(null);
        assertNotNull(msg);
        assertTrue(msg.contains("error desconocido") || msg.contains("Intenta nuevamente"));
    }

    @Test
    public void getErrorMessage_unknownHostException_retornaMensajeRed() {
        String msg = ErrorHandler.getErrorMessage(new UnknownHostException("Unknown host"));
        assertNotNull(msg);
        assertTrue(msg.contains("conexión") || msg.contains("internet"));
    }

    @Test
    public void getErrorMessage_socketTimeoutException_retornaMensajeRed() {
        String msg = ErrorHandler.getErrorMessage(new SocketTimeoutException("Timeout"));
        assertNotNull(msg);
        assertTrue(msg.contains("conexión") || msg.contains("internet"));
    }

    @Test
    public void getErrorMessage_exceptionConMensajeSimple_retornaEseMensaje() {
        String msg = ErrorHandler.getErrorMessage(new Exception("Dato inválido"));
        assertNotNull(msg);
        assertEquals("Dato inválido", msg);
    }

    @Test
    public void getErrorMessage_exceptionConMensajeTecnico_retornaGenerico() {
        String msg = ErrorHandler.getErrorMessage(new Exception("NullPointerException at line 10"));
        assertNotNull(msg);
        assertTrue(msg.contains("Ocurrió un error") && msg.contains("Intenta nuevamente"));
    }

    @Test
    public void getErrorMessage_exceptionVacia_retornaMensajeInesperado() {
        String msg = ErrorHandler.getErrorMessage(new Exception());
        assertNotNull(msg);
        assertTrue(msg.contains("inesperado") || msg.contains("Intenta nuevamente"));
    }
}
