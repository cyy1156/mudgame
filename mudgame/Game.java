package com.mudgame;
import com.javaclass.keyboardinput;
import java.util.Scanner;

public class Game {
    private Scanner scanner = new Scanner(System.in);
    private Figure player;
    private SceneManager sceneManager;

    public void start() {
        System.out.println("******** 欢迎进入 MUD 武侠世界 ********");

        System.out.println("萧言");

        player = new XiaoYan();


        sceneManager = new SceneManager(player, scanner);

        sceneManager.registerScene(new BattleScene());
        sceneManager.registerScene(new NpcScene());

        mainMenu();
    }

    private void mainMenu() {
        while (true) {
            System.out.println("\n===== 主菜单 =====");
            System.out.println("1. 打怪练级");
            System.out.println("2. NPC 过招");
            System.out.println("3. 查看状态");
            System.out.println("4. 退出游戏");

            int ch = keyboardinput.getInt(1, 4);

            switch (ch) {
                case 1: sceneManager.enterScene("battle"); break;
                case 2: sceneManager.enterScene("npc"); break;
                case 3: player.showStatus(); break;
                case 4:
                    System.out.println("退出游戏，江湖再会！");
                    return;
            }
        }
    }

    /* 工具方法：限定输入范围 */

        public static void main(String[] args) {
            Game game = new Game();
            game.start();
        }
    }


