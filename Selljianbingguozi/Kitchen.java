package com.Selljianbingguozi;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.*;
import java.util.concurrent.*;

public class Kitchen {
    private static int currentCount =0;
    private static final int MAX=5;
    private static final Lock myLock= new ReentrantLock();

    private static final Condition stockNotEnough =myLock.newCondition();
    private static final Condition stackNOtFull =myLock.newCondition();
    private static final String Chef="煎饼师傅";
    private static final BlockingQueue<Order> orderQueue=new ArrayBlockingQueue<>(20);
    private static final Set<String> purchasedStudent =new HashSet<>();

    public static  boolean enqueueOrder(Order order) throws InterruptedException
    {
        String studentName=order.getStudent().getName();

        myLock.lock();
        try{
            if(purchasedStudent.contains(studentName))
            {
                return false;
            }
            orderQueue.add(order);
            purchasedStudent.add(studentName);
            System.out.println(studentName+"来了，排在第"+orderQueue.size()+"个");
            return true;
        }finally {
            myLock.unlock();
        }
    }
    public static Order dequeueOrder()throws InterruptedException
    {
        return orderQueue.take();
    }
    public static int getQueueSize()
    {
        return orderQueue.size();
    }
    /*public static void consume(int amount,String name)
    {
        myLock.lock();
        try{
            while(currentdata<amount)
            {
                System.out.println("煎饼摊现有"+currentdata+name+"需要的量为"+amount);
                System.out.println("所以"+name+"在等待");
                waitCondition.await();
            }
            currentdata-=amount;
            System.out.println(name+"消费了"+amount+"煎饼摊还剩余"+currentdata);
            waitCondition.signalAll();

        }catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
        finally {
            myLock.unlock();
        }
    }
    public static void produce()
    {
        myLock.lock();
        try {
            while(currentdata>MAX)
            {
                System.out.println("煎饼摊现在有"+currentdata+"已经超过最大存量"+Chef+"休息一下");
                waitCondition.await();
            }
            Random random=new Random();
            currentdata+=random.nextInt(5)+1;
            System.out.println("煎饼摊当前还有"+currentdata+"个");
            waitCondition.signalAll();
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
        finally {
            myLock.unlock();
        }
    }


*/
    public static void takePancakes(Order order)throws InterruptedException
    {
        myLock.lock();
        try{
            Student student=order.getStudent();
            int needCount=order.getPancakeCount();
            while(needCount> currentCount)
            {
               System.out.println(student.getName()+"正在等饼，"
                       +student.getName()+"需要"+needCount+"当前有多少"+needCount+"个，等待师傅做饼.......");
               stockNotEnough.await();
            }
            currentCount-=needCount;
            System.out.println(student.getName()+"开心地购买了"+needCount+"个饼,当前还剩"+currentCount+"个");
            stackNOtFull.signalAll();
        }finally {
            myLock.unlock();
        }
    }
    public static void producePancakes()throws InterruptedException
    {
        myLock.lock();
        try {
            while (currentCount > MAX) {
                System.out.println(Chef + "已经做了" + currentCount + "个，还没有学生来我想休息一下...");
                stackNOtFull.await();
            }
            currentCount++;
            System.out.println(Chef+"做完了一个，当前有"+currentCount+"个");
            stockNotEnough.signalAll();
        }finally {
            myLock.unlock();
        }
    }
}
