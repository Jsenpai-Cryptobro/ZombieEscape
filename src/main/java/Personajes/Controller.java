/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

import GameLogic.Map;
import GameLogic.Map;
import Cliente.ClienteJuego;
import GameLogic.PlayerState;
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
        if (cliente.getEstado() == PlayerState.MUERTO) {
            return;
        }
        
        if (map != null && map.getManager().getPersonaje() != null) {
            Personaje personaje = map.getManager().getPersonaje();
            int newX = personaje.getX();
            int newY = personaje.getY();

            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                    newY--;
                    break;
                case KeyEvent.VK_S:
                    newY++;
                    break;
                case KeyEvent.VK_A:
                    newX--;
                    break;
                case KeyEvent.VK_D:
                    newX++;
                    break;
            }

            if (esMovimientoValido(newX, newY)) {
                personaje.setX(newX);
                personaje.setY(newY);
                map.repaint();

                // Send movement to server
                if (cliente != null) {
                    cliente.enviarMovimiento(newX, newY);
                }
            }
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
