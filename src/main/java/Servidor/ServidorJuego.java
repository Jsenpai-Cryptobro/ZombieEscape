
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servidor;

import Cliente.ThreadCliente;
import GameLogic.FileManager;
import GameLogic.PlayerState;
import Personajes.Zombie;
import java.awt.Color;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jsero
 */
public class ServidorJuego {

    private final int PORT = 8085;
    private ServerSocket server;
    private HashMap<String, ThreadCliente> clientes = new HashMap<>();
    private List<Zombie> zombies = new ArrayList<>();
    private Thread zombieManagerThread;
    private int[][] mapa;
    private ThreadCliente adminCliente;
    private boolean gameStarted = false;

    public ServidorJuego() {

        try {
            server = new ServerSocket(PORT);
            System.out.println("Servidor iniciado en puerto " + PORT);

            while (true) {
                Socket socket = server.accept();
                ThreadCliente cliente = new ThreadCliente(socket, this);

                //Primero en entrar es admin
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

    //Recibe los clientes pero si el juego ya inicio no los deja unirse
    public synchronized void registrarCliente(String nombre, ThreadCliente hilo) throws IOException {
        if (gameStarted) {
            throw new IOException("El juego ya ha iniciado");
        }

        clientes.put(nombre, hilo);
        broadcastPlayerList();
    }
    
    //Cambia al siguiente nivel (Aca toda hacer que el los mapas tengan el nombre mapa#.txt) asi simplemente
    //se crea un atributo de lvlActual y va cambiando conforme se supera un nivel
    public void avanzarNivel() {
        System.out.println("¡Avanzando nivel!");
        gameStarted = false;

        File archivoSiguiente = new File("maps/mapa2.txt"); //Cambiar esto cuando tenga lvlActual
        if (!archivoSiguiente.exists()) {
            System.out.println("No hay mas niveles.");
            return;
        }

        this.mapa = FileManager.cargarMapaDesdeArchivo(archivoSiguiente.getPath());
        for (ThreadCliente cliente : clientes.values()) {
            cliente.setEnMeta(false);
            try {
                cliente.enviarInicioJuego();
                enviarMapaInicial(cliente);
            } catch (IOException ex) {
                System.out.println("Error al avanzar nivel con " + cliente.getNombre());
            }
        }

        //Regenerar zombies
        zombies.clear();
        for (int y = 0; y < mapa.length; y++) {
            for (int x = 0; x < mapa[0].length; x++) {
                if (mapa[y][x] == 2) {
                    zombies.add(new Zombie(x, y, Color.GREEN));
                }
            }
        }

        gameStarted = true;

        iniciarZombieManager();
    }
    
    //Verifica que todos esten en la meta para poder avanzar de nivel
    public void verificarMetaGlobal() {
        //System.out.println("Verificando si todos los jugadores llegaron a la meta");//Esto es para ver si se esta activando o no pq no me esta sirviendo

        for (ThreadCliente cliente : clientes.values()) {
            System.out.println(" - " + cliente.getNombre() + ": muerto=" + (cliente.getEstado() == PlayerState.MUERTO) + ", enMeta=" + cliente.isEnMeta());
            if (cliente.getEstado() != PlayerState.MUERTO && !cliente.isEnMeta()) {
                return;
            }
        }

        System.out.println("Todos los jugadores vivos llegaron a la meta");
        avanzarNivel();
    }

    //Actualiza la posicion de x jugador en el mapa(para si mismo y para los demas)
    public synchronized void actualizarPosicion(String nombre, int x, int y) {
        for (ThreadCliente cliente : clientes.values()) {
            if (!cliente.getNombre().equals(nombre)) {
                try {
                    cliente.enviarPosicion(nombre, x, y);
                } catch (IOException ex) {
                    System.out.println("Error al actualizar posicion: " + ex.getMessage());
                }
            }
        }

        ThreadCliente jugador = clientes.get(nombre);
        if (jugador != null && jugador.getEstado() != PlayerState.MUERTO) {
            System.out.println("Jugador " + nombre + " se movió a (" + x + ", " + y + "), tile = " + mapa[y][x]);
            if (mapa[y][x] == 5) {
                if (!jugador.isEnMeta()) {
                    jugador.setEnMeta(true);
                    System.out.println(jugador.getNombre() + " llegó a la meta.");
                    verificarMetaGlobal();
                }
            } else {
                jugador.setEnMeta(false); // Salió de la meta
            }
        }
    }

    //Envia el mapa a los clientes
    public void enviarMapaInicial(ThreadCliente cliente) {
        try {
            DataOutputStream salida = cliente.getSalida();

            //Valida el mapa, que exista y su largo 
            if (mapa == null || mapa.length == 0 || mapa[0].length == 0) {
                throw new IOException("Mapa invalido");
            }

            //Crea alto y ancho
            int alto = mapa.length;
            int ancho = mapa[0].length;

            //Pone limite y minimo al ancho y alto
            if (ancho <= 0 || alto <= 0 || ancho > 1000 || alto > 1000) {
                throw new IOException("Dimensiones invalidas: " + ancho + "x" + alto);
            }

            //Lo escribe
            salida.writeInt(ancho);
            salida.writeInt(alto);

            //Cuenta y envia las casillas que no estan vacias
            int casillasLlenas = 0;
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    if (mapa[y][x] != 0) {
                        casillasLlenas++;
                    }
                }
            }

            salida.writeInt(casillasLlenas);

            //Manda los datos
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    if (mapa[y][x] != 0) {
                        salida.writeInt(y);
                        salida.writeInt(x);
                        salida.writeInt(mapa[y][x]);
                    }
                }
            }

            //Manda los jugadores
            int playerCount = 0;
            for (ThreadCliente otro : clientes.values()) {
                if (!otro.getNombre().equals(cliente.getNombre()) && otro.getEstado() != PlayerState.MUERTO) {
                    playerCount++;
                }
            }
            salida.writeInt(playerCount);

            for (ThreadCliente otro : clientes.values()) {
                if (!otro.getNombre().equals(cliente.getNombre()) && otro.getEstado() != PlayerState.MUERTO) {
                    salida.writeUTF(otro.getNombre());
                    salida.writeInt(otro.getX());
                    salida.writeInt(otro.getY());
                }
            }
            enviarZombiesIniciales(cliente);

        } catch (IOException ex) {
            System.out.println("Error al enviar mapa inicial: " + ex.getMessage());
        }
    }

    //Cuando el admin inicia el juego
    public synchronized void iniciarJuego(ThreadCliente iniciador, String nombreMapa) {
        if (iniciador == adminCliente && !gameStarted) {
            gameStarted = true;

            File archivoMapa = new File("maps/" + nombreMapa);
            if (!archivoMapa.exists()) {
                System.err.println("El mapa no existe: " + nombreMapa + ". Usando mapa.txt por defecto.");
                archivoMapa = new File("maps/mapa.txt");
            }

            this.mapa = FileManager.cargarMapaDesdeArchivo(archivoMapa.getPath());
            if (this.mapa == null) {
                System.err.println("Error cargando el mapa: " + archivoMapa.getPath());
                return;
            }

            for (int y = 0; y < mapa.length; y++) {
                for (int x = 0; x < mapa[0].length; x++) {
                    if (mapa[y][x] == 2) { // Tile 2 = spawn de zombie
                        zombies.add(new Zombie(x, y, Color.GREEN));
                        System.out.println("Zombie creado en: " + x + "," + y);
                    }
                }
            }

            for (ThreadCliente cliente : clientes.values()) {
                try {
                    cliente.enviarInicioJuego();
                    enviarMapaInicial(cliente);
                } catch (IOException ex) {
                    System.out.println("Error al iniciar juego para " + cliente.getNombre() + ": " + ex.getMessage());
                }
            }

            // Crear zombies en posiciones específicas del mapa (por ejemplo, donde haya valor 2)
            iniciarZombieManager();
        }
    }

    public void volverASala() {
        gameStarted = false;
        zombies.clear();

        for (ThreadCliente cliente : clientes.values()) {
            try {
                cliente.setEstado(PlayerState.IN_WAITING_ROOM);
                cliente.getSalida().writeUTF("VOLVER_A_SALA");
            } catch (IOException e) {
                System.out.println("Error al volver a sala con " + cliente.getNombre());
            }
        }
    }

    public void verificarColisionesConJugadores() throws IOException {
        for (Zombie z : zombies) {
            int zx = z.getX();
            int zy = z.getY();
            int dx = 0, dy = 0;

            switch (z.getDireccion()) {
                case 0 ->
                    dy = -1;
                case 1 ->
                    dx = 1;
                case 2 ->
                    dy = 1;
                case 3 ->
                    dx = -1;
            }

            for (int i = 1; i <= 2; i++) {
                int vx = zx + dx * i;
                int vy = zy + dy * i;

                if (!esMovimientoValido(vx, vy)) {
                    break;
                }

                for (ThreadCliente cliente : clientes.values()) {
                    if (cliente.getX() == vx && cliente.getY() == vy && cliente.getEstado() != PlayerState.MUERTO) {
                        System.out.println("Jugador " + cliente.getNombre() + " fue detectado por zombie");
                        cliente.setEstado(PlayerState.MUERTO);
                        cliente.enviarMuerte();
                        for (ThreadCliente otro : clientes.values()) {
                            if (otro != cliente) {
                                otro.getSalida().writeUTF("JUGADOR_DESCONECTADO");
                                otro.getSalida().writeUTF(cliente.getNombre());
                            }
                        }
                        reiniciarJuegoExcepto(cliente);
                        return; // Solo uno detectado por vez
                    }
                }
            }
        }
        // Verificar si todos están muertos
        boolean todosMuertos = true;
        for (ThreadCliente cliente : clientes.values()) {
            if (cliente.getEstado() != PlayerState.MUERTO) {
                todosMuertos = false;
                break;
            }
        }

        if (todosMuertos) {
            System.out.println("Todos los jugadores murieron. Volviendo a sala de espera...");
            volverASala();
        }

    }

    public void reiniciarJuegoExcepto(ThreadCliente muerto) {
        for (ThreadCliente cliente : clientes.values()) {
            if (cliente != muerto && cliente.getEstado() != PlayerState.MUERTO) {
                try {
                    cliente.enviarInicioJuego();
                    enviarMapaInicial(cliente);
                } catch (IOException ex) {
                    System.out.println("Error al reiniciar juego para " + cliente.getNombre() + ": " + ex.getMessage());
                }
            }
        }
    }
    
    
    public void iniciarZombieManager() {
        // Detener hilo anterior si está corriendo
        if (zombieManagerThread != null && zombieManagerThread.isAlive()) {
            zombieManagerThread.interrupt();
            try {
                zombieManagerThread.join(); // Esperar que termine
            } catch (InterruptedException e) {
                System.out.println("Error al esperar zombieManager anterior.");
            }
        }

        // Ahora crear nuevo hilo
        zombieManagerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {

                if (!gameStarted) {
                    try {
                        Thread.sleep(200); // Evitar consumir CPU en espera
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue; // No hacer nada si el juego aún no ha empezado
                }

                moverZombies();
                try {
                    verificarColisionesConJugadores();
                } catch (IOException ex) {
                    Logger.getLogger(ServidorJuego.class.getName()).log(Level.SEVERE, null, ex);
                }
                enviarPosicionesZombies();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        zombieManagerThread.start();
    }
    
    //Va moviendo los zombies
    public void moverZombies() {
        for (Zombie zombie : zombies) {
            int dx = 0, dy = 0;
            int dir = zombie.getDireccion();

            switch (dir) {
                case 0 ->
                    dy = -1;
                case 1 ->
                    dx = 1;
                case 2 ->
                    dy = 1;
                case 3 ->
                    dx = -1;
            }

            int newX = zombie.getX() + dx;
            int newY = zombie.getY() + dy;

            if (esMovimientoValido(newX, newY)) {
                zombie.setX(newX);
                zombie.setY(newY);
            } else {
                zombie.setDireccion((dir + 1) % 4); // girar
            }
        }
    }
    
    //Envia las posiciones actuales de los zombies al clienteJuego
    public void enviarPosicionesZombies() {
        for (ThreadCliente cliente : clientes.values()) {
            try {
                DataOutputStream out = cliente.getSalida();
                out.writeUTF("ZOMBIE_UPDATE");
                out.writeInt(zombies.size());
                for (Zombie zombie : zombies) {
                    out.writeInt(zombie.getX());
                    out.writeInt(zombie.getY());
                    out.writeInt(zombie.getDireccion());
                }
            } catch (IOException e) {
                System.out.println("Error al enviar zombies: " + e.getMessage());
            }
        }
    }
    
    //Envia la posicion inicial de los zombies al clienteJuego
    public void enviarZombiesIniciales(ThreadCliente cliente) {
        try {
            DataOutputStream out = cliente.getSalida();
            out.writeUTF("ZOMBIE_INICIAL");
            out.writeInt(zombies.size());
            for (Zombie zombie : zombies) {
                out.writeInt(zombie.getX());
                out.writeInt(zombie.getY());
                out.writeInt(zombie.getDireccion());
            }
        } catch (IOException e) {
            System.out.println("Error al enviar zombies iniciales: " + e.getMessage());
        }
    }

    public boolean esMovimientoValido(int x, int y) {
        return x >= 0 && x < mapa[0].length && y >= 0 && y < mapa.length && mapa[y][x] == 0;
    }

    //Envia la lista de jugadores
    public void broadcastPlayerList() {
        for (ThreadCliente cliente : clientes.values()) {
            try {
                cliente.enviarListaJugadores(new ArrayList<>(clientes.keySet()));
            } catch (IOException ex) {
                System.out.println("Error al enviar lista de jugadores: " + ex.getMessage());
            }
        }
    }

    //Envia la posicion de cada jugador
    public synchronized void broadcastPosicion(String nombre, int x, int y) {
        for (ThreadCliente cliente : clientes.values()) {
            if (!cliente.getNombre().equals(nombre)) {
                try {
                    cliente.enviarPosicion(nombre, x, y);
                } catch (IOException ex) {
                    System.out.println("Error al enviar posicion a " + cliente.getNombre() + ": " + ex.getMessage());
                }
            }
        }
        ThreadCliente jugador = clientes.get(nombre);
        if (jugador != null && jugador.getEstado() != PlayerState.MUERTO) {
            if (mapa[y][x] == 5) {
                if (!jugador.isEnMeta()) {
                    jugador.setEnMeta(true);
                    System.out.println(jugador.getNombre() + " llegó a la meta.");
                    verificarMetaGlobal();
                }
            } else {
                jugador.setEnMeta(false); // Se salió de la meta
            }
        }
    }

    //Notifica al resto que alguien jalo
    public synchronized void clienteDesconectado(ThreadCliente cliente) {
        clientes.remove(cliente.getNombre());
        for (ThreadCliente otros : clientes.values()) {
            try {
                otros.getSalida().writeUTF("JUGADOR_DESCONECTADO");
                otros.getSalida().writeUTF(cliente.getNombre());
            } catch (IOException ex) {
                System.out.println("Error al notificar desconexion a " + otros.getNombre());
            }
        }
    }

    //Para iniciar el servidor
    public static void main(String[] args) {
        new ServidorJuego();
    }
}
