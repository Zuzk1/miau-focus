package com.peggos.pomodoro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;

public class tareasActivity extends BaseActivity {

    private TextView tvTareaActiva;
    private TextView tvSubtituloActiva;
    private TextView tvPendientes;
    private TextView tvCompletadasHoy;
    private TextView tvTotalTareas;
    private RecyclerView recyclerTareas;
    private View estadoVacioTareas;
    private MaterialButton btnAnadirTarea;
    private MaterialButton btnIrAEnfoque;

    private SharedPreferences preferencias;
    private static final String PREFS_NAME = GestorAlertas.PREFS_NOMBRE_COMPARTIDO;

    private ArrayList<Tarea> listaTareas;
    private TareasAdapter adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tareas_main);

        preferencias = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvTareaActiva = findViewById(R.id.tvTareaActiva);
        tvSubtituloActiva = findViewById(R.id.tvSubtituloActiva);
        tvPendientes = findViewById(R.id.tvPendientes);
        tvCompletadasHoy = findViewById(R.id.tvCompletadasHoy);
        tvTotalTareas = findViewById(R.id.tvTotalTareas);
        recyclerTareas = findViewById(R.id.recyclerTareas);
        estadoVacioTareas = findViewById(R.id.estadoVacioTareas);
        btnAnadirTarea = findViewById(R.id.btnAnadirTarea);
        btnIrAEnfoque = findViewById(R.id.btnIrAEnfoque);

        listaTareas = new ArrayList<>();

        recyclerTareas.setLayoutManager(new LinearLayoutManager(this));

        adaptador = new TareasAdapter(listaTareas,
                posicion -> mostrarOpcionesTarea(posicion),
                posicion -> alternarCompletada(posicion));
        recyclerTareas.setAdapter(adaptador);

        cargarDatos();

        btnAnadirTarea.setOnClickListener(v ->
                DialogoRedactarTarea.mostrar(this, getString(R.string.dialogo_tarea_nueva), "", tarea -> {
                    agregarTarea(tarea);
                    Toast.makeText(this, R.string.toast_tarea_agregada, Toast.LENGTH_SHORT).show();
                }));

        btnIrAEnfoque.setOnClickListener(v -> {
            Intent intentEnfoque = new Intent(this, CronometroActivity.class);
            intentEnfoque.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptionsCompat opciones = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.fade_in, R.anim.fade_out);
            startActivity(intentEnfoque, opciones.toBundle());
        });

        configurarNavegacion(R.id.nav_tareas);
    }

    private void mostrarOpcionesTarea(int posicion) {
        if (posicion < 0 || posicion >= listaTareas.size()) return;
        String tareaSeleccionada = listaTareas.get(posicion).nombre;

        DialogoOpcionesTarea.mostrar(this,
                () -> {
                    guardarTareaActiva(tareaSeleccionada);
                    Toast.makeText(this, R.string.toast_tarea_fijada, Toast.LENGTH_SHORT).show();
                },
                () -> DialogoRedactarTarea.mostrar(this, getString(R.string.dialogo_tarea_editar), tareaSeleccionada, tarea -> {
                    editarTarea(posicion, tarea);
                    Toast.makeText(this, R.string.toast_tarea_actualizada, Toast.LENGTH_SHORT).show();
                }),
                () -> {
                    eliminarTarea(posicion);
                    Toast.makeText(this, R.string.toast_tarea_eliminada, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirigidoAAlerta) return;
        cargarDatos();
    }

    private void alternarCompletada(int posicion) {
        if (posicion < 0 || posicion >= listaTareas.size()) return;

        Tarea tarea = listaTareas.get(posicion);
        tarea.completada = !tarea.completada;
        tarea.diaCompletada = tarea.completada ? GestorEstadisticas.obtenerDiaActual() : -1;

        if (tarea.completada) {
            String activaActual = preferencias.getString("tarea_activa_actual", "Ninguna");
            if (activaActual.equals(tarea.nombre)) {
                guardarTareaActiva(primeraPendiente());
            }
        }

        ordenarTareas();
        adaptador.notifyDataSetChanged();
        actualizarResumen();
        guardarDatos();
    }

    private String primeraPendiente() {
        for (Tarea tarea : listaTareas) {
            if (!tarea.completada) return tarea.nombre;
        }
        return "Ninguna";
    }

    private void ordenarTareas() {
        Collections.sort(listaTareas, (a, b) -> Boolean.compare(a.completada, b.completada));
    }

    private void agregarTarea(String tarea) {
        listaTareas.add(new Tarea(tarea, false, -1));
        ordenarTareas();
        adaptador.notifyDataSetChanged();
        guardarTareaActiva(tarea);
        actualizarResumen();
        guardarDatos();
    }

    private void editarTarea(int posicion, String nuevoTexto) {
        if (posicion >= 0 && posicion < listaTareas.size()) {
            String activaActual = preferencias.getString("tarea_activa_actual", "Ninguna");
            if (activaActual.equals(listaTareas.get(posicion).nombre)) {
                guardarTareaActiva(nuevoTexto);
            }

            listaTareas.get(posicion).nombre = nuevoTexto;
            adaptador.notifyItemChanged(posicion);
            guardarDatos();
        }
    }

    private void eliminarTarea(int posicion) {
        if (posicion >= 0 && posicion < listaTareas.size()) {
            String tareaAEliminar = listaTareas.get(posicion).nombre;
            listaTareas.remove(posicion);
            adaptador.notifyItemRemoved(posicion);
            adaptador.notifyItemRangeChanged(posicion, listaTareas.size());

            String activaActual = preferencias.getString("tarea_activa_actual", "Ninguna");
            if (listaTareas.isEmpty()) {
                guardarTareaActiva("Ninguna");
            } else if (activaActual.equals(tareaAEliminar)) {
                guardarTareaActiva(listaTareas.get(listaTareas.size() - 1).nombre);
            }

            actualizarResumen();
            guardarDatos();
        }
    }

    private void actualizarResumen() {
        int total = listaTareas.size();
        long hoy = GestorEstadisticas.obtenerDiaActual();
        int pendientes = 0;
        int completadasHoy = 0;

        for (Tarea tarea : listaTareas) {
            if (!tarea.completada) {
                pendientes++;
            } else if (tarea.diaCompletada == hoy) {
                completadasHoy++;
            }
        }

        if (tvPendientes != null) tvPendientes.setText(String.valueOf(pendientes));
        if (tvCompletadasHoy != null) tvCompletadasHoy.setText(String.valueOf(completadasHoy));
        if (tvTotalTareas != null) tvTotalTareas.setText(String.valueOf(total));

        boolean vacia = listaTareas.isEmpty();
        if (estadoVacioTareas != null) estadoVacioTareas.setVisibility(vacia ? View.VISIBLE : View.GONE);
        if (recyclerTareas != null) recyclerTareas.setVisibility(vacia ? View.GONE : View.VISIBLE);
    }

    private void guardarTareaActiva(String tarea) {
        preferencias.edit().putString("tarea_activa_actual", tarea).apply();
        mostrarTareaActiva(tarea);
    }

    private void mostrarTareaActiva(String tarea) {
        boolean hayActiva = tarea != null && !tarea.isEmpty() && !tarea.equals("Ninguna");

        if (tvTareaActiva != null) {
            tvTareaActiva.setText(hayActiva ? tarea : getString(R.string.tareas_sin_tarea_activa));
        }
        if (tvSubtituloActiva != null) {
            tvSubtituloActiva.setText(hayActiva
                    ? R.string.tareas_toca_ir_enfoque
                    : R.string.tareas_selecciona);
        }
        if (btnIrAEnfoque != null) {
            btnIrAEnfoque.setVisibility(hayActiva ? View.VISIBLE : View.GONE);
        }
    }

    private void guardarDatos() {
        SharedPreferences.Editor editor = preferencias.edit();

        int cantidadAnterior = preferencias.getInt("cantidad_tareas", 0);
        for (int i = 0; i < cantidadAnterior; i++) {
            editor.remove("tarea_" + i);
            editor.remove("completada_" + i);
            editor.remove("completada_fecha_" + i);
        }

        editor.putInt("cantidad_tareas", listaTareas.size());

        for (int i = 0; i < listaTareas.size(); i++) {
            Tarea tarea = listaTareas.get(i);
            editor.putString("tarea_" + i, tarea.nombre);
            editor.putBoolean("completada_" + i, tarea.completada);
            editor.putLong("completada_fecha_" + i, tarea.diaCompletada);
        }

        editor.apply();
    }

    private void cargarDatos() {
        mostrarTareaActiva(preferencias.getString("tarea_activa_actual", "Ninguna"));

        listaTareas.clear();
        int cantidad = preferencias.getInt("cantidad_tareas", 0);

        for (int i = 0; i < cantidad; i++) {
            String nombre = preferencias.getString("tarea_" + i, null);
            if (nombre != null) {
                boolean completada = preferencias.getBoolean("completada_" + i, false);
                long diaCompletada = preferencias.getLong("completada_fecha_" + i, -1);
                listaTareas.add(new Tarea(nombre, completada, diaCompletada));
            }
        }

        ordenarTareas();
        adaptador.notifyDataSetChanged();
        actualizarResumen();
    }
}
