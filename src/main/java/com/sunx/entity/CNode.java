package com.sunx.entity;

/**
 * Created by sunx on 2017/6/3.
 */
public class CNode {
    //key
    private String clazz;
    //左右偏移多少
    private double left;
    //左右缩进多少
    private double indent;
    //上下浮层
    private double zindex;
    //放大缩小
    private double line;
    //对应的坐标
    private int index;

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getIndent() {
        return indent;
    }

    public void setIndent(double indent) {
        this.indent = indent;
    }

    public double getZindex() {
        return zindex;
    }

    public void setZindex(double zindex) {
        this.zindex = zindex;
    }

    public double getLine() {
        return line;
    }

    public void setLine(double line) {
        this.line = line;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String toString(){
        return "[clazz:" + clazz + ",left:" + left + ",indent:" + indent + ",zindex:" + zindex + ",line:" + line + "]";
    }
}
