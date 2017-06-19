package com.sunx.moudle.channel.tuniu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunx.common.encrypt.MD5;
import com.sunx.constant.Constant;
import com.sunx.downloader.*;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.enums.ImageType;
import com.sunx.moudle.proxy.IProxy;
import com.sunx.moudle.proxy.ProxyManager;
import com.sunx.moudle.template.Template;
import com.sunx.parser.Page;
import com.sunx.storage.DBFactory;
import com.sunx.utils.FileUtil;
import com.sunx.utils.Helper;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 给定商品地址来进行数据的采集
 * 1 删除标签.book_bar下的所有的标签
 * 2 读取模板1,添加酒店信息以及人员组成,入住日期,以及价格数据
 * 3 读取模板2,添加房型数据,同时更新价格
 * 4 将模板2插入到模板1中,同时将模板1写入到网页对应标签.book_bar下
 * 5 将网页对象转化为字符串写入到文件中
 * 6 向数据库中写入具体的数据内容
 */
@Service(id = 6, service = "com.fosun.fonova.moudle.channel.tuniu.TuNiuItem")
public class TuNiuItem implements IParser {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(TuNiuItem.class);

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();

    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String ROOM_DETAIIL = "tuniu";
    private String ROOM_DETAIL_LIST = "tuniu-rooms";

    private Pattern pattern = Pattern.compile("\\d+");
    //获取酒店数据
    private String HOTEL_SEARCH_URL = "http://www.tuniu.com/yii.php?r=order/tourV3DriveOrder/getDefaultHotelNRTRoom&productId=PRO_ID&departCityCode=2500&backCityCode=0&bookCityCode=2500&adultNum=ADULT_NUM&childNum=CHILD_NUM&departDate=START_DAY";
    //卡券数据
    private String KAQUAN_SEARCH_URL = "http://www.tuniu.com/yii.php?r=order/DiyV3Order/GetDiyV3AltAdditionalItemResources";


    public TuNiuItem(){
        //请求头
        site.addHeader("accept-encoding","gzip, deflate, sdch, br");
        site.addHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

        //设置超时时间
        site.setTimeOut(10000);
    }

