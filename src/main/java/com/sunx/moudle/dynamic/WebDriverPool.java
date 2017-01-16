package com.sunx.moudle.dynamic;

import com.sunx.constant.Constant;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openqa.selenium.WebDriver;

/**
 * phantomjs渠道对象池
 */
public class WebDriverPool {
    //webdriver对象池
    private GenericObjectPool<WebDriver> pool = null;
    //构造单利对象
    private static class SingleClass{
        private static WebDriverPool driverPool = new WebDriverPool();
    }

    /**
     * 构造单利对象
     * @return
     */
    public static WebDriverPool me(){
        return SingleClass.driverPool;
    }

    /**
     * 构造函数初始化对象池
     */
    private WebDriverPool(){
        pool = initPool();
    }

    /**
     * 构造对象池
     * @return
     */
    private GenericObjectPool<WebDriver> initPool(){
		//对象工厂
		WebDriverFactory factory = new WebDriverFactory();
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(Constant.WEB_DRIVER_NUM); //整个池最大值
        config.setMaxWaitMillis(1000); //获取不到永远不等待
        config.setTimeBetweenEvictionRunsMillis(5 * 60*1000L); //-1不启动。默认1min一次
        config.setMinEvictableIdleTimeMillis(10 * 60000L); //可发呆的时间,10mins
        GenericObjectPool<WebDriver>  pool = new GenericObjectPool<>(factory, config);
		return pool;
	}

    /**
     * 获取数据
     * @return
     */
    public WebDriver get(){
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 回收数据
     * @param driver
     */
    public void recycle(WebDriver driver){
        pool.returnObject(driver);
    }
    /**
     * 关闭对象池
     */
    public void close(){
        pool.close();
    }
    /**
     * 清空对象池
     */
    public void clear(){
        pool.clear();
    }

    public static void main(String[] args){
        WebDriver driver = WebDriverPool.me().get();
        System.out.println(driver);

        WebDriverPool.me().recycle(driver);
    }
}