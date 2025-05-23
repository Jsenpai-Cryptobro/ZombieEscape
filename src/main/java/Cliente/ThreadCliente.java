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
    private int x;
    private int y;
    private boolean enMeta = false;

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

    //GETTERS Y SETTERS
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

    public PlayerState getEstado() {
        return estado;
    }

    public void setEstado(PlayerState estado) {
        this.estado = estado;
    }

    public boolean isEnMeta() {
        return enMeta;
    }

    public void setEnMeta(boolean enMeta) {
        this.enMeta = enMeta;
    }

    //Metodos para enviar los datos al server
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

    public void enviarMuerte() {
        try {
            salida.writeUTF("MUERTO");
        } catch (IOException ex) {
            System.out.println("Error al enviar muerte: " + ex.getMessage());
        }
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
                            String nombreMapa = entrada.readUTF();
                            servidor.iniciarJuego(this, nombreMapa);
                        }
                        break;
                    case "MOVIMIENTO":
                        int newX = entrada.readInt();
                        int newY = entrada.readInt();
                        //Cambiar posicion
                        this.x = newX;
                        this.y = newY;
                        //Mandarla al resto de jugadores
                        servidor.broadcastPosicion(nombre, newX, newY);
                        break;
                    case "CHAT":
                        String mensajeChat = entrada.readUTF();
                        servidor.enviarChat(mensajeChat);
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Cliente desconectado: " + ex.getMessage());
            servidor.clienteDesconectado(this);
        }
    }
}
