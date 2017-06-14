package com.sunx.moudle.channel.aoyou;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sunx.constant.Constant;
import com.sunx.downloader.*;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.enums.ImageType;
import com.sunx.parser.Node;
import com.sunx.parser.Page;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 遨游的数据采集
 */
@Service(id = 14, service = "com.fosun.fonova.moudle.channel.aoyou.AoYouSearchItem")
public class AoYouSearchItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(AoYouSearchItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();

    //常用参数
    private String SEARCH_ROOMS_URL = "http://tour.aoyou.com/ErpTourProduct/SynGetPriceType";
    private String PRICE_URL = "http://tour.aoyou.com/ErpTourProduct/SynGetCalendar";
    private String SEARCH_ROOMS_DETAIL_URL = "http://tour.aoyou.com/ErpTourProduct/SynGetHotelDatePrice";


    public AoYouSearchItem(){
        site.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
        site.addHeader("Accept-Encoding","gzip, deflate");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8");
        site.addHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
    }
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
            String html = Helper.downlaoder(downloader,request.setUrl(task.getUrl()).setMethod(Method.GET),site.setTimeOut(1000 * 10),false);
            Page page = Page.me().bind(html);
            //开始请求,获取价格的请求参数
            //抽取出商品id
            String pid = page.css("#h_productid","value");
            //年份
            String year = page.css("#h_year","value");
            //月份
            String m = page.css("#h_month","value");
            //type
            String type = page.css("#h_type","value");
            //封装任务对象
            Map<String,String> map = new HashMap<>();
            map.put("productid",pid);
            map.put("year",year);
            map.put("month",m);
            //开始请求,获取价格请求参数
            String priceParam = Helper.downlaoder(downloader,request.setUrl(PRICE_URL).setMethod(Method.POST).setPostData(map),site,false);
            //格式化对象
            String param = Page.me().bind(priceParam).css("#h_fpd","value");
            //根据不同的类型,自助游还是自由行还是酒店+景点来进行数据的分离处理
            return toDetail(factory,task,page,pid,param,type);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 获取房型和人员组成网页源码
     * @param factory
     * @param task
     * @param page
     * @return
     */
    public int toDetail(DBFactory factory,TaskEntity task,Page page,String pid,String param,String type){
        //post请求的参数
        Map<String,String> map = new HashMap<>();
        map.put("productid",pid);
        map.put("priceid",param);
        map.put("type",type);

        site.addHeader("Referer",task.getUrl());

        //获取到商品id以后,开始获取基础的房型数据
        String roomHtml = Helper.downlaoder(downloader,
                                            request.setUrl(SEARCH_ROOMS_URL).setMethod(Method.POST).setPostData(map),
                                            site.setTimeOut(1000 * 10),false
                                           );
        //对网页源码进行盘点
        if(roomHtml == null)return Constant.TASK_FAIL;
        JSONObject bean = JSON.parseObject(roomHtml);
        if(!bean.containsKey("pricetype"))return Constant.TASK_FAIL;
        Page roomPage = Page.me().bind(bean.getString("pricetype"));
        //开始处理价格内容
        return toPrice(factory,task,page,roomPage,pid);
    }

    /**
     * 获取价格内容js
     * @param factory
     * @param task
     * @param page
     * @param roomPage
     * @return
     */
    public int toPrice(DBFactory factory,TaskEntity task,Page page,Page roomPage,String pid){
        //post请求的参数
        Map<String,String> map = new HashMap<>();
        map.put("productid",pid);
        map.put("startdate",task.getCheckInDate().replaceAll("-","/"));
        map.put("enddate",task.getCheckOutDate().replaceAll("-","/"));

        //获取到商品id以后,开始获取基础的房型数据
        String priceHtml = Helper.downlaoder(downloader,
                request.setUrl(SEARCH_ROOMS_DETAIL_URL).setMethod(Method.POST).setPostData(map),
                site.setTimeOut(1000 * 10),false
        );
        //对网页源码进行盘点
        if(priceHtml == null)return Constant.TASK_FAIL;
        JSONObject bean = JSON.parseObject(priceHtml);
        if(!bean.containsKey("grouppircehtml"))return Constant.TASK_FAIL;
        Page pricePage = Page.me().bind(bean.getString("grouppircehtml"));
        //获取到具体的数据格式以后
        return toSnapshot(factory,task,page,roomPage,pricePage);
    }

    /**
     * 生产快照
     * @param factory
     * @param task
     * @param page
     * @param roomPage
     * @param pricePage
     * @return
     */
    public int toSnapshot(DBFactory factory,TaskEntity task,Page page,Page roomPage,Page pricePage){
        int cnt = -1;
        //抽取出房型
        Node node = roomPage.$("[class=row_choose package_sec clearfix]:contains(房) div.row_item");
        //抽取出人员组成
        Node people = roomPage.$("[class=row_choose package_sec clearfix]:contains(成人) div.row_item");

        for(int i=0;i<node.size();i++){
            //房型
            String houseType = node.css(i,"a");
            //房型id
            String hid = node.css(i,"a","id");
            //读取房型的网页源码,修改网页源码的选择选项
            roomPage.attr("[class=row_choose package_sec clearfix]:contains(房) .selected","class","row_item")
                    .attr("[class=row_choose package_sec clearfix]:contains(房) .row_item:has(a[id="+ hid +"])","class","row_item selected");

            for(int j=0;j<people.size();j++){
                //人员组成
                String peopleType = people.css(j,"a");
                //人员类型id
                String pid = people.css(j,"a","id");

                //获取对应的价格内容
                int price = toPrice(hid,pid,pricePage);

                //读取房型的网页源码,修改网页源码的选择选项
                //读取房型的网页源码,修改网页源码的选择选项
                roomPage.attr("[class=row_choose package_sec clearfix]:contains(成人) .selected","class","row_item")
                        .attr("[class=row_choose package_sec clearfix]:contains(成人) .row_item:has(a[id="+ pid +"])","class","row_item selected");

                //数据获取内容完成,准备存储数据
                toSnapshot(factory,task,page,roomPage,pricePage,houseType,peopleType,price);
            }
        }
        if(cnt > 0)return Constant.TASK_SUCESS;
        return Constant.TASK_FAIL;
    }

    /**
     * 获取价格
     * @param hid
     * @param pid
     * @param page
     * @return
     */
    public int toPrice(String hid,String pid,Page page){
        int price = 0;
        String cssQuery = "li[class*=t" + hid + "t" + pid + "]";
        Node node = page.$(cssQuery);
        for(int i=0;i<node.size();i++){
            String txt = node.css(i,"li","value");
            try{
                price += Integer.parseInt(txt);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return price;
    }

    /**
     * 生成快照
     * @param factory
     * @param task
     * @param house
     * @param people
     * @param price
     */
    public void toSnapshot(DBFactory factory,TaskEntity task,Page page,Page roomPage,Page pricePage,String house,String people,int price){
        //房型的网页源码
        String html = roomPage.html("body");
        //开始修改下载到的网页源码
        page.replace("#input_data_span",task.getCheckInDate())
            .replace("#input_data_span2",task.getCheckOutDate())
            .replace("#rentday font",task.getSleep()+"")
            .replace(".ft_price font",price + "")
            .replace(".ft_calculate font",price + "")
            .replace("div.base_price strong",price + "")
            .attr(".submit_btn","style","background-color: rgb(255, 102, 0);")
            .replace("#pricetype",html)
            ;

        save(factory,task,people,house,price,page.html());
    }

    /**
     * 截图
     * @param task
     */
    public void save(DBFactory factory,TaskEntity task,String peopleType,String houseType,int price,String html){
        try{
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + ","
                      + task.getCheckInDate() + "," + task.getUrl() + "," + task.getSleep() + ","
                      + peopleType + "," + houseType;
            String md5 = MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), html, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate().replaceAll("-",""));
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(houseType);
            resultEntity.setPeopleType(peopleType);
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);
            resultEntity.setSleep(task.getSleep());
            //价格

//            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(123l);
        taskEntity.setUrl("http://tour.aoyou.com/ep942");
        taskEntity.setChannelId(14);
        taskEntity.setChannelName("遨游");
        taskEntity.setTitle("");
        taskEntity.setStatus(Constant.TASK_NEW);
        taskEntity.setRegion(Constant.DEFALUT_REGION);
        taskEntity.setSleep(3);
        taskEntity.setCheckInDate("2017-06-23");
        taskEntity.setCheckOutDate("2017-06-26");

        AoYouSearchItem item = new AoYouSearchItem();
        item.parser(null,null,taskEntity);
    }
}
