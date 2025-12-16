package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.BotiquinActivity;
import com.controlmedicamentos.myapplication.HistorialActivity;
import com.controlmedicamentos.myapplication.AjustesActivity;
import com.controlmedicamentos.myapplication.MainActivity;
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
     * Configura los botones de navegación inferior para cualquier Activity.
     * Detecta automáticamente la Activity actual y ajusta el comportamiento.
     * 
     * @param activity La actividad actual.
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
        
        Class<?> currentActivityClass = activity.getClass();
        boolean isMainActivity = currentActivityClass == MainActivity.class;
        boolean isNuevaMedicinaActivity = currentActivityClass == NuevaMedicinaActivity.class;
        boolean isBotiquinActivity = currentActivityClass == BotiquinActivity.class;
        boolean isHistorialActivity = currentActivityClass == HistorialActivity.class;
        boolean isAjustesActivity = currentActivityClass == AjustesActivity.class;
        
        // Botón Home
        if (btnNavHome != null) {
            if (isMainActivity) {
                // Ya estamos en home, no hacer nada
                btnNavHome.setOnClickListener(v -> {
                    // Ya estamos en home
                });
            } else {
                btnNavHome.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(intent);
                    activity.finish();
                });
            }
        }
        
        // Botón Nueva Medicina
        if (btnNavNuevaMedicina != null) {
            if (isNuevaMedicinaActivity) {
                // Ya estamos en nueva medicina, no hacer nada
                btnNavNuevaMedicina.setOnClickListener(v -> {
                    // Ya estamos en nueva medicina
                });
            } else {
                btnNavNuevaMedicina.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, NuevaMedicinaActivity.class);
                    activity.startActivity(intent);
                    if (!isMainActivity) {
                        activity.finish();
                    }
                });
            }
        }

        // Botón Botiquín
        if (btnNavBotiquin != null) {
            if (isBotiquinActivity) {
                // Ya estamos en botiquín, no hacer nada
                btnNavBotiquin.setOnClickListener(v -> {
                    // Ya estamos en botiquín
                });
            } else {
                btnNavBotiquin.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, BotiquinActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                });
            }
        }

        // Botón Historial
        if (btnNavHistorial != null) {
            if (isHistorialActivity) {
                // Ya estamos en historial, no hacer nada
                btnNavHistorial.setOnClickListener(v -> {
                    // Ya estamos en historial
                });
            } else {
                btnNavHistorial.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, HistorialActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                });
            }
        }

        // Botón Ajustes
        if (btnNavAjustes != null) {
            if (isAjustesActivity) {
                // Ya estamos en ajustes, no hacer nada
                btnNavAjustes.setOnClickListener(v -> {
                    // Ya estamos en ajustes
                });
            } else {
                btnNavAjustes.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, AjustesActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                });
            }
        }
        
        Logger.d("NavigationHelper", "Navegación configurada correctamente para " + currentActivityClass.getSimpleName());
    }
}

