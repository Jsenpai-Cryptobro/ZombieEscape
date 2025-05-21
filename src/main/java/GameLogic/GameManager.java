/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GameLogic;

import Personajes.JugadorRemoto;
import Personajes.Personaje;
import Personajes.Zombie;
import Personajes.ZombieThread;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author jsero
 */
public class GameManager {

    //Atributos
    private Personaje personaje;
    private HashMap<String, JugadorRemoto> jugadoresRemotos = new HashMap<>(); //HashMap para que cada jugador se pueda identificar

    private List<ZombieThread> zombies = new CopyOnWriteArrayList<>();
    private List<Zombie> zombiesT = new CopyOnWriteArrayList<>();

    private Map mapa;

    //Constructor
    public GameManager(Map mapa) {
        this.mapa = mapa;
    }

    //GETTERS Y SETTERS
    public Personaje getPersonaje() {
        return personaje;
    }

    public void setPersonaje(Personaje personaje) {
        this.personaje = personaje;
        // Ensure map is repainted when player is set
        if (mapa != null) {
            mapa.repaint();
        }
    }

    public List<ZombieThread> getZombies() {
        return zombies;
    }

    public List<Zombie> getZombiesInstancias() {
        return zombiesT;
    }

    public void setZombies(ArrayList<ZombieThread> zombies) {
        this.zombies = zombies;
    }

    public Map getMapa() {
        return mapa;
    }

    public void setMapa(Map mapa) {
        this.mapa = mapa;
    }

    public HashMap<String, JugadorRemoto> getJugadoresRemotos() {
        return jugadoresRemotos;
    }

    //RESTO DE LOGICA
    public void detenerZombies() {
        for (ZombieThread thread : zombies) {
            thread.detener();
        }
    }

    public void agregarJugadorRemoto(String nombre, int x, int y) {
        jugadoresRemotos.put(nombre, new JugadorRemoto(x, y, Color.RED, nombre));
        mapa.repaint();
    }

    public void actualizarPosicionJugador(String nombre, int x, int y) {
        if (!jugadoresRemotos.containsKey(nombre)) {
            // Create new remote player if it doesn't exist
            jugadoresRemotos.put(nombre, new JugadorRemoto(x, y, Color.RED, nombre));
        } else {
            // Update existing player position
            JugadorRemoto jugador = jugadoresRemotos.get(nombre);
            jugador.setX(x);
            jugador.setY(y);
        }
        // Request repaint to show updated positions
        mapa.repaint();
    }

    public void removerJugador(String nombre) {
        jugadoresRemotos.remove(nombre);
        mapa.repaint();
    }

}
