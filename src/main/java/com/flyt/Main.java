package com.flyt;

import com.flyt.launcher.Downloader;
import com.flyt.launcher.GameLauncher;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.Properties;

public class Main extends JFrame {

    // ─── Цветовая палитра ────────────────────────────────────────────────────
    private static final Color BG_DEEP    = new Color(10,  12,  16);
    private static final Color BG_PANEL   = new Color(16,  19,  26);
    private static final Color BG_CARD    = new Color(22,  27,  38);
    private static final Color BG_HOVER   = new Color(30,  37,  52);
    private static final Color BG_INPUT   = new Color(18,  22,  32);
    private static final Color ACCENT     = new Color(82,  196, 122);
    private static final Color ACCENT2    = new Color(56,  139, 253);
    private static final Color TEXT_PRI   = new Color(230, 235, 245);
    private static final Color TEXT_SEC   = new Color(120, 135, 160);
    private static final Color TEXT_DIM   = new Color(65,  75,  95);
    private static final Color BORDER_COL = new Color(32,  40,  58);
    private static final Color DANGER     = new Color(240, 80,  80);

    private JTextField  nicknameField;
    private JComboBox<String> versionSelect;
    private JProgressBar progressBar;
    private JLabel  statusLabel;
    private JSlider ramSlider;
    private JLabel  ramValueLabel;
    private JTextField jvmField;
    private JPanel  contentPanel;
    private CardLayout cardLayout;
    private JButton playButton;
    private String  activeCard = "home";

    private final String BASE_DIR    = System.getProperty("user.home") + File.separator + "FlytLauncher";
    private final String CONFIG_FILE = BASE_DIR + File.separator + "launcher.properties";

    private static final String[] VERSIONS = {
        "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9"
    };

