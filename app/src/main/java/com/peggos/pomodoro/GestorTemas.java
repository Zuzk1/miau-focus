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
        AZUL_OSCURO("azul_oscuro", R.string.tema_color_azul, R.style.Theme_Pomodoro_Dark_Azul,
                R.color.tema_oscuro_fondo_principal, R.color.acento_azul, 0, true),
        AZUL_CLARO("azul_claro", R.string.tema_color_azul, R.style.Theme_Pomodoro_Light_Azul,
                R.color.tema_claro_fondo_principal, R.color.acento_azul, 0, true),

        VERDE_OSCURO("verde_oscuro", R.string.tema_color_verde, R.style.Theme_Pomodoro_Dark_Verde,
                R.color.tema_oscuro_fondo_principal, R.color.acento_verde, 100, false),
        VERDE_CLARO("verde_claro", R.string.tema_color_verde, R.style.Theme_Pomodoro_Light_Verde,
                R.color.tema_claro_fondo_principal, R.color.acento_verde, 100, false),

        CACAO_OSCURO("cacao_oscuro", R.string.tema_color_cacao, R.style.Theme_Pomodoro_Dark_Cacao,
                R.color.tema_oscuro_fondo_principal, R.color.acento_cacao, 110, false),
        CACAO_CLARO("cacao_claro", R.string.tema_color_cacao, R.style.Theme_Pomodoro_Light_Cacao,
                R.color.tema_claro_fondo_principal, R.color.acento_cacao, 110, false),

        NARANJA_OSCURO("naranja_oscuro", R.string.tema_color_naranja, R.style.Theme_Pomodoro_Dark_Naranja,
                R.color.tema_oscuro_fondo_principal, R.color.acento_naranja, 120, false),
        NARANJA_CLARO("naranja_claro", R.string.tema_color_naranja, R.style.Theme_Pomodoro_Light_Naranja,
                R.color.tema_claro_fondo_principal, R.color.acento_naranja, 120, false),

        ROJO_OSCURO("rojo_oscuro", R.string.tema_color_rojo, R.style.Theme_Pomodoro_Dark_Rojo,
                R.color.tema_oscuro_fondo_principal, R.color.acento_rojo, 140, false),
        ROJO_CLARO("rojo_claro", R.string.tema_color_rojo, R.style.Theme_Pomodoro_Light_Rojo,
                R.color.tema_claro_fondo_principal, R.color.acento_rojo, 140, false),

        EUCALIPTO_OSCURO("eucalipto_oscuro", R.string.tema_color_eucalipto, R.style.Theme_Pomodoro_Dark_Eucalipto,
                R.color.tema_oscuro_fondo_principal, R.color.acento_eucalipto, 150, false),
        EUCALIPTO_CLARO("eucalipto_claro", R.string.tema_color_eucalipto, R.style.Theme_Pomodoro_Light_Eucalipto,
                R.color.tema_claro_fondo_principal, R.color.acento_eucalipto, 150, false),

        MORADO_OSCURO("morado_oscuro", R.string.tema_color_morado, R.style.Theme_Pomodoro_Dark_Morado,
                R.color.tema_oscuro_fondo_principal, R.color.acento_morado, 160, false),
        MORADO_CLARO("morado_claro", R.string.tema_color_morado, R.style.Theme_Pomodoro_Light_Morado,
                R.color.tema_claro_fondo_principal, R.color.acento_morado, 160, false),

        ROSA_OSCURO("rosa_oscuro", R.string.tema_color_rosa, R.style.Theme_Pomodoro_Dark_Rosa,
                R.color.tema_oscuro_fondo_principal, R.color.acento_rosa, 180, false),
        ROSA_CLARO("rosa_claro", R.string.tema_color_rosa, R.style.Theme_Pomodoro_Light_Rosa,
                R.color.tema_claro_fondo_principal, R.color.acento_rosa, 180, false),

        MARINO_OSCURO("marino_oscuro", R.string.tema_color_marino, R.style.Theme_Pomodoro_Dark_Marino,
                R.color.tema_oscuro_fondo_principal, R.color.acento_marino, 200, false),
        MARINO_CLARO("marino_claro", R.string.tema_color_marino, R.style.Theme_Pomodoro_Light_Marino,
                R.color.tema_claro_fondo_principal, R.color.acento_marino, 200, false),

        MOSTAZA_OSCURO("mostaza_oscuro", R.string.tema_color_mostaza, R.style.Theme_Pomodoro_Dark_Mostaza,
                R.color.tema_oscuro_fondo_principal, R.color.acento_mostaza, 220, false),
        MOSTAZA_CLARO("mostaza_claro", R.string.tema_color_mostaza, R.style.Theme_Pomodoro_Light_Mostaza,
                R.color.tema_claro_fondo_principal, R.color.acento_mostaza, 220, false),

        VAINILLA_OSCURO("vainilla_oscuro", R.string.tema_color_vainilla, R.style.Theme_Pomodoro_Dark_Vainilla,
                R.color.tema_oscuro_fondo_principal, R.color.acento_vainilla, 230, false),
        VAINILLA_CLARO("vainilla_claro", R.string.tema_color_vainilla, R.style.Theme_Pomodoro_Light_Vainilla,
                R.color.tema_claro_fondo_principal, R.color.acento_vainilla, 230, false),

        TURQUESA_OSCURO("turquesa_oscuro", R.string.tema_color_turquesa, R.style.Theme_Pomodoro_Dark_Turquesa,
                R.color.tema_oscuro_fondo_principal, R.color.acento_turquesa, 240, false),
        TURQUESA_CLARO("turquesa_claro", R.string.tema_color_turquesa, R.style.Theme_Pomodoro_Light_Turquesa,
                R.color.tema_claro_fondo_principal, R.color.acento_turquesa, 240, false),

        AGUAMARINA_OSCURO("aguamarina_oscuro", R.string.tema_color_aguamarina, R.style.Theme_Pomodoro_Dark_Aguamarina,
                R.color.tema_oscuro_fondo_principal, R.color.acento_aguamarina, 250, false),
        AGUAMARINA_CLARO("aguamarina_claro", R.string.tema_color_aguamarina, R.style.Theme_Pomodoro_Light_Aguamarina,
                R.color.tema_claro_fondo_principal, R.color.acento_aguamarina, 250, false),

        GRAFITO_OSCURO("grafito_oscuro", R.string.tema_color_grafito, R.style.Theme_Pomodoro_Dark_Grafito,
                R.color.tema_oscuro_fondo_principal, R.color.acento_grafito, 260, false),
        GRAFITO_CLARO("grafito_claro", R.string.tema_color_grafito, R.style.Theme_Pomodoro_Light_Grafito,
                R.color.tema_claro_fondo_principal, R.color.acento_grafito, 260, false),

        SALVIA_OSCURO("salvia_oscuro", R.string.tema_color_salvia, R.style.Theme_Pomodoro_Dark_Salvia,
                R.color.tema_oscuro_fondo_principal, R.color.acento_salvia, 270, false),
        SALVIA_CLARO("salvia_claro", R.string.tema_color_salvia, R.style.Theme_Pomodoro_Light_Salvia,
                R.color.tema_claro_fondo_principal, R.color.acento_salvia, 270, false),

        CORAL_OSCURO("coral_oscuro", R.string.tema_color_coral, R.style.Theme_Pomodoro_Dark_Coral,
                R.color.tema_oscuro_fondo_principal, R.color.acento_coral, 280, false),
        CORAL_CLARO("coral_claro", R.string.tema_color_coral, R.style.Theme_Pomodoro_Light_Coral,
                R.color.tema_claro_fondo_principal, R.color.acento_coral, 280, false),

        DENIM_OSCURO("denim_oscuro", R.string.tema_color_denim, R.style.Theme_Pomodoro_Dark_Denim,
                R.color.tema_oscuro_fondo_principal, R.color.acento_denim, 290, false),
        DENIM_CLARO("denim_claro", R.string.tema_color_denim, R.style.Theme_Pomodoro_Light_Denim,
                R.color.tema_claro_fondo_principal, R.color.acento_denim, 290, false),

        LAVANDA_OSCURO("lavanda_oscuro", R.string.tema_color_lavanda, R.style.Theme_Pomodoro_Dark_Lavanda,
                R.color.tema_oscuro_fondo_principal, R.color.acento_lavanda, 300, false),
        LAVANDA_CLARO("lavanda_claro", R.string.tema_color_lavanda, R.style.Theme_Pomodoro_Light_Lavanda,
                R.color.tema_claro_fondo_principal, R.color.acento_lavanda, 300, false),

        PETROLEO_OSCURO("petroleo_oscuro", R.string.tema_color_petroleo, R.style.Theme_Pomodoro_Dark_Petroleo,
                R.color.tema_oscuro_fondo_principal, R.color.acento_petroleo, 310, false),
        PETROLEO_CLARO("petroleo_claro", R.string.tema_color_petroleo, R.style.Theme_Pomodoro_Light_Petroleo,
                R.color.tema_claro_fondo_principal, R.color.acento_petroleo, 310, false),

        BERENJENA_OSCURO("berenjena_oscuro", R.string.tema_color_berenjena, R.style.Theme_Pomodoro_Dark_Berenjena,
                R.color.tema_oscuro_fondo_principal, R.color.acento_berenjena, 330, false),
        BERENJENA_CLARO("berenjena_claro", R.string.tema_color_berenjena, R.style.Theme_Pomodoro_Light_Berenjena,
                R.color.tema_claro_fondo_principal, R.color.acento_berenjena, 330, false),

        CARMESI_OSCURO("carmesi_oscuro", R.string.tema_color_carmesi, R.style.Theme_Pomodoro_Dark_Carmesi,
                R.color.tema_oscuro_fondo_principal, R.color.acento_carmesi, 340, false),
        CARMESI_CLARO("carmesi_claro", R.string.tema_color_carmesi, R.style.Theme_Pomodoro_Light_Carmesi,
                R.color.tema_claro_fondo_principal, R.color.acento_carmesi, 340, false),

        ARENA_OSCURO("arena_oscuro", R.string.tema_color_arena, R.style.Theme_Pomodoro_Dark_Arena,
                R.color.tema_oscuro_fondo_principal, R.color.acento_arena, 350, false),
        ARENA_CLARO("arena_claro", R.string.tema_color_arena, R.style.Theme_Pomodoro_Light_Arena,
                R.color.tema_claro_fondo_principal, R.color.acento_arena, 350, false),

        FANTASMA_OSCURO("fantasma_oscuro", R.string.tema_color_violeta, R.style.Theme_Pomodoro_Dark_Fantasma,
                R.color.tema_oscuro_fondo_principal, R.color.acento_fantasma, 380, false),
        FANTASMA_CLARO("fantasma_claro", R.string.tema_color_violeta, R.style.Theme_Pomodoro_Light_Fantasma,
                R.color.tema_claro_fondo_principal, R.color.acento_fantasma, 380, false),

        MEDIANOCHE_OSCURO("medianoche_oscuro", R.string.tema_color_medianoche, R.style.Theme_Pomodoro_Dark_Medianoche,
                R.color.tema_oscuro_fondo_principal, R.color.acento_medianoche, 420, false),
        MEDIANOCHE_CLARO("medianoche_claro", R.string.tema_color_medianoche, R.style.Theme_Pomodoro_Light_Medianoche,
                R.color.tema_claro_fondo_principal, R.color.acento_medianoche, 420, false),

        NEON_OSCURO("neon_oscuro", R.string.tema_color_neon, R.style.Theme_Pomodoro_Dark_Neon,
                R.color.tema_oscuro_fondo_principal, R.color.acento_neon, 460, false),
        NEON_CLARO("neon_claro", R.string.tema_color_neon, R.style.Theme_Pomodoro_Light_Neon,
                R.color.tema_claro_fondo_principal, R.color.acento_neon, 460, false),

        ULTRAMAR_OSCURO("ultramar_oscuro", R.string.tema_color_ultramar, R.style.Theme_Pomodoro_Dark_Ultramar,
                R.color.tema_oscuro_fondo_principal, R.color.acento_ultramar, 520, false),
        ULTRAMAR_CLARO("ultramar_claro", R.string.tema_color_ultramar, R.style.Theme_Pomodoro_Light_Ultramar,
                R.color.tema_claro_fondo_principal, R.color.acento_ultramar, 520, false),

        LIMA_OSCURO("lima_oscuro", R.string.tema_color_lima, R.style.Theme_Pomodoro_Dark_Lima,
                R.color.tema_oscuro_fondo_principal, R.color.acento_lima, 600, false),
        LIMA_CLARO("lima_claro", R.string.tema_color_lima, R.style.Theme_Pomodoro_Light_Lima,
                R.color.tema_claro_fondo_principal, R.color.acento_lima, 600, false);

        public final String id;
        public final int nombreColorRes;
        public final int idEstilo;
        public final int colorFondoPreview;
        public final int colorAcentoPreview;
        public final int precio;
        public final boolean gratis;

        Tema(String id, int nombreColorRes, int idEstilo, int colorFondoPreview, int colorAcentoPreview, int precio, boolean gratis) {
            this.id = id;
            this.nombreColorRes = nombreColorRes;
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

        public String getNombre(Context contexto) {
            return contexto.getString(
                    esClaro() ? R.string.tema_variante_claro : R.string.tema_variante_oscuro,
                    contexto.getString(nombreColorRes));
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
