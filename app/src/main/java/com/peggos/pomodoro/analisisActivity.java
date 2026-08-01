package com.peggos.pomodoro;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class analisisActivity extends BaseActivity {

    private GestorEstadisticas gestor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.analisis_main);

            gestor = new GestorEstadisticas(this);
            cargarDatosReales();
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
            Toast.makeText(this, "Error al cargar la pantalla: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirigidoAAlerta || gestor == null) return;
        int diasAMostrar = 15;
        cargarDatosReales();
        cargarActividadesRecientes();
        dibujarGraficaDeBarras(gestor.getDatosUltimosDias(diasAMostrar), gestor.getEtiquetasUltimosDias(diasAMostrar));
    }

    private void cargarDatosReales() {
        int minutosTotales = gestor.getMinutosTotales();
        int completados = gestor.getPomodorosCompletados();
        int fallos = gestor.getPomodorosFallados();
        int racha = gestor.getRachaActual();
        float puntos = gestor.getPuntos();

        int horas = minutosTotales / 60;
        int minutosRestantes = minutosTotales % 60;
        String textoTiempo = "Tiempo de Enfoque Total: " + horas + " hrs " + minutosRestantes + " min";
        ((TextView) findViewById(R.id.tvTiempoTotal)).setText(textoTiempo);

        ((TextView) findViewById(R.id.tvPomodorosCompletados)).setText("Pomodoros Completados: " + completados);
        ((TextView) findViewById(R.id.tvPuntos)).setText(GestorEstadisticas.formatearPuntos(puntos) + " Puntos");

        TextView tvRacha = findViewById(R.id.tvRachaActual);
        tvRacha.setText("Racha Actual: " + racha + " Días");

        float porcentajeFallo = 0;
        int intentosTotales = completados + fallos;
        if (intentosTotales > 0) {
            porcentajeFallo = ((float) fallos / intentosTotales) * 100;
        }

        TextView tvFallos = findViewById(R.id.tvTasaFallos);
        String stringPorcentaje = String.format("%.0f%%", porcentajeFallo);
        tvFallos.setText("Tasa de Fallos: " + stringPorcentaje + " · " + fallos + " bloques abandonados");
    }

    private void cargarActividadesRecientes() {
        ArrayList<String> actividades = gestor.getActividadesRecientes(5);
        TextView tvActividades = findViewById(R.id.tvActividadesRecientes);

        if (actividades.isEmpty()) {
            tvActividades.setText("Aún no hay actividades completadas");
            return;
        }

        StringBuilder textoActividades = new StringBuilder();
        for (String actividad : actividades) {
            String[] partes = actividad.split("\\|");
            String nombre = partes[0];
            String sufijo = partes.length > 1 ? " · " + partes[1] + " min" : "";
            textoActividades.append("• ").append(nombre).append(sufijo).append("\n");
        }
        tvActividades.setText(textoActividades.toString().trim());
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

            int MAX_VALOR_EJE_Y = 8;
            for (int dato : datosFinal) {
                if (dato > MAX_VALOR_EJE_Y) MAX_VALOR_EJE_Y = dato + 2;
            }

            for (int i = 0; i < datosFinal.length && i < etiquetasFinal.length; i++) {
                LinearLayout columna = new LinearLayout(this);
                columna.setOrientation(LinearLayout.VERTICAL);
                columna.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                columna.setLayoutParams(new LinearLayout.LayoutParams(anchoColumna, LinearLayout.LayoutParams.MATCH_PARENT));

                LinearLayout areaBarra = new LinearLayout(this);
                areaBarra.setOrientation(LinearLayout.VERTICAL);
                areaBarra.setWeightSum(MAX_VALOR_EJE_Y);
                LinearLayout.LayoutParams paramsAreaBarra = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
                paramsAreaBarra.bottomMargin = 8;
                areaBarra.setLayoutParams(paramsAreaBarra);

                float vacio = MAX_VALOR_EJE_Y - datosFinal[i];
                View espaciador = new View(this);
                espaciador.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, vacio));

                View barra = new View(this);
                barra.setBackgroundResource(R.drawable.fondo_barra);
                LinearLayout.LayoutParams paramsBarra = new LinearLayout.LayoutParams(
                        (int) (32 * getResources().getDisplayMetrics().density), 0, datosFinal[i]);
                paramsBarra.gravity = Gravity.CENTER_HORIZONTAL;
                barra.setLayoutParams(paramsBarra);

                areaBarra.addView(espaciador);
                areaBarra.addView(barra);

                TextView tvDia = new TextView(this);
                tvDia.setText(etiquetasFinal[i]);
                tvDia.setTextColor(TemaUtils.resolverColor(this, R.attr.themeTextoSecundario));
                tvDia.setTextSize(12f);
                tvDia.setGravity(Gravity.CENTER);

                columna.addView(areaBarra);
                columna.addView(tvDia);
                llContenedorBarras.addView(columna);
            }

            hsvGraficaBarras.fullScroll(View.FOCUS_RIGHT);
        });
    }
}