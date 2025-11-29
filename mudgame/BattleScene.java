package com.mudgame;
import java.util.Scanner;
public class BattleScene extends Scene {
    @Override
    public String getName() { return "battle"; }

    @Override
    public void enter(Figure p, Scanner sc) {
        Monster m = new Monster();
        System.out.println("\n你遇到了怪物：" + m.getName());

        BattleSystem.fight(p, m, sc);
    }
}
