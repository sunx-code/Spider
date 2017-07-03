package com.sunx.moudle.dynamic;

import com.sunx.constant.Configuration;
import com.sunx.constant.Constant;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class DriverManager {
    //user-agent
    private String UserAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2684.0 Safari/537.36";

    private static class SingleClass{
        private static DriverManager manager = new DriverManager();
    }

    public static DriverManager me(){
        return SingleClass.manager;
    }

    public WebDriver get(){
        DesiredCapabilities caps = DesiredCapabilities.phantomjs();
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
                new String[] { "--ignore-ssl-errors=yes" });
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, Configuration.me().getString(Constant.PATH_DRIVER));
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", UserAgent);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "resourceTimeout", 20000);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages",false);

        caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        caps.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
        caps.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS, true);
        caps.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);

        WebDriver driver = new PhantomJSDriver(caps);

        //设置超时时间
        driver.manage().timeouts().pageLoadTimeout(Constant.DYNAMIC_LOAD_TIMEOUT, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        return driver;
    }

    /**
     * 关闭对象
     * @param driver
     */
    public void recycle(WebDriver driver){
        if(driver == null)return;
        try{
            driver.close();
            driver.quit();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
