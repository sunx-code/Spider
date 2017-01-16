package com.sunx.moudle.channel.qunar;

import com.sunx.constant.Constant;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.enums.ImageType;
import com.sunx.utils.FileUtil;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Service(id = 5, service = "com.fosun.fonova.moudle.channel.qunar.QunarExecutableItem")
public class QunarExecutableItem implements IParser {
    private static final Logger logger = LoggerFactory.getLogger(QunarExecutableItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 开始解析数据
     *
     * @param pageDriver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver pageDriver, TaskEntity task) {
        try{
            logger.info("开始处理数据:" + task.getUrl());
            pageDriver.get(task.getUrl());
            Wait.wait(pageDriver, 3, 60, () -> {
                List<WebElement> findElements = pageDriver.findElements(By.className("b_loading"));
                for (WebElement webElement : findElements) {
                    String attribute = webElement.getAttribute("style");
                    if (StringUtils.containsIgnoreCase(attribute, "none")) {
                        return true;
                    }
                }
                return false;
            });
            // ---------------
            List<WebElement> findElements2 = pageDriver.findElements(By.cssSelector(".more-room-wraper-ct"));
            for (WebElement webElement : findElements2) {
                webElement.click();
                Wait.wait(pageDriver, 1, 1, () -> true);
            }
            List<WebElement> findElements = pageDriver.findElements(By.cssSelector(".btn-book-ct"));
            for (WebElement webElement : findElements) {
                webElement.click();
                Wait.wait(pageDriver, 1, 1, () -> true);
            }

            String pageSource = pageDriver.getPageSource();
            logger.info("*****************************************************************");
            //遍历整个集合,监测是否有数据被隐藏
            try {
                List<WebElement> eles = pageDriver.findElements(By.xpath("//td[@class='e2']//div[@class='promote']"));
                List<String> result = new ArrayList<String>();
                if (eles != null || eles.size() > 0) {
                    dealParse(pageDriver, eles, result);
                }
                logger.info("当前集合中的数据量有：" + result.size());

                //综合处理结果数据
                pageSource = dealSource(result, pageSource);
            } catch (Exception e) {
            }
            logger.info("*****************************************************************");

            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);

            String id = vday + "," + task.getChannelName() + "," + task.getRegion() + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getSleep();
            String md5 = MD5.md5(id);

            byte[] screenshotAs = FileUtil.getScreenshot(pageDriver);
            String imgPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), task.getCheckInDate(), md5, ImageType.PNG);
            FileUtils.writeByteArrayToFile(new File(imgPath), screenshotAs);

            String txtPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), task.getCheckInDate(), md5, ImageType.TXT);
            FileUtils.writeStringToFile(new File(txtPath), pageSource, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate());
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setRegion(task.getRegion());
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(now);
            resultEntity.setPath(txtPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
            // ===================================
            return Constant.TASK_SUCESS;
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 构造网页源码
     *
     * @param result
     * @param pageSource
     * @return
     */
    public String dealSource(List<String> result, String pageSource) {
        if (result == null || result.size() <= 0) return pageSource;
        StringBuffer buffer = new StringBuffer();
        for (String input : result) {
            buffer.append("\n" + input);
        }
        return pageSource.replaceAll("</html>", buffer.toString() + "</html>");
    }

    /**
     * @param driver
     * @param eles
     * @param result
     */
    public void dealParse(RemoteWebDriver driver, List<WebElement> eles, List<String> result) {
        try {
            logger.info("抽取到的tr标签数为:" + eles.size());
            for (int i = 0; i < eles.size(); i++) {
                WebElement ele = eles.get(i);

                //抽取标签文本,进行判定
                String text = ele.getText();
                if (text != null && text.contains("...")) {
                    try {
                        Actions builder = new Actions(driver);
                        builder.moveToElement(ele).build().perform();

                        //等待渲染完成
                        Wait.wait(driver, 2, 2, () -> true);
                    } catch (Exception e) {
                    }

                    //抽取文本结构
                    WebElement e = driver.findElement(By.xpath("//div[@class='m-txt-pop logotip']"));
                    String value = e.getText();
                    result.add("<input class='self-tag-for-elipsis' value='" + value + "'/>");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    ////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///
//    ///    采用api的方式来进行数据抓取
//    ///
//    ////////////////////////////////////////////////////////////////////////////////////////////////////
//    private void dealParse(String url,List<WebElement> eles, List<String> result) {
//        //从链接中抽取两个参数
//        String required0 = find(url, cityUrl);
//        String hotelSEQ = find(url, seq);
//
//        for (WebElement ele : eles) {
//            try {
//                logger.info("线程休眠1.5秒以后进行...数据的补全....");
//                //线程休眠1.5s以后进行处理数据
//                Thread.sleep(1500);
//
//                //期初的数据
//                String source = ele.getAttribute("onclick");
//                if (source == null || source.length() <= 0) continue;
//
//                String data = clean(source, "window.open\\('", "','_blank'\\);");
//                //拼接Booking_Main.jsp
//                String codeBase = find(data, cbcp);
//
//                String link = "http://hotel.qunar.com" + data.replaceAll("booking.jsp", "Booking_Main.jsp") + "&required0=" + required0 + "&hotelSEQ=" + hotelSEQ + "&codeBase=" + codeBase;
//
//                String src = downloader.downloader(new Request(link), site);
//                //对数据进行判定
//                if (src == null || src.length() <= 100) continue;
//
//                Page page = Page.me().bind(src);
//                //下载到数据了,需要解析当前网页,根据解析到的数据,拼接post请求,请求json结果数据
//                String bksign = page.css("input[name=bksign]", "value");
//                String productUniqKey = page.css("input[name=productUniqKey]", "value");
//                String bkts = page.css("input[name=bkts]", "value");
//                String inventoryType = page.css("input[name=inventoryType]", "value");
//                String taxes = page.css("input[name=taxes]", "value");
//                String reduce = page.css("input[name=reduce]", "value");
//                String cashback = page.css("input[name=cashback]", "value");
//                String priceCut = page.css("input[name=priceCut]", "value");
//                String from = page.css("input[name=from]", "value");
//                String productType = page.css("input[name=productType]", "value");
//                String price = page.css("input[name=price]", "value");
//                String cityUrl = page.css("input[name=cityUrl]", "value");
//                String bd_sign = page.css("input[name=bd_sign]", "value");
//                String bd_ssid = page.css("input[name=bd_ssid]", "value");
//                String QHFP = page.css("input[name=QHFP]", "value");
//                String checkOutDate = page.css("input[name=checkOutDate]", "value");
//                String checkInDate = page.css("input[name=checkInDate]", "value");
//                String productId = page.css("input[name=productId]", "value");
//                String hotelSeq = page.css("input[name=hotelSeq]", "value");
//                String wrapperId = page.css("input[name=wrapperId]", "value");
//
//                String query = "https://qta.qunar.com/user/order/data/www/price/query?"
//                        + "wrapperId=" + wrapperId + "&hotelSeq=" + hotelSeq + "&productId=" + productId + "&checkInDate=" + checkInDate
//                        + "&checkOutDate=" + checkOutDate + "&QHFP=" + QHFP + "&bd_ssid=" + bd_ssid + "&bd_sign=" + bd_sign + "&cityUrl=" + cityUrl
//                        + "&price=" + price + "&productType=" + productType + "&from=" + from + "&priceCut=" + priceCut + "&cashback=" + cashback + "&reduce=" + reduce
//                        + "&taxes=" + taxes + "&inventoryType=" + inventoryType + "&bkts=" + bkts + "&bksign=" + bksign + "&productUniqKey=" + productUniqKey
//                        + "&sleepTask=&_=" + System.currentTimeMillis();
//
//                //打印链接地址
//                String json = downloader.downloader(new Request(query), site);
//                //对数据进行判断
//                if (json == null) continue;
//                JSONObject bean = JSON.parseObject(json);
//                if (bean == null) continue;
//                JSONObject obj = bean.containsKey("data") ? bean.getJSONObject("data") : null;
//                if (obj == null) continue;
//                String productRoomName = obj.containsKey("productRoomName") ? obj.getString("productRoomName") : null;
//                if (productRoomName != null) {
//                    result.add(productRoomName);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private String find(String str, Pattern pattern) {
//        Matcher matcher = pattern.matcher(str);
//        if (matcher.find()) return matcher.group(1);
//        return matcher.group();
//    }
//
//    private String clean(String str, String... regex) {
//        if (regex == null || regex.length <= 0) return str;
//        for (String s : regex) {
//            str = str.replaceAll(s, "");
//        }
//        return str;
//    }
}
