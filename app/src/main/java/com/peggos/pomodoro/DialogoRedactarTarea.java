package com.peggos.pomodoro;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.function.Consumer;

public class DialogoRedactarTarea {

    public static void mostrar(Context contexto, String titulo, String textoInicial, Consumer<String> alGuardar) {
        Dialog dialogo = new Dialog(contexto);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_redactar_tarea);

        Window ventana = dialogo.getWindow();
        if (ventana != null) {
            ventana.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            ventana.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        TextView tvTitulo = dialogo.findViewById(R.id.tvTituloRedactarTarea);
        EditText etTarea = dialogo.findViewById(R.id.etRedactarTareaInput);
        MaterialButton btnGuardar = dialogo.findViewById(R.id.btnGuardarRedactarTarea);
        TextView tvCancelar = dialogo.findViewById(R.id.tvCancelarRedactarTarea);

        tvTitulo.setText(titulo);
        etTarea.setText(textoInicial);

        btnGuardar.setOnClickListener(v -> {
            String texto = etTarea.getText().toString().trim();
            if (texto.isEmpty()) {
                Toast.makeText(contexto, "Escribe una tarea", Toast.LENGTH_SHORT).show();
                return;
            }
            dialogo.dismiss();
            alGuardar.accept(texto);
        });
        tvCancelar.setOnClickListener(v -> dialogo.dismiss());

        dialogo.show();
        etTarea.requestFocus();
        if (textoInicial != null && !textoInicial.isEmpty()) {
            etTarea.setSelection(etTarea.getText().length());
        }
    }
}
