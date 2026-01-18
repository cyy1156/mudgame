package com.Selljianbingguozi;

import java.util.Random;

public class Student {
    private String name;

    // 预定义名字数组（提取为静态变量，避免重复创建）
    private static final String[] NAMES = {"王强","张三","李婷","赵伟","孙杰","陈琳",
            "周阳","吴丹","郑磊","王芳","冯浩","刘敏","程雪",
            "马俊","唐丽","朱辉","胡强","林静","郭宇","何颖",
            "高鹏","罗璇","谢萌","韩涛","蔡鑫","彭媛","袁博",
            "邓佳","许晴","夏磊","姜峰","薛燕","潘玥","董超","田硕",
            "丁蕊","任杰","沈琪","姚哲","卢薇","傅航","钟琳"};
    private static final Random random = new Random(); // 静态Random更高效

    public Student() {
        this.name = NAMES[random.nextInt(NAMES.length)];
    }

    public String getName() {
        return name;
    }
}