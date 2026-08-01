package com.peggos.pomodoro;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class GestorTemas {
    private static final String PREF_NAME = "PomodoroTemas";
    private static final String CLAVE_TEMA_ACTUAL = "tema_actual";
    private static final String CLAVE_TEMAS_DESBLOQUEADOS = "temas_desbloqueados";

    private final SharedPreferences prefs;

    public enum Tema {
        AZUL_OSCURO("azul_oscuro", "Azul Oscuro", R.style.Theme_Pomodoro_Dark_Azul,
                R.color.tema_oscuro_fondo_principal, R.color.acento_azul, 0, true),
        AZUL_CLARO("azul_claro", "Azul Claro", R.style.Theme_Pomodoro_Light_Azul,
                R.color.tema_claro_fondo_principal, R.color.acento_azul, 0, true),

        VERDE_OSCURO("verde_oscuro", "Verde Oscuro", R.style.Theme_Pomodoro_Dark_Verde,
                R.color.tema_oscuro_fondo_principal, R.color.acento_verde, 100, false),
        VERDE_CLARO("verde_claro", "Verde Claro", R.style.Theme_Pomodoro_Light_Verde,
                R.color.tema_claro_fondo_principal, R.color.acento_verde, 100, false),

        NARANJA_OSCURO("naranja_oscuro", "Naranja Oscuro", R.style.Theme_Pomodoro_Dark_Naranja,
                R.color.tema_oscuro_fondo_principal, R.color.acento_naranja, 120, false),
        NARANJA_CLARO("naranja_claro", "Naranja Claro", R.style.Theme_Pomodoro_Light_Naranja,
                R.color.tema_claro_fondo_principal, R.color.acento_naranja, 120, false),

        ROJO_OSCURO("rojo_oscuro", "Rojo Oscuro", R.style.Theme_Pomodoro_Dark_Rojo,
                R.color.tema_oscuro_fondo_principal, R.color.acento_rojo, 140, false),
        ROJO_CLARO("rojo_claro", "Rojo Claro", R.style.Theme_Pomodoro_Light_Rojo,
                R.color.tema_claro_fondo_principal, R.color.acento_rojo, 140, false),

        MORADO_OSCURO("morado_oscuro", "Morado Oscuro", R.style.Theme_Pomodoro_Dark_Morado,
                R.color.tema_oscuro_fondo_principal, R.color.acento_morado, 160, false),
        MORADO_CLARO("morado_claro", "Morado Claro", R.style.Theme_Pomodoro_Light_Morado,
                R.color.tema_claro_fondo_principal, R.color.acento_morado, 160, false),

        ROSA_OSCURO("rosa_oscuro", "Rosa Oscuro", R.style.Theme_Pomodoro_Dark_Rosa,
                R.color.tema_oscuro_fondo_principal, R.color.acento_rosa, 180, false),
        ROSA_CLARO("rosa_claro", "Rosa Claro", R.style.Theme_Pomodoro_Light_Rosa,
                R.color.tema_claro_fondo_principal, R.color.acento_rosa, 180, false),

        MARINO_OSCURO("marino_oscuro", "Azul Marino Oscuro", R.style.Theme_Pomodoro_Dark_Marino,
                R.color.tema_oscuro_fondo_principal, R.color.acento_marino, 200, false),
        MARINO_CLARO("marino_claro", "Azul Marino Claro", R.style.Theme_Pomodoro_Light_Marino,
                R.color.tema_claro_fondo_principal, R.color.acento_marino, 200, false),

        MOSTAZA_OSCURO("mostaza_oscuro", "Mostaza Oscuro", R.style.Theme_Pomodoro_Dark_Mostaza,
                R.color.tema_oscuro_fondo_principal, R.color.acento_mostaza, 220, false),
        MOSTAZA_CLARO("mostaza_claro", "Mostaza Claro", R.style.Theme_Pomodoro_Light_Mostaza,
                R.color.tema_claro_fondo_principal, R.color.acento_mostaza, 220, false),

        TURQUESA_OSCURO("turquesa_oscuro", "Turquesa Oscuro", R.style.Theme_Pomodoro_Dark_Turquesa,
                R.color.tema_oscuro_fondo_principal, R.color.acento_turquesa, 240, false),
        TURQUESA_CLARO("turquesa_claro", "Turquesa Claro", R.style.Theme_Pomodoro_Light_Turquesa,
                R.color.tema_claro_fondo_principal, R.color.acento_turquesa, 240, false),

        GRAFITO_OSCURO("grafito_oscuro", "Grafito Oscuro", R.style.Theme_Pomodoro_Dark_Grafito,
                R.color.tema_oscuro_fondo_principal, R.color.acento_grafito, 260, false),
        GRAFITO_CLARO("grafito_claro", "Grafito Claro", R.style.Theme_Pomodoro_Light_Grafito,
                R.color.tema_claro_fondo_principal, R.color.acento_grafito, 260, false),

        SALVIA_OSCURO("salvia_oscuro", "Salvia Oscuro", R.style.Theme_Pomodoro_Dark_Salvia,
                R.color.tema_oscuro_fondo_principal, R.color.acento_salvia, 270, false),
        SALVIA_CLARO("salvia_claro", "Salvia Claro", R.style.Theme_Pomodoro_Light_Salvia,
                R.color.tema_claro_fondo_principal, R.color.acento_salvia, 270, false),

        CORAL_OSCURO("coral_oscuro", "Coral Oscuro", R.style.Theme_Pomodoro_Dark_Coral,
                R.color.tema_oscuro_fondo_principal, R.color.acento_coral, 280, false),
        CORAL_CLARO("coral_claro", "Coral Claro", R.style.Theme_Pomodoro_Light_Coral,
                R.color.tema_claro_fondo_principal, R.color.acento_coral, 280, false),

        LAVANDA_OSCURO("lavanda_oscuro", "Lavanda Oscuro", R.style.Theme_Pomodoro_Dark_Lavanda,
                R.color.tema_oscuro_fondo_principal, R.color.acento_lavanda, 300, false),
        LAVANDA_CLARO("lavanda_claro", "Lavanda Claro", R.style.Theme_Pomodoro_Light_Lavanda,
                R.color.tema_claro_fondo_principal, R.color.acento_lavanda, 300, false),

        PETROLEO_OSCURO("petroleo_oscuro", "Petróleo Oscuro", R.style.Theme_Pomodoro_Dark_Petroleo,
                R.color.tema_oscuro_fondo_principal, R.color.acento_petroleo, 310, false),
        PETROLEO_CLARO("petroleo_claro", "Petróleo Claro", R.style.Theme_Pomodoro_Light_Petroleo,
                R.color.tema_claro_fondo_principal, R.color.acento_petroleo, 310, false),

        CARMESI_OSCURO("carmesi_oscuro", "Carmesí Oscuro", R.style.Theme_Pomodoro_Dark_Carmesi,
                R.color.tema_oscuro_fondo_principal, R.color.acento_carmesi, 340, false),
        CARMESI_CLARO("carmesi_claro", "Carmesí Claro", R.style.Theme_Pomodoro_Light_Carmesi,
                R.color.tema_claro_fondo_principal, R.color.acento_carmesi, 340, false),

        ARENA_OSCURO("arena_oscuro", "Arena Oscuro", R.style.Theme_Pomodoro_Dark_Arena,
                R.color.tema_oscuro_fondo_principal, R.color.acento_arena, 350, false),
        ARENA_CLARO("arena_claro", "Arena Claro", R.style.Theme_Pomodoro_Light_Arena,
                R.color.tema_claro_fondo_principal, R.color.acento_arena, 350, false),

        FANTASMA_OSCURO("fantasma_oscuro", "Fantasma Oscuro", R.style.Theme_Pomodoro_Dark_Fantasma,
                R.color.tema_oscuro_fondo_principal, R.color.acento_fantasma, 380, false),
        FANTASMA_CLARO("fantasma_claro", "Fantasma Claro", R.style.Theme_Pomodoro_Light_Fantasma,
                R.color.tema_claro_fondo_principal, R.color.acento_fantasma, 380, false),

        MEDIANOCHE_OSCURO("medianoche_oscuro", "Medianoche Oscuro", R.style.Theme_Pomodoro_Dark_Medianoche,
                R.color.tema_oscuro_fondo_principal, R.color.acento_medianoche, 420, false),
        MEDIANOCHE_CLARO("medianoche_claro", "Medianoche Claro", R.style.Theme_Pomodoro_Light_Medianoche,
                R.color.tema_claro_fondo_principal, R.color.acento_medianoche, 420, false),

        LIMA_OSCURO("lima_oscuro", "Lima Oscuro", R.style.Theme_Pomodoro_Dark_Lima,
                R.color.tema_oscuro_fondo_principal, R.color.acento_lima, 600, false),
        LIMA_CLARO("lima_claro", "Lima Claro", R.style.Theme_Pomodoro_Light_Lima,
                R.color.tema_claro_fondo_principal, R.color.acento_lima, 600, false);

        public final String id;
        public final String nombre;
        public final int idEstilo;
        public final int colorFondoPreview;
        public final int colorAcentoPreview;
        public final int precio;
        public final boolean gratis;

        Tema(String id, String nombre, int idEstilo, int colorFondoPreview, int colorAcentoPreview, int precio, boolean gratis) {
            this.id = id;
            this.nombre = nombre;
            this.idEstilo = idEstilo;
            this.colorFondoPreview = colorFondoPreview;
            this.colorAcentoPreview = colorAcentoPreview;
            this.precio = precio;
            this.gratis = gratis;
        }

        public static Tema porId(String id) {
            for (Tema tema : values()) {
                if (tema.id.equals(id)) return tema;
            }
            return AZUL_OSCURO;
        }

        public boolean esClaro() {
            return id.endsWith("claro");
        }
    }

    public GestorTemas(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Tema getTemaActual() {
        return Tema.porId(prefs.getString(CLAVE_TEMA_ACTUAL, Tema.AZUL_OSCURO.id));
    }

    public void establecerTemaActual(Tema tema) {
        prefs.edit().putString(CLAVE_TEMA_ACTUAL, tema.id).apply();
    }

    public boolean estaDesbloqueado(Tema tema) {
        if (tema.gratis) return true;
        return obtenerDesbloqueados().contains(tema.id);
    }

    public void desbloquear(Tema tema) {
        Set<String> desbloqueados = obtenerDesbloqueados();
        desbloqueados.add(tema.id);
        prefs.edit().putStringSet(CLAVE_TEMAS_DESBLOQUEADOS, desbloqueados).apply();
    }

    private Set<String> obtenerDesbloqueados() {
        return new HashSet<>(prefs.getStringSet(CLAVE_TEMAS_DESBLOQUEADOS, new HashSet<>()));
    }

    public void aplicarTema(Activity actividad) {
        actividad.setTheme(getTemaActual().idEstilo);
    }
}