    public Main() {
        ensureDirsExist();
        setTitle("FlytLauncher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 660);
        setMinimumSize(new Dimension(860, 580));
        setLocationRelativeTo(null);
        setBackground(BG_DEEP);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DEEP);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setBackground(BG_DEEP);
        right.add(buildContent(), BorderLayout.CENTER);
        right.add(buildFooter(), BorderLayout.SOUTH);
        root.add(right, BorderLayout.CENTER);

        loadConfig();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BORDER_COL);
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                GradientPaint gp = new GradientPaint(0, 0, new Color(82, 196, 122, 20), 0, 120, new Color(82, 196, 122, 0));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), 120);
                g2.dispose();
            }
        };
        sidebar.setBackground(BG_PANEL);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(28, 16, 24, 16));

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel iconLbl = new JLabel("◈");
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 26));
        iconLbl.setForeground(ACCENT);
        JLabel logoTxt = new JLabel(" FLYT");
        logoTxt.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoTxt.setForeground(TEXT_PRI);
        logoPanel.add(iconLbl);
        logoPanel.add(logoTxt);
        sidebar.add(logoPanel);

        JLabel tagline = new JLabel("Minecraft Launcher");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tagline.setForeground(TEXT_DIM);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(28));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(14));

        addNavBtn(sidebar, "home",     "⌂",  "Главная");
        addNavBtn(sidebar, "install",  "↓",  "Установка");
        addNavBtn(sidebar, "mods",     "⧉",  "Моды");
        addNavBtn(sidebar, "settings", "⚙",  "Настройки");

        sidebar.add(Box.createVerticalGlue());

        // Dir block
        JPanel dirBlock = new JPanel(new BorderLayout(8, 4));
        dirBlock.setBackground(new Color(13, 16, 22));
        dirBlock.setBorder(new EmptyBorder(10, 12, 10, 12));
        dirBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        dirBlock.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel dirIcon = new JLabel("📁");
        dirIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        JPanel dirTxt = new JPanel(new GridLayout(2, 1, 0, 2));
        dirTxt.setOpaque(false);
        JLabel dirTitle = new JLabel("Папка игры");
        dirTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        dirTitle.setForeground(TEXT_SEC);
        JLabel dirPath = new JLabel(BASE_DIR.replace(System.getProperty("user.home"), "~"));
        dirPath.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dirPath.setForeground(TEXT_DIM);
        dirPath.setToolTipText(BASE_DIR);
        dirTxt.add(dirTitle);
        dirTxt.add(dirPath);
        dirBlock.add(dirIcon, BorderLayout.WEST);
        dirBlock.add(dirTxt, BorderLayout.CENTER);
        sidebar.add(dirBlock);
        sidebar.add(Box.createVerticalStrut(8));

        JLabel ver = new JLabel("v1.0.0");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ver.setForeground(TEXT_DIM);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(ver);

        return sidebar;
    }

    private void addNavBtn(JPanel parent, String card, String icon, String label) {
        JPanel btn = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = activeCard.equals(card);
                if (active) {
                    g2.setColor(new Color(82, 196, 122, 22));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                } else if (getClientProperty("hover") != null) {
                    g2.setColor(BG_HOVER);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconL = new JLabel(icon);
        iconL.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        iconL.setForeground(activeCard.equals(card) ? ACCENT : TEXT_SEC);
        JLabel textL = new JLabel(label);
        textL.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textL.setForeground(activeCard.equals(card) ? TEXT_PRI : TEXT_SEC);
        btn.add(iconL, BorderLayout.WEST);
        btn.add(textL, BorderLayout.CENTER);
        btn.putClientProperty("card", card);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.putClientProperty("hover", true); btn.repaint(); }
            public void mouseExited(MouseEvent e)  { btn.putClientProperty("hover", null); btn.repaint(); }
            public void mouseClicked(MouseEvent e) {
                activeCard = card;
                cardLayout.show(contentPanel, card);
                // Перекрасить все nav-кнопки
                for (Component c : parent.getComponents()) {
                    if (!(c instanceof JPanel)) continue;
                    JPanel p = (JPanel) c;
                    Object cc = p.getClientProperty("card");
                    if (cc == null) continue;
                    boolean isActive = cc.equals(card);
                    for (Component ch : p.getComponents()) {
                        if (ch instanceof JLabel) {
                            String txt = ((JLabel) ch).getText();
                            if (txt.equals(icon) || txt.length() <= 2) {
                                ch.setForeground(isActive ? ACCENT : TEXT_SEC);
                            } else {
                                ch.setForeground(isActive ? TEXT_PRI : TEXT_SEC);
                            }
                        }
                    }
                    p.repaint();
                }
            }
        });
        parent.add(btn);
        parent.add(Box.createVerticalStrut(4));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONTENT
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildContent() {
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_DEEP);
        contentPanel.add(buildHomePage(),     "home");
        contentPanel.add(buildInstallPage(),  "install");
        contentPanel.add(buildPlaceholder("Моды", "Появится в следующем обновлении"), "mods");
        contentPanel.add(buildSettingsPage(), "settings");
        return contentPanel;
    }

    private JPanel buildHomePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG_DEEP);
        page.setBorder(new EmptyBorder(32, 32, 24, 32));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Добро пожаловать");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRI);
        JLabel subtitle = new JLabel("Выбери версию внизу и нажми ИГРАТЬ");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SEC);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        page.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(24, 0, 0, 0));
        grid.add(makeInfoCard("1.20.4", "Последняя стабильная версия", ACCENT, "◈ Актуальная"));
        grid.add(makeInfoCard("Установка", "Перейди во вкладку «Установка» для загрузки других версий", ACCENT2, "↓ Скачать"));
        grid.add(makeInfoCard("RAM", "Настрой выделенную память в разделе «Настройки»", new Color(180, 120, 255), "⚙ Производительность"));
        grid.add(makeInfoCard("Папка игры", BASE_DIR.replace(System.getProperty("user.home"), "~"), new Color(240, 160, 60), "📁 Хранилище"));
        page.add(grid, BorderLayout.CENTER);
        return page;
    }

    private JPanel makeInfoCard(String title, String desc, Color accent, String badge) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 3, 3, 3);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 18, 18));
        JLabel badgeLbl = new JLabel(badge);
        badgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badgeLbl.setForeground(accent);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(TEXT_PRI);
        JLabel descLbl = new JLabel("<html><body style='width:160px'>" + desc + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(TEXT_SEC);
        card.add(badgeLbl, BorderLayout.NORTH);
        card.add(titleLbl, BorderLayout.CENTER);
        card.add(descLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildInstallPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG_DEEP);
        page.setBorder(new EmptyBorder(32, 32, 24, 32));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Установка версий");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("Файлы сохраняются в " + BASE_DIR);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 180));
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.CENTER);
        page.add(header, BorderLayout.NORTH);

        JPanel installCard = new JPanel(new BorderLayout(0, 20)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
            }
        };
        installCard.setOpaque(false);
        installCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel selectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        selectRow.setOpaque(false);
        JLabel verLbl = new JLabel("Версия:");
        verLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        verLbl.setForeground(TEXT_SEC);
        JComboBox<String> installBox = new JComboBox<>(VERSIONS);
        installBox.setPreferredSize(new Dimension(140, 36));
        JButton installBtn = makeButton("  Установить  ", ACCENT2, 36);
        selectRow.add(verLbl);
        selectRow.add(installBox);
        selectRow.add(installBtn);
        installCard.add(selectRow, BorderLayout.NORTH);

        JPanel progPanel = new JPanel(new BorderLayout(0, 10));
        progPanel.setOpaque(false);
        JProgressBar installProg = new JProgressBar(0, 100);
        installProg.setPreferredSize(new Dimension(0, 5));
        installProg.setStringPainted(false);
        installProg.setForeground(ACCENT2);
        installProg.setBackground(BORDER_COL);
        installProg.setBorderPainted(false);
        JLabel installStatus = new JLabel("Выбери версию и нажми «Установить»");
        installStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        installStatus.setForeground(TEXT_SEC);
        progPanel.add(installProg, BorderLayout.NORTH);
        progPanel.add(installStatus, BorderLayout.CENTER);
        installCard.add(progPanel, BorderLayout.CENTER);

        installBtn.addActionListener(e -> {
            String ver = (String) installBox.getSelectedItem();
            installBtn.setEnabled(false);
            installProg.setValue(0);
            Downloader dl = new Downloader(BASE_DIR);
            dl.setProgressCallback((pct, msg) -> SwingUtilities.invokeLater(() -> {
                installProg.setValue(pct);
                installStatus.setText(msg);
                installStatus.setForeground(TEXT_SEC);
            }));
            new SwingWorker<Void, Void>() {
                String err = null;
                @Override protected Void doInBackground() {
                    try { dl.downloadVersion(ver); } catch (Exception ex) { err = ex.getMessage(); }
                    return null;
                }
                @Override protected void done() {
                    installBtn.setEnabled(true);
                    if (err != null) {
                        installStatus.setText("Ошибка: " + err);
                        installStatus.setForeground(DANGER);
                        installProg.setForeground(DANGER);
                    } else {
                        installStatus.setText("✓  Версия " + ver + " установлена");
                        installStatus.setForeground(ACCENT);
                        installProg.setForeground(ACCENT);
                        installProg.setValue(100);
                    }
                }
            }.execute();
        });

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(20, 0, 0, 0));
        center.add(installCard, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildSettingsPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG_DEEP);
        page.setBorder(new EmptyBorder(32, 32, 24, 32));
        JLabel title = new JLabel("Настройки запуска");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_PRI);
        page.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(24, 0, 0, 0));

        form.add(sectionLabel("ПАМЯТЬ JAVA (RAM)"));
        form.add(Box.createVerticalStrut(10));

        JPanel ramRow = new JPanel(new BorderLayout(16, 0));
        ramRow.setOpaque(false);
        ramRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        ramRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramSlider = new JSlider(512, 8192, 2048);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(ACCENT);
        ramValueLabel = new JLabel("2048 MB");
        ramValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ramValueLabel.setForeground(ACCENT);
        ramValueLabel.setPreferredSize(new Dimension(90, 30));
        ramSlider.addChangeListener(e -> ramValueLabel.setText(ramSlider.getValue() + " MB"));
        ramRow.add(ramSlider, BorderLayout.CENTER);
        ramRow.add(ramValueLabel, BorderLayout.EAST);
        form.add(ramRow);
        form.add(Box.createVerticalStrut(24));

        form.add(sectionLabel("JVM АРГУМЕНТЫ"));
        form.add(Box.createVerticalStrut(10));
        jvmField = new JTextField("-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200");
        jvmField.setFont(new Font("Consolas", Font.PLAIN, 12));
        jvmField.setForeground(TEXT_PRI);
        jvmField.setBackground(BG_INPUT);
        jvmField.setCaretColor(ACCENT);
        jvmField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            new EmptyBorder(8, 12, 8, 12)));
        jvmField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        jvmField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(jvmField);
        form.add(Box.createVerticalStrut(28));

        JButton saveBtn = makeButton("  Сохранить  ", ACCENT2, 38);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> {
            saveConfig();
            statusLabel.setText("✓  Настройки сохранены");
            statusLabel.setForeground(ACCENT);
        });
        form.add(saveBtn);

        page.add(form, BorderLayout.CENTER);
        return page;
    }

    private JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel buildPlaceholder(String t, String d) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_DEEP);
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel("⧉");
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 52));
        icon.setForeground(new Color(35, 45, 65));
        icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel tl = new JLabel(t);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tl.setForeground(new Color(55, 68, 90));
        tl.setAlignmentX(CENTER_ALIGNMENT);
        JLabel dl = new JLabel(d);
        dl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dl.setForeground(new Color(45, 55, 75));
        dl.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(icon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(tl);
        inner.add(Box.createVerticalStrut(6));
        inner.add(dl);
        p.add(inner);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FOOTER
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BORDER_COL);
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        };
        footer.setBackground(BG_PANEL);
        footer.setPreferredSize(new Dimension(0, 90));
        footer.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Статус
        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setOpaque(false);
        statusLabel = new JLabel("Готово к запуску");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_SEC);
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(0, 4));
        progressBar.setStringPainted(false);
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(BORDER_COL);
        progressBar.setBorderPainted(false);
        progressBar.setValue(0);
        progressBar.setVisible(false);
        left.add(statusLabel, BorderLayout.CENTER);
        left.add(progressBar, BorderLayout.SOUTH);

        // Контролы
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        controls.setOpaque(false);

        JPanel nickWrap = labeledField("НИК");
        nicknameField = new JTextField("Player");
        nicknameField.setPreferredSize(new Dimension(155, 36));
        nicknameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nicknameField.setForeground(TEXT_PRI);
        nicknameField.setBackground(BG_INPUT);
        nicknameField.setCaretColor(ACCENT);
        nicknameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            new EmptyBorder(4, 10, 4, 10)));
        nickWrap.add(nicknameField);

        JPanel verWrap = labeledField("ВЕРСИЯ");
        versionSelect = new JComboBox<>(VERSIONS);
        versionSelect.setPreferredSize(new Dimension(120, 36));
        verWrap.add(versionSelect);

        playButton = makeButton("▶   ИГРАТЬ", ACCENT, 44);
        playButton.setPreferredSize(new Dimension(155, 44));
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        playButton.addActionListener(e -> onPlay());

        controls.add(nickWrap);
        controls.add(verWrap);
        controls.add(playButton);

        footer.add(left, BorderLayout.CENTER);
        footer.add(controls, BorderLayout.EAST);
        return footer;
    }

    private JPanel labeledField(String label) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(TEXT_DIM);
        p.add(l, BorderLayout.NORTH);
        return p;
    }

    private JButton makeButton(String text, Color accent, int height) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = !isEnabled() ? new Color(45, 55, 70)
                           : getModel().isPressed()  ? accent.darker()
                           : getModel().isRollover() ? accent.brighter()
                           : accent;
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(isEnabled() ? Color.WHITE : TEXT_DIM);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LAUNCH
    // ══════════════════════════════════════════════════════════════════════════
    private void onPlay() {
        String nick    = nicknameField.getText().trim();
        String version = (String) versionSelect.getSelectedItem();

        if (nick.length() < 3 || !nick.matches("[a-zA-Z0-9_]+")) {
            JOptionPane.showMessageDialog(this,
                "Никнейм: минимум 3 символа, только A-Z, 0-9, _",
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        saveConfig();
        playButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setForeground(ACCENT);

        int    ramMB = ramSlider != null ? ramSlider.getValue() : 2048;
        String jvm   = jvmField  != null ? jvmField.getText()  : "";

        new SwingWorker<Process, Object>() {
            String error = null;
            @Override protected Process doInBackground() {
                try {
                    Downloader dl = new Downloader(BASE_DIR);
                    dl.setProgressCallback((pct, msg) -> SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(pct);
                        statusLabel.setText(msg);
                        statusLabel.setForeground(TEXT_SEC);
                    }));
                    dl.downloadVersion(version);
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Запуск Minecraft " + version + "…"));
                    GameLauncher launcher = new GameLauncher(BASE_DIR);
                    return launcher.launch(version, nick, ramMB, jvm, (pct, msg) ->
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(pct);
                                statusLabel.setText(msg);
                            }));
                } catch (Exception ex) { error = ex.getMessage(); return null; }
            }
            @Override protected void done() {
                progressBar.setVisible(false);
                playButton.setEnabled(true);
                if (error != null) {
                    statusLabel.setText("Ошибка: " + error);
                    statusLabel.setForeground(DANGER);
                    JOptionPane.showMessageDialog(Main.this,
                        "Ошибка запуска:\n" + error, "Ошибка", JOptionPane.ERROR_MESSAGE);
                } else {
                    statusLabel.setText("✓  Minecraft " + version + " запущен  •  " + nick);
                    statusLabel.setForeground(ACCENT);
                }
            }
        }.execute();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONFIG
    // ══════════════════════════════════════════════════════════════════════════
    private void ensureDirsExist() {
        for (String s : new String[]{"","versions","libraries","assets","natives","mods","saves","runtime"})
            new File(BASE_DIR + (s.isEmpty() ? "" : File.separator + s)).mkdirs();
    }

    private void saveConfig() {
        Properties p = new Properties();
        p.setProperty("nickname", nicknameField.getText().trim());
        p.setProperty("version",  (String) versionSelect.getSelectedItem());
        if (ramSlider != null) p.setProperty("ram", String.valueOf(ramSlider.getValue()));
        if (jvmField  != null) p.setProperty("jvm", jvmField.getText());
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) { p.store(out, "FlytLauncher"); }
        catch (IOException ignored) {}
    }

    private void loadConfig() {
        File f = new File(CONFIG_FILE);
        if (!f.exists()) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            p.load(in);
            nicknameField.setText(p.getProperty("nickname", "Player"));
            String sv = p.getProperty("version", VERSIONS[0]);
            for (int i = 0; i < VERSIONS.length; i++)
                if (VERSIONS[i].equals(sv)) { versionSelect.setSelectedIndex(i); break; }
            if (ramSlider != null) {
                ramSlider.setValue(Integer.parseInt(p.getProperty("ram", "2048")));
                ramValueLabel.setText(ramSlider.getValue() + " MB");
            }
            if (jvmField != null)
                jvmField.setText(p.getProperty("jvm", "-XX:+UseG1GC -XX:+ParallelRefProcEnabled"));
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("ComboBox.background",          new Color(18, 22, 32));
        UIManager.put("ComboBox.foreground",          new Color(200, 210, 230));
        UIManager.put("ComboBox.selectionBackground", new Color(30, 37, 52));
        UIManager.put("Slider.thumbColor",            new Color(82, 196, 122));
        UIManager.put("Slider.trackColor",            new Color(32, 40, 58));
        UIManager.put("ProgressBar.background",       new Color(32, 40, 58));
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
