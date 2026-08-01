package com.peggos.pomodoro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;

public class OnboardingAdapterActivity extends RecyclerView.Adapter<OnboardingAdapterActivity.OnboardingViewHolder> {

    private final String[] titulos = {
            "¡Bienvenido a\nMiau Focus!",
            "Sin distracciones,\nsin excusas",
            "Tu gato te vigila,\nno falles"
    };

    private final String[] descripciones = {
            "Tu compañero felino te ayudará a mantenerte enfocado con la técnica Pomodoro. ¡Desliza para descubrir cómo!",
            "Si sales de la app durante una sesión, el cronómetro se pausa y, si te tardas en volver, pierdes puntos. Mantente enfocado.",
            "Completa ciclos, gana puntos y desbloquea temas. Cada sesión completada fortalece tu racha."
    };

    private final int[] animaciones = {
            R.raw.gatos_rueda,
            R.raw.gato_descansa,
            R.raw.gato_observa
    };

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.tvHeadline.setText(titulos[position]);
        holder.tvDescripcion.setText(descripciones[position]);
        holder.lottie.setAnimation(animaciones[position]);
        holder.lottie.playAnimation();
    }

    @Override
    public int getItemCount() {
        return titulos.length;
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeadline, tvDescripcion;
        LottieAnimationView lottie;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeadline = itemView.findViewById(R.id.tvHeadline);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            lottie = itemView.findViewById(R.id.lottieOnboarding);
        }
    }
}