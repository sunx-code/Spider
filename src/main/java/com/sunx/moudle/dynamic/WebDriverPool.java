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
    private static GenericObjectPool<WebDriver> pool = initPool();
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
     * 构造对象池
     * @return
     */
    private static GenericObjectPool<WebDriver> initPool(){
		//对象工厂
		WebDriverFactory factory = new WebDriverFactory();
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(Constant.WEB_DRIVER_NUM); //整个池最大值
        config.setMaxWaitMillis(1000); //获取不到永远不等待
        config.setTimeBetweenEvictionRunsMillis(5 * 60 * 1000L);
        config.setMinEvictableIdleTimeMillis(10 * 6000L);
        return new GenericObjectPool<>(factory, config);
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
        if(driver == null)return;
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
        driver.get("http://news.baidu.com");
        System.out.println(driver.getPageSource());

        WebDriverPool.me().recycle(driver);

        driver = WebDriverPool.me().get();
        driver.get("http://news.baidu.com");
        System.out.println(driver.getPageSource());
    }
}
