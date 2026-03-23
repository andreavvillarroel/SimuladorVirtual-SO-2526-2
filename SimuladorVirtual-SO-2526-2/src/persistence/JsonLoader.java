/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package persistence;

import models.Process;
import dataStructures.MyList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
/**
 *
 * @author Francisco
 */
// --- Carga y parsea un archivo JSON de prueba sin librerías externas ---
public class JsonLoader {

    // --- Resultado de cargar un JSON: todos los datos del test ---
    public static class TestData {
        public String           testId;
        public int              initialHead;
        public MyList<Process>  procesos;    // solicitudes de disco
        public MyList<String[]> archivos;    // [pos, nombre, bloques]

        public TestData() {
            procesos = new MyList<>();
            archivos = new MyList<>();
        }
    }

    // --- Lee el archivo y retorna un TestData listo para usar ---
    public static TestData cargar(String rutaArchivo) throws IOException {
        String contenido = leerArchivo(rutaArchivo);
        return parsear(contenido);
    }

    // --- Lee el archivo línea por línea y lo une en un String ---
    private static String leerArchivo(String ruta) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea.trim());
            }
        }
        return sb.toString();
    }

    // --- Extrae los campos del JSON usando búsqueda de texto simple ---
    private static TestData parsear(String json) {
        TestData data = new TestData();

        // --- test_id ---
        data.testId = extraerString(json, "test_id");

        // --- initial_head ---
        data.initialHead = extraerInt(json, "initial_head");

        // --- requests: array de objetos {"pos": N, "op": "..."}  ---
        String bloqueRequests = extraerBloque(json, "requests");
        if (bloqueRequests != null) {
            // Separar cada objeto { ... } dentro del array
            MyList<String> objetos = separarObjetos(bloqueRequests);
            int pid = 1;
            for (int i = 0; i < objetos.getSize(); i++) {
                String obj = objetos.get(i);
                int    pos = extraerInt(obj, "pos");
                String op  = extraerString(obj, "op");
                data.procesos.add(new Process(pid++, op, pos));
            }
        }

        // --- system_files: objeto cuyas claves son posiciones ---
        String bloqueFiles = extraerBloque(json, "system_files");
        if (bloqueFiles != null) {
            // Cada entrada tiene forma  "POS": { "name": "...", "blocks": N }
            parsearSystemFiles(bloqueFiles, data);
        }

        return data;
    }

    // --- Parsea system_files extrayendo posición, nombre y bloques ---
    private static void parsearSystemFiles(String bloque, TestData data) {
        int i = 0;
        while (i < bloque.length()) {
            // Buscar la clave numérica (posición)
            int quoteStart = bloque.indexOf('"', i);
            if (quoteStart == -1) break;
            int quoteEnd = bloque.indexOf('"', quoteStart + 1);
            if (quoteEnd == -1) break;

            String clave = bloque.substring(quoteStart + 1, quoteEnd);

            // Verificar que la clave sea numérica (posición de bloque)
            if (!clave.matches("\\d+")) {
                i = quoteEnd + 1;
                continue;
            }

            // Buscar el objeto { } que sigue a la clave
            int objStart = bloque.indexOf('{', quoteEnd);
            if (objStart == -1) break;
            int objEnd = cerrarLlave(bloque, objStart);
            if (objEnd == -1) break;

            String obj    = bloque.substring(objStart, objEnd + 1);
            String nombre = extraerString(obj, "name");
            int    bloqs  = extraerInt(obj, "blocks");

            // Guardar como arreglo [pos, nombre, bloques]
            data.archivos.add(new String[]{clave, nombre, String.valueOf(bloqs)});

            i = objEnd + 1;
        }
    }

    // --- Extrae el valor String de una clave JSON (sin librerías) ---
    private static String extraerString(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return "";
        int colon = json.indexOf(':', idx);
        if (colon == -1) return "";
        int q1 = json.indexOf('"', colon + 1);
        if (q1 == -1) return "";
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 == -1) return "";
        return json.substring(q1 + 1, q2);
    }

    // --- Extrae el valor entero de una clave JSON ---
    private static int extraerInt(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return -1;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return -1;

        // Leer dígitos a partir del colon
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (!Character.isWhitespace(c) && num.length() > 0) {
                break;
            }
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : -1;
    }

    // --- Extrae el contenido entre [ ] o { } de una clave JSON ---
    private static String extraerBloque(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return null;

        // Encontrar el primer [ o {
        int inicio = -1;
        char apertura = ' ', cierre = ' ';
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') { inicio = i; apertura = '['; cierre = ']'; break; }
            if (c == '{') { inicio = i; apertura = '{'; cierre = '}'; break; }
        }
        if (inicio == -1) return null;

        // Contar niveles para encontrar el cierre correcto
        int nivel = 0;
        for (int i = inicio; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == apertura) nivel++;
            else if (c == cierre) {
                nivel--;
                if (nivel == 0) return json.substring(inicio + 1, i);
            }
        }
        return null;
    }

    // --- Separa objetos { } de primer nivel dentro de un String ---
    private static MyList<String> separarObjetos(String texto) {
        MyList<String> lista = new MyList<>();
        int i = 0;
        while (i < texto.length()) {
            int inicio = texto.indexOf('{', i);
            if (inicio == -1) break;
            int fin = cerrarLlave(texto, inicio);
            if (fin == -1) break;
            lista.add(texto.substring(inicio + 1, fin));
            i = fin + 1;
        }
        return lista;
    }

    // --- Encuentra la llave de cierre que corresponde al { en la posición dada ---
    private static int cerrarLlave(String texto, int inicio) {
        int nivel = 0;
        for (int i = inicio; i < texto.length(); i++) {
            if (texto.charAt(i) == '{') nivel++;
            else if (texto.charAt(i) == '}') {
                nivel--;
                if (nivel == 0) return i;
            }
        }
        return -1;
    }
}
