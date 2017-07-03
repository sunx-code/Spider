package com.sunx.moudle.channel.ali;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.bcel.internal.generic.TABLESWITCH;
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
import com.sunx.utils.Helper;
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
    private String SEARCH_DATA_URL = "https://acs.m.taobao.com/h5/mtop.trip.hotel.patternRender/1.0?type=jsonp&callback=mtopjsonp1&api=mtop.trip.hotel.patternRender&v=1.0&deviceId=3358a8dc-bc96-4917-h5h5-e79ab325h5h5&ttid=201300%40travel_h5_3.1.0&appKey=12574478&t=TIME_CODE&sign=SIGN_CODE&data=";
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(AliHotelsItem.class);

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();

    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //搜索需要的请求参数
    private String SEARCH_DATA = "{\"patternName\":\"hotel_search\",\"patternVersion\":\"2.0\",\"clientPlatform\":\"h5\",\"args\":\"{\\\"keyWords\\\":\\\"clubmed\\\",\\\"cityCode\\\":\\\"CITY_CODE\\\",\\\"cityName\\\":\\\"CITY_NAME\\\",\\\"checkIn\\\":\\\"CHECK_IN_DAY\\\",\\\"checkOut\\\":\\\"CHECK_OUT_DAY\\\",\\\"priceMin\\\":\\\"0\\\",\\\"priceMax\\\":\\\"-1\\\",\\\"star\\\":\\\"-1\\\",\\\"order\\\":\\\"0\\\",\\\"dir\\\":\\\"0\\\",\\\"pageNo\\\":\\\"1\\\",\\\"pageSize\\\":\\\"20\\\",\\\"labels\\\":\\\"-1\\\",\\\"offset\\\":\\\"0\\\",\\\"sellerId\\\":\\\"-1\\\",\\\"isIncludePayLater\\\":\\\"0\\\",\\\"filterByPayment\\\":\\\"0\\\",\\\"isNeedSelectData\\\":\\\"1\\\",\\\"isDisplayMultiRate\\\":\\\"1\\\",\\\"useTemplate\\\":\\\"1\\\",\\\"isShowExpedia\\\":\\\"1\\\",\\\"supportPCI\\\":\\\"1\\\",\\\"sversion\\\":\\\"6\\\",\\\"displayPackage\\\":1}\",\"h5Version\":\"0.8.55\",\"_prism_lk\":\"{\\\"_qid\\\":\\\"\\\",\\\"_skey\\\":\\\"\\\"}\"}";
    //访问详情需要的数据内容
    private String RESULT_DATA = "{\"filterByPayment\":\"0\",\"cityCode\":\"CITY_ID\",\"checkIn\":\"CHECK_IN_DAY\",\"checkOut\":\"CHECK_OUT_DAY\",\"guid\":\"\",\"from\":\"hotel-list-page\",\"surroundingByHotel\":\"0\",\"adultNum\":\"2\",\"wirelessStraightField\":\"{\\\"searchId\\\":\\\"SEARCH_ID\\\"}\",\"hid\":\"0\",\"cityName\":\"CITY_NAME\",\"shid\":\"SHOP_ID\",\"spm\":\"181.7437890.1998398719._standard_0\",\"ttid\":\"201300@travel_h5_3.1.0\",\"_preProjVer\":\"0.8.55\",\"_projVer\":\"0.8.60\",\"isIncludePayLater\":0,\"needDeal\":1,\"childrenAges\":\"\",\"isShowExpedia\":1,\"supportPCI\":1,\"sversion\":8,\"displayPackage\":1,\"hidden\":\"{\\\"straight_field\\\":{\\\"searchId\\\":\\\"SEARCH_ID\\\"}}\",\"h5Version\":\"0.8.60\"}";

    /**
     * 构造函数初始化数据
     */
    public AliHotelsItem(){
        //初始化请求头
        site.addHeader("Accept","*/*");
        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8,en;q=0.6");
        site.addHeader("User-Agent","Dalvik/2.1.0 (Linux; U; Android 6.0; KNT-UL10 Build/HUAWEIKNT-UL10)");
        //设置保存cookie
        site.setIsSave(true).setTimeOut(10000);
    }

    /**
     * 开始解析数据
     *
     * @param factory
     * @param task
     */
    public int parser(DBFactory factory,TaskEntity task) {
        //开始下载处理数据
        try{
            int cnt = toParse(factory,task);
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
     * @param task
     * @return
     */
    public int toParse(DBFactory factory, TaskEntity task){
        try{
            JSONObject param = JSON.parseObject(task.getUrl());
            //改动搜索参数data
            String dataStr = toSearchData(param);
            //获取下载的网页内容
            String src = getSrc(task,false,SEARCH_DATA_URL,dataStr);
            if(src == null || src.length() <= 0)return Constant.TASK_FAIL;
            if(src.contains("FAIL_SYS")){
                logger.info("处理搜索数据错误 -> " + src);
                //token已经失效,需要重新添加
                clean();
                return 0;
            }
            //获取到搜索数据以后,开始进行详情数据的获取
            logger.info("下载数据完成,开始处理数据.....");
            //格式化数据为json
            JSONObject bean = toSearchDetail(task,param,src);
            if(bean == null)return Constant.TASK_FAIL;
            JSONObject data = bean.getJSONObject("data");
            //开始处理第一个网页快照内容
            String snapshot = toSnapshot(data,param,"ali-hotel");
            //解析内容,并将相应的数据插入到数据库中
            return dealData(factory,task,data,snapshot);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("任务id:" + task.getId() + ",任务链接为:" + task.getUrl() + ",错误信息为:" + e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 格式化数据
     * @param param
     * @param src
     * @return
     */
    public JSONObject toSearchDetail(TaskEntity task,JSONObject param,String src) throws Exception{
        JSONObject bean = JSON.parseObject(src);
        //开始获取key
        JSONObject dataBean = bean.getJSONObject("data");
        if(dataBean == null || !dataBean.containsKey("_prism_lk"))return null;
        JSONObject lk = dataBean.getJSONObject("_prism_lk");
        if(lk == null || !lk.containsKey("_skey"))return null;
        String searchId = lk.getString("_skey");
        //获取到searchId后,开始执行详细的数据获取
        String detailData = toData(param,searchId);

        //开始处理详情数据内容
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        //获取下载的网页内容
        String html = getSrc(task,true,ALI_HOTEL_URL,detailData);
        if(html == null || html.length() <= 0)return null;
        if(html.contains("FAIL_SYS")){
            logger.info("处理详情数据内容 -> " + html);
            h5_tk = "";
            h5_tk_enc = "";
            h5_tk_time = "";
            return null;
        }
        //获取到搜索数据以后,开始进行详情数据的获取
        logger.info("下载数据完成,开始处理数据.....");
        return JSON.parseObject(html);
    }

    /**
     *
     * @param param
     * @param searchId
     * @return
     */
    public String toData(JSONObject param,String searchId){
        return RESULT_DATA.replaceAll("CITY_ID",param.getString("cityId"))
                          .replaceAll("CHECK_IN_DAY",param.getString("checkInDay"))
                          .replaceAll("CHECK_OUT_DAY",param.getString("checkOutDay"))
                          .replaceAll("SEARCH_ID",searchId)
                          .replaceAll("CITY_NAME",param.getString("cityName"))
                          .replaceAll("SHOP_ID",param.getString("shopId"))
                          ;
    }

    /**
     * 替换搜索data中的参数内容
     * @param bean
     * @return
     */
    public String toSearchData(JSONObject bean){
        return SEARCH_DATA.replaceAll("CITY_CODE",bean.getString("cityId"))
                          .replaceAll("CITY_NAME",bean.getString("cityName"))
                          .replaceAll("CHECK_IN_DAY",bean.getString("checkInDay"))
                          .replaceAll("CHECK_OUT_DAY",bean.getString("checkOutDay"))
                          ;
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
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
            return Constant.TASK_SUCESS;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("保存快照出现错误,错误信息为:" + e.getMessage());
        }
        return Constant.TASK_FAIL;
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
                logger.error("遍历房型数据内容出现错误:" + roomTypes.toJSONString() + ",错误信息为:" + e.getMessage());
            }
        }
    }
    /**
     * 下载数据
     * @param data
     * @return
     */
    public String getSrc(TaskEntity task,boolean flag,String url,String data){
        String page = null;
        int index = 0;
        while(index <= 2){
            try {
                if (h5_tk != null && h5_tk.length() > 0) {
                    site.addHeader("Cookie", "_m_h5_tk=" + h5_tk + "_" + h5_tk_time + "; _m_h5_tk_enc=" + h5_tk_enc + ";");
                }
                String link = getUrl(url,data, h5_tk, appkey);
                if(flag){task.setUrl(link);}
                page = Helper.downlaoder(task.getChannelId(),downloader,request.setUrl(link), site);
                if (page == null || page.length() <= 0) break;
                if (page.contains("FAIL_SYS")) {
                    //token已经失效,需要重新添加
                    clean();
                    //说明数据失败,需要重新抓取
                    update(site);
                    index++;

                    try{
                        Thread.sleep(3000);
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
     * @param data
     * @return
     */
    public String getUrl(String url,String data,String h5_tk,String appkey) throws Exception{
        String t = System.currentTimeMillis() + "";
        String sign = sign(h5_tk,t,appkey,data);

        return url.replaceAll("TIME_CODE",t).replaceAll("SIGN_CODE",sign) + URLEncoder.encode(data,"utf-8");
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
     * 清空数据内容
     */
    public void clean(){
        h5_tk = "";
        h5_tk_enc = "";
        h5_tk_time = "";
    }

    /**
     *
     * @param bean
     * @param type
     * @return
     */
    public String toSnapshot(JSONObject bean,JSONObject data,String type){
        //获取模板
        String template = Template.me().get(type);
        try{
            //开始替换数据
            template = template.replaceAll("#SHOP_NAME",bean.getString("name"))
                               .replaceAll("#SHOP_SCORE",bean.getString("rateScore"))
                               .replaceAll("#SHOP_REPLY_NUM",bean.getString("rateNumber"))
                               .replaceAll("#SHOP_DISTRICT",bean.getString("area"))
                               .replaceAll("#SHOP_ADDRESS",bean.getString("address"))
                               .replaceAll("#CHECK_IN_DATA",data.getString("checkInDay"))
                               .replaceAll("#CHECK_OUT_DATA",data.getString("checkOutDay"));
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

    public static void main(String[] args) throws Exception{
        AliHotelsItem item = new AliHotelsItem();
//        String data = "";
//        String appKey = "12574478";
//        String time = "1497278968824";
//
//        String sign = item.sign("0ed3989e5183741995b96c9edff262f0",time,appKey,data);
//        System.out.println(sign);


        JSONObject bean = new JSONObject();
        bean.put("cityId","110100");
        bean.put("checkInDay","2017-06-21");
        bean.put("checkOutDay","2017-06-24");
        bean.put("cityName","北京市");
        bean.put("shopId","10077759");

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannelId(1);
        taskEntity.setChannelName("阿里酒店");
        taskEntity.setRegion("北京");
        taskEntity.setSleep(1);
        taskEntity.setUrl(bean.toJSONString());

        item.parser(null,taskEntity);
    }
}
