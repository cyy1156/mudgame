package com.mudgame;

public class Mo extends Figure {

    public Mo()
    {
<<<<<<< HEAD
        super("èŽ«");
        System.out.println("èŽ«ï¼Œå‡ºç”ŸäºŽäº‘å±±ï¼Œå¿—å‘ä¸ºå®ˆæŠ¤è‹ç”Ÿã€‚");
=======
        super("Äª");
        System.out.println("Äª³öÉúÓÚÔÆÉ½£¬ÀøÖ¾³ÉÎªÎäÁÖ¸ßÊÖ¡£");
>>>>>>> e1501ce6d55714bf6aecc1e18dd84acda821f7d9
        initialise();
    }
    
    public void initialise() {
<<<<<<< HEAD
        Skill firstskill = new Skill("é£Žç³»çªè¢­", 2, "é£Žç³»å¼ºåŠ›æ”»å‡»");
        learnSkill(firstskill);
        System.out.println("ä½ å­¦ä¼šäº†ç¬¬ä¸€ä¸ªæŠ€èƒ½ï¼š" + firstskill.getName());
        int Grade = this.getGrade();
        if (Grade == 10) {
            Skill secondskill = new Skill("å½±è¢­", 1, "ç–¾é£Žå¦‚å½±æŽ è¿‡æ•Œäºº");
            learnSkill(secondskill);
            System.out.println("ä½ å­¦ä¼šäº†æ–°çš„æŠ€èƒ½ï¼š" + secondskill.getName());
        }
        if (Grade == 20) {
            Skill thirdskill = new Skill("äº‘æµ·å¤©ç½¡", 3, "å¬å”¤äº‘æµ·ä¹‹åŠ›è¿›è¡Œå¼ºåŠ›æ”»å‡»");
            learnSkill(thirdskill);
            System.out.println("ä½ å­¦ä¼šäº†æ–°çš„æŠ€èƒ½ï¼š" + thirdskill.getName());
=======
        Skill firstskill = new Skill("·çÏµÍ»´Ì", 2, "·çÏµÇ¿»¯´Ì»÷");
        learnSkill(firstskill);
        System.out.println("Äã»ñµÃÁËµÚÒ»¼¼ÄÜ" + firstskill.getName());
        int Grade = this.getGrade();
        if (Grade == 10) {
            Skill secondskill = new Skill("ÔÆÓ°²½", 1, "ÇáÓ¯»Ø±Ü¶¯×÷");
            learnSkill(secondskill);
            System.out.println("Äã»ñµÃÁËÐÂ¼¼ÄÜ" + secondskill.getName());
        }
        if (Grade == 20) {
            Skill thirdskill = new Skill("·çÔÆÁ¬»÷", 3, "¿ìËÙÈýÁ¬´ò");
            learnSkill(thirdskill);
            System.out.println("Äã»ñµÃÁËÐÂ¼¼ÄÜ" + thirdskill.getName());
>>>>>>> e1501ce6d55714bf6aecc1e18dd84acda821f7d9
        }
    }
}
