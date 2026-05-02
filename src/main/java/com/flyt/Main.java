package com.flyt;

import com.flyt.launcher.Downloader;
import com.flyt.launcher.GameLauncher;
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
    private JSlider ramSlider;
    private JTextField jvmField;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JButton playButton;

    // ─── Папка установки: C:\Users\<user>\FlytLauncher ───────────────────────
    private final String BASE_DIR   = System.getProperty("user.home") + File.separator + "FlytLauncher";
    private final String CONFIG_FILE = BASE_DIR + File.separator + "launcher.properties";

    private static final String[] VERSIONS = {
        "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9"
    };

    public Main() {
        ensureDirsExist();

        setTitle("FlytLauncher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(970, 630);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(28, 28, 32));
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        loadConfig();
    }

    // ─── Sidebar ─────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(20, 20, 24));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(24, 12, 20, 12));

        JLabel logo = new JLabel("⬡ FLYT");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(new Color(80, 200, 100));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Launcher");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(100, 100, 110));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(sub);
        sidebar.add(Box.createVerticalStrut(28));

        addNavButton(sidebar, "🏠  Главная",    "home");
        addNavButton(sidebar, "📥  Установка",  "install");
        addNavButton(sidebar, "🧩  Моды",       "mods");
        addNavButton(sidebar, "⚙️  Настройки",  "settings");

        sidebar.add(Box.createVerticalGlue());

        // Папка установки
        JLabel dirLabel = new JLabel("<html><center>📁 " + BASE_DIR + "</center></html>");
        dirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        dirLabel.setForeground(new Color(70, 70, 80));
        dirLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(dirLabel);
        sidebar.add(Box.createVerticalStrut(6));

        JLabel ver = new JLabel("FlytLauncher v1.0.0");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ver.setForeground(new Color(65, 65, 75));
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(ver);

        return sidebar;
    }

    private void addNavButton(JPanel parent, String text, String card) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(32, 32, 38));
        btn.setForeground(new Color(195, 195, 205));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(48, 48, 56)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(32, 32, 38)); }
        });
        btn.addActionListener(e -> cardLayout.show(contentPanel, card));
        parent.add(btn);
        parent.add(Box.createVerticalStrut(5));
    }

    // ─── Content ─────────────────────────────────────────────────────────────
    private JPanel buildContent() {
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(38, 38, 44));

        contentPanel.add(buildHomePage(),     "home");
        contentPanel.add(buildInstallPage(),  "install");
        contentPanel.add(buildPlaceholder("🧩 Моды", "Управление модами появится скоро"), "mods");
        contentPanel.add(buildSettingsPage(), "settings");

        return contentPanel;
    }

    // Главная страница
    private JPanel buildHomePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(new Color(38, 38, 44));
        page.setBorder(new EmptyBorder(28, 28, 20, 28));

        JLabel title = new JLabel("Добро пожаловать в FlytLauncher!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        page.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(18, 0, 0, 0));

        String[][] items = {
            {"🎮 Minecraft 1.20.4",  "Последняя стабильная версия"},
            {"📥 Установка версий",   "Перейди во вкладку «Установка»"},
            {"⚙️ Настройки запуска", "Выдели нужное количество RAM"},
            {"📁 Папка игры",         BASE_DIR}
        };

        for (String[] item : items) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(46, 46, 54));
            card.setBorder(new EmptyBorder(14, 16, 14, 16));

            JLabel t = new JLabel(item[0]);
            t.setFont(new Font("Segoe UI", Font.BOLD, 13));
            t.setForeground(Color.WHITE);

            JLabel d = new JLabel("<html>" + item[1] + "</html>");
            d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            d.setForeground(new Color(140, 140, 150));

            card.add(t, BorderLayout.NORTH);
            card.add(d, BorderLayout.CENTER);
            cards.add(card);
        }

        page.add(cards, BorderLayout.CENTER);
        return page;
    }

    // Страница установки версий
    private JPanel buildInstallPage() {
        JPanel page = new JPanel();
        page.setBackground(new Color(38, 38, 44));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(28, 28, 20, 28));

        JLabel title = new JLabel("📥 Установка версий Minecraft");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(title);
        page.add(Box.createVerticalStrut(6));

        JLabel hint = new JLabel("Файлы сохраняются в: " + BASE_DIR);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(80, 200, 100));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(hint);
        page.add(Box.createVerticalStrut(20));

        // Выбор версии для установки
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Версия для установки: ");
        lbl.setForeground(new Color(180, 180, 190));

        JComboBox<String> installVersionBox = new JComboBox<>(VERSIONS);
        installVersionBox.setPreferredSize(new Dimension(130, 34));

        JButton installBtn = new JButton("Установить");
        installBtn.setBackground(new Color(50, 120, 220));
        installBtn.setForeground(Color.WHITE);
        installBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        installBtn.setPreferredSize(new Dimension(140, 34));
        installBtn.setBorderPainted(false);
        installBtn.setFocusPainted(false);
        installBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Прогресс-бар установки
        JProgressBar installProgress = new JProgressBar(0, 100);
        installProgress.setStringPainted(true);
        installProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        installProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
        installProgress.setVisible(false);
        installProgress.setForeground(new Color(50, 120, 220));

        JLabel installStatus = new JLabel(" ");
        installStatus.setForeground(new Color(150, 150, 160));
        installStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        installStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        installBtn.addActionListener(e -> {
            String ver = (String) installVersionBox.getSelectedItem();
            installBtn.setEnabled(false);
            installProgress.setVisible(true);
            installProgress.setValue(0);

            Downloader dl = new Downloader(BASE_DIR);
            dl.setProgressCallback((pct, msg) -> SwingUtilities.invokeLater(() -> {
                installProgress.setValue(pct);
                installProgress.setString(pct + "%");
                installStatus.setText(msg);
            }));

            new SwingWorker<Void, Void>() {
                String error = null;
                @Override protected Void doInBackground() {
                    try { dl.downloadVersion(ver); }
                    catch (Exception ex) { error = ex.getMessage(); }
                    return null;
                }
                @Override protected void done() {
                    installBtn.setEnabled(true);
                    if (error != null) {
                        installStatus.setText("❌ Ошибка: " + error);
                        installProgress.setForeground(new Color(220, 60, 60));
                    } else {
                        installStatus.setText("✅ Версия " + ver + " установлена!");
                        installProgress.setForeground(new Color(80, 200, 100));
                    }
                }
            }.execute();
        });

        row.add(lbl);
        row.add(installVersionBox);
        row.add(Box.createHorizontalStrut(12));
        row.add(installBtn);

        page.add(row);
        page.add(Box.createVerticalStrut(16));
        page.add(installProgress);
        page.add(Box.createVerticalStrut(6));
        page.add(installStatus);

        return page;
    }

    // Страница настроек
    private JPanel buildSettingsPage() {
        JPanel page = new JPanel();
        page.setBackground(new Color(38, 38, 44));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(28, 28, 20, 28));

        JLabel title = new JLabel("⚙️ Настройки запуска");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(title);
        page.add(Box.createVerticalStrut(22));

        // RAM
        JLabel ramLbl = new JLabel("Выделенная RAM (MB): ");
        ramLbl.setForeground(new Color(180, 180, 190));
        ramLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(ramLbl);

        ramSlider = new JSlider(512, 8192, 2048);
        ramSlider.setMajorTickSpacing(1024);
        ramSlider.setMinorTickSpacing(512);
        ramSlider.setPaintTicks(true);
        ramSlider.setPaintLabels(true);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(new Color(150, 150, 160));
        ramSlider.setMaximumSize(new Dimension(520, 60));
        ramSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ramValue = new JLabel("2048 MB");
        ramValue.setForeground(new Color(80, 200, 100));
        ramValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramSlider.addChangeListener(e -> ramValue.setText(ramSlider.getValue() + " MB"));

        page.add(ramSlider);
        page.add(ramValue);
        page.add(Box.createVerticalStrut(20));

        // JVM аргументы
        JLabel jvmLbl = new JLabel("Дополнительные JVM аргументы:");
        jvmLbl.setForeground(new Color(180, 180, 190));
        jvmLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(jvmLbl);

        jvmField = new JTextField("-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200");
        jvmField.setMaximumSize(new Dimension(540, 32));
        jvmField.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(jvmField);
        page.add(Box.createVerticalStrut(20));

        // Кнопка сохранить
        JButton saveBtn = new JButton("💾 Сохранить настройки");
        saveBtn.setBackground(new Color(50, 120, 220));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            saveConfig();
            JOptionPane.showMessageDialog(this, "Настройки сохранены!", "✅", JOptionPane.INFORMATION_MESSAGE);
        });
        page.add(saveBtn);

        return page;
    }

    private JPanel buildPlaceholder(String t, String d) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(38, 38, 44));
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        JLabel tl = new JLabel(t); tl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        tl.setForeground(new Color(90, 90, 100)); tl.setAlignmentX(CENTER_ALIGNMENT);
        JLabel dl = new JLabel(d); dl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dl.setForeground(new Color(70, 70, 80)); dl.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(tl); inner.add(Box.createVerticalStrut(8)); inner.add(dl);
        p.add(inner);
        return p;
    }

    // ─── Footer ──────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(18, 18, 22));
        footer.setPreferredSize(new Dimension(0, 112));
        footer.setBorder(new EmptyBorder(8, 20, 8, 20));

        // Статус + прогресс
        JPanel statusPanel = new JPanel();
        statusPanel.setOpaque(false);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        statusLabel = new JLabel("Готово к запуску");
        statusLabel.setForeground(new Color(110, 110, 120));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setForeground(new Color(80, 200, 100));
        progressBar.setBackground(new Color(45, 45, 55));
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);

        statusPanel.add(statusLabel);
        statusPanel.add(Box.createVerticalStrut(4));
        statusPanel.add(progressBar);
        footer.add(statusPanel, BorderLayout.NORTH);

        // Контролы
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        controls.setOpaque(false);

        nicknameField = new JTextField("Player");
        nicknameField.setPreferredSize(new Dimension(160, 34));
        nicknameField.setToolTipText("Ваш никнейм в игре (только a-z, 0-9, _)");

        versionSelect = new JComboBox<>(VERSIONS);
        versionSelect.setPreferredSize(new Dimension(120, 34));

        playButton = new JButton("▶  ИГРАТЬ");
        playButton.setBackground(new Color(40, 167, 69));
        playButton.setForeground(Color.WHITE);
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        playButton.setPreferredSize(new Dimension(160, 44));
        playButton.setBorderPainted(false);
        playButton.setFocusPainted(false);
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (playButton.isEnabled()) playButton.setBackground(new Color(50, 190, 80)); }
            public void mouseExited(MouseEvent e)  { playButton.setBackground(new Color(40, 167, 69)); }
        });
        playButton.addActionListener(e -> onPlay());

        JLabel nl = new JLabel("Ник:");     nl.setForeground(new Color(155, 155, 165));
        JLabel vl = new JLabel("Версия:");  vl.setForeground(new Color(155, 155, 165));

        controls.add(nl); controls.add(nicknameField);
        controls.add(vl); controls.add(versionSelect);
        controls.add(playButton);
        footer.add(controls, BorderLayout.CENTER);

        return footer;
    }

    // ─── Логика кнопки ИГРАТЬ ────────────────────────────────────────────────
    private void onPlay() {
        String nick    = nicknameField.getText().trim();
        String version = (String) versionSelect.getSelectedItem();

        if (nick.length() < 3 || !nick.matches("[a-zA-Z0-9_]+")) {
            JOptionPane.showMessageDialog(this,
                "Никнейм: минимум 3 символа, только латиница/цифры/_",
                "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveConfig();
        playButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);

        int ramMB   = ramSlider  != null ? ramSlider.getValue()  : 2048;
        String jvm  = jvmField   != null ? jvmField.getText()    : "";

        new SwingWorker<Process, Object>() {
            String error = null;

            @Override
            protected Process doInBackground() {
                try {
                    // Сначала проверяем / скачиваем файлы
                    Downloader dl = new Downloader(BASE_DIR);
                    dl.setProgressCallback((pct, msg) -> SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(pct);
                        statusLabel.setText(msg);
                    }));
                    dl.downloadVersion(version);

                    // Запускаем игру
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Запуск Minecraft " + version + "..."));
                    GameLauncher launcher = new GameLauncher(BASE_DIR);
                    return launcher.launch(version, nick, ramMB, jvm, (pct, msg) ->
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(pct);
                                statusLabel.setText(msg);
                            }));

                } catch (Exception ex) {
                    error = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                playButton.setEnabled(true);
                if (error != null) {
                    statusLabel.setText("❌ " + error);
                    JOptionPane.showMessageDialog(Main.this,
                        "Ошибка запуска:\n" + error, "Ошибка", JOptionPane.ERROR_MESSAGE);
                } else {
                    statusLabel.setText("✅ Minecraft " + version + " запущен!");
                }
            }
        }.execute();
    }

    // ─── Конфиг ──────────────────────────────────────────────────────────────
    private void ensureDirsExist() {
        // Создаём все нужные папки внутри ~/FlytLauncher
        for (String sub : new String[]{"", "versions", "libraries", "assets", "natives", "mods", "saves", "logs"}) {
            new File(BASE_DIR + (sub.isEmpty() ? "" : File.separator + sub)).mkdirs();
        }
    }

    private void saveConfig() {
        Properties p = new Properties();
        p.setProperty("nickname", nicknameField.getText().trim());
        p.setProperty("version",  (String) versionSelect.getSelectedItem());
        if (ramSlider != null) p.setProperty("ram", String.valueOf(ramSlider.getValue()));
        if (jvmField  != null) p.setProperty("jvm", jvmField.getText());
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            p.store(out, "FlytLauncher Settings");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadConfig() {
        Properties p = new Properties();
        File f = new File(CONFIG_FILE);
        if (!f.exists()) return;
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            p.load(in);
            nicknameField.setText(p.getProperty("nickname", "Player"));
            String savedVer = p.getProperty("version", VERSIONS[0]);
            for (int i = 0; i < VERSIONS.length; i++) {
                if (VERSIONS[i].equals(savedVer)) { versionSelect.setSelectedIndex(i); break; }
            }
            if (ramSlider != null)
                ramSlider.setValue(Integer.parseInt(p.getProperty("ram", "2048")));
            if (jvmField != null)
                jvmField.setText(p.getProperty("jvm", "-XX:+UseG1GC -XX:+ParallelRefProcEnabled"));
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
