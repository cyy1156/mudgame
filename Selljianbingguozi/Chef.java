package com.Selljianbingguozi;

public class Chef implements Runnable{
    private static final int BASE_DELAY=1500;
    public void run()
    {
        System.out.println("煎饼师傅准备就绪，等待学生排队......");
        while(true)
        {

            try{
                int queueSize=Kitchen.getQueueSize();
                if(queueSize==0)
                {
                    System.out.println("当前没有人排队...");
                    Thread.sleep((int)(Math.random()*1000+500));
                }
                float makeDelay =calculateMakeDelay(queueSize);
                System.out.println("当前有"+queueSize+"人,煎饼师傅要"+(makeDelay/1000)+"秒/个");
                Order currentOrder=Kitchen.dequeueOrder();
                 if(currentOrder==null)continue;
                Student student=currentOrder.getStudent();
                int needCount=currentOrder.getPancakeCount();

                System.out.println("煎饼师傅正在做"+student.getName()+"的煎饼"+student.getName()+"需要"+needCount+"个，剩余"+queueSize+"人排队");
                for(int i=0;i<needCount;i++)
                {

                   Thread.sleep((int)makeDelay);
                   Kitchen.producePancakes();


                }

                System.out.println(student.getName()+"的"+needCount+"个已经全部做好了");
                Kitchen.takePancakes(currentOrder);
                System.out.println();
            }catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
    private int calculateMakeDelay(int queueSize)
    {
        if(queueSize<2){
            return BASE_DELAY;
        }
        else if(queueSize<=5)
        {
            return BASE_DELAY-500;
        }
        else {
            return BASE_DELAY-700;
        }
    }
}
