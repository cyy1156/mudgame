package com.mudgame;

import java.util.Scanner;

class NpcScene extends Scene {
    @Override
    public String getName() { return "npc"; }

    @Override
    public void enter(Figure p, Scanner sc) {
        Npc npc = new Npc("书生", "武林中最强的是什么？（答：心）", "心", 50);
        System.out.println("\n你遇到 NPC：" + npc.getName());
        System.out.println("他问你一个问题：");
        System.out.println(npc.getQuestion());

        String reply = sc.nextLine();

        if (npc.checkAnswer(reply)) {
            System.out.println("回答正确！获得奖励经验：" + npc.getExp());
            p.gainExp(npc.getExp());
        } else {
            System.out.println("回答错误，没有奖励。");
        }
    }
}
