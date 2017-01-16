//package com.fosun.fonova.moudle.channel.tuniu;
//
//import com.fosun.fonova.conf.Conf;
//import com.fosun.fonova.downloader.Downloader;
//import com.fosun.fonova.downloader.HttpClientDownloader;
//import com.fosun.fonova.downloader.Request;
//import com.fosun.fonova.downloader.Site;
//import com.fosun.fonova.downloader.selector.Node;
//import com.fosun.fonova.downloader.selector.Page;
//import com.fosun.fonova.spider.SeedImportable;
//import com.fosun.fonova.spider.queue.SpiderQueues;
//import org.apache.log4j.Logger;
//
//import java.text.SimpleDateFormat;
//import java.util.AbstractMap;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// *
// */
//public class TuNiuSearch implements SeedImportable {
//    //日志记录
//    private Logger logger = Logger.getLogger(TuNiuSearch.class);
//    //下载器
//    private Downloader downloader = new HttpClientDownloader();
//    //站点对象
//    private Site site = new Site();
//    //请求对象
//    private Request request = new Request();
//    //城市id
//    private Map<String,String> city = new HashMap<>();
//    //关键字
//    private String[] words = new String[]{"club%20med","clubmed"};
//    //国内搜索条件
//    private String GUONEI_SEARCH_URL = "http://hotel.tuniu.com/list?city=CITY_ID&checkindate=START_DAY&checkoutdate=END_DAY&keyword=KEY_WORD";
//    //渠道数据
//    private AbstractMap.SimpleEntry<String,String> channel = new AbstractMap.SimpleEntry<>("途牛-酒店","TuNiuSearch");
//    //
//    private String DETAIL_URL = "http://hotel.tuniu.com/detail/PRO_ID?checkindate=START_DAY&checkoutdate=END_DAY";
//    //日期格式化工具
//    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//    //
//    private Pattern pattern = Pattern.compile("(?:detail/)(\\d+)");
//    /**
//     * 抽取过程
//     * @param priority
//     * @param spiderQueues
//     */
//    public void importable(int priority, SpiderQueues spiderQueues){
//        //初始化城市数据
//        init();
//
//        //构造入住日期
//        Map<String,Map<Integer,String>> days = initDay();
//
//        //循环遍历请求,进行搜索下载解析
//        for(String word : words){
//            String checkInDay = sdf.format(new Date());
//            int sleep = 1;
//            String end = sdf.format(new Date(System.currentTimeMillis() + 86400l * 1000));
//
//            //处理国内数据
//            dealGuoNei(days,word, checkInDay, sleep, end, priority, spiderQueues);
//        }
//    }
//
//    /**
//     * 处理国内的数据
//     * @param checkInDay
//     * @param sleep
//     * @param end
//     * @param priority
//     * @param spiderQueues
//     */
//    public void dealGuoNei(Map<String,Map<Integer,String>> days,String word, String checkInDay, int sleep, String end, int priority, SpiderQueues spiderQueues){
//        try{
//            //循环遍历城市id
//            for(Map.Entry<String,String> entry : city.entrySet()){
//                //城市名称和id
//                String cityName = entry.getValue();
//                String cityId = entry.getKey();
//
//                //封装链接
//                String link = GUONEI_SEARCH_URL.replaceAll("CITY_ID",cityId)
//                                               .replaceAll("START_DAY",checkInDay)
//                                               .replaceAll("END_DAY",end)
//                                               .replaceAll("KEY_WORD",word);
//
//                //请求网页源码
//                Page page = getPage(link);
//                if(page == null || page.getHtml() == null){
//                    logger.error("下载网页源码错误,对应的链接地址为:" + link);
//                    continue;
//                }
//                //开始进行解析数据
//                parser(page,cityName,priority,spiderQueues,days);
//
//                //线程休眠一定时间后在进行下一个搜索的查看
//                try{
//                    Thread.sleep(1500);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 抽取数据
//     * @param cityName
//     * @param priority
//     * @param spiderQueues
//     */
//    private void parser(Page page,String cityName,int priority,SpiderQueues spiderQueues,Map<String,Map<Integer,String>> days){
//        try{
//            //抽取数据
//            Node node = page.$("div.hotel-list div.hotel");
//            if(node == null || node.size() <= 0){
//                logger.error("在途牛酒店中,搜索出来的结果中为空,对应的搜索条件为:div.hotel-list div.hotel");
//                return;
//            }
//            //循环遍历,抽取其中的链接,进行后续的操作
//            for(int i=0;i<node.size();i++){
//                //抽取其中的超链接
//                String href = node.css(i,"h2.nameAndIcon a.name","href");
//                String title = node.css(i,"h2.nameAndIcon a.name");
//                if(href == null || href.length() <= 0 || href.contains("javascript"))continue;
//
//                String id = find(href);
//
//                //循环遍历日期数据
//                for(Map.Entry<String,Map<Integer,String>> dayEntry : days.entrySet()){
//                    //入住日期
//                    String checkInDay = dayEntry.getKey();
//                    //遍历入住天数
//                    for(Map.Entry<Integer,String> sleepEntry : dayEntry.getValue().entrySet()){
//                        //入住多久
//                        int sleep = sleepEntry.getKey();
//                        //离开的日期
//                        String end = sleepEntry.getValue();
//
//                        String url = DETAIL_URL.replaceAll("PRO_ID",id).replaceAll("START_DAY",checkInDay).replaceAll("END_DAY",end);
//
//                        logger.info("途牛搜索: " + title + "\t" + url);
//
//                        TuNiuSearchItem item = new TuNiuSearchItem(priority,channel,checkInDay,url,cityName,sleep);
//                        spiderQueues.enqueue(item);
//                    }
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 下载网页源码
//     * @param link
//     * @return
//     */
//    public Page getPage(String link){
//        try{
//            return downloader.download(request.setUrl(link),site);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    /**
//     * 初始化城市数据
//     */
//    private void init(){
//        //初始化城市数据
//        city.put("628","东澳岛");
//        city.put("906","三亚");
//        city.put("705","桂林");
//    }
//
//    /**
//     * 初始化日期天数
//     * @return
//     */
//    private Map<String,Map<Integer,String>> initDay(){
//        int days = Conf.CRAWLING_RANGE_DAYS;
//        int[] duration = Conf.CRAWLING_SLEEP_DAYS;
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        Map<String,Map<Integer,String>> result = new HashMap<>();
//        //循环遍历数据,初始化日期数据
//        for(int i=0;i<days;i++){
//            //i = 0 表示当天
//            long current = System.currentTimeMillis() + 86400l * 1000 * i;
//            String start = sdf.format(new Date(current));
//
//            //结果存储集合
//            Map<Integer,String> sleepMap = new HashMap<>();
//            //循环遍历,获取返回日期
//            for(int sleep : duration){
//                long nextUnix = current + 86400l * 1000 * sleep;
//                String next = sdf.format(nextUnix);
//
//                sleepMap.put(sleep,next);
//            }
//            //向结果集合中插入数据
//            result.put(start,sleepMap);
//        }
//        return result;
//    }
//
//    private String find(String url){
//        Matcher matcher = pattern.matcher(url);
//        if(matcher.find())return matcher.group(1);
//        return null;
//    }
//}
