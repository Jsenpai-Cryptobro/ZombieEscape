/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

import GameLogic.Map;
import GameLogic.Map;
import Cliente.ClienteJuego;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author jsero
 */
public class Controller implements KeyListener {

    private Map map;
    private ClienteJuego cliente;

    public Controller(Map map, ClienteJuego cliente) {
        this.map = map;
        this.cliente = cliente;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Personaje personaje = map.getManager().getPersonaje();
        int nuevoX = personaje.getX();
        int nuevoY = personaje.getY();

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W ->
                nuevoY--;
            case KeyEvent.VK_S ->
                nuevoY++;
            case KeyEvent.VK_A ->
                nuevoX--;
            case KeyEvent.VK_D ->
                nuevoX++;
        }

        // Verificar límites del mapa y obstáculos
        if (esMovimientoValido(nuevoX, nuevoY)) {
            personaje.setX(nuevoX);
            personaje.setY(nuevoY);

            if (cliente != null) {
                cliente.enviarMovimiento(nuevoX, nuevoY);
            }

            map.repaint();
        }
    }

    private boolean esMovimientoValido(int x, int y) {
        // Verify coordinates are within map bounds
        if (x < 0 || x >= map.getMAP_WIDTH() || y < 0 || y >= map.getMAP_HEIGHT()) {
            return false;
        }

        // Check if it's not an obstacle (tile value 1)
        return map.getTileAt(x, y) != 1;
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
