package com.peggos.pomodoro;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class analisisActivity extends BaseActivity {

    private GestorEstadisticas gestor;
    private ObjectAnimator animadorLlama;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.analisis_main);

            gestor = new GestorEstadisticas(this);
            cargarDatosReales();
            dibujarFranjaSemanal();
            cargarActividadesRecientes();

            int diasAMostrar = 15;
            int[] datosUltimosDias = gestor.getDatosUltimosDias(diasAMostrar);
            String[] etiquetasUltimosDias = gestor.getEtiquetasUltimosDias(diasAMostrar);
            dibujarGraficaDeBarras(datosUltimosDias, etiquetasUltimosDias);

            findViewById(R.id.btnTienda).setOnClickListener(v -> {
                Intent intent = new Intent(analisisActivity.this, personalizacionActivity.class);
                startActivity(intent);
            });

            configurarNavegacion(R.id.nav_estadisticas);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.analisis_error_cargar, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirigidoAAlerta || gestor == null) return;
        int diasAMostrar = 15;
        cargarDatosReales();
        dibujarFranjaSemanal();
        cargarActividadesRecientes();
        dibujarGraficaDeBarras(gestor.getDatosUltimosDias(diasAMostrar), gestor.getEtiquetasUltimosDias(diasAMostrar));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (animadorLlama != null) {
            animadorLlama.cancel();
            animadorLlama = null;
        }
    }

    private void cargarDatosReales() {
        int minutosTotales = gestor.getMinutosTotales();
        int completados = gestor.getPomodorosCompletados();
        int fallos = gestor.getPomodorosFallados();
        int racha = gestor.getRachaActual();
        float puntos = gestor.getPuntos();

        int horas = minutosTotales / 60;
        int minutosRestantes = minutosTotales % 60;
        String textoTiempo = horas + "h " + minutosRestantes + "m";
        ((TextView) findViewById(R.id.tvTiempoTotal)).setText(textoTiempo);

        ((TextView) findViewById(R.id.tvPomodorosCompletados)).setText(String.valueOf(completados));
        ((TextView) findViewById(R.id.tvPuntos)).setText(getString(R.string.analisis_puntos, GestorEstadisticas.formatearPuntos(puntos)));

        TextView tvRacha = findViewById(R.id.tvRachaActual);
        tvRacha.setText(String.valueOf(racha));

        float porcentajeFallo = 0;
        int intentosTotales = completados + fallos;
        if (intentosTotales > 0) {
            porcentajeFallo = ((float) fallos / intentosTotales) * 100;
        }

        TextView tvFallos = findViewById(R.id.tvTasaFallos);
        String stringPorcentaje = String.format("%.0f%%", porcentajeFallo);
        tvFallos.setText(stringPorcentaje);
        ((TextView) findViewById(R.id.tvBloquesAbandonados)).setText(getResources().getQuantityString(R.plurals.analisis_abandonados, fallos, fallos));
    }

    

    private void dibujarFranjaSemanal() {
        LinearLayout franja = findViewById(R.id.llFranjaSemanal);
        franja.removeAllViews();

        int diasDesdeDomingo = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        int[] datosSemana = gestor.getDatosUltimosDias(diasDesdeDomingo + 1);

        float densidad = getResources().getDisplayMetrics().density;
        int colorAcento = TemaUtils.resolverColor(this, R.attr.themeAcento);
        int colorInicial = TemaUtils.resolverColor(this, R.attr.themeTextoTerciario);
        int colorInicialHoy = TemaUtils.resolverColor(this, R.attr.themeTextoPrimario);
        int ladoCirculo = (int) (26 * densidad);
        int ladoLlama = (int) (18 * densidad);

        String[] inicialesSemana = getResources().getStringArray(R.array.iniciales_semana);
        for (int i = 0; i < inicialesSemana.length; i++) {
            LinearLayout columna = new LinearLayout(this);
            columna.setOrientation(LinearLayout.VERTICAL);
            columna.setGravity(Gravity.CENTER_HORIZONTAL);
            columna.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvInicial = new TextView(this);
            tvInicial.setText(inicialesSemana[i]);
            tvInicial.setTextSize(11f);
            tvInicial.setGravity(Gravity.CENTER);
            tvInicial.setLayoutParams(new LinearLayout.LayoutParams(ladoCirculo, ladoCirculo));

            boolean esHoy = i == diasDesdeDomingo;
            if (esHoy) {
                tvInicial.setBackgroundResource(R.drawable.fondo_circulo_hoy);
                tvInicial.setTextColor(colorInicialHoy);
            } else {
                tvInicial.setTextColor(colorInicial);
            }

            ImageView ivLlama = new ImageView(this);
            ivLlama.setImageResource(R.drawable.ic_llama);
            ivLlama.setColorFilter(colorAcento);
            boolean encendida = i < datosSemana.length && datosSemana[i] > 0;
            ivLlama.setAlpha(encendida ? 1f : 0.2f);
            LinearLayout.LayoutParams paramsLlama = new LinearLayout.LayoutParams(ladoLlama, ladoLlama);
            paramsLlama.topMargin = (int) (7 * densidad);
            ivLlama.setLayoutParams(paramsLlama);

            columna.addView(tvInicial);
            columna.addView(ivLlama);
            franja.addView(columna);
        }

        boolean enfocoHoy = diasDesdeDomingo < datosSemana.length && datosSemana[diasDesdeDomingo] > 0;
        int racha = gestor.getRachaActual();

        TextView tvMensaje = findViewById(R.id.tvMensajeRacha);
        if (racha == 0) {
            tvMensaje.setText(R.string.racha_empieza);
        } else if (enfocoHoy) {
            tvMensaje.setText(R.string.racha_ya_hoy);
        } else {
            tvMensaje.setText(R.string.racha_manten);
        }

        ImageView ivLlamaRacha = findViewById(R.id.ivLlamaRacha);
        ivLlamaRacha.setAlpha(racha > 0 ? 1f : 0.25f);
        animarRespiracionLlama(ivLlamaRacha);
    }

    private void animarRespiracionLlama(ImageView llama) {
        if (animadorLlama != null) animadorLlama.cancel();

        PropertyValuesHolder escalaX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f);
        PropertyValuesHolder escalaY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f);
        animadorLlama = ObjectAnimator.ofPropertyValuesHolder(llama, escalaX, escalaY);
        animadorLlama.setDuration(1400);
        animadorLlama.setRepeatCount(ValueAnimator.INFINITE);
        animadorLlama.setRepeatMode(ValueAnimator.REVERSE);
        animadorLlama.start();
    }

    private static final int ACTIVIDADES_VISIBLES = 5;
    private boolean actividadesExpandidas = false;

    private void cargarActividadesRecientes() {
        int limite = actividadesExpandidas ? Integer.MAX_VALUE : ACTIVIDADES_VISIBLES;
        ArrayList<String> actividades = gestor.getActividadesRecientes(limite);
        boolean hayMas = !actividadesExpandidas
                && gestor.getActividadesRecientes(ACTIVIDADES_VISIBLES + 1).size() > ACTIVIDADES_VISIBLES;

        LinearLayout filas = findViewById(R.id.llFilasActividades);
        View estadoVacio = findViewById(R.id.estadoVacioActividades);
        TextView tvVerTodo = findViewById(R.id.tvVerTodoActividades);

        filas.removeAllViews();

        if (actividades.isEmpty()) {
            estadoVacio.setVisibility(View.VISIBLE);
            tvVerTodo.setVisibility(View.GONE);
            return;
        }

        estadoVacio.setVisibility(View.GONE);

        float densidad = getResources().getDisplayMetrics().density;
        int colorSeparador = TemaUtils.resolverColor(this, R.attr.themeAcentoOverlay20);

        for (int i = 0; i < actividades.size(); i++) {
            String[] partes = actividades.get(i).split("\\|");
            String nombre = partes[0];
            String duracion = partes.length > 1 ? partes[1] + " min" : "";

            if (i > 0) {
                View separador = new View(this);
                separador.setBackgroundColor(colorSeparador);
                filas.addView(separador, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) densidad)));
            }

            filas.addView(crearFilaActividad(nombre, duracion, densidad));
        }

        tvVerTodo.setVisibility(hayMas ? View.VISIBLE : View.GONE);
        tvVerTodo.setOnClickListener(v -> {
            actividadesExpandidas = true;
            cargarActividadesRecientes();
        });
    }

    private View crearFilaActividad(String nombre, String duracion, float densidad) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(0, (int) (12 * densidad), 0, (int) (12 * densidad));

        ImageView icono = new ImageView(this);
        icono.setBackgroundResource(R.drawable.fondo_circulo_actividad);
        icono.setImageResource(R.drawable.ic_llama);
        icono.setColorFilter(TemaUtils.resolverColor(this, R.attr.themeAcentoTexto));
        int ladoIcono = (int) (32 * densidad);
        int reborde = (int) (8 * densidad);
        icono.setPadding(reborde, reborde, reborde, reborde);
        LinearLayout.LayoutParams paramsIcono = new LinearLayout.LayoutParams(ladoIcono, ladoIcono);
        paramsIcono.rightMargin = (int) (12 * densidad);
        icono.setLayoutParams(paramsIcono);

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombre);
        tvNombre.setTextSize(14f);
        tvNombre.setMaxLines(1);
        tvNombre.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvNombre.setTextColor(TemaUtils.resolverColor(this, R.attr.themeTextoPrimario));

        TextView tvDuracion = new TextView(this);
        tvDuracion.setText(duracion);
        tvDuracion.setTextSize(11f);
        tvDuracion.setTextColor(TemaUtils.resolverColor(this, R.attr.themeTextoTerciario));

        textos.addView(tvNombre);
        textos.addView(tvDuracion);

        fila.addView(icono);
        fila.addView(textos);
        return fila;
    }

    private static final int COLUMNAS_VISIBLES = 7;

    private void dibujarGraficaDeBarras(int[] datos, String[] etiquetas) {
        if (datos == null || datos.length == 0) {
            datos = new int[]{0};
            etiquetas = new String[]{""};
        }

        int[] datosFinal = datos;
        String[] etiquetasFinal = etiquetas;

        android.widget.HorizontalScrollView hsvGraficaBarras = findViewById(R.id.hsvGraficaBarras);
        LinearLayout llContenedorBarras = findViewById(R.id.llContenedorBarras);

        hsvGraficaBarras.post(() -> {
            int anchoDisponible = hsvGraficaBarras.getWidth()
                    - hsvGraficaBarras.getPaddingLeft() - hsvGraficaBarras.getPaddingRight();
            int anchoColumna = anchoDisponible / COLUMNAS_VISIBLES;

            llContenedorBarras.removeAllViews();

            float densidad = getResources().getDisplayMetrics().density;
            int colorAcento = TemaUtils.resolverColor(this, R.attr.themeAcento);
            int altoLineaBase = Math.max(1, (int) (3 * densidad));
            int anchoBarra = Math.max(1, (int) (anchoColumna * 0.74f));

            int valorMaximo = 0;
            for (int dato : datosFinal) {
                if (dato > valorMaximo) valorMaximo = dato;
            }
            float MAX_VALOR_EJE_Y = valorMaximo > 0 ? valorMaximo / 0.88f : 1f;

            for (int i = 0; i < datosFinal.length && i < etiquetasFinal.length; i++) {
                LinearLayout columna = new LinearLayout(this);
                columna.setOrientation(LinearLayout.VERTICAL);
                columna.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                columna.setLayoutParams(new LinearLayout.LayoutParams(anchoColumna, LinearLayout.LayoutParams.MATCH_PARENT));

                LinearLayout areaBarra = new LinearLayout(this);
                areaBarra.setOrientation(LinearLayout.VERTICAL);
                areaBarra.setWeightSum(MAX_VALOR_EJE_Y);
                LinearLayout.LayoutParams paramsAreaBarra = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
                areaBarra.setLayoutParams(paramsAreaBarra);

                float vacio = MAX_VALOR_EJE_Y - datosFinal[i];
                View espaciador = new View(this);
                espaciador.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, vacio));

                View barra = new View(this);
                barra.setBackgroundResource(R.drawable.fondo_barra);
                LinearLayout.LayoutParams paramsBarra = new LinearLayout.LayoutParams(anchoBarra, 0, datosFinal[i]);
                paramsBarra.gravity = Gravity.CENTER_HORIZONTAL;
                barra.setLayoutParams(paramsBarra);

                areaBarra.addView(espaciador);
                areaBarra.addView(barra);

                View lineaBase = new View(this);
                lineaBase.setBackgroundColor(colorAcento);
                lineaBase.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, altoLineaBase));

                TextView tvDia = new TextView(this);
                tvDia.setText(etiquetasFinal[i]);
                tvDia.setTextColor(TemaUtils.resolverColor(this, R.attr.themeTextoSecundario));
                tvDia.setTextSize(11f);
                tvDia.setGravity(Gravity.CENTER);
                tvDia.setPadding(0, (int) (8 * densidad), 0, 0);

                columna.addView(areaBarra);
                columna.addView(lineaBase);
                columna.addView(tvDia);
                llContenedorBarras.addView(columna);
            }

            hsvGraficaBarras.fullScroll(View.FOCUS_RIGHT);
        });
    }
}