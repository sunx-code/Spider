//package com.sunx.moudle.channel.qunar;
//
//import com.sunx.moudle.channel.IParser;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.interactions.Actions;
//import org.openqa.selenium.remote.RemoteWebDriver;
//import org.openqa.selenium.remote.ScreenshotException;
//
//import java.io.File;
//import java.util.AbstractMap.SimpleEntry;
//import java.util.*;
//import java.util.Map.Entry;
//
//public class QunarExecutableItem implements IParser {
//	private transient Logger logger = Logger.getLogger(QunarExecutableItem.class);
//
//	@Override
//	public void capture(WebBrowser webBrowser){
//		webBrowser.fetch(pageDriver -> {
//			String channel = super.channel.getKey();
//			String checkInDate = super.check_in_day;
//
//			String url_suffix = "&fromDate=###fromDate&toDate=###toDate";
//			// ==========日期=============================
//
//			for (Entry<Integer, String> entry : sleepDayTocheckOutDay.entrySet()) {
//				// ---------------
//				Date date = DateUtil.generateCrawlDate();
//				String crawlingDate = DateUtil.yyyymmdd(date);
//				// ---------------
//				Integer sleepDay = entry.getKey();
//				String checkOutDate = entry.getValue();
//				// ---------------
//				String _url = url
//						+ url_suffix.replaceAll("###fromDate", checkInDate).replaceAll("###toDate", checkOutDate);
//
//				logger.info("开始处理数据 > " + channel + "\t" + checkInDate + "\t" + checkOutDate + " ...");
//
//				// ---------------
//				// webBrowser.waiting(10);// 10s
//				// phantomJSDriver.executeScript("window.location.href=\""
//				// + _url + "\"");
//				pageDriver.get(_url);
//				webBrowser.waiting(3, 60, () -> {
//					List<WebElement> findElements = pageDriver.findElements(By.className("b_loading"));
//					for (WebElement webElement : findElements) {
//						String attribute = webElement.getAttribute("style");
//						if (StringUtils.containsIgnoreCase(attribute, "none")) {
//							return true;
//						}
//					}
//					return false;
//				});
//				//将代理存到缓冲中
////				ProxyManager.me().offer(proxy);
//				// ---------------
//				// ---------------
//				List<WebElement> findElements2 = pageDriver.findElements(By.cssSelector(".more-room-wraper-ct"));
//				for (WebElement webElement : findElements2) {
//					webElement.click();
//					webBrowser.waiting(1, 1, () -> true);
//				}
//				List<WebElement> findElements = pageDriver.findElements(By.cssSelector(".btn-book-ct"));
//				for (WebElement webElement : findElements) {
//					webElement.click();
//					webBrowser.waiting(1, 1, () -> true);
//				}
//
//				// ---------------
//				{
//					byte[] screenshotAs = FileUtil.getScreenshot(pageDriver);
//					String file_path_image = FileUtil.createPageFile(crawlingDate, channel, region, checkInDate,
//							sleepDay, -1, -1, null, ImageType.BASE64);
//					FileUtils.writeByteArrayToFile(new File(file_path_image), screenshotAs);
//
//					String file_path_pagesource = FileUtil.createPageFile(crawlingDate, channel, region, checkInDate,
//							sleepDay, -1, -1, null, ImageType.HTML);
//					String pageSource = pageDriver.getPageSource();
//
//					logger.info("*****************************************************************");
//					//遍历整个集合,监测是否有数据被隐藏
//					try{
//						List<WebElement> eles = pageDriver.findElements(By.xpath("//td[@class='e2']//div[@class='promote']"));
//						List<String> result = new ArrayList<>();
//						if(eles != null || eles.size() > 0){
//							dealParse(webBrowser,pageDriver,eles,result);
//						}
//						logger.info("当前集合中的数据量有：" + result.size());
//
//						//综合处理结果数据
//						pageSource = dealSource(result,pageSource);
//					}catch (Exception e){}
//					logger.info("*****************************************************************");
//
//					logger.info("网页源码对应地址为:" + file_path_pagesource);
//
//					FileUtils.writeStringToFile(new File(file_path_pagesource), pageSource, "UTF8");
//					// ===================================
//
//					String check_in_date = DateUtil.yyyymmdd3_To_yyyymmdd1(checkInDate);
//					String id = crawlingDate + "," + channel + "," + region + "," + check_in_date + "," + sleepDay;
//
//					String id_generateDigest = MessageDigestUtils
//							.generateDigestByMessageDigestMode(MessageDigestMode.MD5, id);
//					// ===================================
//
//					RawdataResult rawdataResult = new RawdataResult();
//					rawdataResult.setId(id_generateDigest);
//					rawdataResult.setCrawlingDate(date);
//					rawdataResult.setChannel(channel);
//					rawdataResult.setRegion(region);
//					rawdataResult.setPersonGroup(null);
//					rawdataResult.setHouseType(null);
//					rawdataResult.setCheckInDate(check_in_date);
//					rawdataResult.setSleepDays(sleepDay);
//					rawdataResult.setUrl(url);
//					rawdataResult.setFile_path_pagesource(file_path_pagesource);
//					rawdataResult.setFile_path_image(file_path_image);
//
//					CacheService.SERVICE.enquque(rawdataResult);
//					// --------
//					// --------
//				}
//				// ===================================
//			}
//			AliTripSearchExecutableItem.save(url,MessageDigestUtils
//					.generateDigestByMessageDigestMode(MessageDigestMode.MD5, url),new Date(),1);
//			return null;
//		});
//	}
//
//	/**
//	 * 构造网页源码
//	 * @param result
//	 * @param pageSource
//	 * @return
//	 */
//	public String dealSource(List<String> result,String pageSource){
//		if(result == null || result.size() <= 0)return pageSource;
//		StringBuffer buffer = new StringBuffer();
//		for(String input : result){
//			buffer.append("\n" + input);
//		}
//		return pageSource.replaceAll("</html>",buffer.toString() + "</html>");
//	}
//
//	/**
//	 *
//	 * @param browser
//	 * @param driver
//	 * @param eles
//     * @param result
//     */
//	public void dealParse(WebBrowser browser, RemoteWebDriver driver, List<WebElement> eles, List<String> result){
//		try {
//			logger.info("抽取到的tr标签数为:" + eles.size());
//			for (int i = 0; i < eles.size(); i++) {
//				WebElement ele = eles.get(i);
//
//				//抽取标签文本,进行判定
//				String text = ele.getText();
//				if (text != null && text.contains("...")) {
////					WebElement t = ele.findElement(By.xpath("//div[@class='js-product']"));
//					try {
//						Actions builder = new Actions(driver);
//						builder.moveToElement(ele).build().perform();
//
//						//等待渲染完成
//						browser.waiting(2, 2, () -> true);
//					} catch (Exception e) {
//					}
//
//					//抽取文本结构
//					WebElement e = driver.findElement(By.xpath("//div[@class='m-txt-pop logotip']"));
//					String value = e.getText();
//					result.add("<input class='self-tag-for-elipsis' value='" + value + "'/>");
//				}
//			}
//		}catch (ScreenshotException e){
//
//		}catch (Exception e){
//			e.printStackTrace();
//		}
//	}
//
//	public static void main(String[] args) throws Exception {
//		WebBrowser browser = new WebBrowser();
//		browser.init();
//
//		Map<Integer,String> day = new HashMap<>();
//		day.put(2,"2017-04-28");
//
//		SimpleEntry<String, String> channel = new SimpleEntry<String, String>("去哪儿", "Qunar");
////		String url = "http://hotel.qunar.com/city/sanya/dt-12418/?#cityurl=sanya&HotelSEQ=sanya_12418&sgroup=1&roomNum=1&from=hoteldetail&fromDate=2016-11-01&toDate=2016-11-03&rnd=1477466478383";
//		String url = "http://hotel.qunar.com/city/sanya/dt-12418/?_=1#cityurl=sanya&HotelSEQ=sanya_12418&sgroup=1&roomNum=1&from=hoteldetail&fromDate=2017-04-28&toDate=2017-04-29&rnd=1493089524087";
//		QunarExecutableItem item = new QunarExecutableItem(5,channel,"2016-11-03","",url,day);
//		item.capture(browser);
//	}
//}
