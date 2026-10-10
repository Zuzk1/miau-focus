package com.peggos.pomodoro;

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

    public static final String ACCION_INICIAR = "com.peggos.pomodoro.ACCION_INICIAR_SERVICIO";
    public static final String ACCION_DETENER = "com.peggos.pomodoro.ACCION_DETENER_SERVICIO";
    public static final String ACCION_PAUSAR = "com.peggos.pomodoro.ACCION_PAUSAR_SERVICIO";
    public static final String EXTRA_TIEMPO_RESTANTE = "extra_tiempo_restante";
    public static final String EXTRA_ES_DESCANSO = "extra_es_descanso";

    private static final int ID_NOTIFICACION_PROGRESO = 1001;

    private static final String ETIQUETA_DEPURACION = "DepuracionCronometro";

    private static final long UMBRAL_AVISO_FIN_DESCANSO = 60000L;
    private static final long UMBRAL_AVISO_SANCION_DESCANSO = 15000L;

    private static final String ETIQUETA_BLOQUEO_DESPERTAR = "MiauFocus:cronometro";
    private static final long MARGEN_BLOQUEO_DESPERTAR = 60000L;

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private android.os.PowerManager.WakeLock bloqueoDespertar;
    private Runnable accionFinalizacion;
    private Runnable accionAvisoFinDescanso;
    private Runnable accionAvisoSancionDescanso;
    private Runnable accionRefrescoIsla;
    private Runnable accionVigilanciaEnfoque;
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
            startForeground(ID_NOTIFICACION_PROGRESO, construirNotificacionPausa());
            if (accionFinalizacion == null) {
                detenerServicio();
                return START_NOT_STICKY;
            }
            pausarSeguimiento();
            return START_STICKY;
        }

        long tiempoRestante = intent.getLongExtra(EXTRA_TIEMPO_RESTANTE, 0);
        boolean esDescanso = intent.getBooleanExtra(EXTRA_ES_DESCANSO, false);
        Log.d(ETIQUETA_DEPURACION, "Servicio: iniciando seguimiento, tiempoRestante=" + tiempoRestante + " esDescanso=" + esDescanso);
        iniciarSeguimiento(tiempoRestante, esDescanso);
        return START_STICKY;
    }

    private void iniciarSeguimiento(long tiempoRestante, boolean esDescanso) {
        cancelarVigilanciaEnfoque();
        if (accionFinalizacion != null) {
            manejador.removeCallbacks(accionFinalizacion);
        }
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }
        if (accionAvisoSancionDescanso != null) {
            manejador.removeCallbacks(accionAvisoSancionDescanso);
            accionAvisoSancionDescanso = null;
        }

        long finEstimado = System.currentTimeMillis() + tiempoRestante;
        finEstimadoActual = finEstimado;
        esDescansoActual = esDescanso;

        Log.d(ETIQUETA_DEPURACION, "Servicio: llamando startForeground");
        startForeground(ID_NOTIFICACION_PROGRESO, construirNotificacionProgreso(finEstimado, esDescanso));

        adquirirBloqueoDespertar(tiempoRestante);
        programarRefrescoIsla();

        accionFinalizacion = () -> {
            boolean eraDescansoLargo = GestorAlertas.esDescansoLargo(
                    esDescanso, GestorAlertas.getCicloActualActual(this));
            GestorCiclos.Resultado resultado = avanzarFaseEnSegundoPlano(esDescanso);
            boolean encadena = !eraDescansoLargo
                    && !(resultado.completoLosCuatroCiclos && !resultado.esDescansoSiguiente);
            Log.d(ETIQUETA_DEPURACION, "Servicio: tiempo agotado, encadena=" + encadena);

            if (esDescanso || !encadena) {
                notificarFinalizacion(esDescanso, encadena, eraDescansoLargo);
            }

            if (encadena) {
                encadenarFase(resultado);
            } else {
                detenerTrasFase(resultado);
                detenerServicio();
            }
        };
        manejador.postDelayed(accionFinalizacion, tiempoRestante);

        if (esDescanso && tiempoRestante > UMBRAL_AVISO_FIN_DESCANSO) {
            accionAvisoFinDescanso = () -> GestorAlertas.mostrarAvisoFinDescansoProximo(this);
            manejador.postDelayed(accionAvisoFinDescanso, tiempoRestante - UMBRAL_AVISO_FIN_DESCANSO);
        }

        if (esDescanso && tiempoRestante > UMBRAL_AVISO_SANCION_DESCANSO) {
            accionAvisoSancionDescanso = () -> GestorAlertas.mostrarAvisoSancionInminente(this);
            manejador.postDelayed(accionAvisoSancionDescanso, tiempoRestante - UMBRAL_AVISO_SANCION_DESCANSO);
        }
    }

    private GestorCiclos.Resultado avanzarFaseEnSegundoPlano(boolean esDescanso) {
        int cicloActual = GestorAlertas.getCicloActualActual(this);
        GestorCiclos.Resultado resultado = GestorCiclos.procesarFinDeFase(this, esDescanso, cicloActual);
        Log.d(ETIQUETA_DEPURACION, "Servicio: finDeFase bg de(descanso=" + esDescanso + ",ciclo=" + cicloActual
                + ") -> (descanso=" + resultado.esDescansoSiguiente + ",ciclo=" + resultado.cicloSiguiente
                + ")");

        GestorAlertas.consumirReanudarTrasInfraccion(this);
        GestorAlertas.obtenerYLimpiarTiempoCongelado(this);

        if (esDescanso && !resultado.esDescansoSiguiente) {
            GestorAlertas.marcarPeriodoGraciaPostDescanso(this);
        }

        return resultado;
    }

    private void encadenarFase(GestorCiclos.Resultado resultado) {
        GestorAlertas.limpiarEstadoCronometroGuardado(this);
        GestorAlertas.guardarEstadoCronometroActual(this,
                System.currentTimeMillis() + resultado.tiempoSiguiente,
                resultado.esDescansoSiguiente, resultado.cicloSiguiente);
        iniciarSeguimiento(resultado.tiempoSiguiente, resultado.esDescansoSiguiente);

        if (!resultado.esDescansoSiguiente) {
            programarVigilanciaEnfoque();
        }
    }

    private void programarVigilanciaEnfoque() {
        cancelarVigilanciaEnfoque();
        if (!new GestorConfiguracion(this).isAlertasDistraccionHabilitadas()) return;

        accionVigilanciaEnfoque = () -> {
            accionVigilanciaEnfoque = null;
            if (VigilanteSesionApp.estaEnPrimerPlano()) return;

            long restante = finEstimadoActual - System.currentTimeMillis();
            if (restante < 0) restante = 0;
            if (!GestorAlertas.debeSancionarPorDistraccion(this, esDescansoActual, restante)) return;

            Log.d(ETIQUETA_DEPURACION, "Servicio: registrarFallo por no volver al enfoque");
            GestorAlertas.marcarInfraccionPendiente(this);
            GestorAlertas.marcarReanudarTrasInfraccion(this);
            new GestorEstadisticas(this).registrarFallo();
            GestorAlertas.congelarTiempoRestante(this, restante);
            GestorAlertas.guardarEstadoCronometro(this, restante, esDescansoActual,
                    GestorAlertas.getCicloActualActual(this));
            GestorAlertas.setCronometroCorriendo(this, false);
            pausarSeguimiento();
            mostrarAlertaInfraccion();
        };
        manejador.postDelayed(accionVigilanciaEnfoque, CronometroActivity.UMBRAL_SIN_PENALIZACION + 500L);
    }

    private void adquirirBloqueoDespertar(long tiempoRestante) {
        liberarBloqueoDespertar();
        android.os.PowerManager gestorEnergia =
                (android.os.PowerManager) getSystemService(POWER_SERVICE);
        if (gestorEnergia == null) return;
        bloqueoDespertar = gestorEnergia.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK, ETIQUETA_BLOQUEO_DESPERTAR);
        bloqueoDespertar.setReferenceCounted(false);
        bloqueoDespertar.acquire(tiempoRestante + MARGEN_BLOQUEO_DESPERTAR);
    }

    private void liberarBloqueoDespertar() {
        if (bloqueoDespertar == null) return;
        if (bloqueoDespertar.isHeld()) {
            bloqueoDespertar.release();
        }
        bloqueoDespertar = null;
    }

    private void cancelarVigilanciaEnfoque() {
        if (accionVigilanciaEnfoque != null) {
            manejador.removeCallbacks(accionVigilanciaEnfoque);
            accionVigilanciaEnfoque = null;
        }
    }

    private void detenerTrasFase(GestorCiclos.Resultado resultado) {
        GestorAlertas.actualizarMetadatosCronometroActual(this,
                resultado.esDescansoSiguiente, resultado.cicloSiguiente);
        GestorAlertas.setCronometroCorriendo(this, false);
        GestorAlertas.guardarEstadoCronometro(this, resultado.tiempoSiguiente,
                resultado.esDescansoSiguiente, resultado.cicloSiguiente);
    }

    private void pausarSeguimiento() {
        if (accionFinalizacion == null) {
            return;
        }
        manejador.removeCallbacks(accionFinalizacion);
        accionFinalizacion = null;
        detenerRefrescoIsla();
        cancelarVigilanciaEnfoque();
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }
        if (accionAvisoSancionDescanso != null) {
            manejador.removeCallbacks(accionAvisoSancionDescanso);
            accionAvisoSancionDescanso = null;
        }

        tiempoRestantePausado = finEstimadoActual - System.currentTimeMillis();
        if (tiempoRestantePausado < 0) {
            tiempoRestantePausado = 0;
        }
        Log.d(ETIQUETA_DEPURACION, "Servicio: pausado, tiempoRestantePausado=" + tiempoRestantePausado);

        liberarBloqueoDespertar();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(ID_NOTIFICACION_PROGRESO, construirNotificacionPausa());
        }
    }

    private android.app.Notification construirNotificacionPausa() {
        Intent intentAbrirApp = new Intent(this, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                this, 0, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder constructor =
                new android.app.Notification.Builder(this, GestorAlertas.CANAL_PROGRESO_ID)
                        .setSmallIcon(R.drawable.ic_gato_notificacion)
                        .setContentTitle(getString(esDescansoActual ? R.string.notif_descanso_pausa : R.string.notif_enfoque_pausa))
                        .setContentText(getString(R.string.notif_pausa_texto))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .setForegroundServiceBehavior(android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE)
                        .setContentIntent(intentoPendienteAbrirApp);

        IslaDinamica.aplicarAlerta(this, constructor, getString(R.string.chip_vuelve));

        return constructor.build();
    }

    private android.app.Notification construirNotificacionProgreso(long finEstimado, boolean esDescanso) {
        return construirNotificacionProgreso(finEstimado, esDescanso, true);
    }

    private android.app.Notification construirNotificacionProgreso(long finEstimado, boolean esDescanso,
            boolean primeraVez) {
        Intent intentAbrirApp = new Intent(this, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                this, 0, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String titulo = getString(esDescanso ? R.string.notif_descanso_curso : R.string.notif_enfoque_curso);
        long tiempoRestante = finEstimado - System.currentTimeMillis();
        if (tiempoRestante < 0) tiempoRestante = 0;

        android.app.Notification.Builder constructor =
                new android.app.Notification.Builder(this, GestorAlertas.CANAL_PROGRESO_ID)
                        .setSmallIcon(R.drawable.ic_gato_notificacion)
                        .setContentTitle(titulo)
                        .setContentText(getString(R.string.notif_progreso_texto))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setUsesChronometer(true)
                        .setChronometerCountDown(true)
                        .setWhen(finEstimado)
                        .setShowWhen(true)
                        .setCategory(android.app.Notification.CATEGORY_STOPWATCH)
                        .setForegroundServiceBehavior(android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE)
                        .setContentIntent(intentoPendienteAbrirApp);

        IslaDinamica.aplicar(this, constructor, getString(esDescanso ? R.string.fase_corta_descanso : R.string.fase_corta_enfoque),
                obtenerTareaActiva(), tiempoRestante, duracionFaseActual(esDescanso), primeraVez, esDescanso);

        return constructor.build();
    }

    private long duracionFaseActual(boolean esDescanso) {
        GestorConfiguracion configuracion = new GestorConfiguracion(this);
        if (!esDescanso) {
            return configuracion.getMinutosTrabajo() * 60000L;
        }
        return GestorAlertas.getCicloActualActual(this) == 1
                ? configuracion.getMinutosDescansoLargo() * 60000L
                : configuracion.getMinutosDescansoCorto() * 60000L;
    }

    private String obtenerTareaActiva() {
        String tareaActiva = getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, MODE_PRIVATE)
                .getString("tarea_activa_actual", "Ninguna");
        if (tareaActiva == null || tareaActiva.isEmpty() || tareaActiva.equals("Ninguna")) {
            return getString(R.string.app_name);
        }
        return tareaActiva;
    }

    private void programarRefrescoIsla() {
        detenerRefrescoIsla();
        accionRefrescoIsla = new Runnable() {
            @Override
            public void run() {
                if (accionFinalizacion == null) return;

                if (finEstimadoActual - System.currentTimeMillis() <= 0) {
                    Log.d(ETIQUETA_DEPURACION, "Servicio: fin de fase disparado por reloj de pared");
                    Runnable finPendiente = accionFinalizacion;
                    manejador.removeCallbacks(finPendiente);
                    accionFinalizacion = null;
                    detenerRefrescoIsla();
                    finPendiente.run();
                    return;
                }

                refrescarNotificacionProgreso();
                manejador.postDelayed(this, IslaDinamica.INTERVALO_REFRESCO);
            }
        };
        manejador.postDelayed(accionRefrescoIsla, IslaDinamica.INTERVALO_REFRESCO);
    }

    private void detenerRefrescoIsla() {
        if (accionRefrescoIsla != null) {
            manejador.removeCallbacks(accionRefrescoIsla);
            accionRefrescoIsla = null;
        }
    }

    private void refrescarNotificacionProgreso() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify(ID_NOTIFICACION_PROGRESO,
                construirNotificacionProgreso(finEstimadoActual, esDescansoActual, false));
    }

    private void notificarFinalizacion(boolean esDescanso, boolean encadena, boolean eraDescansoLargo) {
        Log.d(ETIQUETA_DEPURACION, "Servicio: notificarFinalizacion, permisoNotificaciones="
                + (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
        Intent intentAbrirApp = new Intent(this, CronometroActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intentoPendienteAbrirApp = PendingIntent.getActivity(
                this, 1, intentAbrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String titulo;
        String texto;
        if (eraDescansoLargo) {
            titulo = getString(R.string.notif_descanso_largo_terminado);
            texto = getString(R.string.notif_completaste_sesion);
        } else if (!esDescanso) {
            titulo = getString(R.string.notif_completaste_sesion);
            texto = getString(R.string.notif_terminaste_4_ciclos);
        } else if (encadena) {
            titulo = getString(R.string.notif_descanso_termino);
            texto = getString(R.string.notif_enfoque_ya_comenzo);
        } else {
            titulo = getString(R.string.notif_descanso_termino);
            texto = getString(R.string.notif_toca_continuar);
        }

        android.app.Notification notificacionAlarma = new NotificationCompat.Builder(this, GestorAlertas.CANAL_AVISOS_ID)
                .setSmallIcon(R.drawable.ic_gato_notificacion)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(intentoPendienteAbrirApp)
                .setAutoCancel(true)
                .build();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(GestorAlertas.ID_NOTIFICACION_ALARMA, notificacionAlarma);
        }
    }

    private void mostrarAlertaInfraccion() {
        Intent intentAlerta = new Intent(this, alertaActivity.class);
        intentAlerta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentAlerta.putExtra(GestorAlertas.EXTRA_ES_INFRACCION, true);
        PendingIntent pi = PendingIntent.getActivity(this, 10, intentAlerta,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder constructor =
                new android.app.Notification.Builder(this, GestorAlertas.CANAL_ALARMA_ID)
                        .setSmallIcon(R.drawable.ic_gato_notificacion)
                        .setContentTitle(getString(R.string.notif_distraccion_titulo))
                        .setContentText(getString(R.string.notif_distraccion_texto))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setCategory(android.app.Notification.CATEGORY_ALARM)
                        .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                        .setFullScreenIntent(pi, true)
                        .setContentIntent(pi);

        IslaDinamica.aplicarAlerta(this, constructor, getString(R.string.chip_vuelve));

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(GestorAlertas.ID_NOTIFICACION_INFRACCION, constructor.build());
        }
    }

    private void detenerServicio() {
        Log.d(ETIQUETA_DEPURACION, "Servicio: detenerServicio");
        liberarBloqueoDespertar();
        detenerRefrescoIsla();
        cancelarVigilanciaEnfoque();
        if (accionFinalizacion != null) {
            manejador.removeCallbacks(accionFinalizacion);
            accionFinalizacion = null;
        }
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }
        if (accionAvisoSancionDescanso != null) {
            manejador.removeCallbacks(accionAvisoSancionDescanso);
            accionAvisoSancionDescanso = null;
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

        if (accionFinalizacion != null
                && GestorAlertas.debeSancionarPorDistraccion(this, esDescansoActual, tiempoRestante)) {
            Log.d(ETIQUETA_DEPURACION, "Servicio: registrarFallo desde onTaskRemoved");
            GestorAlertas.marcarInfraccionPendiente(this);
            GestorAlertas.marcarReanudarTrasInfraccion(this);
            new GestorEstadisticas(this).registrarFallo();
            GestorAlertas.congelarTiempoRestante(this, tiempoRestante);
            mostrarAlertaInfraccion();
        }

        GestorAlertas.guardarEstadoCronometro(this, tiempoRestante,
                esDescansoActual, GestorAlertas.getCicloActualActual(this));
        GestorAlertas.setCronometroCorriendo(this, false);

        detenerServicio();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        liberarBloqueoDespertar();
        detenerRefrescoIsla();
        cancelarVigilanciaEnfoque();
        if (accionFinalizacion != null) {
            manejador.removeCallbacks(accionFinalizacion);
            accionFinalizacion = null;
        }
        if (accionAvisoFinDescanso != null) {
            manejador.removeCallbacks(accionAvisoFinDescanso);
            accionAvisoFinDescanso = null;
        }
        if (accionAvisoSancionDescanso != null) {
            manejador.removeCallbacks(accionAvisoSancionDescanso);
            accionAvisoSancionDescanso = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
