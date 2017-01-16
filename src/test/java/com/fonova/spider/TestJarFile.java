package com.fonova.spider;

import com.sunx.spider.server.Spider;
import org.junit.Test;

/**
 * Created by sunxing on 2016/12/28.
 */
public class TestJarFile {

    @Test
    public void test() throws Exception{
//        Spider.me().deal(IParser.class,new JarFile("D:\\code\\fonova-spider\\classes\\artifacts\\spider_jar\\fonova-spider.jar"),"com/fosun/fonova/moudle/channel");
        Spider.me().scan();
    }
}
