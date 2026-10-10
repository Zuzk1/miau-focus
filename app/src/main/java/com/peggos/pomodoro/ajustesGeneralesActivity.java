package com.peggos.pomodoro;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;
import java.util.function.IntConsumer;

public class ajustesGeneralesActivity extends BaseActivity {

    private MaterialSwitch switchAlertaDistraccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes_generales);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.ivAtras).setOnClickListener(v -> finish());

        findViewById(R.id.btnAjustesNotificaciones).setOnClickListener(v -> {
            Intent intentAjustes = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intentAjustes.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                intentAjustes = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intentAjustes.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intentAjustes);
        });

        switchAlertaDistraccion = findViewById(R.id.switchAlertaDistraccion);
        GestorConfiguracion gestorConfiguracion = new GestorConfiguracion(this);

        switchAlertaDistraccion.setChecked(gestorConfiguracion.isAlertasDistraccionHabilitadas());
        switchAlertaDistraccion.setOnCheckedChangeListener((buttonView, isChecked) ->
                gestorConfiguracion.setAlertasDistraccionHabilitadas(isChecked));
        aplicarBloqueoAlertaDistraccion();

        MaterialSwitch switchVibracion = findViewById(R.id.switchVibracion);
        switchVibracion.setChecked(gestorConfiguracion.isVibracionHabilitada());
        switchVibracion.setOnCheckedChangeListener((buttonView, isChecked) ->
                gestorConfiguracion.setVibracionHabilitada(isChecked));

        MaterialSwitch switchModoSilencioso = findViewById(R.id.switchModoSilencioso);
        switchModoSilencioso.setChecked(gestorConfiguracion.isModoSilenciosoHabilitado());
        switchModoSilencioso.setOnCheckedChangeListener((buttonView, isChecked) ->
                gestorConfiguracion.setModoSilenciosoHabilitado(isChecked));

        MaterialButton btnHoraInicioSilencio = findViewById(R.id.btnHoraInicioSilencio);
        MaterialButton btnHoraFinSilencio = findViewById(R.id.btnHoraFinSilencio);
        actualizarTextoHora(btnHoraInicioSilencio, gestorConfiguracion.getHoraInicioSilencio());
        actualizarTextoHora(btnHoraFinSilencio, gestorConfiguracion.getHoraFinSilencio());

        btnHoraInicioSilencio.setOnClickListener(v -> mostrarSelectorHora(
                gestorConfiguracion.getHoraInicioSilencio(), hora -> {
                    gestorConfiguracion.setHoraInicioSilencio(hora);
                    actualizarTextoHora(btnHoraInicioSilencio, hora);
                }));

        btnHoraFinSilencio.setOnClickListener(v -> mostrarSelectorHora(
                gestorConfiguracion.getHoraFinSilencio(), hora -> {
                    gestorConfiguracion.setHoraFinSilencio(hora);
                    actualizarTextoHora(btnHoraFinSilencio, hora);
                }));

        findViewById(R.id.btnReiniciarEstadisticas).setOnClickListener(v -> confirmarReinicioEstadisticas());

        MaterialButton btnIdioma = findViewById(R.id.btnIdioma);
        btnIdioma.setText(nombreIdiomaActual());
        View.OnClickListener abrirSelectorIdioma = v -> mostrarSelectorIdioma();
        btnIdioma.setOnClickListener(abrirSelectorIdioma);
        findViewById(R.id.llIdioma).setOnClickListener(abrirSelectorIdioma);

        View.OnClickListener abrirAcercaDe = v -> startActivity(new Intent(this, acercaDeActivity.class));
        findViewById(R.id.btnAcercaDe).setOnClickListener(abrirAcercaDe);
        findViewById(R.id.llAcercaDe).setOnClickListener(abrirAcercaDe);
    }

    private void actualizarTextoHora(MaterialButton boton, int hora) {
        boton.setText(String.format(Locale.getDefault(), "%02d:00", hora));
    }

    private void mostrarSelectorHora(int horaActual, IntConsumer alSeleccionar) {
        MaterialTimePicker selector = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(horaActual)
                .setMinute(0)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTheme(R.style.ThemeOverlay_MiauFocus_TimePicker)
                .build();
        selector.addOnPositiveButtonClickListener(v -> alSeleccionar.accept(selector.getHour()));
        selector.show(getSupportFragmentManager(), "selector_hora_silencio");
    }

    private int indiceIdiomaActual() {
        LocaleListCompat seleccionados = AppCompatDelegate.getApplicationLocales();
        if (seleccionados.isEmpty()) return 0;

        String etiqueta = seleccionados.get(0).toLanguageTag();
        String[] codigos = getResources().getStringArray(R.array.idiomas_codigos);

        for (int i = 0; i < codigos.length; i++) {
            if (codigos[i].equalsIgnoreCase(etiqueta)) return i + 1;
        }

        String idioma = seleccionados.get(0).getLanguage();
        for (int i = 0; i < codigos.length; i++) {
            if (Locale.forLanguageTag(codigos[i]).getLanguage().equals(idioma)) return i + 1;
        }

        return 0;
    }

    private String nombreIdiomaActual() {
        int indice = indiceIdiomaActual();
        if (indice == 0) return getString(R.string.idioma_sistema);
        return getResources().getStringArray(R.array.idiomas_nombres)[indice - 1];
    }

    private void mostrarSelectorIdioma() {
        DialogoIdioma.mostrar(this, indiceIdiomaActual(), this::aplicarIdioma);
    }

    private void aplicarIdioma(int indice) {
        if (indice == indiceIdiomaActual()) return;

        LocaleListCompat locales = indice == 0
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(getResources().getStringArray(R.array.idiomas_codigos)[indice - 1]);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private void confirmarReinicioEstadisticas() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialogo_reiniciar_titulo)
                .setMessage(R.string.dialogo_reiniciar_texto)
                .setPositiveButton(R.string.ajustes_reiniciar_boton, (dialog, which) -> {
                    new GestorEstadisticas(this).reiniciarEstadisticas();
                    Toast.makeText(this, R.string.toast_estadisticas_reiniciadas, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirigidoAAlerta) {
            return;
        }
        actualizarEstadoNotificaciones();
        aplicarBloqueoAlertaDistraccion();
    }

    private void aplicarBloqueoAlertaDistraccion() {
        boolean bloqueada = GestorAlertas.haySesionBloqueante(this);
        switchAlertaDistraccion.setEnabled(!bloqueada);
        switchAlertaDistraccion.setAlpha(bloqueada ? 0.6f : 1f);

        android.widget.TextView aviso = findViewById(R.id.tvSesionActivaAviso);
        if (aviso != null) aviso.setVisibility(bloqueada ? View.VISIBLE : View.GONE);
    }

    private void actualizarEstadoNotificaciones() {
        boolean notificacionesActivas = NotificationManagerCompat.from(this).areNotificationsEnabled();
        android.widget.TextView tvEstado = findViewById(R.id.tvEstadoNotificaciones);
        tvEstado.setText(notificacionesActivas
                ? R.string.notif_estado_activadas
                : R.string.notif_estado_desactivadas);
    }
}
