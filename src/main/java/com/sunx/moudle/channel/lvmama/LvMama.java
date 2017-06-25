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
import com.sunx.utils.Helper;
import com.sunx.utils.TimerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * http://s.lvmama.com/scenictour/H8?keyword=club+med#list
 */
public class LvMama implements IMonitor {
    //关键字
    private String[] keys = new String[]{"club med","clubmed"};
    //请求集合
    private String[] urls = new String[]{
                                        "http://s.lvmama.com/scenictour/H8?keyword=KEY_WORD#list"   //酒店 + 景点
                                        };
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(LvMama.class);
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();

    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //缓存,用于去重
    private Set<String> cache = new HashSet<>();

    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        //开始处理,遍历集合,构造数据
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
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
                    dealData(factory,id,days,name,link);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        cache.clear();
    }

    /**
     * 处理数据
     * @param factory
     * @param id
     * @param name
     * @param link
     */
    public void dealData(DBFactory factory, Long id,List<String> days, String name,String link){
        try{
            //下载网页源码,解析其中的链接,并判断是否需要翻页
            //请求结果数据
            String src = Helper.downlaoder(downloader,request.setUrl(link),site,false);
            if(src == null || src.length() <= 0){
                logger.error("下载数据异常,对应的链接地址为:" + link);
                return;
            }
            Page page = Page.me().bind(src);
            //开始解析数据
            Node node = page.$("#route-list div.product-item");
            if(node == null || node.size() <= 0){
                logger.error("解析出现错误,css为：#route-list div.product-item,解析的网页为:" + link);
                return;
            }
            //遍历处理每个数据
            dealData(factory,id,days,name,node);
            //处理下是否有下一页
            dealNext(factory,id,days,name,page);
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
    public void dealData(DBFactory factory, Long id,List<String> days, String name,Node node){
        List<TaskEntity> tasks = new ArrayList<>();
        //遍历数据
        for(int i=0;i<node.size();i++){
            String href = node.css(i,"h3.product-title > a","href");
            if(href == null || href.length() <= 0)continue;
            if(!cache.add(href))continue;

            for(String checkInDay : days){
                //封装结果数据
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setUrl(href);
                taskEntity.setChannelId(id);
                taskEntity.setChannelName(name);
                taskEntity.setCreateAt(fs.format(new Date()));
                taskEntity.setStatus(Constant.TASK_NEW);
                taskEntity.setRegion(Constant.DEFALUT_REGION);
                taskEntity.setCheckInDate(checkInDay);

                tasks.add(taskEntity);
            }
            logger.info("驴妈妈的种子链接为:" + href);
        }
        factory.insert(Constant.DEFAULT_DB_POOL, tasks);
    }

    /**
     * 处理下一页
     * @param page
     * @param factory
     * @param id
     * @param name
     */
    public void dealNext(DBFactory factory, Long id,List<String> days, String name,Page page){
        try{
            String href = page.css("a[class=nextpage]:contains(下一页)","onclick");
            //如果没有下一页了
            if(href == null || href.length() <= 0)return;
            //对链接进行处理
            String link = clean(href);

            //获取到需要处理的额数据以后,进行下载处理
            dealData(factory,id,days,name,link);
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

    public static void main(String[] args){
        new LvMama().monitor(null,10l,"驴妈妈");
    }
}
