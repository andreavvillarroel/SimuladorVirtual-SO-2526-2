/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author Andrea
 */
public class File {
    private String name;
    private int size; // Tamaño en bloques
    private String owner;
    private int startBlockId;
    private String colorHex;

    public File(String name, int size, String owner, int startBlockId, String colorHex) {
        this.name = name;
        this.size = size;
        this.owner = owner;
        this.startBlockId = startBlockId;
        this.colorHex = colorHex;
    }

    // Getters
    public String getName() { return name; }
    public int getSize() { return size; }
    public String getOwner() { return owner; }
    public int getStartBlockId() { return startBlockId; }
    public String getColorHex() { return colorHex; }
    
    @Override
    public String toString() { return name; } // Para que el JTree muestre el nombre
}