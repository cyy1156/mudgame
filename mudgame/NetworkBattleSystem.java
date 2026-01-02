package com.mudgame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 网络版战斗系统
 */
public class NetworkBattleSystem {
    
    public static void fight(Figure player, Monster monster, ClientHandler handler, NetworkGameServer server) {
        PrintWriter out = handler.getOutput();
        BufferedReader in = handler.getInput();
        
        int playercount = 0;
        int monstercount = 0;
        int playerolddefence = player.getDefend();
        int monsterolddefence = monster.getDefend();
        
        while (player.isAlive() && monster.isAlive()) {
            out.printf("\n你 HP:%d | %s HP:%d\n", player.getLifeValue(), monster.getName(), monster.getLifeValue());
            out.println("1. 普通攻击");
            out.println("2. 使用技能");
            out.println("请选择(1-2):");
            
            try {
                String choiceStr = in.readLine();
                int choice = Integer.parseInt(choiceStr);
                
                if (choice == 1) {
                    int dmg = Math.max(1, player.getAttack() - monster.getDefend());
                    monster.takeDamageNetwork(dmg, out);
                    if (playercount == 0) {
                        out.println(monster.getName() + "的防御力为" + monsterolddefence);
                        out.println("你造成了 " + dmg + " 点伤害！");
                        monster.setDefend(0);
                    } else {
                        out.println("怪物只能防御一次伤害");
                        out.println("你造成了 " + dmg + " 点伤害！");
                    }
                    playercount = 1;
                } else if (choice == 2) {
                    useSkillNetwork(player, monster, in, out);
                }
                
                if (!monster.isAlive()) break;
                
                int enemyDmg = Math.max(1, monster.getAttack() - player.getDefend());
                player.takeDamageNetwork(enemyDmg, out);
                if (monstercount == 0) {
                    out.println("你的防御力为" + playerolddefence);
                    out.println("你被造成了 " + enemyDmg + " 点伤害！");
                    player.setDefend(0);
                } else {
                    out.println(player.getName() + "的防御力为" + playerolddefence + "防御力只能抵挡一次伤害");
                    out.println("你被造成了 " + enemyDmg + " 点伤害！");
                }
                monstercount = 1;
            } catch (IOException | NumberFormatException e) {
                out.println("输入错误，请重试");
                continue;
            }
        }
        
        if (player.isAlive()) {
            out.println("\n你击败了 " + monster.getName());
            player.gainExp(monster.getExp());
            handler.sendStatus();
        } else {
            out.println("\n你被击败了……已自动恢复满血。");
            player.healToFull();
        }
        player.setDefend(playerolddefence);
        monster.setDefend(monsterolddefence);
    }
    
    private static void useSkillNetwork(Figure player, Monster monster, BufferedReader in, PrintWriter out) {
        try {
            java.util.List<Skill> skills = player.getSkills();
            if (skills.isEmpty()) {
                out.println("你没有技能！");
                return;
            }
            
            out.println("选择技能：");
            for (int i = 0; i < skills.size(); i++) {
                out.println((i + 1) + ". " + skills.get(i).getName());
            }
            out.println("请选择:");
            
            String choiceStr = in.readLine();
            int ch = Integer.parseInt(choiceStr);
            
            if (ch < 1 || ch > skills.size()) {
                out.println("无效选择！");
                return;
            }
            
            Skill s = skills.get(ch - 1);
            int dmg = s.calculateDamage(player.getAttack()) - monster.getDefend();
            dmg = Math.max(1, dmg);
            monster.takeDamageNetwork(dmg, out);
            out.println("你施放了 " + s.getName() + "，造成 " + dmg + " 点伤害！");
        } catch (IOException | NumberFormatException e) {
            out.println("技能使用错误！");
        }
    }
    
