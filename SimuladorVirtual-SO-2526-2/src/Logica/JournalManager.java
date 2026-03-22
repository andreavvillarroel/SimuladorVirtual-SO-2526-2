/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Logica;

import models.JournalEntry;
import dataStructures.MyList;
 
import java.util.Observable;

/**
 *
 * @author Francisco
 */
// --- Registro transaccional: guarda operaciones antes de ejecutarlas y permite recuperación ---
public class JournalManager extends Observable {
 
    // --- Historial completo de transacciones ---
    private final MyList<JournalEntry> entries;
 
    // --- Contador autoincremental para IDs de transacción ---
    private int nextId;
 
    // --- Referencia al DiskManager para deshacer bloques durante la recuperación ---
    private DiskManager diskManager;
 
 
    // --- Inicializa el journal vacío ---
    public JournalManager() {
        this.entries = new MyList<>();
        this.nextId  = 1;
    }
 
    // --- Inyecta el DiskManager después de construir (evita dependencia circular) ---
    public void setDiskManager(DiskManager diskManager) {
        this.diskManager = diskManager;
    }
 
 
    // --- Registra una operación como PENDING antes de ejecutarla ---
    public int logOperation(String operation, String targetName, int startBlock, int size) {
        int txId = nextId++;
        entries.add(new JournalEntry(txId, operation, targetName, startBlock, size));
 
        setChanged();
        notifyObservers("JOURNAL_UPDATED");
        return txId;
    }
 
    // --- Marca una transacción como completada con éxito ---
    public void commit(int transactionId) {
        JournalEntry entry = findEntry(transactionId);
        if (entry != null) {
            entry.setStatus("COMMITTED");
            setChanged();
            notifyObservers("JOURNAL_UPDATED");
        }
    }
 
    // --- Marca una transacción como fallida ---
    public void markFailed(int transactionId) {
        JournalEntry entry = findEntry(transactionId);
        if (entry != null) {
            entry.setStatus("FAILED");
            setChanged();
            notifyObservers("JOURNAL_UPDATED");
        }
    }
 
 
    // --- Simula un crash corrompiendo las últimas N entradas COMMITTED a PENDING ---
    public void simulateCrash(int count) {
        int corrupted = 0;
        for (int i = entries.getSize() - 1; i >= 0 && corrupted < count; i--) {
            JournalEntry entry = entries.get(i);
            if (entry.getStatus().equals("COMMITTED")) {
                entry.setStatus("PENDING");
                corrupted++;
            }
        }
        setChanged();
        notifyObservers("CRASH_SIMULATED");
    }
 
    // --- Recupera el sistema deshaciendo todas las operaciones PENDING abiertas ---
    public int recoverSystem() {
        int undoneCount = 0;
 
        for (int i = 0; i < entries.getSize(); i++) {
            JournalEntry entry = entries.get(i);
 
            if (entry.getStatus().equals("PENDING")) {
                // CREATE pendiente: liberar bloques reservados que nunca se confirmaron
                if (entry.getOperation().equals("CREATE") && diskManager != null
                        && entry.getStartBlock() != -1) {
                    diskManager.freeBlocks(entry.getStartBlock());
                }
                // DELETE y UPDATE pendientes: marcar como FAILED sin acción adicional
 
                entry.setStatus("FAILED");
                undoneCount++;
            }
        }
 
        setChanged();
        notifyObservers("RECOVERY_COMPLETE:" + undoneCount);
        return undoneCount;
    }
 
 
    // --- Retorna todas las entradas del journal ---
    public MyList<JournalEntry> getEntries() {
        return entries;
    }
 
    // --- Retorna solo las entradas con el estado indicado ---
    public MyList<JournalEntry> getEntriesByStatus(String status) {
        MyList<JournalEntry> filtered = new MyList<>();
        for (int i = 0; i < entries.getSize(); i++) {
            if (entries.get(i).getStatus().equals(status)) {
                filtered.add(entries.get(i));
            }
        }
        return filtered;
    }
 
    // --- Cuenta cuántas transacciones están en estado PENDING ---
    public int countPending() {
        int count = 0;
        for (int i = 0; i < entries.getSize(); i++) {
            if (entries.get(i).getStatus().equals("PENDING")) count++;
        }
        return count;
    }
 
    // --- Limpia todo el historial del journal ---
    public void clearJournal() {
        while (!entries.isEmpty()) entries.remove(0);
        nextId = 1;
        setChanged();
        notifyObservers("JOURNAL_UPDATED");
    }
 
 
    // --- Busca una entrada en el historial por su ID de transacción ---
    private JournalEntry findEntry(int transactionId) {
        for (int i = 0; i < entries.getSize(); i++) {
            if (entries.get(i).getTransactionId() == transactionId) {
                return entries.get(i);
            }
        }
        return null;
    }
}
