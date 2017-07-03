package com.sunx.moudle.channel.ali;

import com.alibaba.fastjson.JSON;
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
import com.sunx.utils.FileUtil;
import com.sunx.utils.Helper;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service(id = 7, service = "com.fosun.fonova.moudle.channel.ali.AliTripSearch")
public class AliTripSearchItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(AliTripSearchItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //默认的token
    private String h5_tk = "";//1f5ffe425695856baeba3403e9766c6a
    private String h5_tk_enc = "";//ed4dcd762a714dacdd6e4268eb592bce
    private String h5_tk_time = "";// + System.currentTimeMillis();
    //key
    private String appkey = "12574478";

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();

    private String DATA_STR = "{\"itemId\":\"ITEM_ID\",\"h5Version\":\"0.2.24\"}";
    private String DATA_DETAIL_URL = "https://acs.m.taobao.com/h5/mtop.trip.traveldetailskip.detail.get/3.0?type=originaljsonp&callback=mtopjsonp1&api=mtop.trip.traveldetailskip.detail.get&v=3.0&data=DATA_STR&ttid=201300@travel_h5_3.1.0&appKey=12574478&t=TIME_CODE&sign=SIGN_CODE";

    private String BASE_URL = "https://items.fliggy.com/item.htm?id=";

    public AliTripSearchItem(){
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
     * https://acs.m.taobao.com/h5/mtop.trip.traveldetailskip.detail.get/3.0?type=originaljsonp&callback=mtopjsonp1
     * &api=mtop.trip.traveldetailskip.detail.get&v=3.0&data=%7B%22itemId%22%3A%2240258420851%22%2C%22h5Version%22%3A%220.2.24%22%7D&ttid=201300@travel_h5_3.1.0
     * &appKey=12574478&t=1497272601446&sign=fab2c9e2640e10f75f84ecbe59a71f15
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        try{
            //id
            String id = task.getUrl();
            //获取下载的网页内容
            String src = getSrc(task);
            if(src == null || src.length() <= 0)return -1;
            if(src.contains("FAIL_SYS")){
                logger.info(src);
                //token已经失效,需要重新添加
                clean();
                return 0;
            }
            logger.info("下载数据完成,开始处理数据.....");
            //格式化数据为json
            JSONObject bean = JSON.parseObject(src);
            if(bean == null)return Constant.TASK_FAIL;
            JSONObject data = bean.getJSONObject("data");
            //解析内容,并将相应的数据插入到数据库中
            return toSnapshot(factory,task,data,id);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("任务id:" + task.getId() + ",任务链接为:" + task.getUrl() + ",错误信息为:" + e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     * 生产快照文件
     * @param factory
     * @param task
     * @param data
     * @return
     */
    public int toSnapshot(DBFactory factory,TaskEntity task,JSONObject data,String itemId){
        try{
            //封装html
            String html = toHtml(data.toJSONString());

            logger.info("开始存储网页快照数据到数据库中....");
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = task.getRegion();
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getUrl();
            String md5 = MD5.md5(id);

            String htmPath = FileUtil.createPageFile(now, task.getChannelName(), region, now, md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(htmPath), html, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(BASE_URL + itemId);
            resultEntity.setVday(vday);
            resultEntity.setPath(htmPath);

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
            return Constant.TASK_SUCESS;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("任务id:" + task.getId() + ",保存快照出现错误,错误信息为:" + e.getMessage());
        }
        return Constant.TASK_FAIL;
    }

    /**
     *
     * @param json
     * @return
     */
    public String toHtml(String json){
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html><head><meta http-equiv=Content-Type content=\"text/html;charset=utf-8\"></head>");
        buffer.append("<body>");
        buffer.append(json);
        buffer.append("</body></html>");
        return buffer.toString();
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
                task.setUrl(link);
                page = Helper.downlaoder(task.getChannelId(),downloader,request.setUrl(link), site);
                if (page == null || page.length() <= 0) break;
                if (page.contains("FAIL_SYS")) {
                    //token已经失效,需要重新添加
                    clean();
                    //说明数据失败,需要重新抓取
                    update(site);
                    index++;

                    try{
                        Thread.sleep(5000);
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
            page = page.replaceAll("mtopjsonp1\\(","")
                       .replaceAll("}\\)","}");
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
     * 清空数据内容
     */
    public void clean(){
        h5_tk = "";
        h5_tk_enc = "";
        h5_tk_time = "";
    }

    /**
     *
     * @param task
     * @return
     */
    public String getUrl(TaskEntity task,String h5_tk,String appkey) throws Exception{
        String t = System.currentTimeMillis() + "";
        String data = DATA_STR.replaceAll("ITEM_ID",task.getUrl());
        String sign = sign(h5_tk,t,appkey,data);

        return DATA_DETAIL_URL.replaceAll("DATA_STR",URLEncoder.encode(data,"utf-8")).replaceAll("TIME_CODE",t).replaceAll("SIGN_CODE",sign);
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
        return com.sunx.utils.MD5.convert(str);
    }


    public static void main(String[] args){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(123);
        taskEntity.setUrl("40258420851");
        taskEntity.setRegion(Constant.DEFALUT_REGION);
        taskEntity.setChannelName("阿里旅行-旅游度假");
        taskEntity.setChannelId(123);

        new AliTripSearchItem().parser(null,taskEntity);
    }

}
