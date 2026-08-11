package com.peggos.pomodoro;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Locale;

public class IslaDinamica {

    public static final long INTERVALO_REFRESCO = 1000L;

    private static final String EXTRA_ENFOQUE = "miui.focus.notification";
    private static final String EXTRA_PARAM = "miui.focus.param";
    private static final String EXTRA_PARAM_V2 = "miui.focus.param.v2";
    private static final String EXTRA_IMAGENES = "miui.focus.pics";
    private static final String CLAVE_IMAGEN = "miui.focus.pic_ticker";

    private static final int PROTOCOLO = 1;
    private static final int TIMER_DESCENDENTE = 2;
    private static final int TIPO_TEXTO = 1;
    private static final int SDK_ACTIVIDADES_EN_VIVO = 36;

    public static void aplicar(Context contexto, Notification.Builder constructor, String titulo,
            String detalle, long tiempoRestante, long duracionTotal) {
        int acento = colorAcento(contexto);
        long restante = Math.max(0L, tiempoRestante);
        long total = Math.max(restante, duracionTotal);

        aplicarActividadEnVivo(contexto, constructor, restante, total, acento);
        aplicarEnfoqueXiaomi(contexto, constructor, titulo, detalle, restante, total, acento);
    }

    public static String formatearRestante(long tiempoRestante) {
        long segundos = Math.max(0L, tiempoRestante + 999L) / 1000L;
        return String.format(Locale.getDefault(), "%d:%02d", segundos / 60, segundos % 60);
    }

    private static void aplicarActividadEnVivo(Context contexto, Notification.Builder constructor,
            long tiempoRestante, long duracionTotal, int acento) {
        if (Build.VERSION.SDK_INT < SDK_ACTIVIDADES_EN_VIVO) return;

        int total = (int) Math.max(1L, duracionTotal);
        int transcurrido = (int) Math.min(total, Math.max(0L, duracionTotal - tiempoRestante));

        Notification.ProgressStyle estilo = new Notification.ProgressStyle()
                .setProgress(transcurrido)
                .setStyledByProgress(true)
                .setProgressSegments(Collections.singletonList(
                        new Notification.ProgressStyle.Segment(total).setColor(acento)))
                .setProgressTrackerIcon(Icon.createWithResource(contexto, R.drawable.ic_gato_notificacion));

        constructor.setStyle(estilo)
                .setShortCriticalText(formatearRestante(tiempoRestante))
                .setColor(acento)
                .setRequestPromotedOngoing(true);
    }

    private static void aplicarEnfoqueXiaomi(Context contexto, Notification.Builder constructor,
            String titulo, String detalle, long tiempoRestante, long duracionTotal, int acento) {
        String param = construirParametro(titulo, detalle, tiempoRestante, duracionTotal, acento);
        String paramHeredado = construirParametroHeredado(titulo, detalle, tiempoRestante);
        if (param == null || paramHeredado == null) return;

        Bundle imagenes = new Bundle();
        imagenes.putParcelable(CLAVE_IMAGEN, Icon.createWithResource(contexto, R.drawable.ic_gato_notificacion));

        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_ENFOQUE, true);
        extras.putString(EXTRA_PARAM, paramHeredado);
        extras.putString(EXTRA_PARAM_V2, param);
        extras.putBundle(EXTRA_IMAGENES, imagenes);

        constructor.addExtras(extras);
    }

    private static String construirParametro(String titulo, String detalle, long tiempoRestante,
            long duracionTotal, int acento) {
        try {
            JSONObject informacionBase = new JSONObject()
                    .put("type", TIPO_TEXTO)
                    .put("title", titulo)
                    .put("content", detalle)
                    .put("picFunction", CLAVE_IMAGEN);

            JSONObject informacionCronometro = new JSONObject()
                    .put("type", TIMER_DESCENDENTE)
                    .put("timerWhen", tiempoRestante)
                    .put("timerTotal", duracionTotal)
                    .put("timerSystemCurrent", SystemClock.elapsedRealtime());

            JSONObject informacionProgreso = new JSONObject()
                    .put("progress", (int) ((duracionTotal - tiempoRestante) * 100L / Math.max(1L, duracionTotal)))
                    .put("colorProgress", enHexadecimal(acento))
                    .put("colorProgressEnd", enHexadecimal(acento));

            JSONObject informacionImagen = new JSONObject()
                    .put("type", TIPO_TEXTO)
                    .put("pic", CLAVE_IMAGEN);

            return new JSONObject()
                    .put("protocol", PROTOCOLO)
                    .put("enableFloat", true)
                    .put("updatable", true)
                    .put("aodTitle", titulo)
                    .put("ticker", titulo + " · " + formatearRestante(tiempoRestante))
                    .put("tickerPic", CLAVE_IMAGEN)
                    .put("baseInfo", informacionBase)
                    .put("timerInfo", informacionCronometro)
                    .put("progressInfo", informacionProgreso)
                    .put("picInfo", informacionImagen)
                    .toString();
        } catch (JSONException error) {
            return null;
        }
    }

    private static String construirParametroHeredado(String titulo, String detalle, long tiempoRestante) {
        try {
            return new JSONObject()
                    .put("protocol", PROTOCOLO)
                    .put("enableFloat", true)
                    .put("updatable", true)
                    .put("ticker", titulo + " · " + formatearRestante(tiempoRestante))
                    .put("tickerPic", CLAVE_IMAGEN)
                    .put("title", titulo)
                    .put("content", detalle)
                    .put("timerType", TIMER_DESCENDENTE)
                    .put("timerWhen", tiempoRestante)
                    .toString();
        } catch (JSONException error) {
            return null;
        }
    }

    private static int colorAcento(Context contexto) {
        return ContextCompat.getColor(contexto, new GestorTemas(contexto).getTemaActual().colorAcentoPreview);
    }

    private static String enHexadecimal(int color) {
        return String.format(Locale.US, "#%06X", color & 0xFFFFFF);
    }
}
