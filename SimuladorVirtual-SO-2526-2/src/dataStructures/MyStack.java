/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dataStructures;

/**
 *
 * @author Andrea
 */
public class MyStack<T> {
    private Node<T> top;
    private int size;

    public MyStack() {
        this.top = null;
        this.size = 0;
    }

    // Insertar arriba de la pila
    public void push(T data) {
        Node<T> newNode = new Node<>(data);
        newNode.setNext(top);
        top = newNode;
        size++;
    }

    // Sacar el de arriba
    public T pop() {
        if (isEmpty()) return null;
        T data = top.getData();
        top = top.getNext();
        size--;
        return data;
    }

    // Ver el de arriba sin sacarlo
    public T peek() {
        return (top != null) ? top.getData() : null;
    }

    public int getSize() { return size; }
    public boolean isEmpty() { return size == 0; }
}