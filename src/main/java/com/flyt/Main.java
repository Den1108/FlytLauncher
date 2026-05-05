package com.flyt;

import com.flyt.launcher.*;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Properties;

public class Main extends JFrame {

    // ── Палитра ────────────────────────────────────────────
    static final Color C_BG      = new Color(8,   10,  15);
    static final Color C_SURFACE = new Color(14,  17,  25);
    static final Color C_CARD    = new Color(19,  24,  36);
    static final Color C_BORDER  = new Color(38,  48,  72);
    static final Color C_ACCENT  = new Color(72,  213, 130);
    static final Color C_BLUE    = new Color(64,  148, 255);
    static final Color C_ORANGE  = new Color(255, 165, 70);
    static final Color C_PURPLE  = new Color(178, 112, 255);
    static final Color C_DANGER  = new Color(255, 82,  82);
    static final Color C_TEXT    = new Color(225, 232, 248);
    static final Color C_TEXT2   = new Color(128, 145, 178);
    static final Color C_TEXT3   = new Color(62,  75,  105);

    // ── Состояние ─────────────────────────────────────────
    private JTextField   nickField;
    private JComboBox<VersionManager.VersionInfo> versionBox;
    private JComboBox<String> loaderVersionBox;
    private JProgressBar progBar;
    private JLabel       statusLbl;
    private JSlider      ramSlider;
    private JLabel       ramLbl;
    private JTextField   jvmField;
    private JPanel       content;
    private CardLayout   cards;
    private JButton      playBtn;
    private String       activePage = "home";
    private BufferedImage customIcon = null;

    // Модлоадер
    private ModLoader.Type selectedLoader = ModLoader.Type.VANILLA;
    private String         selectedLoaderVer = "";

    // Фильтр версий
    private boolean showSnapshots = false;
    private boolean showLegacy    = false;

    private VersionManager versionManager;
    private ModLoader      modLoader;

    private final String BASE      = System.getProperty("user.home") + File.separator + "FlytLauncher";
    private final String CFG       = BASE + File.separator + "launcher.properties";
    private final String ICON_PATH = BASE + File.separator + "icon.png";

    // ── INIT ──────────────────────────────────────────────
    public Main() {
        mkdirs();
        loadCustomIcon();
        versionManager = new VersionManager(BASE);
        modLoader      = new ModLoader(BASE);

        setTitle("FlytLauncher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(920, 600));
        setLocationRelativeTo(null);
        setBackground(C_BG);
        applyWindowIcon();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        setContentPane(root);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(C_BG);
        right.add(buildContent(), BorderLayout.CENTER);
        right.add(buildFooter(), BorderLayout.SOUTH);
        root.add(right, BorderLayout.CENTER);

        loadCfg();

        // Загружаем версии асинхронно
        new SwingWorker<List<VersionManager.VersionInfo>, Void>() {
            @Override protected List<VersionManager.VersionInfo> doInBackground() {
                return versionManager.getAllVersions();
            }
            @Override protected void done() { refreshVersionBox(); }
        }.execute();
    }

    // ── ICON ──────────────────────────────────────────────
    private void loadCustomIcon() {
        File f = new File(ICON_PATH);
        if (f.exists()) { try { customIcon = ImageIO.read(f); } catch (Exception ignored) {} }
    }

    private void applyWindowIcon() {
        setIconImage(customIcon != null ? customIcon : generateDefaultIcon(64));
    }

    private BufferedImage generateDefaultIcon(int sz) {
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(C_SURFACE);
        g.fill(new RoundRectangle2D.Float(0, 0, sz, sz, sz * 0.28f, sz * 0.28f));
        int cx = sz/2, cy = sz/2, r = (int)(sz * 0.36f);
        int[] px = new int[6], py = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI/6 + i * Math.PI/3;
            px[i] = (int)(cx + r * Math.cos(a));
            py[i] = (int)(cy + r * Math.sin(a));
        }
        g.setColor(C_ACCENT);
        g.setStroke(new BasicStroke(sz * 0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(px, py, 6);
        g.setFont(new Font("Segoe UI", Font.BOLD, (int)(sz * 0.36f)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("F", cx - fm.stringWidth("F")/2, cy + fm.getAscent()/2 - 1);
        g.dispose();
        return img;
    }

    // ── SIDEBAR ───────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel s = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setColor(C_BORDER); g2.fillRect(getWidth()-1, 0, 1, getHeight());
                GradientPaint gp = new GradientPaint(0,0,new Color(72,213,130,22),0,150,new Color(72,213,130,0));
                g2.setPaint(gp); g2.fillRect(0,0,getWidth(),150); g2.dispose();
            }
        };
        s.setBackground(C_SURFACE);
        s.setPreferredSize(new Dimension(228, 0));
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBorder(new EmptyBorder(26, 14, 22, 14));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        logoRow.setOpaque(false); logoRow.setAlignmentX(CENTER_ALIGNMENT);
        logoRow.add(new JLabel(new ImageIcon(customIcon != null
            ? customIcon.getScaledInstance(30,30,Image.SCALE_SMOOTH)
            : generateDefaultIcon(30))));
        JLabel nameL = new JLabel("FLYT");
        nameL.setFont(new Font("Segoe UI", Font.BOLD, 22)); nameL.setForeground(C_TEXT);
        logoRow.add(nameL);
        s.add(logoRow);

        JLabel sub = new JLabel("Minecraft Launcher");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11)); sub.setForeground(C_TEXT3);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        s.add(sub); s.add(Box.createVerticalStrut(22));
        s.add(makeSep()); s.add(Box.createVerticalStrut(12));

