/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author Andrea
 */
public class Process {
    private int pid;
    private String operation; // READ, WRITE, CREATE, DELETE, UPDATE
    private String state;     // READY, RUNNING, BLOCKED
    private int targetBlock;  // Bloque al que intenta acceder (para el planificador)

    public Process(int pid, String operation, int targetBlock) {
        this.pid = pid;
        this.operation = operation;
        this.targetBlock = targetBlock;
        this.state = "READY";
    }

    // Getters  necesarios para el Scheduler
    public int    getPid()          { return pid; }
    public String getOperation()    { return operation; }
    public int getTargetBlock() { return targetBlock; }
    public String getState() { return state; }
    
    
    //Setters necesarios para el Scheduler
    public void setState(String state) { this.state = state; }
}