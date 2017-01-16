package com.sunx.moudle.channel.ctrip;

import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.utils.TimerUtils;
import com.sunx.storage.DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class CTrip implements IMonitor {
	//格式化日期数据
	private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	//格式化日期
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	//日志记录
	private static final Logger logger = LoggerFactory.getLogger(CTrip.class);

	/**
	 * 抓取种子数据
	 * @param factory   数据库操作
	 * @param id        渠道id
	 * @param name      渠道名称
	 */
	public void monitor(DBFactory factory, Long id, String name) {
		Map<String, String> urls = new LinkedHashMap<>();
		urls.put("三亚", "http://hotels.ctrip.com/hotel/4037056.html");
		urls.put("桂林", "http://hotels.ctrip.com/hotel/437385.html");
		urls.put("东澳岛", "http://hotels.ctrip.com/hotel/900272.html");
		urls.put("北大壶", "");
		urls.put("亚布力", "http://hotels.ctrip.com/hotel/436418.html");
		urls.put("石垣岛", "http://hotels.ctrip.com/international/1573404.html");
		urls.put("佐幌", "");
		urls.put("巴厘岛", "http://hotels.ctrip.com/international/1609709.html");
		urls.put("民丹岛", "http://hotels.ctrip.com/international/1867428.html");
		urls.put("珍拉丁湾", "http://hotels.ctrip.com/international/5265794.html");
		urls.put("普吉岛", "http://hotels.ctrip.com/international/1574459.html");
		urls.put("卡尼岛", "http://hotels.ctrip.com/international/1573832.html");
		urls.put("翡诺岛", "http://hotels.ctrip.com/international/5155248.html");
		urls.put("爱必浓", "http://hotels.ctrip.com/international/1562490.html");
		urls.put("康隆尼角", "http://hotels.ctrip.com/international/5265808.html");

		// ========================================================
		List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
		int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;

		List<TaskEntity> tasks = new ArrayList<>();
		for (Entry<String, String> entry : urls.entrySet()) {
			String region = entry.getKey();
			String url = entry.getValue();

			if(url == null || url.trim().length() <= 0)continue;

			//遍历日期,开始进行封装链接地址
			for(String day : days){
				for(int sleep : sleepdays){
					//离开时间
					String end = TimerUtils.toDate(sdf, day, sleep);

					String link = url + "?startDate=" + day + "&depDate=" + end;
					logger.info("携程抽取到的数据为：" + link);
					//封装结果数据
					TaskEntity taskEntity = new TaskEntity();
					taskEntity.setUrl(link);
					taskEntity.setRegion(region);
					taskEntity.setChannelId(id);
					taskEntity.setChannelName(name);
					taskEntity.setCreateAt(fs.format(new Date()));
					taskEntity.setStatus(Constant.TASK_NEW);
					taskEntity.setSleep(sleep);
					taskEntity.setRegion(Constant.DEFALUT_REGION);
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
