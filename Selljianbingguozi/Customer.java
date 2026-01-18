package com.Selljianbingguozi;

import java.util.Random;

public class Customer implements Runnable {


    @Override
    public void run() {
        try {
            Random random = new Random();
            Student student = new Student();
            int amount = random.nextInt(3) + 1; // 1-3个需求
            System.out.println(student.getName() + "来到煎饼摊，需要" + amount + "个煎饼");
            Order order = new Order(student, amount);
            boolean enqueueSuccess = Kitchen.enqueueOrder(order);
            if (enqueueSuccess) {

                System.out.println(student.getName() + "正在排队等待");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}