package com.cnlod.agora.entity;

import java.util.Map;

public class User {
//    {"list":[["34",947090903],["22",186136837],["2",448070539]],"num":3,"result":"ok"}
    private String result;
    private int num;
    private Map<String,Integer> list;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public Map<String, Integer> getList() {
        return list;
    }

    public void setList(Map<String, Integer> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "User{" +
                "result='" + result + '\'' +
                ", num=" + num +
                ", list=" + list +
                '}';
    }
}
