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
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author jsero
 */
public class Map extends JPanel {

    //Atributos
    private int TILE_SIZE = 35; //Tamaño de los cuadros de la matriz
    private int MAP_WIDTH, MAP_HEIGHT;
    private int VIEW_WIDTH = 9, VIEW_HEIGHT = 9; //Tamaño de la vista del jugador

    private HashMap<Point, Integer> mapaTiles; //Mapa que funciona con hash porque la matriz sola me estaba cagando la existencia para metodos que hice luego
    private List<Zombie> zombiesSincronizados = new ArrayList<>(); 

    private Controller controller;
    private GameManager gManager;

    private boolean modoEspectador = false;

    //Constructor, de una vez carga el mapa usando FileManager
    public Map() {
        gManager = new GameManager(this);
        mapaTiles = new HashMap<>();
        setFocusable(true);
    }
    
    //GETTERS Y SETTERS
    public void setModoEspectador(boolean b) {
        this.modoEspectador = b;

        //Al entrar a modo espectador, elimina el personaje que murio
        if (b) {
            gManager.setPersonaje(null);
            VIEW_WIDTH = MAP_WIDTH;
            VIEW_HEIGHT = MAP_HEIGHT;
            setPreferredSize(new Dimension(VIEW_WIDTH * TILE_SIZE, VIEW_HEIGHT * TILE_SIZE));
            revalidate();
            repaint();
        }
    }
    
    public void setZombiesSincronizados(List<Zombie> zombies) {
        this.zombiesSincronizados = zombies;
        repaint();
    }

    public void setMapa(int[][] nuevoMapa) {
        MAP_HEIGHT = nuevoMapa.length;
        MAP_WIDTH = nuevoMapa[0].length;
        mapaTiles.clear();

        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                int valor = nuevoMapa[y][x];

                if (valor != 0) {
                    mapaTiles.put(new Point(x, y), valor);

                    // Solo crear personaje si no hay uno y NO es espectador
                    if (valor == 4 && gManager.getPersonaje() == null && !modoEspectador) {
                        gManager.setPersonaje(new Personaje(x, y, Color.BLUE));
                    }
                }
            }
        }

        //Ajusta vista a la del espectador
        if (modoEspectador) {
            VIEW_WIDTH = MAP_WIDTH;
            VIEW_HEIGHT = MAP_HEIGHT;
            setPreferredSize(new Dimension(VIEW_WIDTH * TILE_SIZE, VIEW_HEIGHT * TILE_SIZE));
            revalidate();
        }

        repaint();
    }

    public int getTile(int x, int y) {
        return mapaTiles.getOrDefault(new Point(x, y), 0);
    }
    
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

    public GameManager getManager() {
        return gManager;
    }

    //Valor de una casilla en coordenadas especificas
    public int getTileAt(int x, int y) {
        return mapaTiles.getOrDefault(new Point(x, y), 0); 
    }

    public void setTileAt(int x, int y, int value) {
        if (value == 0) {
            mapaTiles.remove(new Point(x, y));
        } else {
            mapaTiles.put(new Point(x, y), value);
        }
    }

    //Revisa si esta dentro del tamaño del mapa
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT;
    }
    
    //Genera los zombies en el mapa
    public void generarZombie(int mapY, int mapX) {
        mapaTiles.remove(new Point(mapX, mapY)); 
        Zombie zombie = new Zombie(mapX, mapY, Color.GREEN);
        gManager.getZombiesInstancias().add(zombie); 

        ZombieThread thread = new ZombieThread(zombie, this);
        gManager.getZombies().add(thread);
        thread.start();
    }

    //Funciones Auxiliares para ir pintando el mapa
    //Pues pinta las casillas del mapa
    public void paintMapa(Graphics g, int camX, int camY) {
        for (int y = 0; y < VIEW_HEIGHT; y++) {
            for (int x = 0; x < VIEW_WIDTH; x++) {
                int mapX = camX + x;
                int mapY = camY + y;

                switch (getTileAt(mapX, mapY)) {
                    case 0 ->
                        g.setColor(Color.BLACK);  // Suelo
                    case 1 ->
                        g.setColor(Color.GRAY);   // Edificio
                    case 4 ->
                        g.setColor(Color.YELLOW); // Spawn
                    case 5 ->
                        g.setColor(Color.PINK); // Meta 
                }

                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    //Pinta el jugador, en la posicion actual en la que este
    public void paintJugador(Graphics g, Personaje personaje, int camX, int camY) {
        if (personaje == null) {
            return;
        }
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
    public void paintZombies(Graphics g, int camX, int camY) {
        for (Zombie zombie : zombiesSincronizados) {
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
    public void paintCampoVision(Graphics g, Zombie zombie, int camX, int camY) {
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
            if (getTileAt(vx, vy) == 1) { //Para que no pueda ver a travez de obstaculos
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
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        Personaje personaje = gManager.getPersonaje();

        if (modoEspectador) {
            personaje = null; //Forzar que si es espectador no tenga personaje
        }

        int camX = 0, camY = 0;
        int viewW, viewH;

        if (modoEspectador || personaje == null) {
            //Forzar camara para ver todo al ser espectador
            viewW = MAP_WIDTH;
            viewH = MAP_HEIGHT;
            camX = 0;
            camY = 0;
        } else {
            //Camara limitada para jugadores vivos
            viewW = 9;
            viewH = 9;
            camX = Math.max(0, Math.min(personaje.getX() - viewW / 2, MAP_WIDTH - viewW));
            camY = Math.max(0, Math.min(personaje.getY() - viewH / 2, MAP_HEIGHT - viewH));
        }

        VIEW_WIDTH = viewW;
        VIEW_HEIGHT = viewH;

        //Pinta todo
        paintMapa(g, camX, camY);
        System.out.println("→ repaint mapa. modoEspectador=" + modoEspectador + ", personaje=" + gManager.getPersonaje());

        if (personaje != null && !modoEspectador) {
            paintJugador(g, personaje, camX, camY);
        }

        for (JugadorRemoto jugador : gManager.getJugadoresRemotos().values()) {
            paintJugador(g, jugador, camX, camY);
        }

        paintZombies(g, camX, camY);
    }
}
