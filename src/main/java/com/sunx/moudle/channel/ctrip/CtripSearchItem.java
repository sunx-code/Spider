//package com.fosun.fonova.moudle.channel.ctrip;
//
//import com.fosun.fonova.cache.CacheService;
//import com.fosun.fonova.cache.dto.RawdataResult;
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
//import java.util.AbstractMap;
//import java.util.Date;
//import java.util.List;
//
///**
// *
// *
// */
//public class CtripSearchItem extends SpiderExecutableItem {
//    //日志记录工具
//    private static final Logger logger = LoggerFactory.getLogger(CtripSearchItem.class);
//    //详情页数据
//    private String url;
//    //详情页类型
//    private String type;
//
//    public CtripSearchItem() {
//        super(0, null, null);
//    }
//
//    public CtripSearchItem(int priority, AbstractMap.SimpleEntry<String, String> channel, String check_in_day,
//                               String url,String type) {
//        super(priority, channel, check_in_day);
//        this.url = url;
//        this.type = type;
//    }
//
//    public void bind(String url){
//        this.url = url;
//    }
//
//    public void bind(String url,String type){
//        this.url = url;
//        this.type = type;
//    }
//
//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        super.writeExternal(out);
//        out.writeUTF(url);
//        out.writeUTF(type);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        super.readExternal(in);
//        url = in.readUTF();
//        type = in.readUTF();
//    }
//
//    @Override
//    public void capture(WebBrowser webBrowser) throws InterruptedException, ResetWebBrowserException {
//        webBrowser.fetch(driver -> {
//            try{
//                //请求页面数据
//                driver.get(url);
//                webBrowser.waiting(5, 1, () -> true);
//
//                //根据不同的类型,自助游还是自由行还是酒店+景点来进行数据的分离处理
//                deal(webBrowser,driver);
//            }catch (Exception e){
//                logger.error(e.getMessage());
//            }
//            return null;
//        });
//    }
//
//    /**
//     * 处理业务数据
//     * @param driver
//     */
//    public void deal(WebBrowser webBrowser,RemoteWebDriver driver){
//        if(type.contains("自助游") || type.contains("跟团游")
//                || type.contains("私家团")){ //半自助游
//            dealBanZhiZhuYou(webBrowser,driver);
//        }else if(type.contains("酒店+景点")){
//            dealJiuDiaAndJingDian(webBrowser, driver);
//        }else if(type.contains("自由行")){
//            dealZhiYouXing(webBrowser,driver);
//        }
//    }
//
//    /**
//     * 处理自由行的数据
//     * @param browser
//     * @param driver
//     */
//    public void dealZhiYouXing(WebBrowser browser,RemoteWebDriver driver){
//        try{
//            //找到点击日期
//            List<WebElement> eles = driver.findElements(By.xpath("//td[@class='bg_blue on']//a"));
//            //对找到的日期进行判定
//            if(eles == null || eles.size() <= 0){
//                logger.error("解析自由行是出现错误,链接为：" + driver.getCurrentUrl() + ",抽取的xpath为://td[@class='bg_blue on']//a");
//                return;
//            }
//            //循环遍历日期,加载入住日期的数据
//            for(int i=0;i<eles.size();i++){
//                WebElement ele = eles.get(i);
//
//                //开始点击日期
//                ele.click();
//
//                //等待数据渲染完成
//                try{
//                    browser.waiting(5, 5, () -> {
//                        List<WebElement> findElements = driver.findElements(By.cssSelector(".htl_detail_img"));
//                        return !findElements.isEmpty();
//                    });
//                }catch (Exception e){
//                    e.printStackTrace();
//                    logger.error(e.getMessage());
//                }
//                //获取入住日期
//                String checkInDay = ele.getAttribute("date");
//                if(checkInDay != null){
//                    checkInDay = checkInDay.replaceAll("[^0-9]","");
//                }
//                //全部房型的css query
//                String showAllXpath = "//a[@class='other_more' and @data-hotel='allrooms']";
//                //展开全部房型
//                showAll(browser,driver,showAllXpath);
//                //进入到页面,首先加载的就是2个成人0个儿童的数据,所以现在截图,并保存数据到数据库
//                if(i % 2 == 0){
//                    save(driver,checkInDay,2,0,"" + i,"2成人");
//
//                    //修改儿童和成人的个数
//                    //人员类型修改为：2成人1儿童
//                    change(browser,driver,
//                            "//div[@id='js-child-selector']//input[@type='text']",
//                            "//div[@id='js-child-selector']//a[contains(text(),'1')]",
//                            "//a[@id='js-sure']",
//                            ".htl_detail_img",
//                            showAllXpath,
//                            checkInDay, 2,1,"2成人1儿童","" + i);
//
//                    //修改成人为1个成人1个儿童
//                    change(browser,driver,
//                            "//div[@id='js-adult-selector']//input[@type='text']",
//                            "//div[@id='js-adult-selector']//a[contains(text(),'1')]",
//                            "//a[@id='js-sure']",
//                            ".htl_detail_img",
//                            showAllXpath,
//                            checkInDay, 1,1,"1成人1儿童","" + i);
//                }else{
//                    save(driver,checkInDay,1,1,"" + i,"1成人1儿童");
//
//                    //修改儿童和成人的个数
//                    //人员类型修改为：2成人1儿童
//                    change(browser,driver,
//                            "//div[@id='js-adult-selector']//input[@type='text']",
//                            "//div[@id='js-adult-selector']//a[contains(text(),'2')]",
//                            "//a[@id='js-sure']",
//                            ".htl_detail_img",
//                            showAllXpath,
//                            checkInDay, 2,1,"2成人1儿童","" + i);
//
//                    //修改成人为1个成人1个儿童
//                    change(browser,driver,
//                            "//div[@id='js-child-selector']//input[@type='text']",
//                            "//div[@id='js-child-selector']//a[contains(text(),'0')]",
//                            "//a[@id='js-sure']",
//                            ".htl_detail_img",
//                            showAllXpath,
//                            checkInDay, 2,0,"2成人","" + i);
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            logger.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 处理酒店 + 景点的数据
//     * @param browser
//     * @param driver
//     */
//    public void dealJiuDiaAndJingDian(WebBrowser browser,RemoteWebDriver driver){
//        try{
//            //查找有数据的日期
//            List<WebElement> eles = driver.findElements(By.xpath("//td[@class='on']//a"));
//            if(eles == null || eles.size() <= 0){
//                logger.error("处理酒店+景点数据异常,抽取到的满足xpath(//td[@class='on']//a)规则的数据为空...");
//                return;
//            }
//            //循环遍历数据,逐个进行点击操作,等待数据加载完成,并截图保存数据
//            for(int i=0;i<eles.size();i++){
//                WebElement ele = eles.get(i);
//
//                //开始点计算标签
//                ele.click();
//
//                //等待加载完成
//                try{
//                    browser.waiting(5, 5, () -> {
//                        List<WebElement> findElements = driver.findElements(By.cssSelector("#hotelDetail"));
//                        return !findElements.isEmpty();
//                    });
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                //抽取入住时间
//                String checkInDay = ele.getAttribute("data-date");
//                if(checkInDay != null){
//                    checkInDay = checkInDay.replaceAll("[^0-9]","");
//                }
//                //截图保存数据
//                save(driver,checkInDay,0,0,"" + i,"");
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 处理自助游,跟团游 私家团
//     * @param driver
//     */
//    public void dealBanZhiZhuYou(WebBrowser webBrowser,RemoteWebDriver driver){
//        try{
//            //找到有数据的日期,进行点击
//            List<WebElement> list = driver.findElements(By.xpath("//td[@class='new_date on ']"));
//            //对查找结果进行盘点
//            if(list == null || list.size() <= 0){
//                //进行截图,保存数据
//                save(driver,"",2,0,"22","2成人");
//                return;
//            }
//            //如果抽取出来数据了,进行每个选择的数据进行点击
//            for(int i=0;i<list.size();i++){
//                WebElement ele = list.get(i);
//                //模拟点击
//                ele.click();
//                //等待点击结果加载渲染完毕
//                try{
//                    webBrowser.waiting(5, 5, () -> {
//                        List<WebElement> findElements = driver.findElements(By.cssSelector(".htl_resource_detail"));
//                        return !findElements.isEmpty();
//                    });
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                //获取入住日期数据
//                String checkInDay = getDay(driver,ele);
//
//                //当前默认为2个成人和1个儿童
//                save(driver,checkInDay,2,0,"" + i,"2成人");
//
//                //相同的处理逻辑,不同的子模块,展开全部对应的xpath路径不同,需要单独处理
//                String showAllCss = null;
//                if(url.contains("/morelinetravel/")){
//                    showAllCss = "//div[@class='flod_spread_btn']//a";
//                }
//
//                //人员类型修改为：2成人1儿童
//                change(webBrowser,driver,
//                       "//li[@class='children_num']//input[@type='number']",
//                        "//li[@class='children_num']//a[contains(text(),'1')]",
//                        null,
//                        ".htl_resource_detail",
//                        showAllCss,
//                        checkInDay, 2,1,"2成人1儿童","" + i);
//
//                //修改成人为1个成人1个儿童
//                change(webBrowser,driver,
//                        "//li[@class='tourist_num']//input[@type='number']",
//                        "//li[@class='tourist_num']//a[contains(text(),'1')]",
//                        null,
//                        ".htl_resource_detail",
//                        showAllCss,
//                        checkInDay, 1,1,"1成人1儿童","" + i);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            logger.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 获取当前的入住时间
//     * @param driver
//     * @param ele
//     * @return
//     */
//    public String getDay(RemoteWebDriver driver,WebElement ele){
//        WebElement day = driver.findElement(By.cssSelector("#js-departure-date"));
//        if(day == null)return "";
//        String value = day.getAttribute("value");
//        if(value != null){
//            value = value.replaceAll("[^0-9]","");
//        }
//        return value;
//    }
//
//    /**
//     * 修改数据,并截图
//     * @param browser
//     * @param driver
//     * @param adultNum
//     * @param childNum
//     */
//    public void change(WebBrowser browser,RemoteWebDriver driver,String clickXpath,String selectXpath,String submit,String waitCss,String showAllCss,String checkInDay,int adultNum,int childNum,String peopleType,String index){
//        WebElement tag = driver.findElementByXPath(clickXpath);
//        if(tag == null){
//            save(driver,checkInDay,adultNum,childNum,"33",peopleType);
//        }else{
//            //点击这个标签
//            tag.click();
//            //查找对应的选择项
//            WebElement select = driver.findElement(By.xpath(selectXpath));
//            if(select == null){
//                save(driver,checkInDay,adultNum,childNum,"44",peopleType);
//                return;
//            }
//            //选择数据进行点击
//            select.click();
//            //判断是自动加载还是需要手动提交
//            if(submit != null && submit.length() > 0){
//                //等待浏览器渲染
//                try{
//                    browser.waiting(3, 5, () -> {
//                        List<WebElement> findElements = driver.findElements(By.xpath(submit));
//                        return !findElements.isEmpty();
//                    });
//                }catch (Exception e){}
//
//                WebElement sub = driver.findElement(By.xpath(submit));
//                if(sub != null){
//                    sub.click();
//                }
//            }
//            //等待点击结果加载渲染完毕
//            try{
//                browser.waiting(5, 5, () -> {
//                    List<WebElement> findElements = driver.findElements(By.cssSelector(waitCss));
//                    return !findElements.isEmpty();
//                });
//            }catch (Exception e){}
//            //展开隐藏的数据
//            if(showAllCss != null && showAllCss.length() > 0){
//                showAll(browser,driver,showAllCss);
//            }
//
//            //截图保存数据
//            save(driver,checkInDay,adultNum,childNum,index,peopleType);
//        }
//    }
//
//    /**
//     * 展示全部
//     * @param driver
//     * @param showAllCss
//     */
//    public void showAll(WebBrowser browser,RemoteWebDriver driver,String showAllCss){
//        try{
//            List<WebElement> display = driver.findElements(By.xpath(showAllCss));
//            if(display != null && display.size() > 0){
//                for(WebElement ele : display){
//                    try{
//                        ele.click();
//
//                        browser.waiting(2, 1, () -> true);
//                    }catch (Exception e){}
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
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
//            String id = url + "," + crawlingDate + "," + channel + "," + region + "," + checkInDay + "," + adultNum + "," + childNum + "," + index + "," + peopleType;
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
//            rawdataResult.setSleepDays(0);
//            rawdataResult.setUrl(url);
//            rawdataResult.setFile_path_pagesource(filePath);
//            rawdataResult.setFile_path_image(imgPath);
//            rawdataResult.setPackageType(type);
//
//            CacheService.SERVICE.enquque(rawdataResult);
//
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
//        CtripSearchItem item = new CtripSearchItem();
//        item.channel = new AbstractMap.SimpleEntry<>("携程-旅游度假", "CtripSearch");
//        //测试半自助游
////        item.bind("http://vacations.ctrip.com/morelinetravel/p12726287s2.html","半自助游");
//        //测试自由行
//        item.bind("http://taocan.ctrip.com/freetravel/p1011967102s2.html","自由行");
//        //测试私家团
////        item.bind("http://vacations.ctrip.com/grouptravel/p7772092s2.html","私家团");
//        //测试酒店+景点
////        item.bind("http://taocan.ctrip.com/sh/65760.html","酒店+景点");
//        //测试跟团游
////        item.bind("http://vacations.ctrip.com/grouptravel/p12824812s2.html","跟团游");
//
//        item.capture(browser);
//    }
//}
