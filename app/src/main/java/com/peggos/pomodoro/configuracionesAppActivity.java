package com.peggos.pomodoro;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.function.IntConsumer;

public class configuracionesAppActivity extends BaseActivity {

    private static final long DURACION_CAMBIO_MODO = 180;

    private NumberPicker npTrabajo;
    private NumberPicker npDescansoCorto;
    private NumberPicker npDescansoLargo;
    private MaterialSwitch switchOpcionesPredeterminadas;
    private MaterialSwitch switchDescansoLargo;

    private MaterialButtonToggleGroup grupoModo;
    private View contenedorBloques;
    private View contenedorHorario;
    private MaterialButton btnInicioHorario;
    private MaterialButton btnFinHorario;
    private TextView tvResumenBloques;
    private TextView tvResumenDescansos;
    private TextView tvResumenTermino;
    private TextView tvResumenNota;
    private View botonGuardar;

    private int inicioHorario;
    private int finHorario;

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
        grupoModo = findViewById(R.id.tgModoTiempos);
        contenedorBloques = findViewById(R.id.llModoBloques);
        contenedorHorario = findViewById(R.id.llModoHorario);
        btnInicioHorario = findViewById(R.id.btnInicioHorario);
        btnFinHorario = findViewById(R.id.btnFinHorario);
        tvResumenBloques = findViewById(R.id.tvResumenBloques);
        tvResumenDescansos = findViewById(R.id.tvResumenDescansos);
        tvResumenTermino = findViewById(R.id.tvResumenTermino);
        tvResumenNota = findViewById(R.id.tvResumenNota);
        botonGuardar = findViewById(R.id.btnGuardarConfig);

        npTrabajo.setMinValue(1);
        npTrabajo.setMaxValue(60);

        npDescansoCorto.setMinValue(1);
        npDescansoCorto.setMaxValue(20);

        npDescansoLargo.setMinValue(1);
        npDescansoLargo.setMaxValue(60);

        GestorConfiguracion gestorConfiguracion = new GestorConfiguracion(this);
        npTrabajo.setValue(gestorConfiguracion.getMinutosTrabajoPorBloques());
        npDescansoCorto.setValue(gestorConfiguracion.getMinutosDescansoCortoPorBloques());
        npDescansoLargo.setValue(gestorConfiguracion.getMinutosDescansoLargo());

        switchDescansoLargo.setChecked(gestorConfiguracion.isDescansoLargoHabilitadoPorBloques());
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

        inicioHorario = gestorConfiguracion.getInicioHorario();
        finHorario = gestorConfiguracion.getFinHorario();
        btnInicioHorario.setOnClickListener(v -> mostrarSelectorHora(inicioHorario, minutos -> {
            inicioHorario = minutos;
            actualizarResumenHorario();
        }));
        btnFinHorario.setOnClickListener(v -> mostrarSelectorHora(finHorario, minutos -> {
            finHorario = minutos;
            actualizarResumenHorario();
        }));

        boolean modoHorario = gestorConfiguracion.isModoHorarioHabilitado();
        grupoModo.check(modoHorario ? R.id.btnModoHorario : R.id.btnModoBloques);
        contenedorBloques.setVisibility(modoHorario ? View.GONE : View.VISIBLE);
        contenedorHorario.setVisibility(modoHorario ? View.VISIBLE : View.GONE);
        grupoModo.addOnButtonCheckedListener((grupo, idBoton, marcado) -> {
            if (!marcado) return;
            mostrarModo(idBoton == R.id.btnModoHorario);
        });
        actualizarResumenHorario();

