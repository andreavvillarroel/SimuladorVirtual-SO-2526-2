/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gui;

import Logica.DiskManager;
import models.Block;
 
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
/**
 *
 * @author Francisco
 */
// --- Panel personalizado que dibuja el disco como una rejilla de bloques con Graphics2D ---
public class DiskPanel extends JPanel {
 
    // --- Colores del tema dark ---
    static final Color BG_COLOR       = new Color(13,  17,  23);
    static final Color FREE_COLOR     = new Color(30,  35,  45);
    static final Color FREE_BORDER    = new Color(40,  48,  60);
    static final Color GRID_COLOR     = new Color(22,  27,  34);
    static final Color TEXT_COLOR     = new Color(100, 120, 140);
    static final Color HEAD_COLOR     = new Color(251, 191,  36);  // amarillo cabezal
    static final Color CHAIN_COLOR    = new Color(99,  202, 183, 120); // verde encadenado
 
    // --- Dimensiones de cada bloque en píxeles ---
    private static final int BLOCK_SIZE = 56;
    private static final int BLOCK_GAP  = 6;
    private static final int COLS       = 8;
    private static final int PADDING    = 20;
 
    // --- Referencia al disco físico ---
    private final DiskManager diskManager;
 
    // --- Posición actual del cabezal (se actualiza desde el hilo) ---
    private int headPosition = -1;
 
