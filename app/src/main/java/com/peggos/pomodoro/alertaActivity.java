package com.peggos.pomodoro;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class alertaActivity extends AppCompatActivity {

    private CountDownTimer contador;
    private Button btnSilenciar;
    private View viewGlowTop;
    private CircularProgressIndicator pbProgresoSilenciar;
    private Ringtone ringtone;
    private Vibrator vibrador;
    private boolean isBotonPresionado = false;
    private boolean esInfraccion = false;

    private AnimatorSet animacionLagrima;
    private Runnable cicloLagrima;

    private ObjectAnimator animacionProgresoFadeIn;
    private ObjectAnimator animacionProgresoFadeOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new GestorTemas(this).aplicarTema(this);
        super.onCreate(savedInstanceState);

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        GestorAlertas.setAlertaActiva(this, true);
        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.alerta_main);

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(new GestorTemas(this).getTemaActual().esClaro());

        btnSilenciar = findViewById(R.id.btnSilenciar);
        viewGlowTop = findViewById(R.id.viewGlowTop);
        pbProgresoSilenciar = findViewById(R.id.pbProgresoSilenciar);

        pbProgresoSilenciar.setAlpha(0f);
        pbProgresoSilenciar.setVisibility(View.INVISIBLE);

        esInfraccion = getIntent().getBooleanExtra(GestorAlertas.EXTRA_ES_INFRACCION, true);
        android.util.Log.d("DepuracionCronometro", "alerta onCreate esInfraccion=" + esInfraccion);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        mostrarDatosInfraccion();
        animarLagrima();
        animarCirculosDecorativos();
        animarAura();
        reproducirAlarma();

        btnSilenciar.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isBotonPresionado = true;
                mostrarProgresoGradual();
                transicionTexto(getString(R.string.alerta_cancelando_inicial), () -> {
                    if (isBotonPresionado) {
                        iniciarContador();
                    }
                });
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isBotonPresionado = false;
                if (contador != null) {
                    contador.cancel();
                    contador = null;
                }
                ocultarProgresoGradual();
                transicionTexto(getString(R.string.alerta_manten_5s), null);
            }
            return true;
        });
    }

    private void mostrarDatosInfraccion() {
        TextView tvInfoPuntos = findViewById(R.id.tvInfoPuntos);
        TextView tvInfoDetalle = findViewById(R.id.tvInfoDetalle);

        float castigo = new GestorEstadisticas(this).getUltimoCastigo();
        if (castigo <= 0f) {
            tvInfoPuntos.setText(R.string.alerta_sin_puntos);
        } else {
            tvInfoPuntos.setText(getResources().getQuantityString(R.plurals.alerta_puntos_perdidos,
                    (int) castigo, GestorEstadisticas.formatearPuntos(castigo)));
        }

        int ciclo = GestorAlertas.getCicloActualActual(this);
        long tiempoCongelado = GestorAlertas.getTiempoCongelado(this);

        if (tiempoCongelado >= 0) {
            tvInfoDetalle.setText(getString(R.string.alerta_ciclo_de_4_tiempo, ciclo,
                    IslaDinamica.formatearRestante(tiempoCongelado)));
        } else {
            tvInfoDetalle.setText(getString(R.string.alerta_ciclo_de_4, ciclo));
        }
    }

    private void animarLagrima() {
        View lagrima = findViewById(R.id.ivGatoLagrima);
        if (lagrima == null) return;

        float recorrido = 46f * getResources().getDisplayMetrics().density;
        lagrima.setAlpha(0f);

        cicloLagrima = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;

                lagrima.setTranslationY(0f);
                lagrima.setAlpha(0f);

                ObjectAnimator brotar = ObjectAnimator.ofFloat(lagrima, "alpha", 0f, 1f);
                brotar.setDuration(650);

                ObjectAnimator caer = ObjectAnimator.ofFloat(lagrima, "translationY", 0f, recorrido);
                caer.setDuration(780);
                caer.setInterpolator(new AccelerateInterpolator(1.7f));

                ObjectAnimator desvanecer = ObjectAnimator.ofFloat(lagrima, "alpha", 1f, 0f);
                desvanecer.setStartDelay(300);
                desvanecer.setDuration(400);

                AnimatorSet caida = new AnimatorSet();
                caida.playTogether(caer, desvanecer);

                animacionLagrima = new AnimatorSet();
                animacionLagrima.playSequentially(brotar, caida);
                animacionLagrima.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lagrima.postDelayed(cicloLagrima, 550);
                    }
                });
                animacionLagrima.start();
            }
        };

        lagrima.postDelayed(cicloLagrima, 350);
    }

    private void animarCirculosDecorativos() {
        iniciarPulso(findViewById(R.id.circleTopLeft), 4500, 0);
        iniciarPulso(findViewById(R.id.circleBottomRight), 5500, 1500);
    }

    private void iniciarPulso(View vista, long duracion, long retraso) {
        if (vista == null) return;

        ObjectAnimator escalaX = ObjectAnimator.ofFloat(vista, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator escalaY = ObjectAnimator.ofFloat(vista, "scaleY", 1f, 1.2f, 1f);
        ObjectAnimator alfa = ObjectAnimator.ofFloat(vista, "alpha", 0.13f, 0.28f, 0.13f);

        escalaX.setDuration(duracion);
        escalaY.setDuration(duracion);
        alfa.setDuration(duracion);

        escalaX.setRepeatCount(ObjectAnimator.INFINITE);
        escalaY.setRepeatCount(ObjectAnimator.INFINITE);
        alfa.setRepeatCount(ObjectAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolador = new AccelerateDecelerateInterpolator();
        escalaX.setInterpolator(interpolador);
        escalaY.setInterpolator(interpolador);
        alfa.setInterpolator(interpolador);

        AnimatorSet conjunto = new AnimatorSet();
        conjunto.playTogether(escalaX, escalaY, alfa);
        conjunto.setStartDelay(retraso);
        vista.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        conjunto.start();
    }

    private void reproducirAlarma() {
        GestorConfiguracion gestorConfiguracion = new GestorConfiguracion(this);
        boolean modoSilencioso = gestorConfiguracion.estaEnHorarioSilencioso();

        if (!modoSilencioso) {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (alert == null) {
                    alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.setLooping(true);
                ringtone.play();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager gestorVibrador = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrador = gestorVibrador.getDefaultVibrator();
        } else {
            vibrador = getSystemService(Vibrator.class);
        }
        if (!modoSilencioso && gestorConfiguracion.isVibracionHabilitada()
                && vibrador != null && vibrador.hasVibrator()) {
            long[] patron = {0, 800, 400, 800};
            vibrador.vibrate(VibrationEffect.createWaveform(patron, 0));
        }
    }

    private void detenerAlarma() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrador != null) {
            vibrador.cancel();
        }
    }

    private void transicionTexto(String nuevoTexto, Runnable accionAlTerminarFondo) {
        ObjectAnimator fadeOut = ObjectAnimator.ofArgb(btnSilenciar, "textColor", ContextCompat.getColor(this, R.color.white), ContextCompat.getColor(this, R.color.color_transparente));
        fadeOut.setDuration(120);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                btnSilenciar.setText(nuevoTexto);
                if (accionAlTerminarFondo != null) {
                    accionAlTerminarFondo.run();
                }
                ObjectAnimator fadeIn = ObjectAnimator.ofArgb(btnSilenciar, "textColor", ContextCompat.getColor(alertaActivity.this, R.color.color_transparente), ContextCompat.getColor(alertaActivity.this, R.color.white));
                fadeIn.setDuration(120);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private void mostrarProgresoGradual() {
        if (animacionProgresoFadeOut != null && animacionProgresoFadeOut.isRunning()) {
            animacionProgresoFadeOut.cancel();
        }

        pbProgresoSilenciar.setVisibility(View.VISIBLE);
        animacionProgresoFadeIn = ObjectAnimator.ofFloat(pbProgresoSilenciar, "alpha", pbProgresoSilenciar.getAlpha(), 1f);
        animacionProgresoFadeIn.setDuration(240);
        animacionProgresoFadeIn.start();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btnSilenciar, "scaleX", 1.0f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btnSilenciar, "scaleY", 1.0f, 0.95f);
        scaleDownX.setDuration(240);
        scaleDownY.setDuration(240);
        scaleDownX.start();
        scaleDownY.start();
    }

    private void ocultarProgresoGradual() {
        if (animacionProgresoFadeIn != null && animacionProgresoFadeIn.isRunning()) {
            animacionProgresoFadeIn.cancel();
        }

        animacionProgresoFadeOut = ObjectAnimator.ofFloat(pbProgresoSilenciar, "alpha", pbProgresoSilenciar.getAlpha(), 0f);
        animacionProgresoFadeOut.setDuration(240);
        animacionProgresoFadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pbProgresoSilenciar.setVisibility(View.INVISIBLE);
                pbProgresoSilenciar.setProgress(0);
            }
        });
        animacionProgresoFadeOut.start();

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btnSilenciar, "scaleX", 0.95f, 1.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btnSilenciar, "scaleY", 0.95f, 1.0f);
        scaleUpX.setDuration(240);
        scaleUpY.setDuration(240);
        scaleUpX.start();
        scaleUpY.start();
    }

    private void animarAura() {
        ObjectAnimator animacionEscalaX = ObjectAnimator.ofFloat(viewGlowTop, "scaleX", 1.0f, 1.15f);
        ObjectAnimator animacionEscalaY = ObjectAnimator.ofFloat(viewGlowTop, "scaleY", 1.0f, 1.15f);
        ObjectAnimator animacionAlfa = ObjectAnimator.ofFloat(viewGlowTop, "alpha", 0.6f, 0.9f);

        animacionEscalaX.setDuration(1000);
        animacionEscalaX.setRepeatCount(ObjectAnimator.INFINITE);
        animacionEscalaX.setRepeatMode(ObjectAnimator.REVERSE);

        animacionEscalaY.setDuration(1000);
        animacionEscalaY.setRepeatCount(ObjectAnimator.INFINITE);
        animacionEscalaY.setRepeatMode(ObjectAnimator.REVERSE);

        animacionAlfa.setDuration(1000);
        animacionAlfa.setRepeatCount(ObjectAnimator.INFINITE);
        animacionAlfa.setRepeatMode(ObjectAnimator.REVERSE);

        animacionEscalaX.start();
        animacionEscalaY.start();
        animacionAlfa.start();
    }

    private void iniciarContador() {
        if (contador != null) {
            contador.cancel();
        }

        contador = new CountDownTimer(5000, 50) {
            int ciclosPuntos = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                long tiempoTranscurrido = 5000 - millisUntilFinished;
                pbProgresoSilenciar.setProgress((int) tiempoTranscurrido);

                long segundosRestantes = (millisUntilFinished / 1000) + 1;

                String puntos = "";
                if (ciclosPuntos % 3 == 0) {
                    puntos = ".";
                } else if (ciclosPuntos % 3 == 1) {
                    puntos = "..";
                } else {
                    puntos = "...";
                }

                if (tiempoTranscurrido % 500 < 50) {
                    ciclosPuntos++;
                }

                btnSilenciar.setText(getString(R.string.alerta_cancelando_cuenta, puntos, segundosRestantes));
            }

            @Override
            public void onFinish() {
                pbProgresoSilenciar.setProgress(5000);
                detenerAlarma();
                GestorAlertas.setAlertaActiva(alertaActivity.this, false);
                android.util.Log.d("DepuracionCronometro", "alerta cancelada esInfraccion=" + esInfraccion);
                if (esInfraccion) {
                    GestorAlertas.limpiarInfraccionPendiente(alertaActivity.this);
                    Intent intentCronometro = new Intent(alertaActivity.this, CronometroActivity.class);
                    intentCronometro.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intentCronometro.putExtra(CronometroActivity.EXTRA_OMITIR_SPLASH, true);
                    startActivity(intentCronometro);
                }
                finish();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GestorAlertas.setAlertaActiva(this, false);
        View lagrima = findViewById(R.id.ivGatoLagrima);
        if (lagrima != null && cicloLagrima != null) {
            lagrima.removeCallbacks(cicloLagrima);
        }
        if (animacionLagrima != null) {
            animacionLagrima.cancel();
            animacionLagrima = null;
        }
        if (contador != null) {
            contador.cancel();
            contador = null;
        }
        detenerAlarma();
    }
}