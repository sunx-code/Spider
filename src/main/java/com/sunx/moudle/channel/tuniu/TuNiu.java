package com.sunx.moudle.channel.tuniu;

import com.sunx.constant.Constant;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.parser.Node;
import com.sunx.parser.Page;
import com.sunx.storage.DBConfig;
import com.sunx.storage.DBFactory;
import com.sunx.storage.pool.DuridPool;
import com.sunx.utils.Helper;
import com.sunx.utils.TimerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 通过拼接链接来够着对象的种子链接
 * 1 根据关键字,入住晚数来拼接种子链接
 * 2 根据给定的过滤规则来进行数据的过滤(保留符合过滤规则的数据)
 */
public class TuNiu implements IMonitor {
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(TuNiu.class);
    //搜索链接地址
    private String searchUrl = "http://s.tuniu.com/search_complex/whole-all-";
    //搜索关键字
    private String[] keys = new String[]{"clubmed","club+med"};
    //过滤规则
    private String[] filters = new String[]{"不含大交通","不含往返大交通"};
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //请求地址
    private Request request = new Request();
    //站点数据
    private Site site = new Site();

    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        //开始处理,遍历集合,构造数据
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
        int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;

        List<TaskEntity> tasks = new ArrayList<>();
        for(String key : keys){
            for (int sleep : sleepdays) {
                //拼接链接地址
                String link = searchUrl + (sleep + 1) + "-" + key;

                //开始进行下载该链接地址,通过过滤规则来过滤需要保留下来的数据
                try{
                    //开始下载搜索页数据
                    String src = Helper.downlaoder(downloader,request.setUrl(link),site.setTimeOut(10000));
                    //对下载到的内容进行数据的判定
                    if(src == null || src.length() <= 0){
                        logger.error("下载失败,对应的链接地址为:" + link);
                        continue;
                    }
                    if(src.contains("验证码")){
                        logger.error("下载失败,ip已经被封...");
                        continue;
                    }
                    //开始解析数据
                    Page page = Page.me().bind(src);
                    //开始抽取其中的链接地址和标题
                    Node node = page.$(".theinfo > a");
                    for(int i=0;i<node.size();i++){
                        //抽取title
                        String title = node.css(i,"span.main-tit","title");
                        //过滤数据
                        if(!isFilter(title))continue;
                        //获取href
                        String href = node.css(i,"a","href");

                        //开始封装任务对象
                        toTask(factory,id,name,days,sleep,href,tasks);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        factory.insert(Constant.DEFAULT_DB_POOL,tasks);
    }

    /**
     * 封装任务对象
     * @param sleep
     * @param link
     */
    public void toTask(DBFactory factory, Long id, String name,List<String> days,int sleep,String link,List<TaskEntity> tasks){
        logger.info("途牛抽取到的数据为：" + link);
        //遍历日期,开始进行封装链接地址
        for(String day : days) {
            //离开时间
            String end = TimerUtils.toDate(sdf, day, sleep);

            //封装结果数据
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUrl(link);
            taskEntity.setChannelId(id);
            taskEntity.setChannelName(name);
            taskEntity.setCreateAt(fs.format(new Date()));
            taskEntity.setStatus(Constant.TASK_NEW);
            taskEntity.setSleep(sleep);
            taskEntity.setRegion(Constant.DEFALUT_REGION);
            taskEntity.setCheckInDate(day);
            taskEntity.setCheckOutDate(end);

            tasks.add(taskEntity);

            logger.info(taskEntity.getUrl());
        }

        if (tasks.size() > 1000) {
            factory.insert(Constant.DEFAULT_DB_POOL, tasks);

            tasks.clear();
        }
    }

    /**
     * 过滤数据
     * @param title
     * @return
     */
    public boolean isFilter(String title){
        boolean flag = false;
        if(title == null)return flag;
        for(String filter : filters){
            if(title.contains(filter)){
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static void main(String[] args){
        //初始化数据库连接池
        DBConfig config = new DBConfig(Constant.DB_CONFIG_FILE);
        DuridPool.me().build(config);

        TuNiu tuNiu = new TuNiu();
        tuNiu.monitor(null,6l,"途牛");
    }
}
