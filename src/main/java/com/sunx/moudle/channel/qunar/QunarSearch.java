//package com.fosun.fonova.moudle.channel.qunar;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.fosun.fonova.downloader.Downloader;
//import com.fosun.fonova.downloader.HttpClientDownloader;
//import com.fosun.fonova.downloader.Request;
//import com.fosun.fonova.downloader.Site;
//import com.fosun.fonova.downloader.selector.Page;
//import com.fosun.fonova.spider.SeedImportable;
//import com.fosun.fonova.spider.queue.SpiderQueues;
//import org.apache.log4j.Logger;
//
//import java.util.AbstractMap;
//
///**
// *
// *
// */
//public class QunarSearch implements SeedImportable {
//    //抽取链接地址
//    private String SEARCH_URL = "https://dujia.qunar.com/golfz/routeList/adaptors/pcTop?isTouch=0&t=all&o=pop-desc&lm=PAGE_NUM_START%2CPAGE_NUM_END&q=clubmed&d=%E4%B8%8A%E6%B5%B7&s=all&qs_ts=TIME_UNIX&tm=i01_search_newc&sourcepage=list&userResident=%E4%B8%8A%E6%B5%B7&aroundWeight=1&m=l%2CbookingInfo%2Clm&displayStatus=pc&fl=hotelPos%2CextendFunction%2Ctype%2Cdep%2Cwrpparid%2CisTuan%2CplayDesc%2ClineSubjects%2Cprice%2Cid%2CbookableTomorrow%2CproductFeatures%2CneedTip%2ChotelGradeText%2CbookableWeekend%2CproductPromotions%2CproductScore%2Ccountries%2CencodeId%2CtrafficInfo%2Csights%2CbookableToday%2Cthumb%2CsoldCount90%2CtagInfos%2CbusiType%2ClongPlanId%2Curl%2CbookingTypeCateTitle%2CsightTicket%2CproductRate%2Cpid%2CbookingAdult%2CsoldCount%2Creviews%2Cdetails%2Ccomplement%2ChotelInfo%2CshortPlanId%2CbookingDate%2CoriginalPrice%2CbookingChild%2Ctitle%2Ccitys%2ClongPlanTitle%2Csummary%2Cfeeinfo%2CbusiLine%2CbookingTimeDiff%2CproductSuccRate%2CshortPlanTitle%2CtwoLeveltype%2CsightInfos%2CvoyageRegion%2CdisplayAdStyle%2CteamType%2CtuanTtsId%2CchargeType%2CpriceDate";
//    //日志记录
//    private Logger logger = Logger.getLogger(QunarSearch.class);
//    //下载器
//    private Downloader downloader = new HttpClientDownloader();
//    //站点对象
//    private Site site = new Site();
//    //请求对象
//    private Request request = new Request();
//    //渠道
//    private AbstractMap.SimpleEntry<String, String> channel = new AbstractMap.SimpleEntry<>("去哪儿-搜索", "QunarSearch");
//
//    /**
//     * 抽取数据
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
//        //拼接第一页的请求,并封装数据
//        dealData(priority,spiderQueues,0,60,System.currentTimeMillis());
//    }
//
//    /**
//     * 1 根据传进来的页码数据和时间戳
//     * 2 封装请求连接,并请求数据
//     * 3 解析数据并存储到文件队列中
//     * 4 根据下载到的数据判定是否需要进行翻页
//     * 5 递归调用自身
//     * @param priority
//     * @param spiderQueues
//     * @param startNum
//     * @param endNum
//     * @param timeUnix
//     */
//    private void dealData(int priority, SpiderQueues spiderQueues,int startNum,int endNum,long timeUnix){
//        try{
//            //封装链接
//            String link = SEARCH_URL.replaceAll("PAGE_NUM_START","" + startNum).replaceAll("PAGE_NUM_END","" + endNum).replaceAll("TIME_UNIX","" + timeUnix);
//            //开始下载网页源码
//            Page page = downloader.download(request.setUrl(link),site);
//            if(page == null || page.getHtml() == null){
//                logger.error("去哪儿下载数据错误,对应的链接地址为:" + link);
//                return;
//            }
//            if(page.getHtml().length() <= 50){
//                logger.error("下载到的数据结果可能异常,对应的链接为:" + link + "\n,下载到的结果为:" + page.getHtml());
//                return;
//            }
//            //开始处理数据
//            dealData(priority,spiderQueues,endNum,page.getHtml());
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 处理结果数据
//     * @param priority
//     * @param spiderQueues
//     * @param endNum
//     * @param json
//     */
//    private void dealData(int priority, SpiderQueues spiderQueues,int endNum,String json){
//        try{
//            //格式化数据为json对象
//            JSONObject bean = JSON.parseObject(json);
//            //判断是否含有data
//            if(!bean.containsKey("data"))return;
//            JSONObject data = bean.getJSONObject("data");
//            //获取到data以后,进行list的判断
//            if(!data.containsKey("list"))return;
//            JSONObject list = data.getJSONObject("list");
//            //获取到真正的数据集合后,进行抽取总的数据量和结果集合
//            int totalNum = list.containsKey("numFound")?list.getIntValue("numFound"):0;
//            JSONArray array = list.containsKey("results")?list.getJSONArray("results"):null;
//            //处理结果集合
//            dealResult(priority,spiderQueues,array);
//            //处理下一页数据
//            if(totalNum > endNum){
//                try{
//                    logger.info("线程休眠2秒钟以后进行下一页的处理...");
//                    Thread.sleep(2000);
//                }catch (Exception e){}
//                //开始处理下一页
//                dealData(priority,spiderQueues,endNum,endNum + 60,System.currentTimeMillis());
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 处理最终结果
//     * @param priority
//     * @param spiderQueues
//     * @param array
//     */
//    private void dealResult(int priority, SpiderQueues spiderQueues,JSONArray array){
//        if(array == null || array.size() <= 0)return;
//        for(int i=0;i<array.size();i++){
//            JSONObject bean = array.getJSONObject(i);
//            String title = bean.containsKey("title")?bean.getString("title"):null;
//            String href = bean.containsKey("url")?bean.getString("url"):null;
//            String type = bean.containsKey("extendFunction")?bean.getString("extendFunction"):null;
//            String summary = summary(bean);
//            //数据判定
//            if(href == null || href.length() <= 0)return;
//
//            logger.info(title + "\t" + href);
//            //封装对象数据,存储到文件队列中
//            String link = "http:" + href;
//
//            //存储数据到文件队列中
//            QunarSearchItem item = new QunarSearchItem(priority,channel,link,type,summary);
//            spiderQueues.enqueue(item);
//        }
//    }
//
//    /**
//     * 获取店铺类型
//     * @param bean
//     * @return
//     */
//    private String summary(JSONObject bean){
//        JSONObject summary = bean.containsKey("summary")?bean.getJSONObject("summary"):null;
//        if(summary == null)return null;
//        JSONObject supplier = summary.containsKey("supplier")?summary.getJSONObject("supplier"):null;
//        if(supplier == null)return null;
//        return supplier.containsKey("url")?supplier.getString("url"):null;
//    }
//}
