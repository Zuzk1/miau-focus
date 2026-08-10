package com.peggos.pomodoro;

public class Tarea {
    public String nombre;
    public boolean completada;
    public long diaCompletada;

    public Tarea(String nombre, boolean completada, long diaCompletada) {
        this.nombre = nombre;
        this.completada = completada;
        this.diaCompletada = diaCompletada;
    }
}
