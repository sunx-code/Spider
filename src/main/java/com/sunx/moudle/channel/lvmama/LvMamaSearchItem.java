package com.sunx.moudle.channel.lvmama;

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
import com.sunx.moudle.channel.tuniu.TuNiuItem;
import com.sunx.moudle.enums.ImageType;
import com.sunx.parser.Page;
import com.sunx.utils.FileUtil;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import com.sunx.utils.Helper;
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
import java.util.Map;

/**
 *
 *
 */
@Service(id = 10, service = "com.sunx.moudle.channel.lvmama.LvMamaSearchItem")
public class LvMamaSearchItem implements IParser {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(LvMamaSearchItem.class);
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

    //详情数据内容
    private String DETAIL_URL = "http://dujia.lvmama.com/package/loadingGoods?selectDate1=CHECK_IN_DAY&adultQuantity=ADULT_NUM&childQuantity=CHILD_NUM&quantity=1&productId=PRO_ID&startDistrictId=-1&changeToPeopleFlag=N&choadultQuantity=&chochildQuantity=";

    public LvMamaSearchItem(){
        site.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8");
        site.addHeader("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        site.setTimeOut(1000 * 10);
    }

    /**
     * 开始解析数据
     *
     * @param factory
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        try{
            return dealFreeData(factory,task);//表示自由行
        }catch (Exception e){
            e.printStackTrace();
            logger.error("渠道id:" + task.getChannelId() + ",任务id:" + task.getId() + "对应的链接地址为:" + task.getUrl() + ",错误信息为:" + e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 处理自由行数据
     * @param factory
     * @param task
     */
    public int dealFreeData(DBFactory factory, TaskEntity task) throws Exception {
        //下载首页
        String html = Helper.downlaoder(task.getChannelId(),downloader,request.setUrl(task.getUrl()),site);
        if(html == null)return Constant.TASK_FAIL;
        //从网页源码中抽取出成人数,儿童数,请求数据
        Page page = Page.me().bind(html);
        //成人数
        String adultNum = page.css("span[id^=adult]");
        //儿童数
        String childNum = page.css("span[id^=child]");
        //商品id
        String proId = page.css("#productId","value");
        if(proId == null){
            proId = page.filter("div.product_info1","p");
            if(proId != null){
                proId = Helper.clean(proId,"[^0-9]","");
            }                     
        }
        if(adultNum == null || childNum == null || proId == null)return Constant.TASK_FAIL;
        //获取价格数据
        String detailHtml = toDetail(task,task.getCheckInDate(),proId,adultNum,childNum);
        //开始抽取出总价格
        Page pricePage = Page.me().bind(detailHtml);
        int price = toPrice(pricePage);
        if(price == 0)return Constant.TASK_FAIL;
        //转化为快照
        toSnapshot(factory,task,page,price,detailHtml,adultNum,childNum);
        return Constant.TASK_SUCESS;
    }

    /**
     * 获取价格
     * @param pricePage
     * @return
     */
    public int toPrice(Page pricePage) throws Exception{
        //获取默认价格
        String priceStr = pricePage.css(".default div[class=package-item adjust-product-item package-button-div]","data-price");
        //剔除保险等数据的价格
        pricePage.remove(".optional-item-status i");
        if(priceStr == null || priceStr.length() <= 0){
            return 0;
        }
        //将价格格式化为整数
        return Helper.toInt(priceStr) / 100;
    }

    /**
     * 获取详情
     * @param checkInDay
     * @param proId
     * @param adultNum
     * @param childNum
     * @return
     */
    public String toDetail(TaskEntity task,String checkInDay,String proId,String adultNum,String childNum){
        try{
            logger.info(proId + "\t" + adultNum + "\t" + childNum);
            String link = DETAIL_URL.replaceAll("CHECK_IN_DAY",checkInDay)
                                    .replaceAll("ADULT_NUM",adultNum)
                                    .replaceAll("CHILD_NUM",childNum)
                                    .replaceAll("PRO_ID",proId);
            //格式化site的请求头
            site.addHeader("Content-Type","application/json;charset=utf-8");
            site.addHeader("Referer","http://dujia.lvmama.com/package/" + proId);

            return Helper.downlaoder(task.getChannelId(),downloader,request.setUrl(link),site,false);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("下载错误,对应的数据内容为:" + proId + "\t" + adultNum + "\t" + childNum + ",错误信息为:" + e.getMessage());
        }
        return null;
    }

    /**
     * 修改网页内容,填充快照
     * @param page
     * @param task
     * @param price
     * @param html
     */
    public void toSnapshot(DBFactory factory,TaskEntity task,Page page,int price,String html,String adultNum,String childNum){
        //调整网页中购买的份数,将购买份数调整为1
        page.append("select#preorder-quantity","<option value=\"1\" selected=\"selected\">1</option>","select#preorder-quantity option");
        //填充日期
        String timeHtml = "<input class=\"like-input JS_calendar\" type=\"text\" value=\""+ task.getCheckInDate() + "\"/>";
        page.append("div[class=hotelSeach-input startDate pr]",timeHtml,"input[class=like-input JS_calendar]");
        //填充价格
        page.clean("#total-price-value");
        page.append("#total-price-value","" + price);

        //填充实际网页内容数据
        page.append("div#preorder-adjust",html);
        //转化为快照
        toSnapshot(factory,task,page.html(),adultNum,childNum);
    }

    /**
     * 保存快照内容
     * @param factory
     * @param task
     * @param html
     */
    public void toSnapshot(DBFactory factory,TaskEntity task,String html,String adultNum,String childNum){
        try{
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + adultNum + "," + childNum;
            String md5 = MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), html, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate().replaceAll("-",""));
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(Helper.people(Helper.toInt(adultNum),Helper.toInt(childNum)));
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setChildNum(Helper.toInt(childNum));
            resultEntity.setAdultNum(Helper.toInt(adultNum));
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 测试main函数
     * @param args
     */
    public static void main(String[] args){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannelId(6);
        taskEntity.setChannelName("驴妈妈-搜索");
        taskEntity.setCheckInDate("2017-07-06");
        taskEntity.setRegion(Constant.DEFALUT_REGION);
        taskEntity.setUrl("http://dujia.lvmama.com/package/625844");

        new LvMamaSearchItem().parser(null,taskEntity);
    }
}
