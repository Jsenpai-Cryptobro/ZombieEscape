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
import java.util.ArrayList;
import java.util.List;

public class WaitingRoom extends JPanel {

    private final JList<String> playerList;
    private final DefaultListModel<String> listModel;
    private final JButton startButton;
    private final JLabel adminLabel;
    private final boolean isAdmin;
    private final ClienteJuego cliente;

    public WaitingRoom(boolean isAdmin, ClienteJuego cliente) {
        this.isAdmin = isAdmin;
        this.cliente = cliente;
        setLayout(new BorderLayout(10, 10));

        // Create player list
        listModel = new DefaultListModel<>();
        playerList = new JList<>(listModel);
        playerList.setBorder(BorderFactory.createTitledBorder("Players"));

        // Create admin indicator
        adminLabel = new JLabel(isAdmin ? "You are the admin" : "Waiting for admin to start...");
        adminLabel.setHorizontalAlignment(JLabel.CENTER);

        // Create start button (only visible to admin)
        startButton = new JButton("Start Game");
        startButton.setVisible(isAdmin);
        startButton.addActionListener(e -> {
            if (isAdmin) {
                cliente.enviarInicioJuego();
            }
        });

        // Add components
        add(new JScrollPane(playerList), BorderLayout.CENTER);
        add(adminLabel, BorderLayout.NORTH);
        add(startButton, BorderLayout.SOUTH);

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(300, 400));
    }

    public void updatePlayerList(List<String> players) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String player : players) {
                listModel.addElement(player);
            }
        });
    }

    public void setPlayerReady(String playerName) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).equals(playerName)) {
                    listModel.set(i, playerName + " (Ready)");
                    break;
                }
            }
        });
    }
}
