package com.sunx.moudle.channel.qunar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunx.common.encrypt.MD5;
import com.sunx.constant.Constant;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.entity.CNode;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.css.CssMode;
import com.sunx.moudle.enums.ImageType;
import com.sunx.moudle.js.ScriptManager;
import com.sunx.moudle.template.Template;
import com.sunx.parser.Page;
import com.sunx.storage.DBFactory;
import com.sunx.utils.FileUtil;
import com.sunx.utils.Helper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * 根据搜索的结果封装好的链接请求
 * 请求页码,根据请求到的页面后,点可以入住的日期
 * 选择2成人0儿童，2成人1儿童，1成人1儿童
 */
@Service(id = 5, service = "com.sunx.moudle.channel.qunar.QunarSearchItem")
public class QunarSearchItem implements IParser {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(QunarSearchItem.class);

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();
    //基础的请求地址链接
    private String HOTEL_URL = "http://touch.qunar.com/hotel/hoteldetail?";
    private String ROOM_TYPE_URL = "http://touch.qunar.com/api/hotel/hoteldetail/price?type=0&sleepTask=&productId=&fromSource=";
    private String PRICE_URL = "http://touch.qunar.com/api/hotel/hoteldetail/price?type=0&desc=price&sleepTask=&productId=&fromSource=";

    private String ROOM_DETAIIL = "qunar";
    private String ROOM_DETAIL_UL_LIST = "qunar-li";

    private String BASE_URL = "http://touch.qunar.com/hotel/hoteldetail?";

    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //去哪儿解析脚本
    private String script = null;

    //css解析类
    private CssMode cssParse = CssMode.me();

    private static final int DEFAULT_PRICE_ONE_KEY_WIDTH = 11;

    /**
     * 构造函数,初始化请求头
     */
    public QunarSearchItem(){
        //请求头
        site.addHeader("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("accept-encoding","gzip, deflate, sdch, br");
        site.addHeader("user-agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Mobile Safari/537.36");

        //设置超时时间
        site.setTimeOut(10000);

        //加载解密脚本
        script = FileUtil.readerByFile(new File("conf/encrypt/qunar.js"),"utf-8");
    }

    /**
     * 开始解析数据
     *
     * @param factory
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        /** 根据请求进行数据的解析 **/
        //第一步,请求网页源码,获取到解析酒店详情的基本数据
        String page = Helper.downlaoder(downloader,request.setUrl(HOTEL_URL + task.getUrl()),site);
        //对下载到的数据进行判定
        if(page == null || page.length() <= 0){
            logger.error("下载酒店详情网页源码失败.任务链接对象为:" + request.getUrl());
            return Constant.TASK_FAIL;
        }
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        //修改请求头
        site.addHeader("Referer",request.getUrl());
        //下载到网页源码后,需要进行房型数据的下载
        String rooms = Helper.downlaoder(downloader,request.setUrl(ROOM_TYPE_URL + "&" + task.getUrl()),site);
        //对下载到的数据进行判定
        if(rooms == null || rooms.length() <= 0){
            logger.error("下载房间数据源码失败.任务链接对象为:" + request.getUrl());
            return Constant.TASK_FAIL;
        }
        //开始根据不同的方形进行数据的整理
        StringBuffer roomHtml = parse(task,rooms);
        //开始处理网页快照
        toSnapShot(factory,task,page,roomHtml);
        //返回任务状态
        return Constant.TASK_SUCESS;
    }

