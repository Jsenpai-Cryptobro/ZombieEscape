/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servidor;

import Cliente.ThreadCliente;
import GameLogic.FileManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author jsero
 */
public class ServidorJuego {

    private final int PORT = 8085;
    private ServerSocket server;
    private HashMap<String, ThreadCliente> clientes = new HashMap<>();
    private ArrayList<int[]> spawnPoints = new ArrayList<>();
    private int[][] mapa;

    public ServidorJuego() {
        // Configura puntos de spawn (deberías cargarlos de tu mapa)
        spawnPoints.add(new int[]{5, 5});
        spawnPoints.add(new int[]{10, 10});
        this.mapa = FileManager.cargarMapaDesdeArchivo("maps/mapa.txt");

        try {
            server = new ServerSocket(PORT);
            System.out.println("Servidor iniciado en puerto " + PORT);

            while (true) {
                Socket socket = server.accept();
                new ThreadCliente(socket, this).start();
            }
        } catch (IOException ex) {
            System.out.println("Error en servidor: " + ex.getMessage());
        }
    }

    public HashMap<String, ThreadCliente> getClientes() {
        return clientes;
    }

    private int[] obtenerPuntoSpawn() {
        // Implementa tu lógica para obtener puntos de spawn
        // Por ejemplo:
        return new int[]{5, 5}; // Posición de ejemplo
    }

    private void notificarNuevoJugador(String nombre, int x, int y) {
        for (ThreadCliente cliente : clientes.values()) {
            if (!cliente.getNombre().equals(nombre)) {
                try {
                    cliente.enviarPosicion(nombre, x, y);
                } catch (IOException ex) {
                    System.out.println("Error al notificar nuevo jugador: " + ex.getMessage());
                }
            }
        }
    }

    public synchronized void registrarCliente(String nombre, ThreadCliente hilo) throws IOException {
        clientes.put(nombre, hilo);

        // Asignar posición inicial
        int[] spawn = obtenerPuntoSpawn();
        hilo.enviarPosicionInicial(spawn[0], spawn[1]);

        // Enviar mapa inicial
        enviarMapaInicial(hilo);

        // Notificar a otros jugadores
        notificarNuevoJugador(nombre, spawn[0], spawn[1]);
    }

    public synchronized void actualizarPosicion(String nombre, int x, int y) {
        for (ThreadCliente cliente : clientes.values()) {
            if (!cliente.getNombre().equals(nombre)) {
                try {
                    cliente.enviarPosicion(nombre, x, y);
                } catch (IOException ex) {
                    System.out.println("Error al actualizar posición: " + ex.getMessage());
                }
            }
        }
    }

    private void enviarMapaInicial(ThreadCliente cliente) {
        try {
            DataOutputStream salida = cliente.getSalida();

            // 1. Enviar solo las diferencias respecto a mapa vacío
            salida.writeInt(mapa[0].length); // Ancho
            salida.writeInt(mapa.length);    // Alto

            // 2. Enviar solo los tiles no vacíos (optimización)
            int nonEmptyTiles = 0;
            for (int y = 0; y < mapa.length; y++) {
                for (int x = 0; x < mapa[0].length; x++) {
                    if (mapa[y][x] != 0) { // 0 representa tile vacío
                        nonEmptyTiles++;
                    }
                }
            }

            salida.writeInt(nonEmptyTiles); // Cantidad de tiles no vacíos

            // Enviar solo los tiles no vacíos
            for (int y = 0; y < mapa.length; y++) {
                for (int x = 0; x < mapa[0].length; x++) {
                    if (mapa[y][x] != 0) {
                        salida.writeInt(y); // Coordenada Y
                        salida.writeInt(x); // Coordenada X
                        salida.writeInt(mapa[y][x]); // Valor del tile
                    }
                }
            }

            // 3. Enviar jugadores existentes
            salida.writeInt(clientes.size() - 1); // Excluye al nuevo jugador
            for (ThreadCliente otro : clientes.values()) {
                if (!otro.getNombre().equals(cliente.getNombre())) {
                    salida.writeUTF(otro.getNombre());
                    salida.writeInt(otro.getX());
                    salida.writeInt(otro.getY());
                }
            }
        } catch (IOException ex) {
            System.out.println("Error al enviar mapa inicial: " + ex.getMessage());
        }
    }

    private void broadcastJugadores() {
        // Enviar lista de jugadores a todos los clientes
        // Implementar según necesidad
    }

    public static void main(String[] args) {
        new ServidorJuego();
    }
}
