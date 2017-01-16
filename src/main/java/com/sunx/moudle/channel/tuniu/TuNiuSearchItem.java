//package com.fosun.fonova.moudle.channel.tuniu;
//
//import com.fosun.fonova.cache.CacheService;
//import com.fosun.fonova.cache.dto.RawdataResult;
//import com.fosun.fonova.spider.channel.ali.AliTripSearchExecutableItem1205;
//import com.fosun.fonova.spider.executable.ResetWebBrowserException;
//import com.fosun.fonova.spider.executable.WebBrowser;
//import com.fosun.fonova.spider.queue.SpiderExecutableItem;
//import com.fosun.fonova.utils.DateUtil;
//import com.fosun.fonova.utils.FileUtil;
//import com.fosun.fonova.utils.ImageUtil;
//import com.fosun.fonova.utils.MessageDigestUtils;
//import com.fosun.fonova.utils._enum.MessageDigestMode;
//import org.apache.commons.io.FileUtils;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.remote.RemoteWebDriver;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectOutput;
//import java.security.GeneralSecurityException;
//import java.util.AbstractMap;
//import java.util.Date;
//import java.util.List;
//
///**
// *
// *
// */
//public class TuNiuSearchItem extends SpiderExecutableItem {
//    //日志记录工具
//    private static final Logger logger = LoggerFactory.getLogger(TuNiuSearchItem.class);
//    //详情页数据
//    private String url;
//    //region
//    private String region;
//    //入住几天
//    private int sleep;
//
//
//    public TuNiuSearchItem() {
//        super(0, null, null);
//    }
//
//    public TuNiuSearchItem(int priority, AbstractMap.SimpleEntry<String, String> channel, String checkInDay,
//                           String url, String region,int sleep) {
//        super(priority, channel, checkInDay);
//        this.url = url;
//        this.region = region;
//        this.sleep = sleep;
//    }
//
//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        super.writeExternal(out);
//        out.writeUTF(url);
//        out.writeUTF(region);
//        out.writeInt(sleep);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        super.readExternal(in);
//        url = in.readUTF();
//        region = in.readUTF();
//        sleep = in.readInt();
//    }
//
//    @Override
//    public void capture(WebBrowser browser) throws InterruptedException, ResetWebBrowserException {
//        try {
//            AliTripSearchExecutableItem1205.save(url,MessageDigestUtils
//                    .generateDigestByMessageDigestMode(MessageDigestMode.MD5, url),new Date(),0);
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
//        }
//        browser.fetch(driver -> {
//            try{
//                logger.info("请求页面数据...");
//                //请求页面数据
//                driver.get(url);
//                //等待浏览器渲染
//                logger.info("等待浏览器渲染...");
//                try{
//                    browser.waiting(10, 5, () -> {
//                        List<WebElement> findElements = driver.findElements(By.cssSelector(".item"));
//                        return !findElements.isEmpty();
//                    });
//                }catch (Exception e){}
//
//                logger.info("查看是否需要店家展开更多报价...");
//                //查看是否需要店家展开更多报价
//                String source = driver.getPageSource();
//                if(source.contains("查看更多产品报价")){
//                    logger.info("等待点击展开渲染为完毕...");
//                    List<WebElement> eles = driver.findElements(By.xpath("//div[@class='openRatePlans fleft']"));
//                    if(eles != null && eles.size() > 0){
//                        for(WebElement ele : eles){
//                            try{
//                                int currentPage = Integer.parseInt(ele.getAttribute("cur-page"));
//                                int totalPage = Integer.parseInt(ele.getAttribute("total-page"));
//
//                                //将一个标签展示完毕
//                                while(currentPage < totalPage){
//                                    try{
//                                        ele.click();
//
//                                        currentPage ++;
//
//                                        //等待点击展开渲染为完毕
//                                        browser.waiting(1, 1, () -> true);
//                                    }catch (Exception e){
//                                        break;
//                                    }
//                                }
//                            }catch (Exception e){
//                                logger.error(e.getMessage());
//                            }
//                        }
//                    }
//                }
//                logger.info("截图该网页,保存数据到数据库中...");
//                //截图该网页,保存数据到数据库中
//                save(driver,check_in_day.replaceAll("[^0-9]",""),0,0,"0","");
//            }catch (Exception e){
//                logger.error(e.getMessage());
//            }
//
//                AliTripSearchExecutableItem1205.save(url,MessageDigestUtils
//                        .generateDigestByMessageDigestMode(MessageDigestMode.MD5, url),new Date(),1);
//            return null;
//        });
//    }
//
//    /**
//     * 截图
//     * @param driver
//     */
//    public void save(RemoteWebDriver driver,String checkInDay,int adultNum,int childNum,String index,String peopleType){
//        try{
//            String region = "Unknown";
//            Date date = DateUtil.generateCrawlDate();
//            String crawlingDate = DateUtil.yyyymmdd(date);
//            String channel = super.channel.getKey();
//            String id = url + "," + crawlingDate + "," + channel + "," + region + "," + checkInDay + "," + 0;
//
//            String md5 = MessageDigestUtils
//                    .generateDigestByMessageDigestMode(MessageDigestMode.MD5, id);
//
//            //保存截图
//            byte[] screen = FileUtil.getScreenshot(driver);
//            String imgPath = FileUtil.createPageFile(crawlingDate, channel, region,
//                    checkInDay, adultNum,childNum,index + "-" + md5, ImageUtil.ImageType.BASE64);
//            FileUtils.writeByteArrayToFile(new File(imgPath), screen);
//
//            //保存网页源码
//            String filePath = FileUtil.createPageFile(crawlingDate, channel, region,
//                    checkInDay, adultNum,childNum,index + "-" + md5,ImageUtil.ImageType.TXT);
//            String pageSource = driver.getPageSource();
//            FileUtils.writeStringToFile(new File(filePath), pageSource, "UTF-8");
//
//            //封装结果数据
//            RawdataResult rawdataResult = new RawdataResult();
//            rawdataResult.setId(md5);
//            rawdataResult.setCrawlingDate(date);
//            rawdataResult.setChannel(channel);
//            rawdataResult.setRegion(region);
//            rawdataResult.setPersonGroup(peopleType);
//            rawdataResult.setHouseType(null);
//            rawdataResult.setCheckInDate(checkInDay);
//            rawdataResult.setSleepDays(sleep);
//            rawdataResult.setUrl(url);
//            rawdataResult.setFile_path_pagesource(filePath);
//            rawdataResult.setFile_path_image(imgPath);
//
//            CacheService.SERVICE.enquque(rawdataResult);
//        }catch (Exception e){
//            e.printStackTrace();
//            logger.error(e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        WebBrowser browser = new WebBrowser();
//        browser.init();
//
//        TuNiuSearchItem item = new TuNiuSearchItem(
//                1,
//                new AbstractMap.SimpleEntry<>("途牛-酒店","TuNiuSearch2"),
////                "2016-10-24",
////                "http://hotel.tuniu.com/detail/1406332275?checkindate=2016-10-24&checkoutdate=2016-10-26",
//                "2016-10-22",
//                "http://hotel.tuniu.com/detail/394573?checkindate=2016-10-22&checkoutdate=2016-10-23",
//                "三亚",
//                1);
//
//        item.capture(browser);
//    }
//}
