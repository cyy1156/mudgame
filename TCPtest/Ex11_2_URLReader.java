//package usst.course.ch11;//Ex11_2_URLReader.java
//Ex11_2_URLReader.java
package com.TCPtest;
import java.io.*;
import java.net.*;

public class Ex11_2_URLReader {
    // 声明main方法抛出所有例外
    public static void main(String[] args) throws Exception {
        // 构建一个URL 对象
        URL urlSina = new URL("http://www.sina.com.cn/");
        // 使用openStream得到一输入流并由此构造一个BufferedReader对象
        BufferedReader in = new BufferedReader(new InputStreamReader(
                urlSina.openStream()));
        String inputLine;
        // 从输入流不断的读数据，直到读完为止
        while ((inputLine = in.readLine()) != null)
            // 把读入的数据打印到屏幕上
            System.out.println(inputLine);
        // 关闭输入流
        in.close();
    }
}
