/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

import GameLogic.Map;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author jsero
 */
public class ClienteJuego {

    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private Map mapa;
    private String nombre;
    private boolean conectado = false;

    // Constructor modificado para recibir el nombre
    public ClienteJuego(Map mapa, String nombre) {
        this.mapa = mapa;
        this.nombre = nombre;
    }

    public void conectar(String host, int port) {
        try {
            socket = new Socket(host, port);
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            // Enviar nombre al servidor
            salida.writeUTF(nombre);

            // 1. Primero cargar el mapa desde el servidor
            cargarMapaDesdeServidor();

            // 2. Luego iniciar el hilo para recibir actualizaciones continuas
            new Thread(this::recibirActualizaciones).start();

        } catch (IOException ex) {
            System.out.println("Error al conectar: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error al conectar al servidor", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recibirDatos() {
        try {
            while (true) {
                String tipo = entrada.readUTF();

                switch (tipo) {
                    case "POSICION_INICIAL":
                        int x = entrada.readInt();
                        int y = entrada.readInt();
                        SwingUtilities.invokeLater(() -> {
                            mapa.getManager().getPersonaje().setX(x);
                            mapa.getManager().getPersonaje().setY(y);
                        });
                        break;

                    case "MAPA_INICIAL":
                        int ancho = entrada.readInt();
                        int alto = entrada.readInt();
                        int[][] mapaServidor = new int[alto][ancho];
                        for (int i = 0; i < alto; i++) {
                            for (int j = 0; j < ancho; j++) {
                                mapaServidor[j][i] = entrada.readInt();
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            mapa.setMapa(mapaServidor);
                        });
                        break;

                    case "JUGADOR":
                        String nombreJugador = entrada.readUTF();
                        int jugadorX = entrada.readInt();
                        int jugadorY = entrada.readInt();
                        SwingUtilities.invokeLater(() -> {
                            mapa.getManager().actualizarPosicionJugador(nombreJugador, jugadorX, jugadorY);
                        });
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Desconectado del servidor");
        }
    }

    public void enviarMovimiento(int x, int y) {
        if (conectado) {
            try {
                salida.writeInt(x);
                salida.writeInt(y);
            } catch (IOException ex) {
                System.out.println("Error al enviar movimiento: " + ex.getMessage());
                conectado = false;
            }
        }
    }

    private void cargarMapaDesdeServidor() throws IOException {
        // 1. Recibir dimensiones
        int ancho = entrada.readInt();
        int alto = entrada.readInt();

        // Crear mapa vacío
        int[][] mapaServidor = new int[alto][ancho]; // Inicializa todos a 0

        // 2. Recibir solo tiles no vacíos
        int nonEmptyTiles = entrada.readInt();
        for (int i = 0; i < nonEmptyTiles; i++) {
            int y = entrada.readInt();
            int x = entrada.readInt();
            int valor = entrada.readInt();
            mapaServidor[y][x] = valor;
        }

        // Actualizar mapa en el hilo de UI
        SwingUtilities.invokeLater(() -> {
            mapa.setMapa(mapaServidor);
        });

        // 3. Recibir jugadores existentes
        int numJugadores = entrada.readInt();
        for (int i = 0; i < numJugadores; i++) {
            String nombre = entrada.readUTF();
            int x = entrada.readInt();
            int y = entrada.readInt();
            mapa.getManager().actualizarPosicionJugador(nombre, x, y);
        }

        // Actualizar mapa en el hilo de UI
        SwingUtilities.invokeLater(() -> {
            mapa.setMapa(mapaServidor);
        });
    }

    private void recibirActualizaciones() {
        try {
            while (true) {
                String tipo = entrada.readUTF();

                if (tipo.equals("POSICION")) {
                    String nombreJugador = entrada.readUTF();
                    int x = entrada.readInt();
                    int y = entrada.readInt();

                    SwingUtilities.invokeLater(() -> {
                        if (!nombreJugador.equals(nombre)) {
                            mapa.getManager().actualizarPosicionJugador(nombreJugador, x, y);
                        }
                    });
                }
                // Puedes añadir más tipos de mensajes aquí
            }
        } catch (IOException ex) {
            System.out.println("Desconectado del servidor");
        }
    }
}
