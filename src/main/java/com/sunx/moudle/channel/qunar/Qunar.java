package com.sunx.moudle.channel.qunar;

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

public class Qunar implements IMonitor {
	//格式化日期数据
	private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	//格式化日期
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	//日志记录
	private static final Logger logger = LoggerFactory.getLogger(Qunar.class);

	/**
	 * 抓取种子数据
	 * @param factory   数据库操作
	 * @param id        渠道id
	 * @param name      渠道名称
	 */
	public void monitor(DBFactory factory, Long id, String name) {
		Map<String, String> urls = new LinkedHashMap<>();
		urls.put("三亚",
				"http://hotel.qunar.com/city/sanya/dt-12418/?cityurl=sanya&HotelSEQ=sanya_12418&sgroup=1&roomNum=1");
		urls.put("桂林",
				"http://hotel.qunar.com/city/guilin/dt-1999/?cityurl=guilin&HotelSEQ=guilin_1999&sgroup=1&roomNum=1");
		urls.put("东澳岛",
				"http://hotel.qunar.com/city/zhuhai/dt-4014/?cityurl=zhuhai&HotelSEQ=zhuhai_4014&sgroup=1&roomNum=1");
		urls.put("石垣岛",
				"http://hotel.qunar.com/city/ishigaki/dt-54/?cityurl=ishigaki&HotelSEQ=ishigaki_54&sgroup=1&roomNum=1");
		urls.put("巴厘岛", "http://hotel.qunar.com/city/bali/dt-4658/?cityurl=bali&HotelSEQ=bali_4658&sgroup=1&roomNum=1");
		urls.put("民丹岛",
				"http://hotel.qunar.com/city/bintan_ria/dt-20/?cityurl=bintan_ria&HotelSEQ=bintan_ria_20&sgroup=1&roomNum=1");
		urls.put("普吉岛",
				"http://hotel.qunar.com/city/koh_phuket_tha/dt-3064/?cityurl=koh_phuket_tha&HotelSEQ=koh_phuket_tha_3064&sgroup=1&roomNum=1");
		urls.put("卡尼岛",
				"http://hotel.qunar.com/city/maldives/dt-18/?cityurl=maldives&HotelSEQ=maldives_18&sgroup=-1&roomNum=1");
		urls.put("翡诺岛",
				"http://hotel.qunar.com/city/maldives/dt-10216/?cityurl=maldives&HotelSEQ=maldives_10216&sgroup=-1&roomNum=1");

//		http://hotel.qunar.com/city/bintan_ria/dt-20/?#cityurl=bintan_ria&HotelSEQ=bintan_ria_20&sgroup=-1&roomNum=1&from=hoteldetail&fromDate=2017-01-21&toDate=2017-01-22

		urls.put("爱必浓", "");
		urls.put("康隆尼角", "");
		urls.put("北大壶", "");
		urls.put("亚布力", "");
		urls.put("珍拉丁湾", "");
		urls.put("佐幌", "");

		// ========================================================
		List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
		int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;

		List<TaskEntity> tasks = new ArrayList<>();
		for (Entry<String, String> entry : urls.entrySet()) {
			String region = entry.getKey();
			String url = entry.getValue();

			if(url == null || url.trim().length() <= 0)continue;

			//遍历日期,开始进行封装链接地址
			for(String day : days) {
				for (int sleep : sleepdays) {
					//离开时间
					String end = TimerUtils.toDate(sdf, day, sleep);

					String link = url + "&from=hoteldetail&fromDate=" + day + "&toDate=" + end;
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
