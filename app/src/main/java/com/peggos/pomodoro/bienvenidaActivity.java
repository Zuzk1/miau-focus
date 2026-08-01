package com.peggos.pomodoro;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;

public class bienvenidaActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private MaterialButton btnComenzar;
    private SharedPreferences preferencias;
    private View[] dots;
    private final int TOTAL_PAGINAS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new GestorTemas(this).aplicarTema(this);
        super.onCreate(savedInstanceState);

        preferencias = getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, MODE_PRIVATE);
        boolean primerInicio = preferencias.getBoolean("primerInicio", true);

        if (!primerInicio) {
            iniciarCronometro();
            return;
        }

        setContentView(R.layout.activity_bienvenida);

        viewPager = findViewById(R.id.viewPager);
        dotsContainer = findViewById(R.id.dotsContainer);
        btnComenzar = findViewById(R.id.btnComenzar);

        viewPager.setAdapter(new OnboardingAdapterActivity());

        configurarDots();
        actualizarDots(0);
        animarCirculos();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarDots(position);
                if (position == TOTAL_PAGINAS - 1) {
                    btnComenzar.setText("COMENZAR");
                } else {
                    btnComenzar.setText("SIGUIENTE");
                }
            }
        });

        btnComenzar.setOnClickListener(v -> {
            int paginaActual = viewPager.getCurrentItem();
            if (paginaActual < TOTAL_PAGINAS - 1) {
                viewPager.setCurrentItem(paginaActual + 1);
            } else {
                preferencias.edit().putBoolean("primerInicio", false).apply();
                iniciarCronometro();
            }
        });
    }

    private void animarCirculos() {
        View circleTopRight = findViewById(R.id.circleTopRight);
        View circleBottomLeft = findViewById(R.id.circleBottomLeft);

        AnimatorSet pulsoTop = crearPulso(circleTopRight, 4000, 0);
        AnimatorSet pulsoBottom = crearPulso(circleBottomLeft, 5000, 1200);

        pulsoTop.start();
        pulsoBottom.start();
    }

    private AnimatorSet crearPulso(View vista, long duracion, long delay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(vista, "scaleX", 1f, 1.18f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(vista, "scaleY", 1f, 1.18f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(vista, "alpha", 0.20f, 0.38f, 0.20f);

        scaleX.setDuration(duracion);
        scaleY.setDuration(duracion);
        alpha.setDuration(duracion);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interpolator);
        scaleY.setInterpolator(interpolator);
        alpha.setInterpolator(interpolator);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setStartDelay(delay);
        return set;
    }

    private void configurarDots() {
        dots = new View[TOTAL_PAGINAS];
        int margen = (int) (6 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < TOTAL_PAGINAS; i++) {
            dots[i] = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (6 * getResources().getDisplayMetrics().density),
                    (int) (6 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(margen, 0, margen, 0);
            dots[i].setLayoutParams(params);
            dots[i].setBackgroundResource(R.drawable.puntos_inactivos_chiquitos);
            dotsContainer.addView(dots[i]);
        }
    }

    private void actualizarDots(int posicionActiva) {
        float density = getResources().getDisplayMetrics().density;
        int margen = (int) (6 * density);

        for (int i = 0; i < TOTAL_PAGINAS; i++) {
            if (i == posicionActiva) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) (20 * density), (int) (6 * density)
                );
                params.setMargins(margen, 0, margen, 0);
                dots[i].setLayoutParams(params);
                dots[i].setBackgroundResource(R.drawable.punto_largo);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) (6 * density), (int) (6 * density)
                );
                params.setMargins(margen, 0, margen, 0);
                dots[i].setLayoutParams(params);
                dots[i].setBackgroundResource(R.drawable.puntos_inactivos_chiquitos);
            }
        }
    }

    private void iniciarCronometro() {
        startActivity(new Intent(this, CronometroActivity.class));
        finish();
    }
}