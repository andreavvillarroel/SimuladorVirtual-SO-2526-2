/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Logica;

import dataStructures.MyList;
import java.util.Observable;
/**
 *
 * @author Francisco
 */
// --- Controla el acceso exclusivo a archivos mediante semáforos binarios simulados ---
public class LockManager extends Observable {
 
    // --- Registro interno de un bloqueo activo: qué archivo y quién lo tiene ---
    private static class LockRecord {
        final String fileName;
        String       ownerPid;
 
        LockRecord(String fileName, String ownerPid) {
            this.fileName = fileName;
            this.ownerPid = ownerPid;
        }
    }
 
    // --- Lista de todos los bloqueos activos en el sistema ---
    private final MyList<LockRecord> activeLocks;
 
 
    // --- Inicializa el gestor sin bloqueos activos ---
    public LockManager() {
        this.activeLocks = new MyList<>();
    }
 
 
    // --- Intenta adquirir un bloqueo exclusivo sobre el archivo (equivale a P / wait) ---
    public synchronized boolean acquireLock(String fileName, String ownerPid) {
        // Denegar si el archivo ya está bloqueado por otro proceso
        if (findRecord(fileName) != null) {
            return false;
        }
 
        activeLocks.add(new LockRecord(fileName, ownerPid));
 
        setChanged();
        notifyObservers("LOCK_UPDATED");
        return true;
    }
 
    // --- Libera el bloqueo sobre el archivo (equivale a V / signal) ---
    public synchronized boolean releaseLock(String fileName, String requester) {
        for (int i = 0; i < activeLocks.getSize(); i++) {
            LockRecord record = activeLocks.get(i);
            if (record.fileName.equals(fileName)) {
                // Solo el dueño del lock o root pueden liberarlo
                if (!record.ownerPid.equals(requester) && !requester.equals("root")) {
                    return false;
                }
                activeLocks.remove(i);
                setChanged();
                notifyObservers("LOCK_UPDATED");
                return true;
            }
        }
        return false;
    }
 
 
    // --- Retorna true si el archivo tiene un bloqueo activo ---
    public boolean isLocked(String fileName) {
        return findRecord(fileName) != null;
    }
 
    // --- Retorna el PID del proceso que tiene el lock, o null si está libre ---
    public String getOwner(String fileName) {
        LockRecord record = findRecord(fileName);
        return (record != null) ? record.ownerPid : null;
    }
 
    // --- Retorna todos los bloqueos activos en formato "archivo -> PID" ---
    public MyList<String> getLockStatus() {
        MyList<String> status = new MyList<>();
        for (int i = 0; i < activeLocks.getSize(); i++) {
            LockRecord r = activeLocks.get(i);
            status.add(r.fileName + " -> " + r.ownerPid);
        }
        return status;
    }
 
    // --- Libera todos los bloqueos de forma forzada (se usa al recuperar tras crash) ---
    public synchronized void releaseAll() {
        while (!activeLocks.isEmpty()) {
            activeLocks.remove(0);
        }
        setChanged();
        notifyObservers("LOCK_UPDATED");
    }
 
 
    // --- Busca un registro de bloqueo por nombre de archivo ---
    private LockRecord findRecord(String fileName) {
        for (int i = 0; i < activeLocks.getSize(); i++) {
            if (activeLocks.get(i).fileName.equals(fileName)) {
                return activeLocks.get(i);
            }
        }
        return null;
    }
}
