package com.controlmedicamentos.myapplication.utils;

import android.content.Context;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios para TomaActionHandler.
 * Verifica validaciones y que se invocan los callbacks esperados.
 * Robolectric proporciona el entorno Android para ValidationUtils.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class TomaActionHandlerTest {

    private Context context;

    @Mock
    private FirebaseService firebaseService;

    @Mock
    private TomaTrackingService tomaTrackingService;

    private TomaActionHandler handler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        handler = new TomaActionHandler(context, firebaseService, tomaTrackingService);
    }

    @Test
    public void constructor_nullContext_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new TomaActionHandler(null, firebaseService, tomaTrackingService));
    }

    @Test
    public void constructor_nullFirebaseService_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new TomaActionHandler(context, null, tomaTrackingService));
    }

    @Test
    public void constructor_nullTomaTrackingService_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new TomaActionHandler(context, firebaseService, null));
    }

    @Test
    public void marcarTomaComoTomada_nullMedicamento_callsOnError() {
        TomaActionHandler.TomaActionCallback callback = mock(TomaActionHandler.TomaActionCallback.class);

        handler.marcarTomaComoTomada(null, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onError(captor.capture());
        verify(callback, never()).onSuccess(any(), any(Boolean.class), any(Boolean.class));
        assertNotNull(captor.getValue());
        assertEquals("Medicamento inválido", captor.getValue().getMessage());
    }

    @Test
    public void marcarTomaComoTomada_nullMedicamento_nullCallback_doesNotThrow() {
        handler.marcarTomaComoTomada(null, null);
        // No excepción
    }
}
