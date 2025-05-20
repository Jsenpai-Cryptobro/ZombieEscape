/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GameLogic;

import Personajes.Controller;
import Personajes.JugadorRemoto;
import Personajes.Personaje;
import Personajes.Zombie;
import Personajes.ZombieThread;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author jsero
 */
public class Map extends JPanel {

    //Atributos
    private final int TILE_SIZE = 35;
    private int MAP_WIDTH, MAP_HEIGHT;
    private final int VIEW_WIDTH = 9, VIEW_HEIGHT = 9; //Tamaño de la vista del jugador

    private int[][] mapa;

    private Controller controller;
    private GameManager gManager;

    //Constructor, de una vez carga el mapa usando FileManager
    public Map() {
        //Crea el manager
        gManager = new GameManager(this);

        //Crea mapa
        mapa = FileManager.cargarMapaDesdeArchivo("maps/mapa.txt");
        if (mapa == null) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el mapa.");
            System.exit(1);
        }

        MAP_HEIGHT = mapa.length;
        MAP_WIDTH = mapa[0].length;

        //Crea el personaje en el punto designado como spawn
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                if (mapa[y][x] == 4) {
                    gManager.setPersonaje(new Personaje(x, y, Color.BLUE));
                }
                if (mapa[y][x] == 2) {
                    generarZombie(y, x);
                }
            }
        }
        //Agrega Dimensiones y KeyListenes para leer movimiento del jugador
        setPreferredSize(new Dimension(VIEW_WIDTH * TILE_SIZE, VIEW_HEIGHT * TILE_SIZE));
        setFocusable(true);

        controller = new Controller(this, null);
        addKeyListener(controller);
    }

    //SETTERS Y GETTERS
    public void setController(Controller controller) {
        removeKeyListener(this.controller);
        this.controller = controller;
        addKeyListener(controller);
    }

    public Controller getController() {
        return controller;
    }

    public int getMAP_WIDTH() {
        return MAP_WIDTH;
    }

    public void setMAP_WIDTH(int MAP_WIDTH) {
        this.MAP_WIDTH = MAP_WIDTH;
    }

    public int getMAP_HEIGHT() {
        return MAP_HEIGHT;
    }

    public void setMAP_HEIGHT(int MAP_HEIGHT) {
        this.MAP_HEIGHT = MAP_HEIGHT;
    }

    public int[][] getMapa() {
        return mapa;
    }

    public void setMapa(int[][] nuevoMapa) {
        this.mapa = nuevoMapa;
        this.MAP_WIDTH = nuevoMapa[0].length;
        this.MAP_HEIGHT = nuevoMapa.length;
        repaint();
    }

    public GameManager getManager() {
        return gManager;
    }

    public void generarZombie(int mapY, int mapX) {
        mapa[mapY][mapX] = 0; //Lo marco como suelo porque ya se creo el zombie
        Zombie zombie = new Zombie(mapX, mapY, Color.GREEN);
        gManager.getZombiesInstancias().add(zombie); //Guarda el zombie

        //Crea e inicia el thread del zombie
        ZombieThread thread = new ZombieThread(zombie, this);
        gManager.getZombies().add(thread);
        thread.start();
    }

    //Funciones Auxiliares para ir pintando el mapa
    //Pues pinta las casillas del mapa
    private void paintMapa(Graphics g, int camX, int camY) {
        for (int y = 0; y < VIEW_HEIGHT; y++) {
            for (int x = 0; x < VIEW_WIDTH; x++) {
                int mapX = camX + x;
                int mapY = camY + y;

                switch (mapa[mapY][mapX]) {
                    case 0 ->
                        g.setColor(Color.BLACK);  // Suelo
                    case 1 ->
                        g.setColor(Color.GRAY);   // Edificio
                    case 4 ->
                        g.setColor(Color.YELLOW); // Spawn
                }

                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    //Pinta el jugador, en la posicion actual en la que este
    private void paintJugador(Graphics g, Personaje personaje, int camX, int camY) {
        int px = personaje.getX() - camX;
        int py = personaje.getY() - camY;

        if (px >= 0 && px < VIEW_WIDTH && py >= 0 && py < VIEW_HEIGHT) {
            g.setColor(personaje.getColor());
            g.fillOval(px * TILE_SIZE + 8, py * TILE_SIZE + 8, 16, 16);

            // Dibujar nombre si es un jugador remoto
            if (personaje instanceof JugadorRemoto) {
                g.setColor(Color.WHITE);
                g.drawString(((JugadorRemoto) personaje).getNombre(),
                        px * TILE_SIZE,
                        py * TILE_SIZE);
            }
        }
    }

    //Pinta el zombie
    private void paintZombies(Graphics g, int camX, int camY) {
        for (Zombie zombie : gManager.getZombiesInstancias()) {
            paintCampoVision(g, zombie, camX, camY);

            int zx = zombie.getX() - camX;
            int zy = zombie.getY() - camY;
            if (zx >= 0 && zx < VIEW_WIDTH && zy >= 0 && zy < VIEW_HEIGHT) {
                g.setColor(zombie.getColor());
                g.fillOval(zx * TILE_SIZE + 8, zy * TILE_SIZE + 8, 16, 16);
            }
        }
    }

    //Pinta el campo de vision del zombie
    private void paintCampoVision(Graphics g, Zombie zombie, int camX, int camY) {
        int dx = 0, dy = 0;
        switch (zombie.getDireccion()) {
            case 0 ->
                dy = -1; //Arriba
            case 1 ->
                dx = 1;  //Derecha
            case 2 ->
                dy = 1;  //Abajo
            case 3 ->
                dx = -1; //Izquierda
        }

        g.setColor(new Color(255, 255, 0, 100)); //Amarillo translucido
        for (int i = 1; i <= 2; i++) {
            int vx = zombie.getX() + dx * i;
            int vy = zombie.getY() + dy * i;

            if (vx < 0 || vy < 0 || vx >= MAP_WIDTH || vy >= MAP_HEIGHT) {
                break;
            }
            if (mapa[vy][vx] == 1) { //Para que no pueda ver a travez de obstaculos
                break;
            }

            int drawX = vx - camX;
            int drawY = vy - camY;
            if (drawX >= 0 && drawX < VIEW_WIDTH && drawY >= 0 && drawY < VIEW_HEIGHT) {
                g.fillRect(drawX * TILE_SIZE, drawY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    //Aca es donde se van pintando todos los componentes de la camara, con cada movimiento o suceso se actualiza
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Personaje personaje = gManager.getPersonaje();

        int camX = Math.max(0, Math.min(personaje.getX() - VIEW_WIDTH / 2, MAP_WIDTH - VIEW_WIDTH));
        int camY = Math.max(0, Math.min(personaje.getY() - VIEW_HEIGHT / 2, MAP_HEIGHT - VIEW_HEIGHT));

        paintMapa(g, camX, camY);
        paintJugador(g, personaje, camX, camY);

        // Dibujar jugadores remotos
        for (JugadorRemoto jugador : gManager.getJugadoresRemotos().values()) {
            paintJugador(g, jugador, camX, camY);
        }

        paintZombies(g, camX, camY);
    }
}
