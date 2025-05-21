/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

import java.awt.Color;

/**
 *
 * @author jsero
 */
public class JugadorRemoto extends Personaje {

    private String nombre;

    public JugadorRemoto(int x, int y, Color color, String nombre) {
        super(x, y, color);
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
