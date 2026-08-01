package com.peggos.pomodoro;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class GestorAlertas {

    public static final String CANAL_PROGRESO_ID = "canal_progreso_pomodoro";
    public static final String CANAL_ALARMA_ID = "canal_alarma_pomodoro";
    public static final String CANAL_BLOQUEO_ID = "canal_bloqueo_pomodoro";
    public static final int ID_NOTIFICACION_ALARMA = 1002;
    public static final int ID_NOTIFICACION_BLOQUEO = 1003;
    public static final String EXTRA_ES_INFRACCION = "extra_es_infraccion";
    public static final String PREFS_NOMBRE_COMPARTIDO = "PreferenciasMiauFocus";
    public static final int ID_NOTIFICACION_AVISO_DESCANSO = 1004;
    public static final int ID_NOTIFICACION_AVISO_SANCION = 1006;
    private static final String PREFS_NOMBRE = "PrefsAlertas";
    private static final String CLAVE_INFRACCION_PENDIENTE = "infraccion_pendiente";
    private static final String CLAVE_TIEMPO_RESTANTE_GUARDADO = "tiempo_restante_guardado";
    private static final String CLAVE_ES_DESCANSO_GUARDADO = "es_descanso_guardado";
    private static final String CLAVE_CICLO_ACTUAL_GUARDADO = "ciclo_actual_guardado";
    private static final String CLAVE_TITULO_GUARDADO = "titulo_guardado";
    private static final String CLAVE_CRONOMETRO_CORRIENDO = "cronometro_corriendo";
    private static final String CLAVE_TIEMPO_FIN_ESTIMADO_ACTUAL = "tiempo_fin_estimado_actual";
    private static final String CLAVE_ES_DESCANSO_ACTUAL = "es_descanso_actual";
    private static final String CLAVE_CICLO_ACTUAL_ACTUAL = "ciclo_actual_actual";
    private static final String CLAVE_TITULO_ACTUAL = "titulo_actual";
    private static final String CLAVE_ALERTA_ACTIVA = "alerta_activa";
    private static final String CLAVE_REANUDAR_TRAS_INFRACCION = "reanudar_tras_infraccion";
    private static final String CLAVE_TIEMPO_RESTANTE_CONGELADO = "tiempo_restante_congelado";
    private static final String CLAVE_GRACIA_INFRACCION_HASTA = "gracia_infraccion_hasta";
    private static final long DURACION_GRACIA_POST_DESCANSO = 10000L;

    public static void crearCanales(Context contexto) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager gestorNotificaciones = contexto.getSystemService(NotificationManager.class);

        NotificationChannel canalProgreso = new NotificationChannel(
                CANAL_PROGRESO_ID, "Sesión en curso", NotificationManager.IMPORTANCE_LOW);
        canalProgreso.setDescription("Muestra el progreso de la sesión de enfoque o descanso");
        gestorNotificaciones.createNotificationChannel(canalProgreso);

        NotificationChannel canalAlarma = new NotificationChannel(
                CANAL_ALARMA_ID, "Alarma de Pomodoro", NotificationManager.IMPORTANCE_HIGH);
        canalAlarma.setDescription("Avisa cuando termina la sesión o cuando te distraes durante el enfoque");
        canalAlarma.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        canalAlarma.enableVibration(true);
        canalAlarma.setVibrationPattern(new long[]{0, 800, 400, 800});
        Uri sonidoAlarma = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        canalAlarma.setSound(sonidoAlarma, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        gestorNotificaciones.createNotificationChannel(canalAlarma);

        NotificationChannel canalBloqueo = new NotificationChannel(
                CANAL_BLOQUEO_ID, "Cronómetro con pantalla bloqueada", NotificationManager.IMPORTANCE_DEFAULT);
        canalBloqueo.setDescription("Recuerda que el cronómetro sigue corriendo mientras la pantalla está bloqueada");
        canalBloqueo.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        gestorNotificaciones.createNotificationChannel(canalBloqueo);
    }

    public static void mostrarNotificacionBloqueo(Context contexto) {
        Intent intentAbrirApp = new Intent(contexto, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                contexto, 2, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notificacion = new NotificationCompat.Builder(contexto, CANAL_BLOQUEO_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle("Cronómetro en curso")
                .setContentText("Vuelve a Miau Focus")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(intentoPendienteAbrirApp)
                .build();

        if (ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION_BLOQUEO, notificacion);
        }
    }

    public static void cancelarNotificacionBloqueo(Context contexto) {
        NotificationManagerCompat.from(contexto).cancel(ID_NOTIFICACION_BLOQUEO);
    }

    public static void marcarInfraccionPendiente(Context contexto) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putBoolean(CLAVE_INFRACCION_PENDIENTE, true).apply();
    }

    public static void limpiarInfraccionPendiente(Context contexto) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putBoolean(CLAVE_INFRACCION_PENDIENTE, false).apply();
    }

    public static boolean hayInfraccionPendiente(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_INFRACCION_PENDIENTE, false);
    }

    public static void guardarEstadoCronometro(Context contexto, long tiempoRestante, boolean esDescanso, int cicloActual, String titulo) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .putLong(CLAVE_TIEMPO_RESTANTE_GUARDADO, tiempoRestante)
                .putBoolean(CLAVE_ES_DESCANSO_GUARDADO, esDescanso)
                .putInt(CLAVE_CICLO_ACTUAL_GUARDADO, cicloActual)
                .putString(CLAVE_TITULO_GUARDADO, titulo)
                .apply();
    }

    public static long getTiempoRestanteGuardado(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getLong(CLAVE_TIEMPO_RESTANTE_GUARDADO, -1);
    }

    public static boolean getEsDescansoGuardado(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_ES_DESCANSO_GUARDADO, false);
    }

    public static int getCicloActualGuardado(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getInt(CLAVE_CICLO_ACTUAL_GUARDADO, 1);
    }

    public static String getTituloGuardado(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getString(CLAVE_TITULO_GUARDADO, "SESIÓN DE ENFOQUE");
    }

    public static void limpiarEstadoCronometroGuardado(Context contexto) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .remove(CLAVE_TIEMPO_RESTANTE_GUARDADO)
                .remove(CLAVE_ES_DESCANSO_GUARDADO)
                .remove(CLAVE_CICLO_ACTUAL_GUARDADO)
                .remove(CLAVE_TITULO_GUARDADO)
                .apply();
    }

    public static void setCronometroCorriendo(Context contexto, boolean corriendo) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putBoolean(CLAVE_CRONOMETRO_CORRIENDO, corriendo).apply();
    }

    public static boolean isCronometroCorriendo(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_CRONOMETRO_CORRIENDO, false);
    }

    public static boolean haySesionBloqueante(Context contexto) {
        long tiempoEnfoque = new GestorConfiguracion(contexto).getMinutosTrabajo() * 60000L;

        if (isCronometroCorriendo(contexto)) {
            int ciclo = getCicloActualActual(contexto);
            boolean esDescansoActual = getEsDescansoActual(contexto);
            if (esDescansoActual && ciclo == 1) return false;
            if (ciclo > 1) return true;
            long tiempoRestanteMs = getTiempoFinEstimadoActual(contexto) - System.currentTimeMillis();
            if (tiempoRestanteMs < 0) tiempoRestanteMs = 0;
            return (tiempoEnfoque - tiempoRestanteMs) > CronometroActivity.UMBRAL_SIN_PENALIZACION;
        }

        long tiempoGuardado = getTiempoRestanteGuardado(contexto);
        if (tiempoGuardado >= 0) {
            int ciclo = getCicloActualGuardado(contexto);
            boolean esDescansoGuardado = getEsDescansoGuardado(contexto);
            if (esDescansoGuardado && ciclo == 1) return false;
            if (ciclo > 1) return true;
            return (tiempoEnfoque - tiempoGuardado) > CronometroActivity.UMBRAL_SIN_PENALIZACION;
        }

        return false;
    }

    public static void guardarEstadoCronometroActual(Context contexto, long tiempoFinEstimado,
            boolean esDescanso, int cicloActual, String titulo) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .putLong(CLAVE_TIEMPO_FIN_ESTIMADO_ACTUAL, tiempoFinEstimado)
                .putBoolean(CLAVE_ES_DESCANSO_ACTUAL, esDescanso)
                .putInt(CLAVE_CICLO_ACTUAL_ACTUAL, cicloActual)
                .putString(CLAVE_TITULO_ACTUAL, titulo)
                .putBoolean(CLAVE_CRONOMETRO_CORRIENDO, true)
                .apply();
    }

    public static void actualizarMetadatosCronometroActual(Context contexto, boolean esDescanso,
            int cicloActual, String titulo) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .putBoolean(CLAVE_ES_DESCANSO_ACTUAL, esDescanso)
                .putInt(CLAVE_CICLO_ACTUAL_ACTUAL, cicloActual)
                .putString(CLAVE_TITULO_ACTUAL, titulo)
                .apply();
    }

    public static long getTiempoFinEstimadoActual(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getLong(CLAVE_TIEMPO_FIN_ESTIMADO_ACTUAL, 0L);
    }

    public static boolean getEsDescansoActual(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_ES_DESCANSO_ACTUAL, false);
    }

    public static int getCicloActualActual(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getInt(CLAVE_CICLO_ACTUAL_ACTUAL, 1);
    }

    public static String getTituloActual(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getString(CLAVE_TITULO_ACTUAL, "SESIÓN DE ENFOQUE");
    }

    public static void setAlertaActiva(Context contexto, boolean activa) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putBoolean(CLAVE_ALERTA_ACTIVA, activa).apply();
    }

    public static boolean isAlertaActiva(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_ALERTA_ACTIVA, false);
    }

    public static void marcarReanudarTrasInfraccion(Context contexto) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putBoolean(CLAVE_REANUDAR_TRAS_INFRACCION, true).apply();
    }

    public static boolean consumirReanudarTrasInfraccion(Context contexto) {
        SharedPreferences preferencias = contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE);
        boolean debeReanudar = preferencias.getBoolean(CLAVE_REANUDAR_TRAS_INFRACCION, false);
        if (debeReanudar) {
            preferencias.edit().remove(CLAVE_REANUDAR_TRAS_INFRACCION).apply();
        }
        return debeReanudar;
    }

    public static void congelarTiempoRestante(Context contexto, long tiempoRestanteCongelado) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putLong(CLAVE_TIEMPO_RESTANTE_CONGELADO, tiempoRestanteCongelado).apply();
    }

    public static long obtenerYLimpiarTiempoCongelado(Context contexto) {
        SharedPreferences preferencias = contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE);
        long tiempoCongelado = preferencias.getLong(CLAVE_TIEMPO_RESTANTE_CONGELADO, -1L);
        if (tiempoCongelado >= 0) {
            preferencias.edit().remove(CLAVE_TIEMPO_RESTANTE_CONGELADO).apply();
        }
        return tiempoCongelado;
    }

    public static void marcarPeriodoGraciaPostDescanso(Context contexto) {
        long graciaHasta = System.currentTimeMillis() + DURACION_GRACIA_POST_DESCANSO;
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putLong(CLAVE_GRACIA_INFRACCION_HASTA, graciaHasta).apply();
    }

    public static boolean estaEnPeriodoGraciaPostDescanso(Context contexto) {
        long graciaHasta = contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getLong(CLAVE_GRACIA_INFRACCION_HASTA, 0L);
        return System.currentTimeMillis() < graciaHasta;
    }

    public static void mostrarAlertaPomodoro(Context contexto, String titulo, String mensaje) {
        Intent intentAlerta = new Intent(contexto, CronometroActivity.class);
        intentAlerta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAlerta = PendingIntent.getActivity(
                contexto, 1, intentAlerta, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notificacionAlarma = new NotificationCompat.Builder(contexto, CANAL_ALARMA_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(intentoPendienteAlerta)
                .setAutoCancel(true)
                .build();

        if (ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION_ALARMA, notificacionAlarma);
        }
    }

    public static void mostrarAvisoFinDescansoProximo(Context contexto) {
        Intent intentAbrirApp = new Intent(contexto, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                contexto, 3, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notificacionAviso = new NotificationCompat.Builder(contexto, CANAL_ALARMA_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle("¡Tu descanso está por terminar!")
                .setContentText("Vuelve a Miau Focus en menos de 1 minuto")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(intentoPendienteAbrirApp)
                .setAutoCancel(true)
                .build();

        if (ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION_AVISO_DESCANSO, notificacionAviso);
        }
    }

    public static void mostrarAvisoSancionInminente(Context contexto) {
        Intent intentAbrirApp = new Intent(contexto, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                contexto, 4, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notificacionAviso = new NotificationCompat.Builder(contexto, CANAL_ALARMA_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle("¡Tu descanso casi termina!")
                .setContentText("Vuelve a Miau Focus en 15 segundos para no ser sancionado")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(intentoPendienteAbrirApp)
                .setAutoCancel(true)
                .build();

        if (ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION_AVISO_SANCION, notificacionAviso);
        }
    }
}
