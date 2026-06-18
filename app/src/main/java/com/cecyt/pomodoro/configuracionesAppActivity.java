package com.cecyt.pomodoro;

import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class configuracionesAppActivity extends BaseActivity {

    private NumberPicker npTrabajo;
    private NumberPicker npDescansoCorto;
    private NumberPicker npDescansoLargo;
    private MaterialSwitch switchOpcionesPredeterminadas;
    private MaterialSwitch switchDescansoLargo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuraciones_app);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        npTrabajo = findViewById(R.id.npTrabajo);
        npDescansoCorto = findViewById(R.id.npDescansoCorto);
        npDescansoLargo = findViewById(R.id.npDescansoLargo);
        switchOpcionesPredeterminadas = findViewById(R.id.switchOpcionesPredeterminadas);
        switchDescansoLargo = findViewById(R.id.switchDescansoLargo);

        npTrabajo.setMinValue(1);
        npTrabajo.setMaxValue(60);

        npDescansoCorto.setMinValue(1);
        npDescansoCorto.setMaxValue(20);

        npDescansoLargo.setMinValue(1);
        npDescansoLargo.setMaxValue(60);

        GestorConfiguracion gestorConfiguracion = new GestorConfiguracion(this);
        npTrabajo.setValue(gestorConfiguracion.getMinutosTrabajo());
        npDescansoCorto.setValue(gestorConfiguracion.getMinutosDescansoCorto());
        npDescansoLargo.setValue(gestorConfiguracion.getMinutosDescansoLargo());

        switchDescansoLargo.setChecked(gestorConfiguracion.isDescansoLargoHabilitado());
        switchDescansoLargo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!haySessionBloqueante()) actualizarEstadoDescansoLargo();
        });

        switchOpcionesPredeterminadas.setChecked(gestorConfiguracion.isOpcionesPredeterminadasHabilitadas());
        aplicarEstadoOpcionesPredeterminadas(switchOpcionesPredeterminadas.isChecked());
        switchOpcionesPredeterminadas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (haySessionBloqueante()) return;
            if (isChecked) {
                npTrabajo.setValue(GestorConfiguracion.MINUTOS_TRABAJO_PREDETERMINADO);
                npDescansoCorto.setValue(GestorConfiguracion.MINUTOS_DESCANSO_CORTO_PREDETERMINADO);
                npDescansoLargo.setValue(GestorConfiguracion.MINUTOS_DESCANSO_LARGO_PREDETERMINADO);
            }
            aplicarEstadoOpcionesPredeterminadas(isChecked);
        });

        findViewById(R.id.btnGuardarConfig).setOnClickListener(v -> {
            if (haySessionBloqueante()) {
                Toast.makeText(this, "No puedes cambiar los tiempos mientras hay una sesión activa", Toast.LENGTH_SHORT).show();
                return;
            }
            gestorConfiguracion.setOpcionesPredeterminadasHabilitadas(switchOpcionesPredeterminadas.isChecked());
            gestorConfiguracion.setDescansoLargoHabilitado(switchDescansoLargo.isChecked());
            gestorConfiguracion.guardar(npTrabajo.getValue(), npDescansoCorto.getValue(), npDescansoLargo.getValue());
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.ivAtras).setOnClickListener(v -> finish());
        aplicarBloqueoSesionActiva();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirigidoAAlerta) return;
        aplicarBloqueoSesionActiva();
    }

    private boolean haySessionBloqueante() {
        return GestorAlertas.haySesionBloqueante(this);
    }

    private void aplicarBloqueoSesionActiva() {
        boolean bloqueada = haySessionBloqueante();
        TextView aviso = findViewById(R.id.tvSesionActivaAviso);
        if (aviso != null) aviso.setVisibility(bloqueada ? View.VISIBLE : View.GONE);

        View botonGuardar = findViewById(R.id.btnGuardarConfig);
        botonGuardar.setEnabled(!bloqueada);
        botonGuardar.setAlpha(bloqueada ? 0.5f : 1f);

        if (bloqueada) {
            npTrabajo.setEnabled(false);
            npDescansoCorto.setEnabled(false);
            npDescansoLargo.setEnabled(false);
            switchOpcionesPredeterminadas.setEnabled(false);
            switchDescansoLargo.setEnabled(false);
        } else {
            switchOpcionesPredeterminadas.setEnabled(true);
            switchDescansoLargo.setEnabled(true);
            aplicarEstadoOpcionesPredeterminadas(switchOpcionesPredeterminadas.isChecked());
        }
    }

    private void aplicarEstadoOpcionesPredeterminadas(boolean habilitadas) {
        npTrabajo.setEnabled(!habilitadas);
        npDescansoCorto.setEnabled(!habilitadas);
        actualizarEstadoDescansoLargo();
    }

    private void actualizarEstadoDescansoLargo() {
        boolean predeterminadasActivas = switchOpcionesPredeterminadas.isChecked();
        npDescansoLargo.setEnabled(switchDescansoLargo.isChecked() && !predeterminadasActivas);
    }
}
