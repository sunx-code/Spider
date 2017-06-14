package com.sunx.moudle.channel.qunar;

import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.storage.DBFactory;
import com.sunx.utils.TimerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * http://touch.qunar.com/hotel/hoteldetail?seq=sanya_12418&checkInDate=2017-05-09&checkOutDate=2017-05-11
 */
public class QunarSearch implements IMonitor {
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(QunarSearch.class);

    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("三亚", "seq=sanya_12418");
        urls.put("桂林", "seq=guilin_1999");
        urls.put("东澳岛","seq=zhuhai_4014");
        urls.put("石垣岛", "seq=ishigaki_54");
        urls.put("巴厘岛", "seq=bali_4658");
        urls.put("民丹岛", "seq=bintan_ria_20");
        urls.put("普吉岛", "seq=koh_phuket_tha_3064");
        urls.put("卡尼岛", "seq=maldives_18");
        urls.put("翡诺岛", "seq=maldives_10216");

        //开始处理,遍历集合,构造数据
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
        int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;

        List<TaskEntity> tasks = new ArrayList<>();
        for (Map.Entry<String, String> entry : urls.entrySet()) {
            String region = entry.getKey();
            String url = entry.getValue();

            if(url == null || url.trim().length() <= 0)continue;

            //遍历日期,开始进行封装链接地址
            for(String day : days) {
                for (int sleep : sleepdays) {
                    //离开时间
                    String end = TimerUtils.toDate(sdf, day, sleep);

                    String link = "d=123&" + url + "&checkInDate=" + day + "&checkOutDate=" + end;
                    logger.info("去哪儿抽取到的数据为：" + link);
                    //封装结果数据
                    TaskEntity taskEntity = new TaskEntity();
                    taskEntity.setUrl(link);
                    taskEntity.setRegion(region);
                    taskEntity.setChannelId(id);
                    taskEntity.setChannelName(name);
                    taskEntity.setCreateAt(fs.format(new Date()));
                    taskEntity.setStatus(Constant.TASK_NEW);
                    taskEntity.setSleep(sleep);
                    taskEntity.setRegion(region);
                    taskEntity.setCheckInDate(day.replaceAll("-",""));

                    tasks.add(taskEntity);
                }

                if (tasks.size() > 1000) {
                    factory.insert(Constant.DEFAULT_DB_POOL, tasks);

                    tasks.clear();
                }
            }
        }
        factory.insert(Constant.DEFAULT_DB_POOL,tasks);
    }
}
