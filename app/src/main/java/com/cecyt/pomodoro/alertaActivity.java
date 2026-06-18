package com.cecyt.pomodoro;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.widget.Button;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

    private ObjectAnimator animacionProgresoFadeIn;
    private ObjectAnimator animacionProgresoFadeOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new GestorTemas(this).aplicarTema(this);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        GestorAlertas.setAlertaActiva(this, true);
        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.alerta_main);

        btnSilenciar = findViewById(R.id.btnSilenciar);
        viewGlowTop = findViewById(R.id.viewGlowTop);
        pbProgresoSilenciar = findViewById(R.id.pbProgresoSilenciar);

        pbProgresoSilenciar.setAlpha(0f);
        pbProgresoSilenciar.setVisibility(View.INVISIBLE);

        esInfraccion = getIntent().getBooleanExtra(GestorAlertas.EXTRA_ES_INFRACCION, false);
        android.util.Log.d("DepuracionCronometro", "alerta onCreate esInfraccion=" + esInfraccion);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        animarAura();
        reproducirAlarma();

        btnSilenciar.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isBotonPresionado = true;
                mostrarProgresoGradual();
                transicionTexto("CANCELANDO...\n5s", () -> {
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
                transicionTexto("MANTÉN 5s", null);
            }
            return true;
        });
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
            vibrador = (Vibrator) getSystemService(VIBRATOR_SERVICE);
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

                btnSilenciar.setText("CANCELANDO" + puntos + "\n" + segundosRestantes + "s");
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
        if (contador != null) {
            contador.cancel();
            contador = null;
        }
        detenerAlarma();
    }
}