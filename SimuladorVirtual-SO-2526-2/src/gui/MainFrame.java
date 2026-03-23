/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gui;

import Logica.DiskManager;
import Logica.FileSystemManager;
import Logica.JournalManager;
import Logica.LockManager;
import algoritmos.DiskScheduler;
import models.Block;
import models.Directory;
import models.JournalEntry;
import models.Process;
import dataStructures.MyList;
import persistence.JsonSaver;
 
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author Francisco
 */
// --- Ventana principal del simulador: conecta todos los subsistemas con la GUI ---
public class MainFrame extends JFrame implements Observer {
 
    // ================================================================
    //  Paleta de colores dark — tema "Control de Misión"
    // ================================================================
    private static final Color C_BG        = new Color(10,  14,  20);
    private static final Color C_PANEL     = new Color(15,  20,  30);
    private static final Color C_SURFACE   = new Color(20,  26,  38);
    private static final Color C_BORDER    = new Color(35,  43,  58);
    private static final Color C_ACCENT    = new Color(79, 142, 247);   // azul
    private static final Color C_GREEN     = new Color(79, 201, 164);   // verde
    private static final Color C_YELLOW    = new Color(251,191,  36);   // amarillo cabezal
    private static final Color C_RED       = new Color(247,  99,  99);  // rojo peligro
    private static final Color C_TEXT      = new Color(200, 210, 230);
    private static final Color C_MUTED     = new Color(90, 110, 140);
    private static final Font  F_MONO      = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font  F_BOLD      = new Font("Monospaced", Font.BOLD,  13);
    private static final Font  F_TITLE     = new Font("Monospaced", Font.BOLD,  11);
 
    // ================================================================
    //  Subsistemas lógicos
    // ================================================================
    private final DiskManager       diskManager;
    private final JournalManager    journalManager;
    private final LockManager       lockManager;
    private final FileSystemManager fsManager;
    private DiskScheduler           scheduler;
    private Thread                  schedulerThread;
 
    // ================================================================
    //  Componentes de la GUI
    // ================================================================
 
    // --- Oeste: árbol de directorios ---
    private JTree            dirTree;
    private DefaultTreeModel treeModel;
    private JLabel           lblInspectorName;
    private JLabel           lblInspectorSize;
    private JLabel           lblInspectorOwner;
    private JLabel           lblInspectorBlock;
 
    // --- Centro: disco y cabezal ---
    private DiskPanel        diskPanel;
    private HeadPanel        headPanel;
 
    // --- Este: tabla de archivos ---
    private JTable           fileTable;
    private DefaultTableModel fileTableModel;
 
    // --- Sur: journal y consola ---
    private JTable           journalTable;
    private DefaultTableModel journalTableModel;
    private JTextArea        consoleArea;
 
    // --- Controles superiores ---
    private JComboBox<String> comboAlgorithm;
    private JComboBox<String> comboMode;
    private JTextField        tfHeadStart;
    private JButton           btnCreate;
    private JButton           btnCreateDir;
    private JButton           btnDelete;
    private JButton           btnCrash;
    private JButton           btnRecover;
    private JButton           btnSave;
    private JButton           btnLoad;
    private JButton           btnRunScheduler;
 
    // --- Estado de modo (Admin / Usuario) ---
    private boolean adminMode = true;
    
    // --- Solicitudes del último test cargado (preserva el orden del JSON) ---
    private MyList<Process> testRequests = null;
 
    // --- Colores para asignar a nuevos archivos ---
    private static final String[] FILE_COLORS = {
        "#4F8EF7","#4FC9A4","#F7934F","#C084FC",
        "#F7D44F","#F76363","#00C9A7","#F72585"
    };
    private int colorIndex = 0;
 
 
    // ================================================================
    //  Constructor
    // ================================================================
    public MainFrame() {
        // --- Inicializar subsistemas ---
        diskManager    = new DiskManager();
        journalManager = new JournalManager();
        lockManager    = new LockManager();
        fsManager      = new FileSystemManager(diskManager, lockManager, journalManager);
        journalManager.setDiskManager(diskManager);
        scheduler      = new DiskScheduler(DiskScheduler.FIFO);
 
        // --- Registrar esta ventana como Observer de todos los subsistemas ---
        diskManager.addObserver(this);
        journalManager.addObserver(this);
        lockManager.addObserver(this);
        fsManager.addObserver(this);
        scheduler.addObserver(this);
 
        // --- Construir la ventana ---
        setTitle("Sistema de Archivos Simulado");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setBackground(C_BG);
 
        applyGlobalTheme();
        buildUI();
        ToolTipManager.sharedInstance().setEnabled(false);
        disableAllTooltips(getContentPane());
        pack();
        setLocationRelativeTo(null);
 
        // --- Directorio inicial ---
        fsManager.createDirectory("/", "home", "root");
        refreshTree();
        log("Sistema inicializado. Directorio raíz creado.");
    }
 
