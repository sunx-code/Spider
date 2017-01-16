package com.sunx.moudle.channel.caissa;

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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service(id = 4, service = "com.fosun.fonova.moudle.channel.caissa.CaiSaSearchItem")
public class CaiSaSearchItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(CaiSaSearchItem.class);
    //格式化日期
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    /**
     * 开始解析数据
     *
     * @param driver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        try {
            logger.info("请求页面数据...");
            //请求页面数据
            driver.get(task.getUrl());
            //等待浏览器渲染
            logger.info("等待浏览器渲染...");
            try {
                Wait.wait(driver, 10, 5, () -> {
                    List<WebElement> findElements = driver.findElements(By.cssSelector(".hotel_xinx"));
                    return !findElements.isEmpty();
                });
            } catch (Exception e) {
            }
            logger.info("截图该网页,保存数据到数据库中...");
            //截图该网页,保存数据到数据库中
            save(driver, task, factory);
            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Constant.TASK_FAIL;
        }
    }

    /**
     * 截图
     *
     * @param driver
     */
    public void save(RemoteWebDriver driver, TaskEntity task ,DBFactory factory) {
        try {
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = "Unknown";
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() +
                    "," + task.getUrl() + "," + task.getPeopleType();
            String md5 = MD5.md5(id);

            byte[] screenshotAs = FileUtil.getScreenshot(driver);
            String imgPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.PNG);
            FileUtils.writeByteArrayToFile(new File(imgPath), screenshotAs);

            String txtPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.TXT);
            String pageSource = driver.getPageSource();
            FileUtils.writeStringToFile(new File(txtPath), pageSource, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate());
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setPeopleType(task.getPeopleType());
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(txtPath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

}
