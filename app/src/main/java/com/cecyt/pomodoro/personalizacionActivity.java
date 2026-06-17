package com.cecyt.pomodoro;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

public class personalizacionActivity extends BaseActivity {

    private TextView tvPuntosDisponibles;
    private TextView tvDeduccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.personalizacion_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvPuntosDisponibles = findViewById(R.id.tvPuntosDisponibles);
        tvDeduccion = findViewById(R.id.tvDeduccion);
        actualizarPuntos();

        RecyclerView rvTemas = findViewById(R.id.rvTemas);
        rvTemas.setAdapter(new TemasAdapter(this, new TemasAdapter.OnTemaSeleccionadoListener() {
            @Override
            public void onTemaCambiado() {
                recreate();
                overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
            }

            @Override
            public void onTemaComprado(int precio) {
                mostrarAnimacionCompra(precio);
            }
        }));

        findViewById(R.id.ivAtras).setOnClickListener(v -> finish());
    }

    private void mostrarAnimacionCompra(int precio) {
        tvDeduccion.setText("-" + precio + " pts");
        tvDeduccion.setAlpha(1f);
        tvDeduccion.setTranslationY(0f);
        tvDeduccion.setVisibility(View.VISIBLE);

        tvDeduccion.animate()
                .translationY(-48f)
                .alpha(0f)
                .setDuration(900)
                .setStartDelay(150)
                .withEndAction(() -> {
                    tvDeduccion.setVisibility(View.GONE);
                    recreate();
                    overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
                }).start();

        tvPuntosDisponibles.setTextColor(Color.parseColor("#FF4444"));
        tvPuntosDisponibles.animate()
                .scaleX(1.18f).scaleY(1.18f).setDuration(120)
                .withEndAction(() ->
                        tvPuntosDisponibles.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                ).start();
    }

    private void actualizarPuntos() {
        float puntos = new GestorEstadisticas(this).getPuntos();
        tvPuntosDisponibles.setText(GestorEstadisticas.formatearPuntos(puntos) + " Puntos");
    }
}
