package com.controlmedicamentos.myapplication;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Tests instrumentados para NuevaMedicinaActivity.
 * Verifica que el formulario de nueva medicina muestre los campos principales.
 * Nota: La actividad puede redirigir a Login si no hay usuario; estos tests
 * comprueban la presencia de vistas cuando la actividad se lanza con intent por defecto.
 */
@RunWith(AndroidJUnit4.class)
public class NuevaMedicinaActivityTest {

    @Test
    public void nuevaMedicinaActivity_showsNombreField() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), NuevaMedicinaActivity.class);
        try (ActivityScenario<NuevaMedicinaActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etNombre)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void nuevaMedicinaActivity_showsSaveButton() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), NuevaMedicinaActivity.class);
        try (ActivityScenario<NuevaMedicinaActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnGuardar)).check(matches(isDisplayed()));
        }
    }
}
