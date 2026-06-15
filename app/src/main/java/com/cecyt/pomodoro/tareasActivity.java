package com.cecyt.pomodoro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class tareasActivity extends BaseActivity {

    private TextView tvTareaActiva;
    private TextView tvResumen;
    private TextView tvTotalTareas;
    private RecyclerView recyclerTareas;
    private MaterialButton btnAnadirTarea;

    private SharedPreferences preferencias;
    private static final String PREFS_NAME = GestorAlertas.PREFS_NOMBRE_COMPARTIDO;

    private ArrayList<String> listaTareas;
    private TareasAdapter adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tareas_main);

        preferencias = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvTareaActiva = findViewById(R.id.tvTareaActiva);
        tvResumen = findViewById(R.id.tvResumen);
        tvTotalTareas = findViewById(R.id.tvTotalTareas);
        recyclerTareas = findViewById(R.id.recyclerTareas);
        btnAnadirTarea = findViewById(R.id.btnAnadirTarea);

        listaTareas = new ArrayList<>();

        recyclerTareas.setLayoutManager(new LinearLayoutManager(this));

        adaptador = new TareasAdapter(listaTareas, posicion -> mostrarOpcionesTarea(posicion));
        recyclerTareas.setAdapter(adaptador);

        cargarDatos();

        btnAnadirTarea.setOnClickListener(v ->
                DialogoRedactarTarea.mostrar(this, "Nueva Tarea", "", tarea -> {
                    agregarTarea(tarea);
                    Toast.makeText(this, "Tarea agregada", Toast.LENGTH_SHORT).show();
                }));

        configurarNavegacion(R.id.nav_tareas);
    }

    private void mostrarOpcionesTarea(int posicion) {
        String tareaSeleccionada = listaTareas.get(posicion);

        DialogoOpcionesTarea.mostrar(this,
                () -> {
                    guardarTareaActiva(tareaSeleccionada);
                    Toast.makeText(this, "Tarea fijada arriba", Toast.LENGTH_SHORT).show();
                },
                () -> DialogoRedactarTarea.mostrar(this, "Editar Tarea", tareaSeleccionada, tarea -> {
                    editarTarea(posicion, tarea);
                    Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                }),
                () -> {
                    eliminarTarea(posicion);
                    Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                });
    }

    private void agregarTarea(String tarea) {
        listaTareas.add(tarea);
        adaptador.notifyItemInserted(listaTareas.size() - 1);
        guardarTareaActiva(tarea);
        actualizarResumen();
        guardarDatos();
    }

    private void editarTarea(int posicion, String nuevoTexto) {
        if (posicion >= 0 && posicion < listaTareas.size()) {
            String activaActual = preferencias.getString("tarea_activa_actual", "Ninguna");
            if (activaActual.equals(listaTareas.get(posicion))) {
                guardarTareaActiva(nuevoTexto);
            }

            listaTareas.set(posicion, nuevoTexto);
            adaptador.notifyItemChanged(posicion);
            guardarDatos();
        }
    }

    private void eliminarTarea(int posicion) {
        if (posicion >= 0 && posicion < listaTareas.size()) {
            String tareaAEliminar = listaTareas.get(posicion);
            listaTareas.remove(posicion);
            adaptador.notifyItemRemoved(posicion);
            adaptador.notifyItemRangeChanged(posicion, listaTareas.size());

            String activaActual = preferencias.getString("tarea_activa_actual", "Ninguna");
            if (listaTareas.isEmpty()) {
                guardarTareaActiva("Ninguna");
            } else if (activaActual.equals(tareaAEliminar)) {
                guardarTareaActiva(listaTareas.get(listaTareas.size() - 1));
            }

            actualizarResumen();
            guardarDatos();
        }
    }

    private void actualizarResumen() {
        int total = listaTareas.size();
        if (tvResumen != null) {
            tvResumen.setText(total + " tareas registradas");
        }
        if (tvTotalTareas != null) {
            tvTotalTareas.setText(String.valueOf(total));
        }
    }

    private void guardarTareaActiva(String tarea) {
        preferencias.edit().putString("tarea_activa_actual", tarea).apply();
        if (tvTareaActiva != null) {
            tvTareaActiva.setText(tarea);
        }
    }

    private void guardarDatos() {
        SharedPreferences.Editor editor = preferencias.edit();

        int cantidadAnterior = preferencias.getInt("cantidad_tareas", 0);
        for (int i = 0; i < cantidadAnterior; i++) {
            editor.remove("tarea_" + i);
        }

        editor.putInt("cantidad_tareas", listaTareas.size());

        for (int i = 0; i < listaTareas.size(); i++) {
            editor.putString("tarea_" + i, listaTareas.get(i));
        }

        editor.apply();
    }

    private void cargarDatos() {
        String tareaActiva = preferencias.getString("tarea_activa_actual", "Ninguna");
        if (tvTareaActiva != null) {
            tvTareaActiva.setText(tareaActiva);
        }

        listaTareas.clear();
        int cantidad = preferencias.getInt("cantidad_tareas", 0);

        for (int i = 0; i < cantidad; i++) {
            String tarea = preferencias.getString("tarea_" + i, null);
            if (tarea != null) {
                listaTareas.add(tarea);
            }
        }

        adaptador.notifyDataSetChanged();
        actualizarResumen();
    }
}