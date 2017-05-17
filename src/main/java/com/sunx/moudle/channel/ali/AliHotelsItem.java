package com.sunx.moudle.channel.ali;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunx.constant.Constant;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.enums.ImageType;
import com.sunx.moudle.proxy.IProxy;
import com.sunx.moudle.proxy.ProxyManager;
import com.sunx.moudle.template.Template;
import com.sunx.storage.DBFactory;
import com.sunx.utils.FileUtil;
import com.sunx.utils.MD5;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 处理阿里就点的数据内容
 *
 * 飞猪移动端首页地址:http://h5.m.taobao.com/trip/index.html
 *
 *  //初始化需要抓取的地区
 *  datas.add(new DataEntity("三亚", "460200", "51141195"));
 *  datas.add(new DataEntity("东澳岛", "440400", "12655240"));
 *  datas.add(new DataEntity("桂林", "450300", "50864003"));
 */
@Service(id = 8, service = "com.sunx.moudle.channel.ali.AliHotelsItem")
public class AliHotelsItem  implements IParser {
    //默认的token
    private String h5_tk = "";//1f5ffe425695856baeba3403e9766c6a
    private String h5_tk_enc = "";//ed4dcd762a714dacdd6e4268eb592bce
    private String h5_tk_time = "" + System.currentTimeMillis();
    //key
    private String appkey = "12574478";

    //酒店详情数据的解析格式
    private String ALI_HOTEL_URL = "https://acs.m.taobao.com/h5/mtop.trip.hotel.hotelDetail/1.0?ttid=201300@travel_h5_3.1.0&type=jsonp&callback=mtopjsonp1&api=mtop.trip.hotel.hotelDetail&v=1.0&appKey=12574478&t=TIME_CODE&sign=SIGN_CODE&data=";

    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(AliHotelsItem.class);

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();

    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数初始化数据
     */
    public AliHotelsItem(){
        //初始化请求头
        site.addHeader("Accept","*/*");
        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8,en;q=0.6");
        site.addHeader("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36");
        //设置保存cookie
        site.setIsSave(true).setTimeOut(10000);
    }