    /**
     * 玩家VS玩家战斗
     */
    public static void playerVsPlayer(Figure player1, Figure player2, 
                                      ClientHandler handler1, ClientHandler handler2,
                                      NetworkGameServer server) {
        PrintWriter out1 = handler1.getOutput();
        PrintWriter out2 = handler2.getOutput();
        BufferedReader in1 = handler1.getInput();
        BufferedReader in2 = handler2.getInput();
        
        out1.println("\n========== PK战斗开始 ==========");
        out1.println("对手: " + player2.getName());
        out2.println("\n========== PK战斗开始 ==========");
        out2.println("对手: " + player1.getName());
        
        int player1OldDefend = player1.getDefend();
        int player2OldDefend = player2.getDefend();
        int player1Count = 0;
        int player2Count = 0;
        
        boolean player1Turn = true;
        
        while (player1.isAlive() && player2.isAlive()) {
            if (player1Turn) {
                // 玩家1的回合
                out1.println("\n=== 你的回合 ===");
                out1.printf("你 HP:%d | %s HP:%d\n", player1.getLifeValue(), player2.getName(), player2.getLifeValue());
                out1.println("1. 普通攻击");
                out1.println("2. 使用技能");
                out1.println("请选择(1-2):");
                
                out2.println("\n=== " + player1.getName() + " 的回合，等待中... ===");
                out2.printf("%s HP:%d | 你 HP:%d\n", player1.getName(), player1.getLifeValue(), player2.getLifeValue());
                
                try {
                    String choiceStr = in1.readLine();
                    int choice = Integer.parseInt(choiceStr);
                    
                    if (choice == 1) {
                        int dmg = Math.max(1, player1.getAttack() - player2.getDefend());
                        player2.takeDamageNetwork(dmg, out2);
                        player2.takeDamageNetwork(dmg, out1);
                        
                        if (player2Count == 0) {
                            out1.println("你造成了 " + dmg + " 点伤害！");
                            out2.println(player1.getName() + " 对你造成了 " + dmg + " 点伤害！");
                            player2.setDefend(0);
                        } else {
                            out1.println("你造成了 " + dmg + " 点伤害！");
                            out2.println(player1.getName() + " 对你造成了 " + dmg + " 点伤害！");
                        }
                        player2Count = 1;
                    } else if (choice == 2) {
                        useSkillPVP(player1, player2, in1, out1, out2, handler1.getPlayerName());
                    }
                } catch (IOException | NumberFormatException e) {
                    out1.println("输入错误，请重试");
                    continue;
                }
            } else {
                // 玩家2的回合
                out2.println("\n=== 你的回合 ===");
                out2.printf("你 HP:%d | %s HP:%d\n", player2.getLifeValue(), player1.getName(), player1.getLifeValue());
                out2.println("1. 普通攻击");
                out2.println("2. 使用技能");
                out2.println("请选择(1-2):");
                
                out1.println("\n=== " + player2.getName() + " 的回合，等待中... ===");
                out1.printf("%s HP:%d | 你 HP:%d\n", player2.getName(), player2.getLifeValue(), player1.getLifeValue());
                
                try {
                    String choiceStr = in2.readLine();
                    int choice = Integer.parseInt(choiceStr);
                    
                    if (choice == 1) {
                        int dmg = Math.max(1, player2.getAttack() - player1.getDefend());
                        player1.takeDamageNetwork(dmg, out1);
                        player1.takeDamageNetwork(dmg, out2);
                        
                        if (player1Count == 0) {
                            out2.println("你造成了 " + dmg + " 点伤害！");
                            out1.println(player2.getName() + " 对你造成了 " + dmg + " 点伤害！");
                            player1.setDefend(0);
                        } else {
                            out2.println("你造成了 " + dmg + " 点伤害！");
                            out1.println(player2.getName() + " 对你造成了 " + dmg + " 点伤害！");
                        }
                        player1Count = 1;
                    } else if (choice == 2) {
                        useSkillPVP(player2, player1, in2, out2, out1, handler2.getPlayerName());
                    }
                } catch (IOException | NumberFormatException e) {
                    out2.println("输入错误，请重试");
                    continue;
                }
            }
            
            if (!player1.isAlive() || !player2.isAlive()) break;
            
            player1Turn = !player1Turn;
        }
        
        // 战斗结束
        if (!player1.isAlive()) {
            out1.println("\n========== 你被击败了！ ==========");
            out2.println("\n========== 你获胜了！ ==========");
            player1.healToFull();
            player2.gainExp(50); // 获胜奖励经验
            handler2.sendStatus();
        } else if (!player2.isAlive()) {
            out2.println("\n========== 你被击败了！ ==========");
            out1.println("\n========== 你获胜了！ ==========");
            player2.healToFull();
            player1.gainExp(50); // 获胜奖励经验
            handler1.sendStatus();
        }
        
        player1.setDefend(player1OldDefend);
        player2.setDefend(player2OldDefend);
        
        handler1.showMainMenu();
        handler2.showMainMenu();
    }
    
    private static void useSkillPVP(Figure attacker, Figure defender, BufferedReader in, 
                                    PrintWriter outAttacker, PrintWriter outDefender, String attackerName) {
        try {
            java.util.List<Skill> skills = attacker.getSkills();
            if (skills.isEmpty()) {
                outAttacker.println("你没有技能！");
                return;
            }
            
            outAttacker.println("选择技能：");
            for (int i = 0; i < skills.size(); i++) {
                outAttacker.println((i + 1) + ". " + skills.get(i).getName());
            }
            outAttacker.println("请选择:");
            
            String choiceStr = in.readLine();
            int ch = Integer.parseInt(choiceStr);
            
            if (ch < 1 || ch > skills.size()) {
                outAttacker.println("无效选择！");
                return;
            }
            
            Skill s = skills.get(ch - 1);
            int dmg = s.calculateDamage(attacker.getAttack()) - defender.getDefend();
            dmg = Math.max(1, dmg);
            defender.takeDamageNetwork(dmg, outAttacker);
            defender.takeDamageNetwork(dmg, outDefender);
            outAttacker.println("你施放了 " + s.getName() + "，对 " + defender.getName() + " 造成 " + dmg + " 点伤害！");
            outDefender.println(attackerName + " 施放了 " + s.getName() + "，对你造成 " + dmg + " 点伤害！");
        } catch (IOException | NumberFormatException e) {
            outAttacker.println("技能使用错误！");
        }
    }
}

