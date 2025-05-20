/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

import GameLogic.Map;
import java.util.ArrayList;

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

    private boolean esObstaculo(int x, int y) {
        int[][] mapa = map.getMapa();

        // Fuera de los límites u obstáculo
        if (x < 0 || x >= map.getMAP_WIDTH() || y < 0 || y >= map.getMAP_HEIGHT() || mapa[y][x] != 0) {
            return true;
        }

        // Hacer una copia sincronizada de la lista de zombies
        ArrayList<ZombieThread> zombies;
        synchronized (map.getManager().getZombies()) {
            zombies = new ArrayList<>(map.getManager().getZombies());
        }

        // Verificar si hay otro zombie bloqueando
        for (ZombieThread tp : zombies) {
            Zombie z = tp.getZombie();
            if (z != zombie && z.getX() == x && z.getY() == y) {
                return true;
            }
        }

        return false;
    }

    //Aun no lo implemento pues ni he agregado jugadores al manager pero deberia funcionar cuando lo tenga
    /*private boolean jugadorDetectado() {
        Personaje jugador = map.getManager().getPersonaje();
        int zx = zombie.getX();
        int zy = zombie.getY();

        for (int i = 1; i <= 2; i++) {
            int checkX = zx, checkY = zy;
            switch (direccion) {
                case 0 ->
                    checkY -= i; 
                case 1 ->
                    checkX += i;
                case 2 ->
                    checkY += i;
                case 3 ->
                    checkX -= i;
            }

            if (checkX == jugador.getX() && checkY == jugador.getY()) {
                return true;
            }
            if (esObstaculo(checkX, checkY)) {
                return false; // pared bloquea la visión
            }
        }

        return false;
    }*/
    private void moverZombie() {
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