        botonGuardar.setOnClickListener(v -> {
            if (haySessionBloqueante()) {
                Toast.makeText(this, R.string.toast_no_cambiar_tiempos, Toast.LENGTH_SHORT).show();
                return;
            }
            boolean guardarHorario = esModoHorarioSeleccionado();
            if (guardarHorario && repartoSeleccionado() == null) return;

            gestorConfiguracion.guardarHorario(guardarHorario, inicioHorario, finHorario);
            if (!guardarHorario) {
                gestorConfiguracion.setOpcionesPredeterminadasHabilitadas(switchOpcionesPredeterminadas.isChecked());
                gestorConfiguracion.setDescansoLargoHabilitado(switchDescansoLargo.isChecked());
                gestorConfiguracion.guardar(npTrabajo.getValue(), npDescansoCorto.getValue(), npDescansoLargo.getValue());
            }
            Toast.makeText(this, R.string.toast_config_guardada, Toast.LENGTH_SHORT).show();
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

    private boolean haySesionEnCurso() {
        return GestorAlertas.isCronometroCorriendo(this)
                || GestorAlertas.getTiempoRestanteGuardado(this) >= 0;
    }

    private boolean esModoHorarioSeleccionado() {
        return grupoModo.getCheckedButtonId() == R.id.btnModoHorario;
    }

    private GestorConfiguracion.Reparto repartoSeleccionado() {
        return GestorConfiguracion.calcularReparto(
                GestorConfiguracion.duracionHorario(inicioHorario, finHorario));
    }

    private void mostrarModo(boolean horario) {
        View entrante = horario ? contenedorHorario : contenedorBloques;
        View saliente = horario ? contenedorBloques : contenedorHorario;
        saliente.animate().cancel();
        entrante.animate().cancel();
        saliente.setVisibility(View.GONE);
        saliente.setAlpha(1f);
        entrante.setAlpha(0f);
        entrante.setVisibility(View.VISIBLE);
        entrante.animate().alpha(1f).setDuration(DURACION_CAMBIO_MODO).start();
        actualizarEstadoGuardar();
    }

    private void mostrarSelectorHora(int minutosDelDia, IntConsumer alSeleccionar) {
        MaterialTimePicker selector = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(minutosDelDia / 60)
                .setMinute(minutosDelDia % 60)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTheme(R.style.ThemeOverlay_MiauFocus_TimePicker)
                .build();
        selector.addOnPositiveButtonClickListener(v ->
                alSeleccionar.accept(selector.getHour() * 60 + selector.getMinute()));
        selector.show(getSupportFragmentManager(), "selector_hora_horario");
    }

    private String formatearHora(int minutosDelDia) {
        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.HOUR_OF_DAY, (minutosDelDia / 60) % 24);
        calendario.set(Calendar.MINUTE, minutosDelDia % 60);
        return DateFormat.getTimeFormat(this).format(calendario.getTime());
    }

    private void actualizarResumenHorario() {
        btnInicioHorario.setText(formatearHora(inicioHorario));
        btnFinHorario.setText(formatearHora(finHorario));

        int duracion = GestorConfiguracion.duracionHorario(inicioHorario, finHorario);
        GestorConfiguracion.Reparto reparto = GestorConfiguracion.calcularReparto(duracion);

        boolean valido = reparto != null;
        tvResumenBloques.setVisibility(valido ? View.VISIBLE : View.GONE);
        tvResumenDescansos.setVisibility(valido ? View.VISIBLE : View.GONE);
        tvResumenTermino.setVisibility(valido ? View.VISIBLE : View.GONE);

        if (valido) {
            tvResumenBloques.setText(getString(R.string.config_horario_bloques, reparto.minutosEnfoque));
            tvResumenDescansos.setText(getString(R.string.config_horario_descansos, reparto.minutosDescanso));
            tvResumenTermino.setText(getString(R.string.config_horario_termino,
                    formatearHora(inicioHorario + reparto.duracionTotal())));
            tvResumenNota.setText(R.string.config_horario_sin_largo);
            tvResumenNota.setTextColor(TemaUtils.resolverColor(this, R.attr.themeTextoSecundario));
        } else {
            tvResumenNota.setText(duracion < GestorConfiguracion.DURACION_MINIMA_HORARIO
                    ? R.string.config_horario_muy_corto
                    : R.string.config_horario_muy_largo);
            tvResumenNota.setTextColor(ContextCompat.getColor(this, R.color.color_rojo_peligro));
        }
        actualizarEstadoGuardar();
    }

    private void actualizarEstadoGuardar() {
        boolean habilitado = !haySessionBloqueante()
                && (!esModoHorarioSeleccionado() || repartoSeleccionado() != null);
        botonGuardar.setEnabled(habilitado);
        botonGuardar.setAlpha(habilitado ? 1f : 0.5f);
    }

    private void aplicarBloqueoSesionActiva() {
        boolean bloqueada = haySessionBloqueante();
        TextView aviso = findViewById(R.id.tvSesionActivaAviso);
        if (aviso != null) aviso.setVisibility(bloqueada ? View.VISIBLE : View.GONE);

        boolean modoBloqueado = bloqueada || haySesionEnCurso();
        TextView avisoModo = findViewById(R.id.tvModoBloqueadoAviso);
        avisoModo.setVisibility(modoBloqueado && !bloqueada ? View.VISIBLE : View.GONE);

        actualizarEstadoGuardar();

        for (int i = 0; i < grupoModo.getChildCount(); i++) {
            grupoModo.getChildAt(i).setEnabled(!modoBloqueado);
        }
        btnInicioHorario.setEnabled(!modoBloqueado);
        btnFinHorario.setEnabled(!modoBloqueado);

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
