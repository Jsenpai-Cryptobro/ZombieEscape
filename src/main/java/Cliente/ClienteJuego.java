/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

import GUI.WaitingRoom;
import GameLogic.Map;
import GameLogic.PlayerState;
import Personajes.Controller;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
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
    private PlayerState estado = PlayerState.IN_WAITING_ROOM;
    private WaitingRoom waitingRoom;
    private JFrame gameFrame;
    private JFrame waitingFrame;
    private boolean isAdmin = false;

    public ClienteJuego(String nombre) {
        this.nombre = nombre;
    }

    public void conectar(String host, int port) {
        try {
            socket = new Socket(host, port);
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            // Enviar nombre al servidor
            salida.writeUTF(nombre);

            // Receive admin status
            isAdmin = entrada.readBoolean();

            // Show waiting room
            mostrarWaitingRoom();

            // Start receiving updates
            new Thread(this::recibirActualizaciones).start();

        } catch (IOException ex) {
            System.out.println("Error al conectar: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error al conectar al servidor", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarWaitingRoom() {
        SwingUtilities.invokeLater(() -> {
            waitingFrame = new JFrame("Waiting Room");
            waitingRoom = new WaitingRoom(isAdmin, this);
            waitingFrame.add(waitingRoom);
            waitingFrame.pack();
            waitingFrame.setLocationRelativeTo(null);
            waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            waitingFrame.setVisible(true);
        });
    }

    public void enviarInicioJuego() {
        if (isAdmin && estado == PlayerState.IN_WAITING_ROOM) {
            try {
                salida.writeUTF("START_GAME");
            } catch (IOException ex) {
                System.out.println("Error al enviar inicio de juego: " + ex.getMessage());
            }
        }
    }

    private void handleGameStart() {
        try {
            iniciarJuego();
            cargarMapaDesdeServidor();
            estado = PlayerState.IN_GAME;
            conectado = true;
        } catch (IOException e) {
            System.err.println("Error starting game: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Error starting game: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
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
        if (conectado && estado == PlayerState.IN_GAME) {
            try {
                salida.writeUTF("MOVIMIENTO");
                salida.writeInt(x);
                salida.writeInt(y);
                salida.flush();
            } catch (IOException ex) {
                System.out.println("Error al enviar movimiento: " + ex.getMessage());
                conectado = false;
            }
        }
    }

    private void iniciarJuego() {
        SwingUtilities.invokeLater(() -> {
            // Create game frame
            gameFrame = new JFrame("Zombie Escape - " + nombre);
            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create map
            mapa = new Map();

            // Create controller
            Controller controller = new Controller(mapa, this);
            mapa.setController(controller);

            // Configure frame
            gameFrame.add(mapa);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);

            // Hide waiting room and show game
            if (waitingFrame != null) {
                waitingFrame.dispose();
            }
            gameFrame.setVisible(true);
            mapa.requestFocusInWindow();
        });
    }

    private void cargarMapaDesdeServidor() throws IOException {
        // 1. Receive dimensions with validation
        int ancho = entrada.readInt();
        int alto = entrada.readInt();

        // Add size validation
        if (ancho <= 0 || alto <= 0 || ancho > 1000 || alto > 1000) {
            throw new IOException("Invalid map dimensions received: " + ancho + "x" + alto);
        }

        try {
            // Create map with validated dimensions
            int[][] mapaServidor = new int[alto][ancho];

            // 2. Receive only non-empty tiles
            int nonEmptyTiles = entrada.readInt();

            // Validate number of non-empty tiles
            if (nonEmptyTiles < 0 || nonEmptyTiles > (alto * ancho)) {
                throw new IOException("Invalid number of non-empty tiles: " + nonEmptyTiles);
            }

            // Read tile data with bounds checking
            for (int i = 0; i < nonEmptyTiles; i++) {
                int y = entrada.readInt();
                int x = entrada.readInt();
                int valor = entrada.readInt();

                // Validate coordinates
                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    mapaServidor[y][x] = valor;
                } else {
                    System.err.println("Invalid tile coordinates received: " + x + "," + y);
                }
            }

            // Update map in UI thread
            SwingUtilities.invokeLater(() -> {
                if (mapa != null) {
                    mapa.setMapa(mapaServidor);
                    // Ensure map has focus for keyboard input
                    mapa.requestFocusInWindow();
                }
            });

            // 3. Receive existing players
            int numJugadores = entrada.readInt();
            if (numJugadores < 0 || numJugadores > 100) { // reasonable limit for players
                throw new IOException("Invalid number of players: " + numJugadores);
            }

            for (int i = 0; i < numJugadores; i++) {
                String nombre = entrada.readUTF();
                int x = entrada.readInt();
                int y = entrada.readInt();
                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    SwingUtilities.invokeLater(() -> {
                        mapa.getManager().actualizarPosicionJugador(nombre, x, y);
                    });
                }
            }
        } catch (OutOfMemoryError e) {
            throw new IOException("Map too large to load: " + e.getMessage());
        }
    }

    private void recibirActualizaciones() {
        try {
            while (true) {
                String tipo = entrada.readUTF();

                switch (tipo) {
                    case "PLAYER_LIST":
                        int numPlayers = entrada.readInt();
                        List<String> players = new ArrayList<>();
                        for (int i = 0; i < numPlayers; i++) {
                            players.add(entrada.readUTF());
                        }
                        if (waitingRoom != null) {
                            waitingRoom.updatePlayerList(players);
                        }
                        break;

                    case "GAME_START":
                        handleGameStart();
                        break;

                    case "PLAYER_READY":
                        String readyPlayer = entrada.readUTF();
                        if (waitingRoom != null) {
                            waitingRoom.setPlayerReady(readyPlayer);
                        }
                        break;

                    case "POSICION":
                        if (estado == PlayerState.IN_GAME) {
                            String nombreJugador = entrada.readUTF();
                            int x = entrada.readInt();
                            int y = entrada.readInt();
                            SwingUtilities.invokeLater(() -> {
                                if (!nombreJugador.equals(nombre)) {
                                    mapa.getManager().actualizarPosicionJugador(nombreJugador, x, y);
                                }
                            });
                        }
                        break;

                    case "JUGADOR_DESCONECTADO":
                        String nombreDesconectado = entrada.readUTF();
                        SwingUtilities.invokeLater(() -> {
                            mapa.getManager().removerJugador(nombreDesconectado);
                        });
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Desconectado del servidor");
        }
    }
}
