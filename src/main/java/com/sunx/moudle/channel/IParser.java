package com.sunx.moudle.channel;

import com.sunx.entity.TaskEntity;
import com.sunx.storage.DBFactory;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * 解析接口
 * 1 根据给定的浏览器驱动进行调用
 * 2 采用反射的方式来进行调用
 */
public interface IParser {
    /**
     * 解析接口
     */
    public int parser(DBFactory factory, RemoteWebDriver driver, TaskEntity task);
}
