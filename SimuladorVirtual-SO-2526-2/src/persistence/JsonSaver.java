/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package persistence;
import models.Directory;
import models.File;
import dataStructures.MyList;
import java.io.FileWriter;
import java.io.IOException;


/**
 *
 * @author Andrea
 */

public class JsonSaver {

    /**
     * Guarda la estructura de directorios en un archivo de texto con formato JSON.
     * @param root El directorio raíz del sistema
     * @param path Ruta donde se guardará el archivo (ej: "system_save.json")
     */
    public static void save(Directory root, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"root\": ");
        sb.append(directoryToJson(root, 1));
        sb.append("\n}");

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(sb.toString());
            System.out.println("Sistema guardado exitosamente en: " + path);
        } catch (IOException e) {
            System.err.println("Error al guardar el sistema: " + e.getMessage());
        }
    }

    // Método recursivo para convertir directorios y su contenido a JSON
    private static String directoryToJson(Directory dir, int indentLevel) {
        String space = "  ".repeat(indentLevel);
        String subSpace = "  ".repeat(indentLevel + 1);
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");
        sb.append(subSpace).append("\"name\": \"").append(dir.getName()).append("\",\n");
        
        // Guardar Archivos
        sb.append(subSpace).append("\"files\": [\n");
        MyList<File> files = dir.getFiles();
        for (int i = 0; i < files.getSize(); i++) {
            File f = files.get(i);
            sb.append(subSpace).append("  { ");
            sb.append("\"name\": \"").append(f.getName()).append("\", ");
            sb.append("\"size\": ").append(f.getSize()).append(", ");
            sb.append("\"owner\": \"").append(f.getOwner()).append("\", ");
            sb.append("\"startBlock\": ").append(f.getStartBlockId()).append(", ");
            sb.append("\"color\": \"").append(f.getColorHex()).append("\"");
            sb.append(" }");
            if (i < files.getSize() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(subSpace).append("],\n");

        // Guardar Subdirectorios (Recursividad)
        sb.append(subSpace).append("\"subdirectories\": [\n");
        MyList<Directory> subs = dir.getSubDirectories();
        for (int i = 0; i < subs.getSize(); i++) {
            sb.append(subSpace).append("  ").append(directoryToJson(subs.get(i), indentLevel + 2));
            if (i < subs.getSize() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(subSpace).append("]\n");
        sb.append(space).append("}");

        return sb.toString();
    }
}