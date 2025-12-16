package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.controlmedicamentos.myapplication.models.Medicamento;
import java.util.List;

/**
 * Clase de utilidad para gestionar alertas de stock de medicamentos.
 * Centraliza la lógica de verificación y notificación de alertas de stock.
 */
public class StockAlertManager {

    private final Context context;
    private final SharedPreferences preferences;

    /**
     * Constructor.
     * 
     * @param context El contexto de la aplicación.
     */
    public StockAlertManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Verifica las alertas de stock para una lista de medicamentos.
     * 
     * @param todosLosMedicamentos La lista completa de medicamentos a verificar.
     */
    public void verificarAlertasStock(List<Medicamento> todosLosMedicamentos) {
        if (todosLosMedicamentos == null || todosLosMedicamentos.isEmpty()) {
            return;
        }
        
        int diasAntesAlerta = preferences.getInt(Constants.PREF_DIAS_ANTELACION_STOCK, 
                                                 Constants.DIAS_ANTELACION_STOCK_DEFAULT);
        
        StockAlertUtils.verificarStock(todosLosMedicamentos, new StockAlertUtils.StockAlertListener() {
            @Override
            public void onStockAgotado(Medicamento medicamento) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    Toast.makeText(context, 
                        "⚠️ " + medicamento.getNombre() + " se ha agotado. Por favor, recarga tu stock.", 
                        Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onStockBajo(Medicamento medicamento, int diasRestantes, String mensaje) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
                });
            }
        }, diasAntesAlerta);
    }
}

