/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

/**
 *
 * @author jsero
 */
import Cliente.ClienteJuego;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WaitingRoom extends JPanel {
    
    //Botones y varas que va a ir teniendo el Waiting Room
    private JComboBox<String> cbxMapas;
    private String[] mapasDisponibles;
    private final JList<String> playerList;
    private final DefaultListModel<String> listModel;
    private final JButton startButton;
    private final JLabel adminLabel;
    private final boolean isAdmin;
    private final ClienteJuego cliente;

    //Aca se crea de una forma u otra dependiendo de si el jugador que entra es el admin o no
    public WaitingRoom(boolean isAdmin, ClienteJuego cliente) {
        this.isAdmin = isAdmin;
        this.cliente = cliente;
        setLayout(new BorderLayout(10, 10));

        //Panel principal
        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new BoxLayout(panelSuperior, BoxLayout.Y_AXIS));

        //Label que indica si es admin o no
        adminLabel = new JLabel(isAdmin ? "Eres el admin" : "Esperando a que el admin inicie...");
        adminLabel.setAlignmentX(CENTER_ALIGNMENT);
        panelSuperior.add(adminLabel);

        //Combo de mapas solo para admin
        if (isAdmin) {
            mapasDisponibles = obtenerMapasDisponibles();
            cbxMapas = new JComboBox<>(mapasDisponibles);
            cbxMapas.setAlignmentX(CENTER_ALIGNMENT);

            JLabel mapaLabel = new JLabel("Selecciona mapa:");
            mapaLabel.setAlignmentX(CENTER_ALIGNMENT);

            panelSuperior.add(Box.createVerticalStrut(10));
            panelSuperior.add(mapaLabel);
            panelSuperior.add(cbxMapas);
        }

        //Boton de iniciar juego
        startButton = new JButton("Iniciar Juego");
        startButton.setVisible(isAdmin);
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        startButton.addActionListener(e -> {
            if (isAdmin) {
                cliente.enviarInicioJuego();
            }
        });
        panelSuperior.add(Box.createVerticalStrut(10));
        panelSuperior.add(startButton);

        //Lista de jugadores (debo ver como hacer que se actualice con solo entrar porque solo se actualiza cuando entra gente
        listModel = new DefaultListModel<>();
        playerList = new JList<>(listModel);
        playerList.setBorder(BorderFactory.createTitledBorder("Jugadores"));

        //Add componentes al principal
        add(panelSuperior, BorderLayout.NORTH);
        add(new JScrollPane(playerList), BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(300, 400));
    }
    
    //Actualiza la lista de jugadores del server
    public void updatePlayerList(List<String> players) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String player : players) {
                listModel.addElement(player);
            }
        });
    }
    
    //Agrega los mapas que esten en la carpeta de mapas al cbx
    public String[] obtenerMapasDisponibles() {
        File carpeta = new File("maps");
        File[] archivos = carpeta.listFiles((dir, name) -> name.endsWith(".txt"));
        if (archivos == null || archivos.length == 0) {
            return new String[]{"mapa.txt"};
        }

        String[] nombres = new String[archivos.length];
        for (int i = 0; i < archivos.length; i++) {
            nombres[i] = archivos[i].getName();
        }
        return nombres;
    }
    
    //Retorna el nombre del mapa seleccionado, is no simplemente un "mapa.txt"
    public String getMapaSeleccionado() {
        if (cbxMapas != null && cbxMapas.getSelectedItem() != null) {
            return cbxMapas.getSelectedItem().toString();
        }
        return "mapa.txt";
    }
}
