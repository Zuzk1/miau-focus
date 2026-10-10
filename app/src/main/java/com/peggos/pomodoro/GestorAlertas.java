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
    public static final String CANAL_AVISOS_ID = "canal_avisos_pomodoro";
    public static final int ID_NOTIFICACION_ALARMA = 1002;
    public static final int ID_NOTIFICACION_BLOQUEO = 1003;
    public static final String EXTRA_ES_INFRACCION = "extra_es_infraccion";
    public static final String PREFS_NOMBRE_COMPARTIDO = "PreferenciasMiauFocus";
    public static final int ID_NOTIFICACION_AVISO_DESCANSO = 1004;
    public static final int ID_NOTIFICACION_INFRACCION = 1005;
    public static final int ID_NOTIFICACION_AVISO_SANCION = 1006;
    private static final String PREFS_NOMBRE = "PrefsAlertas";
    private static final String CLAVE_INFRACCION_PENDIENTE = "infraccion_pendiente";
    private static final String CLAVE_TIEMPO_RESTANTE_GUARDADO = "tiempo_restante_guardado";
    private static final String CLAVE_ES_DESCANSO_GUARDADO = "es_descanso_guardado";
    private static final String CLAVE_CICLO_ACTUAL_GUARDADO = "ciclo_actual_guardado";
    private static final String CLAVE_CRONOMETRO_CORRIENDO = "cronometro_corriendo";
    private static final String CLAVE_TIEMPO_FIN_ESTIMADO_ACTUAL = "tiempo_fin_estimado_actual";
    private static final String CLAVE_ES_DESCANSO_ACTUAL = "es_descanso_actual";
    private static final String CLAVE_CICLO_ACTUAL_ACTUAL = "ciclo_actual_actual";
    private static final String CLAVE_ALERTA_ACTIVA = "alerta_activa";
    private static final String CLAVE_REANUDAR_TRAS_INFRACCION = "reanudar_tras_infraccion";
    private static final String CLAVE_TIEMPO_RESTANTE_CONGELADO = "tiempo_restante_congelado";
    private static final String CLAVE_GRACIA_INFRACCION_HASTA = "gracia_infraccion_hasta";
    private static final long DURACION_GRACIA_POST_DESCANSO = 10000L;

    public static void crearCanales(Context contexto) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager gestorNotificaciones = contexto.getSystemService(NotificationManager.class);

        NotificationChannel canalProgreso = new NotificationChannel(
                CANAL_PROGRESO_ID, contexto.getString(R.string.canal_progreso_nombre), NotificationManager.IMPORTANCE_LOW);
        canalProgreso.setDescription(contexto.getString(R.string.canal_progreso_desc));
        gestorNotificaciones.createNotificationChannel(canalProgreso);

        NotificationChannel canalAvisos = new NotificationChannel(
                CANAL_AVISOS_ID, contexto.getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_HIGH);
        canalAvisos.setDescription(contexto.getString(R.string.canal_avisos_desc));
        canalAvisos.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        canalAvisos.enableVibration(true);
        Uri sonidoNotificacion = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        canalAvisos.setSound(sonidoNotificacion, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        gestorNotificaciones.createNotificationChannel(canalAvisos);

        NotificationChannel canalAlarma = new NotificationChannel(
                CANAL_ALARMA_ID, contexto.getString(R.string.canal_alarma_nombre), NotificationManager.IMPORTANCE_HIGH);
        canalAlarma.setDescription(contexto.getString(R.string.canal_alarma_desc));
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
                CANAL_BLOQUEO_ID, contexto.getString(R.string.canal_bloqueo_nombre), NotificationManager.IMPORTANCE_DEFAULT);
        canalBloqueo.setDescription(contexto.getString(R.string.canal_bloqueo_desc));
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
                .setContentTitle(contexto.getString(R.string.notif_bloqueo_titulo))
                .setContentText(contexto.getString(R.string.notif_bloqueo_texto))
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

        NotificationManagerCompat gestor = NotificationManagerCompat.from(contexto);
        gestor.cancel(ID_NOTIFICACION_ALARMA);
        gestor.cancel(ID_NOTIFICACION_INFRACCION);
    }

    public static boolean hayInfraccionPendiente(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_INFRACCION_PENDIENTE, false);
    }

    public static int resTituloDeFase(boolean esDescanso, int ciclo) {
        if (!esDescanso) return R.string.fase_enfoque;
        return ciclo == 1 ? R.string.fase_descanso_largo : R.string.fase_descanso_corto;
    }

    public static boolean esDescansoLargo(boolean esDescanso, int ciclo) {
        return esDescanso && ciclo == 1;
    }

    public static void guardarEstadoCronometro(Context contexto, long tiempoRestante, boolean esDescanso, int cicloActual) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .putLong(CLAVE_TIEMPO_RESTANTE_GUARDADO, tiempoRestante)
                .putBoolean(CLAVE_ES_DESCANSO_GUARDADO, esDescanso)
                .putInt(CLAVE_CICLO_ACTUAL_GUARDADO, cicloActual)
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
        return contexto.getString(resTituloDeFase(
                getEsDescansoGuardado(contexto), getCicloActualGuardado(contexto)));
    }

    public static void limpiarEstadoCronometroGuardado(Context contexto) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .remove(CLAVE_TIEMPO_RESTANTE_GUARDADO)
                .remove(CLAVE_ES_DESCANSO_GUARDADO)
                .remove(CLAVE_CICLO_ACTUAL_GUARDADO)
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
            boolean esDescanso, int cicloActual) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .putLong(CLAVE_TIEMPO_FIN_ESTIMADO_ACTUAL, tiempoFinEstimado)
                .putBoolean(CLAVE_ES_DESCANSO_ACTUAL, esDescanso)
                .putInt(CLAVE_CICLO_ACTUAL_ACTUAL, cicloActual)
                .putBoolean(CLAVE_CRONOMETRO_CORRIENDO, true)
                .apply();
    }

    public static void actualizarMetadatosCronometroActual(Context contexto, boolean esDescanso,
            int cicloActual) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE).edit()
                .putBoolean(CLAVE_ES_DESCANSO_ACTUAL, esDescanso)
                .putInt(CLAVE_CICLO_ACTUAL_ACTUAL, cicloActual)
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
        return contexto.getString(resTituloDeFase(
                getEsDescansoActual(contexto), getCicloActualActual(contexto)));
    }

    public static void setAlertaActiva(Context contexto, boolean activa) {
        contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .edit().putBoolean(CLAVE_ALERTA_ACTIVA, activa).apply();
    }

    public static boolean isAlertaActiva(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(CLAVE_ALERTA_ACTIVA, false);
    }

    public static boolean debeSancionarPorDistraccion(Context contexto, boolean esDescanso, long tiempoRestanteFase) {
        if (esDescanso) return false;
        if (hayInfraccionPendiente(contexto)) return false;
        if (estaEnPeriodoGraciaPostDescanso(contexto)) return false;
        if (VigilanteSesionApp.seBloqueoConLaAppAlFrente(contexto)) return false;

        GestorConfiguracion configuracion = new GestorConfiguracion(contexto);
        if (!configuracion.isAlertasDistraccionHabilitadas()) return false;

        if (getCicloActualActual(contexto) > 1) return true;

        long duracionEnfoque = configuracion.getMinutosTrabajo() * 60000L;
        long transcurrido = duracionEnfoque - tiempoRestanteFase;
        return transcurrido >= CronometroActivity.UMBRAL_SIN_PENALIZACION;
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

    public static long getTiempoCongelado(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getLong(CLAVE_TIEMPO_RESTANTE_CONGELADO, -1L);
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

        android.app.Notification.Builder constructor =
                new android.app.Notification.Builder(contexto, CANAL_ALARMA_ID)
                        .setSmallIcon(R.drawable.ic_gato_notificacion)
                        .setContentTitle(titulo)
                        .setContentText(mensaje)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setCategory(android.app.Notification.CATEGORY_ALARM)
                        .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(intentoPendienteAlerta);

        IslaDinamica.aplicarAlerta(contexto, constructor, contexto.getString(R.string.chip_vuelve));

        if (ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION_ALARMA, constructor.build());
        }
    }

    public static void mostrarAvisoFinDescansoProximo(Context contexto) {
        Intent intentAbrirApp = new Intent(contexto, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                contexto, 3, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notificacionAviso = new NotificationCompat.Builder(contexto, CANAL_AVISOS_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(contexto.getString(R.string.notif_aviso_fin_descanso_titulo))
                .setContentText(contexto.getString(R.string.notif_aviso_fin_descanso_texto))
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

        android.app.Notification notificacionAviso = new NotificationCompat.Builder(contexto, CANAL_AVISOS_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(contexto.getString(R.string.notif_aviso_sancion_titulo))
                .setContentText(contexto.getString(R.string.notif_aviso_sancion_texto))
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
