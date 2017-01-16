package com.sunx.moudle.channel.aoyou;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 1 通过给定的搜索的链接地址,下载网页源码
 * 2 重新下载到的网页源码中抽取出相应的商品详情页的链接地址
 * 3 保存抽取到的链接地址到指定的文档中
 */
public class AoYouSearch implements IMonitor {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(AoYouSearch.class);
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();
    //关键字
    private String[] words = new String[]{"clubmed"};
    //遨游搜索链接地址
    private String AOYOU_SEARCH_URL = "http://www.aoyou.com/search/b210-kKEY_WORD/";
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        //初始化站点,绑定请求头
        initSite();

        //循环遍历关键字,处理数据
        for(String word : words){
            //拼接链接地址
            String link = AOYOU_SEARCH_URL.replaceAll("KEY_WORD",word);
            //开始处理数据
            dealData(factory,id,name,link);
        }
    }

    /**
     * 处理数据
     * @param factory
     * @param id
     * @param name
     * @param link
     */
    private void dealData(DBFactory factory, Long id, String name,String link){
        try{
            //请求结果数据
            String src = downloader.downloader(request.setUrl(link),site);
            if(src == null || src.length() <= 0){
                logger.error("下载数据异常,对应的链接地址为:" + link);
                return;
            }
            Page page = Page.me().bind(src);
            //抽取数据
            Node node = page.$("div.product-box-right");
            if(node == null || node.size() <= 0){
                logger.error("遨游抽取正文页是出现错误,对应的链接为:" + link + ",抽取规则为:div.product-box-right");
                return;
            }
            List<TaskEntity> tasks = new ArrayList<>();
            //遍历数据,并将数据整合到文件队列中
            for(int i=0;i<node.size();i++){
                //抽取标题
                String title = node.css(i,"a.product-box-right-title");
                //抽取超链接
                String href = node.css(i,"a.product-box-right-title","href");
                //打印数据
                logger.info("遨游抽取到的数据为:" + title + "\t" + href);
                //入队到文件队列中
                //封装结果数据
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setUrl(link);
                taskEntity.setChannelId(id);
                taskEntity.setChannelName(name);
                taskEntity.setCreateAt(fs.format(new Date()));
                taskEntity.setShopName(null);
                taskEntity.setShopUrl(null);
                taskEntity.setTitle(title);
                taskEntity.setStatus(Constant.TASK_NEW);
                taskEntity.setRegion(Constant.DEFALUT_REGION);

                tasks.add(taskEntity);
                if(tasks.size() > 1000){
                    factory.insert(Constant.DEFAULT_DB_POOL,tasks);

                    tasks.clear();
                }
            }
            //将最后一批数据提交到数据库中
            factory.insert(Constant.DEFAULT_DB_POOL,tasks);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 绑定站点请求
     */
    private void initSite(){
        site.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        site.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
        site.addHeader("Accept-Language","zh-CN,zh;q=0.8");
    }
}
