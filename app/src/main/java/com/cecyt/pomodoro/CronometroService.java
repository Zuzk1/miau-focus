package com.cecyt.pomodoro;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class CronometroService extends Service {

    public static final String ACCION_INICIAR = "com.cecyt.pomodoro.ACCION_INICIAR_SERVICIO";
    public static final String ACCION_DETENER = "com.cecyt.pomodoro.ACCION_DETENER_SERVICIO";
    public static final String ACCION_PAUSAR = "com.cecyt.pomodoro.ACCION_PAUSAR_SERVICIO";
    public static final String EXTRA_TIEMPO_RESTANTE = "extra_tiempo_restante";
    public static final String EXTRA_ES_DESCANSO = "extra_es_descanso";

    private static final int ID_NOTIFICACION_PROGRESO = 1001;

    private static final String ETIQUETA_DEPURACION = "DepuracionCronometro";

    private static final long UMBRAL_AVISO_FIN_DESCANSO = 60000L;

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private Runnable accionFinalizacion;
    private Runnable accionAvisoFinDescanso;
    private long finEstimadoActual;
    private boolean esDescansoActual;
    private long tiempoRestantePausado = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(ETIQUETA_DEPURACION, "Servicio: onCreate");
        GestorAlertas.crearCanales(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(ETIQUETA_DEPURACION, "Servicio: onStartCommand con intent nulo, deteniendo");
            detenerServicio();
            return START_NOT_STICKY;
        }

        String accion = intent.getAction();
        Log.d(ETIQUETA_DEPURACION, "Servicio: onStartCommand accion=" + accion);
        if (ACCION_DETENER.equals(accion)) {
            detenerServicio();
            return START_NOT_STICKY;
        }

        if (ACCION_PAUSAR.equals(accion)) {
            if (accionFinalizacion == null) {
                detenerServicio();
                return START_NOT_STICKY;
            }
            startForeground(ID_NOTIFICACION_PROGRESO, construirNotificacionProgreso(finEstimadoActual, esDescansoActual));
            pausarSeguimiento();
            return START_STICKY;
        }

        startForeground(ID_NOTIFICACION_PROGRESO, construirNotificacionProgreso(finEstimadoActual, esDescansoActual));

        long tiempoRestante = intent.getLongExtra(EXTRA_TIEMPO_RESTANTE, 0);
        boolean esDescanso = intent.getBooleanExtra(EXTRA_ES_DESCANSO, false);
        Log.d(ETIQUETA_DEPURACION, "Servicio: iniciando seguimiento, tiempoRestante=" + tiempoRestante + " esDescanso=" + esDescanso);
        iniciarSeguimiento(tiempoRestante, esDescanso);
        return START_STICKY;
    }

    private void iniciarSeguimiento(long tiempoRestante, boolean esDescanso) {
        if (accionFinalizacion != null) {
            manejador.removeCallbacks(accionFinalizacion);
        }
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }

        long finEstimado = System.currentTimeMillis() + tiempoRestante;
        finEstimadoActual = finEstimado;
        esDescansoActual = esDescanso;

        Log.d(ETIQUETA_DEPURACION, "Servicio: llamando startForeground");
        startForeground(ID_NOTIFICACION_PROGRESO, construirNotificacionProgreso(finEstimado, esDescanso));

        accionFinalizacion = () -> {
            Log.d(ETIQUETA_DEPURACION, "Servicio: tiempo agotado, notificando finalizacion");
            avanzarFaseEnSegundoPlano(esDescanso);
            notificarFinalizacion(esDescanso);
            detenerServicio();
        };
        manejador.postDelayed(accionFinalizacion, tiempoRestante);

        if (esDescanso && tiempoRestante > UMBRAL_AVISO_FIN_DESCANSO) {
            accionAvisoFinDescanso = () -> GestorAlertas.mostrarAvisoFinDescansoProximo(this);
            manejador.postDelayed(accionAvisoFinDescanso, tiempoRestante - UMBRAL_AVISO_FIN_DESCANSO);
        }
    }

    private void avanzarFaseEnSegundoPlano(boolean esDescanso) {
        int cicloActual = GestorAlertas.getCicloActualActual(this);
        GestorCiclos.Resultado resultado = GestorCiclos.procesarFinDeFase(this, esDescanso, cicloActual);

        GestorAlertas.actualizarMetadatosCronometroActual(this,
                resultado.esDescansoSiguiente, resultado.cicloSiguiente, resultado.tituloSiguiente);
        GestorAlertas.setCronometroCorriendo(this, false);

        GestorAlertas.guardarEstadoCronometro(this, resultado.tiempoSiguiente,
                resultado.esDescansoSiguiente, resultado.cicloSiguiente, resultado.tituloSiguiente);

        //HOLAAAA
        if (esDescanso && !resultado.esDescansoSiguiente) {
            GestorAlertas.marcarPeriodoGraciaPostDescanso(this);
        }
    }

    private void pausarSeguimiento() {
        if (accionFinalizacion == null) {
            return;
        }
        manejador.removeCallbacks(accionFinalizacion);
        accionFinalizacion = null;
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }

        tiempoRestantePausado = finEstimadoActual - System.currentTimeMillis();
        if (tiempoRestantePausado < 0) {
            tiempoRestantePausado = 0;
        }
        Log.d(ETIQUETA_DEPURACION, "Servicio: pausado, tiempoRestantePausado=" + tiempoRestantePausado);

        android.app.Notification notificacionPausada = new NotificationCompat.Builder(this, GestorAlertas.CANAL_PROGRESO_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(esDescansoActual ? "Descanso en pausa" : "Sesión de enfoque en pausa")
                .setContentText("Atiende la alerta para continuar")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        NotificationManagerCompat.from(this).notify(ID_NOTIFICACION_PROGRESO, notificacionPausada);
    }

    private android.app.Notification construirNotificacionProgreso(long finEstimado, boolean esDescanso) {
        Intent intentAbrirApp = new Intent(this, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                this, 0, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, GestorAlertas.CANAL_PROGRESO_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(esDescanso ? "Descanso en curso" : "Sesión de enfoque en curso")
                .setContentText("Toca para volver a Miau Focus")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(finEstimado)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(intentoPendienteAbrirApp)
                .build();
    }

    private void notificarFinalizacion(boolean esDescanso) {
        Log.d(ETIQUETA_DEPURACION, "Servicio: notificarFinalizacion, permisoNotificaciones="
                + (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
        Intent intentAlerta = new Intent(this, alertaActivity.class);
        intentAlerta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent intentoPendienteAlerta = PendingIntent.getActivity(
                this, 1, intentAlerta, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notificacionAlarma = new NotificationCompat.Builder(this, GestorAlertas.CANAL_ALARMA_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(esDescanso ? "¡El descanso terminó!" : "¡La sesión de enfoque terminó!")
                .setContentText("Toca para continuar")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(intentoPendienteAlerta, true)
                .setContentIntent(intentoPendienteAlerta)
                .setAutoCancel(true)
                .build();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(GestorAlertas.ID_NOTIFICACION_ALARMA, notificacionAlarma);
        }
    }

    private void detenerServicio() {
        Log.d(ETIQUETA_DEPURACION, "Servicio: detenerServicio");
        if (accionFinalizacion != null) {
            manejador.removeCallbacks(accionFinalizacion);
            accionFinalizacion = null;
        }
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(ETIQUETA_DEPURACION, "Servicio: onTaskRemoved");

        if (accionFinalizacion == null && tiempoRestantePausado < 0) {
            detenerServicio();
            return;
        }

        long tiempoRestante = tiempoRestantePausado >= 0
                ? tiempoRestantePausado
                : finEstimadoActual - System.currentTimeMillis();
        if (tiempoRestante < 0) tiempoRestante = 0;

        if (accionFinalizacion != null && !esDescansoActual
                && !GestorAlertas.estaEnPeriodoGraciaPostDescanso(this)
                && new GestorConfiguracion(this).isAlertasDistraccionHabilitadas()
                && !GestorAlertas.hayInfraccionPendiente(this)) {
            GestorAlertas.marcarInfraccionPendiente(this);
            GestorAlertas.marcarReanudarTrasInfraccion(this);
            new GestorEstadisticas(this).registrarFallo();
            GestorAlertas.congelarTiempoRestante(this, tiempoRestante);
        }

        GestorAlertas.guardarEstadoCronometro(this, tiempoRestante,
                esDescansoActual, GestorAlertas.getCicloActualActual(this), GestorAlertas.getTituloActual(this));
        GestorAlertas.setCronometroCorriendo(this, false);

        detenerServicio();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (accionFinalizacion != null) {
            manejador.removeCallbacks(accionFinalizacion);
            accionFinalizacion = null;
        }
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
