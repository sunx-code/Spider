package com.sunx.moudle.channel.ctrip;

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

@Service(id = 3, service = "com.fosun.fonova.moudle.channel.ctrip.CTripExecutableItem")
public class CTripExecutableItem implements IParser {
    private static final Logger logger = LoggerFactory.getLogger(CTripExecutableItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
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
            pageDriver.get(task.getUrl());
            Wait.wait(pageDriver, 5, 4, () -> true);

            //判断是否需要进行店家展开更多房型
            String source = pageDriver.getPageSource();
            if(source != null && source.contains("展开全部房型")){
                try{
                    List<WebElement> eles = pageDriver.findElements(By.cssSelector(".J_ExpandLeftRoom.show_unfold"));
                    if(eles != null && eles.size() > 0){
                        for(WebElement ele : eles){
                            ele.click();

                            Wait.wait(pageDriver,1,1,() -> true);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);

            String id = vday + "," + task.getChannelName() + "," + task.getRegion() + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getSleep();
            String md5 = MD5.md5(id);

            byte[] screenshotAs = FileUtil.getScreenshot(pageDriver);
            String imgPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), task.getCheckInDate(), md5, ImageType.PNG);
            FileUtils.writeByteArrayToFile(new File(imgPath), screenshotAs);

            String txtPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), task.getCheckInDate(), md5, ImageType.TXT);
            FileUtils.writeStringToFile(new File(txtPath), source, "UTF-8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate());
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setRegion(task.getRegion());
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(txtPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);

            return Constant.TASK_SUCESS;
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return Constant.TASK_FAIL;
        }
    }
}