    /**
     * 开始针对不同的方形进行数据处理
     * @param task
     * @param rooms
     * @return
     */
    public StringBuffer parse(TaskEntity task,String rooms){
        //拼接的网页内容快照
        StringBuffer buffer = new StringBuffer();
        try{
            //格式化rooms
            JSONObject bean = JSON.parseObject(rooms);
            if(!bean.containsKey("data"))return buffer;
            JSONObject data = bean.getJSONObject("data");
            if(data == null || !data.containsKey("price"))return buffer;
            JSONArray array = data.getJSONArray("price");
            if(array == null)return buffer;
            //开始处理每种房型的数据
            for(int i=0;i<array.size();i++){
                try{
                    //现场休眠1.5s后进行数据下载
                    Thread.sleep(2000);
                    //获取第i个对象
                    JSONObject node = array.getJSONObject(i);
                    //抽取出对应的房间名称
                    String name = node.getString("name");
                    //房间描述
                    String desc = node.getString("roomDesc");
                    //最低价格
                    String lowPrice = node.getString("lowPrice");

                    //找到房型以后,开始处理对应房型下的价格问题
                    String price = Helper.downlaoder(downloader,request.setUrl(PRICE_URL + "&" + task.getUrl() + "&room=" + name),site);
                    if(price == null)continue;
                    //开始处理这个价格对应的网页内容,格式化为json对象
                    JSONObject priceBean = JSON.parseObject(price);
                    if(!priceBean.containsKey("data"))continue;
                    JSONObject priceDataBean = priceBean.getJSONObject("data");
                    if(!priceDataBean.containsKey("price"))continue;
                    JSONArray priceArray = priceDataBean.getJSONArray("price");
                    //css解析结果
                    Map<Integer,CNode> cssMap = decode(data.getString("css"));
                    //数据缓存
                    StringBuffer buff = new StringBuffer();
                    //开始对数组进行遍历处理
                    for(int j=0;j<priceArray.size();j++){
                        //获取一个方形的价格内容,用于填写网页末班
                        JSONObject obj = priceArray.getJSONObject(j);
                        //找到对应的内容问题
                        String priceHtml = findPrice(obj.getString("priceMix"),cssMap);
                        //获取对应的版块
                        String str = toTemplate(obj,priceHtml);
                        //追加到集合中
                        buff.append(str);
                    }
                    //处理模板数据
                    String str = toTemplate(name,desc,lowPrice,buff);
                    if(str == null)continue;
                    buffer.append(str);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * 填充模板
     * @param bean
     * @return
     */
    public String toTemplate(JSONObject bean,String price){
        try{
            //获取模板
            String template = Template.me().get(ROOM_DETAIL_UL_LIST);
            //开始批量替换上数据内容
            String data = template.replaceAll("#ROOM_BASIC_INFO",bean.getString("basicInfos"));
            data = data.replaceAll("#ROOM_TITLE",bean.getString("showRoomName"));
            data = data.replaceAll("#ROOM_TYPE",bean.getString("wrapperName"));
            data = data.replaceAll("#ROOM_PRICE",price);
            return data.replaceAll("#ROOM_PAY_TYPE",bean.getString("pricePayStr"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 格式化数据
     * @param name
     * @param desc
     * @param lowPrice
     * @param buffer
     * @return
     */
    public String toTemplate(String name, String desc,String lowPrice,StringBuffer buffer){
        try{
            //获取模板
            String template = Template.me().get(ROOM_DETAIIL);
            //开始批量替换上数据内容
            String data = template.replaceAll("#ROOM_NAME",name);
            data = data.replaceAll("#ROOM_DESC",desc);
            data = data.replaceAll("#ROOM_LOW_PRICE",lowPrice);
            return data.replaceAll("#ROOM_DETAIL_UL_LIST",buffer.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 构造网页快照,并保存
     * @param task
     * @param page
     * @param rooms
     */
    public void toSnapShot(DBFactory factory,TaskEntity task,String page,StringBuffer rooms){
        //找到房型加载中的标签,将该标签剔除掉,同时遍历集合,将数据添加进网页中
        String txt = removeTag(page);
        //遍历集合,开始追加数据
        txt = txt.replaceAll("#ROOM_HTML",rooms.toString());

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
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(BASE_URL + task.getUrl());
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
            page.remove(".room-loading")
                .remove("script")
                .append(".room-list","#ROOM_HTML");
            return page.html();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 根据dom树型结构,来解析对应的价格问题
     * @param html
     * @param cssMap
     * @return
     */
    public String findPrice(String html,Map<Integer,CNode> cssMap){
        //解析Dom
        Page page = Page.me().bind(html);
        //抽取价格的位数
        String css = page.css("span","style");
        if(css == null)return null;
        //从style中抽取出对应的位置偏移量
        int size = Helper.toInt(Helper.clean(css,"[^0-9]","")) / DEFAULT_PRICE_ONE_KEY_WIDTH;
        StringBuffer source = new StringBuffer();
        source.append("<span class='price' style='width:" + (11 * size) + "px'>");

        //解析价格的内容
        String[] tmps = new String[size];
        //遍历处理数据
        for(Map.Entry<Integer,CNode> entry : cssMap.entrySet()){
            //下标位置
            int index = entry.getKey();
            //对下标进行判定
            if(index <= 0 || index > size)continue;

            //开始处理节点数据
            CNode n = entry.getValue();
            //css
            String clazz = n.getClazz();
            //获取文本内容
            String txt = page.css("." + clazz);
            if(txt == null || txt.length() <= 0)continue;
            //给数组赋值
            tmps[index - 1] = txt;
        }
        //开始处理价格
        source.append("<i>");
        source.append(toString(tmps));
        source.append("</i>");
        //返回html
        return source.toString();
    }

    /**
     * 转字符串
     * @param tmps
     * @return
     */
    public String toString(String[] tmps){
        StringBuffer buffer = new StringBuffer();
        for(String s : tmps){
            buffer.append(s);
        }
        return buffer.toString();
    }

    /**
     * 处理加密问题
     */
    public Map<Integer,CNode> decode(String css){
        //解密结果数据
        StringBuffer str = new StringBuffer();

        //参数集合
        Map<String,Object> map = new HashMap<>();
        map.put("buffer",str);
        map.put("css",css);
        map.put("out",System.out);

        //开始执行脚本,获取对应的内容
        ScriptManager.me().runScript(script,map);

        //开始处理结果
        String result = str.toString();
        //进行解析css
        return cssParse.mode(result);
    }

    /**
     * 测试main函数
     * @param args
     */
    public static void main(String[] args){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannelId(5);
        taskEntity.setChannelName("去哪儿");
        taskEntity.setCheckInDate("2017-06-19");
        taskEntity.setRegion("三亚");
        taskEntity.setSleep(3);
        taskEntity.setUrl("d=123&seq=sanya_12418&checkInDate=2017-06-19&checkOutDate=2017-06-22");

        new QunarSearchItem().parser(null,taskEntity);
    }
}
