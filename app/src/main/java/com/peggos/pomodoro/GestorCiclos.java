package com.peggos.pomodoro;

import android.content.Context;
import android.content.SharedPreferences;

public class GestorCiclos {

    public static class Resultado {
        public final long tiempoSiguiente;
        public final boolean esDescansoSiguiente;
        public final int cicloSiguiente;
        public final boolean completoLosCuatroCiclos;

        Resultado(long tiempoSiguiente, boolean esDescansoSiguiente, int cicloSiguiente,
                  boolean completoLosCuatroCiclos) {
            this.tiempoSiguiente = tiempoSiguiente;
            this.esDescansoSiguiente = esDescansoSiguiente;
            this.cicloSiguiente = cicloSiguiente;
            this.completoLosCuatroCiclos = completoLosCuatroCiclos;
        }

        public int resTitulo() {
            return GestorAlertas.resTituloDeFase(esDescansoSiguiente, cicloSiguiente);
        }
    }

    public static Resultado procesarFinDeFase(Context contexto, boolean esDescanso, int cicloActual) {
        GestorConfiguracion configuracion = new GestorConfiguracion(contexto);
        long tiempoEnfoque = configuracion.getMinutosTrabajo() * 60000L;
        long tiempoDescansoCorto = configuracion.getMinutosDescansoCorto() * 60000L;
        long tiempoDescansoLargo = configuracion.getMinutosDescansoLargo() * 60000L;

        if (esDescanso) {
            return new Resultado(tiempoEnfoque, false, cicloActual, false);
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
            marcarTareaActivaComoCompletada(preferencias, tareaActiva);

            if (configuracion.isDescansoLargoHabilitado()) {
                return new Resultado(tiempoDescansoLargo, true, 1, true);
            }
            return new Resultado(tiempoEnfoque, false, 1, true);
        }

        return new Resultado(tiempoDescansoCorto, true, siguienteCiclo, false);
    }

    private static void marcarTareaActivaComoCompletada(SharedPreferences preferencias, String tareaActiva) {
        if (tareaActiva == null || tareaActiva.isEmpty() || tareaActiva.equals("Ninguna")) return;

        int cantidad = preferencias.getInt("cantidad_tareas", 0);
        int indiceCompletada = -1;

        for (int i = 0; i < cantidad; i++) {
            if (tareaActiva.equals(preferencias.getString("tarea_" + i, null))
                    && !preferencias.getBoolean("completada_" + i, false)) {
                indiceCompletada = i;
                break;
            }
        }

        if (indiceCompletada == -1) return;

        String siguienteActiva = "Ninguna";
        for (int i = 0; i < cantidad; i++) {
            if (i == indiceCompletada || preferencias.getBoolean("completada_" + i, false)) continue;
            String nombre = preferencias.getString("tarea_" + i, null);
            if (nombre != null) {
                siguienteActiva = nombre;
                break;
            }
        }

        preferencias.edit()
                .putBoolean("completada_" + indiceCompletada, true)
                .putLong("completada_fecha_" + indiceCompletada, GestorEstadisticas.obtenerDiaActual())
                .putString("tarea_activa_actual", siguienteActiva)
                .apply();
    }
}
