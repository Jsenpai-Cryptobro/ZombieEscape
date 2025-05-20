/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

import Servidor.ServidorJuego;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author jsero
 */
public class ThreadCliente extends Thread {

    private int x;
    private int y;
    private Socket socket;
    private ServidorJuego servidor;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private String nombre;

    public ThreadCliente(Socket socket, ServidorJuego servidor) {
        this.socket = socket;
        this.servidor = servidor;
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Error al crear streams: " + ex.getMessage());
        }
    }
    
    public DataOutputStream getSalida() {
        return salida;
    }
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void run() {
        try {
            nombre = entrada.readUTF();
            servidor.registrarCliente(nombre, this);

            while (true) {
                int x = entrada.readInt();
                int y = entrada.readInt();
                servidor.actualizarPosicion(nombre, x, y);
            }
        } catch (IOException ex) {
            System.out.println("Cliente desconectado: " + nombre);
            servidor.getClientes().remove(nombre);
        }
    }

    public void enviarPosicionInicial(int x, int y) throws IOException {
        this.x = x;
        this.y = y;
        salida.writeUTF("POSICION_INICIAL");
        salida.writeInt(x);
        salida.writeInt(y);
    }

    public void enviarMapaInicial(int[][] mapa) throws IOException {
        salida.writeUTF("MAPA_INICIAL");
        salida.writeInt(mapa[0].length); // Ancho
        salida.writeInt(mapa.length);    // Alto

        // Enviar el mapa
        for (int y = 0; y < mapa.length; y++) {
            for (int x = 0; x < mapa[0].length; x++) {
                salida.writeInt(mapa[y][x]);
            }
        }

        // Enviar jugadores existentes
        salida.writeInt(servidor.getClientes().size() - 1); // Excluye al propio jugador
        for (ThreadCliente cliente : servidor.getClientes().values()) {
            if (!cliente.getNombre().equals(this.nombre)) {
                salida.writeUTF(cliente.getNombre());
                salida.writeInt(cliente.getX());
                salida.writeInt(cliente.getY());
            }
        }
    }

    public void enviarPosicion(String nombre, int x, int y) throws IOException {
        salida.writeUTF(nombre);
        salida.writeInt(x);
        salida.writeInt(y);
    }

}