    // --- Bloque seleccionado al hacer hover ---
    private int hoveredBlock = -1;
 
 
    public DiskPanel(DiskManager diskManager) {
        this.diskManager = diskManager;
        setBackground(BG_COLOR);
        setOpaque(true);
 
        // --- Detectar hover sobre bloques ---
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int idx = blockIndexAt(e.getX(), e.getY());
                if (idx != hoveredBlock) {
                    hoveredBlock = idx;
                    repaint();
                }
            }
        });
    }
 
    // --- Actualiza la posición del cabezal y repinta ---
    public void setHeadPosition(int blockIndex) {
        this.headPosition = blockIndex;
        SwingUtilities.invokeLater(this::repaint);
    }
 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 
        Block[] disk = diskManager.getDisk();
 
        for (int i = 0; i < DiskManager.TOTAL_BLOCKS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x   = PADDING + col * (BLOCK_SIZE + BLOCK_GAP);
            int y   = PADDING + row * (BLOCK_SIZE + BLOCK_GAP);
 
            drawBlock(g2, disk[i], i, x, y);
        }
 
        // --- Dibujar flechas de encadenamiento ---
        drawChainArrows(g2, disk);
 
        // --- Dibujar cabezal ---
        if (headPosition >= 0 && headPosition < DiskManager.TOTAL_BLOCKS) {
            drawHead(g2, headPosition);
        }
 
        g2.dispose();
    }
 
    // --- Dibuja un bloque individual con color, ID y efecto hover ---
    private void drawBlock(Graphics2D g2, Block block, int index, int x, int y) {
        RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, BLOCK_SIZE, BLOCK_SIZE, 10, 10);
 
        if (block.isFree()) {
            // Bloque libre: gris oscuro
            g2.setColor(index == hoveredBlock ? FREE_BORDER : FREE_COLOR);
            g2.fill(rect);
            g2.setColor(FREE_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rect);
        } else {
            // Bloque ocupado: color del archivo
            Color fileColor = parseColor(block.getColorHex());
            Color fillColor = index == hoveredBlock
                    ? fileColor.brighter()
                    : new Color(fileColor.getRed(), fileColor.getGreen(), fileColor.getBlue(), 200);
 
            // Fondo con gradiente
            GradientPaint gradient = new GradientPaint(
                    x, y,          fillColor,
                    x, y + BLOCK_SIZE, fileColor.darker()
            );
            g2.setPaint(gradient);
            g2.fill(rect);
 
            // Borde con el color del archivo
            g2.setColor(fileColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(rect);
        }
 
        // --- Número de bloque ---
        g2.setColor(block.isFree() ? TEXT_COLOR : Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        String label = String.valueOf(index);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label,
                x + (BLOCK_SIZE - fm.stringWidth(label)) / 2,
                y + BLOCK_SIZE - 8);
 
        // --- Indicador de puntero al siguiente bloque (punto pequeño) ---
        if (!block.isFree() && block.getNextBlockId() != -1) {
            g2.setColor(CHAIN_COLOR.brighter());
            g2.fillOval(x + BLOCK_SIZE - 10, y + 6, 6, 6);
        }
    }
 
    // --- Dibuja flechas entre bloques encadenados ---
    private void drawChainArrows(Graphics2D g2, Block[] disk) {
        g2.setColor(CHAIN_COLOR);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{4, 3}, 0));
 
        for (int i = 0; i < DiskManager.TOTAL_BLOCKS; i++) {
            if (!disk[i].isFree() && disk[i].getNextBlockId() != -1) {
                int next = disk[i].getNextBlockId();
                Point from = blockCenter(i);
                Point to   = blockCenter(next);
 
                // Línea corta de conexión
                g2.drawLine(from.x, from.y, to.x, to.y);
 
                // Punta de flecha
                drawArrowHead(g2, from, to);
            }
        }
    }
 
    // --- Dibuja la punta de la flecha ---
    private void drawArrowHead(Graphics2D g2, Point from, Point to) {
        double angle  = Math.atan2(to.y - from.y, to.x - from.x);
        int    size   = 6;
        int    ax     = (int) (to.x - size * Math.cos(angle - 0.4));
        int    ay     = (int) (to.y - size * Math.sin(angle - 0.4));
        int    bx     = (int) (to.x - size * Math.cos(angle + 0.4));
        int    by     = (int) (to.y - size * Math.sin(angle + 0.4));
 
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(to.x, to.y, ax, ay);
        g2.drawLine(to.x, to.y, bx, by);
    }
 
    // --- Dibuja el indicador del cabezal sobre el bloque ---
    private void drawHead(Graphics2D g2, int blockIndex) {
        int col = blockIndex % COLS;
        int row = blockIndex / COLS;
        int x   = PADDING + col * (BLOCK_SIZE + BLOCK_GAP);
        int y   = PADDING + row * (BLOCK_SIZE + BLOCK_GAP);
 
        // Borde brillante amarillo
        g2.setColor(HEAD_COLOR);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x - 2, y - 2, BLOCK_SIZE + 4, BLOCK_SIZE + 4, 12, 12);
 
        // Triángulo indicador arriba del bloque
        int[] xPts = {x + BLOCK_SIZE / 2 - 6, x + BLOCK_SIZE / 2 + 6, x + BLOCK_SIZE / 2};
        int[] yPts = {y - 14, y - 14, y - 4};
        g2.setColor(HEAD_COLOR);
        g2.fillPolygon(xPts, yPts, 3);
    }
 
    // --- Calcula el centro de un bloque para dibujar flechas ---
    private Point blockCenter(int index) {
        int col = index % COLS;
        int row = index / COLS;
        int x   = PADDING + col * (BLOCK_SIZE + BLOCK_GAP) + BLOCK_SIZE / 2;
        int y   = PADDING + row * (BLOCK_SIZE + BLOCK_GAP) + BLOCK_SIZE / 2;
        return new Point(x, y);
    }
 
    // --- Retorna el índice del bloque en la posición del mouse, o -1 si no hay ---
    private int blockIndexAt(int mx, int my) {
        for (int i = 0; i < DiskManager.TOTAL_BLOCKS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x   = PADDING + col * (BLOCK_SIZE + BLOCK_GAP);
            int y   = PADDING + row * (BLOCK_SIZE + BLOCK_GAP);
            if (mx >= x && mx <= x + BLOCK_SIZE && my >= y && my <= y + BLOCK_SIZE) {
                return i;
            }
        }
        return -1;
    }
 
    // --- Convierte un string hex a Color de forma segura ---
    private Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return new Color(79, 142, 247);
        }
    }
 
    @Override
    public Dimension getPreferredSize() {
        int rows  = (int) Math.ceil((double) DiskManager.TOTAL_BLOCKS / COLS);
        int width  = PADDING * 2 + COLS * (BLOCK_SIZE + BLOCK_GAP);
        int height = PADDING * 2 + rows * (BLOCK_SIZE + BLOCK_GAP) + 20;
        return new Dimension(width, height);
    }
}
