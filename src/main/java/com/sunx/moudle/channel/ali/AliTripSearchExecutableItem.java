package com.sunx.moudle.channel.ali;

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

@Service(id = 7, service = "com.fosun.fonova.moudle.channel.ali.AliTripSearch")
public class AliTripSearchExecutableItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(AliTripSearchExecutableItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat fs = new SimpleDateFormat("yyyyMMdd");

    /**
     * 开始解析数据
     *
     * @param driver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        //获取最大的日期
        String max = fs.format(new Date(System.currentTimeMillis() + 86400l * 31 * 1000));
        logger.info("max:" + max);
        try {
            //开始抓取链接,记录日志...
            logger.info("开始抓取数据,对应的链接地址为:" + task.getUrl() + "....");
            driver.get(task.getUrl());
            Wait.wait(driver, 10, 1, () -> true);
            // ==========房型=============================
            Map<String, WebElement> day_month_elements = new LinkedHashMap<>();
            Map<String, WebElement> day_day_elements = new LinkedHashMap<>();
            // ==========提取类型=============================
            {
                WebElement month_list_findElementByXPath = driver
                        .findElementByXPath("//*[@id=\"J_PropCalendar\"]/div[1]/div[2]");
                List<WebElement> month_list_elements = month_list_findElementByXPath
                        .findElements(By.cssSelector(".J_MonthTab"));

                WebElement date_wrap_findElementByXPath = driver
                        .findElementByXPath("//*[@id=\"J_PropCalendar\"]/div[2]");

                for (int i = 0, j = month_list_elements.size(); i < j && i < 2; i++) {
                    WebElement month = month_list_elements.get(i);
                    month.click();
                    // ------------
                    Wait.wait(driver, 1, 1, () -> true);
                    // ------------
                    List<WebElement> date_wrap_elements = date_wrap_findElementByXPath
                            .findElements(By.cssSelector(".date-cell.J_DateCell.date-cell-active"));

                    for (WebElement date_wrap_element : date_wrap_elements) {
                        String day = date_wrap_element.getAttribute("data-datetime");

                        //判断日期是否是我们需要的数据
                        try {
                            int cid = Integer.parseInt(day.replaceAll("-", ""));
                            int maxDay = Integer.parseInt(max);
                            //超过最大的日期,数据丢弃掉
                            if (cid > maxDay) {
                                System.out.println("当前日期超过需求范围,当前日期为:" + day);
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        System.err.println(day);
                        day_month_elements.put(day, month);
                        day_day_elements.put(day, date_wrap_element);
                    }
                }
            }
            Map<String, WebElement> housetype_elements = new LinkedHashMap<>();
            {
                WebElement housetype_findElementByXPath = driver
                        .findElementByXPath("//*[@id=\"J_SkuWrap\"]/dd[1]/ul");
                List<WebElement> housetype_findElements = housetype_findElementByXPath
                        .findElements(By.cssSelector("li"));
                for (WebElement housetype_ : housetype_findElements) {
                    String housetype_text = housetype_.getText();
                    System.err.println(housetype_text);
                    housetype_elements.put(housetype_.getText(), housetype_);
                }
            }
            // ==========人员类型=============================
            Map<String, WebElement> peopletype_elements = new LinkedHashMap<>();
            {
                WebElement peopletype_findElementByXPath = driver
                        .findElementByXPath("//*[@id=\"J_SkuWrap\"]/dd[2]/ul");
                List<WebElement> peopletype_findElements = peopletype_findElementByXPath
                        .findElements(By.cssSelector("li"));
                for (WebElement peopletype_ : peopletype_findElements) {
                    String peopletype_text = peopletype_.getText();
                    System.err.println(peopletype_text);
                    peopletype_elements.put(peopletype_text, peopletype_);
                }
            }
            // ===================================
            dealNew(driver, day_month_elements, day_day_elements, housetype_elements, peopletype_elements, task, factory);
            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return Constant.TASK_FAIL;
        }
    }

    /**
     * 处理数据
     *
     * @param pageDriver
     * @param day_month_elements
     * @param day_day_elements
     * @param housetype_elements
     * @param peopletype_elements
     */
    private void dealNew(RemoteWebDriver pageDriver,
                         Map<String, WebElement> day_month_elements,
                         Map<String, WebElement> day_day_elements,
                         Map<String, WebElement> housetype_elements,
                         Map<String, WebElement> peopletype_elements,
                         TaskEntity task,
                         DBFactory factory) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);

        //遍历日期集合
        for (String check_in_date : days) {
            WebElement month_webElement = day_month_elements.get(check_in_date);
            if (month_webElement == null) continue;

            String monthClz = month_webElement.getAttribute("class");
            if (monthClz != null && !monthClz.contains("active-month")) {
                month_webElement.click();
            }
            Wait.wait(pageDriver, 1, 1, () -> true);
            WebElement day_element = day_day_elements.get(check_in_date);
            //如果当前标签为未选中的状态,则执行点击
            day_element.click();

            for (Entry<String, WebElement> housetype_element : housetype_elements.entrySet()) {
                String housetype = housetype_element.getKey();
                WebElement houseType = housetype_element.getValue();
                //处理动态人员类型的数据变动
                String clz = houseType.getAttribute("class");
                logger.info("获取到的房型标签的class为:" + clz + ",当前的入住日期为:" + check_in_date);
                if (clz == null || clz.contains("disable")) {
                    //如果为空,或则是不显示,则跳过这个选项
                    logger.info("房型类型不可选,class = " + clz);
                    continue;
                } else {
                    //如果当前标签为未选中的状态,则执行点击
                    houseType.click();
                }

                for (Entry<String, WebElement> peopletype_element : peopletype_elements.entrySet()) {
                    String peopletype = peopletype_element.getKey();

                    System.out.println("开始处理数据:" + task.getChannelName() + "\t" + housetype + "\t" + peopletype + "\t" + check_in_date + " ....");

                    //处理动态人员类型的数据变动
                    WebElement people = peopletype_element.getValue();
                    String peopleClz = people.getAttribute("class");
                    if (peopleClz == null || peopleClz.contains("disable")) {
                        //如果为空,或则是不显示,则跳过这个选项
                        System.out.println("人员类型不可选,class = " + peopleClz);
                        continue;
                    } else if (!peopleClz.contains("selected")) {
                        //如果当前标签为未选中的状态,则执行点击
                        people.click();
                    }

                    Wait.wait(pageDriver, 1, 1, () -> true);
                    //截图保存数据
                    save(factory, pageDriver, task, check_in_date, peopletype, housetype);
                    //取消出行人群
                    people.click();
                }
                //取消房型的选择
                houseType.click();
            }
        }
    }

    /**
     * @param driver
     * @param check_in_date
     * @param people
     * @param house
     * @throws Exception
     */
    private void save(DBFactory factory, RemoteWebDriver driver, TaskEntity task, String check_in_date, String people, String house) throws Exception {
        Date date = new Date();
        String vday = fs.format(date);
        String now = sdf.format(date);
        String region = "Unknown";
        String id = vday + "," + task.getChannelName() + "," + region + "," + check_in_date + "," + task.getUrl() + "," + people + "," + house;
        String md5 = MD5.md5(id);

        byte[] screenshotAs = FileUtil.getScreenshot(driver);
        String imgPath = FileUtil.createPageFile(vday, task.getChannelName(), region, check_in_date, md5, ImageType.PNG);
        FileUtils.writeByteArrayToFile(new File(imgPath), screenshotAs);

        String txtPath = FileUtil.createPageFile(vday, task.getChannelName(), region, check_in_date, md5, ImageType.TXT);
        String pageSource = driver.getPageSource();
        FileUtils.writeStringToFile(new File(txtPath), pageSource, "UTF8");

        // ===================================
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setId(md5);
        resultEntity.setCheckInDate(check_in_date);
        resultEntity.setChannelName(task.getChannelName());
        resultEntity.setHouseType(house);
        resultEntity.setPeopleType(people);
        resultEntity.setRegion(region);
        resultEntity.setTid(task.getId());
        resultEntity.setUrl(task.getUrl());
        resultEntity.setVday(now);
        resultEntity.setPath(txtPath);

        factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
    }
}
