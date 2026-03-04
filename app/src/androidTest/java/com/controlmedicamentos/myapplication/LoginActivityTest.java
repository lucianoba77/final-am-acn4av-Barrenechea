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
 * Tests instrumentados para LoginActivity.
 * Verifica que la pantalla de login muestre los elementos principales.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.controlmedicamentos.myapplication", appContext.getPackageName());
    }

    @Test
    public void loginActivity_launchesAndShowsEmailField() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void loginActivity_showsPasswordField() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void loginActivity_showsLoginButton() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void loginActivity_showsRegisterButton() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnRegister)).check(matches(isDisplayed()));
        }
    }
}
