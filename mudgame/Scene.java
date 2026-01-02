package com.mudgame;
import java.util.Scanner;
abstract class Scene {
    abstract String getName();
    abstract void enter(Figure p, Scanner scanner);
}
