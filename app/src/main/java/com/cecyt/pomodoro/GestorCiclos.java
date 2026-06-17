package com.cecyt.pomodoro;

import android.content.Context;
import android.content.SharedPreferences;

public class GestorCiclos {

    public static class Resultado {
        public final long tiempoSiguiente;
        public final boolean esDescansoSiguiente;
        public final int cicloSiguiente;
        public final String tituloSiguiente;
        public final boolean completoLosCuatroCiclos;

        Resultado(long tiempoSiguiente, boolean esDescansoSiguiente, int cicloSiguiente,
                  String tituloSiguiente, boolean completoLosCuatroCiclos) {
            this.tiempoSiguiente = tiempoSiguiente;
            this.esDescansoSiguiente = esDescansoSiguiente;
            this.cicloSiguiente = cicloSiguiente;
            this.tituloSiguiente = tituloSiguiente;
            this.completoLosCuatroCiclos = completoLosCuatroCiclos;
        }
    }

    public static Resultado procesarFinDeFase(Context contexto, boolean esDescanso, int cicloActual) {
        GestorConfiguracion configuracion = new GestorConfiguracion(contexto);
        long tiempoEnfoque = configuracion.getMinutosTrabajo() * 60000L;
        long tiempoDescansoCorto = configuracion.getMinutosDescansoCorto() * 60000L;
        long tiempoDescansoLargo = configuracion.getMinutosDescansoLargo() * 60000L;

        if (esDescanso) {
            return new Resultado(tiempoEnfoque, false, cicloActual, "SESIÓN DE ENFOQUE", false);
        }

        GestorEstadisticas gestor = new GestorEstadisticas(contexto);
        boolean alertasHabilitadas = configuracion.isAlertasDistraccionHabilitadas();
        float puntosPorCiclo = alertasHabilitadas ? 37.5f : 3.75f;

        SharedPreferences preferencias = contexto.getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE);
        String tareaActiva = preferencias.getString("tarea_activa_actual", "Ninguna");

        int siguienteCiclo = cicloActual + 1;
        boolean esCicloCompleto = siguienteCiclo > 4;

        // UNIFICADO: Guardar todo de una vez para evitar pérdida de datos
        gestor.registrarFinDeFase((int) (tiempoEnfoque / 60000), puntosPorCiclo, tareaActiva, esCicloCompleto);

        if (esCicloCompleto) {
            if (configuracion.isDescansoLargoHabilitado()) {
                return new Resultado(tiempoDescansoLargo, true, 1, "DESCANSO LARGO", true);
            }
            return new Resultado(tiempoEnfoque, false, 1, "SESIÓN DE ENFOQUE", true);
        }

        return new Resultado(tiempoDescansoCorto, true, siguienteCiclo, "DESCANSO CORTO", false);
    }
}