        navBtn(s, "home",    "⌂", "Главная");
        navBtn(s, "install", "↓", "Установка");
        navBtn(s, "mods",    "⧉", "Моды");
        navBtn(s, "settings","⚙", "Настройки");

        s.add(Box.createVerticalGlue());
        s.add(makeSep()); s.add(Box.createVerticalStrut(10));

        JButton iconBtn = flatBtn("🖼  Поменять иконку", C_TEXT2);
        iconBtn.setAlignmentX(CENTER_ALIGNMENT);
        iconBtn.addActionListener(e -> pickIcon());
        s.add(iconBtn); s.add(Box.createVerticalStrut(8));

        // Путь к папке
        JPanel dir = miniCard("📁 " + BASE.replace(System.getProperty("user.home"),"~"));
        s.add(dir); s.add(Box.createVerticalStrut(8));

        JLabel ver = new JLabel("v1.0.0");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 10)); ver.setForeground(C_TEXT3);
        ver.setAlignmentX(CENTER_ALIGNMENT); s.add(ver);
        return s;
    }

    private void navBtn(JPanel parent, String page, String icon, String label) {
        JPanel btn = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = activePage.equals(page);
                if (active) {
                    g2.setColor(new Color(72,213,130,18));
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                    g2.setColor(C_ACCENT); g2.fillRoundRect(0,8,3,getHeight()-16,3,3);
                } else if (getClientProperty("h") != null) {
                    g2.setColor(new Color(255,255,255,8));
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(11,14,11,14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("page", page);

        boolean active = activePage.equals(page);
        JLabel iL = new JLabel(icon); iL.setFont(new Font("Segoe UI Symbol",Font.PLAIN,15)); iL.setForeground(active?C_ACCENT:C_TEXT2);
        JLabel tL = new JLabel(label); tL.setFont(new Font("Segoe UI",Font.PLAIN,13)); tL.setForeground(active?C_TEXT:C_TEXT2);
        btn.add(iL, BorderLayout.WEST); btn.add(tL, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.putClientProperty("h",1); btn.repaint(); }
            public void mouseExited(MouseEvent e)  { btn.putClientProperty("h",null); btn.repaint(); }
            public void mouseClicked(MouseEvent e) {
                activePage = page; cards.show(content, page);
                for (Component c : parent.getComponents()) {
                    if (!(c instanceof JPanel p)) continue;
                    Object pg = p.getClientProperty("page"); if (pg==null) continue;
                    boolean a = pg.equals(page);
                    for (Component ch : p.getComponents()) {
                        if (!(ch instanceof JLabel l)) continue;
                        if (l.getText().length()<=2) l.setForeground(a?C_ACCENT:C_TEXT2);
                        else l.setForeground(a?C_TEXT:C_TEXT2);
                    }
                    p.repaint();
                }
            }
        });
        parent.add(btn); parent.add(Box.createVerticalStrut(3));
    }

    // ── ICON PICKER ───────────────────────────────────────
    private void pickIcon() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Выбери иконку (PNG / JPG)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Изображения","png","jpg","jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage img = ImageIO.read(fc.getSelectedFile());
            if (img == null) throw new IOException("Не удалось прочитать изображение");
            BufferedImage out = new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img, 0, 0, 256, 256, null); g.dispose();
            ImageIO.write(out, "PNG", new File(ICON_PATH));
            customIcon = out;
            applyWindowIcon();
            JOptionPane.showMessageDialog(this,
                "✓  Иконка сохранена!\n\nЧтобы иконка появилась на рабочем столе (.exe),\nнужно пересобрать проект через GitHub Actions.\n\nПоложи этот файл в:\nsrc/main/resources/icon.png",
                "Иконка", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: "+ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── CONTENT ───────────────────────────────────────────
    private JPanel buildContent() {
        cards = new CardLayout(); content = new JPanel(cards); content.setBackground(C_BG);
        content.add(homePage(),     "home");
        content.add(installPage(),  "install");
        content.add(modsPage(),     "mods");
        content.add(settingsPage(), "settings");
        return content;
    }

    // ─── Главная ─────────────────────────────────────────
    private JPanel homePage() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,20,34));
        JPanel hdr = new JPanel(new BorderLayout(0,5)); hdr.setOpaque(false);
        JLabel t = new JLabel("Добро пожаловать"); t.setFont(new Font("Segoe UI",Font.BOLD,30)); t.setForeground(C_TEXT);
        JLabel st = new JLabel("Выбери версию внизу, загрузчик и нажми  ▶ ИГРАТЬ"); st.setFont(new Font("Segoe UI",Font.PLAIN,13)); st.setForeground(C_TEXT2);
        hdr.add(t,BorderLayout.NORTH); hdr.add(st,BorderLayout.CENTER); p.add(hdr,BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2,2,16,16));
        grid.setOpaque(false); grid.setBorder(new EmptyBorder(22,0,0,0));
        grid.add(infoCard("Все версии","Release, Snapshot, Beta\nи Alpha — все доступны",C_ACCENT,"◈ Полная библиотека"));
        grid.add(infoCard("Fabric","Современный модлоадер\nдля большинства модов",C_BLUE,"⚡ Fabric"));
        grid.add(infoCard("Forge","Классический загрузчик,\nтысячи модов",C_ORANGE,"🔨 Forge"));
        grid.add(infoCard("Оффлайн","Работает без аккаунта\nMicrosoft",C_PURPLE,"✓ Оффлайн"));
        p.add(grid,BorderLayout.CENTER); return p;
    }

    // ─── Установка ───────────────────────────────────────
    private JPanel installPage() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,24,34));
        JPanel hdr = new JPanel(new BorderLayout(0,5)); hdr.setOpaque(false);
        JLabel t = new JLabel("Установка"); t.setFont(new Font("Segoe UI",Font.BOLD,28)); t.setForeground(C_TEXT);
        JLabel st = new JLabel("Minecraft + модлоадер. Файлы: " + BASE); st.setFont(new Font("Segoe UI",Font.PLAIN,12)); st.setForeground(new Color(72,213,130,160));
        hdr.add(t,BorderLayout.NORTH); hdr.add(st,BorderLayout.CENTER); p.add(hdr,BorderLayout.NORTH);

        JPanel card = glassCard(); card.setLayout(new BorderLayout(0,18)); card.setBorder(new EmptyBorder(24,24,24,24));

        // Строка 1: версия MC + чекбоксы фильтра
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); row1.setOpaque(false);
        JLabel vl = label13("Версия MC:"); row1.add(vl);

        JComboBox<VersionManager.VersionInfo> instBox = new JComboBox<>();
        instBox.setPreferredSize(new Dimension(155,38));
        instBox.setRenderer(versionCellRenderer());

        JCheckBox snapCb  = miniCheck("Snapshots");
        JCheckBox legCb   = miniCheck("Beta/Alpha");

        Runnable refillInstBox = () -> {
            List<VersionManager.VersionInfo> all = versionManager.getAllVersions();
            instBox.removeAllItems();
            for (VersionManager.VersionInfo v : all) {
                if (v.isRelease()) { instBox.addItem(v); continue; }
                if (v.isSnapshot() && snapCb.isSelected()) { instBox.addItem(v); continue; }
                if (v.isLegacy()   && legCb.isSelected())  { instBox.addItem(v); }
            }
        };
        snapCb.addActionListener(e -> refillInstBox.run());
        legCb.addActionListener(e  -> refillInstBox.run());

        row1.add(instBox); row1.add(Box.createHorizontalStrut(8));
        row1.add(snapCb); row1.add(legCb);
        card.add(row1, BorderLayout.NORTH);

        // Строка 2: модлоадер
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); row2.setOpaque(false);
        row2.add(label13("Загрузчик:"));

        JComboBox<String> loaderTypeBox = new JComboBox<>(new String[]{"Vanilla","Fabric","Forge"});
        loaderTypeBox.setPreferredSize(new Dimension(115, 38));

        JLabel loaderVerLbl = new JLabel("Версия:"); loaderVerLbl.setForeground(C_TEXT2); loaderVerLbl.setFont(new Font("Segoe UI",Font.PLAIN,13));
        JComboBox<String> loaderVerBox = new JComboBox<>(); loaderVerBox.setPreferredSize(new Dimension(145,38)); loaderVerBox.setEnabled(false);

        loaderTypeBox.addActionListener(e -> {
            String chosen = (String) loaderTypeBox.getSelectedItem();
            boolean needsVer = !"Vanilla".equals(chosen);
            loaderVerBox.setEnabled(needsVer);
            if (!needsVer) { loaderVerBox.removeAllItems(); return; }
            VersionManager.VersionInfo selVer = (VersionManager.VersionInfo) instBox.getSelectedItem();
            if (selVer == null) return;
            loaderVerBox.removeAllItems();
            loaderVerBox.addItem("Загрузка…");
            String mc = selVer.id();
            new SwingWorker<List<String>,Void>() {
                @Override protected List<String> doInBackground() throws Exception {
                    return "Fabric".equals(chosen)
                        ? modLoader.getFabricLoaderVersions(mc)
                        : modLoader.getForgeVersions(mc);
                }
                @Override protected void done() {
                    try {
                        List<String> vers = get();
                        loaderVerBox.removeAllItems();
                        if (vers.isEmpty()) loaderVerBox.addItem("(нет для этой версии)");
                        else vers.forEach(loaderVerBox::addItem);
                    } catch (Exception ex) { loaderVerBox.removeAllItems(); loaderVerBox.addItem("Ошибка"); }
                }
            }.execute();
        });

        instBox.addActionListener(e -> {
            if (loaderTypeBox.getSelectedIndex() > 0) loaderTypeBox.actionPerformed(e);
        });

        row2.add(loaderTypeBox); row2.add(loaderVerLbl); row2.add(loaderVerBox);
        card.add(row2, BorderLayout.CENTER);

        // Строка 3: кнопка + прогресс
        JPanel bottom = new JPanel(new BorderLayout(0,10)); bottom.setOpaque(false);
        JButton instBtn = accentBtn("  Установить  ", C_BLUE, 38);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); btnRow.setOpaque(false); btnRow.add(instBtn);
        JProgressBar pb = new JProgressBar(0,100); pb.setPreferredSize(new Dimension(0,5)); pb.setStringPainted(false);
        pb.setForeground(C_BLUE); pb.setBackground(C_BORDER); pb.setBorderPainted(false);
        JLabel sl = new JLabel("Выбери версию и нажми «Установить»"); sl.setFont(new Font("Segoe UI",Font.PLAIN,12)); sl.setForeground(C_TEXT2);
        bottom.add(btnRow, BorderLayout.NORTH); bottom.add(pb, BorderLayout.CENTER); bottom.add(sl, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);

        instBtn.addActionListener(e -> {
            VersionManager.VersionInfo selVer = (VersionManager.VersionInfo) instBox.getSelectedItem();
            if (selVer == null) return;
            String mc       = selVer.id();
            String loaderTp = (String) loaderTypeBox.getSelectedItem();
            String loaderV  = (String) loaderVerBox.getSelectedItem();
            if ((!"Vanilla".equals(loaderTp)) && (loaderV == null || loaderV.startsWith("(") || loaderV.equals("Загрузка…"))) {
                JOptionPane.showMessageDialog(this, "Дождись загрузки списка версий модлоадера", "Внимание", JOptionPane.WARNING_MESSAGE); return;
            }
            instBtn.setEnabled(false); pb.setValue(0); pb.setForeground(C_BLUE);
            sl.setForeground(C_TEXT2);

            new SwingWorker<Void,Void>(){
                String err;
                @Override protected Void doInBackground() {
                    try {
                        // 1. Скачиваем Vanilla
                        Downloader dl = new Downloader(BASE);
                        dl.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{pb.setValue(pct/2);sl.setText(msg);}));
                        dl.downloadVersion(mc);
                        // 2. Устанавливаем модлоадер если нужен
                        if ("Fabric".equals(loaderTp)) {
                            modLoader.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{pb.setValue(50+pct/2);sl.setText(msg);}));
                            modLoader.installFabric(mc, loaderV);
                        } else if ("Forge".equals(loaderTp)) {
                            modLoader.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{pb.setValue(50+pct/2);sl.setText(msg);}));
                            // Forge нужна java, находим через JavaDownloader
                            JavaDownloader jd = new JavaDownloader(BASE);
                            String javaPath = jd.getOrDownloadJava();
                            modLoader.installForge(mc, loaderV, javaPath);
                        }
                    } catch (Exception ex) { err = ex.getMessage(); }
                    return null;
                }
                @Override protected void done() {
                    instBtn.setEnabled(true);
                    if (err!=null) { sl.setText("Ошибка: "+err); sl.setForeground(C_DANGER); pb.setForeground(C_DANGER); }
                    else { sl.setText("✓  Установлено!"); sl.setForeground(C_ACCENT); pb.setForeground(C_ACCENT); pb.setValue(100); }
                }
            }.execute();
        });

        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.setBorder(new EmptyBorder(20,0,0,0));
        wrap.add(card,BorderLayout.NORTH); p.add(wrap,BorderLayout.CENTER);

        // Заполняем версии после загрузки
        SwingUtilities.invokeLater(() -> {
            List<VersionManager.VersionInfo> all = versionManager.getAllVersions();
            all.stream().filter(VersionManager.VersionInfo::isRelease).forEach(instBox::addItem);
        });

        return p;
    }

    // ─── Моды ────────────────────────────────────────────
    private JPanel modsPage() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,24,34));
        JLabel t = new JLabel("Моды"); t.setFont(new Font("Segoe UI",Font.BOLD,28)); t.setForeground(C_TEXT);
        p.add(t, BorderLayout.NORTH);

        JPanel card = glassCard(); card.setLayout(new BorderLayout(0,14)); card.setBorder(new EmptyBorder(22,22,22,22));

        JLabel hint = new JLabel("<html><body style='width:500px;color:rgb(128,145,178)'>"
            + "<b style='color:rgb(225,232,248)'>Как добавить моды:</b><br><br>"
            + "1. Установи Fabric или Forge через вкладку «Установка»<br>"
            + "2. Открой папку  <b style='color:rgb(72,213,130)'>" + BASE + "\\mods</b><br>"
            + "3. Скопируй туда .jar файлы модов<br>"
            + "4. Убедись что мод совместим с выбранной версией MC<br><br>"
            + "<b>Рекомендуемые сайты для скачивания модов:</b><br>"
            + "• <u>modrinth.com</u> — современный, безопасный<br>"
            + "• <u>curseforge.com</u> — самый большой каталог<br>"
            + "• <u>github.com</u> — для open-source модов"
            + "</body></html>");
        hint.setFont(new Font("Segoe UI",Font.PLAIN,13));
        card.add(hint, BorderLayout.CENTER);

        JButton openFolder = accentBtn("  📂  Открыть папку mods  ", C_ACCENT, 40);
        openFolder.addActionListener(e -> {
            try { Desktop.getDesktop().open(new File(BASE + File.separator + "mods")); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Не удалось открыть папку:\n"+ex.getMessage()); }
        });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT)); btnRow.setOpaque(false); btnRow.add(openFolder);
        card.add(btnRow, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.setBorder(new EmptyBorder(20,0,0,0));
        wrap.add(card, BorderLayout.NORTH); p.add(wrap, BorderLayout.CENTER); return p;
    }

    // ─── Настройки ───────────────────────────────────────
    private JPanel settingsPage() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,24,34));
        JLabel t = new JLabel("Настройки запуска"); t.setFont(new Font("Segoe UI",Font.BOLD,28)); t.setForeground(C_TEXT); p.add(t,BorderLayout.NORTH);

        JPanel form = new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS)); form.setBorder(new EmptyBorder(22,0,0,0));

        form.add(sectionLbl("ПАМЯТЬ (RAM)")); form.add(Box.createVerticalStrut(10));
        JPanel rr = new JPanel(new BorderLayout(14,0)); rr.setOpaque(false); rr.setMaximumSize(new Dimension(Integer.MAX_VALUE,50)); rr.setAlignmentX(LEFT_ALIGNMENT);
        ramSlider = new JSlider(512,8192,2048); ramSlider.setOpaque(false); ramSlider.setForeground(C_ACCENT);
        ramLbl = new JLabel("2048 MB"); ramLbl.setFont(new Font("Segoe UI",Font.BOLD,15)); ramLbl.setForeground(C_ACCENT); ramLbl.setPreferredSize(new Dimension(95,32));
        ramSlider.addChangeListener(e -> ramLbl.setText(ramSlider.getValue()+" MB"));
        rr.add(ramSlider,BorderLayout.CENTER); rr.add(ramLbl,BorderLayout.EAST); form.add(rr); form.add(Box.createVerticalStrut(26));

        form.add(sectionLbl("JVM АРГУМЕНТЫ")); form.add(Box.createVerticalStrut(10));
        jvmField = styledTF("-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -Dfml.ignorePatchDiscrepancies=true", new Font("Consolas",Font.PLAIN,12));
        form.add(jvmField); form.add(Box.createVerticalStrut(26));

        // Иконка
        form.add(sectionLbl("ИКОНКА ЛАУНЧЕРА")); form.add(Box.createVerticalStrut(8));
        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT,12,0)); iconRow.setOpaque(false); iconRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel prev = new JLabel(new ImageIcon(customIcon!=null?customIcon.getScaledInstance(44,44,Image.SCALE_SMOOTH):generateDefaultIcon(44)));
        JButton pickB = accentBtn("  Выбрать файл…  ",C_BLUE,38);
        pickB.addActionListener(e -> { pickIcon(); prev.setIcon(new ImageIcon(customIcon!=null?customIcon.getScaledInstance(44,44,Image.SCALE_SMOOTH):generateDefaultIcon(44))); });
        JButton resetB = flatBtn("✕ Сбросить",C_DANGER);
        resetB.addActionListener(e -> { new File(ICON_PATH).delete(); customIcon=null; applyWindowIcon(); prev.setIcon(new ImageIcon(generateDefaultIcon(44))); });
        iconRow.add(prev); iconRow.add(pickB); iconRow.add(resetB);

        JLabel iconNote = new JLabel("<html><body style='width:400px;color:rgb(62,75,105)'>"
            + "Чтобы иконка отображалась на рабочем столе (.exe): положи icon.png<br>"
            + "в папку <b>src/main/resources/</b> и пересобери через GitHub Actions.</body></html>");
        iconNote.setFont(new Font("Segoe UI",Font.PLAIN,11)); iconNote.setAlignmentX(LEFT_ALIGNMENT);

        form.add(iconRow); form.add(Box.createVerticalStrut(6)); form.add(iconNote); form.add(Box.createVerticalStrut(26));

        // Папки
        form.add(sectionLbl("БЫСТРЫЙ ДОСТУП")); form.add(Box.createVerticalStrut(10));
        JPanel foldersRow = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); foldersRow.setOpaque(false); foldersRow.setAlignmentX(LEFT_ALIGNMENT);
        foldersRow.add(folderBtn("📂 Saves",   "saves"));
        foldersRow.add(folderBtn("📂 Mods",    "mods"));
        foldersRow.add(folderBtn("📂 Versions","versions"));
        foldersRow.add(folderBtn("📄 Logs",    ""));
        form.add(foldersRow); form.add(Box.createVerticalStrut(26));

        JButton save = accentBtn("  Сохранить настройки  ",C_ACCENT,40); save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e -> { saveCfg(); statusLbl.setText("✓  Настройки сохранены"); statusLbl.setForeground(C_ACCENT); });
        form.add(save);

        JScrollPane sc = new JScrollPane(form); sc.setOpaque(false); sc.getViewport().setOpaque(false); sc.setBorder(null);
        p.add(sc,BorderLayout.CENTER); return p;
    }

    private JButton folderBtn(String lbl, String sub) {
        JButton b = flatBtn(lbl, C_TEXT2);
        b.setFont(new Font("Segoe UI",Font.PLAIN,12));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER,1),
            new EmptyBorder(6,12,6,12)));
        b.addActionListener(e -> {
            try {
                File f = sub.isEmpty() ? new File(BASE) : new File(BASE+File.separator+sub);
                f.mkdirs(); Desktop.getDesktop().open(f);
            } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Не удалось открыть:\n"+ex.getMessage()); }
        });
        return b;
    }

    // ── FOOTER ────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setColor(C_BORDER); g2.fillRect(0,0,getWidth(),1);
                GradientPaint grd = new GradientPaint(0,0,new Color(14,17,25),0,getHeight(),new Color(10,12,18));
                g2.setPaint(grd); g2.fillRect(0,1,getWidth(),getHeight()-1); g2.dispose();
            }
        };
        f.setOpaque(false); f.setPreferredSize(new Dimension(0,100)); f.setBorder(new EmptyBorder(0,26,0,26));

        // Статус
        JPanel left = new JPanel(new BorderLayout(0,6)); left.setOpaque(false);
        statusLbl = new JLabel("Готово к запуску"); statusLbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); statusLbl.setForeground(C_TEXT2);
        progBar = new JProgressBar(0,100); progBar.setPreferredSize(new Dimension(0,4)); progBar.setStringPainted(false);
        progBar.setForeground(C_ACCENT); progBar.setBackground(C_BORDER); progBar.setBorderPainted(false); progBar.setVisible(false);
        left.add(statusLbl,BorderLayout.CENTER); left.add(progBar,BorderLayout.SOUTH);

        // Правая часть — контролы
        JPanel right = new JPanel(); right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));

        // НИК
        JPanel nickW = fg("НИК");
        nickField = styledTF("Player", new Font("Segoe UI",Font.PLAIN,13));
        nickField.setPreferredSize(new Dimension(155,38)); nickField.setMaximumSize(new Dimension(155,38));
        nickW.add(nickField);

        // ВЕРСИЯ (с фильтром)
        JPanel verPanel = new JPanel(new BorderLayout(0,5)); verPanel.setOpaque(false);
        JLabel verLabel = new JLabel("ВЕРСИЯ"); verLabel.setFont(new Font("Segoe UI",Font.BOLD,9)); verLabel.setForeground(C_TEXT3);

        JPanel verRow = new JPanel(new BorderLayout(4,0)); verRow.setOpaque(false);
        versionBox = new JComboBox<>();
        versionBox.setRenderer(versionCellRenderer());
        versionBox.setPreferredSize(new Dimension(148,38)); versionBox.setMaximumSize(new Dimension(148,38));

        // Кнопка фильтра
        JButton filterBtn = new JButton("▼");
        filterBtn.setPreferredSize(new Dimension(30,38)); filterBtn.setMaximumSize(new Dimension(30,38));
        filterBtn.setFont(new Font("Segoe UI",Font.PLAIN,9));
        filterBtn.setForeground(C_TEXT3); filterBtn.setBackground(new Color(19,24,36));
        filterBtn.setBorderPainted(false); filterBtn.setFocusPainted(false);
        filterBtn.setToolTipText("Фильтр версий");
        filterBtn.addActionListener(e -> showVersionFilter(filterBtn));

        verRow.add(versionBox, BorderLayout.CENTER); verRow.add(filterBtn, BorderLayout.EAST);
        verPanel.add(verLabel, BorderLayout.NORTH); verPanel.add(verRow, BorderLayout.CENTER);

        // ЗАГРУЗЧИК
        JPanel loaderPanel = new JPanel(new BorderLayout(0,5)); loaderPanel.setOpaque(false);
        JLabel loaderLabel = new JLabel("ЗАГРУЗЧИК"); loaderLabel.setFont(new Font("Segoe UI",Font.BOLD,9)); loaderLabel.setForeground(C_TEXT3);

        JPanel loaderRow = new JPanel(new BorderLayout(4,0)); loaderRow.setOpaque(false);
        JComboBox<String> loaderTypeFooter = new JComboBox<>(new String[]{"Vanilla","Fabric","Forge"});
        loaderTypeFooter.setPreferredSize(new Dimension(100,38)); loaderTypeFooter.setMaximumSize(new Dimension(100,38));

        loaderVersionBox = new JComboBox<>();
        loaderVersionBox.setPreferredSize(new Dimension(140,38)); loaderVersionBox.setMaximumSize(new Dimension(140,38));
        loaderVersionBox.setEnabled(false);

        loaderTypeFooter.addActionListener(e -> {
            String tp = (String) loaderTypeFooter.getSelectedItem();
            boolean needsV = !"Vanilla".equals(tp);
            loaderVersionBox.setEnabled(needsV);
            selectedLoader = switch(tp) {
                case "Fabric" -> ModLoader.Type.FABRIC;
                case "Forge"  -> ModLoader.Type.FORGE;
                default       -> ModLoader.Type.VANILLA;
            };
            if (!needsV) { loaderVersionBox.removeAllItems(); selectedLoaderVer=""; return; }
            VersionManager.VersionInfo sel = (VersionManager.VersionInfo) versionBox.getSelectedItem();
            if (sel == null) return;
            loadLoaderVersions(tp, sel.id(), loaderVersionBox);
        });

        loaderVersionBox.addActionListener(e -> {
            Object sel = loaderVersionBox.getSelectedItem();
            selectedLoaderVer = (sel!=null && !sel.toString().startsWith("(")) ? sel.toString() : "";
        });

        loaderRow.add(loaderTypeFooter, BorderLayout.WEST); loaderRow.add(loaderVersionBox, BorderLayout.CENTER);
        loaderPanel.add(loaderLabel, BorderLayout.NORTH); loaderPanel.add(loaderRow, BorderLayout.CENTER);

        // Кнопка ИГРАТЬ
        playBtn = accentBtn("▶   ИГРАТЬ", C_ACCENT, 48);
        playBtn.setFont(new Font("Segoe UI",Font.BOLD,14));
        playBtn.setPreferredSize(new Dimension(155,48)); playBtn.setMaximumSize(new Dimension(155,48));
        playBtn.addActionListener(e -> launch());

        right.add(Box.createHorizontalStrut(16));
        right.add(nickW); right.add(Box.createHorizontalStrut(12));
        right.add(verPanel); right.add(Box.createHorizontalStrut(12));
        right.add(loaderPanel); right.add(Box.createHorizontalStrut(14));
        right.add(playBtn); right.add(Box.createHorizontalStrut(4));

        f.add(left, BorderLayout.CENTER); f.add(right, BorderLayout.EAST); return f;
    }

    private void showVersionFilter(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        JCheckBoxMenuItem snap = new JCheckBoxMenuItem("Snapshots", showSnapshots);
        JCheckBoxMenuItem leg  = new JCheckBoxMenuItem("Beta / Alpha", showLegacy);
        snap.addActionListener(e -> { showSnapshots=snap.isSelected(); refreshVersionBox(); });
        leg.addActionListener(e  -> { showLegacy=leg.isSelected();     refreshVersionBox(); });
        menu.add(snap); menu.add(leg);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void refreshVersionBox() {
        if (versionBox == null) return;
        VersionManager.VersionInfo selected = (VersionManager.VersionInfo) versionBox.getSelectedItem();
        versionBox.removeAllItems();
        for (VersionManager.VersionInfo v : versionManager.getAllVersions()) {
            if (v.isRelease())  { versionBox.addItem(v); continue; }
            if (v.isSnapshot() && showSnapshots) { versionBox.addItem(v); continue; }
            if (v.isLegacy()   && showLegacy)   { versionBox.addItem(v); }
        }
        if (selected != null) versionBox.setSelectedItem(selected);
    }

    private void loadLoaderVersions(String type, String mc, JComboBox<String> box) {
        box.removeAllItems(); box.addItem("Загрузка…");
        new SwingWorker<List<String>,Void>(){
            @Override protected List<String> doInBackground() throws Exception {
                return "Fabric".equals(type) ? modLoader.getFabricLoaderVersions(mc) : modLoader.getForgeVersions(mc);
            }
            @Override protected void done() {
                try {
                    List<String> vers = get(); box.removeAllItems();
                    if (vers.isEmpty()) box.addItem("(нет для этой версии)");
                    else vers.forEach(box::addItem);
                } catch (Exception ex) { box.removeAllItems(); box.addItem("Ошибка"); }
            }
        }.execute();
    }

    private ListCellRenderer<Object> versionCellRenderer() {
        return new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, idx, sel, focus);
                if (value instanceof VersionManager.VersionInfo v) {
                    setText(v.id());
                    Color tc = v.isRelease() ? C_TEXT : v.isSnapshot() ? C_BLUE : C_TEXT2;
                    setForeground(sel ? Color.WHITE : tc);
                }
                setBackground(sel ? new Color(30,38,58) : new Color(11,14,21));
                setBorder(new EmptyBorder(4,8,4,8));
                return this;
            }
        };
    }

    // ── LAUNCH ────────────────────────────────────────────
    private void launch() {
        String nick = nickField.getText().trim();
        VersionManager.VersionInfo selVer = (VersionManager.VersionInfo) versionBox.getSelectedItem();
        if (selVer == null) { JOptionPane.showMessageDialog(this,"Выбери версию","Ошибка",JOptionPane.WARNING_MESSAGE); return; }
        if (nick.length()<3 || !nick.matches("[a-zA-Z0-9_]+")) {
            JOptionPane.showMessageDialog(this,"Никнейм: минимум 3 символа, A-Z 0-9 _","Ошибка",JOptionPane.WARNING_MESSAGE); return;
        }

        String mc = selVer.id();
        saveCfg(); playBtn.setEnabled(false); progBar.setVisible(true); progBar.setValue(0);
        progBar.setForeground(C_ACCENT); statusLbl.setForeground(C_TEXT2);
        int ram=ramSlider!=null?ramSlider.getValue():2048; String jvm=jvmField!=null?jvmField.getText():"";

        new SwingWorker<Process,Object>(){
            String err;
            @Override protected Process doInBackground() {
                try {
                    // 1. Скачиваем базовую версию
                    Downloader dl = new Downloader(BASE);
                    dl.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{progBar.setValue(pct*70/100);statusLbl.setText(msg);}));
                    dl.downloadVersion(mc);

                    // 2. Определяем реальный versionId для запуска
                    String launchId = mc;
                    if (selectedLoader == ModLoader.Type.FABRIC && !selectedLoaderVer.isEmpty()) {
                        launchId = modLoader.resolveVersionId(mc, ModLoader.Type.FABRIC, selectedLoaderVer);
                        if (!modLoader.isInstalled(launchId)) {
                            SwingUtilities.invokeLater(()->statusLbl.setText("Установка Fabric…"));
                            modLoader.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{progBar.setValue(70+pct*25/100);statusLbl.setText(msg);}));
                            modLoader.installFabric(mc, selectedLoaderVer);
                        }
                    } else if (selectedLoader == ModLoader.Type.FORGE && !selectedLoaderVer.isEmpty()) {
                        launchId = modLoader.resolveVersionId(mc, ModLoader.Type.FORGE, selectedLoaderVer);
                        if (!modLoader.isInstalled(launchId)) {
                            SwingUtilities.invokeLater(()->statusLbl.setText("Установка Forge…"));
                            JavaDownloader jd = new JavaDownloader(BASE);
                            String jp = jd.getOrDownloadJava();
                            modLoader.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{progBar.setValue(70+pct*25/100);statusLbl.setText(msg);}));
                            modLoader.installForge(mc, selectedLoaderVer, jp);
                        }
                    }

                    final String finalId = launchId;
                    SwingUtilities.invokeLater(()->{ progBar.setValue(97); statusLbl.setText("Запуск "+finalId+"…"); });
                    return new GameLauncher(BASE).launch(finalId, nick, ram, jvm,
                        (pct,msg)->SwingUtilities.invokeLater(()->{progBar.setValue(pct);statusLbl.setText(msg);}));
                } catch (Exception ex) { err=ex.getMessage(); return null; }
            }
            @Override protected void done() {
                progBar.setVisible(false); playBtn.setEnabled(true);
                if (err!=null) { statusLbl.setText("Ошибка: "+err); statusLbl.setForeground(C_DANGER);
                    JOptionPane.showMessageDialog(Main.this,"Ошибка запуска:\n"+err,"Ошибка",JOptionPane.ERROR_MESSAGE); }
                else { statusLbl.setText("✓  Запущен  •  "+nick); statusLbl.setForeground(C_ACCENT); }
            }
        }.execute();
    }

    // ── HELPERS ───────────────────────────────────────────
    private JPanel glassCard() {
        return new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));
                g2.setColor(C_BORDER); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,14,14)); g2.dispose();
            }
        };
    }

    private JPanel infoCard(String title, String desc, Color accent, String badge) {
        JPanel c = new JPanel(new BorderLayout(0,10)){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));
                g2.setColor(accent); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),3,3,3));
                GradientPaint sh=new GradientPaint(0,0,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),28),0,55,new Color(0,0,0,0));
                g2.setPaint(sh); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),55,14,14)); g2.dispose();
            }
        };
        c.setOpaque(false); c.setBorder(new EmptyBorder(18,20,20,20));
        JLabel bl=new JLabel(badge); bl.setFont(new Font("Segoe UI",Font.BOLD,10)); bl.setForeground(accent);
        JLabel tl=new JLabel(title); tl.setFont(new Font("Segoe UI",Font.BOLD,16)); tl.setForeground(C_TEXT);
        JLabel dl=new JLabel("<html><body style='width:155px;color:rgb(128,145,178)'>"+desc.replace("\n","<br>")+"</body></html>"); dl.setFont(new Font("Segoe UI",Font.PLAIN,12));
        c.add(bl,BorderLayout.NORTH); c.add(tl,BorderLayout.CENTER); c.add(dl,BorderLayout.SOUTH); return c;
    }

    private JButton accentBtn(String text, Color ac, int h){
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color col=!isEnabled()?new Color(40,50,68):getModel().isPressed()?ac.darker():getModel().isRollover()?ac.brighter():ac;
                g2.setColor(col); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),9,9));
                g2.setColor(isEnabled()?Color.WHITE:C_TEXT3); g2.setFont(getFont()); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
            }
        };
        b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setForeground(Color.WHITE);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width, h)); return b;
    }

    private JButton flatBtn(String t, Color fg){
        JButton b=new JButton(t); b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setForeground(fg);b.setFont(new Font("Segoe UI",Font.PLAIN,11));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }

    private JTextField styledTF(String def, Font font){
        JTextField tf=new JTextField(def); tf.setFont(font);tf.setForeground(C_TEXT);tf.setBackground(new Color(11,14,21));tf.setCaretColor(C_ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER,1),new EmptyBorder(6,12,6,12)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));tf.setAlignmentX(LEFT_ALIGNMENT);return tf;
    }

    private JCheckBox miniCheck(String lbl){
        JCheckBox cb=new JCheckBox(lbl); cb.setOpaque(false);cb.setForeground(C_TEXT2);cb.setFont(new Font("Segoe UI",Font.PLAIN,12));return cb;
    }

    private JLabel label13(String t){ JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.PLAIN,13));l.setForeground(C_TEXT2);return l; }
    private JPanel fg(String lbl){ JPanel p=new JPanel(new BorderLayout(0,5));p.setOpaque(false);JLabel l=new JLabel(lbl);l.setFont(new Font("Segoe UI",Font.BOLD,9));l.setForeground(C_TEXT3);p.add(l,BorderLayout.NORTH);return p; }
    private JLabel sectionLbl(String t){ JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.BOLD,10));l.setForeground(C_TEXT3);l.setAlignmentX(LEFT_ALIGNMENT);return l; }
    private Component makeSep(){ JSeparator s=new JSeparator();s.setForeground(C_BORDER);s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));return s; }
    private JPanel miniCard(String txt){
        JPanel p=new JPanel(){
            @Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(new Color(11,14,21));g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));g2.dispose();}
        };
        p.setOpaque(false);p.setBorder(new EmptyBorder(8,12,8,12));p.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));p.setAlignmentX(CENTER_ALIGNMENT);
        JLabel l=new JLabel(txt);l.setFont(new Font("Segoe UI",Font.PLAIN,10));l.setForeground(C_TEXT3);l.setToolTipText(BASE);p.add(l);return p;
    }

    // ── CONFIG ────────────────────────────────────────────
    private void mkdirs(){
        for(String s:new String[]{"","versions","libraries","assets","natives","mods","saves","runtime","temp"})
            new File(BASE+(s.isEmpty()?"":File.separator+s)).mkdirs();
    }

    private void saveCfg(){
        Properties p=new Properties();
        p.setProperty("nickname",nickField.getText().trim());
        Object selVer = versionBox.getSelectedItem();
        if (selVer!=null) p.setProperty("version",selVer.toString());
        p.setProperty("loader",selectedLoader.name());
        p.setProperty("loaderVer",selectedLoaderVer);
        p.setProperty("showSnapshots",String.valueOf(showSnapshots));
        p.setProperty("showLegacy",String.valueOf(showLegacy));
        if(ramSlider!=null) p.setProperty("ram",String.valueOf(ramSlider.getValue()));
        if(jvmField!=null) p.setProperty("jvm",jvmField.getText());
        try(OutputStream o=new FileOutputStream(CFG)){p.store(o,"FlytLauncher");}catch(IOException ignored){}
    }

    private void loadCfg(){
        File f=new File(CFG); if(!f.exists())return;
        Properties p=new Properties();
        try(InputStream i=new FileInputStream(CFG)){
            p.load(i);
            nickField.setText(p.getProperty("nickname","Player"));
            showSnapshots=Boolean.parseBoolean(p.getProperty("showSnapshots","false"));
            showLegacy=Boolean.parseBoolean(p.getProperty("showLegacy","false"));
            if(ramSlider!=null){ramSlider.setValue(Integer.parseInt(p.getProperty("ram","2048")));ramLbl.setText(ramSlider.getValue()+" MB");}
            if(jvmField!=null) jvmField.setText(p.getProperty("jvm","-XX:+UseG1GC -XX:+ParallelRefProcEnabled"));
            try { selectedLoader=ModLoader.Type.valueOf(p.getProperty("loader","VANILLA")); } catch(Exception ignored){}
            selectedLoaderVer=p.getProperty("loaderVer","");
        }catch(Exception ignored){}
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("ComboBox.background",         new Color(11,14,21));
        UIManager.put("ComboBox.foreground",         new Color(200,210,230));
        UIManager.put("ComboBox.selectionBackground",new Color(28,36,56));
        UIManager.put("CheckBox.background",         new Color(14,17,25));
        UIManager.put("Slider.thumbColor",           new Color(72,213,130));
        UIManager.put("Slider.trackColor",           new Color(38,48,72));
        UIManager.put("ProgressBar.background",      new Color(38,48,72));
        UIManager.put("PopupMenu.background",        new Color(19,24,36));
        UIManager.put("MenuItem.background",         new Color(19,24,36));
        UIManager.put("MenuItem.foreground",         new Color(200,210,230));
        UIManager.put("MenuItem.selectionBackground",new Color(30,38,56));
        SwingUtilities.invokeLater(()->new Main().setVisible(true));
    }
}
