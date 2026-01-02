package com.mudgame;

public class Npc {
    private String name;
    private String question;
    private String answer;
    private int exp;
    public Npc(String name,String question,String answer,int exp){
        this.name=name;
        this.answer=answer;
        this.exp=exp;
        this.question=question;

    }
    public boolean checkAnswer(String input) {
        return input.equalsIgnoreCase(answer);
    }

    public String getName() {
        return name;
    }
    public String getQuestion() {
        return question;
    }
    public String getAnswer() {
        return answer;
    }
    public int getExp() {
        return exp;
    }
}
