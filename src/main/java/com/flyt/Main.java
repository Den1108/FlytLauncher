package com.flyt;

import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;

public class Main {
    public static void main(String[] args) {
        // Включаем темную тему
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("FlytLauncher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        
        JLabel label = new JLabel("FlytLauncher готов к разработке!", SwingConstants.CENTER);
        frame.add(label);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}