    // ================================================================
    //  Observer — recibe señales de los subsistemas
    // ================================================================
    @Override
    public void update(Observable source, Object signal) {
        String msg = String.valueOf(signal);
        SwingUtilities.invokeLater(() -> {
            switch (msg) {
                case "DISK_UPDATED"    -> { refreshDisk(); refreshFileTable(); }
                case "FS_UPDATED"      -> { refreshTree(); refreshFileTable(); }
                case "LOCK_UPDATED"    -> refreshFileTable();
                case "JOURNAL_UPDATED" -> refreshJournalTable();
                case "CRASH_SIMULATED" -> { refreshJournalTable(); log("⚠ Crash simulado."); }
                default -> {
                    if (msg.startsWith("HEAD:")) {
                        int pos = Integer.parseInt(msg.substring(5));
                        diskPanel.setHeadPosition(pos);
                        headPanel.moveTo(pos);
                    } else if (msg.startsWith("REQUEST_SERVED:")) {
                        int pos = Integer.parseInt(msg.substring(15));
                        log("✓ Solicitud atendida → bloque " + pos);
                    } else if (msg.startsWith("RECOVERY_COMPLETE:")) {
                        log("✓ Recuperación completada. Operaciones deshechas: "
                                + msg.substring(18));
                        refreshJournalTable();
                    }
                }
            }
        });
    }
 
    // ================================================================
    //  Construcción de la interfaz
    // ================================================================
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(C_BG);
        
        JPanel topBar = buildTopBar();
        
        add(topBar,    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        
        // Panel sur: cabezal arriba, journal+consola abajo
        JPanel southPane = darkPanel(new BorderLayout(0, 0));
        southPane.add(buildHeadBar(),   BorderLayout.NORTH);
        southPane.add(buildBottomBar(), BorderLayout.CENTER);
        add(southPane, BorderLayout.SOUTH);
    }
 
