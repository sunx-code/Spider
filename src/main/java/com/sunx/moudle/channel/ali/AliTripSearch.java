package com.sunx.moudle.channel.ali;

import java.text.SimpleDateFormat;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunx.constant.Configuration;
import com.sunx.constant.Constant;
import com.sunx.downloader.*;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.moudle.proxy.IProxy;
import com.sunx.moudle.proxy.ProxyManager;
import com.sunx.parser.Node;
import com.sunx.parser.Page;
import com.sunx.storage.DBFactory;
import com.sunx.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里旅行数据解析
 *
 * # 1 从店铺的首页开始,进行搜索,找到该店铺下符合要求的数据
 https://szlrdjly.fliggy.com/search.htm

 post

 orderType:defaultSort
 viewType:grid
 keyword:club med
 lowPrice:
 highPrice:

 # 调用移动端的api接口来获取数据

 # 按照类型,日期等封装
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
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //搜索链接地址
    private String SEARCH_URL ="https://shopSHOP_ID.taobao.com/search.htm?orderType=defaultSort&viewType=grid&search=y&keyword=KEY_WORD&pageNo=PAGE_NUM&tsearch=y#anchor";
    //店铺列表
    private List<String> shopIds = new ArrayList<>();

    public AliTripSearch(){
        //设置请求头
        site.addHeader("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("accept-encoding","gzip, deflate, br");
        site.addHeader("accept-language","zh-CN,zh;q=0.8");
        site.addHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

        site.setTimeOut(1000 * 10);
        //添加需要采集的店铺地址
//        shopIds.add("109030968");
//        shopIds.add("140046115");
        initShop();
    }

    public void initShop(){
        String str = Configuration.me().getString("alitrip.shop.ids");
        if(str == null){
            logger.error("阿里旅游度假渠道对应的ids配置错误...");
            return;
        }
        String[] tmps = str.split(",");
        for(String s : tmps){
            if(s == null || s.length() <= 0)continue;
            shopIds.add(s);
        }
    }

    /**
     * 抓取种子数据
     *
     * @param factory 数据库操作
     * @param id      渠道id
     * @param name    渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        //循环遍历数据,将其中的关键字取出来,进行遍历抽取结果数据
        for (String word : keys) {
            for(String shopId : shopIds){
                //访问这个关键字的第一页数据
                get(word, 1, factory, id, name,shopId);

                //线程休眠一定时间后进行下一页的数据采集
                try {
                    logger.info("线程休眠1.5s以后进行下一个关键字的解析...");
                    Thread.sleep(1500);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    /**
     * 抽取这个关键字结果中的第一页
     *
     * @param word
     * @param pageNum
     */
    private void get(String word, int pageNum, DBFactory factory, long id, String name,String shopId) {
        try{
            //拼接链接
            String link = SEARCH_URL.replaceAll("SHOP_ID",shopId)
                                    .replaceAll("KEY_WORD",word)
                                    .replaceAll("PAGE_NUM",pageNum + "");
            logger.info("当前处理第" + pageNum + "页数据,链接地址为:" + link);
            String src = Helper.downlaoder(downloader,request.setUrl(link),site,false);
            if(src == null || src.length() <= 0){
                logger.error("下载数据异常,对应的链接地址为:" + link);
                return;
            }
            //开始解析数据,将数据封装为json
            Page page = Page.me().bind(src);
            Node node = page.$(".J_TItems > div[class^=item]");
            toDeal(node,factory,id,name,shopId);
            try{
                Thread.sleep(1500);
                //处理下一页数据
                dealNext(page,pageNum,word,factory,id,name,shopId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 处理下一页数据
     *
     * @param page
     */
    private void dealNext(Page page, int pageNum, String word, DBFactory factory, long id, String name,String shopId) {
        String next = page.css("a[class=J_SearchAsync next]","href");
        if(next != null && !"".equals(next)){
            //下载处理第二页
            get(word,  pageNum + 1, factory, id, name,shopId);
        }
    }

    /**
     * 处理json数据
     *
     * @param node
     * @param factory
     * @param id
     * @param name
     */
    private void toDeal(Node node, DBFactory factory, long id, String name,String shopId) {
        try {
            //获取结果集合
            List<TaskEntity> tasks = new ArrayList<>();
            //遍历集合,进行数据解析
            for (int i = 0; i < node.size(); i++) {
                Node tr = (Node)node.$(i,"[class^=item][data-id]");
                for(int j=0;j<tr.size();j++){
                    //获取标签对应的id
                    String pid = tr.css(j,"dl","data-id");

                    //封装结果数据
                    TaskEntity taskEntity = new TaskEntity();
                    taskEntity.setUrl(pid);
                    taskEntity.setRegion(Constant.DEFALUT_REGION);
                    taskEntity.setChannelId(id);
                    taskEntity.setChannelName(name);
                    taskEntity.setCreateAt(fs.format(new Date()));
                    taskEntity.setStatus(Constant.TASK_NEW);

                    tasks.add(taskEntity);

                    System.out.println(taskEntity.getUrl());
                }
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

    public static void main(String[] args){
        new AliTripSearch().monitor(null,9l,"阿里旅行-旅游度假");
    }

}
