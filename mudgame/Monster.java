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
    //受到伤害
    public Monster()
    {
        String[] names = {"野狼", "山猪", "恶虎", "盗匪"};
        Random r = new Random();
        Random random=new Random();
        this.name=names[r.nextInt(names.length)];
        this.Grade=random.nextInt(1,5);
        this.defend=2*this.Grade;
        this.attack=this.Grade;
        this.MaxLifeValue=5*this.Grade;
        this.LifeValue= MaxLifeValue;
        this.exp=50;
    }
    public void takeDamage (int attack) {
        int oldLifeValue = this.LifeValue;
        int totalattack=defend-attack;
        if(totalattack>0)
        {
            totalattack=1;
        }
        this.LifeValue = Math.max(0, this.LifeValue +totalattack); // 血量最低为0
        // 扣血效果显示（MUD风格文本提示）
        System.out.println(name + " 受到 " + attack + " 点伤害！");
        System.out.println("血量变化：" + oldLifeValue + " → " + this.LifeValue);
        if (this.LifeValue == 0) {
            System.out.println(name + " 已阵亡！");
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
