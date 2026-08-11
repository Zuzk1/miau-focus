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
    private static final String EXTRA_IMAGENES = "miui.focus.pics";
    private static final String EXTRA_SIN_REFLOTAR = "miui.island.updateNoFloat";
    private static final String CLAVE_IMAGEN = "miui.focus.pic_ticker";
    private static final String EXTRA_PROMOVER = "android.requestPromotedOngoing";

    private static final int PROTOCOLO = 1;
    private static final int TIMER_DESCENDENTE = 2;
    private static final int TIPO_TEXTO = 1;
    private static final int SDK_ACTIVIDADES_EN_VIVO = 36;


    public static void aplicar(Context contexto, Notification.Builder constructor, String titulo,
            String detalle, long tiempoRestante, long duracionTotal, boolean primeraVez) {
        int acento = colorAcento(contexto);
        long restante = Math.max(0L, tiempoRestante);
        long total = Math.max(restante, duracionTotal);

        if (Build.VERSION.SDK_INT >= SDK_ACTIVIDADES_EN_VIVO) {
            aplicarActividadEnVivo(contexto, constructor, restante, total, acento);
            return;
        }
        aplicarEnfoqueXiaomi(contexto, constructor, titulo, detalle, restante, total, acento, primeraVez);
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

        try {
            Notification.ProgressStyle estilo = new Notification.ProgressStyle()
                    .setProgress(transcurrido)
                    .setStyledByProgress(true)
                    .setProgressSegments(Collections.singletonList(
                            new Notification.ProgressStyle.Segment(total).setColor(acento)))
                    .setProgressTrackerIcon(Icon.createWithResource(contexto, R.drawable.ic_gato_notificacion));

            Bundle promocion = new Bundle();
            promocion.putBoolean(EXTRA_PROMOVER, true);

            constructor.setStyle(estilo)
                    .setShortCriticalText(formatearRestante(tiempoRestante))
                    .setColor(acento)
                    .addExtras(promocion);
        } catch (Throwable error) {
            constructor.setColor(acento);
        }
    }

    private static void aplicarEnfoqueXiaomi(Context contexto, Notification.Builder constructor,
            String titulo, String detalle, long tiempoRestante, long duracionTotal, int acento,
            boolean primeraVez) {
        String param = construirParametro(titulo, detalle, tiempoRestante, duracionTotal, acento, primeraVez);
        if (param == null) return;

        Bundle imagenes = new Bundle();
        imagenes.putParcelable(CLAVE_IMAGEN, Icon.createWithResource(contexto, R.drawable.ic_gato_notificacion));

        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_ENFOQUE, true);
        extras.putString(EXTRA_PARAM, param);
        extras.putBundle(EXTRA_IMAGENES, imagenes);
        extras.putBoolean(EXTRA_SIN_REFLOTAR, !primeraVez);

        constructor.addExtras(extras);
    }

    private static String construirParametro(String titulo, String detalle, long tiempoRestante,
            long duracionTotal, int acento, boolean primeraVez) {
        try {
            String ticker = titulo + " · " + formatearRestante(tiempoRestante);
            String colorAcento = enHexadecimal(acento);

            JSONObject informacionCronometro = new JSONObject()
                    .put("timerType", TIMER_DESCENDENTE)
                    .put("timerWhen", tiempoRestante)
                    .put("timerTotal", duracionTotal)
                    .put("timerSystemCurrent", SystemClock.elapsedRealtime());

            JSONObject informacionBase = new JSONObject()
                    .put("type", TIPO_TEXTO)
                    .put("title", titulo)
                    .put("content", detalle)
                    .put("colorTitle", colorAcento)
                    .put("picFunction", CLAVE_IMAGEN);

            JSONObject informacionDestacada = new JSONObject()
                    .put("type", TIPO_TEXTO)
                    .put("title", titulo)
                    .put("colorTitle", colorAcento)
                    .put("picFunction", CLAVE_IMAGEN)
                    .put("timerInfo", informacionCronometro);

            JSONObject informacionProgreso = new JSONObject()
                    .put("progress", (int) ((duracionTotal - tiempoRestante) * 100L / Math.max(1L, duracionTotal)))
                    .put("colorProgress", colorAcento)
                    .put("colorProgressEnd", colorAcento);

            JSONObject informacionImagen = new JSONObject()
                    .put("type", TIPO_TEXTO)
                    .put("pic", CLAVE_IMAGEN);

            JSONObject plantilla = new JSONObject()
                    .put("protocol", PROTOCOLO)
                    .put("enableFloat", true)
                    .put("updatable", true)
                    .put("islandFirstFloat", primeraVez)
                    .put("aodTitle", titulo)
                    .put("ticker", ticker)
                    .put("tickerPic", CLAVE_IMAGEN)
                    .put("baseInfo", informacionBase)
                    .put("highlightInfo", informacionDestacada)
                    .put("progressInfo", informacionProgreso)
                    .put("picInfo", informacionImagen);

            return new JSONObject()
                    .put("protocol", PROTOCOLO)
                    .put("enableFloat", true)
                    .put("updatable", true)
                    .put("ticker", ticker)
                    .put("tickerPic", CLAVE_IMAGEN)
                    .put("title", titulo)
                    .put("content", detalle)
                    .put("timerType", TIMER_DESCENDENTE)
                    .put("timerWhen", tiempoRestante)
                    .put("param_v2", plantilla)
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
