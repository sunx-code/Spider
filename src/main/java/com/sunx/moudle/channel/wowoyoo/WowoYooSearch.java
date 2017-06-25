package com.sunx.moudle.channel.wowoyoo;

import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.utils.TimerUtils;
import com.sunx.storage.DBFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class WowoYooSearch implements IMonitor {
    //链接地址
    private String SEARCH_URL = "https://wowoyoo.com/clubmed/cau";
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 开始导入
     * @param factory
     * @param id
     * @param name
     */
    public void monitor(DBFactory factory,Long id,String name) {
        Map<String, String> regions = new HashMap<String, String>();
        regions.put("三亚", "sanya");
        regions.put("桂林", "guilin");
        regions.put("东澳度假村", "dongao");
        regions.put("北大壶", "beidahu");
//        regions.put("亚布力", "yabuli");
        regions.put("石垣岛", "kabira");
        regions.put("佐幌", "sahoro");
        regions.put("巴厘岛", "bali");
        regions.put("民丹岛", "bintan");
        regions.put("珍拉丁湾", "cherating");
        regions.put("普吉岛", "phuket");
        regions.put("卡尼岛", "kani");
        regions.put("翡诺岛", "finolhu");
        regions.put("爱必浓", "albion");
        regions.put("康隆尼角", "canonniers");

        // =====================================================
        List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
        String birthday = TimerUtils.birthday(sdf,10);
        int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;
        int[] number_of_adult = Constant.NUMBER_OF_ADULT;
        int[] number_of_children = Constant.NUMBER_OF_CHILDREN;

        // =====================================================
        List<TaskEntity> tasks = new ArrayList<TaskEntity>();

        // =====================================================
        for (Map.Entry<String, String> region_arg : regions.entrySet()) {
            // =====================================================
            String region = region_arg.getKey();
            String regionParam = region_arg.getValue();

            for (String checkInDate : days) {// 爬取天
                for (int sleepDay : sleepdays) {// 几晚
                    for (int adult_num : number_of_adult) {// 成人
                        for (int children_num : number_of_children) {// 儿童
                            //只处理1+1,2+0,2+1 => adult_num + children_num > 1
                            if(adult_num + children_num <= 1)continue;

                            String type = "";
                            if(adult_num == 2 && children_num == 1){
                                type = "2成人1儿童";
                            }else if(adult_num == 2 && children_num == 0){
                                type = "2成人";
                            }else if(adult_num == 1 && children_num == 1){
                                type = "1成人1儿童";
                            }

                            //构建任务对象
                            TaskEntity taskEntity = new TaskEntity();
                            taskEntity.setAdultNum(adult_num);
                            taskEntity.setChannelId(id);
                            taskEntity.setChannelName(name);
                            taskEntity.setCheckInDate(checkInDate);
                            taskEntity.setChildNum(children_num);
                            taskEntity.setCreateAt(fs.format(new Date()));
                            taskEntity.setPeopleType(type);
                            taskEntity.setRegion(region);
                            taskEntity.setRegionParam(regionParam);
                            taskEntity.setSleep(sleepDay);
                            taskEntity.setTryTime(0);
                            taskEntity.setUrl(SEARCH_URL);
                            taskEntity.setBirthday(birthday);
                            taskEntity.setStatus(Constant.TASK_NEW);

                            tasks.add(taskEntity);

                            if(tasks.size() > 1000){
                                factory.insert(Constant.DEFAULT_DB_POOL,tasks);

                                tasks.clear();
                            }
                        }
                    }
                }
            }
        }
        //将最后一批数据提交到数据库中
        factory.insert(Constant.DEFAULT_DB_POOL,tasks);
    }
}