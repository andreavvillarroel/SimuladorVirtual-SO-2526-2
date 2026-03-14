/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Logica;

import models.File;
import models.Directory;
import dataStructures.MyList;
 
import java.util.Observable;

/**
 *
 * @author Francisco
 */

// --- Núcleo del sistema de archivos: crea, borra archivos y controla permisos ---
public class FileSystemManager extends Observable {
 
    // --- Raíz del árbol de directorios ---
    private final Directory root;
 
    // --- Subsistemas conectados ---
    private final DiskManager    diskManager;
    private final LockManager    lockManager;
    private final JournalManager journalManager;
 
 
    // --- Inicializa el sistema de archivos con un directorio raíz vacío ---
    public FileSystemManager(DiskManager diskManager,
                             LockManager lockManager,
                             JournalManager journalManager) {
        this.root           = new Directory("/", "root");
        this.diskManager    = diskManager;
        this.lockManager    = lockManager;
        this.journalManager = journalManager;
    }
 
    // --- Crea un subdirectorio dentro del directorio padre indicado ---
    public void createDirectory(String parentName, String dirName, String owner) {
        Directory parent = findDirectory(root, parentName);
        if (parent == null) return;
 
        // Evitar nombres duplicados en el mismo nivel
        if (parent.getSubDirectories().findByName(dirName) != null) return;
 
        parent.getSubDirectories().add(new Directory(dirName, owner));
 
        setChanged();
        notifyObservers("FS_UPDATED");
    }
 
    // --- Crea un archivo usando el protocolo WAL (write-ahead logging) ---
    public boolean createFile(String dirName, String fileName,
                              int sizeBlocks, String owner, String colorHex) {
        Directory target = findDirectory(root, dirName);
        if (target == null) return false;
 
        // Evitar nombres duplicados en el mismo directorio
        if (target.getFiles().findByName(fileName) != null) return false;
 
        // 1. Registrar la intención antes de tocar el disco
        int txId = journalManager.logOperation("CREATE", fileName, -1, sizeBlocks);
 
        // 2. Pedir bloques al DiskManager (objeto temporal para pasar tamaño y color)
        File temp       = new File(fileName, sizeBlocks, owner, -1, colorHex);
        int  firstBlock = diskManager.allocateBlocks(temp);
 
        if (firstBlock == -1) {
            // Sin espacio disponible: abortar y marcar en el journal
            journalManager.markFailed(txId);
            return false;
        }
 
        // 3. Crear el archivo definitivo y agregarlo al directorio
        target.getFiles().add(new File(fileName, sizeBlocks, owner, firstBlock, colorHex));
 
        // 4. Confirmar la operación
        journalManager.commit(txId);
 
        setChanged();
        notifyObservers("FS_UPDATED");
        return true;
    }
 
    // --- Elimina un archivo si el solicitante tiene permiso y no está bloqueado ---
    public boolean deleteFile(String dirName, String fileName, String requester) {
        Directory directory = findDirectory(root, dirName);
        if (directory == null) return false;
 
        File file = (File) directory.getFiles().findByName(fileName);
        if (file == null) return false;
 
        // Solo el dueño o root pueden eliminar
        if (!file.getOwner().equals(requester) && !requester.equals("root")) {
            return false;
        }
 
        // No eliminar si otro proceso tiene el archivo bloqueado
        if (lockManager.isLocked(fileName)) {
            return false;
        }
 
        // 1. Registrar la intención de borrado en el journal
        int txId = journalManager.logOperation(
                "DELETE", fileName, file.getStartBlockId(), file.getSize());
 
        // 2. Liberar bloques en disco
        diskManager.freeBlocks(file.getStartBlockId());
 
        // 3. Remover el archivo de la lista del directorio
        MyList<File> fileList = directory.getFiles();
        for (int i = 0; i < fileList.getSize(); i++) {
            if (fileList.get(i).getName().equals(fileName)) {
                fileList.remove(i);
                break;
            }
        }
 
        // 4. Confirmar la operación
        journalManager.commit(txId);
 
        setChanged();
        notifyObservers("FS_UPDATED");
        return true;
    }
 
    // --- Retorna el directorio raíz ---
    public Directory getRoot() {
        return root;
    }
 
    // --- Retorna los archivos de un directorio dado su nombre ---
    public MyList<File> listFiles(String dirName) {
        Directory dir = findDirectory(root, dirName);
        return (dir != null) ? dir.getFiles() : null;
    }
 
    // --- Busca un directorio en el árbol por nombre usando recorrido DFS ---
    private Directory findDirectory(Directory current, String name) {
        if (current.getName().equals(name)) return current;
 
        MyList<Directory> subDirs = current.getSubDirectories();
        for (int i = 0; i < subDirs.getSize(); i++) {
            Directory result = findDirectory(subDirs.get(i), name);
            if (result != null) return result;
        }
        return null;
    }
}
