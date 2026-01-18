<<<<<<< HEAD
package com.mudgame;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Random;

/**
 * 武林秘籍修炼台 - 共享资源
 * 同一时间只能有一个玩家使用
 */
public class TrainingAltar {
    private boolean isOccupied = false;  // 是否被占用
    private String currentUser = null;   // 当前使用者
    private Random random = new Random();
    
    // 修炼问题库
    private static final String[][] TRAINING_QUESTIONS = {
        {"武学修炼的根本是什么？", "1", "心法", "招式", "内力", "兵器"},
        {"炼气化神的关键在于？", "2", "速度", "专注", "力量", "技巧"},
        {"武林秘籍修炼需要什么？", "3", "勇气", "金钱", "悟性", "运气"},
        {"内功修炼最重要的是？", "1", "循序渐进", "急功近利", "闭门造车", "投机取巧"},
        {"真正的武学大师具备什么品质？", "4", "力量", "速度", "技巧", "心境"}
    };
    
    /**
     * 尝试开始修炼
     * @return true 如果成功获取修炼台，false 如果已被占用
     */
    public synchronized boolean tryStartTraining(String playerName) {
        if (isOccupied) {
            return false;
        }
        isOccupied = true;
        currentUser = playerName;
        return true;
    }
    
    /**
     * 结束修炼
     */
    public synchronized void endTraining() {
        isOccupied = false;
        currentUser = null;
    }
    
    /**
     * 检查是否被占用
     */
    public synchronized boolean isOccupied() {
        return isOccupied;
    }
    
