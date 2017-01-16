package com.sunx.moudle.channel.aoyou;

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

/**
 * 遨游的数据采集
 */
@Service(id = 14, service = "com.fosun.fonova.moudle.channel.aoyou.AoYouSearchItem")
public class AoYouSearchItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(AoYouSearchItem.class);
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
        try{
            logger.info("开始处理数据:" + task.getUrl());
            //请求页面数据
            driver.get(task.getUrl());
            Wait.wait(driver,5, 1, () -> true);

            //根据不同的类型,自助游还是自由行还是酒店+景点来进行数据的分离处理
            deal(driver,factory,task);
            return Constant.TASK_SUCESS;
        }catch (Exception e){
            logger.error(e.getMessage());
            return Constant.TASK_FAIL;
        }
    }

    /**
     * 任务集合
     * @param driver
     * @param factory
     */
    private void deal(RemoteWebDriver driver,DBFactory factory,TaskEntity task){
        try{
            //找到点击日期
            List<WebElement> eles = driver.findElements(By.xpath("//table[contains(@class,'calendar-table')]//a[@href='#bookdiv']"));
            //对找到的日期进行判定
            if(eles == null || eles.size() <= 0){
                logger.error("解析日期数据出现错误,链接为：" + driver.getCurrentUrl() + ",抽取的xpath为://table[contains(@class,'calendar-table')]//a[@href='#bookdiv']");
                return;
            }
            //循环遍历日期,加载入住日期的数据
            for(int i=0;i<eles.size();i++){
                WebElement ele = eles.get(i);

                String txt = ele.getText();
                if(txt.contains("售罄"))continue;

                //获取入住日期
                String checkInDay = null;
                try{
                    WebElement dayTag = ele.findElement(By.cssSelector("input[name=DepartDate]"));
                    if(dayTag != null){
                        checkInDay = dayTag.getAttribute("value");
                    }
                }catch (Exception e){}
                if(checkInDay != null){
                    checkInDay = checkInDay.replaceAll("[^0-9]","");
                }

                //开始点击日期
                ele.click();

                //等待数据渲染完成
                try{
                    Wait.wait(driver,5, 5, () -> {
                        List<WebElement> findElements = driver.findElements(By.cssSelector(".panel-left"));
                        return !findElements.isEmpty();
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
                //进入到页面,首先加载的就是2个成人0个儿童的数据,所以现在截图,并保存数据到数据库
                    save(factory,task,driver,checkInDay,2,0,"" + i,"2成人");

                    //修改儿童和成人的个数
                    //人员类型修改为：2成人1儿童
                    change(factory,task,driver,
                            "//input[@id='selectChildNum']",
                            "//div[@class='downbox selectperson']//a[@class='listnum list1']",
                            "//div[@id='bookSelect']//a[@class='btn-confirm ml10']",
                            ".panel-left",
                            null,
                            checkInDay, 2,1,"2成人1儿童","" + i,true);

                    //修改成人为1个成人1个儿童
                    change(factory,task,driver,
                            "//input[@id='selectAdultNum']",
                            "//div[@class='downbox selectperson']//a[contains(@class,'listnum list1')]",
                            "//div[@id='bookSelect']//a[@class='btn-confirm ml10']",
                            ".panel-left",
                            null,
                            checkInDay, 1,1,"1成人1儿童","" + i,true);

                //js 修改为2 1
                //修改下拉菜单的显示
                driver.executeScript("document.getElementById('selectChildNum').value ='0';");
                driver.executeScript("document.getElementById('selectAdultNum').value ='2';");
                //人员类型修改为：2成人0儿童
                change(factory,task,driver,
                        "//input[@id='selectChildNum']",
                        "//div[@class='downbox selectperson']//a[@class='listnum list0']",
                        "//div[@id='bookSelect']//a[@class='btn-confirm ml10']",
                        ".panel-left",
                        null,
                        checkInDay, 2,0,"2成人","" + i,false);
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    /**
     * 修改数据,并截图
     * @param factory
     * @param driver
     * @param adultNum
     * @param childNum
     */
    public void change(DBFactory factory,TaskEntity task,RemoteWebDriver driver,String clickXpath,String selectXpath,String submit,String waitCss,String showAllCss,String checkInDay,int adultNum,int childNum,String peopleType,String index,boolean flag){
        try{
            WebElement tag = driver.findElementByXPath(clickXpath);
            if(tag == null && flag){
                save(factory,task,driver,checkInDay,adultNum,childNum,"33",peopleType);
            }else{
                while(true){
                    //点击这个标签
                    tag.click();

                    tag.sendKeys();
                    //等待浏览器渲染
                    try{
                        Wait.wait(driver,2, 5, () -> {
                            List<WebElement> findElements = driver.findElements(By.xpath(selectXpath));
                            return !findElements.isEmpty();
                        });
                        break;
                    }catch (Exception e){}
                }
                //修改下拉菜单的显示
                driver.executeScript("document.getElementById('selectChildNum').parentNode.children[1].style.display ='';");
                driver.executeScript("document.getElementById('selectAdultNum').parentNode.children[1].style.display ='';");

                //查找对应的选择项
                WebElement select = driver.findElement(By.xpath(selectXpath));
                if(select == null&& flag){
                    save(factory,task,driver,checkInDay,adultNum,childNum,"44",peopleType);
                    return;
                }
                //选择数据进行点击
                select.click();
                //判断是自动加载还是需要手动提交
                if(submit != null && submit.length() > 0){
                    //等待浏览器渲染
                    try{
                        Wait.wait(driver,3, 5, () -> {
                            List<WebElement> findElements = driver.findElements(By.xpath(submit));
                            return !findElements.isEmpty();
                        });
                    }catch (Exception e){}

                    WebElement sub = driver.findElement(By.xpath(submit));
                    if(sub != null){
                        sub.click();
                    }
                }
                //等待点击结果加载渲染完毕
                try{
                    Wait.wait(driver,5, 5, () -> {
                        List<WebElement> findElements = driver.findElements(By.cssSelector(waitCss));
                        return !findElements.isEmpty();
                    });
                }catch (Exception e){}
                if(flag){
                    //截图保存数据
                    save(factory,task,driver,checkInDay,adultNum,childNum,index,peopleType);
                }
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    /**
     * 截图
     * @param driver
     */
    public void save(DBFactory factory,TaskEntity task,RemoteWebDriver driver,String checkInDay,int adultNum,int childNum,String index,String peopleType){
        try{
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = "Unknown";
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() +
                    "," + task.getUrl() + "," + task.getPeopleType();
            String md5 = MD5.md5(id);

            //保存截图
            byte[] screen = FileUtil.getScreenshot(driver);
            String imgPath = FileUtil.createPageFile(now,task.getChannelName(), region,checkInDay, md5, ImageType.PNG);
            FileUtils.writeByteArrayToFile(new File(imgPath), screen);

            //保存网页源码
            String filePath = FileUtil.createPageFile(now,task.getChannelName(), region,checkInDay, md5, ImageType.TXT);
            String pageSource = driver.getPageSource();
            FileUtils.writeStringToFile(new File(filePath), pageSource, "UTF-8");

            //封装结果数据
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate());
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setPeopleType(task.getPeopleType());
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(now);
            resultEntity.setPath(filePath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
