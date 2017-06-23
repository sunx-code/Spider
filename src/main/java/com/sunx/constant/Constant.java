package com.sunx.constant;

/**
 * 常量类
 */
public class Constant {
    //数据库配置文件存储路径
    public static final String DB_CONFIG_FILE ="conf/db.properties";
    //数据库连接池默认名称
    public static final String DEFAULT_DB_POOL = "localhost";

    //爬虫参数
    public static int[] NUMBER_OF_ADULT = { 1,2 };// 几个成人
    public static int[] NUMBER_OF_CHILDREN = { 0, 1 };// 几个儿童
    public static int CRAWLING_RANGE_DAYS = 30; // 爬取多少天
    public static int[] CRAWLING_SLEEP_DAYS = { 2, 3, 4, 5};// 爬取几晚，6, 7, 14


    public static String PATH_DRIVER = "driver.path";
    public static int DYNAMIC_LOAD_TIMEOUT = 1000;
    public static int WEB_DRIVER_NUM = 15;

    public static String DEFAULT_IMG_SAVE = "img.save.path";


    public static final int TASK_NEW = 0;
    public static final int TASK_SUCESS = 1;
    public static final int TASK_FAIL = -1;

    public static final String DEFALUT_REGION = "Unknow";

    public static final int DEFAULT_TIME_OUT = 10000;

    public static final String TEMPLATE_FILE_LIST_PATH = "conf/template";


    public static final String DEFAULT_ENCODING = "UTF-8";

}
