package com.cecyt.pomodoro;

import android.content.Context;
import android.util.TypedValue;

public class TemaUtils {

    public static int resolverColor(Context context, int attrResId) {
        TypedValue valor = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, valor, true);
        return valor.data;
    }
}
