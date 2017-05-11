package com.sunx.moudle.channel.ali;

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
 * 处理阿里就点的数据内容
 *
 * 飞猪移动端首页地址:http://h5.m.taobao.com/trip/index.html
 *
 *  //初始化需要抓取的地区
 *  datas.add(new DataEntity("三亚", "460200", "51141195"));
 *  datas.add(new DataEntity("东澳岛", "440400", "12655240"));
 *  datas.add(new DataEntity("桂林", "450300", "50864003"));
 */
public class AliHotels implements IMonitor {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(AliHotels.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //数据格式
    private String data = "{\"filterByPayment\":\"0\",\"cityCode\":\"CITY_CODE\",\"checkIn\":\"CHECK_IN_DATA\",\"checkOut\":\"CHECK_OUT_DATA\",\"guid\":\"\",\"from\":\"hotel-list-page\",\"surroundingByHotel\":\"0\",\"adultNum\":\"2\",\"wirelessStraightField\":\"{\\\"searchId\\\":\\\"\\\"}\",\"hid\":\"0\",\"cityName\":\"CITY_NAME\",\"shid\":\"SHOP_ID\",\"spm\":\"\",\"ttid\":\"201300@travel_h5_3.1.0\",\"_preProjVer\":\"0.8.40\",\"_projVer\":\"0.8.40\",\"isIncludePayLater\":0,\"needDeal\":1,\"childrenAges\":\"\",\"isShowExpedia\":1,\"supportPCI\":1,\"sversion\":7,\"displayPackage\":1,\"hidden\":\"{\\\"straight_field\\\":{\\\"searchId\\\":\\\"\\\"}}\",\"h5Version\":\"0.8.40\"}";
    //CITY_CODE
    //CHECK_IN_DATA
    //CHECK_OUT_DATA
    //CITY_NAME
    //SHOP_ID

    /**
     * 抓取种子数据
     * @param factory   数据库操作
     * @param id        渠道id
     * @param name      渠道名称
     */
    public void monitor(DBFactory factory, Long id, String name) {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("三亚", "460200-51141195");
        urls.put("东澳岛", "440400-12655240");
        urls.put("桂林","450300-50864003");

        //开始处理,遍历集合,构造数据
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
        int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;

        List<TaskEntity> tasks = new ArrayList<>();
        for (Map.Entry<String, String> entry : urls.entrySet()) {
            String region = entry.getKey();
            String param = entry.getValue();
            String[]tmps = param.split("-");

            //遍历日期,开始进行封装链接地址
            for(String day : days) {
                for (int sleep : sleepdays) {
                    //离开时间
                    String end = TimerUtils.toDate(sdf, day, sleep);

                    String link = data.replaceAll("CITY_CODE",tmps[0])
                                      .replaceAll("CHECK_IN_DATA",day)
                                      .replaceAll("CHECK_OUT_DATA",end)
                                      .replaceAll("CITY_NAME",region)
                                      .replaceAll("SHOP_ID",tmps[1]);
                    logger.info("阿里酒店抽取到的数据为：" + link);
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
