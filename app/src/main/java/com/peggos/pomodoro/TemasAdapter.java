package com.peggos.pomodoro;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

public class TemasAdapter extends RecyclerView.Adapter<TemasAdapter.TemaViewHolder> {

    public interface OnTemaSeleccionadoListener {
        void onTemaCambiado();
        void onTemaComprado(int precio);
    }

    private final Context context;
    private final GestorTemas gestorTemas;
    private final GestorEstadisticas gestorEstadisticas;
    private final OnTemaSeleccionadoListener listener;
    private final GestorTemas.Tema[] temas = GestorTemas.Tema.values();

    public TemasAdapter(Context context, OnTemaSeleccionadoListener listener) {
        this.context = context;
        this.gestorTemas = new GestorTemas(context);
        this.gestorEstadisticas = new GestorEstadisticas(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TemaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(context).inflate(R.layout.item_tema, parent, false);
        return new TemaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull TemaViewHolder holder, int position) {
        GestorTemas.Tema tema = temas[position];

        holder.viewFondoPreview.setBackgroundColor(context.getColor(tema.colorFondoPreview));
        holder.viewAcentoPreview.setBackgroundColor(context.getColor(tema.colorAcentoPreview));
        holder.tvNombreTema.setText(tema.getNombre(holder.itemView.getContext()));

        boolean esActual = tema == gestorTemas.getTemaActual();
        boolean desbloqueado = gestorTemas.estaDesbloqueado(tema);

        if (esActual) {
            holder.tvEstadoTema.setText(R.string.tema_seleccionado);
            holder.cardTema.setStrokeColor(TemaUtils.resolverColor(context, R.attr.themeAcento));
        } else if (desbloqueado) {
            holder.tvEstadoTema.setText(R.string.tema_tocar_aplicar);
            holder.cardTema.setStrokeColor(context.getColor(android.R.color.transparent));
        } else {
            holder.tvEstadoTema.setText(holder.itemView.getResources().getQuantityString(R.plurals.tema_precio, tema.precio, tema.precio));
            holder.cardTema.setStrokeColor(context.getColor(android.R.color.transparent));
        }

        holder.itemView.setOnClickListener(v -> {
            if (esActual) return;

            if (desbloqueado) {
                gestorTemas.establecerTemaActual(tema);
                listener.onTemaCambiado();
            } else if (gestorEstadisticas.gastarPuntos(tema.precio)) {
                gestorTemas.desbloquear(tema);
                gestorTemas.establecerTemaActual(tema);
                animarCompra(holder.cardTema, () -> listener.onTemaComprado(tema.precio));
            } else {
                animarFondosInsuficientes(holder.cardTema);
            }
        });
    }

    private void animarCompra(MaterialCardView card, Runnable alTerminar) {
        int colorVerde = ContextCompat.getColor(context, R.color.color_verde_exito);
        card.setStrokeColor(colorVerde);
        card.animate().scaleX(1.09f).scaleY(1.09f).setDuration(130).withEndAction(() ->
                card.animate().scaleX(1f).scaleY(1f).setDuration(220)
                        .withEndAction(alTerminar::run).start()
        ).start();
    }

    private void animarFondosInsuficientes(MaterialCardView card) {
        int colorRojo = ContextCompat.getColor(context, R.color.color_rojo_peligro);
        card.setStrokeColor(colorRojo);
        card.animate().translationX(14f).setDuration(60).withEndAction(() ->
            card.animate().translationX(-14f).setDuration(60).withEndAction(() ->
                card.animate().translationX(10f).setDuration(60).withEndAction(() ->
                    card.animate().translationX(-10f).setDuration(60).withEndAction(() ->
                        card.animate().translationX(0f).setDuration(60).withEndAction(() -> {
                            ValueAnimator fade = new ValueAnimator();
                            fade.setIntValues(colorRojo, Color.TRANSPARENT);
                            fade.setEvaluator(new ArgbEvaluator());
                            fade.setDuration(500);
                            fade.addUpdateListener(a -> card.setStrokeColor((int) a.getAnimatedValue()));
                            fade.start();
                        }).start()
                    ).start()
                ).start()
            ).start()
        ).start();
    }

    @Override
    public int getItemCount() {
        return temas.length;
    }

    static class TemaViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTema;
        View viewFondoPreview;
        View viewAcentoPreview;
        TextView tvNombreTema;
        TextView tvEstadoTema;

        TemaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTema = itemView.findViewById(R.id.cardTema);
            viewFondoPreview = itemView.findViewById(R.id.viewFondoPreview);
            viewAcentoPreview = itemView.findViewById(R.id.viewAcentoPreview);
            tvNombreTema = itemView.findViewById(R.id.tvNombreTema);
            tvEstadoTema = itemView.findViewById(R.id.tvEstadoTema);
        }
    }
}
