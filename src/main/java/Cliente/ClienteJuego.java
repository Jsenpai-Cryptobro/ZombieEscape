/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

import GUI.WaitingRoom;
import GameLogic.Map;
import GameLogic.PlayerState;
import Personajes.Controller;
import Personajes.Personaje;
import Personajes.Zombie;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
    private final List<Zombie> zombiesSincronizados = new ArrayList<>();

    private JTextArea chatArea;

    public ClienteJuego(String nombre) {
        this.nombre = nombre;
    }

    //GETTERS Y SETTERS
    public Map getMapa() {
        return mapa;
    }

    public PlayerState getEstado() {
        return estado;
    }

    public void setEstado(PlayerState estado) {
        this.estado = estado;
    }

    //METODOS
    //Conecta el cliente al servidor
    public void conectar(String host, int port) {
        try {
            socket = new Socket(host, port);
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            //Enviar nombre al servidor
            salida.writeUTF(nombre);

            //Lo hace admin
            isAdmin = entrada.readBoolean();

            //Muestra la sala de espera
            mostrarWaitingRoom();

            //Thread para que reciba actualizaciones 
            new Thread(this::recibirActualizaciones).start();

        } catch (IOException ex) {
            System.out.println("Error al conectar: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error al conectar al servidor", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Crea el frame con la sala de espera
    public void mostrarWaitingRoom() {
        SwingUtilities.invokeLater(() -> {
            waitingFrame = new JFrame("Sala de espera");
            waitingRoom = new WaitingRoom(isAdmin, this);
            waitingFrame.add(waitingRoom);
            waitingFrame.pack();
            waitingFrame.setLocationRelativeTo(null);
            waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            waitingFrame.setVisible(true);
        });
    }

    //Manda el dato necesario para que cuando el server lo reciba inicie el juego
    //(Agregar que minimos sean dos jugadores para poder iniciar)
    public void enviarInicioJuego() {
        if (isAdmin && estado == PlayerState.IN_WAITING_ROOM) {
            try {
                salida.writeUTF("START_GAME");
                salida.writeUTF(waitingRoom.getMapaSeleccionado());
            } catch (IOException ex) {
                System.out.println("Error al enviar inicio de juego: " + ex.getMessage());
            }
        }
    }

    //Ayuda a hacer todo lo necesario para que el juego inicie
    public void handleInicioJuego() {
        try {
            iniciarJuego();
            cargarMapaDesdeServidor();
            estado = PlayerState.IN_GAME;
            conectado = true;
        } catch (IOException e) {
            /*System.err.println("Error starting game: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Error starting game: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            */
        }
    }

    //Envia el movimiento de este cliente
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

    //Crea cosas necesarias para poder crear el juego, frames, controllers, y cierra sala de espera
    public void iniciarJuego() {
        if (gameFrame != null) {
            gameFrame.dispose();
        }

        SwingUtilities.invokeLater(() -> {
            //Modificar tamaño de la ventana a gusto
            final int FRAME_WIDTH = 346;
            final int FRAME_HEIGHT = 480;
            final int MAPA_HEIGHT = 600;
            final int CHAT_HEIGHT = 120;

            // Crear el frame
            gameFrame = new JFrame("Zombie Escape - " + nombre);
            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameFrame.setResizable(false);

            // Panel principal con BorderLayout
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

            // === MAPA ===
            mapa = new Map();
            mapa.setZombiesSincronizados(zombiesSincronizados);
            Controller controller = new Controller(mapa, this);
            mapa.setController(controller);

            // IMPORTANTE: dejar que el Map pinte su tamaño natural, sin envolverlo
            JScrollPane mapaScroll = new JScrollPane(mapa);
            mapaScroll.setPreferredSize(new Dimension(FRAME_WIDTH, MAPA_HEIGHT));
            mapaScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            mapaScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            mainPanel.add(mapaScroll, BorderLayout.CENTER);

            // === CHAT ===
            JPanel chatPanel = new JPanel(new BorderLayout());

            chatArea = new JTextArea(5, 30);
            chatArea.setEditable(false);
            JScrollPane chatScroll = new JScrollPane(chatArea);
            chatPanel.add(chatScroll, BorderLayout.CENTER);
            chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            JTextField chatInput = new JTextField();
            JButton sendButton = new JButton("Enviar");

            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(chatInput, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);
            chatPanel.add(inputPanel, BorderLayout.SOUTH);

            chatPanel.setPreferredSize(new Dimension(FRAME_WIDTH, CHAT_HEIGHT));
            mainPanel.add(chatPanel, BorderLayout.SOUTH);

            // Agregar al frame
            gameFrame.setContentPane(mainPanel);
            gameFrame.pack(); // asegura layout sin que se reduzca el frame
            gameFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
            gameFrame.setResizable(false);
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setVisible(true);

            // Cerrar sala de espera si existe
            if (waitingFrame != null && waitingFrame.isDisplayable()) {
                waitingFrame.setVisible(false);
                waitingFrame.dispose();
                waitingFrame = null;
            }

            mapa.requestFocusInWindow();

            // Listener para enviar mensajes
            sendButton.addActionListener(e -> {
                String msg = chatInput.getText().trim();
                if (!msg.isEmpty()) {
                    try {
                        salida.writeUTF("CHAT");
                        salida.writeUTF(nombre + ": " + msg);
                        chatInput.setText("");
                    } catch (IOException ex) {
                        chatArea.append("Error al enviar mensaje.\n");
                    }
                }
                mapa.requestFocusInWindow();
            });

            chatInput.addActionListener(sendButton.getActionListeners()[0]);
        });
    }

    //Carga y crea el mapa que recibe del server
    public void cargarMapaDesdeServidor() throws IOException {
        //Recibe las dimensiones del mapa y las valida
        int ancho = entrada.readInt();
        int alto = entrada.readInt();

        if (ancho <= 0 || alto <= 0 || ancho > 1000 || alto > 1000) {
            throw new IOException("Invalid map dimensions received: " + ancho + "x" + alto);
        }

        try {
            int[][] mapaServidor = new int[alto][ancho];
            int nonEmptyTiles = entrada.readInt();

            if (nonEmptyTiles < 0 || nonEmptyTiles > (alto * ancho)) {
                throw new IOException("Numero invalido de casillas no vacias: " + nonEmptyTiles);
            }

            for (int i = 0; i < nonEmptyTiles; i++) {
                int y = entrada.readInt();
                int x = entrada.readInt();
                int valor = entrada.readInt();

                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    mapaServidor[y][x] = valor;
                } else {
                    System.err.println("Coordenadas invalidas: " + x + "," + y);
                }
            }

            //Actualiza UI
            SwingUtilities.invokeLater(() -> {
                if (mapa != null) {
                    mapa.setMapa(mapaServidor);
                    if (estado == PlayerState.MUERTO) {
                        mapa.setModoEspectador(true);
                    }
                    mapa.requestFocusInWindow();

                    if (gameFrame != null) {
                        gameFrame.setLocationRelativeTo(null);
                    }
                }
            });

            //Recibe los jugadores
            int numJugadores = entrada.readInt();
            if (numJugadores < 1 || numJugadores > 10) {
                throw new IOException("Invalid number of players: " + numJugadores);
            }

            for (int i = 0; i < numJugadores; i++) {
                String nombreJugador = entrada.readUTF();
                int x = entrada.readInt();
                int y = entrada.readInt();

                SwingUtilities.invokeLater(() -> {
                    // Actualizar para todos los jugadores, incluyendo el local
                    if (nombreJugador.equals(nombre)) {
                        // Solo crear personaje local si no existe
                        if (mapa.getManager().getPersonaje() == null) {
                            mapa.getManager().setPersonaje(new Personaje(x, y, Color.BLUE));
                        }
                    } else {
                        mapa.getManager().actualizarPosicionJugador(nombreJugador, x, y);
                    }
                });
            }

        } catch (OutOfMemoryError e) {
            throw new IOException("Map too large to load: " + e.getMessage());
        }
    }

    //Hace los cambios necesarios en la pantalla de cada jugador segun sea lo indicado
    public void recibirActualizaciones() {
        try {
            while (true) {
                //Verificar si no se cae por algun motivo(El modo espectador me esta dejando de funcionar sepa dios por que)
                long lastPrint = System.currentTimeMillis();
                if (System.currentTimeMillis() - lastPrint > 5000) {
                    System.out.println("Cliente " + nombre + " sigue activo. Estado: " + estado);
                    lastPrint = System.currentTimeMillis();
                }

                String tipo = entrada.readUTF();

                switch (tipo) {
                    case "PLAYER_LIST": //Actualiza la lista de jugadores (se cambia en la sala de espera)
                        int numPlayers = entrada.readInt();
                        List<String> players = new ArrayList<>();
                        for (int i = 0; i < numPlayers; i++) {
                            players.add(entrada.readUTF());
                        }
                        if (waitingRoom != null) {
                            waitingRoom.updatePlayerList(players);
                        }
                        break;

                    case "GAME_START": //Inicia el juego
                        handleInicioJuego();
                        break;

                    case "POSICION": //Actualiza la posicion de los jugadores
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

                    case "JUGADOR_DESCONECTADO": //Avisa si un jugador se desconecto y lo remueve del mapa
                        String nombreDesconectado = entrada.readUTF();
                        SwingUtilities.invokeLater(() -> {
                            mapa.getManager().removerJugador(nombreDesconectado);
                        });
                        break;

                    case "ZOMBIE_INICIAL": //Pone los zombies en el mapa en su posicion inicial
                        int cantidadZombies = entrada.readInt();
                        zombiesSincronizados.clear();
                        for (int i = 0; i < cantidadZombies; i++) {
                            int zx = entrada.readInt();
                            int zy = entrada.readInt();
                            int direccion = entrada.readInt();
                            Zombie zombie = new Zombie(zx, zy, Color.GREEN);
                            zombie.setDireccion(direccion);
                            zombiesSincronizados.add(zombie);
                            System.out.println("Zombies recibidos: " + cantidadZombies);
                        }
                        SwingUtilities.invokeLater(() -> mapa.setZombiesSincronizados(zombiesSincronizados));
                        break;

                    case "ZOMBIE_UPDATE": //Actualiza posicion zombies y su campo de vision
                        int totalZombies = entrada.readInt();
                        System.out.println("Cliente " + nombre + " recibió ZOMBIE_UPDATE (" + zombiesSincronizados.size() + " zombies)");

                        for (int i = 0; i < totalZombies; i++) {
                            int x = entrada.readInt();
                            int y = entrada.readInt();
                            int dir = entrada.readInt();
                            if (i < zombiesSincronizados.size()) {
                                Zombie z = zombiesSincronizados.get(i);
                                z.setX(x);
                                z.setY(y);
                                z.setDireccion(dir);
                            }
                        }
                        SwingUtilities.invokeLater(() -> mapa.repaint());
                        break;

                    case "MUERTO": //Si un jugador muere se reinicia el  nivel y el que murio se vuelve espectador
                        estado = PlayerState.MUERTO;
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, "¡Has sido atrapado por un zombie!", "Derrota", JOptionPane.WARNING_MESSAGE);
                            mapa.setModoEspectador(true);
                            mapa.repaint();
                        });
                        break;

                    case "VOLVER_A_SALA": //Si todos mueren vuelve a sala de espera
                        estado = PlayerState.IN_WAITING_ROOM;

                        SwingUtilities.invokeLater(() -> {
                            if (gameFrame != null) {
                                gameFrame.dispose();
                            }

                            mostrarWaitingRoom();
                        });
                        break;

                    case "CHAT": //Mensajes
                        String mensaje = entrada.readUTF();
                        SwingUtilities.invokeLater(() -> {
                            chatArea.append(mensaje + "\n");
                        });
                        break;

                }
            }
        } catch (IOException ex) {
            System.out.println("Desconectado del servidor");
        }
    }

}
