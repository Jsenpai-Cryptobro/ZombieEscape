/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GameLogic;

/**
 *
 * @author jsero
 */
public class FileManager {

    public static int[][] cargarMapaDesdeArchivo(String ruta) {
        java.util.List<int[]> filas = new java.util.ArrayList<>();

        try (java.util.Scanner scanner = new java.util.Scanner(new java.io.File(ruta))) {
            while (scanner.hasNextLine()) {
                String linea = scanner.nextLine().trim();
                if (linea.isEmpty()) {
                    continue;
                }

                String[] partes = linea.split("\\s+");
                int[] fila = new int[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    fila[i] = Integer.parseInt(partes[i]);
                }
                filas.add(fila);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        int[][] mapa = new int[filas.size()][];
        for (int i = 0; i < filas.size(); i++) {
            mapa[i] = filas.get(i);
        }

        return mapa;
    }

}
