package com.sunx.moudle.channel.clubmed;

import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.channel.IMonitor;
import com.sunx.utils.Helper;
import com.sunx.utils.TimerUtils;
import com.sunx.storage.DBFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class ClubMed implements IMonitor {
	//格式化日期
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	//格式化日期数据
	private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 抓取种子数据
	 * @param factory   数据库操作
	 * @param id        渠道id
	 * @param name      渠道名称
	 */
	public void monitor(DBFactory factory, Long id, String name) {
		// =====================================================
		// =====================================================
		List<String> days = TimerUtils.initDay(sdf, Constant.CRAWLING_RANGE_DAYS);
		int[] sleepdays = Constant.CRAWLING_SLEEP_DAYS;
		int[] number_of_adult = Constant.NUMBER_OF_ADULT;
		int[] number_of_children = Constant.NUMBER_OF_CHILDREN;
		String birthday = TimerUtils.birthday(sdf,6);

		String people_arg = "&nbParticipants="; // 总共几人
		String children_num_arg = "&nbEnfants="; // 儿童几人
		String children_birthday_arg = "&dateDeNaissance="; // 儿童生日&dateDeNaissance=2010/01/01,2010/01/02
		String date_arg = "&dateDeDebut="; // 入住日期
		String sleepdays_arg = "&dureeSejour=";// 几晚
		// =====================================================
		Map<String, String> urls = new LinkedHashMap<>();
		urls.put("三亚", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=SAYC");
		urls.put("桂林", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=GUIC");
		urls.put("东澳岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=DAOC");
		urls.put("北大壶","https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=BEIC");
		urls.put("亚布力", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=YABC");

		urls.put("石垣岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=KABC");
		urls.put("佐幌", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=SAOC");

		urls.put("巴厘岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=BALC");
		urls.put("民丹岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=RBIC");

		urls.put("珍拉丁湾", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=CHEC");

		urls.put("普吉岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=PHUC");

		urls.put("卡尼岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=KANC");
		urls.put("翡诺岛", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=KANV");

		urls.put("爱必浓", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=ALBC");
		urls.put("康隆尼角", "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=MAUC");

//		http://www.clubmed.com.cn/cm/callProposal.do?PAYS=66&LANG=ZH&village=SAYC&dateDeDebut=2017%2F05%2F07&dureeSejour=4&nbParticipants=2&nbEnfants=0&dateDeNaissance=

		// =====================================================
		List<TaskEntity> tasks = new ArrayList<TaskEntity>();

		// =====================================================
		for (Entry<String, String> entry : urls.entrySet()) {
			for (String check_in_date : days) {// 爬取天
				for (int sleep_day : sleepdays) {// 几晚
					for (int adult_num : number_of_adult) {// 成人
						for (int children_num : number_of_children) {// 儿童
							//只处理1+1,2+0,2+1 => adult_num + children_num > 1
							if(adult_num + children_num <= 1)continue;

							// =====================================================
							String region = entry.getKey();
							String _url = entry.getValue();
							String _check_in_date_arg = date_arg + check_in_date;
							String _sleepdays_arg = sleepdays_arg + sleep_day;
							String _people_arg = people_arg + (adult_num + children_num);
							String _children_num_arg = children_num_arg + children_num;
							String _children_birthday_arg = children_birthday_arg
									+ generateChildrenBirthday(birthday, children_num);
							// --------------------
							_url += (_sleepdays_arg + _check_in_date_arg.replaceAll("-","%2F") + _people_arg);
							if (children_num > 0)
								_url += (_children_birthday_arg.replaceAll("-","%2F") + _children_num_arg);

							String peopleType = Helper.people(adult_num,children_num);

							// =====================================================
							//构建任务对象
							TaskEntity taskEntity = new TaskEntity();
							taskEntity.setAdultNum(adult_num);
							taskEntity.setChannelId(id);
							taskEntity.setChannelName(name);
							taskEntity.setCheckInDate(check_in_date);
							taskEntity.setChildNum(children_num);
							taskEntity.setCreateAt(fs.format(new Date()));
							taskEntity.setPeopleType(peopleType);
							taskEntity.setRegion(region);
							taskEntity.setRegionParam(region);
							taskEntity.setSleep(sleep_day);
							taskEntity.setTryTime(0);
							taskEntity.setUrl(_url);
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

	private String generateChildrenBirthday(String children_birthday, int children) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= children; i++) {
			sb.append(children_birthday + ",");
		}
		if (sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

}
