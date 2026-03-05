package com.controlmedicamentos.myapplication.utils;

import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.controlmedicamentos.myapplication.BuildConfig;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.GoogleCalendarAuthService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import java.util.Map;

/**
 * Helper que centraliza la lógica de Google Calendar en Ajustes:
 * verificación de conexión, actualización de UI, conectar/desconectar y manejo del resultado de Sign-In.
 */
public class AjustesGoogleCalendarHelper {

    private static final int RC_GOOGLE_CALENDAR_SIGN_IN = 9002;

    private final AppCompatActivity activity;
    private final AuthService authService;
    private final GoogleCalendarAuthService googleCalendarAuthService;
    private final String webClientId;

    private android.widget.TextView tvCalendarStatus;
    private android.widget.TextView tvCalendarInfo;
    private com.google.android.material.button.MaterialButton btnConectarGoogleCalendar;
    private com.google.android.material.button.MaterialButton btnDesconectarGoogleCalendar;

    private boolean googleCalendarConectado = false;
    private com.google.android.gms.auth.api.signin.GoogleSignInClient googleCalendarSignInClient;

    public AjustesGoogleCalendarHelper(AppCompatActivity activity,
                                       AuthService authService,
                                       GoogleCalendarAuthService googleCalendarAuthService) {
        this.activity = activity;
        this.authService = authService;
        this.googleCalendarAuthService = googleCalendarAuthService;
        this.webClientId = BuildConfig.WEB_CLIENT_ID;
    }

    public void setViews(android.widget.TextView tvCalendarStatus,
                         android.widget.TextView tvCalendarInfo,
                         com.google.android.material.button.MaterialButton btnConectarGoogleCalendar,
                         com.google.android.material.button.MaterialButton btnDesconectarGoogleCalendar) {
        this.tvCalendarStatus = tvCalendarStatus;
        this.tvCalendarInfo = tvCalendarInfo;
        this.btnConectarGoogleCalendar = btnConectarGoogleCalendar;
        this.btnDesconectarGoogleCalendar = btnDesconectarGoogleCalendar;
    }

    public void init() {
        if (webClientId != null && !webClientId.isEmpty()) {
            googleCalendarSignInClient = authService.initializeGoogleSignInForCalendar(activity, webClientId);
        }
        verificarConexionGoogleCalendar();
    }

