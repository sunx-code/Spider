package com.fonova.spider;

import com.sunx.common.ip.IPFinder;
import com.sunx.downloader.*;
import com.sunx.spider.server.Spider;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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

    @Test
    public void test2(){
        String url = "http://m.lvmama.com/route/router/rest.do?firstChannel=TOUCH&formate=json&lvversion=7.9.4&method=api.com.route.product.getHotelCombGoodsList&productId=625844&secondChannel=LVMM&version=1.0.0&visitDate=2017-06-08&_=1496574242399";
        Downloader downloader = new HttpClientDownloader();
        Request request = new Request();
        Site site = new Site();

        request.setUrl(url).setMethod(Method.GET);

        site.addHeader("Accept","application/json");
        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8");
        site.addHeader("signal","ab4494b2-f532-4f99-b57e-7ca121a137ca");
        site.addHeader("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Mobile Safari/537.36");
        site.setTimeOut(10000);

        String src = downloader.downloader(request,site);
        System.out.println(src);
    }
    @Test
    public void testDown(){
        String url = "https://shop140046115.taobao.com/search.htm";

        Site site = new Site();
        site.addHeader("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("accept-encoding","gzip, deflate, br");
        site.addHeader("accept-language","zh-CN,zh;q=0.8");
        site.addHeader("content-type","application/x-www-form-urlencoded");
        site.addHeader("referer","https://clubmed.fliggy.com/search.htm");
        site.addHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

        Map<String,String> map = new HashMap<String,String>();
        map.put("orderType","defaultSort");
        map.put("viewType","grid");
        map.put("keyword","clubmed");
        map.put("lowPrice","");
        map.put("highPrice","");

        Downloader downloader = new HttpClientDownloader();
        String src = downloader.downloader(new Request(url).setMethod(Method.POST).setPostData(map),site.setTimeOut(10000).setCharset("GBK"));
        System.out.println(src);
    }
}
