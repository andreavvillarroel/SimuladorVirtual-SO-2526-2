/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dataStructures;

/**
 *
 * @author Andrea
 */
public class MyQueue<T> {
    private Node<T> front;
    private Node<T> rear;
    private int size;

    public MyQueue() {
        this.front = this.rear = null;
        this.size = 0;
    }

    public void enqueue(T data) {
        Node<T> newNode = new Node<>(data);
        if (this.rear == null) {
            this.front = this.rear = newNode;
        } else {
            this.rear.setNext(newNode);
            this.rear = newNode;
        }
        size++;
    }

    public T dequeue() {
        if (this.front == null) return null;
        T data = this.front.getData();
        this.front = this.front.getNext();
        if (this.front == null) this.rear = null;
        size--;
        return data;
    }

    public T peek() {
        return (front != null) ? front.getData() : null;
    }

    public int getSize() { return size; }
    public boolean isEmpty() { return size == 0; }
}