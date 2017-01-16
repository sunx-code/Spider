package com.sunx.moudle.channel.lvmama;

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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 *
 */
@Service(id = 10, service = "com.fosun.fonova.moudle.channel.lvmama.LvMamaExecutable")
public class LvMamaExecutable implements IParser {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(LvMamaExecutable.class);
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
        //开始抓取网页
        driver.get(task.getUrl());
        //等待一定时间后进行后续的加载
        Wait.wait(driver,5, 10, () -> true);

        switch (task.getType()){
            case "自由行":
                try{
                    return dealFreeData(factory,driver,task);//表示自由行
                }catch (Exception e){
                    e.printStackTrace();
                }
            case "当地游":
                try{
                    return dealLoaclData(factory,driver,task);//表示当地游
                }catch (Exception e){
                    e.printStackTrace();
                }
            case "机票+酒店":
                try{
                   return dealJiPiaoData(factory,driver,task);//表示机加酒
                }catch (Exception e){
                    e.printStackTrace();
                }
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 处理数据
     *
     * @param factory
     * @param driver
     * @param task
     */
    private int dealJiPiaoData(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        try {
            //抽取日期中含有价格的节点,用于点击加载数据
            List<WebElement> eles = driver.findElements(By.xpath("//table[@class='caltable']//td"));
            if (eles == null || eles.size() <= 0) {
                logger.error("网页加载异常...");
                return Constant.TASK_FAIL;
            }
            //遍历每个td
            for (int i = 0; i < eles.size(); i++) {
                WebElement td = eles.get(i);
                //获取文本,进行判断
                String txt = td.getText();
                if (txt == null || txt.length() <= 0 || !txt.contains("起")) continue;
                //点击选中,加载数据
                WebElement ele = td.findElement(By.cssSelector(".caldate"));
                ele.click();

                //等待网页加载完成
                Wait.wait(driver,5, 5, () -> true);

                //获取入住日期
                String checkInDay = td.getAttribute("date-map");
                if (checkInDay != null) {
                    checkInDay = checkInDay.replaceAll("-", "");
                } else {
                    checkInDay = fs.format(new Date());
                }

                logger.info("开始处理日期：" + checkInDay + " 的数据....");

                //当加载完成后,选择成人和儿童的个数
                //1个成人,1个儿童
                select(factory,task,driver, null, checkInDay, 1, 1, "1成人1儿童", i);
                //2个成人,0个儿童
                select(factory,task,driver, null, checkInDay, 2, 0, "2成人", i);
                //2个成人,1个儿童
                select(factory,task, driver, null, checkInDay, 2, 1, "2成人1儿童", i);
            }
            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 处理的当地有数据
     * @param factory
     * @param driver
     * @param task
     */
    public int dealLoaclData(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        try {
            //获取房型
            String houseType = getType(driver);

            //抽取日期中含有价格的节点,用于点击加载数据
            List<WebElement> eles = driver.findElements(By.xpath("//table[@class='caltable']//td"));
            if (eles == null || eles.size() <= 0) {
                logger.error("网页加载异常...");
                return Constant.TASK_FAIL;
            }
            //遍历每个td
            for (int i = 0; i < eles.size(); i++) {
                WebElement td = eles.get(i);
                //获取文本,进行判断
                String txt = td.getText();
                if (txt == null || txt.length() <= 0 || !txt.contains("起")) continue;
                //点击选中,加载数据
                td.click();

                //等待网页加载完成
                Wait.wait(driver,5, 5, () -> true);

                //获取入住日期
                String checkInDay = td.getAttribute("data-date-map");
                if (checkInDay != null) {
                    checkInDay = checkInDay.replaceAll("-", "");
                } else {
                    checkInDay = fs.format(new Date());
                }

                logger.info("开始处理日期：" + checkInDay + " 的数据....");

                //当加载完成后,选择成人和儿童的个数

                //1个成人,1个儿童
                select(factory, task ,driver, houseType, checkInDay, 1, 1, "1成人1儿童", i);
                //2个成人,0个儿童
                select(factory, task , driver, houseType, checkInDay, 2, 0, "2成人", i);
                //2个成人,1个儿童
                select(factory, task ,driver, houseType, checkInDay, 2, 1, "2成人1儿童", i);
            }
            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 处理数据
     *
     * @param factory
     * @param task
     * @param driver
     * @param adult
     * @param child
     */
    public void select(DBFactory factory,TaskEntity task, WebDriver driver, String houseType, String checkInDay, int adult, int child, String group, int index) {
        try {
            ((JavascriptExecutor) driver).executeScript("document.getElementById('adult-count').style.display='block';");
            ((JavascriptExecutor) driver).executeScript("document.getElementById('children-count').style.display='block';");

            //选择大人
            Select select = new Select(driver.findElement(By.id("adult-count")));
            //重新选择
            select.selectByValue(adult + "");

            //选择小孩选项
            Select childSelect = new Select(driver.findElement(By.id("children-count")));
            //重新选择
            childSelect.selectByValue(child + "");

            //等待页面加载完
            Wait.wait(driver,5, 10, () -> true);

            //隐藏标签
            ((JavascriptExecutor) driver).executeScript("document.getElementById('adult-count').style.display='display: none;';");
            ((JavascriptExecutor) driver).executeScript("document.getElementById('children-count').style.display='display: none;';");

            //等待页面加载完
            Wait.wait(driver,1, 2, () -> true);

            //进行最后的截图和存储数据库
            save(factory,task,driver, houseType, checkInDay, adult, child, group, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取对应的房型
     *
     * @param driver
     * @return
     */
    public String getType(WebDriver driver) {
        String type = null;
        try {
            WebElement ele = driver.findElement(By.xpath("//span[@class='instance-travel-xc-info-pr20' and contains(text(),'房型')]"));
            if (ele != null) {
                type = ele.getText();
                if (type != null) {
                    type = type.replaceAll("房型：", "");
                }
            }
        } catch (Exception e) {
        }
        return type;
    }

    /**
     * 处理自由行数据
     * @param factory
     * @param driver
     * @param task
     */
    public int dealFreeData(DBFactory factory, RemoteWebDriver driver, TaskEntity task) throws Exception {
        //抽取日期中含有价格的节点,用于点击加载数据
        List<WebElement> eles = driver.findElements(By.xpath("//table[@class='caltable']//td"));
        if (eles == null || eles.size() <= 0) {
            logger.error("网页加载异常...");
            throw new Exception("网页加载异常...");
        }
        //遍历每个td
        for (int i = 0; i < eles.size(); i++) {
            try {
                WebElement td = eles.get(i);
                //获取文本,进行判断
                String txt = td.getText();
                if (txt == null || txt.length() <= 0 || !txt.contains("起")) continue;
                //点击选中,加载数据
                td.click();

                //等待网页加载完成
                Wait.wait(driver,5, 5, () -> true);

                //获取入住日期
                String checkInDay = td.getAttribute("data-date-map");
                if (checkInDay != null) {
                    checkInDay = checkInDay.replaceAll("-", "");
                } else {
                    checkInDay = fs.format(new Date());
                }

                //保存数据
                save(factory,task,driver, null, checkInDay, 2, 1, "2成人1儿童", i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Constant.TASK_SUCESS;
    }

    /**
     * 进行截图,保存网页源码
     * @param factory
     * @param task
     * @param driver
     * @param houseType
     * @param checkInDay
     * @param adult
     * @param child
     */
    public void save(DBFactory factory,TaskEntity task,WebDriver driver, String houseType, String checkInDay, int adult, int child, String group, int tmpId) {
        try {
            String region = "Unknown";
            String crawlingDate = fs.format(new Date());

            String id = crawlingDate + "," + task.getChannelName() + "," + region + "," + checkInDay + ","
                    + adult + "," + houseType + "," + child + "," + task.getUrl() + "," + tmpId;
           String md5 = MD5.md5(id);

            //保存网页源码
            String txtPath = FileUtil.createPageFile(crawlingDate, task.getChannelName(), region,checkInDay, md5, ImageType.TXT);
            String pageSource = driver.getPageSource();
            FileUtils.writeStringToFile(new File(txtPath), pageSource, "UTF8");

            //保存截图
            byte[] screen = FileUtil.getScreenshot((RemoteWebDriver) driver);
            String imgPath = FileUtil.createPageFile(crawlingDate, task.getChannelName(), region,checkInDay, md5, ImageType.PNG);
            FileUtils.writeByteArrayToFile(new File(imgPath), screen);

            //封装结果数据,将数据插入到数据库中
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(checkInDay);
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(houseType);
            resultEntity.setPeopleType(group);
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(sdf.format(new Date()));
            resultEntity.setPath(txtPath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
