/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

import GameLogic.Map;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author jsero
 */
public class ZombieThread extends Thread {

    private Zombie zombie;
    private Map map;
    private boolean running = true;
    private int direccion = 0; // 0 = arriba, 1 = derecha, 2 = abajo, 3 = izquierda

    public ZombieThread(Zombie zombie, Map map) {
        this.zombie = zombie;
        this.map = map;
    }

    public Zombie getZombie() {
        return zombie;
    }

    @Override
    public void run() {
        while (running) {
            moverZombie();
            map.repaint(); //Redibuja el mapa
            try {
                Thread.sleep(500);//Se mueven cada medio segundo
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    //Revisa todo lo que se considere un obstaculo para ver si cambia de direccion el zombie
    public boolean esObstaculo(int x, int y) {
        //Limites del mapa 
        if (x < 0 || x >= map.getMAP_WIDTH() || y < 0 || y >= map.getMAP_HEIGHT()) {
            return true;
        }

        //Casilla distinta a 0 (por donde caminan los zombies)
        if (map.getTileAt(x, y) != 0) {
            return true;
        }

        //Copia sincronizada de la lista de zombies, para revisar si se chocan entre ellos
        ArrayList<ZombieThread> zombies;
        synchronized (map.getManager().getZombies()) {
            zombies = new ArrayList<>(map.getManager().getZombies());
        }

        //Revisa si otro zombie le estorba
        for (ZombieThread tp : zombies) {
            Zombie z = tp.getZombie();
            if (z != zombie && z.getX() == x && z.getY() == y) {
                return true;
            }
        }

        return false;
    }
    
    //Mueve el zombie en una direccion
    public void moverZombie() {
        int dx = 0, dy = 0;

        switch (direccion) {
            case 0 ->
                dy = -1; //Arriba
            case 1 ->
                dx = 1;  //Derecha
            case 2 ->
                dy = 1;  //Abajo
            case 3 ->
                dx = -1; //Izquierda
        }

        int nuevoX = zombie.getX() + dx;
        int nuevoY = zombie.getY() + dy;

        // Verifica si hay obstáculo
        if (!esObstaculo(nuevoX, nuevoY)) {
            zombie.setX(nuevoX);
            zombie.setY(nuevoY);

        } else {
            //Gira 90° a la derecha
            //Basicamente es un ciclo en el switch de direccion, asi que para girar a la derecha sumo uno
            //y agarro su residuo al dividir entre 4(cantidad de elementos en el switch)
            direccion = (direccion + 1) % 4;
        }

        zombie.setDireccion(direccion);
    }

    public void detener() {
        running = false;
    }
}
