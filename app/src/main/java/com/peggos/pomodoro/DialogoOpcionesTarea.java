package com.peggos.pomodoro;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

public class DialogoOpcionesTarea {

    public static void mostrar(Context contexto, Runnable alFijar, Runnable alEditar, Runnable alEliminar) {
        Dialog dialogo = new Dialog(contexto);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_opciones_tarea);

        Window ventana = dialogo.getWindow();
        if (ventana != null) {
            ventana.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnFijar = dialogo.findViewById(R.id.btnFijarTarea);
        MaterialButton btnEditar = dialogo.findViewById(R.id.btnEditarTarea);
        MaterialButton btnEliminar = dialogo.findViewById(R.id.btnEliminarTarea);
        TextView tvCancelar = dialogo.findViewById(R.id.tvCancelarOpcionesTarea);

        btnFijar.setOnClickListener(v -> {
            dialogo.dismiss();
            alFijar.run();
        });
        btnEditar.setOnClickListener(v -> {
            dialogo.dismiss();
            alEditar.run();
        });
        btnEliminar.setOnClickListener(v -> {
            dialogo.dismiss();
            alEliminar.run();
        });
        tvCancelar.setOnClickListener(v -> dialogo.dismiss());

        dialogo.show();
    }
}
