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
    private ThreadCliente adminCliente;
    private boolean gameStarted = false;

    public ServidorJuego() {
        spawnPoints.add(new int[]{5, 5});
        spawnPoints.add(new int[]{10, 10});
        this.mapa = FileManager.cargarMapaDesdeArchivo("maps/mapa.txt");

        try {
            server = new ServerSocket(PORT);
            System.out.println("Servidor iniciado en puerto " + PORT);

            while (true) {
                Socket socket = server.accept();
                ThreadCliente cliente = new ThreadCliente(socket, this);

                // First player becomes admin
                if (adminCliente == null) {
                    adminCliente = cliente;
                    cliente.setAdmin(true);
                }

                cliente.start();
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
        if (gameStarted) {
            throw new IOException("Game already in progress");
        }

        clientes.put(nombre, hilo);
        broadcastPlayerList();
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

            // Validate map dimensions
            if (mapa == null || mapa.length == 0 || mapa[0].length == 0) {
                throw new IOException("Invalid map data");
            }

            // 1. Send map dimensions
            int alto = mapa.length;
            int ancho = mapa[0].length;

            // Validate dimensions
            if (ancho <= 0 || alto <= 0 || ancho > 1000 || alto > 1000) {
                throw new IOException("Invalid map dimensions: " + ancho + "x" + alto);
            }

            salida.writeInt(ancho);
            salida.writeInt(alto);

            // 2. Count and send non-empty tiles
            int nonEmptyTiles = 0;
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    if (mapa[y][x] != 0) {
                        nonEmptyTiles++;
                    }
                }
            }

            salida.writeInt(nonEmptyTiles);

            // Send non-empty tile data
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    if (mapa[y][x] != 0) {
                        salida.writeInt(y);
                        salida.writeInt(x);
                        salida.writeInt(mapa[y][x]);
                    }
                }
            }

            // 3. Send existing players
            int playerCount = clientes.size() - 1;
            salida.writeInt(playerCount);

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

    public synchronized void iniciarJuego(ThreadCliente iniciador) {
        if (iniciador == adminCliente && !gameStarted) {
            gameStarted = true;
            for (ThreadCliente cliente : clientes.values()) {
                try {
                    cliente.enviarInicioJuego();
                    enviarMapaInicial(cliente);
                } catch (IOException ex) {
                    System.out.println("Error al iniciar juego: " + ex.getMessage());
                }
            }
        }
    }

    private void broadcastPlayerList() {
        for (ThreadCliente cliente : clientes.values()) {
            try {
                cliente.enviarListaJugadores(new ArrayList<>(clientes.keySet()));
            } catch (IOException ex) {
                System.out.println("Error al enviar lista de jugadores: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new ServidorJuego();
    }
}
