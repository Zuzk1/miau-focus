package com.peggos.pomodoro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private ArrayList<String> listaTareas;
    private OnTareaClickListener clickListener;


    public interface OnTareaClickListener {
        void onTareaClick(int posicion);
    }


    public TareasAdapter(ArrayList<String> listaTareas, OnTareaClickListener clickListener) {
        this.listaTareas = listaTareas;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int posicion) {
        String tarea = listaTareas.get(posicion);
        holder.tvTextoTarea.setText(tarea);


        holder.tvTextoTarea.setTextColor(TemaUtils.resolverColor(holder.itemView.getContext(), R.attr.themeTextoPrimario));
        holder.tvTextoTarea.setPadding(40, 45, 40, 45);


        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTareaClick(posicion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaTareas.size();
    }


    static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTextoTarea;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTextoTarea = itemView.findViewById(android.R.id.text1);
        }
    }
}