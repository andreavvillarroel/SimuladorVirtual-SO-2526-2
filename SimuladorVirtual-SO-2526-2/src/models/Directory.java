/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;
import dataStructures.MyList;

/**
 *
 * @author Andrea
 */
public class Directory {
    private String name;
    private String owner;
    private MyList<File> files;
    private MyList<Directory> subDirectories;

    public Directory(String name, String owner) {
        this.name = name;
        this.owner = owner;
        this.files = new MyList<>();
        this.subDirectories = new MyList<>();
    }

    public String getName() { return name; }
    public MyList<File> getFiles() { return files; }
    public MyList<Directory> getSubDirectories() { return subDirectories; }

    @Override
    public String toString() { return name; }
}