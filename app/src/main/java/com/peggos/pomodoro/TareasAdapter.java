package com.peggos.pomodoro;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private ArrayList<Tarea> listaTareas;
    private OnTareaClickListener clickListener;
    private OnTareaToggleListener toggleListener;


    public interface OnTareaClickListener {
        void onTareaClick(int posicion);
    }

    public interface OnTareaToggleListener {
        void onTareaToggle(int posicion);
    }


    public TareasAdapter(ArrayList<Tarea> listaTareas, OnTareaClickListener clickListener,
                         OnTareaToggleListener toggleListener) {
        this.listaTareas = listaTareas;
        this.clickListener = clickListener;
        this.toggleListener = toggleListener;
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int posicion) {
        Tarea tarea = listaTareas.get(posicion);
        holder.tvNombreTarea.setText(tarea.nombre);

        if (tarea.completada) {
            holder.ivCirculoTarea.setBackgroundResource(R.drawable.fondo_circulo_completado);
            holder.ivPalomita.setVisibility(View.VISIBLE);
            holder.tvNombreTarea.setPaintFlags(
                    holder.tvNombreTarea.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.contenedorTarea.setAlpha(0.45f);
        } else {
            holder.ivCirculoTarea.setBackgroundResource(R.drawable.fondo_circulo_pendiente);
            holder.ivPalomita.setVisibility(View.GONE);
            holder.tvNombreTarea.setPaintFlags(
                    holder.tvNombreTarea.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.contenedorTarea.setAlpha(1f);
        }

        holder.contenedorCirculo.setOnClickListener(v -> {
            if (toggleListener != null) {
                toggleListener.onTareaToggle(holder.getAdapterPosition());
            }
        });

        holder.ivOpcionesTarea.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTareaClick(holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTareaClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaTareas.size();
    }


    static class TareaViewHolder extends RecyclerView.ViewHolder {
        View contenedorTarea;
        View contenedorCirculo;
        ImageView ivCirculoTarea;
        ImageView ivPalomita;
        TextView tvNombreTarea;
        ImageView ivOpcionesTarea;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            contenedorTarea = itemView.findViewById(R.id.contenedorTarea);
            contenedorCirculo = itemView.findViewById(R.id.contenedorCirculo);
            ivCirculoTarea = itemView.findViewById(R.id.ivCirculoTarea);
            ivPalomita = itemView.findViewById(R.id.ivPalomita);
            tvNombreTarea = itemView.findViewById(R.id.tvNombreTarea);
            ivOpcionesTarea = itemView.findViewById(R.id.ivOpcionesTarea);
        }
    }
}
