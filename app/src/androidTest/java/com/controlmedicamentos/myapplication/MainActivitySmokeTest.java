package com.controlmedicamentos.myapplication;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

/**
 * Smoke test para MainActivity.
 * Abre MainActivity y verifica que la app no crashea y que se muestra
 * la pantalla principal (RecyclerView) o la de login si no hay usuario.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.controlmedicamentos.myapplication", appContext.getPackageName());
    }

    @Test
    public void mainActivity_launchesWithoutCrash() {
        // MainActivity redirige a Login si no hay usuario; en cualquier caso no debe crashear.
        // No usamos onActivity porque si redirige, la actividad ya está destruida.
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Si llegamos aquí sin excepción, el launch no crasheó.
        }
    }

    @Test
    public void mainActivity_orLogin_showsExpectedContent() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Tras el launch, podemos estar en MainActivity (con RecyclerView) o haber
            // sido redirigidos a LoginActivity (con etEmail). Comprobamos que al menos
            // una vista típica está presente para no tener pantalla en blanco.
            try {
                onView(withId(R.id.rvMedicamentos)).check(matches(isDisplayed()));
            } catch (Exception e) {
                // Si no está el RecyclerView, deberíamos estar en Login
                onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
            }
        }
    }
}
