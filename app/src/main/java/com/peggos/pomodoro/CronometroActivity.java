package com.peggos.pomodoro;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class CronometroActivity extends BaseActivity {

    private CountDownTimer temporizador;

    private long TIEMPO_ENFOQUE = 1500000;
    private long TIEMPO_DESCANSO_CORTO = 300000;
    private long TIEMPO_DESCANSO_LARGO = 900000;

    public static final long UMBRAL_SIN_PENALIZACION = 10000;
    public static final long UMBRAL_SANCION = 60000;

    public static final String EXTRA_OMITIR_SPLASH = "extra_omitir_splash";

    private static final String CLAVE_PERMISO_NOTIFICACIONES_BLOQUEADO = "permiso_notificaciones_bloqueado";

    private long tiempoRestante = TIEMPO_ENFOQUE;
    private boolean estaCorriendo = false;
    private boolean esDescanso = false;
    private int cicloActual = 1;

    private TextView textoTiempo;
    private TextView tvTitulo;
    private TextView tvSubtitulo;
    private CircularProgressIndicator barraProgreso;
    private FloatingActionButton botonPausar;
    private FloatingActionButton botonRenunciar;
    private LottieAnimationView gatoAnimado;

    private TextView tvCiclo1, tvCiclo2, tvCiclo3, tvCiclo4;

    private ObjectAnimator animacionBarra;
    private ObjectAnimator animacionTexto;

    private ValueAnimator ralentizador;

    private Handler handlerCiclos = new Handler(Looper.getMainLooper());
    private Runnable runnableCiclos;
    private static final long INTERVALO_ANIMACION_CICLOS = 10000;

    private long startTimeBase;
    private long tiempoFinEstimado;

    private ValueAnimator animacionNavegacion;

    private ActivityResultLauncher<String> solicitarPermisoNotificaciones;

    private static final String ETIQUETA_DEPURACION = "DepuracionCronometro";

    private enum ModoBotonEscudo { OCULTO, ESCUDO, SALTAR }
    private FloatingActionButton botonEscudo;
    private boolean sesionProtegida = false;
    private boolean descansoLargoCache = false;
    private ModoBotonEscudo modoBotonEscudo = ModoBotonEscudo.OCULTO;

    @Override
    protected void onPause() {
        super.onPause();
        if (estaCorriendo) {
            long tiempoRestanteReal = tiempoFinEstimado - SystemClock.elapsedRealtime();
            if (tiempoRestanteReal < 0) tiempoRestanteReal = 0;

            if (!esDescanso && !GestorAlertas.estaEnPeriodoGraciaPostDescanso(this)
                    && new GestorConfiguracion(this).isAlertasDistraccionHabilitadas()
                    && !GestorAlertas.hayInfraccionPendiente(this)) {
                
                Log.d(ETIQUETA_DEPURACION, "Activity: registrarFallo desde onPause");
                GestorAlertas.marcarInfraccionPendiente(this);
                GestorAlertas.marcarReanudarTrasInfraccion(this);
                new GestorEstadisticas(this).registrarFallo();
                GestorAlertas.congelarTiempoRestante(this, tiempoRestanteReal);
                GestorAlertas.setCronometroCorriendo(this, false);

                detenerServicioCronometro();

                Intent intentAlerta = new Intent(this, alertaActivity.class);
                intentAlerta.putExtra(GestorAlertas.EXTRA_ES_INFRACCION, true);
                intentAlerta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentAlerta);

                if (temporizador != null) {
                    temporizador.cancel();
                }
                return;
            }

            long finEstimadoWallClock = System.currentTimeMillis() + tiempoRestanteReal;
            GestorAlertas.guardarEstadoCronometroActual(this, finEstimadoWallClock, esDescanso, cicloActual, tvTitulo.getText().toString());

            Intent intentServicio = new Intent(this, CronometroService.class);
            intentServicio.setAction(CronometroService.ACCION_INICIAR);
            intentServicio.putExtra(CronometroService.EXTRA_TIEMPO_RESTANTE, tiempoRestanteReal);
            intentServicio.putExtra(CronometroService.EXTRA_ES_DESCANSO, esDescanso);
            ContextCompat.startForegroundService(this, intentServicio);

            if (temporizador != null) {
                temporizador.cancel();
            }
        } else {
            boolean tieneProgreso = esDescanso || cicloActual > 1 || (tiempoRestante > 0 && tiempoRestante < TIEMPO_ENFOQUE);
            if (tieneProgreso) {
                GestorAlertas.guardarEstadoCronometro(this, tiempoRestante, esDescanso, cicloActual, tvTitulo.getText().toString());
            } else {
                GestorAlertas.limpiarEstadoCronometroGuardado(this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirigidoAAlerta) {
            return;
        }
        Log.d(ETIQUETA_DEPURACION, "onResume: deteniendo servicio si estaba activo");
        detenerServicioCronometro();
        actualizarTareaActiva();
        recargarConfiguracionTiempos();
    }

    private void recargarConfiguracionTiempos() {
        GestorConfiguracion gestorConfiguracion = new GestorConfiguracion(this);
        TIEMPO_ENFOQUE = gestorConfiguracion.getMinutosTrabajo() * 60000L;
        TIEMPO_DESCANSO_CORTO = gestorConfiguracion.getMinutosDescansoCorto() * 60000L;
        TIEMPO_DESCANSO_LARGO = gestorConfiguracion.getMinutosDescansoLargo() * 60000L;
        descansoLargoCache = gestorConfiguracion.isDescansoLargoHabilitado();

        if (GestorAlertas.isCronometroCorriendo(this)) {
            cicloActual = GestorAlertas.getCicloActualActual(this);
            esDescanso = GestorAlertas.getEsDescansoActual(this);
            tvTitulo.setText(GestorAlertas.getTituloActual(this));
            Log.d(ETIQUETA_DEPURACION, "restaurar(corriendo): ciclo=" + cicloActual
                    + " descanso=" + esDescanso + " titulo=" + GestorAlertas.getTituloActual(this));

            tiempoRestante = GestorAlertas.getTiempoFinEstimadoActual(this) - System.currentTimeMillis();
            if (tiempoRestante < 0) tiempoRestante = 0;

            long duracionFaseActual = esDescanso
                    ? (cicloActual == 1 ? TIEMPO_DESCANSO_LARGO : TIEMPO_DESCANSO_CORTO)
                    : TIEMPO_ENFOQUE;
            barraProgreso.setIndicatorColor(esDescanso
                    ? getColor(R.color.color_verde_exito)
                    : TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
            barraProgreso.setMax((int) duracionFaseActual);
            actualizarIndicadoresSegunEstado();
            actualizarInterfaz();
            iniciarCronometro();
            return;
        }

        boolean debeReanudarTrasInfraccion = GestorAlertas.consumirReanudarTrasInfraccion(this);
        long tiempoCongelado = GestorAlertas.obtenerYLimpiarTiempoCongelado(this);

        long duracionFaseActual;
        long tiempoGuardado = tiempoCongelado >= 0 ? tiempoCongelado : GestorAlertas.getTiempoRestanteGuardado(this);

        if (tiempoGuardado >= 0) {
            tiempoRestante = tiempoGuardado;
            esDescanso = GestorAlertas.getEsDescansoGuardado(this);
            cicloActual = GestorAlertas.getCicloActualGuardado(this);
            tvTitulo.setText(GestorAlertas.getTituloGuardado(this));
            Log.d(ETIQUETA_DEPURACION, "restaurar(guardado): ciclo=" + cicloActual
                    + " descanso=" + esDescanso + " titulo=" + GestorAlertas.getTituloGuardado(this));
            barraProgreso.setIndicatorColor(esDescanso
                    ? getColor(R.color.color_verde_exito)
                    : TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
            GestorAlertas.limpiarEstadoCronometroGuardado(this);
            duracionFaseActual = esDescanso
                    ? (cicloActual == 1 ? TIEMPO_DESCANSO_LARGO : TIEMPO_DESCANSO_CORTO)
                    : TIEMPO_ENFOQUE;
        } else {
            if (esDescanso) {
                tiempoRestante = cicloActual == 1 ? TIEMPO_DESCANSO_LARGO : TIEMPO_DESCANSO_CORTO;
            } else {
                tiempoRestante = TIEMPO_ENFOQUE;
            }
            duracionFaseActual = tiempoRestante;
        }

        actualizarIndicadoresSegunEstado();
        barraProgreso.setMax((int) duracionFaseActual);
        actualizarInterfaz();

        if (debeReanudarTrasInfraccion && tiempoGuardado >= 0) {
            iniciarCronometro();
        } else {
            estaCorriendo = false;
            actualizarEstadoIconosToolbar();
            botonPausar.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void actualizarIndicadoresSegunEstado() {
        restaurarIndicadoresGlobales();
        for (int i = 1; i < cicloActual; i++) {
            actualizarIndicadorVisual(i);
        }
        if (!esDescanso) {
            Log.d(ETIQUETA_DEPURACION, "indicadores: enfoque slotActivo=" + cicloActual);
            configurarIndicadorActual(cicloActual);
        } else {
            String titulo = tvTitulo.getText().toString();
            if ("DESCANSO LARGO".equals(titulo)) {
                Log.d(ETIQUETA_DEPURACION, "indicadores: descanso largo slot=4");
                configurarIndicadorDescanso(4);
            } else if (cicloActual > 1) {
                Log.d(ETIQUETA_DEPURACION, "indicadores: descanso corto ciclo=" + cicloActual + " slot=" + (cicloActual - 1));
                configurarIndicadorDescanso(cicloActual - 1);
            } else {
                Log.d(ETIQUETA_DEPURACION, "indicadores: descanso pero ciclo=" + cicloActual + " -> ningun slot");
            }
        }
    }

    private void iniciarFlujoSolicitudPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisoNotificacionesConTarjeta(this::solicitarPermisoPantallaCompleta);
        } else {
            solicitarPermisoPantallaCompleta();
        }
    }

    private void solicitarPermisoNotificacionesConTarjeta(Runnable alFinalizar) {
        SharedPreferences preferencias = getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE);
        if (preferencias.getBoolean(CLAVE_PERMISO_NOTIFICACIONES_BLOQUEADO, false)) {
            alFinalizar.run();
            return;
        }

        final boolean[] pidioElPermiso = {false};
        DialogoPermiso.mostrar(this,
                "¡No te pierdas el aviso!",
                "Activa las notificaciones para que Miau Focus te avise apenas termine tu sesión de enfoque o descanso.",
                "Activar notificaciones",
                () -> {
                    pidioElPermiso[0] = true;
                    solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
                },
                () -> {
                    if (!pidioElPermiso[0]) alFinalizar.run();
                });
    }

    private void solicitarPermisoPantallaCompleta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return;
        }

        SharedPreferences preferencias = getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE);
        if (preferencias.getBoolean("permiso_pantalla_completa_solicitado", false)) {
            return;
        }

        NotificationManager gestorNotificaciones = getSystemService(NotificationManager.class);
        if (gestorNotificaciones != null && !gestorNotificaciones.canUseFullScreenIntent()) {
            DialogoPermiso.mostrar(this,
                    "¡No te pierdas la alerta!",
                    "Para que la alerta de fin de sesión se muestre en pantalla completa aunque estés usando otra app, activa el permiso de pantalla completa para Miau Focus.",
                    "Configurar",
                    () -> {
                        Intent intentConfiguracion = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                        intentConfiguracion.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intentConfiguracion);
                    },
                    () -> preferencias.edit().putBoolean("permiso_pantalla_completa_solicitado", true).apply());
        }
    }

    private void detenerServicioCronometro() {
        Intent intentServicio = new Intent(this, CronometroService.class);
        intentServicio.setAction(CronometroService.ACCION_DETENER);
        startService(intentServicio);
    }

    private void actualizarTareaActiva() {
        SharedPreferences preferencias = getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE);
        String tareaActiva = preferencias.getString("tarea_activa_actual", "Ninguna");
        if (tvSubtitulo != null) {
            tvSubtitulo.setText("Enfocado en: " + tareaActiva);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerCiclos.removeCallbacks(runnableCiclos);
        if (animacionNavegacion != null) {
            animacionNavegacion.cancel();
        }
        if (!estaCorriendo) {
            detenerServicioCronometro();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cronometro_main);

        GestorConfiguracion gestorConfiguracion = new GestorConfiguracion(this);
        TIEMPO_ENFOQUE = gestorConfiguracion.getMinutosTrabajo() * 60000L;
        TIEMPO_DESCANSO_CORTO = gestorConfiguracion.getMinutosDescansoCorto() * 60000L;
        TIEMPO_DESCANSO_LARGO = gestorConfiguracion.getMinutosDescansoLargo() * 60000L;
        if (!GestorAlertas.isCronometroCorriendo(this)) {
            tiempoRestante = esDescanso ? TIEMPO_DESCANSO_CORTO : TIEMPO_ENFOQUE;
        }

        solicitarPermisoNotificaciones = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedido -> {
                    if (!concedido && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE)
                                .edit().putBoolean(CLAVE_PERMISO_NOTIFICACIONES_BLOQUEADO, true).apply();
                    }
                    solicitarPermisoPantallaCompleta();
                });

        View splashOverlay = findViewById(R.id.splashOverlay);
        boolean omitirSplash = getIntent().getBooleanExtra(EXTRA_OMITIR_SPLASH, false);
        if (savedInstanceState == null && !omitirSplash) {
            iniciarFlujoSolicitudPermisos();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                splashOverlay.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                        .start();
            }, 850);
        } else {
            splashOverlay.setVisibility(View.GONE);
        }

        textoTiempo = findViewById(R.id.tvTiempo);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        barraProgreso = findViewById(R.id.pbCronometro);
        botonPausar = findViewById(R.id.btnPausar);
        botonRenunciar = findViewById(R.id.btnRenunciar);
        botonEscudo = findViewById(R.id.btnEscudo);
        gatoAnimado = findViewById(R.id.lottieGato);
        if (new GestorTemas(this).getTemaActual().esClaro()) {
            gatoAnimado.setAnimation(R.raw.gato_tema_claro);
        }

        tvCiclo1 = findViewById(R.id.tvCiclo1);
        tvCiclo2 = findViewById(R.id.tvCiclo2);
        tvCiclo3 = findViewById(R.id.tvCiclo3);
        tvCiclo4 = findViewById(R.id.tvCiclo4);

        barraProgreso.setMax((int) TIEMPO_ENFOQUE);
        barraProgreso.setProgress((int) tiempoRestante);

        configurarAnimaciones();
        animarCirculos();

        botonPausar.setOnClickListener(v -> {
            if (estaCorriendo) {
                pausarCronometro();
            } else {
                iniciarCronometro();
            }
        });

        botonRenunciar.setOnClickListener(v -> {
            long duracionEtapaActual = esDescanso
                    ? (cicloActual == 1 ? TIEMPO_DESCANSO_LARGO : TIEMPO_DESCANSO_CORTO)
                    : TIEMPO_ENFOQUE;
            long transcurridoEtapaActual = duracionEtapaActual - tiempoRestante;
            if (transcurridoEtapaActual < UMBRAL_SIN_PENALIZACION) {
                reiniciarCronometro();
            } else {
                mostrarDialogoAdvertencia();
            }
        });

        botonEscudo.setOnClickListener(v -> {
            if (modoBotonEscudo == ModoBotonEscudo.SALTAR) {
                procesarFinDeCiclo();
            } else if (modoBotonEscudo == ModoBotonEscudo.ESCUDO) {
                DialogoEscudo.mostrar(this, () -> {
                    sesionProtegida = true;
                    marcarEscudoUsado();
                    reiniciarCronometro();
                });
            }
        });

        configurarNavegacion(R.id.nav_enfoque);
    }

    private void habilitarNavegacion(boolean habilitar) {
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setEnabled(habilitar);
        }
        animarColoresNavegacion(habilitar);

        if (habilitar) {
            reanudarAnimacionMenuSuave();
        } else {
            pausarAnimacionMenuSuave();
        }
    }

    private void animarColoresNavegacion(boolean habilitar) {
        if (animacionNavegacion != null) {
            animacionNavegacion.cancel();
        }

        int colorIconoNormal = TemaUtils.resolverColor(this, R.attr.themeTextoPrimario);
        int colorIconoApagado = TemaUtils.resolverColor(this, R.attr.themeIconoApagado);
        int colorIndicadorNormal = TemaUtils.resolverColor(this, R.attr.themeAcentoContenedor);
        int colorIndicadorApagado = TemaUtils.resolverColor(this, R.attr.themeIndicadorApagado);

        int colorIconoInicio = habilitar ? colorIconoApagado : colorIconoNormal;
        int colorIconoFin = habilitar ? colorIconoNormal : colorIconoApagado;
        int colorIndicadorInicio = habilitar ? colorIndicadorApagado : colorIndicadorNormal;
        int colorIndicadorFin = habilitar ? colorIndicadorNormal : colorIndicadorApagado;

        ArgbEvaluator evaluador = new ArgbEvaluator();

        animacionNavegacion = ValueAnimator.ofFloat(0f, 1f);
        animacionNavegacion.setDuration(500);
        animacionNavegacion.addUpdateListener(animacion -> {
            float fraccion = (float) animacion.getAnimatedValue();
            int colorIcono = (int) evaluador.evaluate(fraccion, colorIconoInicio, colorIconoFin);
            int colorIndicador = (int) evaluador.evaluate(fraccion, colorIndicadorInicio, colorIndicadorFin);

            bottomNavigationView.setItemIconTintList(ColorStateList.valueOf(colorIcono));
            bottomNavigationView.setItemTextColor(ColorStateList.valueOf(colorIcono));
            bottomNavigationView.setItemActiveIndicatorColor(ColorStateList.valueOf(colorIndicador));
        });
        animacionNavegacion.start();
    }

    private void animarCirculos() {
        View circleTopLeft = findViewById(R.id.circleTopLeft);
        View circleBottomRight = findViewById(R.id.circleBottomRight);
        if (circleTopLeft != null) iniciarPulso(circleTopLeft, 4500, 0);
        if (circleBottomRight != null) iniciarPulso(circleBottomRight, 5500, 1500);
    }

    private void iniciarPulso(View vista, long duracion, long delay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(vista, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(vista, "scaleY", 1f, 1.2f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(vista, "alpha", 0.13f, 0.28f, 0.13f);

        scaleX.setDuration(duracion);
        scaleY.setDuration(duracion);
        alpha.setDuration(duracion);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        AccelerateDecelerateInterpolator interp = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interp);
        scaleY.setInterpolator(interp);
        alpha.setInterpolator(interp);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setStartDelay(delay);
        vista.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        set.start();
    }

    private void iniciarCicloAnimacionCiclos() {

        final long sessionLength = barraProgreso.getMax();
        final long elapsed = sessionLength - tiempoRestante;
        startTimeBase = SystemClock.elapsedRealtime() - elapsed;

        runnableCiclos = new Runnable() {
            @Override
            public void run() {
                long now = SystemClock.elapsedRealtime();
                long elapsedReal = now - startTimeBase;

                animarCiclosEnCascada();

                long nextTarget = ((elapsedReal / INTERVALO_ANIMACION_CICLOS) + 1) * INTERVALO_ANIMACION_CICLOS;
                long delayNext = nextTarget - elapsedReal;
                if (delayNext <= 0) delayNext = INTERVALO_ANIMACION_CICLOS;

                handlerCiclos.postDelayed(this, delayNext);
            }
        };

        long now = SystemClock.elapsedRealtime();
        long elapsedNow = now - startTimeBase;
        long nextTargetFirst = ((elapsedNow / INTERVALO_ANIMACION_CICLOS) + 1) * INTERVALO_ANIMACION_CICLOS;
        long delayFirst = nextTargetFirst - elapsedNow;
        if (delayFirst <= 0) delayFirst = INTERVALO_ANIMACION_CICLOS;
        handlerCiclos.postDelayed(runnableCiclos, delayFirst);
    }

    private void animarCiclosEnCascada() {
        TextView[] ciclos = {tvCiclo1, tvCiclo2, tvCiclo3, tvCiclo4};
        long delayBase = 0;

        for (TextView ciclo : ciclos) {
            if (ciclo == null) continue;
            long delay = delayBase;

            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(ciclo, "scaleX", 1f, 1.35f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(ciclo, "scaleY", 1f, 1.35f);
            ObjectAnimator translateUp = ObjectAnimator.ofFloat(ciclo, "translationY", 0f, -18f);

            scaleUpX.setDuration(350);
            scaleUpY.setDuration(350);
            translateUp.setDuration(350);
            translateUp.setInterpolator(new OvershootInterpolator(2f));

            AnimatorSet subida = new AnimatorSet();
            subida.playTogether(scaleUpX, scaleUpY, translateUp);

            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(ciclo, "scaleX", 1.35f, 1f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(ciclo, "scaleY", 1.35f, 1f);
            ObjectAnimator translateDown = ObjectAnimator.ofFloat(ciclo, "translationY", -18f, 0f);

            scaleDownX.setDuration(400);
            scaleDownY.setDuration(400);
            translateDown.setDuration(400);

            AnimatorSet bajada = new AnimatorSet();
            bajada.playTogether(scaleDownX, scaleDownY, translateDown);

            AnimatorSet brinco = new AnimatorSet();
            brinco.playSequentially(subida, bajada);
            brinco.setStartDelay(delay);

            final TextView cicloFinal = ciclo;
            cicloFinal.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            brinco.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    cicloFinal.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            });
            brinco.start();

            delayBase += 200;
        }
    }

    private void configurarAnimaciones() {
        animacionBarra = ObjectAnimator.ofFloat(barraProgreso, "translationY", 0f, -30f, 0f);
        animacionBarra.setDuration(5000);
        animacionBarra.setRepeatCount(ObjectAnimator.INFINITE);

        animacionTexto = ObjectAnimator.ofFloat(textoTiempo, "translationY", 0f, -30f, 0f);
        animacionTexto.setDuration(5000);
        animacionTexto.setRepeatCount(ObjectAnimator.INFINITE);
    }

    private void iniciarCronometro() {
        if (ralentizador != null && ralentizador.isRunning()) {
            ralentizador.cancel();
        }

        gatoAnimado.setSpeed(1f);

        handlerCiclos.removeCallbacks(runnableCiclos);
        iniciarCicloAnimacionCiclos();
        habilitarNavegacion(false);

        tiempoFinEstimado = SystemClock.elapsedRealtime() + tiempoRestante;
        long finEstimadoWallClock = System.currentTimeMillis() + tiempoRestante;
        GestorAlertas.guardarEstadoCronometroActual(this, finEstimadoWallClock, esDescanso, cicloActual, tvTitulo.getText().toString());

        temporizador = new CountDownTimer(tiempoRestante, 1000) {
            @Override
            public void onTick(long milisegundos) {
                tiempoRestante = milisegundos;
                actualizarInterfaz();
                verificarEstadoBotonEscudo(milisegundos);
            }

            @Override
            public void onFinish() {
                procesarFinDeCiclo();
            }
        }.start();

        estaCorriendo = true;
        actualizarEstadoIconosToolbar();
        botonPausar.setImageResource(android.R.drawable.ic_media_pause);
        gatoAnimado.resumeAnimation();

        if (animacionBarra.isPaused()) {
            animacionBarra.resume();
            animacionTexto.resume();
        } else {
            animacionBarra.start();
            animacionTexto.start();
        }
    }

    private void pausarCronometro() {
        handlerCiclos.removeCallbacks(runnableCiclos);
        habilitarNavegacion(true);

        if (temporizador != null) {
            temporizador.cancel();
        }

        tiempoRestante = tiempoFinEstimado - SystemClock.elapsedRealtime();
        if (tiempoRestante < 0) tiempoRestante = 0;

        estaCorriendo = false;
        GestorAlertas.setCronometroCorriendo(this, false);
        actualizarEstadoIconosToolbar();
        botonPausar.setImageResource(android.R.drawable.ic_media_play);

        if (ralentizador != null && ralentizador.isRunning()) {
            ralentizador.cancel();
        }

        ralentizador = ValueAnimator.ofFloat(gatoAnimado.getSpeed(), 0f);
        ralentizador.setDuration(600);
        ralentizador.addUpdateListener(anim -> gatoAnimado.setSpeed((float) anim.getAnimatedValue()));
        ralentizador.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                gatoAnimado.pauseAnimation();
            }
        });
        ralentizador.start();

        animacionBarra.pause();
        animacionTexto.pause();
    }

    private void procesarFinDeCiclo() {
        handlerCiclos.removeCallbacks(runnableCiclos);

        estaCorriendo = false;
        GestorAlertas.setCronometroCorriendo(this, false);
        actualizarEstadoIconosToolbar();
        botonPausar.setImageResource(android.R.drawable.ic_media_play);
        detenerYRestablecerAnimaciones();
        gatoAnimado.pauseAnimation();

        boolean eraDescanso = esDescanso;
        boolean eraDescansoLargo = eraDescanso && tvTitulo.getText().toString().equals("DESCANSO LARGO");
        GestorCiclos.Resultado resultado = GestorCiclos.procesarFinDeFase(this, esDescanso, cicloActual);

        if (eraDescanso && !resultado.esDescansoSiguiente) {
            GestorAlertas.marcarPeriodoGraciaPostDescanso(this);
        }

        Log.d(ETIQUETA_DEPURACION, "finDeFase: de(descanso=" + eraDescanso + ",ciclo=" + cicloActual
                + ") -> (descanso=" + resultado.esDescansoSiguiente + ",ciclo=" + resultado.cicloSiguiente
                + ",titulo=" + resultado.tituloSiguiente + ")");
        cicloActual = resultado.cicloSiguiente;
        esDescanso = resultado.esDescansoSiguiente;
        tiempoRestante = resultado.tiempoSiguiente;
        String tituloParaMostrar = resultado.tituloSiguiente;

        GestorAlertas.guardarEstadoCronometro(this, tiempoRestante, esDescanso, cicloActual, tituloParaMostrar);

        modoBotonEscudo = ModoBotonEscudo.OCULTO;
        sesionProtegida = false;
        if (botonEscudo.getVisibility() == View.VISIBLE) {
            botonEscudo.animate().cancel();
            botonEscudo.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200).withEndAction(() -> {
                botonEscudo.setVisibility(View.GONE);
                botonEscudo.setAlpha(1f);
                botonEscudo.setScaleX(1f);
                botonEscudo.setScaleY(1f);
            }).start();
        }
        tvTitulo.animate().alpha(0f).setDuration(200).start();
        textoTiempo.animate().alpha(0f).setDuration(200).start();
        barraProgreso.animate().alpha(0f).setDuration(200).withEndAction(() ->
                finalizarTransicionFase(resultado, eraDescanso, eraDescansoLargo, tituloParaMostrar)
        ).start();
    }

    private void finalizarTransicionFase(GestorCiclos.Resultado resultado, boolean eraDescanso, boolean eraDescansoLargo, String titulo) {
        tvTitulo.setText(titulo);

        if (resultado.completoLosCuatroCiclos) {
            actualizarIndicadoresSegunEstado();
            if (esDescanso) {
                barraProgreso.setIndicatorColor(ContextCompat.getColor(this, R.color.color_verde_exito));
                barraProgreso.setMax((int) tiempoRestante);
                actualizarInterfaz();
                fadeInFase();
                iniciarCronometro();
            } else {
                habilitarNavegacion(true);
                barraProgreso.setIndicatorColor(TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
                barraProgreso.setMax((int) tiempoRestante);
                actualizarInterfaz();
                fadeInFase();
                Toast.makeText(this, "¡Completaste tus 4 ciclos de enfoque! Inicia cuando quieras seguir.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (eraDescansoLargo) {
            habilitarNavegacion(true);
            actualizarIndicadoresSegunEstado();
            barraProgreso.setIndicatorColor(TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
            barraProgreso.setMax((int) tiempoRestante);
            actualizarInterfaz();
            fadeInFase();
            Toast.makeText(this, "¡Descanso largo terminado! Inicia cuando quieras tu nuevo ciclo.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!eraDescanso) {
            barraProgreso.setIndicatorColor(ContextCompat.getColor(this, R.color.color_verde_exito));
        } else {
            barraProgreso.setIndicatorColor(TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
        }

        actualizarIndicadoresSegunEstado();
        barraProgreso.setMax((int) tiempoRestante);
        actualizarInterfaz();
        fadeInFase();
        iniciarCronometro();
    }

    private void fadeInFase() {
        tvTitulo.animate().alpha(1f).setDuration(220).start();
        textoTiempo.animate().alpha(1f).setDuration(220).start();
        barraProgreso.animate().alpha(1f).setDuration(220).start();
    }

    private void actualizarIndicadorVisual(int ciclo) {
        TextView tvObjetivo = obtenerTextViewPorCiclo(ciclo);
        if (tvObjetivo != null) {
            tvObjetivo.setBackgroundTintList(ColorStateList.valueOf(TemaUtils.resolverColor(this, R.attr.themeCicloInactivo)));
            tvObjetivo.setTextColor(TemaUtils.resolverColor(this, R.attr.themeTextoSecundario));
        }
    }

    private void configurarIndicadorActual(int ciclo) {
        TextView tvObjetivo = obtenerTextViewPorCiclo(ciclo);
        if (tvObjetivo != null) {
            tvObjetivo.setBackgroundTintList(ColorStateList.valueOf(TemaUtils.resolverColor(this, R.attr.themeTextoPrimario)));
            tvObjetivo.setTextColor(TemaUtils.resolverColor(this, R.attr.themeFondoPrincipal));
        }
    }

    private void configurarIndicadorDescanso(int ciclo) {
        TextView tvObjetivo = obtenerTextViewPorCiclo(ciclo);
        if (tvObjetivo != null) {
            tvObjetivo.setBackgroundTintList(ColorStateList.valueOf(TemaUtils.resolverColor(this, R.attr.themeAcento)));
            tvObjetivo.setTextColor(TemaUtils.resolverColor(this, R.attr.themeSobreAcento));
        }
    }

    private TextView obtenerTextViewPorCiclo(int ciclo) {
        switch (ciclo) {
            case 1: return tvCiclo1;
            case 2: return tvCiclo2;
            case 3: return tvCiclo3;
            case 4: return tvCiclo4;
            default: return null;
        }
    }

    private void restaurarIndicadoresGlobales() {
        int colorOscuro = TemaUtils.resolverColor(this, R.attr.themeCicloInactivo);
        int colorBlanco = TemaUtils.resolverColor(this, R.attr.themeTextoPrimario);
        tvCiclo1.setBackgroundTintList(ColorStateList.valueOf(colorOscuro));
        tvCiclo1.setTextColor(colorBlanco);
        tvCiclo2.setBackgroundTintList(ColorStateList.valueOf(colorOscuro));
        tvCiclo2.setTextColor(colorBlanco);
        tvCiclo3.setBackgroundTintList(ColorStateList.valueOf(colorOscuro));
        tvCiclo3.setTextColor(colorBlanco);
        tvCiclo4.setBackgroundTintList(ColorStateList.valueOf(colorOscuro));
        tvCiclo4.setTextColor(colorBlanco);
    }

    private void mostrarDialogoAdvertencia() {
        DialogoAdvertencia.mostrar(this, this::reiniciarCronometro);
    }

    private void reiniciarCronometro() {
        ocultarBotonEscudoInstante();
        handlerCiclos.removeCallbacks(runnableCiclos);
        habilitarNavegacion(true);

        if (temporizador != null) {
            temporizador.cancel();
        }

        if (!esDescanso) {
            long transcurrido = TIEMPO_ENFOQUE - tiempoRestante;
            boolean menosDelUmbral = transcurrido < UMBRAL_SANCION;
            if (!menosDelUmbral && !sesionProtegida) {
                Log.d(ETIQUETA_DEPURACION, "Activity: registrarFallo desde reiniciarCronometro");
                new GestorEstadisticas(this).registrarFallo();
            }
        }
        sesionProtegida = false;

        tiempoRestante = TIEMPO_ENFOQUE;
        estaCorriendo = false;
        GestorAlertas.setCronometroCorriendo(this, false);
        GestorAlertas.consumirReanudarTrasInfraccion(this);
        actualizarEstadoIconosToolbar();
        esDescanso = false;
        cicloActual = 1;

        botonPausar.setImageResource(android.R.drawable.ic_media_play);
        gatoAnimado.pauseAnimation();
        gatoAnimado.setProgress(0);

        tvTitulo.setText("SESIÓN DE ENFOQUE");
        barraProgreso.setIndicatorColor(TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
        barraProgreso.setMax((int) TIEMPO_ENFOQUE);
        restaurarIndicadoresGlobales();
        configurarIndicadorActual(cicloActual);

        actualizarInterfaz();
        detenerYRestablecerAnimaciones();
    }

    private void actualizarInterfaz() {
        int minutos = (int) (tiempoRestante / 1000) / 60;
        int segundos = (int) (tiempoRestante / 1000) % 60;
        textoTiempo.setText(String.format("%02d:%02d", minutos, segundos));
        barraProgreso.setProgress((int) tiempoRestante);
    }

    private void detenerYRestablecerAnimaciones() {
        animacionBarra.cancel();
        animacionTexto.cancel();
        barraProgreso.setTranslationY(0f);
        textoTiempo.setTranslationY(0f);
    }

    private boolean puedeUsarEscudoHoy() {
        return getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE)
                .getLong("escudo_dia_usado", -1L) != diaActual();
    }

    private void marcarEscudoUsado() {
        getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE)
                .edit().putLong("escudo_dia_usado", diaActual()).apply();
    }

    private long diaActual() {
        long ahora = System.currentTimeMillis();
        return (ahora + java.util.TimeZone.getDefault().getOffset(ahora)) / 86400000L;
    }

    private void verificarEstadoBotonEscudo(long milisegundos) {
        long elapsed = (long) barraProgreso.getMax() - milisegundos;

        if (modoBotonEscudo == ModoBotonEscudo.OCULTO && !esDescanso
                && elapsed >= UMBRAL_SIN_PENALIZACION && puedeUsarEscudoHoy()) {
            mostrarBotonComoEscudo();
        }

        if (!esDescanso && cicloActual == 4 && milisegundos <= 30000
                && modoBotonEscudo != ModoBotonEscudo.SALTAR
                && !new GestorConfiguracion(this).isDescansoLargoHabilitado()) {
            mostrarBotonComoSaltar();
        }
    }

    private void mostrarBotonComoEscudo() {
        modoBotonEscudo = ModoBotonEscudo.ESCUDO;
        botonEscudo.setImageResource(R.drawable.ic_finalizarseguro);
        botonEscudo.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        botonEscudo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_verde_exito)));
        botonEscudo.setVisibility(View.VISIBLE);
        botonEscudo.setScaleX(0f);
        botonEscudo.setScaleY(0f);
        botonEscudo.setAlpha(0f);
        botonEscudo.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    private void mostrarBotonComoSaltar() {
        boolean eraVisible = botonEscudo.getVisibility() == View.VISIBLE;
        modoBotonEscudo = ModoBotonEscudo.SALTAR;

        if (eraVisible) {
            botonEscudo.animate().cancel();
            botonEscudo.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                botonEscudo.setImageResource(android.R.drawable.ic_media_next);
                botonEscudo.animate().alpha(1f).setDuration(150).start();
            }).start();
        } else {
            configurarVisualBotonSaltar();
            botonEscudo.setVisibility(View.VISIBLE);
            botonEscudo.setAlpha(0f);
            botonEscudo.setScaleX(0f);
            botonEscudo.setScaleY(0f);
            botonEscudo.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(450).setInterpolator(new OvershootInterpolator(2.5f)).start();
        }
    }

    private void configurarVisualBotonSaltar() {
        botonEscudo.setImageResource(android.R.drawable.ic_media_next);
        botonEscudo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_verde_exito)));
        botonEscudo.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
    }

    private void ocultarBotonEscudo() {
        modoBotonEscudo = ModoBotonEscudo.OCULTO;
        botonEscudo.animate().cancel();
        botonEscudo.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(300)
                .withEndAction(() -> {
                    botonEscudo.setVisibility(View.GONE);
                    botonEscudo.setAlpha(1f);
                    botonEscudo.setScaleX(1f);
                    botonEscudo.setScaleY(1f);
                }).start();
    }

    private void ocultarBotonEscudoInstante() {
        modoBotonEscudo = ModoBotonEscudo.OCULTO;
        botonEscudo.animate().cancel();
        botonEscudo.setVisibility(View.GONE);
        botonEscudo.setAlpha(1f);
        botonEscudo.setScaleX(1f);
        botonEscudo.setScaleY(1f);
    }
}