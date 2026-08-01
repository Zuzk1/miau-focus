package com.peggos.pomodoro;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;

public class DialogoPermiso {

    public static void mostrar(Context contexto, String titulo, String mensaje,
                                String textoBotonAceptar, Runnable accionAceptar) {
        mostrar(contexto, titulo, mensaje, textoBotonAceptar, accionAceptar, null);
    }

    public static void mostrar(Context contexto, String titulo, String mensaje,
                                String textoBotonAceptar, Runnable accionAceptar, Runnable alCerrar) {
        Dialog dialogo = new Dialog(contexto);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_permiso);

        Window ventana = dialogo.getWindow();
        if (ventana != null) {
            ventana.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LottieAnimationView gatoPermiso = dialogo.findViewById(R.id.lottieGatoPermiso);
        TextView tvTitulo = dialogo.findViewById(R.id.tvTituloPermiso);
        TextView tvMensaje = dialogo.findViewById(R.id.tvMensajePermiso);
        MaterialButton btnAceptar = dialogo.findViewById(R.id.btnAceptarPermiso);
        TextView tvAhoraNo = dialogo.findViewById(R.id.tvAhoraNoPermiso);

        tvTitulo.setText(titulo);
        tvMensaje.setText(mensaje);
        btnAceptar.setText(textoBotonAceptar);

        btnAceptar.setOnClickListener(v -> {
            dialogo.dismiss();
            if (accionAceptar != null) accionAceptar.run();
        });
        tvAhoraNo.setOnClickListener(v -> dialogo.dismiss());

        if (alCerrar != null) {
            dialogo.setOnDismissListener(dialogoCerrado -> alCerrar.run());
        }

        animarGato(gatoPermiso);

        dialogo.show();
    }

    private static void animarGato(LottieAnimationView gato) {
        ObjectAnimator animacionRebote = ObjectAnimator.ofFloat(gato, "translationY", 0f, -18f);
        animacionRebote.setDuration(650);
        animacionRebote.setRepeatCount(ObjectAnimator.INFINITE);
        animacionRebote.setRepeatMode(ObjectAnimator.REVERSE);
        animacionRebote.setInterpolator(new AccelerateDecelerateInterpolator());
        animacionRebote.start();
    }
}
