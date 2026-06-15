package com.cecyt.pomodoro;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.util.Map;

public class AplicacionMiauFocus extends Application implements DefaultLifecycleObserver {

    private static final String CLAVE_MIGRACION_HECHA = "migracion_prefs_caja_facil_hecha";

    private boolean pantallaEncendida = true;

    @Override
    public void onCreate() {
        super.onCreate();
        migrarPreferenciasSiHaceFalta();
        GestorAlertas.crearCanales(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        IntentFilter filtroPantalla = new IntentFilter();
        filtroPantalla.addAction(Intent.ACTION_SCREEN_OFF);
        filtroPantalla.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pantallaEncendida = Intent.ACTION_SCREEN_ON.equals(intent.getAction());
                if (GestorAlertas.isCronometroCorriendo(context)) {
                    if (pantallaEncendida) {
                        GestorAlertas.cancelarNotificacionBloqueo(context);
                    } else {
                        GestorAlertas.mostrarNotificacionBloqueo(context);
                    }
                }
            }
        }, filtroPantalla);
    }

    private void migrarPreferenciasSiHaceFalta() {
        SharedPreferences nuevas = getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, MODE_PRIVATE);
        if (nuevas.getBoolean(CLAVE_MIGRACION_HECHA, false)) return;

        SharedPreferences viejas = getSharedPreferences("PreferenciasCajaFacil", MODE_PRIVATE);
        SharedPreferences.Editor editor = nuevas.edit();
        for (Map.Entry<String, ?> entrada : viejas.getAll().entrySet()) {
            Object valor = entrada.getValue();
            String clave = entrada.getKey();
            if (valor instanceof Boolean) editor.putBoolean(clave, (Boolean) valor);
            else if (valor instanceof String) editor.putString(clave, (String) valor);
            else if (valor instanceof Integer) editor.putInt(clave, (Integer) valor);
            else if (valor instanceof Long) editor.putLong(clave, (Long) valor);
            else if (valor instanceof Float) editor.putFloat(clave, (Float) valor);
        }
        editor.putBoolean(CLAVE_MIGRACION_HECHA, true);
        editor.apply();

        viejas.edit().clear().apply();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner propietario) {
        android.os.PowerManager gestorEnergia = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        boolean pantallaEncendidaAhora = gestorEnergia != null && gestorEnergia.isInteractive();

        boolean cronometroCorriendo = GestorAlertas.isCronometroCorriendo(this);
        boolean esDescanso = GestorAlertas.getEsDescansoActual(this);

        if (cronometroCorriendo && !esDescanso && pantallaEncendidaAhora
                && !GestorAlertas.estaEnPeriodoGraciaPostDescanso(this)
                && new GestorConfiguracion(this).isAlertasDistraccionHabilitadas()) {
            GestorAlertas.marcarInfraccionPendiente(this);
            GestorAlertas.marcarReanudarTrasInfraccion(this);
            new GestorEstadisticas(this).registrarFallo();

            long tiempoRestante = GestorAlertas.getTiempoFinEstimadoActual(this) - System.currentTimeMillis();
            if (tiempoRestante < 0) tiempoRestante = 0;

            GestorAlertas.congelarTiempoRestante(this, tiempoRestante);

            GestorAlertas.guardarEstadoCronometro(this, tiempoRestante, esDescanso,
                    GestorAlertas.getCicloActualActual(this), GestorAlertas.getTituloActual(this));

            GestorAlertas.setCronometroCorriendo(this, false);

            Intent intentPausar = new Intent(this, CronometroService.class);
            intentPausar.setAction(CronometroService.ACCION_PAUSAR);
            ContextCompat.startForegroundService(this, intentPausar);

            GestorAlertas.mostrarAlertaPomodoro(this,
                    "¡Te estás distrayendo!",
                    "Tu cronómetro está en pausa. Vuelve a Miau Focus para continuar");
        }
    }
}
