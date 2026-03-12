/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package algoritmos;

import models.Process;
import dataStructures.MyQueue;
import dataStructures.MyList;

import java.util.Observable;


/**
 *
 * @author Francisco
 */
// --- Planificador de disco: ejecuta FIFO, SSTF, SCAN o C-SCAN en un hilo aparte ---
public class DiskScheduler extends Observable implements Runnable {

    // --- Nombres de los algoritmos disponibles ---
    public static final String FIFO   = "FIFO";
    public static final String SSTF   = "SSTF";
    public static final String SCAN   = "SCAN";
    public static final String C_SCAN = "C-SCAN";

    // --- Cola de procesos esperando acceso al disco ---
    private final MyQueue<Process> colaSolicitudes;

    // --- Algoritmo activo en este momento ---
    private String algoritmo;

    // --- Posición actual del cabezal (número de bloque) ---
    private int cabezal;

    // --- Dirección de movimiento para SCAN y C-SCAN ---
    private boolean direccionAscendente;

    // --- Controla si el hilo sigue corriendo ---
    private volatile boolean activo;

    // --- Pausa entre movimientos del cabezal en milisegundos ---
    private int velocidadMs;

    // --- Cuando es false, el cabezal salta directo al destino (modo terminal) ---
    private boolean animacionActiva;


    // --- Constructor: cabezal arranca en el bloque 0 ---
    public DiskScheduler(String algoritmo) {
        this.colaSolicitudes     = new MyQueue<>();
        this.algoritmo           = algoritmo;
        this.cabezal             = 0;
        this.direccionAscendente = true;
        this.activo              = true;
        this.velocidadMs         = 500;
        this.animacionActiva     = true;
    }


    // --- Agrega un proceso a la cola de solicitudes ---
    public synchronized void agregarSolicitud(Process proceso) {
        proceso.setState("READY");
        colaSolicitudes.enqueue(proceso);
    }

    // --- Cambia el algoritmo sin detener el hilo ---
    public synchronized void cambiarAlgoritmo(String nuevoAlgoritmo) {
        this.algoritmo = nuevoAlgoritmo;
    }

    // --- Detiene el hilo de forma segura ---
    public void detener() {
        this.activo = false;
    }

    // --- Ajusta la velocidad de movimiento del cabezal ---
    public void setVelocidad(int ms) {
        this.velocidadMs = ms;
    }

    // --- Posiciona el cabezal en un bloque inicial (se usa al cargar un JSON) ---
    public void setCabezal(int posicion) {
        this.cabezal = posicion;
    }

    // --- Desactiva la animación paso a paso (útil en terminal, sin GUI) ---
    public void setModoAnimacion(boolean activa) {
        this.animacionActiva = activa;
    }

    // --- Procesa exactamente una solicitud de la cola y retorna ---
    public void ejecutarUno() {
        if (colaSolicitudes.isEmpty()) return;
        switch (algoritmo) {
            case FIFO   -> ejecutarFIFO();
        }
    }
    
    // --- Bucle principal del hilo: despacha al algoritmo activo ---
    @Override
    public void run() {
        while (activo) {
            if (!colaSolicitudes.isEmpty()) {
                switch (algoritmo) {
                    case FIFO   -> ejecutarFIFO();
                }
            } else {
                dormirMs(100); // espera corta para no quemar CPU
            }
        }
    }


    // --- FIFO: atiende en orden de llegada, sin importar la distancia ---
    private void ejecutarFIFO() {
        Process proceso = colaSolicitudes.dequeue();
        if (proceso == null) return;

        int destino = proceso.getTargetBlock();
        moverCabezal(destino);
        proceso.setState("RUNNING");
        dormirMs(velocidadMs);
        proceso.setState("BLOCKED");

        setChanged();
        notifyObservers("PROCESO_ATENDIDO:" + destino);
    }
    
    // --- Atiende el proceso en la posición dada y notifica ---
    private void atenderProceso(MyList<Process> lista, int indice) {
        Process p = lista.get(indice);
        lista.remove(indice);
        moverCabezal(p.getTargetBlock());
        p.setState("RUNNING");
        dormirMs(velocidadMs);
        p.setState("BLOCKED");
        setChanged();
        notifyObservers("PROCESO_ATENDIDO:" + p.getTargetBlock());
    }


    // --- Mueve el cabezal hacia el destino; si la animación está off, salta directo ---
    private void moverCabezal(int destino) {
        if (!animacionActiva) {
            System.out.printf("    cabezal: %d → %d  (dist: %d)%n",
                    cabezal, destino, Math.abs(destino - cabezal));
            cabezal = destino;
            setChanged();
            notifyObservers("CABEZAL:" + cabezal);
            return;
        }
        int paso = (destino > cabezal) ? 1 : -1;
        while (cabezal != destino) {
            cabezal += paso;
            setChanged();
            notifyObservers("CABEZAL:" + cabezal);
            dormirMs(30);
        }
    }

    // --- Vacía la cola a una MyList para poder recorrerla ---
    private MyList<Process> vaciarColaEnLista() {
        MyList<Process> lista = new MyList<>();
        while (!colaSolicitudes.isEmpty()) {
            lista.add(colaSolicitudes.dequeue());
        }
        return lista;
    }

    // --- Devuelve los procesos de la lista a la cola en el mismo orden ---
    private void recargarColaDesodeLista(MyList<Process> lista) {
        for (int i = 0; i < lista.getSize(); i++) {
            colaSolicitudes.enqueue(lista.get(i));
        }
    }

    // --- Ordena la lista de procesos por bloque objetivo (burbuja) ---
    private void ordenarPorBloque(MyList<Process> lista) {
        int n = lista.getSize();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (lista.get(j).getTargetBlock() > lista.get(j + 1).getTargetBlock()) {
                    // Reconstruir la lista intercambiando j y j+1
                    Process a = lista.get(j);
                    Process b = lista.get(j + 1);
                    MyList<Process> aux = new MyList<>();
                    for (int k = 0; k < lista.getSize(); k++) aux.add(lista.get(k));
                    while (!lista.isEmpty()) lista.remove(0);
                    for (int k = 0; k < j; k++)               lista.add(aux.get(k));
                    lista.add(b);
                    lista.add(a);
                    for (int k = j + 2; k < aux.getSize(); k++) lista.add(aux.get(k));
                    break;
                }
            }
        }
    }

    // --- Pausa el hilo la cantidad de milisegundos indicada ---
    private void dormirMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // --- Retorna la posición actual del cabezal ---
    public int getCabezal() { return cabezal; }

    // --- Retorna el nombre del algoritmo activo ---
    public String getAlgoritmo() { return algoritmo; }

    // --- Retorna cuántos procesos esperan en la cola ---
    public int getSolicitudesPendientes() { return colaSolicitudes.getSize(); }
}
