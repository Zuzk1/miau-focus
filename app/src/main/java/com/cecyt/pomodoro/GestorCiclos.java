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
        gestor.registrarSesionEnfoque((int) (tiempoEnfoque / 60000), puntosPorCiclo);

        SharedPreferences preferencias = contexto.getSharedPreferences(GestorAlertas.PREFS_NOMBRE_COMPARTIDO, Context.MODE_PRIVATE);
        String tareaActiva = preferencias.getString("tarea_activa_actual", "Ninguna");
        gestor.registrarActividadCompletada(tareaActiva);

        int siguienteCiclo = cicloActual + 1;
        if (siguienteCiclo > 4) {
            gestor.registrarCicloCompleto();
            if (configuracion.isDescansoLargoHabilitado()) {
                return new Resultado(tiempoDescansoLargo, true, 1, "DESCANSO LARGO", true);
            }
            return new Resultado(tiempoEnfoque, false, 1, "SESIÓN DE ENFOQUE", true);
        }

        return new Resultado(tiempoDescansoCorto, true, siguienteCiclo, "DESCANSO CORTO", false);
    }
}
