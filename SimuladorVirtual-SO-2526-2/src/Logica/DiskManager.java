/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Logica;

import models.Block;
import models.File;
import dataStructures.MyList;
 
import java.util.Observable;

/**
 *
 * @author Francisco
 */
// --- Gestiona el disco físico simulado como un arreglo de bloques ---
public class DiskManager extends Observable {
 
    // --- Número total de bloques en el disco simulado ---
    public static final int TOTAL_BLOCKS = 64;
 
    // --- Arreglo físico del disco: índice == ID del bloque ---
    private final Block[] disk;
 
 
    // --- Inicializa todos los bloques como libres ---
    public DiskManager() {
        disk = new Block[TOTAL_BLOCKS];
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            disk[i] = new Block(i);
        }
    }
 
 
    // --- Busca bloques libres y los encadena para almacenar el archivo ---
    public int allocateBlocks(File file) {
        int blocksNeeded = file.getSize();
 
        // Verificar espacio antes de modificar nada
        if (countFreeBlocks() < blocksNeeded) {
            return -1;
        }
 
        // Recolectar índices de bloques libres en una MyList
        MyList<Integer> freeBlocks = new MyList<>();
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            if (disk[i].isFree()) {
                freeBlocks.add(i);
                if (freeBlocks.getSize() == blocksNeeded) break;
            }
        }
 
        // Encadenar bloques: cada uno apunta al siguiente, el último a -1
        for (int i = 0; i < blocksNeeded; i++) {
            int   blockId = freeBlocks.get(i);
            Block block   = disk[blockId];
            block.setFree(false);
            block.setColorHex(file.getColorHex());
            block.setNextBlockId(i < blocksNeeded - 1 ? freeBlocks.get(i + 1) : -1);
        }
 
        setChanged();
        notifyObservers("DISK_UPDATED");
 
        return freeBlocks.get(0);
    }
 
    // --- Libera todos los bloques encadenados desde el bloque inicial ---
    public void freeBlocks(int startBlockId) {
        int currentId = startBlockId;
 
        while (currentId != -1) {
            Block block  = disk[currentId];
            int   nextId = block.getNextBlockId();
 
            block.setFree(true);
            block.setNextBlockId(-1);
            block.setColorHex("#FFFFFF");
 
            currentId = nextId;
        }
 
        setChanged();
        notifyObservers("DISK_UPDATED");
    }
 
 
    // --- Retorna el bloque en la posición indicada ---
    public Block getBlock(int id) {
        if (id < 0 || id >= TOTAL_BLOCKS) return null;
        return disk[id];
    }
 
    // --- Cuenta cuántos bloques están libres actualmente ---
    public int countFreeBlocks() {
        int count = 0;
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            if (disk[i].isFree()) count++;
        }
        return count;
    }
 
    // --- Retorna el arreglo completo de bloques ---
    public Block[] getDisk() {
        return disk;
    }
 
    // --- Recorre la cadena de bloques de un archivo y retorna sus IDs ---
    public MyList<Integer> getBlockChain(int startBlockId) {
        MyList<Integer> chain     = new MyList<>();
        int             currentId = startBlockId;
 
        while (currentId != -1) {
            chain.add(currentId);
            currentId = disk[currentId].getNextBlockId();
        }
        return chain;
    }
}
