/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

import GameLogic.PlayerState;
import Servidor.ServidorJuego;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 *
 * @author jsero
 */
public class ThreadCliente extends Thread {

    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private ServidorJuego servidor;
    private String nombre;
    private boolean isAdmin = false;
    private PlayerState estado = PlayerState.IN_WAITING_ROOM;
    private int x; // Player's X coordinate
    private int y; // Player's Y coordinate

    public ThreadCliente(Socket socket, ServidorJuego servidor) {
        this.socket = socket;
        this.servidor = servidor;
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Error creando streams: " + ex.getMessage());
        }
    }

    // Getters and setters for coordinates
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getNombre() {
        return nombre;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public DataOutputStream getSalida() {
        return salida;
    }

    public void enviarPosicionInicial(int x, int y) throws IOException {
        this.x = x;
        this.y = y;
        salida.writeUTF("POSICION_INICIAL");
        salida.writeInt(x);
        salida.writeInt(y);
    }

    public void enviarPosicion(String jugador, int x, int y) throws IOException {
        salida.writeUTF("POSICION");
        salida.writeUTF(jugador);
        salida.writeInt(x);
        salida.writeInt(y);
    }

    public void enviarListaJugadores(List<String> jugadores) throws IOException {
        salida.writeUTF("PLAYER_LIST");
        salida.writeInt(jugadores.size());
        for (String jugador : jugadores) {
            salida.writeUTF(jugador);
        }
    }

    public void enviarInicioJuego() throws IOException {
        salida.writeUTF("GAME_START");
    }

    @Override
    public void run() {
        try {
            nombre = entrada.readUTF();
            salida.writeBoolean(isAdmin);
            servidor.registrarCliente(nombre, this);

            while (true) {
                String mensaje = entrada.readUTF();
                switch (mensaje) {
                    case "START_GAME":
                        if (isAdmin) {
                            servidor.iniciarJuego(this);
                        }
                        break;
                    case "MOVIMIENTO":
                        // Read new position
                        int newX = entrada.readInt();
                        int newY = entrada.readInt();
                        // Update position
                        this.x = newX;
                        this.y = newY;
                        // Broadcast to other players
                        servidor.actualizarPosicion(nombre, newX, newY);
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Cliente desconectado: " + ex.getMessage());
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error cerrando socket: " + e.getMessage());
            }
            servidor.getClientes().remove(nombre);
        }
    }
}
