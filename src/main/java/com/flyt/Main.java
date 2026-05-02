package com.flyt;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Properties;

public class Main extends JFrame {

    private JTextField nicknameField;
    private JComboBox<String> versionSelect;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JPanel contentPanel; // Центральная область с CardLayout
    private CardLayout cardLayout;

    private final String WORK_DIR   = System.getProperty("user.home") + File.separator + ".FlytLauncher";
    private final String CONFIG_FILE = WORK_DIR + File.separator + "launcher_config.properties";

    // Доступные версии Minecraft
    private static final String[] VERSIONS = {
        "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9"
    };

    public Main() {
        ensureWorkDirExists();

        FlatDarkLaf.setup();

        setTitle("FlytLauncher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 620);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        setUndecorated(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(28, 28, 32));
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        loadConfig();
    }

    // ─── Боковая панель ───────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(22, 22, 26));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(24, 12, 20, 12));

        // Логотип
        JLabel logo = new JLabel("⬡ FLYT");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(new Color(100, 200, 100));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);

        JLabel subtitle = new JLabel("Launcher");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(120, 120, 130));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(subtitle);

        sidebar.add(Box.createVerticalStrut(30));

        // Кнопки навигации
        addNavButton(sidebar, "🏠  Главная",    "home");
        addNavButton(sidebar, "📚  Библиотека", "library");
        addNavButton(sidebar, "🧩  Моды",       "mods");
        addNavButton(sidebar, "⚙️  Настройки",  "settings");

        sidebar.add(Box.createVerticalGlue());

        // Версия лаунчера
        JLabel version = new JLabel("v1.0.0");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(80, 80, 90));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(version);

        return sidebar;
    }

    private void addNavButton(JPanel parent, String text, String card) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(35, 35, 40));
        btn.setForeground(new Color(200, 200, 210));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(50, 50, 58)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(35, 35, 40)); }
        });

        btn.addActionListener(e -> cardLayout.show(contentPanel, card));
        parent.add(btn);
        parent.add(Box.createVerticalStrut(6));
    }

    // ─── Центральная область ─────────────────────────────────────────────────
    private JPanel buildContent() {
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(38, 38, 44));

        contentPanel.add(buildHomePage(),     "home");
        contentPanel.add(buildPlaceholder("📚 Библиотека", "Здесь будут ваши миры и сохранения"), "library");
        contentPanel.add(buildPlaceholder("🧩 Моды",       "Управление модами и модпаками"),       "mods");
        contentPanel.add(buildSettingsPage(), "settings");

        return contentPanel;
    }

    private JPanel buildHomePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(new Color(38, 38, 44));
        page.setBorder(new EmptyBorder(30, 30, 20, 30));

        // Заголовок
        JLabel title = new JLabel("Добро пожаловать, Игрок!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        page.add(title, BorderLayout.NORTH);

        // Карточки новостей (заглушка)
        JPanel newsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        newsPanel.setOpaque(false);
        newsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        String[][] news = {
            {"🎮 Minecraft 1.20.4", "Последнее обновление доступно"},
            {"🧱 Новые блоки", "Cherry Blossom и Bamboo биомы"},
            {"🔧 Оптимизация", "Советы по настройке производительности"},
            {"🌍 Сервера", "Найдите свой идеальный сервер"}
        };

        for (String[] item : news) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(48, 48, 56));
            card.setBorder(new EmptyBorder(14, 16, 14, 16));

            JLabel cardTitle = new JLabel(item[0]);
            cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            cardTitle.setForeground(Color.WHITE);

            JLabel cardDesc = new JLabel(item[1]);
            cardDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            cardDesc.setForeground(new Color(150, 150, 160));

            card.add(cardTitle, BorderLayout.NORTH);
            card.add(cardDesc, BorderLayout.CENTER);
            newsPanel.add(card);
        }

        page.add(newsPanel, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildSettingsPage() {
        JPanel page = new JPanel();
        page.setBackground(new Color(38, 38, 44));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(30, 30, 20, 30));

        JLabel title = new JLabel("⚙️ Настройки");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(title);
        page.add(Box.createVerticalStrut(20));

        // RAM ползунок
        JLabel ramLabel = new JLabel("Выделенная RAM (MB):");
        ramLabel.setForeground(new Color(180, 180, 190));
        ramLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(ramLabel);

        JSlider ramSlider = new JSlider(512, 8192, 2048);
        ramSlider.setMajorTickSpacing(1024);
        ramSlider.setPaintTicks(true);
        ramSlider.setPaintLabels(true);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(new Color(150, 150, 160));
        ramSlider.setMaximumSize(new Dimension(500, 60));
        ramSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(ramSlider);

        page.add(Box.createVerticalStrut(20));

        JLabel jvmLabel = new JLabel("Дополнительные JVM аргументы:");
        jvmLabel.setForeground(new Color(180, 180, 190));
        jvmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(jvmLabel);

        JTextField jvmField = new JTextField("-XX:+UseG1GC -XX:+ParallelRefProcEnabled");
        jvmField.setMaximumSize(new Dimension(500, 32));
        jvmField.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(jvmField);

        page.add(Box.createVerticalStrut(20));

        JLabel gameDirLabel = new JLabel("Папка игры: " + WORK_DIR);
        gameDirLabel.setForeground(new Color(100, 200, 100));
        gameDirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gameDirLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(gameDirLabel);

        return page;
    }

    private JPanel buildPlaceholder(String title, String desc) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(new Color(38, 38, 44));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 26));
        t.setForeground(new Color(100, 100, 110));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel d = new JLabel(desc);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        d.setForeground(new Color(80, 80, 90));
        d.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(t);
        inner.add(Box.createVerticalStrut(8));
        inner.add(d);
        page.add(inner);
        return page;
    }

    // ─── Нижняя панель ───────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(20, 20, 24));
        footer.setPreferredSize(new Dimension(0, 110));
        footer.setBorder(new EmptyBorder(8, 20, 8, 20));

        // Прогресс-бар + статус
        JPanel statusPanel = new JPanel();
        statusPanel.setOpaque(false);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        statusLabel = new JLabel("Готово к запуску");
        statusLabel.setForeground(new Color(120, 120, 130));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setForeground(new Color(100, 200, 100));
        progressBar.setBackground(new Color(50, 50, 58));
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);

        statusPanel.add(statusLabel);
        statusPanel.add(Box.createVerticalStrut(4));
        statusPanel.add(progressBar);

        footer.add(statusPanel, BorderLayout.NORTH);

        // Контролы: ник / версия / кнопка
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        controls.setOpaque(false);

        nicknameField = new JTextField("Player");
        nicknameField.setPreferredSize(new Dimension(160, 34));
        nicknameField.setToolTipText("Ваш никнейм в игре");

        versionSelect = new JComboBox<>(VERSIONS);
        versionSelect.setPreferredSize(new Dimension(120, 34));

        JButton playButton = new JButton("▶  ИГРАТЬ");
        playButton.setBackground(new Color(40, 167, 69));
        playButton.setForeground(Color.WHITE);
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        playButton.setPreferredSize(new Dimension(160, 44));
        playButton.setBorderPainted(false);
        playButton.setFocusPainted(false);
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        playButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { playButton.setBackground(new Color(50, 190, 80)); }
            public void mouseExited(MouseEvent e)  { playButton.setBackground(new Color(40, 167, 69)); }
        });

        playButton.addActionListener(e -> onPlay(playButton));

        // Метки
        JLabel nickLabel = new JLabel("Ник:");
        nickLabel.setForeground(new Color(160, 160, 170));

        JLabel verLabel = new JLabel("Версия:");
        verLabel.setForeground(new Color(160, 160, 170));

        controls.add(nickLabel);
        controls.add(nicknameField);
        controls.add(verLabel);
        controls.add(versionSelect);
        controls.add(playButton);

        footer.add(controls, BorderLayout.CENTER);
        return footer;
    }

    // ─── Логика запуска ──────────────────────────────────────────────────────
    private void onPlay(JButton playButton) {
        String nick = nicknameField.getText().trim();

        if (nick.isEmpty() || nick.length() < 3) {
            JOptionPane.showMessageDialog(this,
                "Никнейм должен быть не менее 3 символов!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!nick.matches("[a-zA-Z0-9_]+")) {
            JOptionPane.showMessageDialog(this,
                "Никнейм может содержать только латинские буквы, цифры и _", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveConfig();
        playButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);

        String selectedVersion = (String) versionSelect.getSelectedItem();

        // Симуляция загрузки (здесь будет реальная логика скачивания)
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String[] steps = {
                    "Проверка файлов...", "Загрузка ассетов...",
                    "Загрузка библиотек...", "Запуск игры..."
                };
                for (int i = 0; i <= 100; i += 5) {
                    Thread.sleep(60);
                    publish(i);
                    int idx = Math.min(i / 25, steps.length - 1);
                    SwingUtilities.invokeLater(() -> statusLabel.setText(steps[idx]));
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                progressBar.setValue(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                playButton.setEnabled(true);
                statusLabel.setText("Готово к запуску");

                // TODO: здесь вызов реального запуска Minecraft через ProcessBuilder
                JOptionPane.showMessageDialog(Main.this,
                    "Minecraft " + selectedVersion + " запущен!\nИгрок: " + nick +
                    "\nПапка игры: " + WORK_DIR,
                    "Запуск", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }

    // ─── Конфиг ──────────────────────────────────────────────────────────────
    private void ensureWorkDirExists() {
        new File(WORK_DIR).mkdirs();
    }

    private void saveConfig() {
        Properties p = new Properties();
        p.setProperty("nickname", nicknameField.getText().trim());
        p.setProperty("version",  (String) versionSelect.getSelectedItem());
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            p.store(out, "FlytLauncher Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        Properties p = new Properties();
        File f = new File(CONFIG_FILE);
        if (f.exists()) {
            try (InputStream in = new FileInputStream(CONFIG_FILE)) {
                p.load(in);
                nicknameField.setText(p.getProperty("nickname", "Player"));
                String savedVer = p.getProperty("version", VERSIONS[0]);
                for (int i = 0; i < VERSIONS.length; i++) {
                    if (VERSIONS[i].equals(savedVer)) { versionSelect.setSelectedIndex(i); break; }
                }
            } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
