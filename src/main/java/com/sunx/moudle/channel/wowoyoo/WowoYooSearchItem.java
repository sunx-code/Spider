package com.sunx.moudle.channel.wowoyoo;

import com.sunx.common.encrypt.MD5;
import com.sunx.constant.Constant;
import com.sunx.downloader.*;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.enums.ImageType;
import com.sunx.storage.DBFactory;
import com.sunx.utils.FileUtil;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 */
@Service(id = 2, service = "com.sunx.moudle.channel.wowoyoo.WowoYooSearchItem")
public class WowoYooSearchItem implements IParser {
    private static final Logger logger = LoggerFactory.getLogger(WowoYooSearchItem.class);

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();

    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 开始解析数据
     *
     * @param factory
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        //第一步,拼接请求参数
        Map<String,String> map = new HashMap<>();
        map.put("area",task.getRegion());
        map.put("day_num",task.getSleep() + "");
        map.put("date",task.getCheckInDate());
        map.put("adlut_num",task.getAdultNum() + "");
        if(task.getChildNum() > 0){
            map.put("kid_olds[0]",task.getBirthday());
        }
        //请求头
        site.addHeader("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("accept-encoding","gzip, deflate, sdch, br");
        site.addHeader("Content-Type","application/x-www-form-urlencoded");
        site.addHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");

        logger.info("开始访问地区首页:" + task.getRegion());
        //访问地点的首页数据
        String src = downloader.downloader(request.setUrl(task.getUrl()).setMethod(Method.POST).setPostData(map),site.setTimeOut(10000));
        //找到相应的位置,进行填写数据后模拟点击
        return dealData(src,factory,task);
    }

    private int dealData(String src,DBFactory factory,TaskEntity task) {
        //对数据进行判定
        if(src == null || src.length() <= 0){
            logger.error("下载出现错误,地区：" + task.getRegion() +",成人数:" + task.getAdultNum() +",类型:" + task.getPeopleType() + ",入住日期:" + task.getCheckInDate());
            return -1;
        }
        //将网页源码中的图片地址,以及css地址,js地址补全
        String html = all(src);
        //补全网页后,讲数据保存
        save(html,factory,task);

        return 1;
    }

    /**
     * 将网页中的css路径地址以及js地址补全
     * @param src
     */
    public String all(String src){
        //开始替换
        try{
            //替换网页内容
            src = src.replaceAll("href=\"/","href=\"https://wowoyoo.com/");
            src = src.replaceAll("src=\"/","src=\"https://wowoyoo.com/");
        }catch (Exception e){
            e.printStackTrace();
        }
        return src;
    }

    /**
     * @param task
     */
    private void save(String src,DBFactory factory, TaskEntity task) {
        try {
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getPeopleType() + "," + task.getSleep();
            String md5 = MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), src, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate().replaceAll("-",""));
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(task.getPeopleType());
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);
            resultEntity.setSleep(task.getSleep());
            resultEntity.setAdultNum(task.getAdultNum());
            resultEntity.setChildNum(task.getChildNum());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setAdultNum(2);
        taskEntity.setChildNum(0);
        taskEntity.setChannelId(2);
        taskEntity.setCheckInDate("2017-05-03");
        taskEntity.setPeopleType("2成人");
        taskEntity.setRegion("卡尼岛");
        taskEntity.setSleep(2);
        taskEntity.setUrl("https://wowoyoo.com/clubmed/cau");

        new WowoYooSearchItem().parser(null,taskEntity);
    }
}
