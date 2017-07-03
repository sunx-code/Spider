//package com.sunx.moudle.dynamic;
//
//import java.util.concurrent.TimeUnit;
//
//import com.sunx.constant.Configuration;
//import com.sunx.constant.Constant;
//import org.apache.commons.pool2.BasePooledObjectFactory;
//import org.apache.commons.pool2.PooledObject;
//import org.apache.commons.pool2.impl.DefaultPooledObject;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.phantomjs.PhantomJSDriver;
//import org.openqa.selenium.phantomjs.PhantomJSDriverService;
//import org.openqa.selenium.remote.CapabilityType;
//import org.openqa.selenium.remote.DesiredCapabilities;
//
///**
// *
// * 类名： WebDriverFactory
// * 描述： 对象池的工厂类
// */
//public class WebDriverFactory extends BasePooledObjectFactory<WebDriver> {
//	//默认use-agent
//	private String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36";
//
//	@Override
//	public WebDriver create() throws Exception {
//		DesiredCapabilities caps = DesiredCapabilities.phantomjs();
//		caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
//				new String[] { "--ignore-ssl-errors=yes" });
//		caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, Configuration.me().getString(Constant.PATH_DRIVER));
//		caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", DEFAULT_USER_AGENT);
//		caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "resourceTimeout", 20000);
//		caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages",false);
//
//		caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
//		caps.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
//		caps.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS, true);
//		caps.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
//
//		WebDriver driver = new PhantomJSDriver(caps);
//
//		//设置超时时间
//		driver.manage().timeouts().pageLoadTimeout(Constant.DYNAMIC_LOAD_TIMEOUT, TimeUnit.SECONDS);
//		driver.manage().window().maximize();
//
//		return driver;
//	}
//
//	@Override
//	public PooledObject<WebDriver> wrap(WebDriver arg0) {
//		return new DefaultPooledObject<WebDriver>(arg0);
//	}
//
//	@Override
//	public void passivateObject(PooledObject<WebDriver> arg1) throws Exception {
//
//	}
//
//	@Override
//	public void destroyObject(PooledObject<WebDriver> p) throws Exception {
//		try {
//			//销毁对象
//			p.getObject().close();
//			p.getObject().quit();
//		}catch (Exception e){}
//	}
//}
