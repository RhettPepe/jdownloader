import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.DecimalFormat;

/**
 * JDownloader 2 Bootstrapper
 * Downloads and launches JDownloader 2 from the official source.
 */
public class Main extends JFrame {

    // ── Official JD2 setup jar (mirrors official installer)
    private static final String JD2_URL =
            "https://installer.jdownloader.org/JDownloader2Setup.exe";
    private static final String JD2_FILENAME = "JDownloader2Setup.exe";

    // ── UI Components
    private JLabel  statusLabel;
    private JLabel  speedLabel;
    private JLabel  sizeLabel;
    private JProgressBar progressBar;
    private JButton actionButton;
    private JLabel  iconLabel;

    private volatile boolean downloading = false;
    private File    destFile;

    // ─────────────────────────────────────────────────────────────────────────
    public Main() {
        super("JDownloader 2 — Installer");
        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 300);
        setMinimumSize(new Dimension(480, 260));
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void buildUI() {
        // Dark theme colours
        Color BG      = new Color(22, 22, 30);
        Color SURFACE = new Color(32, 32, 46);
        Color ACCENT  = new Color(82, 130, 255);
        Color TEXT    = new Color(220, 220, 235);
        Color MUTED   = new Color(130, 130, 155);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        // ── Top banner ──────────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        topPanel.setBackground(SURFACE);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 72)));

        iconLabel = new JLabel("⬇");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 34));
        iconLabel.setForeground(ACCENT);
        topPanel.add(iconLabel);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("JDownloader 2");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Download Manager Installer");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);
        titleBox.add(title);
        titleBox.add(subtitle);
        topPanel.add(titleBox);
        add(topPanel, BorderLayout.NORTH);

        // ── Centre panel ─────────────────────────────────────────────────────
        JPanel centre = new JPanel();
        centre.setBackground(BG);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBorder(new EmptyBorder(22, 28, 14, 28));

        statusLabel = new JLabel("Ready to download JDownloader 2 installer.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(statusLabel);
        centre.add(Box.createVerticalStrut(14));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(SURFACE);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(440, 22));
        progressBar.setMaximumSize (new Dimension(Integer.MAX_VALUE, 22));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(progressBar);
        centre.add(Box.createVerticalStrut(10));

        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedLabel = new JLabel("Speed: —");
        speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        speedLabel.setForeground(MUTED);
        sizeLabel  = new JLabel("   |   Size: —");
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLabel.setForeground(MUTED);
        infoRow.add(speedLabel);
        infoRow.add(sizeLabel);
        centre.add(infoRow);

        add(centre, BorderLayout.CENTER);

        // ── Bottom button row ────────────────────────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        bottom.setBackground(SURFACE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 72)));

        actionButton = new JButton("Download & Install");
        styleButton(actionButton, ACCENT, TEXT);
        actionButton.addActionListener(e -> onAction());
        bottom.add(actionButton);

        add(bottom, BorderLayout.SOUTH);
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 34));
        btn.addMouseListener(new MouseAdapter() {
            Color orig = bg;
            public void mouseEntered(MouseEvent e) { btn.setBackground(orig.brighter()); }
            public void mouseExited (MouseEvent e) { btn.setBackground(orig); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void onAction() {
        if (downloading) return;

        // Default save into user Downloads folder
        String userHome  = System.getProperty("user.home");
        destFile = new File(userHome + File.separator + "Downloads"
                          + File.separator + JD2_FILENAME);

        // Let user override destination
        JFileChooser fc = new JFileChooser(new File(userHome + "\\Downloads"));
        fc.setSelectedFile(destFile);
        fc.setDialogTitle("Save JDownloader2 installer as…");
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        destFile = fc.getSelectedFile();

        downloading = true;
        actionButton.setEnabled(false);
        progressBar.setValue(0);

        Thread t = new Thread(this::downloadJD2, "JD2-Downloader");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void downloadJD2() {
        try {
            URL url = new URL(JD2_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.connect();

            long total = conn.getContentLengthLong();
            DecimalFormat df = new DecimalFormat("#.##");

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Downloading  " + JD2_FILENAME + "…");
                if (total > 0) {
                    sizeLabel.setText("   |   Size: " + df.format(total / 1_048_576.0) + " MB");
                }
            });

            long downloaded = 0;
            long lastTime   = System.currentTimeMillis();
            long lastBytes  = 0;

            try (InputStream in  = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(
                         new FileOutputStream(destFile))) {

                byte[] buf = new byte[65536];
                int    n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    downloaded += n;

                    long now   = System.currentTimeMillis();
                    long delta = now - lastTime;
                    if (delta >= 500) {
                        long bps       = (downloaded - lastBytes) * 1000 / delta;
                        lastTime  = now;
                        lastBytes = downloaded;

                        final long d = downloaded;
                        final long t2 = total;
                        final long s = bps;
                        SwingUtilities.invokeLater(() -> {
                            if (t2 > 0) {
                                int pct = (int)(d * 100 / t2);
                                progressBar.setValue(pct);
                                progressBar.setString(pct + "%");
                            } else {
                                progressBar.setIndeterminate(true);
                            }
                            speedLabel.setText("Speed: " + df.format(s / 1024.0) + " KB/s");
                        });
                    }
                }
            }

            // ── Download complete ────────────────────────────────────────────
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                progressBar.setIndeterminate(false);
                progressBar.setString("Done!");
                statusLabel.setText("✔  Saved to: " + destFile.getAbsolutePath());
                speedLabel.setText("Speed: —");

                int launch = JOptionPane.showConfirmDialog(this,
                        "Download complete!\n\nLaunch the JDownloader 2 installer now?",
                        "Complete", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (launch == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().open(destFile);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Could not launch installer:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                downloading = false;
                actionButton.setEnabled(true);
                actionButton.setText("Download Again");
            });

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("✖  Error: " + ex.getMessage());
                progressBar.setValue(0);
                progressBar.setString("Failed");
                downloading = false;
                actionButton.setEnabled(true);
            });
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(Main::new);
    }
}
