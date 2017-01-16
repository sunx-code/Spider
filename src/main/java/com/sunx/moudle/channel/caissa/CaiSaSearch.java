package com.sunx.moudle.channel.caissa;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.util.Set;

/**
 *  凯撒搜索数据
 */
public class CaiSaSearch implements IMonitor {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(CaiSaSearch.class);
    //下载器
    private Downloader downloader = new HttpClientDownloader();
    //站点对象
    private Site site = new Site();
    //请求对象
    private Request request = new Request();
    //关键字
    private String[] words = new String[]{"clubmed","club%20med"};
    //搜索请求
    private String SEARCH_URL = "http://dj.caissa.com.cn/search/search_list.php?q=KEY_WORD&page_type=2&source=2&page=%2Fsearch";
    //链接base
    private String BASE_LINK = "http://dj.caissa.com.cn";
    //异步加载的请求,获取可以入住的日期数据
    private String AJAX_URL = "http://dj.caissa.com.cn/ajax/calendar.php?_type=&p_id=";
    //日期格式化工具
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
    //详情页的对应的访问链接
    private String DETAIL_URL = "http://dj.caissa.com.cn/reserve/reserve.php?pro_id=PRO_ID&pid=P_ID&nomal=NOMAL_DATA&_type=&child=1&adults=ADULT_NUM&childrens=CHILD_TYPE&ages=0-11&kc=999&presaleNum=&last_time=LAST_DATE&last_p=2&start_date=CTRIP_DATE";
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

