package com.peggos.pomodoro;

import android.content.Context;
import android.content.SharedPreferences;

public class GestorConfiguracion {

    private static final String PREF_NAME = "PomodoroConfiguracion";
    private static final String CLAVE_MINUTOS_TRABAJO = "minutos_trabajo";
    private static final String CLAVE_MINUTOS_DESCANSO_CORTO = "minutos_descanso_corto";
    private static final String CLAVE_MINUTOS_DESCANSO_LARGO = "minutos_descanso_largo";
    private static final String CLAVE_ALERTAS_DISTRACCION_HABILITADAS = "alertas_distraccion_habilitadas";
    private static final String CLAVE_DESCANSO_LARGO_HABILITADO = "descanso_largo_habilitado";
    private static final String CLAVE_OPCIONES_PREDETERMINADAS = "opciones_predeterminadas_habilitadas";
    private static final String CLAVE_VIBRACION_HABILITADA = "vibracion_habilitada";
    private static final String CLAVE_MODO_SILENCIOSO_HABILITADO = "modo_silencioso_habilitado";
    private static final String CLAVE_HORA_INICIO_SILENCIO = "hora_inicio_silencio";
    private static final String CLAVE_HORA_FIN_SILENCIO = "hora_fin_silencio";

    public static final int MINUTOS_TRABAJO_PREDETERMINADO = 25;
    public static final int MINUTOS_DESCANSO_CORTO_PREDETERMINADO = 5;
    public static final int MINUTOS_DESCANSO_LARGO_PREDETERMINADO = 15;

    private final SharedPreferences prefs;

    public GestorConfiguracion(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getMinutosTrabajo() {
        if (isOpcionesPredeterminadasHabilitadas()) return MINUTOS_TRABAJO_PREDETERMINADO;
        return prefs.getInt(CLAVE_MINUTOS_TRABAJO, MINUTOS_TRABAJO_PREDETERMINADO);
    }

    public int getMinutosDescansoCorto() {
        if (isOpcionesPredeterminadasHabilitadas()) return MINUTOS_DESCANSO_CORTO_PREDETERMINADO;
        return prefs.getInt(CLAVE_MINUTOS_DESCANSO_CORTO, MINUTOS_DESCANSO_CORTO_PREDETERMINADO);
    }

    public int getMinutosDescansoLargo() {
        if (isOpcionesPredeterminadasHabilitadas()) return MINUTOS_DESCANSO_LARGO_PREDETERMINADO;
        return prefs.getInt(CLAVE_MINUTOS_DESCANSO_LARGO, MINUTOS_DESCANSO_LARGO_PREDETERMINADO);
    }

    public void guardar(int minutosTrabajo, int minutosDescansoCorto, int minutosDescansoLargo) {
        prefs.edit()
                .putInt(CLAVE_MINUTOS_TRABAJO, minutosTrabajo)
                .putInt(CLAVE_MINUTOS_DESCANSO_CORTO, minutosDescansoCorto)
                .putInt(CLAVE_MINUTOS_DESCANSO_LARGO, minutosDescansoLargo)
                .apply();
    }

    public boolean isAlertasDistraccionHabilitadas() {
        return prefs.getBoolean(CLAVE_ALERTAS_DISTRACCION_HABILITADAS, true);
    }

    public void setAlertasDistraccionHabilitadas(boolean habilitadas) {
        prefs.edit().putBoolean(CLAVE_ALERTAS_DISTRACCION_HABILITADAS, habilitadas).apply();
    }

    public boolean isDescansoLargoHabilitado() {
        return prefs.getBoolean(CLAVE_DESCANSO_LARGO_HABILITADO, true);
    }

    public void setDescansoLargoHabilitado(boolean habilitado) {
        prefs.edit().putBoolean(CLAVE_DESCANSO_LARGO_HABILITADO, habilitado).apply();
    }

    public boolean isOpcionesPredeterminadasHabilitadas() {
        return prefs.getBoolean(CLAVE_OPCIONES_PREDETERMINADAS, false);
    }

    public void setOpcionesPredeterminadasHabilitadas(boolean habilitadas) {
        prefs.edit().putBoolean(CLAVE_OPCIONES_PREDETERMINADAS, habilitadas).apply();
    }

    public boolean isVibracionHabilitada() {
        return prefs.getBoolean(CLAVE_VIBRACION_HABILITADA, true);
    }

    public void setVibracionHabilitada(boolean habilitada) {
        prefs.edit().putBoolean(CLAVE_VIBRACION_HABILITADA, habilitada).apply();
    }

    public boolean isModoSilenciosoHabilitado() {
        return prefs.getBoolean(CLAVE_MODO_SILENCIOSO_HABILITADO, false);
    }

    public void setModoSilenciosoHabilitado(boolean habilitado) {
        prefs.edit().putBoolean(CLAVE_MODO_SILENCIOSO_HABILITADO, habilitado).apply();
    }

    public int getHoraInicioSilencio() {
        return prefs.getInt(CLAVE_HORA_INICIO_SILENCIO, 22);
    }

    public void setHoraInicioSilencio(int hora) {
        prefs.edit().putInt(CLAVE_HORA_INICIO_SILENCIO, hora).apply();
    }

    public int getHoraFinSilencio() {
        return prefs.getInt(CLAVE_HORA_FIN_SILENCIO, 7);
    }

    public void setHoraFinSilencio(int hora) {
        prefs.edit().putInt(CLAVE_HORA_FIN_SILENCIO, hora).apply();
    }

    public boolean estaEnHorarioSilencioso() {
        if (!isModoSilenciosoHabilitado()) return false;

        int horaActual = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int inicio = getHoraInicioSilencio();
        int fin = getHoraFinSilencio();

        if (inicio == fin) return false;
        if (inicio < fin) {
            return horaActual >= inicio && horaActual < fin;
        }
        return horaActual >= inicio || horaActual < fin;
    }
}
