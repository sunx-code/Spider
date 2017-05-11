package com.fonova.spider;

import com.sunx.common.ip.IPFinder;
import com.sunx.spider.server.Spider;
import org.junit.Test;

/**
 *
 */
public class TestJarFile {

    @Test
    public void test() throws Exception{
//        Spider.me().deal(IParser.class,new JarFile("D:\\code\\fonova-spider\\classes\\artifacts\\spider_jar\\fonova-spider.jar"),"com/fosun/fonova/moudle/channel");
        Spider.me().scan();
    }

    @Test
    public void test1(){
        System.out.println(IPFinder.getIP());
    }
}
