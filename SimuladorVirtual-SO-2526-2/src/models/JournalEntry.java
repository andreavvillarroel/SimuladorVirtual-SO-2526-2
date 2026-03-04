/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author Andrea
 */
public class JournalEntry {
    private int transactionId;
    private String operation; // "CREATE", "DELETE", "UPDATE"
    private String targetName; // Nombre del archivo o directorio afectado
    private int startBlock;    // El bloque inicial (importante para el UNDO)
    private int size;          // Tamaño en bloques
    private String status;     // "PENDING" o "COMMITTED"
    private long timestamp;    // Para mostrar en el log de la interfaz

    public JournalEntry(int transactionId, String operation, String targetName, int startBlock, int size) {
        this.transactionId = transactionId;
        this.operation = operation;
        this.targetName = targetName;
        this.startBlock = startBlock;
        this.size = size;
        this.status = "PENDING"; // Toda operación nace como PENDIENTE
        this.timestamp = System.currentTimeMillis();
    }

    // Getters y Setters
    public int getTransactionId() { return transactionId; }
    public String getOperation() { return operation; }
    public String getTargetName() { return targetName; }
    public int getStartBlock() { return startBlock; }
    public int getSize() { return size; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        // Este formato es el que se verá en la lista de Journal de la interfaz
        return "[" + transactionId + "] " + operation + " " + targetName + " - " + status;
    }
}
