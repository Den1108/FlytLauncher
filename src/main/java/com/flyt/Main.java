package com.flyt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.Properties;
import com.formdev.flatlaf.FlatDarkLaf;

public class Main extends JFrame {
    private JTextField nicknameField;
    
    // Определяем пути: папка .FlytLauncher в домашнем каталоге пользователя
    private final String WORK_DIR = System.getProperty("user.home") + File.separator + ".FlytLauncher";
    private final String CONFIG_FILE = WORK_DIR + File.separator + "launcher_config.properties";

    public Main() {
        // 1. Проверяем и создаем рабочую папку перед запуском интерфейса
        ensureWorkDirExists();

        // Настройки окна
        setTitle("FlytLauncher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Главный контейнер
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // --- Боковая панель (Sidebar) ---
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

        // --- Центральная область (Content) ---
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(45, 45, 45));
        JLabel welcomeText = new JLabel("Добро пожаловать в FlytLauncher", SwingConstants.CENTER);
        welcomeText.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        welcomeText.setForeground(Color.LIGHT_GRAY);
        content.add(welcomeText, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // --- Нижняя панель (Footer) ---
        JPanel footer = new JPanel(new BorderLayout());
        footer.setPreferredSize(new Dimension(0, 100));
        footer.setBackground(new Color(30, 30, 30));
        footer.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Элементы управления
        nicknameField = new JTextField();
        nicknameField.setPreferredSize(new Dimension(150, 30));
        
        // Загружаем ник из конфига
        loadConfig();

        JComboBox<String> versionSelect = new JComboBox<>(new String[]{"1.20.1", "1.16.5", "1.12.2"});
        versionSelect.setPreferredSize(new Dimension(120, 30));

        JButton playButton = new JButton("ИГРАТЬ");
        playButton.setBackground(new Color(40, 167, 69));
        playButton.setForeground(Color.WHITE);
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playButton.setPreferredSize(new Dimension(180, 45));

        // Логика кнопки "Играть"
        playButton.addActionListener(e -> {
            saveConfig();
            JOptionPane.showMessageDialog(this, "Запуск игры для: " + nicknameField.getText() + 
                                          "\nФайлы сохраняются в: " + WORK_DIR);
        });

        // Панель для группировки ника, версии и кнопки
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 25));
        controls.setOpaque(false);
        
        JLabel nickLabel = new JLabel("Ник:");
        nickLabel.setForeground(Color.WHITE);
        controls.add(nickLabel);
        controls.add(nicknameField);
        
        JLabel verLabel = new JLabel("Версия:");
        verLabel.setForeground(Color.WHITE);
        controls.add(verLabel);
        controls.add(versionSelect);
        
        controls.add(playButton);

        footer.add(controls, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
    }

    // Создание рабочей папки
    private void ensureWorkDirExists() {
        File dir = new File(WORK_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Сохранение ника в файл
    private void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("nickname", nicknameField.getText());
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            prop.store(out, "FlytLauncher Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Загрузка ника из файла
    private void loadConfig() {
        Properties prop = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(CONFIG_FILE)) {
                prop.load(in);
                nicknameField.setText(prop.getProperty("nickname", "Player"));
            } catch (IOException e) {
                nicknameField.setText("Player");
            }
        } else {
            nicknameField.setText("Player");
        }
    }

    public static void main(String[] args) {
        // Установка современной темной темы
        FlatDarkLaf.setup();
        
        // Запуск интерфейса
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}