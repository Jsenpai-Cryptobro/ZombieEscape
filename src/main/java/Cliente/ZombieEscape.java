/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package Cliente;

import GameLogic.Map;
import Personajes.Controller;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author jsero
 */
public class ZombieEscape {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Pedir nombre
            String nombre = JOptionPane.showInputDialog("Ingresa tu nombre:");
            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = "Jugador" + System.currentTimeMillis() % 1000;
            }

            // Crear cliente y conectar al servidor
            ClienteJuego cliente = new ClienteJuego(nombre);
            new Thread(() -> {
                cliente.conectar("localhost", 8085);
            }).start();
        });
    }
}
