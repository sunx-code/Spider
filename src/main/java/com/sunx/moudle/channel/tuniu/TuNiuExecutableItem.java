package com.sunx.moudle.channel.tuniu;

import com.sunx.constant.Constant;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.enums.ImageType;
import com.sunx.utils.FileUtil;
import com.sunx.utils.TimerUtils;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Service(id = 6, service = "com.fosun.fonova.moudle.channel.tuniu.TuNiuExecutableItem")
public class TuNiuExecutableItem implements IParser {
    private static final Logger logger = LoggerFactory.getLogger(TuNiuExecutableItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat fs = new SimpleDateFormat("yyyyMMdd");

    /**
     * 开始解析数据
     *
     * @param pageDriver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver pageDriver, TaskEntity task) {
        pageDriver.get(task.getUrl());
        try{
            Wait.wait(pageDriver,5, 30, () -> {
                List<WebElement> findElements = pageDriver.findElements(By.cssSelector(".book_title_cell.submit_btn"));
                for (WebElement webElement : findElements) {
                    if (StringUtils.containsIgnoreCase(webElement.getText(), "即预订")) {
                        return true;
                    }
                }
                return false;
            });
        }catch (Exception e){
            e.printStackTrace();
        }

        // =============================================
        Map<String, String> date_elements = new LinkedHashMap<>();
        List<WebElement> findElements_date = pageDriver.findElementsByCssSelector(".book_date_select option");
        for (WebElement webElement : findElements_date) {
            String date = webElement.getAttribute("value");
            date_elements.put(date, "$(\".book_date_select option[value='" + date + "']\").change()");
        }
        Map<String, String> types_map = new LinkedHashMap<>();

        String template = "$(\".num_select.adult_num option[value='#1']\").change();";
        template += "$(\".num_select.child_num option[value='#2']\").change();";

        types_map.put("2成人", template.replace("#1", "2").replace("#2", "0"));
        types_map.put("2成人1儿童", template.replace("#1", "2").replace("#2", "1"));
        types_map.put("1成人1儿童", template.replace("#1", "1").replace("#2", "1"));

        // =============================================
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
        // =============================================
        for (String checkInDate : days) {
            if (!date_elements.containsKey(checkInDate))continue;
            String date_script = date_elements.get(checkInDate);

            logger.info("开始处理数据 > " + task.getChannelName() + "\t" + checkInDate + " ...");
            for (Entry<String, String> entry : types_map.entrySet()) {
                try{
                    // =============================================
                    String peopletype = entry.getKey();
                    String script = entry.getValue();

                    pageDriver.executeScript(script);
                    Wait.wait(pageDriver,5, 10, () -> {
                        List<WebElement> findElements = pageDriver
                                .findElements(By.cssSelector(".book_title_cell.submit_btn"));
                        for (WebElement webElement : findElements) {
                            if (StringUtils.containsIgnoreCase(webElement.getText(), "即预订")
                                    || StringUtils.containsIgnoreCase(webElement.getText(), "日售")) {
                                return true;// 当日售罄
                            }
                        }
                        return false;
                    });

                    pageDriver.executeScript(date_script);
                    Wait.wait(pageDriver,8, 5, () -> {
                        List<WebElement> findElements = pageDriver
                                .findElements(By.cssSelector(".book_title_cell.submit_btn"));
                        for (WebElement webElement : findElements) {
                            if (StringUtils.containsIgnoreCase(webElement.getText(), "即预订")
                                    || StringUtils.containsIgnoreCase(webElement.getText(), "日售")) {
                                return true;// 当日售罄
                            }
                        }
                        return false;
                    });

                    //展开全部数据
                    try {
                        List<WebElement> eles = pageDriver.findElements(By.cssSelector(".T_show_all a"));
                        for (WebElement e : eles) {
                            e.click();

                            Wait.wait(pageDriver,2, 2, () -> true);
                        }
                    } catch (Exception e) {}


                    Date date = new Date();
                    String vday = fs.format(date);
                    String now = sdf.format(date);

                    String id = vday + "," + task.getChannelName() + "," + task.getRegion() + "," + checkInDate + "," + task.getUrl() + "," + peopletype;
                    String md5 = MD5.md5(id);

                    // ---------------
                    byte[] screenshotAs = FileUtil.getScreenshot(pageDriver);
                    String imgPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), checkInDate,md5, ImageType.PNG);
                    FileUtils.writeByteArrayToFile(new File(imgPath), screenshotAs);

                    String txtPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), checkInDate,md5, ImageType.TXT);
                    FileUtils.writeStringToFile(new File(txtPath), pageDriver.getPageSource(), "UTF8");

                    // ===================================
                    ResultEntity resultEntity = new ResultEntity();
                    resultEntity.setId(md5);
                    resultEntity.setCheckInDate(checkInDate);
                    resultEntity.setChannelName(task.getChannelName());
                    resultEntity.setRegion(task.getRegion());
                    resultEntity.setTid(task.getId());
                    resultEntity.setUrl(task.getUrl());
                    resultEntity.setVday(now);
                    resultEntity.setPath(txtPath);

                    factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
                    // ===================================
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return Constant.TASK_SUCESS;
    }
}
