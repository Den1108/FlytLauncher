package com.flyt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import com.formdev.flatlaf.FlatDarkLaf;

public class Main extends JFrame {

    public Main() {
        setTitle("FlytLauncher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Основной контейнер
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // 1. Боковая панель (Sidebar)
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(35, 35, 35));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel logo = new JLabel("FLYT");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(30));

        String[] menuItems = {"Библиотека", "Моды", "Настройки"};
        for (String item : menuItems) {
            JButton btn = new JButton(item);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(10));
        }

        root.add(sidebar, BorderLayout.WEST);

        // 2. Центральная часть (Main Content)
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(45, 45, 45));
        
        JLabel welcomeText = new JLabel("Добро пожаловать в FlytLauncher", SwingConstants.CENTER);
        welcomeText.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        content.add(welcomeText, BorderLayout.CENTER);

        root.add(content, BorderLayout.CENTER);

        // 3. Нижняя панель (Footer / Play Bar)
        JPanel footer = new JPanel(new BorderLayout());
        footer.setPreferredSize(new Dimension(0, 80));
        footer.setBackground(new Color(30, 30, 30));
        footer.setBorder(new EmptyBorder(10, 20, 10, 20));

        JComboBox<String> versionSelect = new JComboBox<>(new String[]{"1.20.1 (Forge)", "1.20.1 (Fabric)", "1.12.2"});
        versionSelect.setPreferredSize(new Dimension(150, 30));

        JButton playButton = new JButton("ИГРАТЬ");
        playButton.setBackground(new Color(40, 167, 69));
        playButton.setForeground(Color.WHITE);
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playButton.setPreferredSize(new Dimension(200, 50));

        JPanel playControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        playControls.setOpaque(false);
        playControls.add(versionSelect);
        playControls.add(playButton);

        footer.add(playControls, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup(); // Включаем крутую темную тему
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}