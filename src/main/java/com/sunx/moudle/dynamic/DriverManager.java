package com.sunx.moudle.dynamic;

import com.sunx.constant.Configuration;
import com.sunx.moudle.proxy.IProxy;
import com.sunx.moudle.proxy.ProxyManager;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * 浏览器对象的封装
 * 1
 */
public class DriverManager {
    /**
     */
    private static class SingleClass{
        private static DriverManager manager = new DriverManager();
    }

    public static DriverManager me(){
        return SingleClass.manager;
    }

    /**
     * 使用代理
     * @param driver
     */
    public void useProxy(RemoteWebDriver driver){
        try{
            boolean flag = Configuration.me().getBoolean("is.use.proxy");
            if(flag == false)return;
            int i = 0;
            IProxy proxy = null;
            while(true){
                if(i > 5) break;
                proxy = ProxyManager.me().poll();
                if(proxy == null){
                    try{
                        i++;
                        Thread.sleep(500);
                        continue;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
            }
            //获取到代理以后,开始执行代理数据
            if(proxy != null){
                exec(driver,proxy.getHost(),proxy.getPort() + "",-1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param driver
     */
    public void removeProxy(RemoteWebDriver driver){
        boolean flag = Configuration.me().getBoolean("is.use.proxy");
        if(flag == false)return;
        exec(driver,null,null,-1);
    }

    /**
     * 执行代理
     * @param driver
     * @param host
     * @param port
     */
    public void exec(RemoteWebDriver driver,String host,String port,int type){
        try{
            String m = null;
            if(type == 1){
                m = "direct";
            }else{
                m = "manual";
            }
            String proxy_arg = "phantom.setProxy('" + host + "', '" + port + "', '" + m + "', '', '');";
            ((PhantomJSDriver) driver).executePhantomJS(proxy_arg);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
