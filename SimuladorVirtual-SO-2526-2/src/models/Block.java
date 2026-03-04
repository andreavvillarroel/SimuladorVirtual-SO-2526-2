/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author Andrea
 */
public class Block {
    private int id;
    private int nextBlockId; // -1 si es el último bloque del archivo
    private boolean isFree;
    private String colorHex; // Color del archivo al que pertenece
    private String content;  // Contenido simulado

    public Block(int id) {
        this.id = id;
        this.nextBlockId = -1;
        this.isFree = true;
        this.colorHex = "#FFFFFF"; // Blanco por defecto (libre)
        this.content = "";
    }

    // Getters y Setters
    public int getId() { return id; }
    public int getNextBlockId() { return nextBlockId; }
    public void setNextBlockId(int nextBlockId) { this.nextBlockId = nextBlockId; }
    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}