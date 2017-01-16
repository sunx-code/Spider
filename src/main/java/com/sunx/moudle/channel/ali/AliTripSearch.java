package com.sunx.moudle.channel.ali;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.storage.DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里旅行数据解析
 */
public class AliTripSearch implements IMonitor {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(AliTripSearch.class);
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();
    //关键字
    private String[] keys = new String[]{"clubmed", "club%20med"};
    //请求集合
    private String SEARCH_URL = "https://s.alitrip.com/vacation/list.htm?cq=全国&mq=KEY_WORD&jumpTo=PAGE_NUM&itemOrderEnum=DEFAULT&orderDirEnum=DESC&searchConditions=&playType=0&_input_charset=utf8&format=json";
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 抓取种子数据
     *
     * @param factory 数据库操作
     * @param id      渠道id
     * @param name    渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        site.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("accept-encoding", "gzip, deflate, sdch, br");
        site.addHeader("accept-language", "zh-CN,zh;q=0.8");
        site.addHeader("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");

        //循环遍历数据,将其中的关键字取出来,进行遍历抽取结果数据
        for (String word : keys) {
            //访问这个关键字的第一页数据
            get(word, 1, factory, id, name);

            //线程休眠一定时间后进行下一页的数据采集
            try {
                logger.info("线程休眠1.5s以后进行下一个关键字的解析...");
                Thread.sleep(1500);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * 抽取这个关键字结果中的第一页
     *
     * @param word
     * @param pageNum
     */
    private void get(String word, int pageNum, DBFactory factory, long id, String name) {
        try {
            //拼接链接
            String link = SEARCH_URL.replaceAll("KEY_WORD", word).replaceAll("PAGE_NUM", "" + pageNum);
            logger.info("当前处理第" + pageNum + "页数据,链接地址为:" + link);
            //请求结果数据
            String src = downloader.downloader(request.setUrl(link), site);
            if (src == null || src.length() <= 0) {
                logger.error("下载数据异常,对应的链接地址为:" + link);
                return;
            }
            //开始解析数据,将数据封装为json
            JSONObject bean = JSON.parseObject(src);
            dealJSON(bean, factory, id, name);
            try {
                Thread.sleep(1500);
                //处理下一页数据
                dealNext(bean, word, factory, id, name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理下一页数据
     *
     * @param bean
     */
    private void dealNext(JSONObject bean, String word, DBFactory factory, long id, String name) {
        JSONObject page = bean.containsKey("page") ? bean.getJSONObject("page") : null;
        if (page == null) return;
        int total = page.containsKey("totalPage") ? page.getIntValue("totalPage") : 0;
        int current = page.containsKey("currentPage") ? page.getIntValue("currentPage") : 0;

        if (total == 0 || current == 0) return;
        if (current >= total) return;
        //下载处理第二页
        get(word, current + 1, factory, id, name);
    }

    /**
     * 处理json数据
     *
     * @param bean
     * @param factory
     * @param id
     * @param name
     */
    private void dealJSON(JSONObject bean, DBFactory factory, long id, String name) {
        try {
            //获取结果集合
            JSONArray array = bean.containsKey("itemList") ? bean.getJSONArray("itemList") : null;
            if (array == null) {
                logger.error("抽取到的jsonArray集合为null..");
                return;
            }

            List<TaskEntity> tasks = new ArrayList<>();
            //遍历集合,进行数据解析
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);

                String title = obj.containsKey("title") ? obj.getString("title") : null;
                String link = obj.containsKey("link") ? obj.getString("link") : null;
                String shopName = obj.containsKey("storeName") ? obj.getString("storeName") : null;
                String shopUrl = obj.containsKey("storeLink") ? obj.getString("storeLink") : null;

                if (link == null) continue;
                //加上协议
                link = "http:" + link;
                shopUrl = "http:" + shopUrl;

                logger.info("阿里旅行-旅游度假抽取到的数据为：" + title + "\t" + link);

                //封装结果数据
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setUrl(link);
                taskEntity.setChannelId(id);
                taskEntity.setChannelName(name);
                taskEntity.setCreateAt(fs.format(new Date()));
                taskEntity.setShopName(shopName);
                taskEntity.setShopUrl(shopUrl);
                taskEntity.setTitle(title);
                taskEntity.setStatus(Constant.TASK_NEW);
                taskEntity.setRegion(Constant.DEFALUT_REGION);

                tasks.add(taskEntity);
                if (tasks.size() > 1000) {
                    factory.insert(Constant.DEFAULT_DB_POOL, tasks);

                    tasks.clear();
                }
            }
            //将最后一批数据提交到数据库中
            factory.insert(Constant.DEFAULT_DB_POOL, tasks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
