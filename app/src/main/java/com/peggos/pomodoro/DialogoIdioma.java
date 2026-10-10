package com.peggos.pomodoro;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import java.util.function.IntConsumer;

public class DialogoIdioma {

    public static void mostrar(Context contexto, int indiceSeleccionado, IntConsumer alSeleccionar) {
        Dialog dialogo = new Dialog(contexto);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_idioma);

        Window ventana = dialogo.getWindow();
        if (ventana != null) {
            ventana.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout lista = dialogo.findViewById(R.id.llListaIdiomas);
        NestedScrollView scroll = dialogo.findViewById(R.id.scrollIdiomas);
        LayoutInflater inflador = LayoutInflater.from(contexto);

        String[] siglas = contexto.getResources().getStringArray(R.array.idiomas_siglas);
        String[] nombres = contexto.getResources().getStringArray(R.array.idiomas_nombres);

        int total = nombres.length + 1;
        for (int i = 0; i < total; i++) {
            final int indice = i;
            View fila = inflador.inflate(R.layout.item_idioma, lista, false);

            TextView sigla = fila.findViewById(R.id.tvSiglaIdioma);
            TextView nombre = fila.findViewById(R.id.tvNombreIdioma);
            ImageView palomita = fila.findViewById(R.id.ivPalomitaIdioma);

            sigla.setText(i == 0 ? contexto.getString(R.string.idioma_sistema_sigla) : siglas[i - 1]);
            nombre.setText(i == 0 ? contexto.getString(R.string.idioma_sistema) : nombres[i - 1]);

            boolean activo = i == indiceSeleccionado;
            fila.setBackgroundResource(activo ? R.drawable.carta_idioma_activa : R.drawable.carta_idioma);
            palomita.setVisibility(activo ? View.VISIBLE : View.INVISIBLE);

            fila.setOnClickListener(v -> {
                dialogo.dismiss();
                if (alSeleccionar != null) alSeleccionar.accept(indice);
            });

            lista.addView(fila);

            if (activo) {
                scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, fila.getTop() - fila.getHeight())));
            }
        }

        dialogo.findViewById(R.id.tvCancelarIdioma).setOnClickListener(v -> dialogo.dismiss());

        dialogo.show();
    }
}
