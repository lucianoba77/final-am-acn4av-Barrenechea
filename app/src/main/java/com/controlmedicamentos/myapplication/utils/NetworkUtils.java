package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Utilidad para verificar el estado de la conexión a internet
 * Usa APIs modernas (NetworkCapabilities) para Android 6.0+ y fallback para versiones anteriores
 */
public class NetworkUtils {

    /**
     * Verifica si hay conexión a internet disponible
     * Usa NetworkCapabilities para Android 6.0+ (API 23+) y NetworkInfo como fallback
     * 
     * @param context Contexto de la aplicación
     * @return true si hay conexión, false en caso contrario
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Logger.w("NetworkUtils", "Context es null en isNetworkAvailable");
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Logger.w("NetworkUtils", "ConnectivityManager es null");
            return false;
        }
        
        // Usar NetworkCapabilities para Android 6.0+ (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) {
                    return false;
                }
                
                // Verificar si tiene capacidad de internet y está conectado
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                       capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } catch (Exception e) {
                Logger.e("NetworkUtils", "Error al verificar conexión con NetworkCapabilities", e);
                // Fallback a método antiguo si hay error
                return isNetworkAvailableLegacy(connectivityManager);
            }
        } else {
            // Fallback para versiones anteriores a Android 6.0
            return isNetworkAvailableLegacy(connectivityManager);
        }
    }

    /**
     * Método legacy para verificar conexión (Android < 6.0)
     * @param connectivityManager El ConnectivityManager
     * @return true si hay conexión, false en caso contrario
     */
    @SuppressWarnings("deprecation")
    private static boolean isNetworkAvailableLegacy(ConnectivityManager connectivityManager) {
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Logger.e("NetworkUtils", "Error al verificar conexión legacy", e);
            return false;
        }
    }

    /**
     * Verifica si hay conexión WiFi disponible
     * Usa NetworkCapabilities para Android 6.0+ y NetworkInfo como fallback
     * 
     * @param context Contexto de la aplicación
     * @return true si hay WiFi, false en caso contrario
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            Logger.w("NetworkUtils", "Context es null en isWifiConnected");
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        // Usar NetworkCapabilities para Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) {
                    return false;
                }
                
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } catch (Exception e) {
                Logger.e("NetworkUtils", "Error al verificar WiFi con NetworkCapabilities", e);
                return isWifiConnectedLegacy(connectivityManager);
            }
        } else {
            return isWifiConnectedLegacy(connectivityManager);
        }
    }

    /**
     * Método legacy para verificar WiFi (Android < 6.0)
     */
    @SuppressWarnings("deprecation")
    private static boolean isWifiConnectedLegacy(ConnectivityManager connectivityManager) {
        try {
            NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiInfo != null && wifiInfo.isConnected();
        } catch (Exception e) {
            Logger.e("NetworkUtils", "Error al verificar WiFi legacy", e);
            return false;
        }
    }

    /**
     * Verifica si hay conexión de datos móviles disponible
     * Usa NetworkCapabilities para Android 6.0+ y NetworkInfo como fallback
     * 
     * @param context Contexto de la aplicación
     * @return true si hay datos móviles, false en caso contrario
     */
    public static boolean isMobileDataConnected(Context context) {
        if (context == null) {
            Logger.w("NetworkUtils", "Context es null en isMobileDataConnected");
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        // Usar NetworkCapabilities para Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) {
                    return false;
                }
                
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            } catch (Exception e) {
                Logger.e("NetworkUtils", "Error al verificar datos móviles con NetworkCapabilities", e);
                return isMobileDataConnectedLegacy(connectivityManager);
            }
        } else {
            return isMobileDataConnectedLegacy(connectivityManager);
        }
    }

    /**
     * Método legacy para verificar datos móviles (Android < 6.0)
     */
    @SuppressWarnings("deprecation")
    private static boolean isMobileDataConnectedLegacy(ConnectivityManager connectivityManager) {
        try {
            NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobileInfo != null && mobileInfo.isConnected();
        } catch (Exception e) {
            Logger.e("NetworkUtils", "Error al verificar datos móviles legacy", e);
            return false;
        }
    }
}

