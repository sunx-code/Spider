//package com.fosun.fonova.moudle.channel.ctrip;
//
//import com.fosun.fonova.downloader.Downloader;
//import com.fosun.fonova.downloader.HttpClientDownloader;
//import com.fosun.fonova.downloader.Request;
//import com.fosun.fonova.downloader.Site;
//import com.fosun.fonova.downloader.selector.Node;
//import com.fosun.fonova.downloader.selector.Page;
//import com.fosun.fonova.spider.SeedImportable;
//import com.fosun.fonova.spider.queue.SpiderQueues;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.AbstractMap;
//
///**
// *
// */
//public class CtripSearch implements SeedImportable {
//    private static final Logger logger = LoggerFactory.getLogger(CtripSearch.class);
//
//    private String SEARCH_URL = "http://vacations.ctrip.com/whole-2B126DC2PPAGE_NUM/?searchValue=clubmed&searchText=clubmed#_flta";
//    private Downloader downloader = new HttpClientDownloader();
//    private Request request = new Request();
//    private Site site = new Site();
//    private AbstractMap.SimpleEntry<String, String> channel = new AbstractMap.SimpleEntry<String, String>("携程-旅游度假", "CtripSearch");
//
//    public static void main(String[] argbs){
//        new CtripSearch().importable(1,null);
//    }
//    /**
//     *
//     * @param priority
//     * @param spiderQueues
//     */
//    public void importable(int priority, SpiderQueues spiderQueues) {
//        //初始化请求头
//        site.addHeader("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
//        site.addHeader("accept-encoding","gzip, deflate, sdch, br");
//        site.addHeader("accept-language","zh-CN,zh;q=0.8");
//        site.addHeader("user-agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
//
//        //开始处理数据
//        dealData(priority,spiderQueues,1);
//    }
//
//    /**
//     * 处理网页数据
//     * @param pageNum
//     */
//    public void dealData(int priority, SpiderQueues spiderQueues,int pageNum){
//        try{
//            logger.info("开始处理第" + pageNum + "页..");
//            //开始请求网页源码
//            Page page = getPage(pageNum);
//            if(page == null || page.getHtml() == null || page.getHtml().length() <= 0){
//                logger.error("下载到的网页源码位空...");
//                return;
//            }
//            //处理数据
//            dealData(priority,spiderQueues,page);
//            //处理下一页的数据
//            String next = page.css("a:contains(下一页)","href");
//            if(next != null && next.length() > 0){
//                //线程休眠一定时间后在进行数据的查看
//                try{
//                    Thread.sleep(1000);
//                }catch (Exception e){}
//                //处理下一页的数据
//                dealData(priority,spiderQueues,pageNum + 1);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 处理数据
//     * @param priority
//     * @param spiderQueues
//     * @param page
//     */
//    public void dealData(int priority, SpiderQueues spiderQueues,Page page){
//        try{
//            Node node = page.$("div[class=main_mod product_box flag_product ]");
//            if(node == null || node.size() <= 0)return;
//            for(int i=0;i<node.size();i++){
//                String href = node.css(i,"h2.product_title a","href");
//                String txt = node.css(i,"h2.product_title a");
//                //类型
//                String type = node.css(i,"div.product_pic em");
//
//                logger.info(href + "\t" + txt + "\t" + type);
//
//                //携程-旅游度假
//                CtripSearchItem item = new CtripSearchItem(priority,channel,"",href,type);
//                spiderQueues.enqueue(item);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 下载网页源码
//     * @param pageNum
//     * @return
//     */
//    public Page getPage(int pageNum){
//        try{
//            //开始请求网页源码
//            return downloader.download(request.setUrl(SEARCH_URL.replaceAll("PAGE_NUM","" + pageNum)),site);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return null;
//    }
//}
