package com.sunx.moudle.channel.wowoyoo;

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
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 *
 */
@Service(id = 2, service = "com.fosun.fonova.moudle.channel.wowoyoo.WowoYooSearchItem")
public class WowoYooSearchItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(WowoYooSearchItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 开始解析数据
     *
     * @param driver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        logger.info("开始访问地区首页:" + task.getUrl());
        //访问地点的首页数据
        driver.get(task.getUrl());
        //等待页面渲染完毕
        Wait.wait(driver,5, 5, () -> true);

        //找到相应的位置,进行填写数据后模拟点击
        return dealData(factory,driver,task);
    }

    /**
     * 开始处理数据
     * @param factory
     * @param driver
     * @param task
     */
    private int dealData(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        try {
            //选择入住天数
            Select vsleep = new Select(driver.findElement(By.xpath("//select[@name='day_num']")));
            vsleep.selectByValue(task.getSleep() + "");
            //入住日期
            driver.executeScript("$('.control_input_date').val('" + task.getCheckInDate() + "');");

            //入住成人数
            Select vadultNum = new Select(driver.findElement(By.xpath("//select[@name='adlut_num']")));
            vadultNum.selectByValue(task.getAdultNum() + "");

            if (task.getChildNum() > 0) {
                driver.executeScript("$('.clubmed_order_form').append('<input type=\"text\" name=\"kid_olds[0]\" value=\"" + task.getBirthday() + "\"/>');");

                //等待渲染完毕
                Wait.wait(driver,1, 1, () -> true);
            }
            //提交表单
            driver.executeScript("$('.clubmed_order_form').submit();");

            //等待页面渲染完毕
            Wait.wait(driver,10, 5, () -> true);
            //截图存储
            save(factory,driver,task);

            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 保存数据到数据库中
     * @param factory
     * @param driver
     * @param task
     */
    private void save(DBFactory factory, RemoteWebDriver driver, TaskEntity task) {
        try {
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getPeopleType() + "," + task.getSleep();
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
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(task.getPeopleType());
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(txtPath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
