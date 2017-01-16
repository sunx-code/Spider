package com.sunx.moudle.channel;

import com.sunx.storage.DBFactory;

public interface IMonitor {
	/**
	 * 监控模块导入数据接口
	 * @param factory
	 * @param id
	 * @param name
     */
	void monitor(DBFactory factory,Long id,String name);
}
