//package com.fosun.fonova.spider.server;
//
//import java.io.IOException;
//
//import org.apache.log4j.PropertyConfigurator;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class Web_Stop {
//
//	static {
//		PropertyConfigurator.configureAndWatch(
//				Thread.currentThread().getContextClassLoader().getResource("web_log4j.properties").getFile(), 5000);
//	}
//	private static final Logger LOGGER = LoggerFactory.getLogger(Web_Stop.class);
//
//	public static void main(String[] args) throws IOException {
////		JMXUtil.invokeMethodToJMXServer(ConfUtil.getWebJMXPort(), "web", mBeanServerConnection -> {
////			try {
////				ObjectName objectName = new ObjectName(Web_Start.OBJECT_NAME);
////				AppMBean mBeanProxy = JMX.newMBeanProxy(mBeanServerConnection, objectName, AppMBean.class);
////				mBeanProxy.stop();
////			} catch (Exception e) {
////				LOGGER.error(ExceptionUtils.getStackTrace(e));
////			}
////		});
//	}
//
//}
