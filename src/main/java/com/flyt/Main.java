package com.flyt;

import com.flyt.launcher.Downloader;
import com.flyt.launcher.GameLauncher;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;

public class Main extends JFrame {

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

    private JTextField   nicknameField;
    private JComboBox<String> versionBox;
    private JProgressBar progressBar;
    private JLabel       statusLbl;
    private JSlider      ramSlider;
    private JLabel       ramLbl;
    private JTextField   jvmField;
    private JPanel       content;
    private CardLayout   cards;
    private JButton      playBtn;
    private String       activePage = "home";
    private BufferedImage customIcon = null;

    private final String BASE      = System.getProperty("user.home") + File.separator + "FlytLauncher";
    private final String CFG       = BASE + File.separator + "launcher.properties";
    private final String ICON_PATH = BASE + File.separator + "icon.png";

    static final String[] VERS = {"1.20.4","1.20.1","1.19.4","1.18.2","1.16.5","1.12.2","1.8.9"};

    public Main() {
        mkdirs();
        loadCustomIcon();
        setTitle("FlytLauncher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 680);
        setMinimumSize(new Dimension(880, 580));
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
    }

    // ── Icon ──────────────────────────────────────────────
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
        g.fill(new RoundRectangle2D.Float(0, 0, sz, sz, sz*0.28f, sz*0.28f));
        int cx=sz/2, cy=sz/2, r=(int)(sz*0.36f);
        int[] px=new int[6], py=new int[6];
        for (int i=0;i<6;i++) { double a=Math.PI/6+i*Math.PI/3; px[i]=(int)(cx+r*Math.cos(a)); py[i]=(int)(cy+r*Math.sin(a)); }
        g.setColor(C_ACCENT);
        g.setStroke(new BasicStroke(sz*0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(px, py, 6);
        g.setFont(new Font("Segoe UI", Font.BOLD, (int)(sz*0.36f)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("F", cx-fm.stringWidth("F")/2, cy+fm.getAscent()/2-1);
        g.dispose();
        return img;
    }

    // ── Sidebar ───────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel s = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(C_BORDER); g2.fillRect(getWidth()-1,0,1,getHeight());
                GradientPaint gp=new GradientPaint(0,0,new Color(72,213,130,22),0,150,new Color(72,213,130,0));
                g2.setPaint(gp); g2.fillRect(0,0,getWidth(),150); g2.dispose();
            }
        };
        s.setBackground(C_SURFACE);
        s.setPreferredSize(new Dimension(228, 0));
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBorder(new EmptyBorder(26, 14, 22, 14));

        // Logo
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        logoRow.setOpaque(false); logoRow.setAlignmentX(CENTER_ALIGNMENT);
        JLabel iconL = new JLabel(new ImageIcon(customIcon!=null
            ? customIcon.getScaledInstance(30,30,Image.SCALE_SMOOTH)
            : generateDefaultIcon(30)));
        JLabel nameL = new JLabel("FLYT");
        nameL.setFont(new Font("Segoe UI", Font.BOLD, 22)); nameL.setForeground(C_TEXT);
        logoRow.add(iconL); logoRow.add(nameL);
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

        JButton iconBtn = flatBtn("🖼  Выбрать иконку", C_TEXT2);
        iconBtn.setAlignmentX(CENTER_ALIGNMENT);
        iconBtn.addActionListener(e -> pickIcon());
        s.add(iconBtn); s.add(Box.createVerticalStrut(10));

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
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active=activePage.equals(page), hover=getClientProperty("h")!=null;
                if (active) {
                    g2.setColor(new Color(72,213,130,18));
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                    g2.setColor(C_ACCENT); g2.fillRoundRect(0,8,3,getHeight()-16,3,3);
                } else if (hover) {
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
                activePage=page; cards.show(content, page);
                for (Component c : parent.getComponents()) {
                    if (!(c instanceof JPanel)) continue;
                    JPanel p=(JPanel)c; Object pg=p.getClientProperty("page"); if (pg==null) continue;
                    boolean a=pg.equals(page);
                    for (Component ch : p.getComponents()) {
                        if (!(ch instanceof JLabel)) continue;
                        JLabel l=(JLabel)ch;
                        if (l.getText().length()<=2) l.setForeground(a?C_ACCENT:C_TEXT2);
                        else l.setForeground(a?C_TEXT:C_TEXT2);
                    }
                    p.repaint();
                }
            }
        });
        parent.add(btn); parent.add(Box.createVerticalStrut(3));
    }

    // ── Icon picker ───────────────────────────────────────
    private void pickIcon() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Выбери иконку (PNG / JPG)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Изображения","png","jpg","jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage img = ImageIO.read(fc.getSelectedFile());
            if (img==null) throw new IOException("Не удалось прочитать изображение");
            BufferedImage out = new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img, 0, 0, 256, 256, null); g.dispose();
            ImageIO.write(out, "PNG", new File(ICON_PATH));
            customIcon = out;
            applyWindowIcon();
            JOptionPane.showMessageDialog(this,"✓  Иконка обновлена!\nОтображается в заголовке окна.","Иконка",JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Ошибка: "+ex.getMessage(),"Ошибка",JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Content ───────────────────────────────────────────
    private JPanel buildContent() {
        cards=new CardLayout(); content=new JPanel(cards); content.setBackground(C_BG);
        content.add(homePage(),    "home");
        content.add(installPage(), "install");
        content.add(placeholder("⧉","Моды","Управление модами — скоро"), "mods");
        content.add(settingsPage(),"settings");
        return content;
    }

    private JPanel homePage() {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,24,34));
        JPanel hdr=new JPanel(new BorderLayout(0,5)); hdr.setOpaque(false);
        JLabel t=new JLabel("Добро пожаловать"); t.setFont(new Font("Segoe UI",Font.BOLD,30)); t.setForeground(C_TEXT);
        JLabel st=new JLabel("Выбери версию внизу и нажми  ▶ ИГРАТЬ"); st.setFont(new Font("Segoe UI",Font.PLAIN,13)); st.setForeground(C_TEXT2);
        hdr.add(t,BorderLayout.NORTH); hdr.add(st,BorderLayout.CENTER); p.add(hdr,BorderLayout.NORTH);
        JPanel grid=new JPanel(new GridLayout(2,2,16,16)); grid.setOpaque(false); grid.setBorder(new EmptyBorder(26,0,0,0));
        grid.add(infoCard("1.20.4","Последняя стабильная\nверсия Minecraft",C_ACCENT,"◈ Актуальная"));
        grid.add(infoCard("Установка","Загрузи нужную версию\nво вкладке «Установка»",C_BLUE,"↓ Скачать"));
        grid.add(infoCard("Оффлайн режим","Работает без аккаунта\nMicrosoft / Mojang",C_PURPLE,"✓ Работает"));
        grid.add(infoCard("Папка игры",BASE.replace(System.getProperty("user.home"),"~"),C_ORANGE,"📁 Хранилище"));
        p.add(grid,BorderLayout.CENTER); return p;
    }

    private JPanel infoCard(String title, String desc, Color accent, String badge) {
        JPanel c=new JPanel(new BorderLayout(0,10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));
                g2.setColor(accent); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),3,3,3));
                GradientPaint sh=new GradientPaint(0,0,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),28),0,55,new Color(0,0,0,0));
                g2.setPaint(sh); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),55,14,14));
                g2.dispose();
            }
        };
        c.setOpaque(false); c.setBorder(new EmptyBorder(18,20,20,20));
        JLabel bl=new JLabel(badge); bl.setFont(new Font("Segoe UI",Font.BOLD,10)); bl.setForeground(accent);
        JLabel tl=new JLabel(title); tl.setFont(new Font("Segoe UI",Font.BOLD,16)); tl.setForeground(C_TEXT);
        JLabel dl=new JLabel("<html><body style='width:155px;color:rgb(128,145,178)'>"+desc.replace("\n","<br>")+"</body></html>"); dl.setFont(new Font("Segoe UI",Font.PLAIN,12));
        c.add(bl,BorderLayout.NORTH); c.add(tl,BorderLayout.CENTER); c.add(dl,BorderLayout.SOUTH);
        return c;
    }

    private JPanel installPage() {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,24,34));
        JPanel hdr=new JPanel(new BorderLayout(0,5)); hdr.setOpaque(false);
        JLabel t=new JLabel("Установка версий"); t.setFont(new Font("Segoe UI",Font.BOLD,28)); t.setForeground(C_TEXT);
        JLabel st=new JLabel("Файлы сохраняются в  "+BASE); st.setFont(new Font("Segoe UI",Font.PLAIN,12)); st.setForeground(new Color(72,213,130,160));
        hdr.add(t,BorderLayout.NORTH); hdr.add(st,BorderLayout.CENTER); p.add(hdr,BorderLayout.NORTH);

        JPanel card=glassCard(); card.setLayout(new BorderLayout(0,20)); card.setBorder(new EmptyBorder(26,26,26,26));
        JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0)); row.setOpaque(false);
        JLabel vl=new JLabel("Версия:"); vl.setFont(new Font("Segoe UI",Font.PLAIN,13)); vl.setForeground(C_TEXT2);
        JComboBox<String> box=new JComboBox<>(VERS); box.setPreferredSize(new Dimension(145,38));
        JButton btn=accentBtn("  Установить  ",C_BLUE,38);
        row.add(vl); row.add(box); row.add(btn); card.add(row,BorderLayout.NORTH);

        JPanel pp=new JPanel(new BorderLayout(0,10)); pp.setOpaque(false);
        JProgressBar pb=new JProgressBar(0,100); pb.setPreferredSize(new Dimension(0,5)); pb.setStringPainted(false);
        pb.setForeground(C_BLUE); pb.setBackground(C_BORDER); pb.setBorderPainted(false);
        JLabel sl=new JLabel("Выбери версию и нажми «Установить»"); sl.setFont(new Font("Segoe UI",Font.PLAIN,12)); sl.setForeground(C_TEXT2);
        pp.add(pb,BorderLayout.NORTH); pp.add(sl,BorderLayout.CENTER); card.add(pp,BorderLayout.CENTER);

        btn.addActionListener(e -> {
            String ver=(String)box.getSelectedItem(); btn.setEnabled(false); pb.setValue(0); pb.setForeground(C_BLUE);
            Downloader dl=new Downloader(BASE);
            dl.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{pb.setValue(pct);sl.setText(msg);sl.setForeground(C_TEXT2);}));
            new SwingWorker<Void,Void>(){String err;
                @Override protected Void doInBackground(){try{dl.downloadVersion(ver);}catch(Exception ex){err=ex.getMessage();}return null;}
                @Override protected void done(){btn.setEnabled(true);
                    if(err!=null){sl.setText("Ошибка: "+err);sl.setForeground(C_DANGER);pb.setForeground(C_DANGER);}
                    else{sl.setText("✓  Версия "+ver+" установлена");sl.setForeground(C_ACCENT);pb.setForeground(C_ACCENT);pb.setValue(100);}}
            }.execute();
        });
        JPanel wrap=new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.setBorder(new EmptyBorder(20,0,0,0)); wrap.add(card,BorderLayout.NORTH);
        p.add(wrap,BorderLayout.CENTER); return p;
    }

    private JPanel settingsPage() {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(34,34,24,34));
        JLabel t=new JLabel("Настройки запуска"); t.setFont(new Font("Segoe UI",Font.BOLD,28)); t.setForeground(C_TEXT); p.add(t,BorderLayout.NORTH);

        JPanel form=new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS)); form.setBorder(new EmptyBorder(24,0,0,0));

        form.add(sectionLbl("ПАМЯТЬ (RAM)")); form.add(Box.createVerticalStrut(10));
        JPanel rr=new JPanel(new BorderLayout(14,0)); rr.setOpaque(false); rr.setMaximumSize(new Dimension(Integer.MAX_VALUE,50)); rr.setAlignmentX(LEFT_ALIGNMENT);
        ramSlider=new JSlider(512,8192,2048); ramSlider.setOpaque(false); ramSlider.setForeground(C_ACCENT);
        ramLbl=new JLabel("2048 MB"); ramLbl.setFont(new Font("Segoe UI",Font.BOLD,15)); ramLbl.setForeground(C_ACCENT); ramLbl.setPreferredSize(new Dimension(95,32));
        ramSlider.addChangeListener(e->ramLbl.setText(ramSlider.getValue()+" MB"));
        rr.add(ramSlider,BorderLayout.CENTER); rr.add(ramLbl,BorderLayout.EAST); form.add(rr); form.add(Box.createVerticalStrut(26));

        form.add(sectionLbl("JVM АРГУМЕНТЫ")); form.add(Box.createVerticalStrut(10));
        jvmField=styledTF("-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200",new Font("Consolas",Font.PLAIN,12));
        form.add(jvmField); form.add(Box.createVerticalStrut(28));

        // Иконка секция
        form.add(sectionLbl("ИКОНКА ЛАУНЧЕРА")); form.add(Box.createVerticalStrut(10));
        JPanel iconRow=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0)); iconRow.setOpaque(false); iconRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel prev=new JLabel(new ImageIcon(customIcon!=null?customIcon.getScaledInstance(44,44,Image.SCALE_SMOOTH):generateDefaultIcon(44)));
        JButton pickB=accentBtn("  Выбрать файл…  ",C_BLUE,38);
        pickB.addActionListener(e->{ pickIcon(); prev.setIcon(new ImageIcon(customIcon!=null?customIcon.getScaledInstance(44,44,Image.SCALE_SMOOTH):generateDefaultIcon(44))); });
        JButton resetB=flatBtn("✕ Сбросить",C_DANGER);
        resetB.addActionListener(e->{ new File(ICON_PATH).delete(); customIcon=null; applyWindowIcon(); prev.setIcon(new ImageIcon(generateDefaultIcon(44))); });
        iconRow.add(prev); iconRow.add(pickB); iconRow.add(resetB);
        JLabel hint=new JLabel("PNG/JPG, рекомендуется 256×256"); hint.setFont(new Font("Segoe UI",Font.PLAIN,11)); hint.setForeground(C_TEXT3); hint.setAlignmentX(LEFT_ALIGNMENT);
        form.add(iconRow); form.add(Box.createVerticalStrut(6)); form.add(hint); form.add(Box.createVerticalStrut(28));

        JButton save=accentBtn("  Сохранить настройки  ",C_ACCENT,40); save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e->{ saveCfg(); statusLbl.setText("✓  Настройки сохранены"); statusLbl.setForeground(C_ACCENT); });
        form.add(save);

        JScrollPane sc=new JScrollPane(form); sc.setOpaque(false); sc.getViewport().setOpaque(false); sc.setBorder(null);
        p.add(sc,BorderLayout.CENTER); return p;
    }

    private JPanel placeholder(String emoji, String title, String desc) {
        JPanel p=new JPanel(new GridBagLayout()); p.setBackground(C_BG);
        JPanel inner=new JPanel(); inner.setOpaque(false); inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS));
        JLabel e=new JLabel(emoji); e.setFont(new Font("Segoe UI Symbol",Font.PLAIN,52)); e.setForeground(new Color(35,46,68)); e.setAlignmentX(CENTER_ALIGNMENT);
        JLabel t=new JLabel(title); t.setFont(new Font("Segoe UI",Font.BOLD,20)); t.setForeground(new Color(52,65,95)); t.setAlignmentX(CENTER_ALIGNMENT);
        JLabel d=new JLabel(desc); d.setFont(new Font("Segoe UI",Font.PLAIN,13)); d.setForeground(new Color(42,52,78)); d.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(e); inner.add(Box.createVerticalStrut(10)); inner.add(t); inner.add(Box.createVerticalStrut(5)); inner.add(d);
        p.add(inner); return p;
    }

    // ── Footer ────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel f=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(C_BORDER); g2.fillRect(0,0,getWidth(),1);
                GradientPaint grd=new GradientPaint(0,0,new Color(14,17,25),0,getHeight(),new Color(10,12,18));
                g2.setPaint(grd); g2.fillRect(0,1,getWidth(),getHeight()-1); g2.dispose();
            }
        };
        f.setOpaque(false); f.setPreferredSize(new Dimension(0,88)); f.setBorder(new EmptyBorder(0,26,0,26));

        JPanel left=new JPanel(new BorderLayout(0,7)); left.setOpaque(false);
        statusLbl=new JLabel("Готово к запуску"); statusLbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); statusLbl.setForeground(C_TEXT2);
        progressBar=new JProgressBar(0,100); progressBar.setPreferredSize(new Dimension(0,4)); progressBar.setStringPainted(false);
        progressBar.setForeground(C_ACCENT); progressBar.setBackground(C_BORDER); progressBar.setBorderPainted(false); progressBar.setVisible(false);
        left.add(statusLbl,BorderLayout.CENTER); left.add(progressBar,BorderLayout.SOUTH);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,14,0)); right.setOpaque(false);
        JPanel nw=fg("НИК"); nicknameField=styledTF("Player",new Font("Segoe UI",Font.PLAIN,13)); nicknameField.setPreferredSize(new Dimension(160,38)); nw.add(nicknameField);
        JPanel vw=fg("ВЕРСИЯ"); versionBox=new JComboBox<>(VERS); versionBox.setPreferredSize(new Dimension(126,38)); vw.add(versionBox);
        playBtn=accentBtn("▶   ИГРАТЬ",C_ACCENT,46); playBtn.setPreferredSize(new Dimension(162,46)); playBtn.setFont(new Font("Segoe UI",Font.BOLD,14)); playBtn.addActionListener(e->launch());
        right.add(nw); right.add(vw); right.add(playBtn);
        f.add(left,BorderLayout.CENTER); f.add(right,BorderLayout.EAST); return f;
    }

    // ── Launch ────────────────────────────────────────────
    private void launch() {
        String nick=nicknameField.getText().trim(), ver=(String)versionBox.getSelectedItem();
        if (nick.length()<3||!nick.matches("[a-zA-Z0-9_]+")) {
            JOptionPane.showMessageDialog(this,"Никнейм: минимум 3 символа, A-Z 0-9 _","Ошибка",JOptionPane.WARNING_MESSAGE); return;
        }
        saveCfg(); playBtn.setEnabled(false); progressBar.setVisible(true); progressBar.setValue(0); progressBar.setForeground(C_ACCENT); statusLbl.setForeground(C_TEXT2);
        int ram=ramSlider!=null?ramSlider.getValue():2048; String jvm=jvmField!=null?jvmField.getText():"";
        new SwingWorker<Process,Object>(){String err;
            @Override protected Process doInBackground(){try{
                Downloader dl=new Downloader(BASE);
                dl.setProgressCallback((pct,msg)->SwingUtilities.invokeLater(()->{progressBar.setValue(pct);statusLbl.setText(msg);}));
                dl.downloadVersion(ver);
                SwingUtilities.invokeLater(()->statusLbl.setText("Запуск Minecraft "+ver+"…"));
                return new GameLauncher(BASE).launch(ver,nick,ram,jvm,(pct,msg)->SwingUtilities.invokeLater(()->{progressBar.setValue(pct);statusLbl.setText(msg);}));
            }catch(Exception ex){err=ex.getMessage();return null;}}
            @Override protected void done(){progressBar.setVisible(false);playBtn.setEnabled(true);
                if(err!=null){statusLbl.setText("Ошибка: "+err);statusLbl.setForeground(C_DANGER);JOptionPane.showMessageDialog(Main.this,"Ошибка запуска:\n"+err,"Ошибка",JOptionPane.ERROR_MESSAGE);}
                else{statusLbl.setText("✓  Minecraft "+ver+" запущен  •  "+nick);statusLbl.setForeground(C_ACCENT);}}
        }.execute();
    }

    // ── Helpers ───────────────────────────────────────────
    private JPanel glassCard() {
        return new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));
                g2.setColor(C_BORDER); g2.setStroke(new BasicStroke(1f)); g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,14,14)); g2.dispose();
            }
        };
    }
    private JButton accentBtn(String text, Color ac, int h){
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color col=!isEnabled()?new Color(40,50,68):getModel().isPressed()?ac.darker():getModel().isRollover()?ac.brighter():ac;
                g2.setColor(col); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),9,9));
                g2.setColor(isEnabled()?Color.WHITE:C_TEXT3); g2.setFont(getFont()); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
            }
        };
        b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setForeground(Color.WHITE);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private JButton flatBtn(String t, Color fg){ JButton b=new JButton(t); b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);b.setForeground(fg);b.setFont(new Font("Segoe UI",Font.PLAIN,11));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b; }
    private JTextField styledTF(String def, Font font){ JTextField tf=new JTextField(def); tf.setFont(font);tf.setForeground(C_TEXT);tf.setBackground(new Color(11,14,21));tf.setCaretColor(C_ACCENT);tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER,1),new EmptyBorder(6,12,6,12)));tf.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));tf.setAlignmentX(LEFT_ALIGNMENT);return tf; }
    private JPanel fg(String lbl){ JPanel p=new JPanel(new BorderLayout(0,5));p.setOpaque(false);JLabel l=new JLabel(lbl);l.setFont(new Font("Segoe UI",Font.BOLD,9));l.setForeground(C_TEXT3);p.add(l,BorderLayout.NORTH);return p; }
    private JLabel sectionLbl(String t){ JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.BOLD,10));l.setForeground(C_TEXT3);l.setAlignmentX(LEFT_ALIGNMENT);return l; }
    private Component makeSep(){ JSeparator s=new JSeparator();s.setForeground(C_BORDER);s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));return s; }
    private JPanel miniCard(String txt){ JPanel p=new JPanel(){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(new Color(11,14,21));g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));g2.dispose();}};p.setOpaque(false);p.setBorder(new EmptyBorder(8,12,8,12));p.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));p.setAlignmentX(CENTER_ALIGNMENT);JLabel l=new JLabel(txt);l.setFont(new Font("Segoe UI",Font.PLAIN,10));l.setForeground(C_TEXT3);l.setToolTipText(BASE);p.add(l);return p; }

    // ── Config ────────────────────────────────────────────
    private void mkdirs(){ for(String s:new String[]{"","versions","libraries","assets","natives","mods","saves","runtime"}) new File(BASE+(s.isEmpty()?"":File.separator+s)).mkdirs(); }
    private void saveCfg(){ Properties p=new Properties();p.setProperty("nickname",nicknameField.getText().trim());p.setProperty("version",(String)versionBox.getSelectedItem());if(ramSlider!=null)p.setProperty("ram",String.valueOf(ramSlider.getValue()));if(jvmField!=null)p.setProperty("jvm",jvmField.getText());try(OutputStream o=new FileOutputStream(CFG)){p.store(o,"FlytLauncher");}catch(IOException ignored){} }
    private void loadCfg(){ File f=new File(CFG);if(!f.exists())return;Properties p=new Properties();try(InputStream i=new FileInputStream(CFG)){p.load(i);nicknameField.setText(p.getProperty("nickname","Player"));String sv=p.getProperty("version",VERS[0]);for(int k=0;k<VERS.length;k++)if(VERS[k].equals(sv)){versionBox.setSelectedIndex(k);break;}if(ramSlider!=null){ramSlider.setValue(Integer.parseInt(p.getProperty("ram","2048")));ramLbl.setText(ramSlider.getValue()+" MB");}if(jvmField!=null)jvmField.setText(p.getProperty("jvm","-XX:+UseG1GC -XX:+ParallelRefProcEnabled"));}catch(Exception ignored){} }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("ComboBox.background",         new Color(11,14,21));
        UIManager.put("ComboBox.foreground",         new Color(200,210,230));
        UIManager.put("ComboBox.selectionBackground",new Color(28,36,56));
        UIManager.put("Slider.thumbColor",           new Color(72,213,130));
        UIManager.put("Slider.trackColor",           new Color(38,48,72));
        UIManager.put("ProgressBar.background",      new Color(38,48,72));
        SwingUtilities.invokeLater(()->new Main().setVisible(true));
    }
}