    public void setupListeners() {
        if (btnConectarGoogleCalendar != null) {
            btnConectarGoogleCalendar.setOnClickListener(v -> conectarGoogleCalendar());
        }
        if (btnDesconectarGoogleCalendar != null) {
            btnDesconectarGoogleCalendar.setOnClickListener(v -> desconectarGoogleCalendar());
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RC_GOOGLE_CALENDAR_SIGN_IN) {
            return false;
        }
        if (data == null) {
            Logger.d("AjustesGoogleCalendarHelper", "Usuario canceló el flujo de Google Sign-In (data es null)");
            return true;
        }
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                String serverAuthCode = account.getServerAuthCode();
                if (serverAuthCode != null && !serverAuthCode.isEmpty()) {
                    Logger.d("AjustesGoogleCalendarHelper", "Server Auth Code obtenido, intercambiando por access token...");
                    intercambiarAuthCodePorToken(serverAuthCode);
                } else {
                    Logger.e("AjustesGoogleCalendarHelper", "No se obtuvo serverAuthCode de Google Sign-In");
                    Toast.makeText(activity, activity.getString(R.string.error_calendar_no_auth_code), Toast.LENGTH_LONG).show();
                }
            }
        } catch (ApiException e) {
            Logger.e("AjustesGoogleCalendarHelper", "Error en Google Sign-In para Calendar", e);
            String mensaje = activity.getString(R.string.error_calendar_connect_generic);
            if (e.getStatusCode() == 12500) {
                mensaje = activity.getString(R.string.error_calendar_play_services);
            } else if (e.getStatusCode() == 10) {
                mensaje = activity.getString(R.string.error_calendar_config);
            } else if (e.getMessage() != null) {
                mensaje += " " + e.getMessage();
            }
            Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    public int getRequestCodeGoogleCalendarSignIn() {
        return RC_GOOGLE_CALENDAR_SIGN_IN;
    }

    public void verificarConexionGoogleCalendar() {
        Logger.d("AjustesGoogleCalendarHelper", "verificarConexionGoogleCalendar: Iniciando verificación");
        googleCalendarAuthService.tieneGoogleCalendarConectado(
                new GoogleCalendarAuthService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        activity.runOnUiThread(() -> {
                            if (result instanceof Boolean) {
                                googleCalendarConectado = (Boolean) result;
                            } else {
                                googleCalendarConectado = false;
                            }
                            actualizarUIGoogleCalendar();
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        Logger.w("AjustesGoogleCalendarHelper", "verificarConexionGoogleCalendar: onError", exception);
                        activity.runOnUiThread(() -> {
                            googleCalendarConectado = false;
                            actualizarUIGoogleCalendar();
                        });
                    }
                });
    }

    private void actualizarUIGoogleCalendar() {
        if (tvCalendarStatus == null || tvCalendarInfo == null || btnConectarGoogleCalendar == null || btnDesconectarGoogleCalendar == null) {
            return;
        }
        if (googleCalendarConectado) {
            tvCalendarStatus.setText(activity.getString(R.string.google_calendar_status_connected));
            tvCalendarInfo.setText(activity.getString(R.string.google_calendar_info_connected));
            btnConectarGoogleCalendar.setVisibility(View.GONE);
            btnDesconectarGoogleCalendar.setVisibility(View.VISIBLE);
        } else {
            tvCalendarStatus.setText(activity.getString(R.string.google_calendar_status_not_connected));
            tvCalendarInfo.setText(activity.getString(R.string.google_calendar_info_not_connected));
            btnConectarGoogleCalendar.setVisibility(View.VISIBLE);
            btnDesconectarGoogleCalendar.setVisibility(View.GONE);
        }
    }

    public void conectarGoogleCalendar() {
        if (googleCalendarSignInClient == null) {
            if (webClientId == null || webClientId.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.error_calendar_client_id), Toast.LENGTH_LONG).show();
                return;
            }
            googleCalendarSignInClient = authService.initializeGoogleSignInForCalendar(activity, webClientId);
            if (googleCalendarSignInClient == null) {
                Toast.makeText(activity, activity.getString(R.string.error_calendar_signin_init), Toast.LENGTH_LONG).show();
                return;
            }
        }
        googleCalendarSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleCalendarSignInClient.getSignInIntent();
            activity.startActivityForResult(signInIntent, RC_GOOGLE_CALENDAR_SIGN_IN);
        });
    }

    public void desconectarGoogleCalendar() {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialog_disconnect_calendar_title))
                .setMessage(activity.getString(R.string.dialog_disconnect_calendar_message))
                .setPositiveButton(activity.getString(R.string.btn_disconnect), (DialogInterface dialog, int which) -> {
                    googleCalendarAuthService.eliminarTokenGoogle(
                            new GoogleCalendarAuthService.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    googleCalendarConectado = false;
                                    actualizarUIGoogleCalendar();
                                    Toast.makeText(activity, activity.getString(R.string.msg_calendar_disconnected), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception exception) {
                                    String msg = exception != null && exception.getMessage() != null
                                            ? exception.getMessage()
                                            : activity.getString(R.string.error_unknown);
                                    Toast.makeText(activity, activity.getString(R.string.msg_error_disconnecting_calendar, msg), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton(activity.getString(R.string.btn_cancel), null)
                .show();
    }

    private void intercambiarAuthCodePorToken(String serverAuthCode) {
        googleCalendarAuthService.intercambiarAuthCodePorToken(serverAuthCode,
                new GoogleCalendarAuthService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result != null && result instanceof Map) {
                            activity.runOnUiThread(() -> {
                                googleCalendarConectado = true;
                                actualizarUIGoogleCalendar();
                                Toast.makeText(activity, activity.getString(R.string.msg_calendar_connected), Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.error_calendar_no_token), Toast.LENGTH_LONG).show());
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Logger.e("AjustesGoogleCalendarHelper", "Error al intercambiar auth code por token", exception);
                        activity.runOnUiThread(() -> {
                            String mensaje = ErrorHandler.getErrorMessage(activity, exception != null ? exception : new Exception(activity.getString(R.string.error_unknown)));
                            if (exception != null && exception.getMessage() != null) {
                                if (exception.getMessage().contains("unauthenticated")) {
                                    mensaje = activity.getString(R.string.error_calendar_unauthenticated);
                                } else if (exception.getMessage().contains("failed-precondition")) {
                                    mensaje = activity.getString(R.string.error_calendar_functions);
                                } else if (exception.getMessage().contains("invalid_grant")) {
                                    mensaje = activity.getString(R.string.error_calendar_invalid_grant);
                                }
                            }
                            Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}
