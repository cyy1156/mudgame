package com.mudgame;
import java.util.Random;
public class Monster {
    private int LifeValue;
    private int Grade;
    private int MaxLifeValue;
    private String name;
    private int defend;
    private int attack;
    private int exp;

    public void setDefend(int defend) {
        this.defend = defend;
    }

    //受到伤害
    public Monster()
    {
        // 扩展的怪物类型数组，包含更多样化的怪物
        String[] names = {
            // 普通怪物 (1-2级)
            "野狼", "山猪", "毒蛇", "野猫", "老鼠精",
            // 中级怪物 (2-4级)
            "恶虎", "盗匪", "山贼", "黑熊", "猛犬", "豺狼",
            // 高级怪物 (3-5级)
            "巨蟒", "恶鬼", "僵尸", "妖狐", "骷髅兵",
            // 精英怪物 (4-6级)
            "狼王", "虎王", "黑风寨主", "邪教护法", "鬼将",
            // Boss级怪物 (5-7级)
            "山大王", "邪教长老", "妖王", "魔头", "黑衣人"
        };
        
        Random r = new Random();
        Random random = new Random();
        this.name = names[r.nextInt(names.length)];
        
        // 根据怪物名称确定等级范围
        int minGrade = 1;
        int maxGrade = 5;
        
        // 调整特殊怪物的等级范围
        if (name.contains("王") || name.contains("魔") || name.contains("长老") || name.contains("大王")) {
            minGrade = 4;
            maxGrade = 7;
        } else if (name.contains("精英") || name.contains("护法") || name.contains("鬼将")) {
            minGrade = 3;
            maxGrade = 6;
        } else if (name.contains("蟒") || name.contains("鬼") || name.contains("僵尸") || name.contains("骷髅")) {
            minGrade = 2;
            maxGrade = 5;
        }
        
        this.Grade = random.nextInt(minGrade, maxGrade + 1);
        
        // 根据等级调整属性
        this.defend = 2 * this.Grade + random.nextInt(3); // 增加随机性
        this.attack = this.Grade + random.nextInt(2, 5);  // 增加攻击随机性
        this.MaxLifeValue = 5 * this.Grade + random.nextInt(5, 15);  // 增加血量随机性
        this.LifeValue = MaxLifeValue;
        this.exp = 40 + (this.Grade * 10) + random.nextInt(20);  // 经验值根据等级动态调整
    }
    public void takeDamage (int attack) {
        int oldLifeValue = this.LifeValue;
        int totalattack=attack-defend;
        if(totalattack<0)
        {
            totalattack=1;
        }
        this.LifeValue = Math.max(0, oldLifeValue - totalattack); // 血量最低为0
        // 扣血效果显示（MUD风格文本提示）
        System.out.println(name + " 受到 " + attack + " 点伤害！");
        System.out.println("血量变化：" + oldLifeValue + " → " + this.LifeValue);
        if (this.LifeValue == 0) {
            System.out.println(name + " 已阵亡！");
        }
    }
    
    //网络版本的遭受伤害（使用PrintWriter输出）
    public void takeDamageNetwork(int attack, java.io.PrintWriter out) {
        int oldLifeValue = this.LifeValue;
        int totalattack = attack - defend;
        if (totalattack < 0) {
            totalattack = 1;
        }
        this.LifeValue = Math.max(0, oldLifeValue - totalattack); // 血量最低为0
        // 扣血效果显示（MUD风格文本提示）
        out.println(name + " 受到 " + attack + " 点伤害！");
        out.println("血量变化：" + oldLifeValue + " → " + this.LifeValue);
        if (this.LifeValue == 0) {
            out.println(name + " 已阵亡！");
        }
    }

    public String getName() {
        return name;
    }

    public int getGrade() {
        return Grade;
    }

    public int getMaxLifeValue() {
        return MaxLifeValue;
    }

    public int getDefend() {
        return defend;
    }

    public int getAttack() {
        return attack;
    }

    public int getExp() {
        return exp;
    }

    public int getLifeValue() {
        return LifeValue;
    }
    boolean isAlive() { return LifeValue > 0; }
}
