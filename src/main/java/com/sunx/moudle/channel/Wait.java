package com.sunx.moudle.channel;

import com.google.common.base.Predicate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 等待类
 * 1 给定方法等参数等待浏览器渲染完成
 */
public class Wait {
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(Wait.class);

    /**
     * @param afterWaitingTime(s)
     * @param maxTimeout(s)
     * @param fw
     * @throws InterruptedException
     */
    public static void wait(WebDriver driver, int afterWaitingTime, int maxTimeout, final FunctionWaiting fw){
        try {
            new WebDriverWait(driver, maxTimeout).until(new Predicate<WebDriver>() {
                public boolean apply(WebDriver webDriver) {
                    return fw.apply();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logger.info("waiting (" + afterWaitingTime + ") s.");
            Thread.sleep(afterWaitingTime * 1000);// s
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param driver
     * @param sleep
     */
    public static void wait(WebDriver driver,int sleep, final Object[] lists){
        try {
            new WebDriverWait(driver, sleep).until(new Predicate<WebDriver>() {
                public boolean apply(WebDriver input) {
                    if(lists == null || lists.length < 0)return true;
                    for(Object css : lists){
                        List<WebElement> eles = input.findElements((By)css);
                        if(!eles.isEmpty()) return true;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static interface FunctionWaiting {
        boolean apply();
    }
}
