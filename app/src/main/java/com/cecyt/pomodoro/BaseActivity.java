package com.cecyt.pomodoro;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;
    private ObjectAnimator animacionMenuActual;
    private View vistaIconoMenuActual;
    private int idMenuActual = 0;
    private boolean animacionMenuPausada = false;
    private GestorTemas.Tema temaAlCrear;
    protected boolean redirigidoAAlerta = false;
    private ImageView ivEscudoToolbar;
    private ImageView ivConfigToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorTemas gestorTemas = new GestorTemas(this);
        temaAlCrear = gestorTemas.getTemaActual();
        gestorTemas.aplicarTema(this);
        super.onCreate(savedInstanceState);
    }

    protected void configurarNavegacion(int idItemSeleccionado) {
        this.idMenuActual = idItemSeleccionado;
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) return;

        bottomNavigationView.setBackgroundColor(TemaUtils.resolverColor(this, R.attr.themeFondoBarraNavegacion));

        aplicarInsetsBarrasSistema();

        bottomNavigationView.getMenu().findItem(idItemSeleccionado).setChecked(true);

        bottomNavigationView.post(() -> {
            ViewGroup menuView = (ViewGroup) bottomNavigationView.getChildAt(0);
            int indice = obtenerIndicePorId(idItemSeleccionado);
            if (indice != -1) animarIconoMenu(menuView, indice);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == idItemSeleccionado) {
                return true;
            }

            Intent intent = null;

            if (itemId == R.id.nav_enfoque) {
                intent = new Intent(this, CronometroActivity.class);
            } else if (itemId == R.id.nav_tareas) {
                intent = new Intent(this, tareasActivity.class);
            } else if (itemId == R.id.nav_estadisticas) {
                intent = new Intent(this, analisisActivity.class);
            }

            if (intent != null) {
                final Intent intentDestino = intent;
                detenerAnimacionIconoMenuSuave();

                bottomNavigationView.postDelayed(() -> {
                    intentDestino.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intentDestino);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }, 180);
            }

            return false;
        });

        animarIconosToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();

        redirigidoAAlerta = redirigirAAlertaSiCorresponde();
        if (redirigidoAAlerta) {
            return;
        }

        if (temaAlCrear != new GestorTemas(this).getTemaActual()) {
            recreate();
            return;
        }

        if (bottomNavigationView != null && idMenuActual != 0) {
            bottomNavigationView.getMenu().findItem(idMenuActual).setChecked(true);
            bottomNavigationView.post(() -> {
                ViewGroup menuView = (ViewGroup) bottomNavigationView.getChildAt(0);
                int indice = obtenerIndicePorId(idMenuActual);
                if (indice != -1) animarIconoMenu(menuView, indice);
            });
        }

        actualizarEstadoIconosToolbar();
    }

    protected boolean redirigirAAlertaSiCorresponde() {
        if (GestorAlertas.isAlertaActiva(this)) {
            Intent intentAlerta = new Intent(this, alertaActivity.class);
            intentAlerta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intentAlerta);
            return true;
        }

        if (GestorAlertas.hayInfraccionPendiente(this)) {
            Intent intentAlerta = new Intent(this, alertaActivity.class);
            intentAlerta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intentAlerta.putExtra(GestorAlertas.EXTRA_ES_INFRACCION, true);
            startActivity(intentAlerta);
            return true;
        }

        return false;
    }

    private void aplicarInsetsBarrasSistema() {
        View contenido = findViewById(android.R.id.content);
        View appBar = findViewById(R.id.appBar);

        ViewCompat.setOnApplyWindowInsetsListener(contenido, (vista, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (appBar != null) {
                appBar.setPadding(appBar.getPaddingLeft(), barras.top, appBar.getPaddingRight(), appBar.getPaddingBottom());
            }

            bottomNavigationView.setPadding(bottomNavigationView.getPaddingLeft(), bottomNavigationView.getPaddingTop(),
                    bottomNavigationView.getPaddingRight(), barras.bottom);

            return insets;
        });
    }

    private void animarIconosToolbar() {
        ivEscudoToolbar = findViewById(R.id.ivEscudo);
        ivConfigToolbar = findViewById(R.id.ivConfigTiempo);
        if (ivEscudoToolbar != null) {
            iniciarPulsoToolbar(ivEscudoToolbar, 3800, 0);
            ivEscudoToolbar.setOnClickListener(v -> startActivity(new Intent(this, ajustesGeneralesActivity.class)));
        }
        if (ivConfigToolbar != null) {
            iniciarPulsoToolbar(ivConfigToolbar, 3800, 700);
            ivConfigToolbar.setOnClickListener(v -> startActivity(new Intent(this, configuracionesAppActivity.class)));
        }
        actualizarEstadoIconosToolbar();
    }

    private void iniciarPulsoToolbar(View vista, long duracion, long delay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(vista, "scaleX", 1f, 1.04f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(vista, "scaleY", 1f, 1.04f, 1f);
        scaleX.setDuration(duracion);
        scaleY.setDuration(duracion);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        AccelerateDecelerateInterpolator interp = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interp);
        scaleY.setInterpolator(interp);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setStartDelay(delay);
        set.start();
    }

    protected void actualizarEstadoIconosToolbar() {
        boolean cronometroCorriendo = GestorAlertas.isCronometroCorriendo(this);
        aplicarEstadoIconoToolbar(ivEscudoToolbar, cronometroCorriendo, ContextCompat.getColor(this, R.color.color_verde_exito));
        aplicarEstadoIconoToolbar(ivConfigToolbar, cronometroCorriendo, TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));
    }

    private void aplicarEstadoIconoToolbar(ImageView icono, boolean deshabilitar, int colorActivo) {
        if (icono == null) return;
        icono.setEnabled(!deshabilitar);

        int colorDestino = deshabilitar ? ContextCompat.getColor(this, R.color.color_gris_icono_apagado) : colorActivo;
        int colorActual = icono.getImageTintList() != null
                ? icono.getImageTintList().getDefaultColor()
                : colorDestino;

        if (colorActual != colorDestino) {
            ValueAnimator transicionColor = ValueAnimator.ofArgb(colorActual, colorDestino);
            transicionColor.setDuration(400);
            transicionColor.addUpdateListener(animacion ->
                    icono.setImageTintList(ColorStateList.valueOf((int) animacion.getAnimatedValue())));
            transicionColor.start();
        }

        icono.animate().alpha(deshabilitar ? 0.45f : 1f).setDuration(400).start();
    }

    private int obtenerIndicePorId(int id) {
        if (id == R.id.nav_enfoque) return 0;
        if (id == R.id.nav_tareas) return 1;
        if (id == R.id.nav_estadisticas) return 2;
        return -1;
    }

    private void animarIconoMenu(ViewGroup menuView, int indiceSeleccionado) {
        detenerAnimacionIconoMenuSuave();

        View vistaIcono = menuView.getChildAt(indiceSeleccionado);
        if (vistaIcono != null) {
            if (animacionMenuPausada) {
                vistaIconoMenuActual = vistaIcono;
            } else {
                iniciarAnimacionIconoMenu(vistaIcono);
            }
        }
    }

    private void iniciarAnimacionIconoMenu(View vistaIcono) {
        vistaIconoMenuActual = vistaIcono;
        animacionMenuActual = ObjectAnimator.ofFloat(vistaIcono, "translationY", 0f, -15f, 0f);
        animacionMenuActual.setDuration(2500);
        animacionMenuActual.setRepeatCount(ObjectAnimator.INFINITE);
        animacionMenuActual.start();
    }

    private void detenerAnimacionIconoMenuSuave() {
        if (animacionMenuActual != null) {
            animacionMenuActual.cancel();
            animacionMenuActual = null;
        }

        if (vistaIconoMenuActual != null) {
            ObjectAnimator detener = ObjectAnimator.ofFloat(vistaIconoMenuActual, "translationY", vistaIconoMenuActual.getTranslationY(), 0f);
            detener.setDuration(300);
            detener.setInterpolator(new AccelerateDecelerateInterpolator());
            detener.start();
        }
    }

    protected void pausarAnimacionMenuSuave() {
        animacionMenuPausada = true;
        detenerAnimacionIconoMenuSuave();
    }

    protected void reanudarAnimacionMenuSuave() {
        animacionMenuPausada = false;
        if (vistaIconoMenuActual != null) {
            iniciarAnimacionIconoMenu(vistaIconoMenuActual);
        }
    }
}