        //遍历关键字进行数据处理
        for(String word : words){
            //拼接请求连接地址
            String link = SEARCH_URL.replaceAll("KEY_WORD",word);

            //开始处理当前这个页面的数据
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
            //解析当前页面的数据
            Node node = page.$("div[class=chanpin_ul clear]");
            if(node == null || node.size() <= 0){
                logger.error("抽取数据出现错误,链接地址为:" + link + ",对应的抽取规则为:div[class=chanpin_ul clear]");
                return;
            }
            //循环遍历,抽取相应的数据
            for(int i=0;i<node.size();i++){
                //抽取相应的链接地址
                String href = node.css(i,"h3.ul_h3 a","href");
                String title = node.css(i,"h3.ul_h3 a");

                if(href == null)continue;
                if(!href.startsWith("http")){
                    href = BASE_LINK + href;
                }
                //打印数据
                logger.info("凯撒搜索抽取到的数据为:" + title + "\t" + href);

                //开始进行后续的处理
                dealDetailData(factory,id,name,title,href);

                //线程休眠一定时间后继续
                try{
                    Thread.sleep(1500);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 1 下载link对应的网页源码
     * 2 抽取其中的pro_id
     * 3 抽取其中的pid
     * 4 抽取其中的nomal
     * 5 拼接post请求,找到对应的入住日期的数据
     * 对应的post请求地址为: http://dj.caissa.com.cn/ajax/calendar.php?_type=&p_id=FIT0004286   get方式也行
     * 抽取出其中对应的字段
     * last_time:20161021           last_date 的值替换/为空
     * start_date:2016/10/26        tripDate  的值替换-为/
     *
     * 获取上面所有的参数以后,拼接请求,保存到对应的文件列表中即可
     * @param factory
     * @param id
     * @param name
     * @param title
     * @param href
     */
    private void dealDetailData(DBFactory factory, Long id, String name,String title,String href){
        try{
            //请求结果数据
            String src = downloader.downloader(request.setUrl(href),site);
            if(src == null || src.length() <= 0){
                logger.error("下载数据异常,对应的链接地址为:" + href);
                return;
            }
            Page page = Page.me().bind(src);
            //开始抽取相应的数据
            String proId = page.css("#J_Calendar","pro_id");
            String pid = page.css("#pid","value");
            String nomal = page.css("#J_Calendar","nomal");

            //对抽取到的数据进行判定
            if(proId == null || proId.length() <= 0){
                logger.error("抽取到的数据为空,对应的链接为:" + href);
                return;
            }
            //加载ajax请求,获取json数据,并封装对应的数据
            String jsonUrl = AJAX_URL + proId;
            String json = downloader.downloader(request.setUrl(jsonUrl),site);
            if(src == null || src.length() <= 0){
                logger.error("下载数据异常,对应的链接地址为:" + jsonUrl);
                return;
            }
            //格式化处理结果数据
            JSONObject bean = JSON.parseObject(json);

            //处理数据
            dealJson(factory,proId,pid,nomal,bean,id,name,title);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 处理抽取到的结果数据
     *
     * {
     confirmType: "2",
     id: "3bac42ff0f674877897aa6f447a1faac",
     isOnlyLove: "0",
     is_show: "1",
     last_date: "2016/10/21",
     lastdate_leave: "2016/10/26",
     live_fly: {
     0: {
     period_time: "2016/10/23",
     sort: "0",
     status: "1",
     title: "北京|桂林"
     },
     1: {
     period_time: "2016/10/23,2016/10/26",
     sort: "1",
     status: "2",
     title: "桂林酒店"
     },
     2: {
     period_time: "2016/10/26",
     sort: "2",
     status: "1",
     title: "桂林|北京"
     }
     },
     minSignNum: "1",
     min_price: "4873.00",
     originalPrice: "",
     presaleNum: "",
     presalePrice: "",
     tripDate: "2016-10-23",
     dkc: "1"
     }
     * @param factory
     * @param proId
     * @param pid
     * @param nomal
     * @param bean
     * @param id
     * @param name
     * @param title
     */
    private void dealJson(DBFactory factory,String proId,String pid,String nomal,JSONObject bean, Long id, String name,String title){
        //直接解析对应的json数据
        Set<String> sets = bean.keySet();
        List<TaskEntity> tasks = new ArrayList<TaskEntity>();
        for(String key : sets){
            //获取key对应的object
            JSONObject obj = bean.getJSONObject(key);

            //抽取其中对应的last_date和tripDate
            String lastDate = obj.getString("last_date");
            String ctripDate = obj.getString("tripDate");

            if(lastDate == null){
                lastDate = sdf.format(new Date());
            }
            lastDate = lastDate.replaceAll("/","");

            if(ctripDate == null){
                ctripDate = sdf2.format(new Date());
            }
            ctripDate = ctripDate.replaceAll("-","");

            //所有的参数都有了,可以进行封装存储数据了
            //1成人1儿童
            enqueue(tasks,proId,pid,nomal,lastDate,ctripDate,id,name,title,1,1,"1成人1儿童");
            //2成人1儿童
            enqueue(tasks,proId,pid,nomal,lastDate,ctripDate,id,name,title,2,1,"2成人1儿童");
            //2成人0儿童
            enqueue(tasks,proId,pid,nomal,lastDate,ctripDate,id,name,title,2,0,"2成人");

            //数据容量是否已经超出上限
            if(tasks.size() > 1000){
                factory.insert(Constant.DEFAULT_DB_POOL,tasks);

                tasks.clear();
            }
        }
        //将最后一批数据提交到数据库中
        factory.insert(Constant.DEFAULT_DB_POOL,tasks);
    }

    /**
     * 将数据写入到文件中
     * @param proId
     * @param pid
     * @param nomal
     * @param lastDate
     * @param ctripDate
     * @param name
     */
    private void enqueue( List<TaskEntity> tasks,String proId,String pid,String nomal,String lastDate,String ctripDate,Long id, String name,String title,int adultNum,int childNum,String peopleType){
        String url = DETAIL_URL.replaceAll("PRO_ID",proId)
                               .replaceAll("P_ID", pid)
                               .replaceAll("NOMAL_DATA", nomal)
                               .replaceAll("ADULT_NUM", "" + adultNum)
                               .replaceAll("CHILD_TYPE","" + childNum)
                               .replaceAll("LAST_DATE", lastDate)
                               .replaceAll("CTRIP_DATE",ctripDate);
        //封装好的链接为：
        logger.info("凯撒抽取的数据封装好的链接为:" + url);
        //将封装好的链接插入到文件队列中
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUrl(url);
        taskEntity.setChannelId(id);
        taskEntity.setChannelName(name);
        taskEntity.setCreateAt(fs.format(new Date()));
        taskEntity.setShopName(null);
        taskEntity.setShopUrl(null);
        taskEntity.setTitle(title);
        taskEntity.setCheckInDate(ctripDate);
        taskEntity.setAdultNum(adultNum);
        taskEntity.setChildNum(childNum);
        taskEntity.setPeopleType(peopleType);
        taskEntity.setStatus(Constant.TASK_NEW);
        taskEntity.setRegion(Constant.DEFALUT_REGION);

        tasks.add(taskEntity);
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