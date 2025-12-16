package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.BotiquinActivity;
import com.controlmedicamentos.myapplication.HistorialActivity;
import com.controlmedicamentos.myapplication.AjustesActivity;
import com.controlmedicamentos.myapplication.NuevaMedicinaActivity;

/**
 * Clase de utilidad para manejar la navegación entre Activities.
 * Centraliza la lógica de navegación para mantener consistencia.
 */
public class NavigationHelper {

    private NavigationHelper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Configura los botones de navegación inferior de MainActivity.
     * 
     * @param activity La actividad principal.
     * @param btnNavHome Botón de home (puede ser null).
     * @param btnNavNuevaMedicina Botón para nueva medicina.
     * @param btnNavBotiquin Botón para botiquín.
     * @param btnNavHistorial Botón para historial.
     * @param btnNavAjustes Botón para ajustes.
     */
    public static void configurarNavegacion(
            AppCompatActivity activity,
            MaterialButton btnNavHome,
            MaterialButton btnNavNuevaMedicina,
            MaterialButton btnNavBotiquin,
            MaterialButton btnNavHistorial,
            MaterialButton btnNavAjustes) {
        
        if (activity == null) {
            Logger.w("NavigationHelper", "Activity es null, no se puede configurar navegación");
            return;
        }
        
        // Botón Home - ya estamos en home, no hacer nada
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                // Ya estamos en home
            });
        }
        
        // Botón Nueva Medicina
        if (btnNavNuevaMedicina != null) {
            btnNavNuevaMedicina.setOnClickListener(v -> {
                Intent intent = new Intent(activity, NuevaMedicinaActivity.class);
                activity.startActivity(intent);
            });
        }

        // Botón Botiquín
        if (btnNavBotiquin != null) {
            btnNavBotiquin.setOnClickListener(v -> {
                Intent intent = new Intent(activity, BotiquinActivity.class);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        // Botón Historial
        if (btnNavHistorial != null) {
            btnNavHistorial.setOnClickListener(v -> {
                Intent intent = new Intent(activity, HistorialActivity.class);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        // Botón Ajustes
        if (btnNavAjustes != null) {
            btnNavAjustes.setOnClickListener(v -> {
                Intent intent = new Intent(activity, AjustesActivity.class);
                activity.startActivity(intent);
                activity.finish();
            });
        }
        
        Logger.d("NavigationHelper", "Navegación configurada correctamente");
    }
}