    /**
     * 开始解析数据
     *
     * @param factory
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        try{
            //开始请求网站首页,获取网页源码
            String src = Helper.downlaoder(downloader,request.setUrl(task.getUrl()),site,false);
            if(src == null || src.length() <= 0){
                logger.error("下载网页源码失败.链接地址为:" + task.getUrl());
                return Constant.TASK_FAIL;
            }
            //抽取其中的productId
            String pro = Helper.find(task.getUrl(),pattern);
            //处理这一天的数据
            String html = dealData(task,pro,2,1);
            toSnapshot(factory,src,html,task,2,1);
            sleep(1500);
            //线程休眠一定时间后继续
            html = dealData(task,pro,2,0);
            toSnapshot(factory,src,html,task,2,1);
            sleep(1500);
            //线程休眠一定时间后继续
            html = dealData(task,pro,1,1);
            toSnapshot(factory,src,html,task,2,1);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Constant.TASK_SUCESS;
    }

    /**
     * 拼接快照
     * @param src
     * @param rooms
     * @param task
     * @param adultNum
     * @param childNum
     */
    public void toSnapshot(DBFactory factory,String src,String rooms,TaskEntity task,int adultNum,int childNum){
        if(rooms == null || rooms.length() <= 0){
            rooms = "暂时没有房型";
        }
        //找到房型加载中的标签,将该标签剔除掉,同时遍历集合,将数据添加进网页中
        String txt = removeTag(src);
        //遍历集合,开始追加数据
        txt = txt.replaceAll("#ROOM_HTML",rooms);

        try{
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getSleep();
            String md5 = MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), txt, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate().replaceAll("-",""));
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(Helper.people(adultNum,childNum));
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 格式化代码,剔除标签
     * @param html
     * @return
     */
    public String removeTag(String html){
        try{
            Page page = Page.me().bind(html);
            page.append(".book_bar","#ROOM_HTML",".book_title",".book_resource");
            return page.html();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 处理结果数据
     * @param task
     * @param pro
     */
    private String dealData(TaskEntity task,String pro,int adultNum,int childNum){
        try{
            //请求酒店数据
            String link = find(HOTEL_SEARCH_URL,task.getCheckInDate(),pro,adultNum,childNum);
            //下载数据
            String src = Helper.downlaoder(downloader,request.setUrl(link),site);
            //处理当前酒店数据
            if (src == null || src.length() <= 0) {
                logger.info("当前集合可能为空...线程休眠一定时间后接续...");
                return null;
            }
            //hotel数据
            JSONObject hotel = findHotel(src);
            //格式化数据
            JSONObject hotelInfo = findRoomes(hotel);
            if(hotelInfo == null)return null;
            JSONArray bean = hotelInfo.getJSONArray("rooms");
            //获取卡券的数据
            JSONObject tec = findTec(hotel,task.getCheckInDate(),pro,adultNum,childNum);
            //获取卡券的价格
            int tecPrice = getPrice(tec,task.getCheckInDate(),adultNum,childNum);
            //更新人物对应的地区
            task.setRegion(hotel.getString("departCity"));

            //开始处理差价以及商品数据内容
            JSONObject selectRoom = null;
            JSONArray detailArr = new JSONArray();
            //循环获取数据
            for(int i=0;i<bean.size();i++){
                try{
                    JSONObject node = bean.getJSONObject(i);
                    //房间标题
                    String resName = node.getString("resName");
                    //maxAdultNum
                    int maxAdultNum = node.getIntValue("maxAdultNum");
                    //判断房间数
                    int roomNum = (int)Math.ceil(1.0 * adultNum / maxAdultNum);
                    //获取价格
                    int price = getPrice(node);
                    //获取房型
                    String houseType = type(resName);
                    //床形
                    String bedType = node.getString("bedType");
                    //早餐
                    String feed = node.getIntValue("breakfastNum") == 2?"双早":"单早";
                    //宽带
                    String network = node.getString("network");
                    //房间总价
                    int totalPrice = price * roomNum + tecPrice;
                    //获取当前选择的房型
                    if(node.getBoolean("select")){
                        selectRoom = node;
                        selectRoom.put("totalPrice",totalPrice);
                        selectRoom.put("desc",bedType + "/" + feed + "/" + network);
                        selectRoom.put("roomNum",roomNum);
                    }
                    //构造一个json对象
                    JSONObject detail = new JSONObject();
                    detail.put("#ROOM_TITLE",resName);
                    detail.put("#BED_TYPE","".equals(bedType)?"--":bedType);
                    detail.put("#BREAK_FAST_NUM",feed);
                    detail.put("#NET_WORK",network);
                    detail.put("#ROOM_NUM",roomNum);
                    detail.put("totalPrice",totalPrice);
                    detail.put("select",node.getBooleanValue("select"));

                    detailArr.add(detail);
                    //打印数据  那一天  那个店铺  店铺链接     房型   成人数   儿童数   酒店价格   卡券价格
                    logger.info(task.getCheckInDate() + "\t" + pro + "\t" + resName + "\t" + houseType + "\t" +  adultNum + "\t" + childNum + "\t" + price * roomNum + "\t" + tecPrice);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            StringBuffer buffer = new StringBuffer();
            //开始计算差价,同时修改模板
            for(int i=0;i<detailArr.size();i++){
                JSONObject obj = detailArr.getJSONObject(i);

                String roomHtml = toTemplate(selectRoom,obj);
                buffer.append(roomHtml);
            }
            //开始处理酒店的信息,将拼接好的数据返回回去,用于填充快照
            return toTemplate(adultNum,childNum,hotel,hotelInfo,buffer,selectRoom);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 填充模板
     * @param hotel
     * @return
     */
    public String toTemplate(int adultNum,int childNum,JSONObject hotel,JSONObject hotelInfo,StringBuffer buffer,JSONObject selectRoom){
        try{
            //更新hotelInfo
            JSONObject hotelResInfo = hotelInfo.getJSONObject("hotelResInfo");
            //获取模板
            String template = Template.me().get(ROOM_DETAIIL);

            //开始批量替换上数据内容
            String data = template.replaceAll("#CHECK_IN_DAY",hotel.getString("startDate"));
            data = data.replaceAll("#ADULT_NUM",adultNum + "");
            data = data.replaceAll("#CHILD_NUM",childNum + "");
            data = data.replaceAll("#CHECK_OUT_DAY",hotel.getString("endDate"));
            data = data.replaceAll("#REGION", hotel.getString("departCity"));
            data = data.replaceAll("#HOTEL_NAME",hotelResInfo.getString("chineseName"));
            data = data.replaceAll("#HOTEL_ADDRESS",hotelResInfo.getString("address"));
            data = data.replaceAll("#SELEC_ROOM_TITLE",selectRoom.getString("resName"));
            data = data.replaceAll("#SELECT_ROOM_DESC",selectRoom.getString("desc"));
            data = data.replaceAll("#SELECT_ROOME_NUM",selectRoom.getString("roomNum"));
            return data.replaceAll("#TOTAL_PRICE",selectRoom.getString("totalPrice"))
                       .replaceAll("#ROOM_LIST",buffer.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 填充模板
     * @param selectRoom
     * @param room
     * @return
     */
    public String toTemplate(JSONObject selectRoom,JSONObject room){
        try{
            //获取价格差价
            String priceStr = "--";
            long resid = selectRoom.getIntValue("resId");
            long roomResId = room.getIntValue("resId");

            boolean falg = room.getBooleanValue("select");
            if(!falg && resid != roomResId){
                int p = room.getIntValue("totalPrice") - selectRoom.getIntValue("totalPrice");
                if(p > 0){
                    priceStr = "+￥" + Math.abs(p);
                }else{
                    priceStr = "-￥" + Math.abs(p);
                }
            }
            //获取模板
            String template = Template.me().get(ROOM_DETAIL_LIST);

            //开始批量替换上数据内容
            String data = template.replaceAll("#ROOM_TITLE",room.getString("#ROOM_TITLE"));
            data = data.replaceAll("#BED_TYPE",room.getString("#BED_TYPE"));
            data = data.replaceAll("#BREAK_FAST_NUM",room.getString("#BREAK_FAST_NUM"));
            data = data.replaceAll("#NET_WORK",room.getString("#NET_WORK"));
            data = data.replaceAll("#ROOM_NUM",room.getString("#ROOM_NUM"));

            return data.replaceAll("#ROOM_PRICE_DIFFERENCE",priceStr);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 冲标题中抽取出房型
     * @param title
     * @return
     */
    private String type(String title){
        try{
            if(title == null || title.length() <= 0 || !title.contains("--"))return null;
            String[] tmp = title.split("--");
            if(tmp == null || tmp.length <= 0)return null;
            return tmp[0];
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 找到哪天的价格
     * @param tec
     * @param day
     * @param adultNum
     * @param childNum
     * @return
     */
    private int getPrice(JSONObject tec, String day, int adultNum, int childNum){
        if(tec == null)return 0;
        if(tec.toJSONString().contains("暂无附加项目数据"))return 0;
        try{
            JSONObject data = tec.getJSONObject("data");
            JSONObject type = data.getJSONObject("5");
            JSONArray array = type.getJSONArray("resList");
            if(array == null || array.size() < 0)return 0;
            JSONObject bean = array.getJSONObject(0);
            JSONArray tmp = bean.getJSONArray("resourceDatePrices");
            //获取当前日期的价格
            int price = 0;
            for(int i=0;i<tmp.size();i++){
                JSONObject node = tmp.getJSONObject(i);

                //获取当前日期
                String current = node.getString("departDate");

                if(day.contains(current)){
                    //获取价格
                    int childPrice = node.getIntValue("childPrice");
                    int adultPrice = node.getIntValue("price");

                    price = adultPrice * adultNum + childPrice * childNum;

                    break;
                }
            }
            return price;
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取价格数据
     * @param node
     * @return
     */
    private int getPrice(JSONObject node){
        JSONObject bean = node.containsKey("price")?node.getJSONObject("price"):null;
        if(bean == null)return 0;
        int total = 0;
        for(String key : bean.keySet()){
            total = total + bean.getIntValue(key);
        }
        return total;
    }

    /**
     * 解析数据
     * @param json
     * @return
     */
    private JSONObject findHotel(String json){
        JSONObject bean = JSON.parseObject(json);
        JSONObject data = bean.containsKey("data")?bean.getJSONObject("data"):null;
        if(data == null)return null;
        JSONObject hd = data.getJSONObject("hotel");
        JSONObject hotels = null;
        for(String key : hd.keySet()){
            hotels = hd.getJSONObject(key);
            break;
        }
        return hotels;
    }

    private JSONObject findRoomes(JSONObject hotels){
        if(hotels == null)return null;
        JSONObject data = hotels.getJSONObject("hotels");
        JSONObject obj = null;
        for(String key : data.keySet()){
            JSONObject h = data.getJSONObject(key);
            //抽取数据
//            obj = h.containsKey("rooms")?h.getJSONArray("rooms"):null;
            obj = h.containsKey("rooms")?h:null;
            break;
        }
        return obj;
    }

    /**
     * 获取数据
     * @param day
     * @param pro
     * @return
     */
    private JSONObject findTec(JSONObject hotels, String day, String pro, int adultNum, int childNum){
        try{
            //获取对应的journeyId
            String jid = hotels.getString("journeyId").replaceAll("j_","");

            //设置请求方式
            request.setMethod(Method.POST)
                    .addPostData("postData[productId]",pro)
                    .addPostData("postData[departCityCode]","2500")
                    .addPostData("postData[backCityCode]","2500")
                    .addPostData("postData[adultNum]","" + adultNum)
                    .addPostData("postData[childNum]","" + childNum)
                    .addPostData("postData[departDate]",day)
                    .addPostData("postData[journeyId]",jid);
            //下载数据,并判断数据
            String src = Helper.downlaoder(downloader,request.setUrl(KAQUAN_SEARCH_URL),site);
            if(src == null || src.length() <= 0){
                logger.info("下载数据异常,需要处理异常数据..");
                return null;
            }
            //开始解析数据
            return JSON.parseObject(src);
        }   catch (Exception e){
            e.printStackTrace();
        }finally{
            request.setMethod(Method.GET);
        }
        return null;
    }

    /**
     *
     * @param url
     * @param day
     * @param proid
     * @param adultNum
     * @param childNum
     * @return
     */
    private String find(String url,String day,String proid,int adultNum,int childNum){
        return url.replaceAll("PRO_ID",proid)
                .replaceAll("ADULT_NUM","" + adultNum)
                .replaceAll("START_DAY",day)
                .replaceAll("CHILD_NUM","" + childNum);
    }

    /**
     * 线程休眠
     * @param sleep
     */
    public void sleep(long sleep){
        try{
            Thread.sleep(sleep);
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
        taskEntity.setChannelName("途牛");
        taskEntity.setCheckInDate("2017-09-03");
        taskEntity.setRegion(Constant.DEFALUT_REGION);
        taskEntity.setSleep(2);
        taskEntity.setUrl("http://www.tuniu.com/tours/210422974");

        new TuNiuItem().parser(null,taskEntity);
    }
}