    /**
     * 获取当前使用者
     */
    public synchronized String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 执行修炼过程
     */
    public void performTraining(Figure player, BufferedReader in, PrintWriter out, 
                                NetworkGameServer server, String playerName) {
        try {
            out.println("\n════════════════════════════════════════════════════════");
            out.println("          【武林秘籍修炼台】          ");
            out.println("════════════════════════════════════════════════════════");
            out.println("你站在神秘的修炼台前，古老的秘籍散发着微光...");
            out.println("只有回答正确问题，才能开启修炼之门！");
            out.println("════════════════════════════════════════════════════════");
            
            // 随机选择一个问题
            String[] questionData = TRAINING_QUESTIONS[random.nextInt(TRAINING_QUESTIONS.length)];
            String question = questionData[0];
            String correctAnswer = questionData[1];
            
            out.println("\n【试炼问题】");
            out.println(question);
            out.println("\n请选择答案:");
            for (int i = 2; i < questionData.length; i++) {
                out.println((i - 1) + ". " + questionData[i]);
            }
            out.println("\n请输入选项编号(1-4):");
            
            String answer = in.readLine();
            
            if (answer != null && answer.trim().equals(correctAnswer)) {
                out.println("\n════════════════════════════════════════════════════════");
                out.println("回答正确！秘籍开启，修炼开始...");
                out.println("════════════════════════════════════════════════════════");
                
                // 全局广播修炼开始
                server.broadcastMessage("系统", 
                    "【武林秘籍】" + playerName + " 成功开启修炼台，正在修炼中...", null);
                
                // 模拟修炼过程
                out.println("\n修炼中...");
                Thread.sleep(2000);
                out.println("悟道中...");
                Thread.sleep(2000);
                out.println("融会贯通...");
                Thread.sleep(2000);
                
                // 随机获得奖励
                int rewardType = random.nextInt(100);
                int oldAttack = player.getAttack();
                int oldDefend = player.getDefend();
                int oldGrade = player.getGrade();
                
                if (rewardType < 30) {
                    // 30% 概率升级
                    int expGain = 150;
                    player.gainExp(expGain);
                    out.println("\n【修炼成果】");
                    out.println("获得大量经验：+" + expGain);
                    if (player.getGrade() > oldGrade) {
                        out.println("恭喜升级！等级：" + oldGrade + " → " + player.getGrade());
                        server.broadcastMessage("系统", 
                            "【武林秘籍】" + playerName + " 修炼有成，突破至 " + player.getGrade() + " 级！", null);
                    } else {
                        server.broadcastMessage("系统", 
                            "【武林秘籍】" + playerName + " 修炼完成，实力大增！", null);
                    }
                } else if (rewardType < 65) {
                    // 35% 概率增加攻击力
                    int attackBonus = 3 + random.nextInt(5); // 3-7点攻击
                    player.addAttack(attackBonus);
                    out.println("\n【修炼成果】");
                    out.println("攻击力大增：" + oldAttack + " → " + player.getAttack() + " (+" + attackBonus + ")");
                    server.broadcastMessage("系统", 
                        "【武林秘籍】" + playerName + " 修炼攻击秘籍，攻击力提升 " + attackBonus + " 点！", null);
                } else {
                    // 35% 概率增加防御力
                    int defendBonus = 3 + random.nextInt(5); // 3-7点防御
                    player.addDefend(defendBonus);
                    out.println("\n【修炼成果】");
                    out.println("防御力大增：" + oldDefend + " → " + player.getDefend() + " (+" + defendBonus + ")");
                    server.broadcastMessage("系统", 
                        "【武林秘籍】" + playerName + " 修炼防御秘籍，防御力提升 " + defendBonus + " 点！", null);
                }
                
                out.println("\n════════════════════════════════════════════════════════");
                out.println("修炼完成！修炼台已恢复可用状态。");
                out.println("════════════════════════════════════════════════════════");
                
            } else {
                out.println("\n════════════════════════════════════════════════════════");
                out.println("回答错误！秘籍紧闭，无法开启修炼...");
                out.println("正确答案是：" + questionData[Integer.parseInt(correctAnswer) + 1]);
                out.println("════════════════════════════════════════════════════════");
                
                server.broadcastMessage("系统", 
                    "【武林秘籍】" + playerName + " 试炼失败，修炼台已恢复可用。", null);
            }
            
        } catch (Exception e) {
            out.println("修炼过程发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


=======
package com.mudgame;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Random;

/**
 * 武林秘籍修炼台 - 共享资源
 * 同一时间只能有一个玩家使用
 */
public class TrainingAltar {
    private boolean isOccupied = false;  // 是否被占用
    private String currentUser = null;   // 当前使用者
    private Random random = new Random();
    
    // 修炼问题库
    private static final String[][] TRAINING_QUESTIONS = {
        {"武学修炼的根本是什么？", "1", "心法", "招式", "内力", "兵器"},
        {"炼气化神的关键在于？", "2", "速度", "专注", "力量", "技巧"},
        {"武林秘籍修炼需要什么？", "3", "勇气", "金钱", "悟性", "运气"},
        {"内功修炼最重要的是？", "1", "循序渐进", "急功近利", "闭门造车", "投机取巧"},
        {"真正的武学大师具备什么品质？", "4", "力量", "速度", "技巧", "心境"}
    };
    
    /**
     * 尝试开始修炼
     * @return true 如果成功获取修炼台，false 如果已被占用
     */
    public synchronized boolean tryStartTraining(String playerName) {
        if (isOccupied) {
            return false;
        }
        isOccupied = true;
        currentUser = playerName;
        return true;
    }
    
    /**
     * 结束修炼
     */
    public synchronized void endTraining() {
        isOccupied = false;
        currentUser = null;
    }
    
    /**
     * 检查是否被占用
     */
    public synchronized boolean isOccupied() {
        return isOccupied;
    }
    
    /**
     * 获取当前使用者
     */
    public synchronized String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 执行修炼过程
     */
    public void performTraining(Figure player, BufferedReader in, PrintWriter out, 
                                NetworkGameServer server, String playerName) {
        try {
            out.println("\n════════════════════════════════════════════════════════");
            out.println("          【武林秘籍修炼台】          ");
            out.println("════════════════════════════════════════════════════════");
            out.println("你站在神秘的修炼台前，古老的秘籍散发着微光...");
            out.println("只有回答正确问题，才能开启修炼之门！");
            out.println("════════════════════════════════════════════════════════");
            
            // 随机选择一个问题
            String[] questionData = TRAINING_QUESTIONS[random.nextInt(TRAINING_QUESTIONS.length)];
            String question = questionData[0];
            String correctAnswer = questionData[1];
            
            out.println("\n【试炼问题】");
            out.println(question);
            out.println("\n请选择答案:");
            for (int i = 2; i < questionData.length; i++) {
                out.println((i - 1) + ". " + questionData[i]);
            }
            out.println("\n请输入选项编号(1-4):");
            
            String answer = in.readLine();
            
            if (answer != null && answer.trim().equals(correctAnswer)) {
                out.println("\n════════════════════════════════════════════════════════");
                out.println("回答正确！秘籍开启，修炼开始...");
                out.println("════════════════════════════════════════════════════════");
                
                // 全局广播修炼开始
                server.broadcastMessage("系统", 
                    "【武林秘籍】" + playerName + " 成功开启修炼台，正在修炼中...", null);
                
                // 模拟修炼过程
                out.println("\n修炼中...");
                Thread.sleep(2000);
                out.println("悟道中...");
                Thread.sleep(2000);
                out.println("融会贯通...");
                Thread.sleep(2000);
                
                // 随机获得奖励
                int rewardType = random.nextInt(100);
                int oldAttack = player.getAttack();
                int oldDefend = player.getDefend();
                int oldGrade = player.getGrade();
                
                if (rewardType < 30) {
                    // 30% 概率升级
                    int expGain = 150;
                    player.gainExp(expGain);
                    out.println("\n【修炼成果】");
                    out.println("获得大量经验：+" + expGain);
                    if (player.getGrade() > oldGrade) {
                        out.println("恭喜升级！等级：" + oldGrade + " → " + player.getGrade());
                        server.broadcastMessage("系统", 
                            "【武林秘籍】" + playerName + " 修炼有成，突破至 " + player.getGrade() + " 级！", null);
                    } else {
                        server.broadcastMessage("系统", 
                            "【武林秘籍】" + playerName + " 修炼完成，实力大增！", null);
                    }
                } else if (rewardType < 65) {
                    // 35% 概率增加攻击力
                    int attackBonus = 3 + random.nextInt(5); // 3-7点攻击
                    player.addAttack(attackBonus);
                    out.println("\n【修炼成果】");
                    out.println("攻击力大增：" + oldAttack + " → " + player.getAttack() + " (+" + attackBonus + ")");
                    server.broadcastMessage("系统", 
                        "【武林秘籍】" + playerName + " 修炼攻击秘籍，攻击力提升 " + attackBonus + " 点！", null);
                } else {
                    // 35% 概率增加防御力
                    int defendBonus = 3 + random.nextInt(5); // 3-7点防御
                    player.addDefend(defendBonus);
                    out.println("\n【修炼成果】");
                    out.println("防御力大增：" + oldDefend + " → " + player.getDefend() + " (+" + defendBonus + ")");
                    server.broadcastMessage("系统", 
                        "【武林秘籍】" + playerName + " 修炼防御秘籍，防御力提升 " + defendBonus + " 点！", null);
                }
                
                out.println("\n════════════════════════════════════════════════════════");
                out.println("修炼完成！修炼台已恢复可用状态。");
                out.println("════════════════════════════════════════════════════════");
                
            } else {
                out.println("\n════════════════════════════════════════════════════════");
                out.println("回答错误！秘籍紧闭，无法开启修炼...");
                out.println("正确答案是：" + questionData[Integer.parseInt(correctAnswer) + 1]);
                out.println("════════════════════════════════════════════════════════");
                
                server.broadcastMessage("系统", 
                    "【武林秘籍】" + playerName + " 试炼失败，修炼台已恢复可用。", null);
            }
            
        } catch (Exception e) {
            out.println("修炼过程发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


>>>>>>> e1501ce6d55714bf6aecc1e18dd84acda821f7d9
