/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gui;

import Logica.DiskManager;
 
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Francisco
 */
// --- Panel que muestra la posición del cabezal como una barra animada ---
public class HeadPanel extends JPanel {
 
    private static final Color BG_COLOR      = new Color(10, 14, 20);
    private static final Color TRACK_COLOR   = new Color(25, 30, 40);
    private static final Color HEAD_COLOR    = new Color(251, 191, 36);
    private static final Color LABEL_COLOR   = new Color(100, 120, 140);
    private static final Color VISITED_COLOR = new Color(251, 191, 36, 40);
 
    // --- Posición actual del cabezal ---
    private int currentPosition = 0;
 
    // --- Historial de posiciones para dibujar el recorrido ---
    private final java.util.List<Integer> visitedPositions = new java.util.ArrayList<>();
 
    // --- Animación: posición visual interpolada ---
    private float visualPosition = 0f;
    private Timer animTimer;
 
 
    public HeadPanel() {
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(0, 72));
 
        // --- Timer que suaviza el movimiento del cabezal ---
        animTimer = new Timer(16, e -> {
            float diff = currentPosition - visualPosition;
            if (Math.abs(diff) < 0.5f) {
                visualPosition = currentPosition;
            } else {
                visualPosition += diff * 0.15f; // easing suave
            }
            repaint();
        });
        animTimer.start();
    }
 
    // --- Mueve el cabezal a una nueva posición y guarda el historial ---
    public void moveTo(int blockIndex) {
        this.currentPosition = blockIndex;
        visitedPositions.add(blockIndex);
        // Limitar historial a los últimos 20 movimientos
        if (visitedPositions.size() > 20) {
            visitedPositions.remove(0);
        }
    }
 
    // --- Limpia el historial de recorrido ---
    public void clearHistory() {
        visitedPositions.clear();
        repaint();
    }
 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 
        int w        = getWidth();
        int h        = getHeight();
        int trackY   = h / 2 + 10;
        int padding  = 30;
        int trackW   = w - padding * 2;
 
        // --- Pista del cabezal ---
        g2.setColor(TRACK_COLOR);
        g2.fillRoundRect(padding, trackY - 3, trackW, 6, 4, 4);
 
        // --- Marcas de bloques ---
        g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g2.setColor(LABEL_COLOR);
        int step = DiskManager.TOTAL_BLOCKS <= 64 ? 8 : DiskManager.TOTAL_BLOCKS <= 128 ? 16 : 32;
        for (int i = 0; i <= DiskManager.TOTAL_BLOCKS; i += step) {
            int x = padding + (int) ((float) i / DiskManager.TOTAL_BLOCKS * trackW);
            g2.drawLine(x, trackY - 6, x, trackY + 6);
            g2.drawString(String.valueOf(i), x - 4, trackY + 18);
        }
 
        // --- Historial de posiciones visitadas ---
        for (int pos : visitedPositions) {
            int x = padding + (int) ((float) pos / DiskManager.TOTAL_BLOCKS * trackW);
            g2.setColor(VISITED_COLOR);
            g2.fillOval(x - 4, trackY - 4, 8, 8);
        }
 
        // --- Cabezal actual ---
        int headX = padding + (int) (visualPosition / DiskManager.TOTAL_BLOCKS * trackW);
 
        // Halo
        g2.setColor(new Color(251, 191, 36, 40));
        g2.fillOval(headX - 12, trackY - 12, 24, 24);
 
        // Círculo principal
        g2.setColor(HEAD_COLOR);
        g2.fillOval(headX - 7, trackY - 7, 14, 14);
 
        // Etiqueta de posición
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        String label = "Bloque " + currentPosition;
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(label);
        int lx = Math.max(padding, Math.min(headX - lw / 2, w - padding - lw));
        int ly = trackY - 16;
        
        // Fondo oscuro para evitar que se solape con el borde del TitledBorder
        g2.setColor(new Color(10, 14, 20, 200));
        g2.fillRoundRect(lx - 4, ly - fm.getAscent(), lw + 8, fm.getHeight(), 4, 4);
        g2.setColor(HEAD_COLOR);
        g2.drawString(label, lx, ly);
 
        g2.dispose();
    }
}
