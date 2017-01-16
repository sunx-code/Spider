//package com.fosun.fonova.moudle.channel.qunar;
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
//import org.apache.log4j.Logger;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.remote.RemoteWebDriver;
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
// * 根据搜索的结果封装好的链接请求
// * 请求页码,根据请求到的页面后,点可以入住的日期
// * 选择2成人0儿童，2成人1儿童，1成人1儿童
// */
//public class QunarSearchItem extends SpiderExecutableItem {
//    private transient Logger logger = Logger.getLogger(QunarSearchItem.class);
//    //商品也链接地址
//    private String url;
//    private String type;
//    private String summary;
//
//    public QunarSearchItem() {
//        super(0, null, null);
//    }
//
//    public QunarSearchItem(int priority, AbstractMap.SimpleEntry<String, String> channel,String url,String type,String summary) {
//        super(priority, channel, "");
//        this.url = url;
//        this.type = type;
//        this.summary = summary;
//    }
//
//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        super.writeExternal(out);
//        out.writeUTF(url);
//        out.writeUTF(type);
//        out.writeUTF(summary);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        super.readExternal(in);
//        url = in.readUTF();
//        type = in.readUTF();
//        summary = in.readUTF();
//    }
//
//    @Override
//    public void capture(WebBrowser browser) throws InterruptedException, ResetWebBrowserException {
//        browser.fetch(driver -> {
//            try{
//                logger.info("开始请求页面:" + url);
//                //请求页面数据
//                driver.get(url);
//                browser.waiting(5, 1, () -> true);
//
//                //根据不同的类型,自助游还是自由行还是酒店+景点来进行数据的分离处理
//                deal(browser,driver);
//            }catch (Exception e){
//                logger.error(e.getMessage());
//            }
//            return null;
//        });
//    }
//
//    /**
//     * 处理数据,找到在售的日期,也就是入住日期
//     * @param browser
//     * @param driver
//     */
//    private void deal(WebBrowser browser,RemoteWebDriver driver){
//        try{
//            //判断类型,根据不同的类型来进行不同的解析
//            if(summary == null || summary.contains("package.qunar.com")){
//                //修改为：
//                type = "package-自由行";
//                //处理数据
//                dealData(browser, driver);
//            }else{
//                dealFreeData(browser,driver);
//            }
//        }catch (Exception e){
//            logger.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 处理携程自由行数据
//     * @param browser
//     * @param driver
//     */
//    private void dealFreeData(WebBrowser browser,RemoteWebDriver driver){
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
//                String checkInDay = ele.getAttribute("data-date");
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
//    /**
//     * 抽取数据
//     * @param browser
//     * @param driver
//     */
//    private void dealData(WebBrowser browser,RemoteWebDriver driver){
//        try{
//            //抽取入住日期
//            List<WebElement> eles = driver.findElements(By.xpath("//td[@data-date]//p[@class='price']/parent::div/parent::*"));
//            if(eles == null || eles.size() <= 0){
//                logger.error("去哪儿抓取数据中抽取数据失败,当前连接为:" + driver.getCurrentUrl() + ",抽取的规则为://td[@data-date]//p[@class='price']/parent::div/parent::*");
//                return;
//            }
//            //处理抽取得到的每一个标签,点击后,修改成人儿童的数据,并截图
//            dealData(eles,browser,driver);
//
//            //切换月份
//            changeMonth(browser,driver);
//        }catch (Exception e){
//            logger.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 只最多切换一个分页,防止展示月份太多
//     * 只要最近30天的,默认展开一个分页
//     * @param browser
//     * @param driver
//     */
//    private void changeMonth(WebBrowser browser,RemoteWebDriver driver){
//        try{
//            //查找可选的月份
//            List<WebElement> eles = driver.findElements(By.xpath("//ul[@class='js-month-list month-list']//li[not(contains(@class,'on'))]"));
//            if(eles == null){
//                logger.error("切换月份是出现错误,抽取规则为://ul[@class='js-month-list month-list']//li[not(contains(@class,'on'))],对应的链接为:" + driver.getCurrentUrl());
//                return;
//            }
//            for(int i=0;i<eles.size();i++){
//                try{
//                    WebElement month = eles.get(i);
//                    month.click();
//
//                    browser.waiting(5,3,() -> true);
//                    //处理数据
//                    dealData(browser,driver);
//                    //
//                    break;
//                }catch (Exception e){
//                    logger.error(e.getMessage());
//                }
//            }
//        }catch (Exception e){
//            logger.error(e.getMessage());
//        }
//    }
//
//    /**
//     * 处理数据
//     * @param eles
//     * @param browser
//     * @param driver
//     */
//    private void dealData(List<WebElement> eles,WebBrowser browser,RemoteWebDriver driver){
//        //循环遍历集合,进行点击
//        for(int i=0;i<eles.size();i++){
//            //获取标签
//            WebElement ele = eles.get(i);
//           try{
//               //获取入住日期
//               String checkInDay = ele.getAttribute("data-date");
//               if(checkInDay != null){
//                   checkInDay = checkInDay.replaceAll("-","");
//               }
//
//               //获取td标签下的div标签,用于点击
//               WebElement div = ele.findElement(By.cssSelector("div"));
//               //点击标签
//               div.click();
//
//               //等待渲染完毕
//               browser.waiting(1,1,() -> true);
//
//               //获取当前对应的成人个数,儿童个数
//               int adultNum = getNum(driver,"//input[@class='textbox js-adult-num']","value");
//               int childNum = getNum(driver,"//input[@class='textbox js-child-num']","value");
//               //判断是否有小孩这个标签
//               if(childNum < 0){
//                   save(driver,checkInDay,2,0,"0","2成人");
//               }else{
//                   //修改人员选择为:2成人0儿童
//                   change(browser, driver, checkInDay, 2, 2 - adultNum, 0, 0 - childNum, "0", "2成人");
//                   //修改人员选择为:2成人1儿童  儿童不变,成人修改为1
//                   change(browser,driver,checkInDay,2,0,1,1,"0","2成人1儿童");
//                   //修改人员选择为:1成人1儿童  儿童不变,成人修改为1
//                   change(browser,driver,checkInDay,1,-1,1,0,"0","1成人1儿童");
//               }
//           }catch (Exception e){
//               logger.error(e.getMessage());
//           }
//        }
//    }
//
//    /**
//     * 修改
//     * @param browser
//     * @param driver
//     * @param checkInDay
//     * @param adultNum
//     * @param childNum
//     * @param index
//     */
//    private void change(WebBrowser browser,RemoteWebDriver driver,String checkInDay,int adultNum,int changeAdultNum,int childNum,int changeChildNum,String index,String peopleType){
//        //修改成人的个数
//        if(changeAdultNum > 0){
//            //指定xpath路径对应的标签,进行changeAdultNum次点击
//            add(browser,driver,"//span[contains(@class,'js-adult-num-increase')]",changeAdultNum);
//        }else if(changeAdultNum < 0){
//            //指定xpath路径对应的标签,进行changeAdultNum次点击
//            add(browser,driver,"//span[contains(@class,'js-adult-num-decrease')]",Math.abs(changeAdultNum));
//        }
//        //修改儿童的数据
//        if(changeChildNum > 0){
//            //指定xpath路径对应的标签,进行changeAdultNum次点击
//            add(browser,driver,"//span[contains(@class,'js-child-num-increase')]",changeChildNum);
//        }else if(changeChildNum < 0){
//            //指定xpath路径对应的标签,进行changeAdultNum次点击
//            add(browser,driver,"//span[contains(@class,'js-child-num-decrease')]",Math.abs(changeChildNum));
//        }
//        //保存数据到文件队列中
//        save(driver,checkInDay,adultNum,childNum,index,peopleType);
//    }
//
//    /**
//     * 指定标签进行点击
//     * @param browser
//     * @param driver
//     * @param xpath
//     * @param clickNum
//     */
//    private void add(WebBrowser browser,RemoteWebDriver driver,String xpath,int clickNum) {
//        WebElement ele = driver.findElement(By.xpath(xpath));
//        for(int i=0;i<clickNum;i++){
//            try{
//                ele.click();
//                browser.waiting(2,1,() -> true);
//            }catch (Exception e){
//                logger.error(e.getMessage());
//            }
//        }
//    }
//    /**
//     * 获取值
//     * @param xpath
//     * @param attribute
//     * @return
//     */
//    private int getNum(RemoteWebDriver driver,String xpath,String attribute){
//        WebElement element = driver.findElement(By.xpath(xpath));
//        if(element == null)return -1;
//        String value = element.getAttribute(attribute);
//        if(value == null)return -1;
//        return Integer.parseInt(value);
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
//            String id = url + "," + crawlingDate + "," + channel + "," + region + "," + checkInDay + "," + adultNum + "," + childNum + "," + index + "," + peopleType + System.currentTimeMillis();
//
//            String md5 = MessageDigestUtils
//                    .generateDigestByMessageDigestMode(MessageDigestMode.MD5, id);
//
//            logger.info(checkInDay + "\t" + adultNum + "\t" + childNum);
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
//        }catch (Exception e){
//            e.printStackTrace();
//            logger.error(e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        int priority = 15;
//
//        AbstractMap.SimpleEntry<String, String> channel = new AbstractMap.SimpleEntry<>("去哪儿-搜索", "QunarSearch");
////        QunarSearchItem item = new QunarSearchItem(priority,channel,"http://sqkq3.package.qunar.com/user/detail.jsp?id=4114149262","自由行","//sqkq3.package.qunar.com");
//        QunarSearchItem item = new QunarSearchItem(priority,channel,"http://dujia.qunar.com/pi/detail_16051908?vendor=%E6%87%92%E4%BA%BA%E5%BA%A6%E5%81%87&function=%E8%87%AA%E7%94%B1%E8%A1%8C&departure=%E4%B8%8A%E6%B5%B7&arrive=clubmed&ttsRouteType=&filterDate=2016-10-27,2016-10-27","携程自由行","package.qunar.com");
//
//        WebBrowser browser = new WebBrowser();
//        browser.init();
//
//        item.capture(browser);
//    }
//}
