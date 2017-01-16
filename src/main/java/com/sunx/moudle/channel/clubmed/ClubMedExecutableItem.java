package com.sunx.moudle.channel.clubmed;

import com.sunx.constant.Constant;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.enums.ImageType;
import com.sunx.utils.FileUtil;
import com.sunx.common.encrypt.MD5;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.parser.Page;
import com.sunx.storage.DBFactory;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(id = 1, service = "com.fosun.fonova.moudle.channel.clubmed.ClubMedExecutableItem")
public class ClubMedExecutableItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(ClubMedExecutableItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat fs = new SimpleDateFormat("yyyyMMdd");
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //请求对象
    private Request request = new Request();
    //站点对象
    private Site site = new Site();
    //base url
    private String BASE_URL = "https://secure.clubmed.com.cn";
    //抽取城市
    private Pattern country = Pattern.compile("(?:country:[\\s]+)(.*?)(?:\",)");
    private Pattern lang = Pattern.compile("(?:lang:[\\s]+)(.*?)(?:\",)");
    private Pattern village = Pattern.compile("(?:village:[\\s]+)(.*?)(?:\",)");
    private Pattern proposal = Pattern.compile("(?:proposal:[\\s]+)(.*?)(?:\",)");

    /**
     * 开始解析数据
     *
     * @param pageDriver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver pageDriver, TaskEntity task) {
        try {
            pageDriver.get(task.getUrl());
            Wait.wait(pageDriver, 20, 20, () ->true);
//            {
//                List<WebElement> findElements = pageDriver.findElements(By.className("cm-Accommodations-results"));
//                return !findElements.isEmpty();
//            }

            //使页面滚动到最底部
            try{
                Actions action = new Actions(pageDriver);
                action.moveByOffset(10000,10000).perform();
            }catch (Exception e){
                e.printStackTrace();
            }
            //模拟点击查看更多房型
            try{
                WebElement ele = pageDriver.findElement(By.cssSelector("#js-AllaccommodationsResults"));
                ele.click();
                Wait.wait(pageDriver,5,3,() -> true);
            }catch (Exception e){
                e.printStackTrace();
            }

            // ===================================
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = "Unknown";
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getPeopleType();
            String md5 = MD5.md5(id);
            // ===================================
            logger.info("开始处理数据 > " + task.getChannelName() + "\t" + task.getCheckInDate() + " ...");

            // ===================================
            byte[] screenshotAs = FileUtil.getScreenshot(pageDriver);
            String imgPath = FileUtil.createPageFile(vday, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.PNG);
            FileUtils.writeByteArrayToFile(new File(imgPath), screenshotAs);

            String txtPath = FileUtil.createPageFile(vday, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.TXT);
            String pageSource = pageDriver.getPageSource();
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
            resultEntity.setVday(now);
            resultEntity.setPath(txtPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
            return Constant.TASK_SUCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return Constant.TASK_FAIL;
        }
    }

    /**
     * 根据给定的链接地址,找到具体的链接地址
     * @param url
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private String find(String url) {
        //找到第一次跳转的链接地址
        try{
            String src = downloader.downloader(request.setUrl(url),site.setTimeOut(Constant.DEFAULT_TIME_OUT).setIsSave(true));
            if(src == null || src.length() <= 0)return url;
            Page page = Page.me().bind(src);
            String str = page.css("meta[http-equiv=refresh]","content");
            if(str == null || str.length() <= 0)return url;
            String link = toUrl(str,"url=",1);
            if(link == null || link.length() <= 0)return url;
            return find(url,link);
        }catch (Exception e){
            e.printStackTrace();
        }
        return url;
    }

    /**
     * 获取地址
     * @param base
     * @param link
     * @return
     */
    public String find(String base,String link){
        //拼接链接地址
        String url = BASE_URL + link;
        //开始处理这一个链接,获取到相应的数据
        try{
            String src = downloader.downloader(request.setUrl(url),site.setTimeOut(Constant.DEFAULT_TIME_OUT).setIsSave(true));
            if(src == null || src.length() <= 0)return url;
            String cty = find(country,src,1);
            String lng = find(lang,src,1);
            String vil = find(village,src,1);
            String pro = find(proposal,src,1);

            if(cty == null || lng == null || vil == null || pro == null)return base;
            return "https://secure.clubmed.com.cn/cm/be/" + cty + "/" + lng + "/" + vil + "/" + pro + "#accommodations";
        }catch (Exception e){
            e.printStackTrace();
        }
        return base;
    }

    /**
     * 从字符串中抽取链接地址
     * @param split
     * @param index
     * @param str
     * @return
     */
    public String toUrl(String str,String split,int index){
        try{
            String[] tmp = str.split(split);
            if(tmp == null)return null;
            return tmp.length <= index?null:tmp[index];
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据给定的正则和字符串匹配出结果
     * @param pattern
     * @param str
     * @param index
     * @return
     */
    private String find(Pattern pattern,String str,int index){
        Matcher matcher = pattern.matcher(str);
        if(matcher.find())return matcher.group(index);
        return null;
    }

    public static void main(String[] args){
        ClubMedExecutableItem ci = new ClubMedExecutableItem();
        String url = "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=SAYC&dureeSejour=2&dateDeDebut=2016%2F12%2F29&nbParticipants=2&dateDeNaissance=2010%2F12%2F25&nbEnfants=1";
        String result = ci.find(url);

        System.out.println(result);
    }
}
