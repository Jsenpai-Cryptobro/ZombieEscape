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
            JFrame ventana = new JFrame("Zombie Escape");
            Map panelMapa = new Map();
            
            // Pedir nombre
            String nombre = JOptionPane.showInputDialog("Ingresa tu nombre:");
            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = "Jugador" + System.currentTimeMillis() % 1000;
            }
            
            // Crear cliente
            ClienteJuego cliente = new ClienteJuego(panelMapa, nombre);
            
            // Configurar controlador
            Controller controller = new Controller(panelMapa, cliente);
            panelMapa.setController(controller);
            
            // Conectar al servidor
            new Thread(() -> {
                cliente.conectar("localhost", 8085);
            }).start();
            
            // Configurar ventana
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.add(panelMapa);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
            panelMapa.requestFocusInWindow();
        });
    }
}
