package com.peggos.pomodoro;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class acercaDeActivity extends BaseActivity {

    private static final String URL_KOFI = "https://ko-fi.com/zuzk1";
    private static final String URL_PRIVACIDAD =
            "https://sites.google.com/view/miau-focus-privacidad/p%C3%A1gina-principal";
    private static final String CORREO_CONTACTO = "peggosteam@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acerca_de);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.ivAtras).setOnClickListener(v -> finish());

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText(obtenerVersion());

        findViewById(R.id.filaCorreo).setOnClickListener(v -> abrirCorreo());
        findViewById(R.id.filaPrivacidad).setOnClickListener(v -> abrirUrl(URL_PRIVACIDAD));
        findViewById(R.id.filaLicencias).setOnClickListener(v -> mostrarLicencias());
        findViewById(R.id.btnApoyar).setOnClickListener(v -> abrirUrl(URL_KOFI));
    }

    private String obtenerVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return getString(R.string.acerca_version, info.versionName);
        } catch (Exception e) {
            return "";
        }
    }

    private void abrirUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_no_abrir_enlace, Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirCorreo() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + CORREO_CONTACTO));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_sin_app_correo, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarLicencias() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.acerca_licencias)
                .setMessage(R.string.licencias_texto)
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
