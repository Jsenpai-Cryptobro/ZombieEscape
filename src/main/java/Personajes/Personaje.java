/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

/**
 *
 * @author jsero
 */
import java.awt.Color;
import java.awt.Graphics;

public class Personaje {
    private int x, y;
    private Color color;

    public Personaje(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    //GETTERS Y SETTERS
    public int getX(){ 
        return x;
    }
    
    public int getY(){ 
        return y; 
    }
    
    public void setX(int x){
        this.x = x; 
    }
    
    public void setY(int y){
        this.y = y; 
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    //Dibujar el personaje
    public void dibujar(Graphics g, int pantallaX, int pantallaY) {
        g.setColor(color);
        g.fillOval(pantallaX + 8, pantallaY + 8, 16, 16);
    }
}
