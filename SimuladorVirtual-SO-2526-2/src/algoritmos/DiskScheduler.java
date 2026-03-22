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
    private final MyQueue<Process> requestQueue;

    // --- Algoritmo activo en este momento ---
    private String algorithm;

    // --- Posición actual del cabezal (número de bloque) ---
    private int headPosition;

    // --- Dirección de movimiento para SCAN y C-SCAN ---
    private boolean movingUp;

    // --- Controla si el hilo sigue corriendo ---
    private volatile boolean running;

    // --- Pausa entre movimientos del cabezal en milisegundos ---
    private int speedMs;

    // --- Cuando es false, el cabezal salta directo al destino (modo terminal) ---
    private boolean animationEnabled;


    // --- Constructor: cabezal arranca en el bloque 0 ---
    public DiskScheduler(String algorithm) {
        this.requestQueue     = new MyQueue<>();
        this.algorithm        = algorithm;
        this.headPosition     = 0;
        this.movingUp         = true;
        this.running          = true;
        this.speedMs          = 500;
        this.animationEnabled = true;
    }


     // --- Agrega un proceso a la cola de solicitudes ---
    public synchronized void addRequest(Process process) {
        process.setState("READY");
        requestQueue.enqueue(process);
    }

    // --- Cambia el algoritmo activo sin detener el hilo ---
    public synchronized void setAlgorithm(String newAlgorithm) {
        this.algorithm = newAlgorithm;
    }

    // --- Detiene el hilo de forma segura al terminar la solicitud actual ---
    public void stop() {
        this.running = false;
    }

    // --- Ajusta la velocidad de movimiento del cabezal en milisegundos ---
    public void setSpeed(int ms) {
        this.speedMs = ms;
    }

    // --- Posiciona el cabezal en un bloque inicial (se usa al cargar un JSON) ---
    public void setHeadPosition(int position) {
        this.headPosition = position;
    }

    // --- Desactiva la animación paso a paso (útil en modo terminal) ---
    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    // --- Procesa exactamente una solicitud de la cola y retorna ---
    public void executeOne() {
        if (requestQueue.isEmpty()) return;
        switch (algorithm) {
                    case FIFO   -> runFIFO();
                    case SSTF   -> runSSTF();
                    case SCAN   -> runSCAN();
                    case C_SCAN -> runCSCAN();
                }
            } 
        
    
    // --- Bucle principal del hilo: despacha al algoritmo activo ---
    @Override
    public void run() {
        while (running) {
            if (!requestQueue.isEmpty()) {
                switch (algorithm) {
                    case FIFO   -> runFIFO();
                    case SSTF   -> runSSTF();
                    case SCAN   -> runSCAN();
                    case C_SCAN -> runCSCAN();
                    
                }
            } else {
                sleep(100); // espera corta para no quemar CPU
            }
        }
    }


    // --- FIFO: atiende en orden de llegada sin importar la distancia ---
    private void runFIFO() {
        Process process = requestQueue.dequeue();
        if (process == null) return;

        int target = process.getTargetBlock();
        moveHead(target);
        process.setState("RUNNING");
        sleep(speedMs);
        process.setState("BLOCKED");

        setChanged();
        notifyObservers("REQUEST_SERVED:" + target);
    }
    
    // --- SSTF: atiende primero el bloque más cercano al cabezal ---
    private void runSSTF() {
        if (requestQueue.isEmpty()) return;

        // Pasar la cola a una lista para poder recorrerla sin destruirla
        MyList<Process> temp    = drainQueueToList();
        int             bestIdx = 0;
        int             minDist = Integer.MAX_VALUE;

        for (int i = 0; i < temp.getSize(); i++) {
            int distance = Math.abs(temp.get(i).getTargetBlock() - headPosition);
            if (distance < minDist) {
                minDist = distance;
                bestIdx = i;
            }
        }

        Process chosen = temp.get(bestIdx);
        temp.remove(bestIdx);
        reloadQueueFromList(temp);

        moveHead(chosen.getTargetBlock());
        chosen.setState("RUNNING");
        sleep(speedMs);
        chosen.setState("BLOCKED");

        setChanged();
        notifyObservers("REQUEST_SERVED:" + chosen.getTargetBlock());
    }
    
    // --- SCAN: barre en una dirección e invierte al llegar al extremo ---
    private void runSCAN() {
        if (requestQueue.isEmpty()) return;

        MyList<Process> temp = drainQueueToList();
        sortByBlock(temp);

        if (movingUp) {
            // Buscar la primera solicitud en dirección ascendente
            for (int i = 0; i < temp.getSize(); i++) {
                if (temp.get(i).getTargetBlock() >= headPosition) {
                    serveFromList(temp, i);
                    reloadQueueFromList(temp);
                    return;
                }
            }
            // Sin solicitudes adelante: invertir dirección
            movingUp = false;

        } else {
            // Buscar la primera solicitud en dirección descendente
            for (int i = temp.getSize() - 1; i >= 0; i--) {
                if (temp.get(i).getTargetBlock() <= headPosition) {
                    serveFromList(temp, i);
                    reloadQueueFromList(temp);
                    return;
                }
            }
            // Sin solicitudes atrás: invertir dirección
            movingUp = true;
        }

        reloadQueueFromList(temp);
    }
    
    // --- C-SCAN: solo atiende en dirección ascendente, salta al inicio al terminar ---
    private void runCSCAN() {
        if (requestQueue.isEmpty()) return;

        MyList<Process> temp = drainQueueToList();
        sortByBlock(temp);

        // Buscar la siguiente solicitud por delante del cabezal
        for (int i = 0; i < temp.getSize(); i++) {
            if (temp.get(i).getTargetBlock() >= headPosition) {
                serveFromList(temp, i);
                reloadQueueFromList(temp);
                return;
            }
        }

        // Sin solicitudes adelante: el cabezal vuelve al inicio sin atender a nadie
        headPosition = 0;
        setChanged();
        notifyObservers("HEAD_RESET:0");

        reloadQueueFromList(temp);
    }

     // --- Atiende el proceso en el índice dado y notifica a los observers ---
    private void serveFromList(MyList<Process> list, int index) {
        Process process = list.get(index);
        list.remove(index);
        moveHead(process.getTargetBlock());
        process.setState("RUNNING");
        sleep(speedMs);
        process.setState("BLOCKED");
        setChanged();
        notifyObservers("REQUEST_SERVED:" + process.getTargetBlock());
    }


    // --- Mueve el cabezal hacia el destino; salta directo si la animación está desactivada ---
    private void moveHead(int target) {
        if (!animationEnabled) {
            System.out.printf("    head: %d -> %d  (distance: %d)%n",
                    headPosition, target, Math.abs(target - headPosition));
            headPosition = target;
            setChanged();
            notifyObservers("HEAD:" + headPosition);
            return;
        }
        int step = (target > headPosition) ? 1 : -1;
        while (headPosition != target) {
            headPosition += step;
            setChanged();
            notifyObservers("HEAD:" + headPosition);
            sleep(30);
        }
    }

    // --- Vacía la cola a una MyList para poder acceder por índice ---
    private MyList<Process> drainQueueToList() {
        MyList<Process> list = new MyList<>();
        while (!requestQueue.isEmpty()) {
            list.add(requestQueue.dequeue());
        }
        return list;
    }

      // --- Devuelve todos los procesos de la lista a la cola en el mismo orden ---
    private void reloadQueueFromList(MyList<Process> list) {
        for (int i = 0; i < list.getSize(); i++) {
            requestQueue.enqueue(list.get(i));
        }
    }

    // --- Ordena la lista de procesos por bloque objetivo de forma ascendente ---
    private void sortByBlock(MyList<Process> list) {
        int n = list.getSize();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j).getTargetBlock() > list.get(j + 1).getTargetBlock()) {
                    swapInList(list, j, j + 1);
                    break;
                }
            }
        }
    }
    
    // --- Intercambia dos elementos en una MyList reconstruyéndola ---
    private void swapInList(MyList<Process> list, int a, int b) {
        Process pa = list.get(a);
        Process pb = list.get(b);
        MyList<Process> temp = new MyList<>();
        for (int k = 0; k < list.getSize(); k++) temp.add(list.get(k));
        while (!list.isEmpty()) list.remove(0);
        for (int k = 0; k < temp.getSize(); k++) {
            if      (k == a) list.add(pb);
            else if (k == b) list.add(pa);
            else             list.add(temp.get(k));
        }
    }

    // --- Pausa el hilo la cantidad de milisegundos indicada ---
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


     // --- Retorna la posición actual del cabezal ---
    public int getHeadPosition()       { return headPosition; }

    // --- Retorna el nombre del algoritmo activo ---
    public String getAlgorithm()       { return algorithm; }

    // --- Retorna cuántas solicitudes esperan en la cola ---
    public int getPendingCount()       { return requestQueue.getSize(); }
}
