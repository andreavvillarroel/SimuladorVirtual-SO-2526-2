package Test;

import Logica.*;
import models.*;
import dataStructures.*;
import persistence.JsonSaver;

public class IntegratedTest {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO SIMULACIÓN INTEGRAL DEL SISTEMA ===\n");

        // 1. Inicialización de subsistemas
        DiskManager disk = new DiskManager(); // 64 bloques
        LockManager locks = new LockManager();
        JournalManager journal = new JournalManager();
        journal.setDiskManager(disk);
        
        FileSystemManager fs = new FileSystemManager(disk, locks, journal);

        // 2. Prueba de Creación con Journaling y Disco
        System.out.println("--- 1. Creando archivo 'tesis.pdf' (5 bloques) ---");
        boolean created = fs.createFile("/", "tesis.pdf", 5, "estudiante", "#FF5733");
        
        if (created) {
            System.out.println("Archivo creado en la jerarquía.");
            System.out.println("Bloques libres restantes: " + disk.countFreeBlocks());
            System.out.println("Estado del Journal: " + journal.getEntries().get(0).getStatus());
        }

        // 3. Prueba de Concurrencia (Locks)
        System.out.println("\n--- 2. Probando Bloqueos (Locks) ---");
        locks.acquireLock("tesis.pdf", "Proceso_01");
        System.out.println("¿Está bloqueado 'tesis.pdf'?: " + locks.isLocked("tesis.pdf"));
        
        System.out.println("Intentando borrar archivo bloqueado...");
        boolean deleted = fs.deleteFile("/", "tesis.pdf", "root");
        if (!deleted) {
            System.out.println("✅ Correcto: El sistema impidió borrar un archivo bloqueado.");
        }

        // 4. Prueba de Fallo y Recuperación (Journaling)
        System.out.println("\n--- 3. Simulando Fallo (Crash) y Recuperación ---");
        // Forzamos una entrada PENDING para simular que el sistema se apagó antes del commit
        journal.logOperation("CREATE", "virus.exe", 10, 2); 
        System.out.println("Transacciones pendientes antes de recuperación: " + journal.countPending());
        
        int recuperados = journal.recoverSystem();
        System.out.println("Transacciones deshechas (Undo): " + recuperados);
        System.out.println("Transacciones pendientes ahora: " + journal.countPending());

        // 5. Prueba de Persistencia (Tu tarea)
        System.out.println("\n--- 4. Guardando estado en JSON ---");
        JsonSaver.save(fs.getRoot(), "estado_sistema.json");
        System.out.println("Archivo 'estado_sistema.json' generado.");

        System.out.println("\n=== SIMULACIÓN COMPLETADA CON ÉXITO ===");
    }
}