    // ----------------------------------------------------------------
    //  Barra superior — controles y algoritmos
    // ----------------------------------------------------------------
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new WrapLayout(WrapLayout.LEFT, 6, 4));
        bar.setBackground(C_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
 
        // --- Modo ---
        bar.add(label("MODO:", C_MUTED));
        comboMode = combo(new String[]{"Administrador", "Usuario"});
        comboMode.addActionListener(e -> {
            adminMode = comboMode.getSelectedIndex() == 0;
            updateButtonStates();
            log("Modo cambiado a: " + comboMode.getSelectedItem());
        });
        bar.add(comboMode);
 
        // --- CRUD ---
        btnCreate    = button("+ Archivo",  C_ACCENT);
        btnCreateDir = button("+ Carpeta",  C_GREEN);
        btnDelete    = button("Eliminar",   C_RED);
        btnCreate.addActionListener(e    -> dialogCreateFile());
        btnCreateDir.addActionListener(e -> dialogCreateDir());
        btnDelete.addActionListener(e    -> deleteSelected());
        bar.add(btnCreate);
        bar.add(btnCreateDir);
        bar.add(btnDelete);
 
        // --- Planificador ---
        bar.add(label("ALGORITMO:", C_MUTED));
        comboAlgorithm = combo(new String[]{
            DiskScheduler.FIFO, DiskScheduler.SSTF,
            DiskScheduler.SCAN, DiskScheduler.C_SCAN
        });
        comboAlgorithm.addActionListener(e ->
            scheduler.setAlgorithm((String) comboAlgorithm.getSelectedItem()));
        bar.add(comboAlgorithm);
 
        bar.add(label("Cabezal:", C_MUTED));
        tfHeadStart = new JTextField("0", 3);
        styleTextField(tfHeadStart);
        bar.add(tfHeadStart);
 
        btnRunScheduler = button("▶ Ejecutar", C_YELLOW);
        btnRunScheduler.setForeground(C_BG);
        btnRunScheduler.addActionListener(e -> runScheduler());
        bar.add(btnRunScheduler);
 
        // --- Journal ---
        btnCrash   = button("⚡ Fallo",    C_RED);
        btnRecover = button("↺ Recuperar", C_GREEN);
        btnCrash.addActionListener(e   -> simulateCrash());
        btnRecover.addActionListener(e -> recoverSystem());
        bar.add(btnCrash);
        bar.add(btnRecover);
 
        // --- Persistencia ---
        btnSave = button("💾 Guardar", C_MUTED);
        btnLoad = button("📂 Cargar",  C_MUTED);
        btnSave.addActionListener(e -> saveSystem());
        btnLoad.addActionListener(e -> loadSystem());
        bar.add(btnSave);
        bar.add(btnLoad);
 
        return bar;
    }
 
    // ----------------------------------------------------------------
    //  Panel central — árbol | disco | tabla
    // ----------------------------------------------------------------
    private JSplitPane buildCenter() {
        JSplitPane leftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildWestPanel(), buildCenterAndEast());
        leftRight.setDividerLocation(220);
        leftRight.setBackground(C_BG);
        leftRight.setBorder(null);
        leftRight.setDividerSize(4);
        return leftRight;
    }
 
    // --- Oeste: árbol + inspector ---
    private JPanel buildWestPanel() {
        JPanel panel = darkPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 4));
 
        // Árbol
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("/");
        treeModel = new DefaultTreeModel(rootNode);
        dirTree   = new JTree(treeModel);
        dirTree.setBackground(C_SURFACE);
        dirTree.setForeground(C_TEXT);
        dirTree.setBorder(new EmptyBorder(4, 4, 4, 4));
        dirTree.setFont(F_MONO);
        dirTree.setRowHeight(22);
        styleTreeIcons();
 
        dirTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) dirTree.getLastSelectedPathComponent();
            if (node != null) updateInspector(node.getUserObject().toString());
        });
 
        JScrollPane treeScroll = scrollPane(dirTree);
        treeScroll.setBorder(titledBorder("DIRECTORIOS"));
        panel.add(treeScroll, BorderLayout.CENTER);
 
        // Inspector
        panel.add(buildInspector(), BorderLayout.SOUTH);
        return panel;
    }
 
    private JPanel buildInspector() {
        JPanel p = darkPanel(new GridLayout(5, 1, 2, 2));
        p.setBorder(titledBorder("INSPECTOR"));
        lblInspectorName  = inspectorLabel("—");
        lblInspectorSize  = inspectorLabel("—");
        lblInspectorOwner = inspectorLabel("—");
        lblInspectorBlock = inspectorLabel("—");
        p.add(inspectorRow("Nombre:",  lblInspectorName));
        p.add(inspectorRow("Tamaño:",  lblInspectorSize));
        p.add(inspectorRow("Dueño:",   lblInspectorOwner));
        p.add(inspectorRow("1° Bloque:", lblInspectorBlock));
        return p;
    }
 
    // --- Centro + Este ---
    private JSplitPane buildCenterAndEast() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildDiskArea(), buildEastPanel());
        split.setDividerLocation(560);
        split.setBackground(C_BG);
        split.setBorder(null);
        split.setDividerSize(4);
        return split;
    }
 
    // --- Disco sin cabezal (el cabezal va en buildUI directamente) ---
    private JPanel buildDiskArea() {
        JPanel panel = darkPanel(new BorderLayout(0, 0));
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));
 
        diskPanel = new DiskPanel(diskManager);
        JScrollPane diskScroll = scrollPane(diskPanel);
        diskScroll.setBorder(titledBorder("VISUALIZADOR DE DISCO"));
        panel.add(diskScroll, BorderLayout.CENTER);
 
        return panel;
    }
 
    // --- Panel del cabezal: altura fija, va entre el centro y el bottom ---
    private JPanel buildHeadBar() {
        headPanel = new HeadPanel();
 
        JPanel wrapper = darkPanel(new BorderLayout(0, 2));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, C_BORDER),
                new EmptyBorder(4, 8, 4, 8)));
        wrapper.setPreferredSize(new Dimension(0, 85));
        wrapper.setMinimumSize(new Dimension(0, 85));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
 
        JLabel lblHead = new JLabel("CABEZAL");
        lblHead.setForeground(C_MUTED);
        lblHead.setFont(F_TITLE);
        wrapper.add(lblHead,   BorderLayout.NORTH);
        wrapper.add(headPanel, BorderLayout.CENTER);
 
        return wrapper;
    }
 
    // --- Este: tabla de archivos ---
    private JPanel buildEastPanel() {
        JPanel panel = darkPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 4, 4, 8));
 
        String[] cols = {"Color", "Nombre", "Bloques", "1° Bloque", "Dueño"};
        fileTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Color.class : String.class;
            }
        };
        fileTable = new JTable(fileTableModel);
        styleTable(fileTable);
 
        // Renderer de color
        fileTable.getColumnModel().getColumn(0).setCellRenderer((t, val, sel, foc, r, c) -> {
            JLabel lbl = new JLabel();
            lbl.setOpaque(true);
            if (val instanceof Color) {
                lbl.setBackground((Color) val);
            }
            return lbl;
        });
        fileTable.getColumnModel().getColumn(0).setMaxWidth(30);
 
        JScrollPane scroll = scrollPane(fileTable);
        scroll.setBorder(titledBorder("TABLA DE ARCHIVOS"));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
 
    // ----------------------------------------------------------------
    //  Barra inferior — journal y consola
    // ----------------------------------------------------------------
    private JSplitPane buildBottomBar() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildJournalPanel(), buildConsolePanel());
        split.setDividerLocation(500);
        split.setPreferredSize(new Dimension(0, 180));
        split.setBackground(C_BG);
        split.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        split.setDividerSize(4);
        return split;
    }
 
    private JPanel buildJournalPanel() {
        JPanel panel = darkPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(4, 8, 4, 4));
 
        String[] cols = {"ID", "Operación", "Archivo", "Bloque", "Estado"};
        journalTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        journalTable = new JTable(journalTableModel);
        styleTable(journalTable);
 
        // Renderer que colorea filas según estado
        journalTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String status = String.valueOf(journalTableModel.getValueAt(r, 4));
                setBackground(switch (status) {
                    case "PENDING"   -> new Color(247, 191, 36, 40);
                    case "COMMITTED" -> new Color(79, 201, 164, 40);
                    case "FAILED"    -> new Color(247, 99, 99, 40);
                    default          -> C_SURFACE;
                });
                setForeground(C_TEXT);
                setFont(F_MONO);
                setBorder(new EmptyBorder(2, 6, 2, 6));
                return this;
            }
        });
 
        JScrollPane scroll = scrollPane(journalTable);
        scroll.setBorder(titledBorder("JOURNAL DE TRANSACCIONES"));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
 
    private JPanel buildConsolePanel() {
        JPanel panel = darkPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(4, 4, 4, 8));
 
        consoleArea = new JTextArea();
        consoleArea.setBackground(new Color(8, 11, 16));
        consoleArea.setForeground(C_GREEN);
        consoleArea.setFont(F_MONO);
        consoleArea.setEditable(false);
        consoleArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        consoleArea.setCaretColor(C_GREEN);
 
        JScrollPane scroll = scrollPane(consoleArea);
        scroll.setBorder(titledBorder("CONSOLA"));
        panel.add(scroll, BorderLayout.CENTER);
 
        JButton btnClear = button("Limpiar", C_MUTED);
        btnClear.addActionListener(e -> consoleArea.setText(""));
        panel.add(btnClear, BorderLayout.SOUTH);
        return panel;
    }
 
    // ================================================================
    //  Acciones de los botones
    // ================================================================
 
    // --- Diálogo para crear un archivo ---
    private void dialogCreateFile() {
        JPanel form = buildFormPanel();
        JTextField tfName  = formField(form, "Nombre del archivo:");
        JTextField tfSize  = formField(form, "Tamaño en bloques:");
        JTextField tfOwner = formField(form, "Propietario:");
        JTextField tfDir   = formField(form, "Directorio destino:");
        tfOwner.setText("root");
        tfDir.setText("/");
 
        int result = JOptionPane.showConfirmDialog(this, form,
                "Crear Archivo", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
 
        if (result == JOptionPane.OK_OPTION) {
            String name  = tfName.getText().trim();
            String dir   = tfDir.getText().trim();
            String owner = tfOwner.getText().trim();
            int    size;
            try { size = Integer.parseInt(tfSize.getText().trim()); }
            catch (NumberFormatException ex) {
                showError("El tamaño debe ser un número entero.");
                return;
            }
 
            if (name.isEmpty()) { showError("El nombre no puede estar vacío."); return; }
            if (diskManager.countFreeBlocks() < size) {
                showError("No hay suficientes bloques libres.\nLibres: "
                        + diskManager.countFreeBlocks() + "  Necesarios: " + size);
                return;
            }
 
            String color = FILE_COLORS[colorIndex % FILE_COLORS.length];
            colorIndex++;
            boolean ok = fsManager.createFile(dir, name, size, owner, color);
            log(ok ? "✓ Archivo '" + name + "' creado en '" + dir + "' (" + size + " bloques)"
                   : "✗ No se pudo crear '" + name + "'");
        }
    }
 
    // --- Diálogo para crear directorio ---
    private void dialogCreateDir() {
        JPanel form = buildFormPanel();
        JTextField tfName   = formField(form, "Nombre del directorio:");
        JTextField tfParent = formField(form, "Directorio padre:");
        JTextField tfOwner  = formField(form, "Propietario:");
        tfParent.setText("/");
        tfOwner.setText("root");
 
        int result = JOptionPane.showConfirmDialog(this, form,
                "Crear Directorio", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
 
        if (result == JOptionPane.OK_OPTION) {
            String name   = tfName.getText().trim();
            String parent = tfParent.getText().trim();
            String owner  = tfOwner.getText().trim();
            if (name.isEmpty()) { showError("El nombre no puede estar vacío."); return; }
            fsManager.createDirectory(parent, name, owner);
            log("✓ Directorio '" + name + "' creado en '" + parent + "'");
        }
    }
 
    // --- Elimina el archivo seleccionado en la tabla ---
    private void deleteSelected() {
        int row = fileTable.getSelectedRow();
        if (row < 0) { showError("Selecciona un archivo en la tabla para eliminar."); return; }
 
        String fileName = (String) fileTableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el archivo '" + fileName + "'?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
 
        if (confirm == JOptionPane.YES_OPTION) {
            // Buscar en qué directorio está
            boolean deleted = tryDeleteInTree(fsManager.getRoot(), fileName);
            log(deleted ? "✓ Archivo '" + fileName + "' eliminado"
                        : "✗ No se pudo eliminar '" + fileName + "'");
        }
    }
 
    // --- Busca y elimina un archivo recorriendo el árbol ---
    private boolean tryDeleteInTree(Directory dir, String fileName) {
        MyList<models.File> files = dir.getFiles();
        for (int i = 0; i < files.getSize(); i++) {
            if (files.get(i).getName().equals(fileName)) {
                return fsManager.deleteFile(dir.getName(), fileName, "root");
            }
        }
        MyList<Directory> subs = dir.getSubDirectories();
        for (int i = 0; i < subs.getSize(); i++) {
            if (tryDeleteInTree(subs.get(i), fileName)) return true;
        }
        return false;
    }
 
    // --- Inicia el DiskScheduler en un hilo separado ---
    private void runScheduler() {
        // Detener el hilo anterior si existe
        if (schedulerThread != null && schedulerThread.isAlive()) {
            scheduler.stop();
        }
 
        // Leer posición inicial
        int headStart = 0;
        try { headStart = Integer.parseInt(tfHeadStart.getText().trim()); }
        catch (NumberFormatException ignored) {}
 
        // Crear nuevo scheduler con el algoritmo y posición seleccionados
        scheduler = new DiskScheduler((String) comboAlgorithm.getSelectedItem());
        scheduler.setHeadPosition(headStart);
        scheduler.addObserver(this);
 
        // Usar las solicitudes del test si hay un JSON cargado,
        // si no, encolar los bloques ocupados del disco en orden
        if (testRequests != null && testRequests.getSize() > 0) {
            for (int i = 0; i < testRequests.getSize(); i++) {
                Process p = testRequests.get(i);
                scheduler.addRequest(new Process(p.getPid(), p.getOperation(), p.getTargetBlock()));
            }
        } else {
            Block[] disk = diskManager.getDisk();
            int pid = 1;
            for (int i = 0; i < DiskManager.TOTAL_BLOCKS; i++) {
                if (!disk[i].isFree()) {
                    scheduler.addRequest(new Process(pid++, "READ", i));
                }
            }
        }
        headPanel.clearHistory();
        headPanel.moveTo(headStart);
        diskPanel.setHeadPosition(headStart);
 
        schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();
 
        log("▶ Scheduler iniciado con algoritmo: " + scheduler.getAlgorithm()
                + " | Cabezal en bloque " + headStart);
    }
 
    // --- Simula un crash en el journal ---
    private void simulateCrash() {
        journalManager.simulateCrash(2);
        log("⚠ Crash simulado: últimas 2 operaciones marcadas como PENDING.");
    }
 
    // --- Recupera el sistema deshaciendo operaciones PENDING ---
    private void recoverSystem() {
        int undone = journalManager.recoverSystem();
        lockManager.releaseAll();
        refreshDisk();
        log("↺ Recuperación completada. Operaciones deshechas: " + undone);
    }
 
    // --- Guarda el sistema usando JsonSaver ---
    private void saveSystem() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar sistema de archivos");
        chooser.setSelectedFile(new java.io.File("system_save.json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
 
        String path = chooser.getSelectedFile().getAbsolutePath();
        // Asegurarse de que tenga extensión .json
        if (!path.endsWith(".json")) path += ".json";
        JsonSaver.save(fsManager.getRoot(), path);
        log("💾 Sistema guardado en: " + path);
    }
 
    // --- Carga un JSON guardado por JsonSaver y reconstruye el árbol y el disco ---
    private void loadSystem() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Cargar JSON");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
 
        String path = chooser.getSelectedFile().getAbsolutePath();
 
        // Leer el archivo completo
        String json;
        try {
            json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
        } catch (Exception e) {
            showError("No se pudo leer el archivo: " + e.getMessage());
            return;
        }
 
        // Detectar formato por clave principal
        if (json.contains("\"test_id\"")) {
            loadTestJson(path);
        } else if (json.contains("\"root\"")) {
            loadSavedSystem(path, json);
        } else {
            showError("Formato JSON no reconocido.\nEl archivo debe contener 'test_id' o 'root'.");
        }
    }
 
    // --- Carga recursivamente un directorio desde su bloque JSON ---
    private void loadDirectory(String block, String parentName) {
        String dirName = extractString(block, "name");
 
        // Crear el directorio si no es la raíz
        if (!dirName.equals("/")) {
            fsManager.createDirectory(parentName, dirName, "root");
        }
 
        String currentDir = dirName.equals("/") ? "/" : dirName;
 
        // Cargar archivos del directorio
        String filesArray = extractArray(block, "files");
        if (filesArray != null && !filesArray.isBlank()) {
            for (String fileObj : splitObjects(filesArray)) {
                String name       = extractString(fileObj, "name");
                int    size       = extractInt(fileObj, "size");
                String owner      = extractString(fileObj, "owner");
                String color      = extractString(fileObj, "color");
                if (name.isEmpty()) continue;
                if (color.isEmpty()) color = FILE_COLORS[colorIndex % FILE_COLORS.length];
                colorIndex++;
                fsManager.createFile(currentDir, name, size, owner, color);
            }
        }
 
        // Cargar subdirectorios recursivamente
        String subsArray = extractArray(block, "subdirectories");
        if (subsArray != null && !subsArray.isBlank()) {
            for (String subObj : splitObjects(subsArray)) {
                if (!subObj.isBlank()) loadDirectory(subObj, currentDir);
            }
        }
    }
 
    // --- Extrae el valor String de una clave JSON ---
        private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int colon = json.indexOf(':', idx);
        if (colon == -1) return "";
        int q1 = json.indexOf('"', colon + 1);
        if (q1 == -1) return "";
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 == -1) return "";
        return json.substring(q1 + 1, q2);
    }
 
    // --- Extrae el valor entero de una clave JSON ---
    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 1;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return 1;
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else if (!Character.isWhitespace(c) && num.length() > 0) break;
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : 1;
    }
 
    // --- Extrae el contenido de un bloque { } asociado a una clave ---
    private String extractBlock(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int start = json.indexOf('{', idx);
        if (start == -1) return "";
        return extractBalanced(json, start, '{', '}');
    }
 
    // --- Extrae el contenido de un array [ ] asociado a una clave ---
    private String extractArray(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = json.indexOf('[', idx);
        if (start == -1) return null;
        int end = findClosing(json, start, '[', ']');
        if (end == -1) return null;
        return json.substring(start + 1, end).trim();
    }
 
    // --- Extrae un bloque balanceado desde la posición dada ---
    private String extractBalanced(String json, int start, char open, char close) {
        int level = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == open)  level++;
            if (json.charAt(i) == close) { level--; if (level == 0) return json.substring(start, i + 1); }
        }
        return "";
    }
 
    // --- Encuentra el índice del cierre balanceado ---
    private int findClosing(String json, int start, char open, char close) {
        int level = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == open)  level++;
            if (json.charAt(i) == close) { level--; if (level == 0) return i; }
        }
        return -1;
    }
 
    // --- Divide el contenido de un array en objetos { } individuales ---
    private java.util.List<String> splitObjects(String array) {
        java.util.List<String> result = new java.util.ArrayList<>();
        int i = 0;
        while (i < array.length()) {
            int start = array.indexOf('{', i);
            if (start == -1) break;
            int end = findClosing(array, start, '{', '}');
            if (end == -1) break;
            result.add(array.substring(start, end + 1));
            i = end + 1;
        }
        return result;
    }
 
    // ================================================================
    //  Refresh de componentes visuales
    // ================================================================
    
    // --- Carga un JSON en formato JsonSaver (root + files + subdirectories) ---
    private void loadSavedSystem(String path, String json) {
        resetDisk();
        journalManager.clearJournal();
        lockManager.releaseAll();
        colorIndex = 0;
 
        try {
            String rootBlock = extractBlock(json, "root");
            loadDirectory(rootBlock, "/");
        } catch (Exception e) {
            showError("Error al parsear el JSON: " + e.getMessage());
            return;
        }
 
        refreshTree();
        refreshDisk();
        refreshFileTable();
        log("📂 Sistema cargado desde: " + new java.io.File(path).getName());
    }
 
    // --- Carga un JSON en formato P1.json (test_id + requests + system_files) ---
    private void loadTestJson(String path) {
        // Usar JsonLoader para parsear el formato test_id/requests/system_files
        persistence.JsonLoader.TestData data;
        try {
            data = persistence.JsonLoader.cargar(path);
        } catch (Exception e) {
            showError("No se pudo cargar el test: " + e.getMessage());
            return;
        }
 
        // Verificar que el disco tenga espacio suficiente
        int totalBloques = 0;
        for (int i = 0; i < data.archivos.getSize(); i++) {
            totalBloques += Integer.parseInt(data.archivos.get(i)[2]);
        }
        if (totalBloques > DiskManager.TOTAL_BLOCKS) {
            showError("El test requiere " + totalBloques + " bloques pero el disco tiene "
                    + DiskManager.TOTAL_BLOCKS + "."
                    + "Aumenta TOTAL_BLOCKS en DiskManager a " + nextPowerOf2(totalBloques) + ".");
            return;
        };
 
        // Reiniciar el sistema
        resetDisk();
        journalManager.clearJournal();
        lockManager.releaseAll();
        colorIndex = 0;
 
        // Crear directorio de trabajo y cargar archivos
        fsManager.createDirectory("/", "test", "root");
        String[] colors = {"#FF6B6B","#FFD93D","#6BCB77","#4D96FF",
                           "#C77DFF","#FF9A3C","#00C9A7","#F72585"};
        for (int i = 0; i < data.archivos.getSize(); i++) {
            String[] a     = data.archivos.get(i);
            String   name  = a[1];
            int      size  = Integer.parseInt(a[2]);
            String   color = colors[i % colors.length];
            fsManager.createFile("test", name, size, "root", color);
        }
        
        // Guardar las solicitudes del test para usarlas al ejecutar el scheduler
        testRequests = data.procesos;
 
        // Configurar el scheduler con el cabezal inicial del JSON
        tfHeadStart.setText(String.valueOf(data.initialHead));
        scheduler.setHeadPosition(data.initialHead);
        headPanel.moveTo(data.initialHead);
        diskPanel.setHeadPosition(data.initialHead);
 
        // Encolar las solicitudes del JSON
        for (int i = 0; i < data.procesos.getSize(); i++) {
            scheduler.addRequest(data.procesos.get(i));
        }
 
        refreshTree();
        refreshDisk();
        refreshFileTable();
 
        log("📋 Test cargado: " + data.testId
                + " | Cabezal: " + data.initialHead
                + " | Archivos: " + data.archivos.getSize()
                + " | Solicitudes: " + data.procesos.getSize());
        log("   Presiona ▶ Ejecutar para iniciar el scheduler.");
    }
 
    // --- Libera todos los bloques ocupados del disco ---
    private void resetDisk() {
        boolean[] isLinked = new boolean[DiskManager.TOTAL_BLOCKS];
        for (int i = 0; i < DiskManager.TOTAL_BLOCKS; i++) {
            int next = diskManager.getBlock(i).getNextBlockId();
            if (next != -1 && next < DiskManager.TOTAL_BLOCKS) isLinked[next] = true;
        }
        for (int i = 0; i < DiskManager.TOTAL_BLOCKS; i++) {
            if (!diskManager.getBlock(i).isFree() && !isLinked[i]) {
                diskManager.freeBlocks(i);
            }
        }
    }
    
     // --- Retorna la siguiente potencia de 2 mayor o igual a n ---
    private int nextPowerOf2(int n) {
        int p = 64;
        while (p < n) p *= 2;
        return p;
    }
 
    // --- Repinta el disco ---
    private void refreshDisk() {
        diskPanel.repaint();
    }
 
    // --- Reconstruye el JTree desde el árbol de directorios ---
    private void refreshTree() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("/");
        buildTreeNode(rootNode, fsManager.getRoot());
        treeModel.setRoot(rootNode);
        expandAllTree();
    }
 
    private void buildTreeNode(DefaultMutableTreeNode parent, Directory dir) {
        // Subdirectorios
        MyList<Directory> subs = dir.getSubDirectories();
        for (int i = 0; i < subs.getSize(); i++) {
            Directory sub = subs.get(i);
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode("📁 " + sub.getName());
            parent.add(subNode);
            buildTreeNode(subNode, sub);
        }
        // Archivos
        MyList<models.File> files = dir.getFiles();
        for (int i = 0; i < files.getSize(); i++) {
            parent.add(new DefaultMutableTreeNode("📄 " + files.get(i).getName()));
        }
    }
 
    private void expandAllTree() {
        for (int i = 0; i < dirTree.getRowCount(); i++) {
            dirTree.expandRow(i);
        }
    }
 
    // --- Actualiza la tabla de archivos con todos los archivos del sistema ---
    private void refreshFileTable() {
        fileTableModel.setRowCount(0);
        collectFiles(fsManager.getRoot());
    }
 
    private void collectFiles(Directory dir) {
        MyList<models.File> files = dir.getFiles();
        for (int i = 0; i < files.getSize(); i++) {
            models.File f = files.get(i);
            Color color;
            try { color = Color.decode(f.getColorHex()); }
            catch (Exception e) { color = C_ACCENT; }
            fileTableModel.addRow(new Object[]{
                color,
                f.getName(),
                f.getSize(),
                f.getStartBlockId(),
                f.getOwner()
            });
        }
        MyList<Directory> subs = dir.getSubDirectories();
        for (int i = 0; i < subs.getSize(); i++) {
            collectFiles(subs.get(i));
        }
    }
 
    // --- Actualiza la tabla del journal ---
    private void refreshJournalTable() {
        journalTableModel.setRowCount(0);
        MyList<JournalEntry> entries = journalManager.getEntries();
        for (int i = 0; i < entries.getSize(); i++) {
            JournalEntry e = entries.get(i);
            journalTableModel.addRow(new Object[]{
                e.getTransactionId(),
                e.getOperation(),
                e.getTargetName(),
                e.getStartBlock(),
                e.getStatus()
            });
        }
        // Scroll al final
        if (journalTableModel.getRowCount() > 0) {
            int last = journalTableModel.getRowCount() - 1;
            journalTable.scrollRectToVisible(journalTable.getCellRect(last, 0, true));
        }
    }
 
    // --- Actualiza el inspector al seleccionar un nodo del árbol ---
    private void updateInspector(String nodeName) {
        String cleanName = nodeName.replace("📁 ", "").replace("📄 ", "").trim();
        models.File found = findFileInTree(fsManager.getRoot(), cleanName);
        if (found != null) {
            lblInspectorName.setText(found.getName());
            lblInspectorSize.setText(found.getSize() + " bloques");
            lblInspectorOwner.setText(found.getOwner());
            lblInspectorBlock.setText(String.valueOf(found.getStartBlockId()));
        } else {
            lblInspectorName.setText(cleanName);
            lblInspectorSize.setText("directorio");
            lblInspectorOwner.setText("—");
            lblInspectorBlock.setText("—");
        }
    }
 
    private models.File findFileInTree(Directory dir, String name) {
        MyList<models.File> files = dir.getFiles();
        for (int i = 0; i < files.getSize(); i++) {
            if (files.get(i).getName().equals(name)) return files.get(i);
        }
        MyList<Directory> subs = dir.getSubDirectories();
        for (int i = 0; i < subs.getSize(); i++) {
            models.File f = findFileInTree(subs.get(i), name);
            if (f != null) return f;
        }
        return null;
    }
 
    // ================================================================
    //  Utilidades de UI
    // ================================================================
 
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }
 
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
 
    private void updateButtonStates() {
        btnCreate.setEnabled(adminMode);
        btnCreateDir.setEnabled(adminMode);
        btnDelete.setEnabled(adminMode);
        btnCrash.setEnabled(adminMode);
        btnRecover.setEnabled(adminMode);
    }
 
    // ================================================================
    //  Helpers de estilo
    // ================================================================
 
    private void applyGlobalTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("Panel.background",          C_BG);
        UIManager.put("ScrollPane.background",     C_SURFACE);
        UIManager.put("Viewport.background",       C_SURFACE);
        UIManager.put("Table.background",          C_SURFACE);
        UIManager.put("Table.foreground",          C_TEXT);
        UIManager.put("Table.gridColor",           C_BORDER);
        UIManager.put("Table.selectionBackground", new Color(79, 142, 247, 80));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background",    C_PANEL);
        UIManager.put("TableHeader.foreground",    C_MUTED);
        UIManager.put("Tree.background",           C_SURFACE);
        UIManager.put("Tree.foreground",           C_TEXT);
        UIManager.put("OptionPane.background",     C_PANEL);
        UIManager.put("OptionPane.messageForeground", C_TEXT);
        UIManager.put("TextField.background",      C_SURFACE);
        UIManager.put("TextField.foreground",      C_TEXT);
        UIManager.put("TextField.caretForeground", C_ACCENT);
        UIManager.put("ComboBox.background",       C_SURFACE);
        UIManager.put("ComboBox.foreground",       C_TEXT);
        UIManager.put("Button.background",         C_SURFACE);
        UIManager.put("Button.foreground",          C_TEXT);
        UIManager.put("Button.border",              new EmptyBorder(4, 10, 4, 10));
        UIManager.put("Button.rollover",            false);
        UIManager.put("ToolTip.background",         new Color(15, 20, 30));
        UIManager.put("ToolTip.foreground",         new Color(200, 210, 230));
    }
 
    private JPanel darkPanel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(C_BG);
        return p;
    }
 
    private JButton button(String text, Color color) {
        Color bgNormal = blend(color, C_PANEL, 0.12f);
        Color bgHover  = blend(color, C_PANEL, 0.25f);
        Color border   = blend(color, C_PANEL, 0.40f);
 
        JButton btn = new JButton(text);
        btn.setBackground(bgNormal);
        btn.setForeground(color);
        btn.setFont(F_TITLE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bgHover); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bgNormal); }
        });
        return btn;
    }
    
    // --- Mezcla dos colores según un factor (0=solo bg, 1=solo fg) ---
    private static Color blend(Color fg, Color bg, float factor) {
        int r = (int)(bg.getRed()   + (fg.getRed()   - bg.getRed())   * factor);
        int g = (int)(bg.getGreen() + (fg.getGreen() - bg.getGreen()) * factor);
        int b = (int)(bg.getBlue()  + (fg.getBlue()  - bg.getBlue())  * factor);
        return new Color(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, b))
        );
    }
    
    private JComboBox<String> combo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(C_SURFACE);
        cb.setForeground(C_TEXT);
        cb.setFont(F_MONO);
        cb.setBorder(BorderFactory.createLineBorder(C_BORDER));
        return cb;
    }
 
    private JLabel label(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(color);
        lbl.setFont(F_TITLE);
        return lbl;
    }
 
    private JLabel inspectorLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(C_ACCENT);
        lbl.setFont(F_MONO);
        return lbl;
    }
 
    private JPanel inspectorRow(String labelText, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(C_SURFACE);
        row.setBorder(new EmptyBorder(2, 6, 2, 6));
        JLabel key = new JLabel(labelText);
        key.setForeground(C_MUTED);
        key.setFont(F_TITLE);
        key.setPreferredSize(new Dimension(80, 16));
        row.add(key, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return row;
    }
 
    private JSeparator separator() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(C_BORDER);
        return sep;
    }
 
    private JScrollPane scrollPane(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(C_SURFACE);
        sp.getViewport().setBackground(C_SURFACE);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setBackground(C_SURFACE);
        return sp;
    }
 
    private Border titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_BORDER, 1), " " + title + " ");
        tb.setTitleColor(C_MUTED);
        tb.setTitleFont(F_TITLE);
        return BorderFactory.createCompoundBorder(tb, new EmptyBorder(4, 4, 4, 4));
    }
 
    private void styleTable(JTable table) {
        table.setBackground(C_SURFACE);
        table.setForeground(C_TEXT);
        table.setFont(F_MONO);
        table.setGridColor(C_BORDER);
        table.setRowHeight(24);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(C_PANEL);
        table.getTableHeader().setForeground(C_MUTED);
        table.getTableHeader().setFont(F_TITLE);
        table.setSelectionBackground(new Color(79, 142, 247, 60));
        table.setSelectionForeground(Color.WHITE);
    }
 
    private void styleTextField(JTextField tf) {
        tf.setBackground(C_SURFACE);
        tf.setForeground(C_TEXT);
        tf.setFont(F_MONO);
        tf.setCaretColor(C_ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                new EmptyBorder(2, 6, 2, 6)));
    }
 
    private void styleTreeIcons() {
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(C_SURFACE);
        renderer.setBackgroundSelectionColor(new Color(79, 142, 247, 60));
        renderer.setTextNonSelectionColor(C_TEXT);
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setBorderSelectionColor(C_ACCENT);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        dirTree.setCellRenderer(renderer);
    }
 
    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 6));
        p.setBackground(C_PANEL);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        return p;
    }
 
    private JTextField formField(JPanel form, String labelText) {
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(C_TEXT);
        lbl.setFont(F_MONO);
        JTextField tf = new JTextField(14);
        styleTextField(tf);
        form.add(lbl);
        form.add(tf);
        return tf;
    }
 
    // ================================================================
    //  Entry point
    // ================================================================
    
     // --- Recorre todos los componentes y elimina su tooltip ---
    private void disableAllTooltips(java.awt.Container container) {
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setToolTipText(null);
            }
            if (c instanceof java.awt.Container) {
                disableAllTooltips((java.awt.Container) c);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
    
    // ================================================================
    //  WrapLayout: FlowLayout que baja a la siguiente fila en lugar
    //  de recortar componentes cuando no hay espacio horizontal
    // ================================================================
    static class WrapLayout extends java.awt.FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
 
        @Override
        public Dimension preferredLayoutSize(java.awt.Container target) {
            return layoutSize(target, true);
        }
        @Override
        public Dimension minimumLayoutSize(java.awt.Container target) {
            return layoutSize(target, false);
        }
 
        private Dimension layoutSize(java.awt.Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right;
                int width = 0, height = 0, rowWidth = 0, rowHeight = 0;
                int count = target.getComponentCount();
                for (int i = 0; i < count; i++) {
                    java.awt.Component c = target.getComponent(i);
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        width = Math.max(width, rowWidth);
                        height += rowHeight + vgap;
                        rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth  += d.width + hgap;
                    rowHeight  = Math.max(rowHeight, d.height);
                }
                width  = Math.max(width, rowWidth);
                height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return new Dimension(width, height);
            }
        }
    }
}
