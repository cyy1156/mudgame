package com.Selljianbingguozi;

public class Order {
    private Student student;
    private int pancakeCount;
    public Order(Student student,int pancakeCount)
    {
        this.student=student;
        this.pancakeCount=pancakeCount;
    }

    public Student getStudent() {
        return student;
    }

    public int getPancakeCount() {
        return pancakeCount;
    }
}
