package com.sunx.moudle.channel.ali;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sunx.constant.Constant;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.qunar.QunarSearchItem;
import com.sunx.storage.DBFactory;
import com.sunx.utils.MD5;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;

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
    private static final Logger logger = LoggerFactory.getLogger(QunarSearchItem.class);

    private Downloader downloader = new HttpClientDownloader();
    private Site site = new Site();
    private Request request = new Request();

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
        site.setIsSave(true);
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
            //格式化数据为json
            JSONObject bean = JSON.parseObject(src);
            if(bean == null)return -1;
            JSONObject data = bean.getJSONObject("data");
            int cnt = -1;
            //解析内容,并将相应的数据插入到数据库中
            cnt += dealData(factory,task,data);
            return cnt;
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
    public int dealData(DBFactory factory,TaskEntity entity,JSONObject data){



        return 0;
    }
    /**
     * 下载数据
     * @param task
     * @return
     */
    public String getSrc(TaskEntity task){
        String page = null;
        int index = 0;
        while(true){
            try {
                if (h5_tk != null && h5_tk.length() > 0) {
                    site.addHeader("Cookie", "_m_h5_tk=" + h5_tk + "_" + h5_tk_time + "; _m_h5_tk_enc=" + h5_tk_enc + ";");
                }
                String link = getUrl(task, h5_tk, appkey);
                Request request = new Request(link);
                page = downloader.downloader(request, site);
                if (page == null || page.length() <= 0) break;
                if (page.contains("FAIL_SYS")) {
                    //说明数据失败,需要重新抓取
                    update(site);
                    index++;

                    if (index >= 2) {
                        break;
                    }
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
}
