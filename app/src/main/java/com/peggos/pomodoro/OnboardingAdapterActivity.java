package com.peggos.pomodoro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;

public class OnboardingAdapterActivity extends RecyclerView.Adapter<OnboardingAdapterActivity.OnboardingViewHolder> {

    private final int[] titulos = {
            R.string.onboarding_titulo_1,
            R.string.onboarding_titulo_2,
            R.string.onboarding_titulo_3
    };

    private final int[] descripciones = {
            R.string.onboarding_texto_1,
            R.string.onboarding_texto_2,
            R.string.onboarding_texto_3
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