    /**
     * 开始解析数据
     *
     * @param pageDriver
     * @param task
     */
    public int parser(DBFactory factory, RemoteWebDriver pageDriver, TaskEntity task) {
        //开始下载处理数据
        try{
            int cnt = toParse(factory,pageDriver,task);
            logger.info("开始更新数据状态...");
            if(cnt < 0){
                return Constant.TASK_FAIL;
            }else if(cnt == 0){
                logger.info("token失效,需要重新获取token...");
                return Constant.TASK_NEW;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return Constant.TASK_SUCESS;
    }

    /**
     * 解析数据
     * @param factory
     * @param pageDriver
     * @param task
     * @return
     */
    public int toParse(DBFactory factory, RemoteWebDriver pageDriver, TaskEntity task){
        try{
            //获取下载的网页内容
            String src = getSrc(task);
            if(src == null || src.length() <= 0)return -1;
            if(src.contains("FAIL_SYS")){
                logger.info(src);
                h5_tk = "";
                h5_tk_enc = "";
                h5_tk_time = "";
                return 0;
            }
            logger.info("下载数据完成,开始处理数据.....");
            //格式化数据为json
            JSONObject bean = JSON.parseObject(src);
            if(bean == null)return -1;
            JSONObject data = bean.getJSONObject("data");
            //开始处理第一个网页快照内容
            String snapshot = toSnapshot(data,task.getUrl(),"ali-hotel");
            //解析内容,并将相应的数据插入到数据库中
            return dealData(factory,task,data,snapshot);
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     *
     * @param factory
     * @param entity
     * @param data
     */
    public int dealData(DBFactory factory,TaskEntity entity,JSONObject data,String snapshot){
        //下载成功
        if(!data.containsKey("roomTypes"))return 1;
        //开始解析数据
        JSONArray roomTypes = data.getJSONArray("roomTypes");
        //网页快照
        StringBuffer html = new StringBuffer();
        //处理数据
        dealData(html,roomTypes);
        //将快照汇总
        snapshot = snapshot.replaceAll("#ROOM_DETAIL_DATA",html.toString());
        //保存数据到数据库中
        return save(factory,entity,snapshot);
    }

    /**
     * 保存网页快照
     * @param factory
     * @param task
     * @param html
     * @return
     */
    public int save(DBFactory factory,TaskEntity task, String html){
        try{
            logger.info("开始存储网页快照数据到数据库中....");
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getSleep();
            String md5 = com.sunx.common.encrypt.MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), html, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate().replaceAll("-",""));
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(null);
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
            return 1;
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     *
     * @param html
     * @param roomTypes
     */
    public void dealData(StringBuffer html,JSONArray roomTypes){
        if(roomTypes == null || roomTypes.size() <= 0)return;
        //遍历房型,针对每一种房型处理数据
        for(int i = 0;i<roomTypes.size();i++){
            try{
                JSONObject node = roomTypes.getJSONObject(i);
                //获取房型内容
                String name = node.getString("name");
                //房间描述
                String desc = node.getString("windowType") + " " + node.getString("bedType");
                /** 最低价格 **/
                int lowPrice = node.getIntValue("price");

                //开始生产快照
                String template = toSnapshot(name,desc,lowPrice,"ali-hotel-items");

                //开始处理该房型下的所有的数据
                JSONArray rooms = node.getJSONArray("sellers");
                StringBuffer buffer = new StringBuffer();
                for(int k=0;k<rooms.size();k++){
                    //格式化json对象内容
                    JSONObject seller = rooms.getJSONObject(k);
                    //获取代理商的名称
                    String sellerName = seller.getString("sellerNick");

                    //找到该代理商对应的房型
                    JSONArray selleRooms = seller.getJSONArray("items");
                    //开始处理这些数据
                    for(int j=0;j<selleRooms.size();j++){
                        //对象
                        JSONObject item = selleRooms.getJSONObject(j);
                        //获取房价的描述
                        String bedDesc = item.getString("bedDesc");
                        String breakfastDesc = item.getString("breakfastDesc");
                        String cancelDesc = item.getString("cancelDesc");

                        //房价title
                        String title = item.getString("rpTitle");
                        //房间价格
                        double showPrice = item.getDoubleValue("showPrice");

                        //开始处理快照内容
                        String snap = toSnapshot(sellerName,bedDesc,breakfastDesc,cancelDesc,title,showPrice,"ali-hotel-node");

                        //将快照加入到字符串中
                        buffer.append(snap);
                    }
                }
                //开始拼接快照
                template = template.replaceAll("#ROOM_NODE_LIST",buffer.toString());
                //将快照添加到集合中
                html.append(template);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    /**
     * 下载数据
     * @param task
     * @return
     */
    public String getSrc(TaskEntity task){
        String page = null;
        int index = 0;
        while(index <= 2){
            try {
                if (h5_tk != null && h5_tk.length() > 0) {
                    site.addHeader("Cookie", "_m_h5_tk=" + h5_tk + "_" + h5_tk_time + "; _m_h5_tk_enc=" + h5_tk_enc + ";");
                }
                String link = getUrl(task, h5_tk, appkey);
                page = loader(downloader,request.setUrl(link), site);
                if (page == null || page.length() <= 0) break;
                if (page.contains("FAIL_SYS")) {
                    //说明数据失败,需要重新抓取
                    update(site);
                    index++;

                    try{
                        Thread.sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    continue;
                }
                break;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        //清晰网页源码
        if(page != null){
            page = page.replaceAll("mtopjsonp1\\(","").replaceAll("}\\)","}");
        }
        return page;
    }

    /**
     * 下载数据内容
     * @param request
     * @param site
     * @return
     */
    public String loader(Downloader downloader,Request request, Site site){
        String src = null;
        try{
            logger.error("开始下载网页内容......");
            int j = 0;
            while(j <= 3){
                //获取代理
                IProxy proxy = null;
                int i = 0;
                while(proxy == null || i < 5){
                    proxy = ProxyManager.me().poll();
                    if(proxy == null){
                        i++;
                        try{
                            logger.error("获取代理失败,等待1s后继续重新获取代理....");
                            Thread.sleep(1000);
                        }catch ( Exception e){
                            e.printStackTrace();
                        }
                        continue;
                    }
                    break;
                }
                if(proxy == null){
                    proxy = new IProxy();
                }
                System.err.println("开始下载......");
                src = downloader.downloader(request,site,proxy.getHost(),proxy.getPort());
                if(src != null && proxy != null && !src.contains("<!DOCTYPE html>")){
                    ProxyManager.me().offer(proxy);
                    break;
                }
                System.err.println("下载失败,等待下次重试......");
                j++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.err.println("下载完成.....");
        return src;
    }

    /**
     * 更新页面数据
     * @param site
     */
    public void update(Site site){
        String tk = site.getCookie("_m_h5_tk");
        if(tk == null)return;
        String[] tmps = tk.split("_");
        if(tmps == null || tmps.length != 2)return;
        this.h5_tk = tmps[0];
        this.h5_tk_time = tmps[1];
        if(h5_tk_time == null || h5_tk_time.length() <= 0){
            h5_tk_time = System.currentTimeMillis() + "";
        }
        //设置enc值
        this.h5_tk_enc = site.getCookie("_m_h5_tk_enc");
    }

    /**
     *
     * @param task
     * @return
     */
    public String getUrl(TaskEntity task,String h5_tk,String appkey) throws Exception{
        String t = System.currentTimeMillis() + "";
        String data = task.getUrl();
        String sign = sign(h5_tk,t,appkey,data);

        return ALI_HOTEL_URL.replaceAll("TIME_CODE",t).replaceAll("SIGN_CODE",sign) + URLEncoder.encode(data,"utf-8");
    }

    /**
     * 根据一些特定的数据加密为相应的值
     * @param h5_tk
     * @param time
     * @param appkey
     * @param data
     * @return
     */
    public String sign(String h5_tk,String time,String appkey,String data) throws Exception{
        String str = h5_tk + "&" + time + "&" + appkey + "&" + data;
        return MD5.convert(str);
    }

    /**
     *
     * @param bean
     * @param type
     * @return
     */
    public String toSnapshot(JSONObject bean,String url,String type){
        //获取模板
        String template = Template.me().get(type);
        try{
            //格式化数据
            JSONObject data = JSON.parseObject(url);
            //开始替换数据
            template = template.replaceAll("#SHOP_NAME",bean.getString("name"))
                               .replaceAll("#SHOP_SCORE",bean.getString("rateScore"))
                               .replaceAll("#SHOP_REPLY_NUM",bean.getString("rateNumber"))
                               .replaceAll("#SHOP_DISTRICT",bean.getString("area"))
                               .replaceAll("#SHOP_ADDRESS",bean.getString("address"))
                               .replaceAll("#CHECK_IN_DATA",data.getString("checkIn"))
                               .replaceAll("#CHECK_OUT_DATA",data.getString("checkOut"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return template;
    }

    /**
     *
     * @param name
     * @param type
     * @return
     */
    public String toSnapshot(String name,String desc,int lowPrice,String type){
        //获取模板
        String template = Template.me().get(type);
        //开始替换数据
        try{
            template = template.replaceAll("#ROOM_NAME",name)
                    .replaceAll("#ROOM_DESC",desc)
                    .replaceAll("#ROOM_LOWPRICE",(lowPrice / 100) + "");
        }catch (Exception e){
            e.printStackTrace();
        }
        return template;
    }

    /**
     *
     * @param sellerName
     * @param bedDesc
     * @param breakfastDesc
     * @param cancelDesc
     * @param title
     * @param showPrice
     * @param type
     * @return
     */
    public String toSnapshot(String sellerName,String bedDesc,String breakfastDesc,String cancelDesc,String title,double showPrice,String type){
        //获取模板
        String template = Template.me().get(type);
        //开始替换数据
        try{
            //拼接描述
            String desc = "<span>" + bedDesc + "</span><span>" + breakfastDesc + "</span><span>" + cancelDesc + "</span>";

            template = template.replaceAll("#ROOM_DETAIL_DESC",desc)
                    .replaceAll("#ROOM_TITLE",title)
                    .replaceAll("#SELLER_NAME",sellerName)
                    .replaceAll("#ROOM_PRICE",((int)(showPrice / 100)) + "");
        }catch (Exception e){
            e.printStackTrace();
        }
        return template;
    }

    public static void main(String[] args){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannelId(5);
        taskEntity.setChannelName("阿里旅行");
        taskEntity.setCheckInDate("2017-05-23");
        taskEntity.setRegion("三亚");
        taskEntity.setSleep(2);
        taskEntity.setUrl("{\"filterByPayment\":\"0\",\"cityCode\":\"460200\",\"checkIn\":\"2017-05-23\",\"checkOut\":\"2017-05-25\",\"guid\":\"\",\"from\":\"hotel-list-page\",\"surroundingByHotel\":\"0\",\"adultNum\":\"2\",\"wirelessStraightField\":\"{\\\"searchId\\\":\\\"\\\"}\",\"hid\":\"0\",\"cityName\":\"三亚\",\"shid\":\"51141195\",\"spm\":\"\",\"ttid\":\"201300@travel_h5_3.1.0\",\"_preProjVer\":\"0.8.40\",\"_projVer\":\"0.8.40\",\"isIncludePayLater\":0,\"needDeal\":1,\"childrenAges\":\"\",\"isShowExpedia\":1,\"supportPCI\":1,\"sversion\":7,\"displayPackage\":1,\"hidden\":\"{\\\"straight_field\\\":{\\\"searchId\\\":\\\"\\\"}}\",\"h5Version\":\"0.8.40\"}");

        new AliHotelsItem().parser(null,null,taskEntity);
    }
}
