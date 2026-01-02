package com.mudgame;

public class Mo extends Figure {

    public Mo() {
        super("莫");
        initialise();
    }
    
    public void initialise() {
        Skill firstskill = new Skill("FengXiZhanJi", 2, "JiFengQiangHuaZhanJi");
        learnSkill(firstskill);
        int Grade = this.getGrade();
        if (Grade == 10) {
            Skill secondskill = new Skill("YunYingShan", 1, "KuaiSuShanBiDongZuo");
            learnSkill(secondskill);
        }
        if (Grade == 20) {
            Skill thirdskill = new Skill("YunLongSanXian", 3, "LianXuSanCiGongJi");
            learnSkill(thirdskill);
        }
    }
}
