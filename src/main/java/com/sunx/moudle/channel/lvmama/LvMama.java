package com.sunx.moudle.channel.lvmama;

import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.parser.Node;
import com.sunx.parser.Page;
import com.sunx.storage.DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * http://s.lvmama.com/scenictour/H8?keyword=club+med#list
 */
public class LvMama implements IMonitor {
    //关键字
    private String[] keys = new String[]{"club med"};
    //请求集合
    private String[] urls = new String[]{
                                        "http://s.lvmama.com/scenictour/H8?keyword=KEY_WORD#list"    //酒店 + 景点
                                        ,
//                                        "http://s.lvmama.com/freetour/H9?keyword=KEY_WORD&k=0#list" //机票 + 酒店
//                                        ,
                                        "http://s.lvmama.com/local/H8?keyword=KEY_WORD#list"        //当地游
                                        };
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(LvMama.class);
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();

    private static String SCNICTOUR_BASE_URL = "http://dujia.lvmama.com/package/";
    private static String LOCAL_BASE_URL = "http://dujia.lvmama.com/local/";
    private static String FTEE_TOUR_URL = "http://dujia.lvmama.com/freetour/";
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        //遍历每个关键字,分别进行处理
        for(String keyWord : keys){
            //对关键字进行编码
            String enWord = encode(keyWord,"UTF-8");

            //遍历请求集合,对不同的请求进行处理
            for(String searchUrl : urls){
                try{
                    //构建搜索请求连接
                    String link = searchUrl.replaceAll("KEY_WORD",enWord);

                    //处理每一个请求的数据
                    dealData(factory,id,name,link);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理数据
     * @param factory
     * @param id
     * @param name
     * @param link
     */
    public void dealData(DBFactory factory, Long id, String name,String link){
        try{
            //下载网页源码,解析其中的链接,并判断是否需要翻页
            //请求结果数据
            String src = downloader.downloader(request.setUrl(link),site);
            if(src == null || src.length() <= 0){
                logger.error("下载数据异常,对应的链接地址为:" + link);
                return;
            }
            Page page = Page.me().bind(src);
            //开始解析数据
            Node node = page.$("div[class=product-item clearfix]");
            if(node == null || node.size() <= 0){
                logger.error("解析出现错误,css为：div[class=product-item clearfix],解析的网页为:" + link);
                return;
            }
            if(link.contains("/scenictour/")){
                //遍历处理每个数据
                dealData(factory,id,name,SCNICTOUR_BASE_URL,node,"自由行");
            }else if(link.contains("/local/")){
                //遍历处理每个数据
                dealData(factory,id,name,LOCAL_BASE_URL,node,"当地游");
            }else if(link.contains("/freetour/")){
                //遍历处理每个数据
                dealData(factory,id,name,FTEE_TOUR_URL,node,"机票+酒店");
            }
            //处理下是否有下一页
            dealNext(factory,id,name,page);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 处理数据
     * @param node
     * @param factory
     * @param id
     * @param name
     */
    public void dealData(DBFactory factory, Long id, String name,String base,Node node,String type){
        //遍历数据
        for(int i=0;i<node.size();i++){
            String title = node.css(i,"h3.product-title a[title]","title");
            String href = base + node.css(i,"div.product-info[id]","id");
            String storeName = node.css(i,"div.product-hotel span");
            if(storeName == null || storeName.length() <= 0){
                storeName = "unknown";
            }
            if(href == null || href.length() <= 0)continue;
            //封装结果数据
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUrl(href);
            taskEntity.setChannelId(id);
            taskEntity.setChannelName(name);
            taskEntity.setCreateAt(fs.format(new Date()));
            taskEntity.setShopName(storeName);
            taskEntity.setTitle(title);
            taskEntity.setType(type);
            taskEntity.setStatus(Constant.TASK_NEW);
            taskEntity.setRegion(Constant.DEFALUT_REGION);

            logger.info("驴妈妈的种子链接为:" + taskEntity.toString());

            //将最后一批数据提交到数据库中
            factory.insert(Constant.DEFAULT_DB_POOL,taskEntity);
        }
    }

    /**
     * 处理下一页
     * @param page
     * @param factory
     * @param id
     * @param name
     */
    public void dealNext(DBFactory factory, Long id, String name,Page page){
        try{
            String href = page.css("a[class=nextpage]:contains(下一页)","onclick");
            //如果没有下一页了
            if(href == null || href.length() <= 0)return;
            //对链接进行处理
            String link = clean(href);

            //获取到需要处理的额数据以后,进行下载处理
            dealData(factory,id,name,link);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 请求获取到的链接地址
     * @param href
     * @return
     */
    public String clean(String href){
        return href.replaceAll("pageAjax\\('","").replaceAll("'\\)","");
    }

    /**
     * 参数编码
     * @param keyWord
     * @param encoding
     * @return
     */
    public String encode(String keyWord,String encoding){
        try{
            return URLEncoder.encode(keyWord,encoding);
        }catch (Exception e){
            e.printStackTrace();
        }
        return keyWord;
    }
}
