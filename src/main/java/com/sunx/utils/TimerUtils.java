package com.sunx.utils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 日期类
 */
public class TimerUtils {

    /**
     * 获取距离第二天00:30多长时间
     *
     * @param current
     * @return
     */
    public static long period(long current) {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime().getTime() - current;
    }

    /**
     * 初始化日期数据
     */
    public static List<String> initDay(SimpleDateFormat sdf,int index) {
        List<String> list = new ArrayList<String>();
        long day = System.currentTimeMillis();

        //组合日期数据
        for (int i=0;i<=index;i++) {
            long next = day + 86400l * 1000 * i;
            String n = sdf.format(new Date(next));
            list.add(n);
        }
        return list;
    }

    /**
     *
     * @param sdf       时间格式化
     * @param age       年龄
     * @return
     */
    public static String birthday(SimpleDateFormat sdf,int age){
        long last = System.currentTimeMillis() - (86400l * 1000 * 365 * age);
        return sdf.format(new Date(last));
    }

    /**
     *
     * @param sdf
     * @param current
     * @param increase
     * @return
     */
    public static String toDate(SimpleDateFormat sdf,String current,int increase){
        try{
            Date now = sdf.parse(current);
            if(now == null)return null;
            long unix = now.getTime();
            return sdf.format(new Date(unix + 86400l * increase * 1000));
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
