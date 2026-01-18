package com.mudgame;

public class Mo extends Figure {

    public Mo()
    {
        super("莫");
        System.out.println("莫，出生于云山，志向为守护苍生。");
        initialise();
    }
    
    public void initialise() {
        Skill firstskill = new Skill("风系突袭", 2, "风系强力攻击");
        learnSkill(firstskill);
        System.out.println("你学会了第一个技能：" + firstskill.getName());
        int Grade = this.getGrade();
        if (Grade == 10) {
            Skill secondskill = new Skill("影袭", 1, "疾风如影掠过敌人");
            learnSkill(secondskill);
            System.out.println("你学会了新的技能：" + secondskill.getName());
        }
        if (Grade == 20) {
            Skill thirdskill = new Skill("云海天罡", 3, "召唤云海之力进行强力攻击");
            learnSkill(thirdskill);
            System.out.println("你学会了新的技能：" + thirdskill.getName());
        }
    }
}
