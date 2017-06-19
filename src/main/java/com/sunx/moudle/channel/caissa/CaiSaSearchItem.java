package com.sunx.moudle.channel.caissa;

import com.sunx.constant.Constant;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.enums.ImageType;
import com.sunx.utils.FileUtil;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import com.sunx.utils.Helper;
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

    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();

    public CaiSaSearchItem(){
        site.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        site.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8");

        site.setTimeOut(1000 * 10);
    }

    /**
     * 开始解析数据
     *
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        try {
            logger.info("请求页面数据...");
            //请求页面数据
            String html = Helper.downlaoder(downloader,request.setUrl(task.getUrl()),site,false);
            logger.info("截图该网页,保存数据到数据库中...");
            //截图该网页,保存数据到数据库中
            toSnapshot(factory,task,html);
            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Constant.TASK_FAIL;
        }
    }

    /**
     * 保存快照内容
     * @param factory
     * @param task
     * @param html
     */
    public void toSnapshot(DBFactory factory,TaskEntity task,String html){
        try{
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String id = vday + "," + task.getChannelName() + "," + task.getRegion() + "," + task.getCheckInDate() +
                    "," + task.getUrl() + "," + task.getAdultNum() + "," + task.getChildNum();
            String md5 = MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), html, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate().replaceAll("-",""));
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(Helper.people(task.getAdultNum(),task.getChildNum()));
            resultEntity.setRegion(task.getRegion());
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static  void main(String[] args){
        CaiSaSearchItem search = new CaiSaSearchItem();

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUrl("http://dj.caissa.com.cn/reserve/reserve.php?pro_id=FIT0005576&pid=192901&nomal=2017-06&_type=&child=1&adults=2&childrens=1&ages=0-11&kc=999&presaleNum=&last_time=20170622&last_p=2&start_date=20170627");
        taskEntity.setId(123l);
        taskEntity.setCheckInDate("20170627");
        taskEntity.setAdultNum(2);
        taskEntity.setChildNum(1);
        taskEntity.setRegion(Constant.DEFALUT_REGION);
        taskEntity.setPeopleType("2成人1儿童");
        taskEntity.setChannelName("凯撒");

        search.parser(null,taskEntity);
    }
}
