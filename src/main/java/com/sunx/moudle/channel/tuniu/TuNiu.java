package com.sunx.moudle.channel.tuniu;

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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TuNiu implements IMonitor {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(TuNiu.class);
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        try {
            // ========================================================
            String[] listUrsls = new String[]{"http://s.tuniu.com/search_complex/pkg-sh-0-clubmed/PAGE_NUM/",
                                              "http://s.tuniu.com/search_complex/pkg-sh-0-club%20med/PAGE_NUM/"};//空格关键字
            //用于存储结果
            for (String url : listUrsls) {
                //处理数据
                dealData(factory,id,name,url,1);
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 处理数据
     * @param url
     * @param pageNum
     */
    public void dealData(DBFactory factory, Long id, String name,String url,int pageNum){
        try{
            //重新封装链接
            String link = url.replaceAll("PAGE_NUM","" + pageNum);
            //下载数据
            String src = downloader.downloader(request.setUrl(link),site);
            if(src == null || src.length() <= 0){
                logger.error("下载链接:" + url + " -> 获取到的网页源码位空...");
                return;
            }
            Page page = Page.me().bind(src);
            //抽取数据
            Node node = page.$("ul[class=thebox clearfix zizhubox] li");
            if(node == null || node.size() <= 0){
                logger.error("抽取出现错误,对应的链接为:" + link + ",抽取的规则为:ul[class=thebox clearfix zizhubox] li");
                return;
            }
            List<String> resultList = new ArrayList<>();
            for(int i=0;i<node.size();i++){
                String title = node.css(i,"span.main-tit[title]","title");
                String href = node.css(i,"a.clearfix","href");
                if(href == null)continue;

                logger.info("途牛抽取到的数据为:" + title + "\t" + href);
                resultList.add(href);
            }
            //添加到文件队列中
            save(factory,id,name,resultList);
            //处理下一页
            String next = page.css("a.page-next","href");
            if(next != null && next.length() > 0){
                //处理下一页
                dealData(factory,id,name,url,pageNum + 1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 保存数据到文件队列中
     * @param factory
     * @param id
     * @param name
     * @param urls
     */
    public void save(DBFactory factory, Long id, String name,List<String> urls){
        List<TaskEntity> tasks = new ArrayList<>();
        for (String _url : urls) {
            //封装结果数据
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUrl(_url);
            taskEntity.setChannelId(id);
            taskEntity.setChannelName(name);
            taskEntity.setCreateAt(fs.format(new Date()));
            taskEntity.setRegion(Constant.DEFALUT_REGION);
            taskEntity.setStatus(Constant.TASK_NEW);

            tasks.add(taskEntity);
        }
        //将最后一批数据提交到数据库中
        factory.insert(Constant.DEFAULT_DB_POOL,tasks);
    }
}
