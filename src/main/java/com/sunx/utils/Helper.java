package com.sunx.utils;

/**
 * Created by sunxing on 2016/12/19.
 */
public class Helper {

    /**
     * 返回通用的方法
     * @param adultNum
     * @param childNum
     * @return
     */
    public static String people(int adultNum,int childNum){
        if(adultNum == 2 && childNum == 1){
            return "2成人1儿童";
        }else if(adultNum == 2 && childNum == 0){
            return "2成人";
        }else if(adultNum == 1 && childNum == 1){
            return "1成人1儿童";
        }
        return null;
    }

    /**
     * 清洗字符串
     * @param str
     * @param child
     * @param replace
     * @return
     */
    public static String clean(String str,String child,String replace){
        if(str == null)return null;
        return str.replaceAll(child,replace);
    }

    /**
     * 转成int
     * @param str
     * @return
     */
    public static int toInt(String str){
        try{
            return Integer.parseInt(str);
        } catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }
}