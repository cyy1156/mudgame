package com.Selljianbingguozi;

public class sell { // 类名首字母大写
    public static void main(String[] args) {
        Thread chefThread = new Thread(new Chef());
        chefThread.start();
        // 启动10个顾客线程

        for (int i = 0; i < 10; i++) {
            Thread customerThread = new Thread(new Customer());
            customerThread.start();
            try {
                Thread.sleep((int) (Math.random() * 1000 + 500));

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
