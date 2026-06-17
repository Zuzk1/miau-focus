package com.cecyt.pomodoro;

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

public class DialogoEscudo {

    public static void mostrar(Context contexto, Runnable accionActivar) {
        Dialog dialogo = new Dialog(contexto);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_escudo);

        Window ventana = dialogo.getWindow();
        if (ventana != null) {
            ventana.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LottieAnimationView gatoEscudo = dialogo.findViewById(R.id.lottieGatoEscudo);
        MaterialButton btnActivar = dialogo.findViewById(R.id.btnActivarEscudo);
        TextView tvCancelar = dialogo.findViewById(R.id.tvCancelarEscudo);

        btnActivar.setOnClickListener(v -> {
            dialogo.dismiss();
            if (accionActivar != null) accionActivar.run();
        });
        tvCancelar.setOnClickListener(v -> dialogo.dismiss());

        animarGato(gatoEscudo);

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
